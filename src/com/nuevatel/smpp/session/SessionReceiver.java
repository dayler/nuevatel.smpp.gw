/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

import com.logica.smpp.Connection;
import com.logica.smpp.Receiver;
import com.logica.smpp.Transmitter;
import java.util.logging.Logger;

/**
 *
 * @author luis
 */
public class SessionReceiver extends Receiver{
    private Session session;
    private static final Logger logger = Logger.getLogger(SessionReceiver.class.getName());
    
    public SessionReceiver(Transmitter transmitter, Connection connection, Session session, Integer processSleepMillis){
        super(transmitter,connection);
        this.session=session;
        setProcessSleepMillis(processSleepMillis);
    }

    @Override
    protected void stopProcessing(Exception e) {
//        logger.finest("Calling stopProcessing"+e.getMessage());
        super.stopProcessing(e);
        if (session!=null) session.stop("Remote Socket Closed");
//        logger.finest("exiting stopProcessing"+e.getMessage());
    }
}
