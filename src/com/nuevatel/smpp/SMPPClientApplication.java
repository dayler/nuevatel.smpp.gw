/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp;

import com.logica.smpp.Connection;
import com.logica.smpp.Data;
import com.logica.smpp.TCPIPConnection;
import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.base.appconn.ActionCollection;
import com.nuevatel.mc.appconn.MCClient;
import com.nuevatel.smpp.session.ClientEventListener;
import com.nuevatel.smpp.session.ClientNoActivityTask;
import com.nuevatel.smpp.session.ClientSession;
import com.nuevatel.smpp.session.SessionProperties;
import com.nuevatel.smpp.session.StatRetrieverTask;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Luis Baldiviezo
 */
public class SMPPClientApplication extends SMPPApplication implements Runnable{

    /** The SMPPApplication singleton */
    private static SMPPClientApplication smppClientApplication;
    /** The default logger*/
    private static final Logger logger = Logger.getLogger(SMPPClientApplication.class.getName());
    /** The SMPP Reconnection thread */
    private ScheduledExecutorService SMPPClientReconnectionThread;
    /** The SMPP Reconnection thread */
    private ScheduledExecutorService SMPPClientEnquireLinkThread;
    /** The SMPP NoActivityTimeou thread */
    private ScheduledExecutorService SMPPClientNoActivityTimeoutThread;
    /** The GSM Expiration thread Pool*/
    private ScheduledThreadPoolExecutor expirationThreadPool;
    private SMPPForwardMTRequestAction forwardMTRequestAction;

    private SMPPClientApplication(){
    }

    @Override public void start(){
        if (isRunning){
            logger.log(Level.WARNING, "Application already running");
            return;
        }
        try {
//            getDbConnectionPool().start();
            CacheHandler.getCacheHandler().setSmppSourceTon(getSmppSourceTon());
            CacheHandler.getCacheHandler().setSmppSourceNpi(getSmppSourceNpi());
            CacheHandler.getCacheHandler().setSmppDestinationTon(getSmppDestinationTon());
            CacheHandler.getCacheHandler().setSmppDestinationNpi(getSmppDestinationNpi());

            CacheHandler.getCacheHandler().setMCNodeID(getMcNodeId());
            CacheHandler.getCacheHandler().setEmptyUDString(getEmptyUDString());
            CacheHandler.getCacheHandler().setDBConnection(getDbConnectionPool());
            CacheHandler.getCacheHandler().loadSessionCache(getMcNodeId());
            CacheHandler.getCacheHandler().setFileLog(isFileLog());
            startAppConn();
            expirationThreadPool = new ScheduledThreadPoolExecutor(getExpirationThreadPoolSize());
            super.start();

            if (getClientReconnectionTimeout()>0){
                //schedule reconnection
                setSMPPClientReconnectionThread(Executors.newSingleThreadScheduledExecutor());
                getSMPPClientReconnectionThread().scheduleAtFixedRate(new SMPPClientReconnectionTask(), getClientReconnectionTimeout(), getClientReconnectionTimeout(), TimeUnit.SECONDS);
            }
            
            if (getClientEnquireLinkPeriod()>0){
                //schedule enquire link
                setSMPPClientEnquireLinkThread(Executors.newSingleThreadScheduledExecutor());
                getSMPPClientEnquireLinkThread().scheduleAtFixedRate(new SMPPClientEnquireLinkTask(), getClientEnquireLinkPeriod(), getClientEnquireLinkPeriod(), TimeUnit.SECONDS);
            }
            if (getClientNoActivityTimeout()>0){
                setSMPPClientNoActivityTimeoutThread(Executors.newSingleThreadScheduledExecutor());
                getSMPPClientNoActivityTimeoutThread().scheduleAtFixedRate(new ClientNoActivityTask(getClientNoActivityTimeout()), 5, 5, TimeUnit.SECONDS);
            }
            if (getStatPrintRate()>0){
                ScheduledThreadPoolExecutor stats = new ScheduledThreadPoolExecutor(1);
                stats.scheduleAtFixedRate(new StatRetrieverTask(this), 0L, getStatPrintRate(), TimeUnit.SECONDS);
            }
            
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void run() {
        for (SessionProperties sessionProperties : CacheHandler.getCacheHandler().getSessionPropertiesCache().values()){
            for (int sessionCount = 0; sessionCount<sessionProperties.getSize(); sessionCount++){
                String remoteAddress = sessionProperties.getAddress();
                int remotePort = sessionProperties.getPort();
                Connection connection = new TCPIPConnection(remoteAddress, remotePort);
                connection.setReceiveTimeout(Data.ACCEPT_TIMEOUT);
                try{
                    connection.open();
                    ClientSession clientSession = new ClientSession(connection, sessionProperties);
//                    clientSession.setSessionProperties(sessionProperties);
                    clientSession.setEventListener(new ClientEventListener(clientSession,getSmppPoolSize()));
                    Thread clientSessionThread = new Thread(clientSession);
                    clientSessionThread.start();
                }catch(IOException ex){
                    logger.severe("No response from "+remoteAddress+":"+remotePort+" "+ex.getMessage());
                }
            }
        }
    }


    /**Establishes communication with MC*/
    private void startAppConn(){

        //create the action collection
        ActionCollection actionCollection = new ActionCollection();
        //create the actions
        forwardMTRequestAction = new SMPPForwardMTRequestAction(getMcPoolSize(), getExpirationDefaultTimeout(), getAppType());
        SMPPLoadCacheAction loadCacheAction = new SMPPLoadCacheAction(this);
        actionCollection.put(getForwardMTRequestAction());
        actionCollection.put(loadCacheAction);
        try {
            mcClient = new MCClient(getMcNodeId(),appClientProperties, actionCollection);
            logger.info("AppConn Client ready");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    /**Returns the SMPPClientApplication*/
    public static synchronized SMPPClientApplication getSMPPClientApplication(){
        if (smppClientApplication==null) smppClientApplication= new SMPPClientApplication();
        return smppClientApplication;
    }

    public ScheduledExecutorService getSMPPClientReconnectionThread(){
        return SMPPClientReconnectionThread;
    }

    public void setSMPPClientReconnectionThread(ScheduledExecutorService smppClientReconnectionThread){
        this.SMPPClientReconnectionThread=smppClientReconnectionThread;
    }

    /**
     * @return the expirationThreadPool
     */
    public ScheduledThreadPoolExecutor getExpirationThreadPool() {
        return expirationThreadPool;
    }

    /**
     * @return the SMPPClientEnquireLinkThread
     */
    public ScheduledExecutorService getSMPPClientEnquireLinkThread() {
        return SMPPClientEnquireLinkThread;
    }

    /**
     * @param SMPPClientEnquireLinkThread the SMPPClientEnquireLinkThread to set
     */
    public void setSMPPClientEnquireLinkThread(ScheduledExecutorService SMPPClientEnquireLinkThread) {
        this.SMPPClientEnquireLinkThread = SMPPClientEnquireLinkThread;
    }

    /**
     * @return the forwardMTRequestAction
     */
    public SMPPForwardMTRequestAction getForwardMTRequestAction() {
        return forwardMTRequestAction;
    }

    /**
     * @return the SMPPClientNoActivityTimeoutThread
     */
    public ScheduledExecutorService getSMPPClientNoActivityTimeoutThread() {
        return SMPPClientNoActivityTimeoutThread;
    }

    /**
     * @param SMPPClientNoActivityTimeoutThread the SMPPClientNoActivityTimeoutThread to set
     */
    public void setSMPPClientNoActivityTimeoutThread(ScheduledExecutorService SMPPClientNoActivityTimeoutThread) {
        this.SMPPClientNoActivityTimeoutThread = SMPPClientNoActivityTimeoutThread;
    }
}
