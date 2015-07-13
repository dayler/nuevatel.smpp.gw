/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

import com.nuevatel.smpp.SMPPApplication;
import com.nuevatel.smpp.SMPPClientApplication;
import com.nuevatel.smpp.cache.CacheHandler;
import java.util.Date;
import java.util.List;

/**
 *
 * @author luis
 */
public class StatRetrieverTask implements Runnable {
    private SMPPClientApplication sMPPClientApplication=null;

    public StatRetrieverTask(SMPPApplication application){
        if (application instanceof SMPPClientApplication){
            sMPPClientApplication = (SMPPClientApplication)application;
        }
    }

    public void run() {
        Date date = new Date();
        System.out.println("Stats at "+date);
        System.out.println("expirationTaskMap size:"+CacheHandler.getCacheHandler().getExpirationTasksMap().size());
        System.out.println("pendingResponsesMap size:"+CacheHandler.getCacheHandler().getPendingResponsesMap().size());
        System.out.println("pendingResponsesMapSRI size:"+CacheHandler.getCacheHandler().getPendingResponsesSRIMap().size());
        System.out.println("unboundSessionPropertiesMap size:"+CacheHandler.getCacheHandler().getUnboundSessionPropertiesMap().size());
        System.out.println("deliveryReceiptAddressMap size:"+CacheHandler.getCacheHandler().getDeliveryReceiptAddressMap().size());
        System.out.println("expirationThreadPoolSize size:"+sMPPClientApplication.getExpirationThreadPool().getQueue().size());
        System.out.println("SMPP Dispatcher thread Pool size:"+sMPPClientApplication.getForwardMTRequestAction().getThreadPool().getQueue().size());

        for (List<Session> sessionInstanceList : CacheHandler.getCacheHandler().getSessionCache().getSessionInstanceListList()){
            for (Session session : sessionInstanceList){
                if (session instanceof ClientSession){
                    ClientSession clientSession = (ClientSession)session;
                    System.out.println("ClientSession:"+clientSession.getSessionProperties().getSmppSessionId());
                    System.out.println("DispatcherQueue size:"+clientSession.getDispatcherQueue().size());
                    System.out.println("MC ThreadPool size:"+((ClientEventListener)clientSession.getEventListener()).getThreadPool().getQueue().size());
                }
            }
        }
    }
}
