/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp;

import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.base.appconn.ActionCollection;
import com.nuevatel.mc.appconn.MCClient;
import com.nuevatel.smpp.node.ServerNode;
import com.nuevatel.smpp.session.SessionListener;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Luis Baldiviezo
 */
public class SMPPServerApplication extends SMPPApplication implements Runnable{

    /** The SMPPApplication singleton */
    private static SMPPServerApplication smppServerApplication;
    /** The default logger*/
    private static final Logger logger = Logger.getLogger(SMPPServerApplication.class.getName());
    /** The GSM Expiration thread Pool*/
    ScheduledThreadPoolExecutor expirationThreadPool;

    private SMPPServerApplication(){
    }

    @Override public void start(){
        if (isRunning){
            logger.log(Level.WARNING, "Application already running");
            return;
        }
        try {
//            getDbConnectionPool().start();
            CacheHandler.getCacheHandler().setMCNodeID(getMcNodeId());
            CacheHandler.getCacheHandler().setEmptyUDString(getEmptyUDString());
            CacheHandler.getCacheHandler().setDBConnection(getDbConnectionPool());
            CacheHandler.getCacheHandler().loadSessionCache(getMcNodeId());
            CacheHandler.getCacheHandler().setFileLog(isFileLog());
            
            startAppConn();
            expirationThreadPool = new ScheduledThreadPoolExecutor(getExpirationThreadPoolSize());
            super.start();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void run() {
        try{
            int port = ((ServerNode)CacheHandler.getCacheHandler().getNodeCache().get(getMcNodeId())).getPort();
            SessionListener sessionListener = new SessionListener(port, getSmppPoolSize());
            sessionListener.start();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }


    /**Establishes communication with MC*/
    private void startAppConn(){

        //create the action collection
        ActionCollection actionCollection = new ActionCollection();
        //create the actions
        SMPPForwardMTRequestAction forwardMTRequestAction = new SMPPForwardMTRequestAction(getMcPoolSize(), getExpirationDefaultTimeout(), getAppType());
        SMPPLoadCacheAction loadCacheAction = new SMPPLoadCacheAction(this);
        actionCollection.put(forwardMTRequestAction);
        actionCollection.put(loadCacheAction);
        try {
            mcClient = new MCClient(getMcNodeId(),appClientProperties, actionCollection);
            logger.info("AppConn Client ready");
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    /**Returns the SMPPServerApplication*/
    public static synchronized SMPPServerApplication getSMPPServerApplication(){
        if (smppServerApplication==null) smppServerApplication= new SMPPServerApplication();
        return smppServerApplication;
    }

    public ScheduledThreadPoolExecutor getExpirationThreadPool(){
        return expirationThreadPool;
    }

}
