/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp;

import com.nuevatel.mc.appconn.ForwardMTSMAdvice;
import com.nuevatel.mc.appconn.MCTypeCollection;
import com.nuevatel.mc.appconn.Name;
import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.smpp.session.ClientSession;
import java.util.logging.Logger;
/**
 *
 * @author Luis Baldiviezo
 */
public class GSMMessageExpirationTask implements Runnable {

    private static final Integer MESSAGE_EXPIRED_SERVICE_MESSAGE=55;
    private static final Logger logger = Logger.getLogger(GSMMessageExpirationTask.class.getName());
//    private ScheduledThreadPoolExecutor threadPool;

    private String id;
    private Name msisdn;
    private Name fromName;
    private Name toName;
    private Integer smppSessionId;
    private int appType;
    
    public GSMMessageExpirationTask(String id, Name msisdn, Name fromName, Name toName, Integer smppSessionId, int appType){
        this.id=id;
        this.msisdn=msisdn;
        this.fromName=fromName;
        this.toName=toName;
        this.smppSessionId=smppSessionId;
        this.appType=appType;
//        this.threadPool=threadPool;
//        logger.finest("GSMMessage created:"+id);
    }
    public void run() {
        int expirationQueueSize=0;
        ForwardMTSMAdvice advice = new ForwardMTSMAdvice(id, MCTypeCollection.REQUEST_FAILED, MESSAGE_EXPIRED_SERVICE_MESSAGE);
        if (appType == SMPPApplication.APP_TYPE_SERVER){
            SMPPServerApplication.getSMPPServerApplication().getMcClient().writeMCAdvice(advice);
            expirationQueueSize = SMPPServerApplication.getSMPPServerApplication().getExpirationThreadPool().getQueue().size();
        }
        else if (appType == SMPPApplication.APP_TYPE_CLIENT){
            SMPPClientApplication.getSMPPClientApplication().getMcClient().writeMCAdvice(advice);
            expirationQueueSize = SMPPClientApplication.getSMPPClientApplication().getExpirationThreadPool().getQueue().size();
        }
        
        CacheHandler.getCacheHandler().getExpirationTasksMap().remove(id);
        logger.fine("GSMMessage expired in queue:"+id);
        logger.finest("GSMMessage expired in queue id:"+id+" msisdn:"+msisdn+" fromName:"+fromName+" toName:"+toName+" smppSessionId:"+smppSessionId+" exp queue size:"+expirationQueueSize);
    }

}