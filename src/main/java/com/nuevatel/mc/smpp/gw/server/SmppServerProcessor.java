/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.server;

import static com.nuevatel.common.util.Util.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Connection;
import org.smpp.Data;
import org.smpp.Receiver;
import org.smpp.ServerPDUEvent;
import org.smpp.Transmitter;
import org.smpp.pdu.DeliverSM;
import org.smpp.pdu.PDU;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.SubmitSM;
import org.smpp.util.ByteBuffer;
import org.smpp.util.NotEnoughDataInByteBufferException;
import org.smpp.util.TerminatingZeroNotFoundException;

import com.nuevatel.common.util.Parameters;
import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.Constants;
import com.nuevatel.mc.smpp.gw.McMessageId;
import com.nuevatel.mc.smpp.gw.SmppDateUtil;
import com.nuevatel.mc.smpp.gw.dialog.Dialog;
import com.nuevatel.mc.smpp.gw.dialog.DialogService;
import com.nuevatel.mc.smpp.gw.dialog.server.SubmitSmDialog;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.DeliverSmEvent;
import com.nuevatel.mc.smpp.gw.event.GenericNAckEvent;
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
    
    private DialogService dialogService = AllocatorService.getDialogService();
    
    private boolean receiving = false;
    
    private Receiver receiver;
    
    private Transmitter transmitter;
    
    private McMessageId mcMsgId = new McMessageId();
    
    /**
     * TODO
     * Count inactivity period before to close connection, by inactivity.
     */
//    private int timeoutCntr = 0;
    
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
    
    /**
     * Take <code>ServerPDUEvent</code>, select its dialog (if no one create it), dispatch it to handle by its dialog.
     */
    public void receive() {
        logger.info("server.receive...");
        try {
        receiver.start();
        receiving = true;
            while (isReceiving()) {
                try {
                    // receive smpp evetn
                    ServerPDUEvent smppEvent = serverPduEvents.poll(Constants.TIMEOUT_POLL_EVENT_QUEUE, TimeUnit.MILLISECONDS);
                    if (smppEvent == null) {
                        // time out
                        logger.trace("No events to process for smppGwId:{}", gwSession == null ? null : gwSession.getSmppGwId());
                        continue;
                    }
                    PDU pdu = smppEvent.getPDU();
                    if (pdu == null) {
                        return;
                    }
                    // Find dialog to dispatch message, if it does not exists
                    // create new dialog.
                    Dialog dialog = null;
                    Long dialogId = dialogService.findDialogIdBySequenceNumber(pdu.getSequenceNumber());
                    if (dialogId != null && (dialog = dialogService.getDialog(dialogId)) != null) {
                        // Handle event
                        dialog.handleSmppEvent(smppEvent);
                    } else {
                        // Create new Dialog
                        if (Data.SUBMIT_SM == pdu.getCommandId() && pdu.isRequest()) {
                            // SmppSessionId is the processor identifier.
                            // TODO
                            Dialog submitDialog = new SubmitSmDialog(mcMsgId.newMcMessageId(LocalDateTime.now(), gwSession.getMcId()), // Assign new message id
                                                                     gwSession.getSmppSessionId()); // Id to identify the processor
                            // Register and init new dialog
                            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
                            long tmpValidityPeriod = SmppDateUtil.parseDateTime(now, ((SubmitSM) pdu).getValidityPeriod()).toEpochSecond() - now.toEpochSecond();
                            dialogService.putDialog(submitDialog, tmpValidityPeriod > 0 ? tmpValidityPeriod : cfg.getDefaultValidityPeriod());
                            // Initialize dialog
                            submitDialog.init();
                        } else {
                            // unknow / unsupported smpp message
                            if (pdu.isRequest()) {
                                // ret nack
                                GenericNAckEvent nackEv = new GenericNAckEvent(pdu.getSequenceNumber(),
                                        Data.ESME_RINVCMDID);
                                // Schedule to dispatch
                                offerSmppEvent(nackEv);
                            } else {
                                // For response ignore it.
                            }
                        }
                    }
                    // TODO logic to select dialogs
                } catch (InterruptedException ex) {
                    logger.error("On recieving event, the processor:{} ...",
                            gwSession == null ? null : gwSession.getSmppGwId(), ex);
                }
            }
        } catch (Throwable ex) {
            logger.fatal("On receiving event, the processor:{} is stoping...",
                    gwSession == null ? null : gwSession.getSmppGwId(), ex);
            shutdown();
        }
    }
    
    /**
     * Schedule a single event for delivering to remote SMSC;
     * 
     * @param event Event to dispatch
     * @return 0 -> event was schedule to dispatch. 1 -> Sender is not allowed to dispatch
     * @throws InterruptedException 
     */
    public int offerSmppEvent(SmppEvent event) throws InterruptedException {
        if (smppEvents.offer(event, Constants.TIMEOUT_OFFER_EVENT_QUEUE, TimeUnit.MILLISECONDS)) {
            return Constants.DISPATCH_EV_OK;
        }
        return Constants.DISPATCH_EV_SOURCE_NOT_ALLOWED;
    }
    
    public void shutdown() {
        receiver.stop();
        receiving = false;
    }
    
    /**
     * Handle outgoing smpp messages. Consume from  <b>smppEvents</b>
     * 
     */
    public void dispatch() {
        try {
            SmppEvent smppEvent = null;
            while (isReceiving()) {
                try {
                    if (!conn.isOpened()) {
                        logger.info("Connection lost...");
                        shutdown();
                        break;
                    }
                    // get scheduled event
                    smppEvent = smppEvents.poll(Constants.TIMEOUT_POLL_EVENT_QUEUE, TimeUnit.MILLISECONDS);
                    if (smppEvent == null) {
                        // timeout
                        continue;
                    }
                    // dispatch to esme
                    dispatchEvent(smppEvent);
                    logger.trace("server.dispatch[{}]...", smppEvent.toString());
                } catch (PDUException | NotEnoughDataInByteBufferException | TerminatingZeroNotFoundException
                        | IOException ex) {
                    logger.warn("Failed to dispatch smppEvent:{}", smppEvent == null ? null : smppEvent.toString());
                    if (logger.isTraceEnabled()) {
                        logger.warn("Exception:", ex);
                    }
                    shutdown();
                }
            }
        } catch (Throwable ex) {
            logger.fatal("Fatal error, stop manager.dispatch()", ex);
        }
    }
    
    private void dispatchEvent(SmppEvent event) throws PDUException,
                                                       NotEnoughDataInByteBufferException,
                                                       TerminatingZeroNotFoundException,
                                                       IOException {
        switch (event.type()) {
        case DeliverSmEvent:
            deliverSm(castAs(DeliverSmEvent.class, event));
            break;

        default:
            logger.warn("Unknown SmppEvent...");
            break;
        }
    }
    
    private void deliverSm(DeliverSmEvent event) throws PDUException, // sm.setData
                                                        NotEnoughDataInByteBufferException, // sm.setData
                                                        TerminatingZeroNotFoundException, // sm.setData
                                                        IOException { // sm.setEncoding, transmitter.send
        DeliverSM deliverSm = new DeliverSM();
        deliverSm.setServiceType(gwSession.getSystemType());
        deliverSm.setEsmClass(event.getEsmClass());
        deliverSm.setSourceAddr(event.getSourceAddr());
        deliverSm.setDestAddr(event.getDestAddr());
        // set short message
        deliverSm.setShortMessageData(new ByteBuffer(event.getData()));
        if (!StringUtils.isEmptyOrNull(event.getEncoding())) {
            deliverSm.setShortMessageEncoding(event.getEncoding());
        }
        deliverSm.setRegisteredDelivery(event.getRegisteredDelivery());
        // Always assign sequence number
        deliverSm.assignSequenceNumber();
        // send pdu
        transmitter.send(deliverSm);
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
            if (!isReceiving()) {
                // no receiving yet
                return;
            }
            // offer event
            if (!serverPduEvents.offer(event, Constants.TIMEOUT_OFFER_EVENT_QUEUE, TimeUnit.MILLISECONDS)) {
                // warn the queue rejects the event
                logger.warn("Failed to offer serverPDUEvent:{}", event.getPDU().debugString());
            }
        } catch (InterruptedException ex) {
            // on offer event
            logger.error("On handleServerPDUEvent...", ex);
        }
    }
}
