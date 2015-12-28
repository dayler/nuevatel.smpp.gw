/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.server;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Connection;
import org.smpp.NotSynchronousException;
import org.smpp.Receiver;
import org.smpp.ServerPDUEvent;
import org.smpp.ServerPDUEventListener;
import org.smpp.Session;
import org.smpp.SmppObject;
import org.smpp.TimeoutException;
import org.smpp.Transmitter;
import org.smpp.pdu.PDU;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.UnknownCommandIdException;
import org.smpp.pdu.ValueNotSetException;

import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.Constants;
import com.nuevatel.mc.smpp.gw.dialog.DialogService;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.SmppEvent;

/**
 * 
 * @author Ariel Salazar
 *
 */
public class SmppServerProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppServerGwProcessor.class);
    
    /**
     * All incoming events from the SMSC (SMPP server)
     */
    private BlockingQueue<ServerPDUEvent>serverPduEvents;
    
    /**
     * All outgoing events. Messages to submit to remote SMSC (SMPP server)
     */
    private BlockingQueue<SmppEvent>smppEvents;
    
    private SmppGwSession gwSession;
    
    private Connection conn;
    
    /**
     * Smpp session, bind from client to server.
     */
    private Session smppSession = null;
    
    private DialogService dialogService = AllocatorService.getDialogService();
    
    private boolean receiving = false;
    
    private Receiver receiver;
    
    private Transmitter transmitter;
    
    /**
     * Count inactivity period before to close connection, by inactivity.
     */
    private int timeoutCntr = 0;
    
    private Config cfg = AllocatorService.getConfig();
    
    public SmppServerProcessor(SmppGwSession gwSession,
                               Connection conn,
                               BlockingQueue<ServerPDUEvent> serverPduEvents,
                               BlockingQueue<SmppEvent> smppEvents) {
        Parameters.checkNull(gwSession, "gwSession");
        Parameters.checkNull(conn, "conn");
        Parameters.checkNull(serverPduEvents, "serverPduEvents");
        Parameters.checkNull(smppEvents, "smppEvents");
        
        this.gwSession = gwSession;
        this.conn = conn;
        this.serverPduEvents = serverPduEvents;
        this.smppEvents = smppEvents;
        transmitter = new Transmitter(conn);
        receiver = new Receiver(transmitter, conn);
        // set handler
        receiver.setServerPDUEventListener((event)->handleServerPduEvent(event));
    }
    
    public void receive() {
        receiver.start();
        receiving = true;
        while(isReceiving()) {
            try {
                // receive smpp evetn
                ServerPDUEvent smppEvent = serverPduEvents.poll(Constants.TIMEOUT_POLL_EVENT_QUEUE, TimeUnit.MILLISECONDS);
                if (smppEvent == null) {
                    // time out
                    logger.trace("No events to process for smppGwId:{}", gwSession.getSmppGwId());
                    continue;
                }
                PDU pdu = smppEvent.getPDU();
                if (pdu == null) {
                    return;
                }
                
                // TODO logic to select dialogs
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    /**
     * TODO remove throws declaration and use try-catch sequence
     * 
     * Handle incoming smpp messages. Consume from <b>serverPduEvents</b>.
     * 
     */
//    public void handleServerPduEvent() throws UnknownCommandIdException, TimeoutException, NotSynchronousException, PDUException, IOException {
//        PDU pdu = null;
//        receiver.start();
//        receiving = true;
//        
//        while (isReceiving()) {
//            pdu = receiver.receive(cfg.getServerReceiverTimeout());
//            
//            if (pdu == null) {
//                timeoutCntr++;
//                // TODO evaluate max for timeout counter
//                if (timeoutCntr > 5) {
//                    logger.info("Not reqest nor response => not doing anything.");
//                }
//            } else {
//                // Handle pdu request
//            }
//        }
//    }
    
    public void shutdown() {
        receiving = false;
    }
    
    /**
     * TODO Remove throws from method
     * 
     * Handle outgoing smpp messages. Consume from  <b>smppEvents</b>
     * 
     */
    public void dispatch() throws InterruptedException {
        while (isReceiving()) {
            if (!conn.isOpened()) {
                logger.info("Connection lost...");
                break;
            }
            // get scheduled event
            SmppEvent smppEvent = smppEvents.poll(Constants.TIMEOUT_POLL_EVENT_QUEUE, TimeUnit.MILLISECONDS);
            if (smppEvent == null) {
                // timeout
                continue;
            }
            // dispatch to esme
            dispatchEvent(smppEvent);
            logger.trace("server.dispatch[{}]...", smppEvent.toString());
        }
    }
    
    private void dispatchEvent(SmppEvent event) {
        // TODO
    }
    
    private void sendPdu(PDU pdu) throws ValueNotSetException, IOException {
        timeoutCntr = 0;
        // send pdu over transmitter
        transmitter.send(pdu);
    }
    
    public boolean isReceiving() {
        return receiving;
    }
    
    /**
     * Used on ServerPDUEventListener. Insert each event on serverPduEvents.
     * @param event
     */
    public void handleServerPduEvent(ServerPDUEvent event) {
        if (event == null) {
            logger.warn("Null ServerPDUEvent...");
            return;
        }

        try {
            if (serverPduEvents.offer(event, Constants.TIMEOUT_REQUEST_EVENT_QUEUE, TimeUnit.MILLISECONDS)) {
                // warn the queue rejects the event
                logger.warn("Failed to offer serverPDUEvent:{}", event.getPDU().debugString());
            }
        } catch (InterruptedException ex) {
            // on offer event
            logger.error("On handleServerPDUEvent...", ex);
        }
    }
}
