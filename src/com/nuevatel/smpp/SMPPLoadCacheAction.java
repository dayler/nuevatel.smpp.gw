/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp;

import com.logica.smpp.pdu.Unbind;
import com.nuevatel.mc.appconn.LoadCacheRequestAction;
import com.nuevatel.mc.appconn.MCTypeCollection;
import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.smpp.session.ClientSession;
import com.nuevatel.smpp.session.SessionProperties;
import com.nuevatel.smpp.utils.PropertiesLoader;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
/**
 *
 * @author luis
 */
public class SMPPLoadCacheAction extends LoadCacheRequestAction{
    private SMPPApplication smppApplication;
    private static final Logger logger = Logger.getLogger(SMPPLoadCacheAction.class.getName());

    public SMPPLoadCacheAction(SMPPApplication smppApplication){
        this.smppApplication=smppApplication;

    }
    @Override
    public byte loadCache() {
        byte result = MCTypeCollection.REQUEST_FAILED;
        if (smppApplication.getAppType()==SMPPApplication.APP_TYPE_SERVER){
            try{
                SMPPServerApplication smppServerApplication = (SMPPServerApplication)smppApplication;
                Properties newProperties = PropertiesLoader.getProperties(smppApplication.getPropertiesFile());
                smppServerApplication.loadProperties(newProperties);
                CacheHandler.getCacheHandler().loadSessionCache(smppServerApplication.getMcNodeId());
                logger.info("Cache reload OK");
                result = MCTypeCollection.REQUEST_ACCEPTED;
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }
        else if (smppApplication.getAppType() == SMPPApplication.APP_TYPE_CLIENT){
            try{
                SMPPClientApplication smppClientApplication = (SMPPClientApplication)smppApplication;
                Properties newProperties = PropertiesLoader.getProperties(smppApplication.getPropertiesFile());
                smppClientApplication.loadProperties(newProperties);
                for (SessionProperties sessionProperties : CacheHandler.getCacheHandler().getSessionPropertiesCache().values()){
                    ClientSession clientSession = (ClientSession)CacheHandler.getCacheHandler().getSessionCache().get(sessionProperties.getSmppSessionId());
                    if (clientSession!=null){
                        Unbind unbind= new Unbind();
                        clientSession.send(unbind);
                        clientSession.stop("LoadCache received");
                    }
                }
                CacheHandler.getCacheHandler().loadSessionCache(smppClientApplication.getMcNodeId());

                //shut down reconnection and enquireLink
                if (smppClientApplication.getClientReconnectionTimeout()!=null) smppClientApplication.getSMPPClientReconnectionThread().shutdownNow();
                if (smppClientApplication.getSMPPClientEnquireLinkThread()!=null) smppClientApplication.getSMPPClientEnquireLinkThread().shutdown();

                if (smppClientApplication.getClientReconnectionTimeout()>0){
                    //schedule reconnectionThread at new rate
                    smppClientApplication.setSMPPClientReconnectionThread(Executors.newSingleThreadScheduledExecutor());
                    smppClientApplication.getSMPPClientReconnectionThread().scheduleAtFixedRate(new SMPPClientReconnectionTask(), 0, smppClientApplication.getClientReconnectionTimeout(), TimeUnit.SECONDS);
                }
                if (smppClientApplication.getClientEnquireLinkPeriod()>0){
                    //schedule enquireLinkThread at new rate
                    smppClientApplication.setSMPPClientEnquireLinkThread(Executors.newSingleThreadScheduledExecutor());
                    smppClientApplication.getSMPPClientEnquireLinkThread().scheduleAtFixedRate(new SMPPClientEnquireLinkTask(), smppClientApplication.getClientEnquireLinkPeriod(), smppClientApplication.getClientEnquireLinkPeriod(), TimeUnit.SECONDS);
                }

                logger.info("Cache reload OK");
                result = MCTypeCollection.REQUEST_ACCEPTED;
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }
        return result;
    }
}
