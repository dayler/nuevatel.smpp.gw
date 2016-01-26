
package com.nuevatel.mc.smpp.gw.server;

import static com.nuevatel.common.util.Util.*;

import java.io.IOException;
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
import com.nuevatel.mc.smpp.gw.Constants;
import com.nuevatel.mc.smpp.gw.SmppProcessor;
import com.nuevatel.mc.smpp.gw.ThrotlleCounter;
import com.nuevatel.mc.smpp.gw.dialog.Dialog;
import com.nuevatel.mc.smpp.gw.dialog.server.SmscSubmitSmDialog;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.DefaultResponseOKEvent;
import com.nuevatel.mc.smpp.gw.event.DeliverSmEvent;
import com.nuevatel.mc.smpp.gw.event.GenericNAckEvent;
import com.nuevatel.mc.smpp.gw.event.GenericResponseEvent;
import com.nuevatel.mc.smpp.gw.event.SmppEvent;

/**
 * 
 * <p>The SmppServerProcessor class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Handle server smpp logic.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class SmppServerProcessor extends SmppProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppServerGwProcessor.class);
    
    /* Private variables */
    
    private Connection conn;
    
    private boolean receiving = false;
    
    private Receiver receiver;
    
    private Transmitter transmitter;
    
    private boolean bound = false;
    
    private Delegate onShutdownDelegate = null;
    
    private Delegate onBindDelegate = null;
    
    private ThrotlleCounter throtlleCounter;
    
    private boolean authorizedBind = true;
    
    /**
     * SmppServerProcessor constructor.
     * @param gwSession
     * @param conn
     * @param throtlleCounter
     */
    public SmppServerProcessor(SmppGwSession gwSession,
                               Connection conn,
                               ThrotlleCounter throtlleCounter) {
        super(gwSession);
        
        Parameters.checkNull(gwSession, "gwSession");
        Parameters.checkNull(conn, "conn");
        Parameters.checkNull(throtlleCounter, "throtlleCounter");
        
        this.gwSession = gwSession;
        this.conn = conn;
        this.throtlleCounter = throtlleCounter;
        transmitter = new Transmitter(conn);
        receiver = new Receiver(transmitter, conn);
        // set handler
        receiver.setServerPDUEventListener((event)->handleServerPDUEvent(event));
    }
    
    @Override
    public boolean isBound() {
        return bound;
    }
    
    /**
     * Checks identity, and checks if bind operation can be done.
     * @param request
     * @return
     */
    private int checkBindRequest(BindRequest request) {
        // verify if limit of binds was reached.
        if (!authorizedBind) {
            // External condition, unauthorized bind operation.
            return Data.ESME_RBINDFAIL;
        }
        // verify identity
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
    
    /**
     * Handle Bind Request.
     * @param request
     * @throws WrongLengthOfStringException
     * @throws InterruptedException
     */
    private void handleBindRequest(Request request) throws WrongLengthOfStringException, InterruptedException {
        int commandId = request.getCommandId();
        if (Data.BIND_TRANSMITTER == commandId
            || Data.BIND_RECEIVER == commandId
            || Data.BIND_TRANSCEIVER == commandId) {
            // do bind
            int cmdSt = checkBindRequest((BindRequest) request);
            if (Data.ESME_ROK == cmdSt) {
                // Successful
                BindResponse bindResponse = (BindResponse) request.getResponse();
                bindResponse.setSystemId(gwSession.getSystemId());
                GenericResponseEvent gresp = new GenericResponseEvent(bindResponse);
                offerSmppEvent(gresp);
                // enable bound
                bound = true;
                // execute bind delegate to notify bind successful
                if (onBindDelegate != null) onBindDelegate.execute();
                logger.info("Bind succeded for systemId:{} systemType:{} bindType:{}", gwSession.getSystemId(), gwSession.getSystemType(), gwSession.getBindType());
            }
            else {
                // failed to authenticated, or bind operation cannot be performed.
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
            }
            else {
                //  cannot response
            }
        }
    }
    
    @Override
    public void receive() {
        // Take <code>ServerPDUEvent</code>, select its dialog (if no one create it), dispatch it to handle by its dialog.
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
                        }
                        else {
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
                            logger.trace("Found dialog for dialogId:{}", dialogId);
                            dialog.handleSmppEvent(smppEvent);
                        }
                        else { 
                            if (pdu.isRequest() && Data.SUBMIT_SM == pdu.getCommandId()) {
                                if (throtlleCounter.exceededLimit(gwSession.getThrottleLimit())) {
                                    // if limit is exceeded, reject request
                                    GenericResponseEvent resp = new GenericResponseEvent(((Request) pdu).getResponse(), Data.ESME_RTHROTTLED);
                                    offerSmppEvent(resp);
                                    continue;
                                }
                                // Check regexp
                                if (!checkSourceAddr(((SubmitSM)pdu).getSourceAddr().getAddress())) {
                                    // if not match with regexp
                                    GenericResponseEvent resp = new GenericResponseEvent(((Request) pdu).getResponse(), Data.ESME_RINVSRCADR);
                                    offerSmppEvent(resp);
                                    continue;
                                }
                                // Continue
                                handleSubmitSmRequest(smppEvent);
                                // Accepted new request
                                throtlleCounter.inc();
                            }
                            else if (pdu.isRequest()
                                       && (Data.BIND_TRANSMITTER == pdu.getCommandId()
                                       || Data.BIND_RECEIVER == pdu.getCommandId()
                                       || Data.BIND_TRANSCEIVER == pdu.getCommandId())) {
                                    // already bind
                                    logger.warn("Already bind. system id:{} smppGwId{} smppSessionId:{}", gwSession.getSystemId(), gwSession.getSmppGwId(), gwSession.getSmppSessionId());
                                    GenericResponseEvent resp = new GenericResponseEvent(((Request)pdu).getResponse(), Data.ESME_RALYBND);
                                    offerSmppEvent(resp);
                            }
                            else if (pdu.isRequest() && Data.UNBIND == pdu.getCommandId()) {
                                // unbind
                                handleUnbindRequest(pdu);
                            }
                            else {
                                // unknow / unsupported smpp message
                                if (pdu.isRequest()) {
                                    // ret nack
                                    GenericNAckEvent nackEv = new GenericNAckEvent(pdu.getSequenceNumber(), Data.ESME_RINVCMDID);
                                    // Schedule to dispatch
                                    offerSmppEvent(nackEv);
                                }
                                else {
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
    
    /**
     * Handle Un-Bind pdu Request.
     * @param pdu
     * @throws InterruptedException
     */
    private void handleUnbindRequest(PDU pdu) throws InterruptedException {
        logger.info("Unbind. system id:{} smppGwId{} smppSessionId:{}", gwSession.getSystemId(), gwSession.getSmppGwId(), gwSession.getSmppSessionId());
        DefaultResponseOKEvent rok = new DefaultResponseOKEvent((Request)pdu);
        offerSmppEvent(rok);
        shutdown();
    }
    
    /**
     * Handle SubmitSm pdu Request.
     * @param smppEv
     */
    private void handleSubmitSmRequest(ServerPDUEvent smppEv) {
        // Create new Dialog
        // SmppSessionId is the processor identifier.
        Dialog submitDialog = new SmscSubmitSmDialog(gwSession.getSmppSessionId(), // Id to identify the processor
                                                     getSmppProcessorId());
        // Initialize dialog
        submitDialog.init();
        // handle event
        submitDialog.handleSmppEvent(smppEv);
        logger.trace("Not found SmscSubmitSmDialog. Created new dialog for dialogId:{}", submitDialog.getDialogId());
    }
    
    /**
     * Handle EnquireLink pdu Request.
     * @param pdu
     * @throws InterruptedException
     */
    private void handleEnquireLinkRequest(PDU pdu) throws InterruptedException {
        DefaultResponseOKEvent rok = new DefaultResponseOKEvent((Request)pdu);
        offerSmppEvent(rok);
    }
    
    /**
     * Set the delegate to execute when is shutting down the processor.
     * @param onShutdownDelegate
     */
    public void setOnShutdownDelegate(Delegate onShutdownDelegate) {
        this.onShutdownDelegate = onShutdownDelegate;
    }
    
    /**
     * Set delegate to execute when bind request was succeeded.
     * @param onBindDelegate
     */
    public void setOnBindDelegate(Delegate onBindDelegate) {
        this.onBindDelegate = onBindDelegate;
    }
    
    @Override
    public void shutdown() {
        // Action to execute before to shutdown.
        if (onShutdownDelegate != null) onShutdownDelegate.execute();
        // stop all services
        receiver.stop();
        setReceiving(false);
        bound = false;
        try {
            if (conn.isOpened()) conn.close();
        } catch (IOException ex) {
            logger.warn("smppGwId{}: Failed to close connection....", gwSession.getSmppGwId());
        }
    }
    
    @Override
    public void dispatch() {
        // Handle outgoing smpp messages. Consume from  <b>smppEvents</b>
        logger.info("server.dispatch...");
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
                    // timeout
                    if (smppEvent == null) continue;
                    // dispatch to esme
                    dispatchSmppEvent(smppEvent);
                    logger.trace("server.dispatch[{}]...", smppEvent.toString());
                } catch (PDUException | NotEnoughDataInByteBufferException | TerminatingZeroNotFoundException | IOException ex) {
                    logger.warn("Failed to dispatch smppEvent:{}", smppEvent == null ? null : smppEvent.toString());
                    if (logger.isTraceEnabled()) {
                        logger.warn("Exception:", ex);
                    }
                    shutdown();
                }
            }
            logger.info("End server.dispatch...");
        } catch (Throwable ex) {
            logger.fatal("Fatal error, stop manager.dispatch()", ex);
        }
    }
    
    /**
     * Dispatch Smpp Event. Get Pdu from <code>SmppEvent</code> and dispatch it to ESME.
     * @param event
     * @throws PDUException
     * @throws NotEnoughDataInByteBufferException
     * @throws TerminatingZeroNotFoundException
     * @throws IOException
     * @throws WrongSessionStateException
     */
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
    
    /**
     * Generic ROK response.
     * @param event
     * @throws ValueNotSetException
     * @throws WrongSessionStateException
     * @throws IOException
     */
    private void genericROkResponse(GenericResponseEvent event) throws ValueNotSetException, WrongSessionStateException, IOException {
        Parameters.checkNull(event, "event");
        transmitter.send(event.getResponse());
    }
    
    /**
     * Dispatch generic NACK event.
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
     * Default ROK response.
     * @param smResp Event to contains default response for smpp event.
     * @throws IOException 
     * @throws WrongSessionStateException 
     * @throws ValueNotSetException 
     */
    private void defaultROkResponse(DefaultResponseOKEvent smResp) throws ValueNotSetException, WrongSessionStateException, IOException {
        Parameters.checkNull(smResp, "smResp");
        transmitter.send(smResp.getDefaultResponse());
    }
    
    /**
     * From <code>DeliverSmEvent</code> create and dispatch <code>DeliverSm</code> pdu.
     * @param event
     * @throws PDUException
     * @throws NotEnoughDataInByteBufferException
     * @throws TerminatingZeroNotFoundException
     * @throws IOException
     */
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
     * Set receiving.
     * @param receiving
     */
    private synchronized void setReceiving(boolean receiving) {
        this.receiving = receiving;
        // wake up all threads
        notifyAll();
    }
    
    /**
     * <code>true</code> if processor is receiving.
     * @return
     */
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
     * <code>true</code> if connection is up.
     * @return
     */
    public boolean isConnected() {
        return conn == null ? false : conn.isOpened();
    }
    
    /**
     * <code>true</code> if bind operation is authorized for the processor.
     * @return
     */
    public boolean isAuthorizedBind() {
        return authorizedBind;
    }
    
    /**
     * Set authorizedBind.
     * @param authorizedBind
     */
    public void setAuthorizedBind(boolean authorizedBind) {
        this.authorizedBind = authorizedBind;
    }
}
