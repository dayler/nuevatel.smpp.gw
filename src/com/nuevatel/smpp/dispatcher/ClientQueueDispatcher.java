/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.dispatcher;

import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.smpp.session.ClientEventListener;
import com.nuevatel.smpp.session.ClientSession;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.logging.Logger;

/**
 *
 * @author luis
 */
public class ClientQueueDispatcher implements Runnable{

    private ClientSession clientSession;
    private int EMPTY_QUEUE_SLEEP=1000;
    private static final Logger logger = Logger.getLogger(ClientQueueDispatcher.class.getName());

    public ClientQueueDispatcher(ClientSession clientSession){
        this.clientSession=clientSession;
    }

    public void run() {
        while (true){
            while (!clientSession.getDispatcherQueue().isEmpty()){
                ClientEventListener clientEventListener = (ClientEventListener)clientSession.getEventListener();
                SMPPDispatcher smppDispatcher = clientSession.getDispatcherQueue().poll();
                RunnableScheduledFuture expirationResult = CacheHandler.getCacheHandler().getExpirationTasksMap().get(smppDispatcher.getId());
                if (expirationResult!=null){
                    //hasn't expired yet, schedule in threadPool
                    clientEventListener.getThreadPool().submit(smppDispatcher);
                }
                
            }
            try{
                Thread.sleep(EMPTY_QUEUE_SLEEP);
            }
            catch (InterruptedException ex){
                logger.warning(ex.getMessage());
            }
        }
    }

}
