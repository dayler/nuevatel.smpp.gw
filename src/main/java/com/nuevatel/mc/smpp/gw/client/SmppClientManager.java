/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.client;

import com.nuevatel.common.util.IntegerUtil;
import com.nuevatel.common.util.LongUtil;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.common.util.StringUtils;
import com.nuevatel.common.util.UniqueID;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.PropName;
import com.nuevatel.mc.smpp.gw.SmppDateUtil;
import com.nuevatel.mc.smpp.gw.dialog.DeliverSmDialog;
import com.nuevatel.mc.smpp.gw.dialog.Dialog;
import com.nuevatel.mc.smpp.gw.dialog.DialogService;
import com.nuevatel.mc.smpp.gw.dialog.SmppEvent;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.CancelSmMcEvent;
import com.nuevatel.mc.smpp.gw.event.DataSmMcEvent;
import com.nuevatel.mc.smpp.gw.event.McIncomingEvent;
import com.nuevatel.mc.smpp.gw.event.QuerySmMcEvent;
import com.nuevatel.mc.smpp.gw.event.ReplaceSmMcEvent;
import com.nuevatel.mc.smpp.gw.event.SubmitSmMcIEvent;
import com.nuevatel.mc.smpp.gw.exception.FailedBindOperationException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
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
import org.smpp.pdu.EnquireLink;
import org.smpp.pdu.EnquireLinkResp;
import org.smpp.pdu.PDU;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.QuerySM;
import org.smpp.pdu.ReplaceSM;
import org.smpp.pdu.Response;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.UnbindResp;
import org.smpp.pdu.ValueNotSetException;
import org.smpp.pdu.Request;

import static com.nuevatel.common.util.Util.*;

/**
 * Handle all acction on SMPP client.
 * 
 * @author asalazar
 */
public class SmppClientManager {
    
    private static Logger logger = LogManager.getLogger(SmppClientManager.class);
    
    private static final int DISPATCH_EV_SOURCE_NOT_ALLOWED = 1;
    
    private static final int DISPATCH_EV_OK = 0;

    private static final int TIME_OUT_MC_EVENT_QUEUE = 500;

    /**
     * To receive sync response
     */
    private static final int RECIEVE_TIMEOUT = 20000;

    private static final long REQUEST_TIMEOUT_MS = 500;
    
    private static long defaultValidityPeriod;
    
    /**
     * All incoming events from the SMSC (SMPP server)
     */
    private BlockingQueue<SmppEvent>smppEvents;
    
    /**
     * All outgoing events. Messages to submit to remote SMSC (SMPP server)
     */
    private BlockingQueue<McIncomingEvent>mcEvents;
    
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
    
    private boolean bound = false;
    
    private Session smppSession = null;

    private AddressRange addressRange = new AddressRange();
    
    private DialogService dialogService = AllocatorService.getDialogService();;
    
    static {
        defaultValidityPeriod = LongUtil.tryParse(AllocatorService.getProperties().getProperty(PropName.defaultValidityPeriod.property()), 86400000L);
    }
    
    public SmppClientManager(SmppGwSession gwSession,
                             BlockingQueue<SmppEvent>smppEvents, 
                             BlockingQueue<McIncomingEvent>mcEvents) {
        Parameters.checkNull(gwSession, "gwSession");
        Parameters.checkNull(smppEvents, "smppEvents");
        Parameters.checkNull(mcEvents, "mcEvents");
        
        this.gwSession = gwSession;
        this.smppEvents = smppEvents;
        this.mcEvents = mcEvents;
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
        if (bound) {
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
        pduListener = new SmppClientEventListener(smppEvents, smppSession);
        // do bind and register listener in the session
        BindResponse response = smppSession.bind(request, pduListener);
        // Log response
        logger.info("Bind response: " + response.debugString());
        // Check if was succedded
        if (bound = Data.ESME_ROK == response.getCommandStatus()) {
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
        if (!bound) {
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
        bound = false;
    }
    
    /**
     * Receive smpp messages from remote SMSC, un till unbind. consume the PDU
     */
    public void receive() {
        logger.info("client.recieve...");
        try {
            while (bound) {
                try {
                    // receive till bound
                    SmppEvent smppEvent = smppEvents.poll(REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS); // 500ms
                    if (smppEvent == null) {
                        // time out
                        continue;
                    }
                    PDU pdu = smppEvent.getPduEvent().getPDU();
                    if (pdu != null) {
                        if (pdu.isRequest()) {
                            Response response = ((Request) pdu).getResponse();
                            // TODO respond with default
                            smppSession.respond(response);
                        }
                    }
                    // Find dialog to handle
                    Dialog dialog = dialogService.getDialog(smppEvent.getDialogId());
                    if (dialog == null) {
                        logger.warn("Not found dialog for dialogId:{}", smppEvent.getDialogId());
                        continue;
                    }
                    // handle event
                    dialog.handleSmppEvent(smppEvent);
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
    public int offerEvent(McIncomingEvent event) throws InterruptedException {
        if (mcEvents.offer(event, TIME_OUT_MC_EVENT_QUEUE, TimeUnit.MILLISECONDS)) {
            return DISPATCH_EV_OK;
        }
        return DISPATCH_EV_SOURCE_NOT_ALLOWED;
    }

    public void enquireLink() throws ValueNotSetException, // do enquireLink
                                      TimeoutException, // do enquireLink
                                      PDUException, // do enquireLink
                                      WrongSessionStateException, // do enquireLink
                                      IOException { // do enquireLink
        smppSession.enquireLink();
    }

    /**
     * Dispatch messages to remote SMSC (smpp seerver) till unbind.
     */
    public void dispatch() {
        try {
            while (bound) {
                McIncomingEvent mcEvent = mcEvents.poll(TIME_OUT_MC_EVENT_QUEUE, TimeUnit.MILLISECONDS);
                if (mcEvent == null) {
                    // timeout
                    continue;
                }
                // dispatch to remote SMSC
                dispatchEvent(mcEvent);
                logger.info("client.dispatch...");
            }
        } catch (Throwable ex) {
            logger.error("Fatal Error, on stop manager.dispatch()", ex);
        }
    }
    
    /**
     * Dispatch event to remote SMSC
     * 
     * @param mcEvent event to dispatch
     * @throws IOException 
     * @throws WrongSessionStateException 
     * @throws PDUException 
     * @throws TimeoutException 
     * @throws ValueNotSetException 
     */
    private void dispatchEvent(McIncomingEvent mcEvent) throws ValueNotSetException,
                                                       TimeoutException, 
                                                       PDUException, 
                                                       WrongSessionStateException, 
                                                       IOException {
        switch (mcEvent.type()) {
        case SubmitSmMcEvent:
            submitSm(castAs(SubmitSmMcIEvent.class, mcEvent));
            break;
        case DataSmMcEvent:
            dataSm(castAs(DataSmMcEvent.class, mcEvent));
            break;
        case CancelSmMcEvent:
            cancelSm(castAs(CancelSmMcEvent.class, mcEvent));
            break;
        case QuerySmMcEvent:
            querySm(castAs(QuerySmMcEvent.class, mcEvent));
            break;
        case ReplaceSmMcEvent:
            replaceSm(castAs(ReplaceSmMcEvent.class, mcEvent));
            break;
        default:
            logger.warn("Unknown McEvent...");
            break;
        }
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
     */
    private void submitSm(SubmitSmMcIEvent smMsg) throws ValueNotSetException, // do submit
                                                       TimeoutException, // do submit
                                                       PDUException, // do submit, service type, short message, schedule delivery time, validity period
                                                       WrongSessionStateException, // do submit
                                                       IOException { // do submit, short message
        SubmitSM request = new SubmitSM();
        // set input values.
        request.setServiceType(gwSession.getSystemType());
        request.setSourceAddr(smMsg.getSourceAddr());
        request.setDestAddr(smMsg.getDestAddr());
        request.setReplaceIfPresentFlag(smMsg.getReplaceIfPresentFlag());
        request.setShortMessage(smMsg.getShortMessage(), smMsg.getEncoding());
        request.setScheduleDeliveryTime(smMsg.getScheduleDeliveryTime());
        request.setValidityPeriod(smMsg.getValidityPeriod());
        request.setEsmClass(smMsg.getEsmClass());
        request.setProtocolId(smMsg.getProtocolId());
        request.setPriorityFlag(smMsg.getPriorityFlag());
        // Always assign sequence number
        assignSequenceNumber(request, smMsg.getMessageId());
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
    private void cancelSm(CancelSmMcEvent event) throws ValueNotSetException, // do cancel
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
    private void dataSm(DataSmMcEvent event) throws ValueNotSetException, // do data ms
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
    private void replaceSm(ReplaceSmMcEvent event) throws ValueNotSetException, // do replace
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
    
    private void querySm(QuerySmMcEvent event) throws ValueNotSetException, // do query
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
        
        private UniqueID uniqueID;
        
        private BlockingQueue<SmppEvent> serverEvents;
        
        private DialogService dialogService = AllocatorService.getDialogService();
        
        public SmppClientEventListener(BlockingQueue<SmppEvent> serverEvents,
                                       Session smppSession) {
            this.serverEvents = serverEvents;
            try {
                uniqueID = new UniqueID();
            } catch (NoSuchAlgorithmException e) {
                // No op. Used standard algorithms.
            }
        }
        
        @Override
        public void handleEvent(ServerPDUEvent pduEvent) {
            PDU pdu = pduEvent.getPDU();
        if (pdu.isRequest()) {
            try {
                if (pdu.isRequest()) {
                    // TODO Handle all request events
                    // Check by ... deliver cancel query enquire link
                    // Ofer event for the processor
                    Long dialogId =  dialogService.findDialogIdBySequenceNumber(pdu.getSequenceNumber());
                    if (dialogId == null) {
                        if (pdu.getCommandId() == Data.DELIVER_SM) {
                            DeliverSM deliverSM = (DeliverSM) pdu;
                            // TODO ask how to handle it
                            DeliverSmDialog deliverSmDialog = new DeliverSmDialog(uniqueID.nextLong());
                            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
                            ZonedDateTime zonedValidityPeriod = null;
                            if (!StringUtils.isEmptyOrNull(deliverSM.getValidityPeriod())
                                && now.isBefore(zonedValidityPeriod = SmppDateUtil.parseDateTime(now, deliverSM.getValidityPeriod()))) {
                                dialogService.putDialog(deliverSmDialog, zonedValidityPeriod.toInstant().toEpochMilli() - now.toInstant().toEpochMilli());
                            } else {
                                dialogService.putDialog(deliverSmDialog, defaultValidityPeriod);
                            }
                            deliverSmDialog.init();
                        } else {
                            // no action missing dialog id
                            logger.warn("Missing DialogId for smpp sequence number:{}", pdu.getSequenceNumber());
                        }
                    }
                    
                    if (!serverEvents.offer(new SmppEvent(dialogId, pduEvent), REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                        // warn the queue rejects the event
                        logger.warn("Failed to offer serverPDUEvent:{}", pdu.debugString());
                    }
                } else if (pdu.isResponse()) {
                    // warn response was recieved, the 
                    logger.warn("Response is recieved serverPDUEvent:{}", pdu.debugString());
                } else if (pdu.isGNack()) {
                    logger.warn("Generic NACK serverPDUEvent:{}", pdu.debugString());
                } else {
                    logger.warn("Unknown serverPDUEvent:{}", pdu.debugString());
                }
                
            } catch (InterruptedException ex) {
                logger.error("On handleServerPDUEvent", ex);
            }
        }
        }
    }
}
