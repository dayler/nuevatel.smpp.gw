/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp;

import com.logica.smpp.pdu.EnquireLink;
import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.smpp.session.Session;
import java.util.List;

/**
 *
 * @author luis
 */
public class SMPPClientEnquireLinkTask implements Runnable{

    @Override public void run() {
        for (List<Session> sessionInstanceList : CacheHandler.getCacheHandler().getSessionCache().getSessionInstanceListList()){
            for (Session session : sessionInstanceList){
                if (session.getState()==Session.STATE_BOUND_RX || session.getState()==Session.STATE_BOUND_TRX || session.getState()==Session.STATE_BOUND_TX){
                    EnquireLink enquireLink = new EnquireLink();
                    enquireLink.assignSequenceNumber();
                    session.send(enquireLink);
                }
            }
        }
    }
}
