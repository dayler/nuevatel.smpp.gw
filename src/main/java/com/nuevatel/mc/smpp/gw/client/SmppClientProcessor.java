/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.client;

import com.nuevatel.common.util.Parameters;
import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.McMessageId;
import com.nuevatel.mc.smpp.gw.SmppDateUtil;
import com.nuevatel.mc.smpp.gw.dialog.Dialog;
import com.nuevatel.mc.smpp.gw.dialog.DialogService;
import com.nuevatel.mc.smpp.gw.dialog.client.DeliverSmDialog;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.CancelSmEvent;
import com.nuevatel.mc.smpp.gw.event.DataSmEvent;
import com.nuevatel.mc.smpp.gw.event.DefaultResponseOKEvent;
import com.nuevatel.mc.smpp.gw.event.GenericNAckEvent;
import com.nuevatel.mc.smpp.gw.event.QuerySmEvent;
import com.nuevatel.mc.smpp.gw.event.ReplaceSmEvent;
import com.nuevatel.mc.smpp.gw.event.SmppEvent;
import com.nuevatel.mc.smpp.gw.event.SubmitSmppEvent;
import com.nuevatel.mc.smpp.gw.exception.FailedBindOperationException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Data;
import org.smpp.ServerPDUEvent;
import org.smpp.ServerPDUEventListener;
import org.smpp.Session;
import org.smpp.SmppObject;
import org.smpp.TCPIPConnection;
import org.smpp.TimeoutException;
import org.smpp.WrongSessionStateException;
import org.smpp.pdu.AddressRange;
import org.smpp.pdu.BindReceiver;
import org.smpp.pdu.BindRequest;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransciever;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.CancelSM;
import org.smpp.pdu.DataSM;
import org.smpp.pdu.DeliverSM;
import org.smpp.pdu.PDU;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.QuerySM;
import org.smpp.pdu.ReplaceSM;
import org.smpp.pdu.Response;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.UnbindResp;
import org.smpp.pdu.ValueNotSetException;
import org.smpp.util.ByteBuffer;
import org.smpp.util.NotEnoughDataInByteBufferException;
import org.smpp.util.TerminatingZeroNotFoundException;
import org.smpp.pdu.Request;

import static com.nuevatel.common.util.Util.*;

/**
 * Handle all acction on SMPP client.
 * 
 * @author asalazar
 */
public class SmppClientProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppClientProcessor.class);
    
    private static final int DISPATCH_EV_SOURCE_NOT_ALLOWED = 1;
    
    private static final int DISPATCH_EV_OK = 0;

    private static final int TIME_OUT_MC_EVENT_QUEUE = 500;

    /**
     * To receive sync response
     */
    private static final int RECIEVE_TIMEOUT = 20000;

    private static final long REQUEST_TIMEOUT_MS = 500;
    
    private static long defaultValidityPeriod = AllocatorService.getConfig().getDefaultValidityPeriod();
    
    /**
     * All incoming events from the SMSC (SMPP server)
     */
    private BlockingQueue<ServerPDUEvent>serverPduEvents;
    
    /**
     * All outgoing events. Messages to submit to remote SMSC (SMPP server)
     */
    private BlockingQueue<SmppEvent>smppEvents;
    
    /**
     * This is an instance of listener which obtains all PDUs received from the SMSC.
     * Application doesn't have explicitly call Session's receive() function,
     * all PDUs are passed to this application callback object.
     * See documentation in Session, Receiver and ServerPDUEventListener classes
     * form the SMPP library.
     * 
     * Its is instantiated in bind()
     */
    private ServerPDUEventListener pduListener = null;
    
    private SmppGwSession gwSession;
    
    private Session smppSession = null;

    private AddressRange addressRange = new AddressRange();
    
    private DialogService dialogService = AllocatorService.getDialogService();
    
    private McMessageId mcMsgId = new McMessageId();
    
    private boolean bound = false;
    
    private boolean running = false;
    
    public SmppClientProcessor(SmppGwSession gwSession,
                             BlockingQueue<ServerPDUEvent>serverPduEvents, 
                             BlockingQueue<SmppEvent>smppEvents) {
        Parameters.checkNull(gwSession, "gwSession");
        Parameters.checkNull(smppEvents, "smppEvents");
        Parameters.checkNull(smppEvents, "mcEvents");
        
        this.gwSession = gwSession;
        this.serverPduEvents = serverPduEvents;
        this.smppEvents = smppEvents;
    }
    
    /**
     * 
     * @return <code>true</code> if client is bound.
     */
    public boolean isBound() {
        return bound;
    }
    
    /**
     * 
     * Do bind operation, between ESME entity and SMSC (SMPP server).
     * 
     * @throws ValueNotSetException
     * @throws TimeoutException
     * @throws PDUException
     * @throws WrongSessionStateException
     * @throws IOException
     */
    public void bind() throws ValueNotSetException,
                              TimeoutException,
                              PDUException,
                              WrongSessionStateException,
                              IOException {
        if (!running) {
            // Start processor thread
            running = true;
        }
        
        if (isBound()) {
            logger.warn("Already bound, unbind first. SmppSessionId=" + gwSession.getSmppSessionId());
        }
        // Do bind op
        BindRequest request = getBindRequest(gwSession.getBindType());
        TCPIPConnection conn = new TCPIPConnection(gwSession.getSmscAddress(), gwSession.getSmscPort());
        conn.setReceiveTimeout(RECIEVE_TIMEOUT);
        smppSession = new Session(conn);
        // set up request
        request.setSystemId(gwSession.getSystemId());
        request.setPassword(gwSession.getPassword());
        request.setSystemType(gwSession.getSystemType());
        request.setInterfaceVersion((byte)0x34);
        request.setAddressRange(addressRange);
        // send bind req
        pduListener = new SmppClientEventListener(serverPduEvents, smppSession);
        // do bind and register listener in the session
        BindResponse response = smppSession.bind(request, pduListener);
        // Log response
        logger.info("Bind response: " + response.debugString());
        // Check if was succedded
        if (bound =  Data.ESME_ROK == response.getCommandStatus()) {
            logger.info("bind succedded");
        } else {
            logger.error("bind failed. commandStatus:{} bindResponse:", response.getCommandStatus(), response.debugString());
            throw new FailedBindOperationException(response);
        }
    }

    /**
     * Finish connection between ESME entity and SMSC (SMPP server). 
     */
    public void unbind() {
        if (!isBound()) {
            logger.warn("Not bound, cannot unbind. SmppGwSessionId=" + gwSession.getSmppSessionId());
            return;
        }
        // send requests
        if (smppSession.getReceiver().isReceiver()) {
            logger.warn("It can take a while to stop the receiver. SmppSessionId=" + gwSession.getSmppSessionId());
        }
        UnbindResp response = null;
        try {
            response = smppSession.unbind();
        } catch (TimeoutException | PDUException | WrongSessionStateException | IOException ex) {
            logger.warn("Failed unbind.", ex);
        }
        logger.info("Unbind response: " + "Unbind response: " + response == null? null : response.debugString());
    }
    
    /**
     * Receive smpp messages from remote SMSC and consume the PDU.
     */
    public void receive() {
        logger.info("client.recieve...");
        try {
            while (isRunning()) {
                try {
                    if (!isBound()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("manager.receive()  Await until bound...");
                        }
                        // Await until bound
                        Thread.sleep(500L);
                        continue;
                    }
                    // receive till bound
                    ServerPDUEvent smppEvent = serverPduEvents.poll(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS); // 500ms
                    if (smppEvent == null) {
                        // time out
                        logger.trace("No events to process for smppGwId:{}", gwSession.getSmppGwId());
                        continue;
                    }
                    PDU pdu = smppEvent.getPDU();
                    if (pdu != null) {
                        // Find dialog to dispatch message to handle it, if it does not exists create new dialog for incoming messages.
                        Dialog dialog = null;
                        Long dialogId = dialogService.findDialogIdBySequenceNumber(pdu.getSequenceNumber());
                        if (dialogId != null && (dialog = dialogService.getDialog(dialogId)) != null) {
                            // Handle event
                            dialog.handleSmppEvent(smppEvent);
                        } else {
                            // Create dialog
                            if (Data.DELIVER_SM == pdu.getCommandId()) {
                                // Create new dialog for deliver sm
                                // TODO
                                System.out.println("******* " + pdu.debugString() + " time " + ZonedDateTime.now().toString());
                                // SmppSessionId is the processor identifier.
                                Dialog deliverSmDialog = new DeliverSmDialog(mcMsgId.newMcMessageId(LocalDateTime.now(), gwSession.getMcId()), // Assign new message id
                                                                                      gwSession.getSmppSessionId(), // Id to identify the processor
                                                                                      (DeliverSM) pdu); // Pdu
                                // Register and init new dialog
                                ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
                                long tmpValidityPeriod = SmppDateUtil.parseDateTime(now, ((DeliverSM)pdu).getValidityPeriod()).toEpochSecond() - now.toEpochSecond();
                                dialogService.putDialog(deliverSmDialog, tmpValidityPeriod > 0 ? tmpValidityPeriod : defaultValidityPeriod);
                                // Initialize dialog
                                deliverSmDialog.init();
                            } else {
                                // ignore only send ok
                                if (pdu.isRequest()) {
                                    Response response = ((Request)pdu).getResponse();
                                    smppSession.respond(response);
                                }
                            }
                        }
                    }
                } catch (InterruptedException | ValueNotSetException | WrongSessionStateException | IOException ex) {
                    logger.error("On recieving event, the processor:{} ...", gwSession == null ? null : gwSession.getSmppGwId(), ex);
                }
            }
        } catch (Throwable ex) {
            logger.fatal("On recieving event, the processor:{} is stoping ...", gwSession == null ? null : gwSession.getSmppGwId(), ex);
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
        if (smppEvents.offer(event, TIME_OUT_MC_EVENT_QUEUE, TimeUnit.MILLISECONDS)) {
            return DISPATCH_EV_OK;
        }
        return DISPATCH_EV_SOURCE_NOT_ALLOWED;
    }

    //TODO move doEnquireLink
    
    public void enquireLink() throws ValueNotSetException, // do enquireLink
                                     TimeoutException, // do enquireLink
                                     PDUException, // do enquireLink
                                     WrongSessionStateException, // do enquireLink
                                     IOException { // do enquireLink
        if (isBound()) {
            smppSession.enquireLink();
            return;
        }
        
        logger.warn("smppGwId:{} is not bound...", gwSession.getSmppGwId());
    }

    /**
     * Receive messages from local MC and dispatch it to remote SMSC
     * 
     */
    public void dispatch() {
        try {
            while (isRunning()) {
                try {
                    if (!isBound()) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("manager.dispatch() Await until bound...");
                        }
                        // Await until bound
                        Thread.sleep(500);
                        continue;
                    }
                    // get scheduled event
                    SmppEvent smppEvent = smppEvents.poll(TIME_OUT_MC_EVENT_QUEUE, TimeUnit.MILLISECONDS);
                    if (smppEvent == null) {
                        // timeout or unbound
                        continue;
                    }
                    // dispatch to MC
                    dispatchEvent(smppEvent);
                    logger.info("client.dispatch...");
                } catch (IOException ex) {
                    logger.warn("Failed to dispatch smppEvent:{}", smppEvents.toString());
                    // schedule to reconnect
                    bound = false;
                }
            }
        } catch (Throwable ex) {
            logger.error("Fatal Error, stop manager.dispatch()", ex);
        }
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void shutdown() {
        running = false;
    }

    /**
     * Dispatch event to remote SMSC
     * 
     * @param smppEvent event to dispatch
     * @throws IOException 
     * @throws WrongSessionStateException 
     * @throws PDUException 
     * @throws TimeoutException 
     * @throws ValueNotSetException 
     * @throws TerminatingZeroNotFoundException 
     * @throws NotEnoughDataInByteBufferException 
     */
    private void dispatchEvent(SmppEvent smppEvent) throws ValueNotSetException,
                                                           TimeoutException, 
                                                           PDUException, 
                                                           WrongSessionStateException, 
                                                           IOException,
                                                           NotEnoughDataInByteBufferException,
                                                           TerminatingZeroNotFoundException {
        switch (smppEvent.type()) {
        case SubmitSmEvent:
            submitSm(castAs(SubmitSmppEvent.class, smppEvent));
            break;
        case DataSmEvent:
            dataSm(castAs(DataSmEvent.class, smppEvent));
            break;
        case CancelSmEvent:
            cancelSm(castAs(CancelSmEvent.class, smppEvent));
            break;
        case QuerySmEvent:
            querySm(castAs(QuerySmEvent.class, smppEvent));
            break;
        case ReplaceSmEvent:
            replaceSm(castAs(ReplaceSmEvent.class, smppEvent));
            break;
        case DefaultResponseEvent:
            defaultResponse(castAs(DefaultResponseOKEvent.class, smppEvent));
            break;
        case GenericNAckEvent:
            genericNAckResponse(castAs(GenericNAckEvent.class, smppEvent));
            break;
        default:
            logger.warn("Unknown McEvent...");
            break;
        }
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
        smppSession.respond(event.getGenericNack());
    }

    /**
     * 
     * @param smResp Event to contains default response for smpp event.
     * @throws IOException 
     * @throws WrongSessionStateException 
     * @throws ValueNotSetException 
     */
    private void defaultResponse(DefaultResponseOKEvent smResp) throws ValueNotSetException, WrongSessionStateException, IOException {
        Parameters.checkNull(smResp, "smResp");
        smppSession.respond(smResp.getDefaultResponse());
    }

    /**
     * Assign seq number and register it on <code>smppMsgToDialogMap</code>.
     * 
     * @param pdu 
     * @param dialogId 
     */
    private void assignSequenceNumber(PDU pdu, long dialogId) {
        pdu.assignSequenceNumber(true);
        dialogService.registerSequenceNumber(pdu.getSequenceNumber(), dialogId);
    }

    /**
     * 
     * @param type
     * @return <code>BindRequest</code> based on <code>SmppGwSession.BIND_TYPE</code>.
     */
    private BindRequest getBindRequest(SmppGwSession.BIND_TYPE type) {
        switch(type) {
        case T:
            return new BindTransmitter();
        case R:
            return new BindReceiver();
        default:
            return new BindTransciever();
        }
    }

    /**
     * Submit a single message from queue
     * @throws IOException 
     * @throws WrongSessionStateException 
     * @throws PDUException 
     * @throws TimeoutException 
     * @throws ValueNotSetException 
     * @throws TerminatingZeroNotFoundException 
     * @throws NotEnoughDataInByteBufferException 
     */
    private void submitSm(SubmitSmppEvent event) throws ValueNotSetException, // do submit
                                                       TimeoutException, // do submit
                                                       PDUException, // do submit, service type, short message, schedule delivery time, validity period
                                                       WrongSessionStateException, // do submit
                                                       IOException,
                                                       NotEnoughDataInByteBufferException,
                                                       TerminatingZeroNotFoundException { // do submit, short message
        Parameters.checkNull(event, "smMsg");

        SubmitSM request = new SubmitSM();
        // set input values.
        request.setServiceType(gwSession.getSystemType());
        request.setSourceAddr(event.getSourceAddr());
        request.setDestAddr(event.getDestAddr());
        request.setReplaceIfPresentFlag(event.getReplaceIfPresentFlag());
        request.setShortMessageData(new ByteBuffer(event.getData()));
        // set encoding if it is present
        if (StringUtils.isEmptyOrNull(event.getEncoding())) {
            request.setShortMessageEncoding(event.getEncoding());
        }
        request.setScheduleDeliveryTime(event.getScheduleDeliveryTime());
        request.setValidityPeriod(event.getValidityPeriod());
        request.setEsmClass(event.getEsmClass());
        request.setProtocolId(event.getProtocolId());
        request.setPriorityFlag(event.getPriorityFlag());
        // Always assign sequence number
        assignSequenceNumber(request, event.getMessageId());
        // Asynchronous submit request, response is catch in the listener
        smppSession.submit(request);
    }

    /**
     * Cancel single short message
     * 
     * @param event
     * @throws ValueNotSetException
     * @throws TimeoutException
     * @throws PDUException
     * @throws WrongSessionStateException
     * @throws IOException
     */
    private void cancelSm(CancelSmEvent event) throws ValueNotSetException, // do cancel
                                                       TimeoutException, // do cancel
                                                       PDUException, // do cancel, servic type, message id
                                                       WrongSessionStateException, // do cancel
                                                       IOException { // do cancel
        CancelSM request = new CancelSM();
        // input values
        request.setServiceType(event.getServiceType());
        // Id of message to cancel
        request.setMessageId(Long.toString(event.getMessageId()));
        request.setSourceAddr(event.getSourceAddr());
        request.setDestAddr(event.getDestAddr());
        assignSequenceNumber(request, event.getMessageId());
        // send request. Response is catch on listener.
        smppSession.cancel(request);
    }
    
    /**
     * Dispatch data sm event
     * 
     * @param event
     * @throws ValueNotSetException
     * @throws TimeoutException
     * @throws PDUException
     * @throws WrongSessionStateException
     * @throws IOException
     */
    private void dataSm(DataSmEvent event) throws ValueNotSetException, // do data ms
                                                 TimeoutException, // do data sm
                                                 PDUException, // do data sm, service type
                                                 WrongSessionStateException, // do data sm
                                                 IOException { // do data sm
        DataSM request = new DataSM();
        // set input values
        request.setServiceType(event.getServiceType());
        request.setSourceAddr(event.getSourceAddr());
        request.setDestAddr(event.getDestAddr());
        request.setEsmClass(event.getEsmClass());
        request.setRegisteredDelivery(event.getRegisteredDelivery());
        request.setDataCoding(event.getDataCoding());
        // Assign sequence number
        assignSequenceNumber(request, event.getMessageId());
        // send request
        smppSession.data(request);
    }
    
    /**
     * @throws IOException 
     * @throws WrongSessionStateException 
     * @throws PDUException 
     * @throws TimeoutException 
     * @throws ValueNotSetException 
     * 
     */
    private void replaceSm(ReplaceSmEvent event) throws ValueNotSetException, // do replace
                                                       TimeoutException, // do replace
                                                       PDUException, // do replace, message id, short message, schedule delivery time, 
                                                       WrongSessionStateException, // do replace
                                                       IOException { // do replace
        ReplaceSM request = new ReplaceSM();
        // set input values
        request.setMessageId(Long.toString(event.getMessageId()));
        request.setSourceAddr(event.getSourceAddr());
        // not replace short message coding
        request.setShortMessage(event.getShortMessage());
        request.setScheduleDeliveryTime(event.getScheduleDeliveryTime());
        request.setValidityPeriod(event.getValidityPeriod());
        request.setRegisteredDelivery(event.getRegisteredDelivery());
        request.setSmDefaultMsgId(event.getSmDefaultMsgId());
        // Assing sequence number
        assignSequenceNumber(request, event.getMessageId());
        // send msg
        smppSession.replace(request);
    }
    
    private void querySm(QuerySmEvent event) throws ValueNotSetException, // do query
                                                     TimeoutException, // do query
                                                     PDUException, // do query, message id
                                                     WrongSessionStateException, // do query
                                                     IOException { // do query
        QuerySM request = new QuerySM();
        // set input parameters
        request.setMessageId(Long.toString(event.getMessageId()));
        request.setSourceAddr(event.getSourceAddr());
        // Assign sequence number
        assignSequenceNumber(request, event.getMessageId());
        // send request
        smppSession.query(request);
    }
    
    /**
     * Handles all messages from remote SMPP server (remote SMSC), catch it and set in the <code>serverEvents</code> queue.
     */
    private static final class SmppClientEventListener extends SmppObject implements ServerPDUEventListener{
        
        private BlockingQueue<ServerPDUEvent> serverEvents;
        
        public SmppClientEventListener(BlockingQueue<ServerPDUEvent> serverEvents,
                                       Session smppSession) {
            this.serverEvents = serverEvents;
        }
        
        /**
         * Only offer events.
         */
        @Override
        public void handleEvent(ServerPDUEvent pduEvent) {
            if (event == null) {
                logger.warn("Null ServerPDUEvent...");
                return;
            }
            try {
                if (!serverEvents.offer(pduEvent, REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    // warn the queue rejects the event
                    logger.warn("Failed to offer serverPDUEvent:{}", pduEvent.getPDU().debugString());
                }
            } catch (InterruptedException ex) {
                // On offer event
                logger.error("On handleServerPDUEvent", ex);
            }
        }
    }
}
