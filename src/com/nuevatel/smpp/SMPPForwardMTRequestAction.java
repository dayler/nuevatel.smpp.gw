/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp;

import com.nuevatel.mc.appconn.ForwardMTSMRequestAction;
import com.nuevatel.mc.appconn.MCTypeCollection;
import com.nuevatel.mc.appconn.Name;
import com.nuevatel.mc.tpdu.SMSDeliver;
import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.smpp.dispatcher.DeliverReceiptSMPPDispatcher;
import com.nuevatel.smpp.dispatcher.SMPPDispatcher;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 *
 * @author luis
 */
public class SMPPForwardMTRequestAction extends ForwardMTSMRequestAction{
    private ScheduledThreadPoolExecutor threadPool;
    private static final Logger logger = Logger.getLogger(SMPPForwardMTRequestAction.class.getName());
    private int expireTimeout;
    private int appType;


    public SMPPForwardMTRequestAction(int threadPoolSize, int expireTimeout, int appType){
        ThreadFactory threadFactory = new ThreadFactory() {

            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("SMPPDispatcher");
                return thread;
            }
        };
        threadPool=new ScheduledThreadPoolExecutor(threadPoolSize, threadFactory);
        this.expireTimeout=expireTimeout;
        this.appType=appType;
    }

    @Override
    public byte forwardMTSMRequest(String string, Name name, byte[] bytes, Byte b, String string1, String string2, Name name1) {
        return MCTypeCollection.REQUEST_FAILED;
    }

    @Override
    public byte forwardMTSMRequest(String messageId, String referenceId, Name msisdn, Name fromName, Name toName, byte[] tpdu, Byte tpduType, Integer smppSessionId) {
//        if (msisdn==null) logger.finest("NULL msisdn" +"id:"+messageId+" msisdn:"+msisdn+" fromName:"+fromName+" toName:"+toName+" smppSessionId:"+smppSessionId); 
        try {
            //this is our message, first schedule expiration
            
            GSMMessageExpirationTask expirationTask = new GSMMessageExpirationTask(messageId, msisdn, fromName, toName, smppSessionId, appType);
            RunnableScheduledFuture expirationResult=null;
            if (appType==SMPPApplication.APP_TYPE_SERVER){
                expirationResult = (RunnableScheduledFuture)SMPPServerApplication.getSMPPServerApplication().getExpirationThreadPool().schedule(expirationTask, expireTimeout, TimeUnit.SECONDS);
            }
            else if (appType==SMPPApplication.APP_TYPE_CLIENT){
                expirationResult = (RunnableScheduledFuture)SMPPClientApplication.getSMPPClientApplication().getExpirationThreadPool().schedule(expirationTask, expireTimeout, TimeUnit.SECONDS);
            }
            RunnableScheduledFuture result = CacheHandler.getCacheHandler().getExpirationTasksMap().put(messageId, expirationResult);
            if (result!=null) logger.warning("expirationTaskMap not NULL for "+messageId);

            //then process the message
            if (tpduType== MCTypeCollection.SMS_DELIVER){
                //this is an sms deliver
                GSMMessage gsmMessage = new GSMMessage(messageId, msisdn, new SMSDeliver(tpdu), smppSessionId);
                getThreadPool().execute(new SMPPDispatcher(gsmMessage,appType));
                return MCTypeCollection.REQUEST_ACCEPTED;
            }
            else if (tpduType == MCTypeCollection.SMS_STATUS_REPORT){
                //this is an sms status report
                getThreadPool().execute(new DeliverReceiptSMPPDispatcher(messageId, referenceId, fromName, toName, smppSessionId));
               return MCTypeCollection.REQUEST_ACCEPTED;
            }
            else{
                logger.warning("unknown message received from MC "+tpduType);
                return MCTypeCollection.REQUEST_FAILED;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
//            logger.severe(ex.getMessage());
            return MCTypeCollection.REQUEST_FAILED;
        }
    }

    /**
     * @return the threadPool
     */
    public ScheduledThreadPoolExecutor getThreadPool() {
        return threadPool;
    }
}
