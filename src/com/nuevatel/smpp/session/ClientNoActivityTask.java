/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

import com.logica.smpp.pdu.Unbind;
import com.nuevatel.smpp.cache.CacheHandler;
import java.util.Date;
import java.util.List;

/**
 *
 * @author luis
 */
public class ClientNoActivityTask implements Runnable{

    private int timeout;
    public ClientNoActivityTask(int timeout){
        this.timeout=timeout;
    }

    public void run() {
        for (List<Session> sessionInstanceList : CacheHandler.getCacheHandler().getSessionCache().getSessionInstanceListList()){
            for (Session session : sessionInstanceList){
                Date currentDate = new Date();
                if (currentDate.getTime()-session.getLastReceivedMessageTimestamp().getTime()>1000*timeout){
                    Unbind unbind = new Unbind();
                    unbind.assignSequenceNumber();
                    session.send(unbind);
//                    session.stop("NoActivityTimeout Reached at " + timeout + " seconds");
                }
            }
        }
    }
}
