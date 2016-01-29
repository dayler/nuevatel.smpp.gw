
package com.nuevatel.mc.smpp.gw.client;

import com.nuevatel.common.util.Parameters;
import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.smpp.gw.Constants;
import com.nuevatel.mc.smpp.gw.SmppProcessor;
import com.nuevatel.mc.smpp.gw.ThrotlleCounter;
import com.nuevatel.mc.smpp.gw.dialog.Dialog;
import com.nuevatel.mc.smpp.gw.dialog.client.EsmeDeliverSmDialog;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.CancelSmEvent;
import com.nuevatel.mc.smpp.gw.event.DataSmEvent;
import com.nuevatel.mc.smpp.gw.event.DefaultResponseOKEvent;
import com.nuevatel.mc.smpp.gw.event.GenericNAckEvent;
import com.nuevatel.mc.smpp.gw.event.GenericResponseEvent;
import com.nuevatel.mc.smpp.gw.event.QuerySmEvent;
import com.nuevatel.mc.smpp.gw.event.ReplaceSmEvent;
import com.nuevatel.mc.smpp.gw.event.SmppEvent;
import com.nuevatel.mc.smpp.gw.event.SubmitSmppEvent;
import com.nuevatel.mc.smpp.gw.exception.FailedBindOperationException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Data;
import org.smpp.ServerPDUEvent;
import org.smpp.Session;
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
import org.smpp.pdu.Request;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.UnbindResp;
import org.smpp.pdu.ValueNotSetException;
import org.smpp.util.ByteBuffer;
import org.smpp.util.NotEnoughDataInByteBufferException;
import org.smpp.util.TerminatingZeroNotFoundException;

import static com.nuevatel.common.util.Util.*;

/**
 * 
 * <p>The SmppClientProcessor class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Implements logic for the SMPP client (single bind).
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class SmppClientProcessor extends SmppProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppClientProcessor.class);
    
    /* Private variables */
    
    /**
     * Smpp session between client and external SMSC
     */
    private Session smppSession = null;

    private AddressRange addressRange = new AddressRange();
    
    private boolean running = false;
    
    private ThrotlleCounter throtlleCounter; 
    
    /**
     * Creates an instance of <code>SmppClientProcessor</code> with <code>SmppGwSession</code> and <code>ThrotlleCounter</code> service.
     * @param gwSession
     * @param throtlleCounter
     */
    public SmppClientProcessor(SmppGwSession gwSession,
                               ThrotlleCounter throtlleCounter) {
        super(gwSession);
        
        Parameters.checkNull(gwSession, "gwSession");
        
        this.gwSession = gwSession;
        this.throtlleCounter = throtlleCounter;
    }
    
    /**
     * <code>true</code> if client is bound.
     * @return 
     */
    @Override
    public boolean isBound() {
        return smppSession == null ? false : (smppSession.isBound() && smppSession.getConnection().isOpened());
    }
    
    /**
     * 
     * Do bind operation, between ESME entity and SMSC (SMPP server).
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
        // Start processor thread
        if (!running) running = true;
        // already bound
        if (isBound()) logger.warn("Already bound, unbind first. SmppSessionId=" + gwSession.getSmppSessionId());
        // Do bind op
        BindRequest request = getBindRequest(gwSession.getBindType());
        TCPIPConnection conn = new TCPIPConnection(gwSession.getSmscAddress(), gwSession.getSmscPort());
        conn.setReceiveTimeout(Constants.TCPIP_CONN_RECIEVE_TIMEOUT);
        smppSession = new Session(conn);
        // set up request
        request.setSystemId(gwSession.getSystemId());
        request.setPassword(gwSession.getPassword());
        request.setSystemType(gwSession.getSystemType());
        request.setInterfaceVersion((byte)0x34);
        request.setAddressRange(addressRange);
        // do bind and register listener in the session
        BindResponse response = smppSession.bind(request, (pduEvent) -> handleServerPDUEvent(pduEvent));
        // Log response
        logger.info("Bind response: " + response.debugString());
        // Check if was succedded
        if (isBound()) logger.info("bind succedded");
        else {
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
        if (smppSession.getReceiver().isReceiver()) logger.warn("It can take a while to stop the receiver. SmppSessionId=" + gwSession.getSmppSessionId());
        UnbindResp response = null;
        try {
            response = smppSession.unbind();
        } catch (TimeoutException | PDUException | WrongSessionStateException | IOException ex) {
            logger.warn("Failed unbind.", ex);
        }
        logger.info("Unbind response: " + "Unbind response: " + response == null? null : response.debugString());
    }
    
    @Override
    public void receive() {
        logger.info("client.recieve...");
        try {
            while (isRunning()) {
                try {
                    if (!isBound()) {
                        if (logger.isTraceEnabled()) logger.trace("manager.receive()  Await until bound...");
                        // Await until bound
                        Thread.sleep(500L);
                        continue;
                    }
                    // receive till bound
                    ServerPDUEvent smppEvent = serverPduEvents.poll(Constants.TIMEOUT_POLL_EVENT_QUEUE, TimeUnit.MILLISECONDS); // 500ms
                    if (smppEvent == null) {
                        // time out
                        logger.trace("No events to process for smppGwId:{}", gwSession.getSmppGwId());
                        continue;
                    }
                    
                    PDU pdu = smppEvent.getPDU();
                    if (pdu != null) {
                        // Find dialog to dispatch message to handle it, if it does not exists create new dialog for incoming messages.
                        Dialog dialog = null;
                        Long dialogId = findDialogId(pdu);
                        if (dialogId != null && (dialog = dialogService.getDialog(dialogId)) != null) {
                            // Handle event by dialog
                            logger.trace("Found dialog for dialogId:{}", dialogId);
                            dialog.handleSmppEvent(smppEvent);
                        }
                        else {
                            // validate throttle limit
                            if (Data.DELIVER_SM == pdu.getCommandId()) {
                                if (throtlleCounter.exceededLimit(gwSession.getThrottleLimit())) {
                                    // if limit is exceeded, reject request
                                    GenericResponseEvent resp = new GenericResponseEvent(((Request)pdu).getResponse(), Data.ESME_RTHROTTLED);
                                    offerSmppEvent(resp);
                                    continue;
                                }
                                // Check regexp
                                if (!checkSourceAddr(((DeliverSM)pdu).getSourceAddr().getAddress())) {
                                    // if not match with regexp
                                    GenericResponseEvent resp = new GenericResponseEvent(((Request) pdu).getResponse(), Data.ESME_RINVSRCADR);
                                    offerSmppEvent(resp);
                                    continue;
                                }
                                // Create dialog
                                // SmppSessionId is the processor identifier.
                                Dialog deliverSmDialog = new EsmeDeliverSmDialog(gwSession.getSmppSessionId(),  // Id to identify the processor
                                                                                getSmppProcessorId());
                                // Initialize dialog
                                deliverSmDialog.init();
                                deliverSmDialog.handleSmppEvent(smppEvent);
                                logger.trace("Not found EsmeDeliverSmDialog. Created new dialog for dialogId:{}", deliverSmDialog.getDialogId());
                                // message was accepted to deliver
                                throtlleCounter.inc();
                            }
                            else {
                                // unknow / unsupported smpp message
                                if (pdu.isRequest()) {
                                    // if is a request return NACK
                                    GenericNAckEvent nackEv = new GenericNAckEvent(pdu.getSequenceNumber(), Data.ESME_RINVCMDID);
                                    // Schedule to dispatch.
                                    offerSmppEvent(nackEv);
                                }
                                else {
                                    // For a response ignore it. do nothing.
                                }
                            }
                        }
                    }
                } catch (InterruptedException | ValueNotSetException ex) {
                    logger.error("On recieving event, the processor:{} ...", gwSession == null ? null : gwSession.getSmppGwId(), ex);
                }
            }
        } catch (Throwable ex) {
            logger.fatal("On recieving event, the processor:{} is stoping ...", gwSession == null ? null : gwSession.getSmppGwId(), ex);
        }
    }
    
    /**
     * Find Dialog id to corresponds with pdu.
     * @param pdu
     * @return
     * @throws ValueNotSetException
     */
    private Long findDialogId(PDU pdu) throws ValueNotSetException {
        if (pdu.isRequest() && Data.DELIVER_SM == pdu.getCommandId()) {
            DeliverSM deliverSM = (DeliverSM) pdu;
            // check if deliver sm contains ack
            if ((deliverSM.getEsmClass() & Data.SM_SMSC_DLV_RCPT_TYPE) == Data.SM_SMSC_DLV_RCPT_TYPE) return dialogService.findDialogIdByMessageId(((DeliverSM)pdu).getReceiptedMessageId());
            // other case deliver will originate dialog
            return null;
        }
        else if (pdu.isResponse()) return dialogService.findDialogIdBySequenceNumber(pdu.getSequenceNumber());
        // dialog not found
        return null;
    }
    
    /**
     * Execute enquirelink.
     * @throws ValueNotSetException
     * @throws TimeoutException
     * @throws PDUException
     * @throws WrongSessionStateException
     */
    public void enquireLink() throws ValueNotSetException, // do enquireLink
                                     TimeoutException, // do enquireLink
                                     PDUException, // do enquireLink
                                     WrongSessionStateException { // do enquireLin
        try {
            if (isBound()) {
                smppSession.enquireLink();
                return;
            }
            logger.warn("smppGwId:{} is not bound...", gwSession.getSmppGwId());
        } catch (IOException ex) {
            logger.warn("Enquirelink failed...");
            if (logger.isTraceEnabled()) logger.warn("Exception:", ex);
        }
    }

    /**
     * Receive messages from local MC and dispatch it to remote SMSC
     */
    @Override
    public void dispatch() {
        try {
            SmppEvent smppEvent = null;
            while (isRunning()) {
                try {
                    if (!isBound()) {
                        if (logger.isTraceEnabled()) logger.trace("manager.dispatch() Await until bound...");
                        // Await until bound
                        Thread.sleep(500L);
                        continue;
                    }
                    // get scheduled event
                    smppEvent = smppEvents.poll(Constants.TIMEOUT_POLL_EVENT_QUEUE, TimeUnit.MILLISECONDS);
                    // timeout
                    if (smppEvent == null) continue;
                    // dispatch to remote SMSC
                    dispatchEvent(smppEvent);
                    logger.trace("client.dispatch[{}]...", smppEvent.toString());
                } catch (TimeoutException | PDUException | WrongSessionStateException | IOException | NotEnoughDataInByteBufferException | TerminatingZeroNotFoundException ex) {
                    logger.warn("Failed to dispatch smppEvent:{}", smppEvent == null ? null : smppEvent.toString());
                    if (logger.isTraceEnabled()) logger.warn("Exception:", ex);
                }
            }
        } catch (Throwable ex) {
            logger.error("Fatal Error, stop manager.dispatch()", ex);
        }
    }
    
    /**
     * <code>true</code> if processor is running.
     * @return
     */
    public boolean isRunning() {
        return running;
    }
    
    @Override
    public void shutdown() {
        running = false;
    }

    /**
     * Dispatch event to remote SMSC
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
        case GenericROkResponseEvent:
            genericROkResponse(castAs(GenericResponseEvent.class, smppEvent));
            break;
        case DefaultROkResponseEvent:
            defaultROkResponse(castAs(DefaultResponseOKEvent.class, smppEvent));
            break;
        case GenericNAckEvent:
            genericNAckResponse(castAs(GenericNAckEvent.class, smppEvent));
            break;
        default:
            logger.warn("Unknown SmppEvent...");
            break;
        }
    }

    /**
     * Send Generic ROk response.
     * @param event
     * @throws ValueNotSetException
     * @throws WrongSessionStateException
     * @throws IOException
     */
    private void genericROkResponse(GenericResponseEvent event) throws ValueNotSetException,
                                                                       WrongSessionStateException,
                                                                       IOException {
        Parameters.checkNull(event, "event");
        smppSession.respond(event.getResponse());
    }

    /**
     * Dispatch generic NACK event.
     * @param event
     * @throws ValueNotSetException
     * @throws WrongSessionStateException
     * @throws IOException
     */
    private void genericNAckResponse(GenericNAckEvent event) throws ValueNotSetException,
                                                                    WrongSessionStateException,
                                                                    IOException {
        Parameters.checkNull(event, "event");
        smppSession.respond(event.getGenericNack());
    }

    /**
     * Send default ROK.
     * @param smResp Event to contains default response for smpp event.
     * @throws IOException 
     * @throws WrongSessionStateException 
     * @throws ValueNotSetException 
     */
    private void defaultROkResponse(DefaultResponseOKEvent smResp) throws ValueNotSetException, 
                                                                          WrongSessionStateException,
                                                                          IOException {
        Parameters.checkNull(smResp, "smResp");
        smppSession.respond(smResp.getDefaultResponse());
    }

    /**
     * <code>BindRequest</code> based on <code>SmppGwSession.BIND_TYPE</code>.
     * @param type
     * @return 
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
     * Submit a single message from queue.
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
        if (StringUtils.isEmptyOrNull(event.getEncoding())) request.setShortMessageEncoding(event.getEncoding());

        request.setScheduleDeliveryTime(event.getScheduleDeliveryTime());
        request.setValidityPeriod(event.getValidityPeriod());
        request.setEsmClass(event.getEsmClass());
        request.setProtocolId(event.getProtocolId());
        request.setPriorityFlag(event.getPriorityFlag());
        // set report delivery status
        request.setRegisteredDelivery(event.getRegisteredDelivery());
        // Always assign sequence number
        assignSequenceNumber(request, event.getMessageId());
        // Asynchronous submit request, response is catch in the listener
        smppSession.submit(request);
    }

    /**
     * Cancel single short message
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
     * Dispatch ReplaceSm pdu.
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
    
    /**
     * Dispatch QuerySm pdu.
     * @param event
     * @throws ValueNotSetException
     * @throws TimeoutException
     * @throws PDUException
     * @throws WrongSessionStateException
     * @throws IOException
     */
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
}
