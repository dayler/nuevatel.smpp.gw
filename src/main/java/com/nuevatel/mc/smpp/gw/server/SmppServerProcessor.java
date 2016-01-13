/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.server;

import static com.nuevatel.common.util.Util.*;

import java.io.IOException;
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
import org.smpp.WrongSessionStateException;
import org.smpp.pdu.BindRequest;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.DeliverSM;
import org.smpp.pdu.PDU;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.Request;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.ValueNotSetException;
import org.smpp.pdu.WrongLengthOfStringException;
import org.smpp.util.ByteBuffer;
import org.smpp.util.NotEnoughDataInByteBufferException;
import org.smpp.util.TerminatingZeroNotFoundException;

import com.nuevatel.common.util.Delegate;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.Constants;
import com.nuevatel.mc.smpp.gw.SmppDateUtil;
import com.nuevatel.mc.smpp.gw.dialog.Dialog;
import com.nuevatel.mc.smpp.gw.dialog.DialogService;
import com.nuevatel.mc.smpp.gw.dialog.server.SmscSubmitSmDialog;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.DefaultResponseOKEvent;
import com.nuevatel.mc.smpp.gw.event.DeliverSmEvent;
import com.nuevatel.mc.smpp.gw.event.GenericNAckEvent;
import com.nuevatel.mc.smpp.gw.event.GenericResponseEvent;
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
    
    private Config cfg = AllocatorService.getConfig();
    
    private boolean bound = false;
    
    private Delegate onShutdownDelegate = null;
    
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
    
    public boolean isBound() {
        return bound;
    }
    
    private int checkIdentity(BindRequest request) {
        // user = system id
        if (!gwSession.getSystemId().equals(request.getSystemId())) {
            logger.info("Invalid system id BindRequest:{}", request.debugString());
            return Data.ESME_RINVSYSID;
        }
        
        if (!gwSession.getPassword().equals(request.getPassword())) {
            logger.info("Invalid password BindRequest:{}", request.debugString());
            return Data.ESME_RINVPASWD;
        }
        
        logger.info("authenticated for system id:{} BindRequest:{}", request.getSystemId(), request.debugString());
        return Data.ESME_ROK;
    }
    
    private void handleBindRequest(Request request) throws WrongLengthOfStringException, InterruptedException {
        int commandId = request.getCommandId();
        if (Data.BIND_TRANSMITTER == commandId
            || Data.BIND_RECEIVER == commandId
            || Data.BIND_TRANSCEIVER == commandId) {
            // do bind
            int cmdSt = checkIdentity((BindRequest) request);
            if (Data.ESME_ROK == cmdSt) {
                // Successful
                BindResponse bindResponse = (BindResponse) request.getResponse();
                bindResponse.setSystemId(gwSession.getSystemId());
                GenericResponseEvent gresp = new GenericResponseEvent(bindResponse);
                offerSmppEvent(gresp);
                // enable bound
                bound = true;
                logger.info("Bind succeded for systemId:{} systemType:{} bindType:{}", gwSession.getSystemId(), gwSession.getSystemType(), gwSession.getBindType());
            } else {
                // failed to authenticated
                GenericResponseEvent nackRespEv = new GenericResponseEvent(request.getResponse(), cmdSt);
                offerSmppEvent(nackRespEv);
                bound = false;
                // shutdown processor
                shutdown();
            }
        } else {
            // no bound, if not bound, then server expects bound pdu.
            if (request.canResponse()) {
                GenericResponseEvent gresp = new GenericResponseEvent(request.getResponse(), Data.ESME_RINVBNDSTS);
                offerSmppEvent(gresp);
            } else {
                //  cannot response
            }
        }
    }
    
    private boolean offerSmppEvent(SmppEvent event) throws InterruptedException {
        return smppEvents.offer(event, Constants.TIMEOUT_OFFER_EVENT_QUEUE, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Take <code>ServerPDUEvent</code>, select its dialog (if no one create it), dispatch it to handle by its dialog.
     */
    public void receive() {
        logger.info("server.receive...");
        try {
            receiver.start();
            setReceiving(true);
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
                        logger.warn("smppGwId:{}. Null pdu...", gwSession == null ? null : gwSession.getSmppGwId());
                        continue;
                    }
                    // bound
                    if (!isBound()) {
                        if (pdu instanceof BindRequest) {
                            handleBindRequest((Request) pdu);
                            continue;
                        } else {
                            GenericResponseEvent gresp = new GenericResponseEvent(((Request)pdu).getResponse(), Data.ESME_RINVBNDSTS);
                            offerSmppEvent(gresp);
                            continue;
                        }
                    } else {
                        // already bound
                        // enquirelink
                        if (pdu.isRequest() && Data.ENQUIRE_LINK == pdu.getCommandId()) {
                            handleEnquireLinkRequest(pdu);
                            continue;
                        }
                        // Find dialog to dispatch message, if it does not exists
                        // create new dialog.
                        Dialog dialog = null;
                        Long dialogId = dialogService.findDialogIdBySequenceNumber(pdu.getSequenceNumber());
                        if (dialogId != null && (dialog = dialogService.getDialog(dialogId)) != null) {
                            // Handle event
                            // TODO debug
                            System.out.println("Found dialog id:" + dialogId);
                            dialog.handleSmppEvent(smppEvent);
                        } else { 
                            if (pdu.isRequest() && Data.SUBMIT_SM == pdu.getCommandId()) {
                                handleSubmitSmRequest(smppEvent);
                            } else if (pdu.isRequest()
                                       && (Data.BIND_TRANSMITTER == pdu.getCommandId()
                                       || Data.BIND_RECEIVER == pdu.getCommandId()
                                       || Data.BIND_TRANSCEIVER == pdu.getCommandId())) {
                                    // already bind
                                    logger.warn("Already bind. system id:{} smppGwId{} smppSessionId:{}", gwSession.getSystemId(), gwSession.getSmppGwId(), gwSession.getSmppSessionId());
                                    GenericResponseEvent resp = new GenericResponseEvent(((Request)pdu).getResponse(), Data.ESME_RALYBND);
                                    offerSmppEvent(resp);
                            } else if (pdu.isRequest() && Data.UNBIND == pdu.getCommandId()) {
                                // unbind
                                handleUnbindRequest(pdu);
                            } else {
                                // unknow / unsupported smpp message
                                if (pdu.isRequest()) {
                                    // ret nack
                                    GenericNAckEvent nackEv = new GenericNAckEvent(pdu.getSequenceNumber(), Data.ESME_RINVCMDID);
                                    // Schedule to dispatch
                                    offerSmppEvent(nackEv);
                                } else {
                                    // For response ignore it.
                                }
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    logger.error("On recieving event, the processor:{} ...",
                            gwSession == null ? null : gwSession.getSmppGwId(), ex);
                }
            }
            logger.info("End server.receive...");
        } catch (Throwable ex) {
            logger.fatal("On receiving event, the processor:{} is stoping...",
                    gwSession == null ? null : gwSession.getSmppGwId(), ex);
            shutdown();
        }
    }

    private void handleUnbindRequest(PDU pdu) throws InterruptedException {
        logger.info("Unbind. system id:{} smppGwId{} smppSessionId:{}", gwSession.getSystemId(), gwSession.getSmppGwId(), gwSession.getSmppSessionId());
        DefaultResponseOKEvent rok = new DefaultResponseOKEvent((Request)pdu);
        offerSmppEvent(rok);
        shutdown();
    }

    private void handleSubmitSmRequest(ServerPDUEvent smppEv) {
        // Create new Dialog
        // SmppSessionId is the processor identifier.
        Dialog submitDialog = new SmscSubmitSmDialog(gwSession.getSmppSessionId()); // Id to identify the processor
        // Register and init new dialog
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        long tmpValidityPeriod = SmppDateUtil.parseDateTime(now, ((SubmitSM) smppEv.getPDU()).getValidityPeriod()).toEpochSecond() - now.toEpochSecond();
        // Initialize dialog
        submitDialog.init();
        // handle event
        submitDialog.handleSmppEvent(smppEv);
        // put dialog, after handleSmppEvent is retrieved message id
        dialogService.putDialog(submitDialog, tmpValidityPeriod > 0 ? tmpValidityPeriod : cfg.getDefaultValidityPeriod());
    }

    private void handleEnquireLinkRequest(PDU pdu) throws InterruptedException {
        DefaultResponseOKEvent rok = new DefaultResponseOKEvent((Request)pdu);
        offerSmppEvent(rok);
    }
    
    public void setOnShutdownDelegate(Delegate onShutdownDelegate) {
        this.onShutdownDelegate = onShutdownDelegate;
    }
    
    public void shutdown() {
        // Action to execute before to shutdown.
        if (onShutdownDelegate != null) {
            onShutdownDelegate.execute();
        }
        // stop all services
        receiver.stop();
        setReceiving(false);
        bound = false;
        try {
            if (conn.isOpened()) {
                conn.close();
            }
        } catch (IOException ex) {
            logger.warn("smppGwId{}: Failed to close connection....", gwSession.getSmppGwId());
        }
    }
    
    /**
     * Handle outgoing smpp messages. Consume from  <b>smppEvents</b>
     * 
     */
    public void dispatch() {
        logger.info("server.dispatch...");
        try {
            SmppEvent smppEvent = null;
            // TODO debug
            System.out.println("isReceiving = " + isReceiving());
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
                    dispatchSmppEvent(smppEvent);
                    logger.trace("server.dispatch[{}]...", smppEvent.toString());
                } catch (PDUException | NotEnoughDataInByteBufferException | TerminatingZeroNotFoundException | IOException ex) {
                    // TODO ex debug
                    logger.warn("Failed to dispatch smppEvent:{}", smppEvent == null ? null : smppEvent.toString(), ex);
                    if (logger.isTraceEnabled()) {
                        logger.warn("Exception:", ex);
                    }
                    shutdown();
                }
            }
            // TODO debug
            System.out.println("End server.dispatch...");
        } catch (Throwable ex) {
            logger.fatal("Fatal error, stop manager.dispatch()", ex);
        }
    }
    
    private void dispatchSmppEvent(SmppEvent event) throws PDUException,
                                                           NotEnoughDataInByteBufferException,
                                                           TerminatingZeroNotFoundException,
                                                           IOException,
                                                           WrongSessionStateException {
        switch (event.type()) {
        case DeliverSmEvent:
            deliverSm(castAs(DeliverSmEvent.class, event));
            break;
        case GenericROkResponseEvent:
            genericROkResponse(castAs(GenericResponseEvent.class, event));
            break;
        case DefaultROkResponseEvent:
            defaultROkResponse(castAs(DefaultResponseOKEvent.class, event));
            break;
        case GenericNAckEvent:
            genericNAckResponse(castAs(GenericNAckEvent.class, event));
            break;

        default:
            logger.warn("Unknown SmppEvent...");
            break;
        }
    }
    
    private void genericROkResponse(GenericResponseEvent event) throws ValueNotSetException, WrongSessionStateException, IOException {
        Parameters.checkNull(event, "event");
        transmitter.send(event.getResponse());
    }
    
    /**
     * Dispatch generic NACK event.
     * 
     * @param event
     * @throws ValueNotSetException
     * @throws WrongSessionStateException
     * @throws IOException
     */
    private void genericNAckResponse(GenericNAckEvent event) throws ValueNotSetException, WrongSessionStateException, IOException {
        Parameters.checkNull(event, "event");
        transmitter.send(event.getGenericNack());
    }
    
    /**
     * 
     * @param smResp Event to contains default response for smpp event.
     * @throws IOException 
     * @throws WrongSessionStateException 
     * @throws ValueNotSetException 
     */
    private void defaultROkResponse(DefaultResponseOKEvent smResp) throws ValueNotSetException, WrongSessionStateException, IOException {
        Parameters.checkNull(smResp, "smResp");
        transmitter.send(smResp.getDefaultResponse());
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
        // optional param, receiped message id
        if (event.isDeliveryAck()) {
            deliverSm.setReceiptedMessageId(event.getReceiptedMessageId());
        }
        // set command status
        deliverSm.setCommandStatus(event.getCommandStatus());
        // Assign sequence number
        assignSequenceNumber(deliverSm, event.getMessageId());
        // send pdu
        transmitter.send(deliverSm);
    }
    
    /**
     * Assign seq number and register it on <code>smppMsgToDialogMap</code>.
     * 
     * @param pdu 
     * @param dialogId 
     */
    private void assignSequenceNumber(PDU pdu, long dialogId) {
        pdu.assignSequenceNumber(true);
        if (dialogId < 0) {
            // no map
            return;
        }
        dialogService.registerSequenceNumber(pdu.getSequenceNumber(), dialogId);
    }
    
    private synchronized void setReceiving(boolean receiving) {
        this.receiving = receiving;
        // wake up all threads
        notifyAll();
    }
    
    public synchronized boolean isReceiving() {
        if (!receiving) {
            // await until activation 500ms
            try {
                wait(1000L);
            } catch (InterruptedException ex) {
                logger.warn("Failed on wait until receiving...", ex);
            }
        }
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
        // TODO
        System.out.println("-------------- " + event.getPDU().debugString());
        
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
    
    public boolean isConnected() {
        return conn == null ? false : conn.isOpened();
    }
}
