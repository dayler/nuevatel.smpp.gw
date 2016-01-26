/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog.client;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Data;
import org.smpp.ServerPDUEvent;
import org.smpp.pdu.Address;
import org.smpp.pdu.PDU;
import org.smpp.pdu.Request;
import org.smpp.pdu.SubmitSMResp;

import com.nuevatel.common.appconn.AppMessages;
import com.nuevatel.common.appconn.Message;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.appconn.ForwardSmICall;
import com.nuevatel.mc.appconn.ForwardSmORetAsyncCall;
import com.nuevatel.mc.appconn.McMessage;
import com.nuevatel.mc.appconn.Name;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.Constants;
import com.nuevatel.mc.smpp.gw.SmppDateUtil;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.dialog.Dialog;
import com.nuevatel.mc.smpp.gw.dialog.DialogState;
import com.nuevatel.mc.smpp.gw.dialog.DialogType;
import com.nuevatel.mc.smpp.gw.event.DefaultResponseOKEvent;
import com.nuevatel.mc.smpp.gw.event.GenericNAckEvent;
import com.nuevatel.mc.smpp.gw.event.SubmitSmppEvent;
import com.nuevatel.mc.smpp.gw.util.EsmClass;
import com.nuevatel.mc.smpp.gw.util.TpDcsUtils;
import com.nuevatel.mc.tpdu.SmsStatusReport;
import com.nuevatel.mc.tpdu.SmsSubmit;
import com.nuevatel.mc.tpdu.TpAddress;
import com.nuevatel.mc.tpdu.Tpdu;

/**
 * 
 * <p>The EsmeSubmitSmDialog class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Handle outgoing message from local MC to remote SMSC.
 * <br/>
 * (1) Receive deliver_sm and create new SubmitSmDialog. Create SmsSubmit MC message, dispatch it to McDisdpatcher.<br/>
 * (1) Check if RDS is enabled.<br/>
 * (1) For RDS true await confirmation.<br/>
 * (1) For RDS true, receive FowardSmOCall(SmsStatusReport) confirmation message.<br/>
 * (1) Dispatch deliver response<br/>
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class EsmeSubmitSmDialog extends Dialog {
    
    private static Logger logger= LogManager.getLogger(EsmeSubmitSmDialog.class);
    
    /* Private variables */
    
    private SmppGwProcessor gwProcessor;
    
    private boolean registeredDelivery;
    
    @SuppressWarnings("unused")
    private long smMessageId;
    
    private Name fromName;
    
    private Name toName;
    
    private SmsSubmit smsSubmit;
    
    private long defaultValidityPeriod;
    
    private byte tpStatus = Tpdu.TP_ST_PERMANENT_ERROR;

    /**
     * Creates an instance of <code>EsmeSubmitSmDialog</code>.
     * 
     * @param messageId
     * @param gwProcessorId
     * @param processorId
     * @param smMessageId
     * @param registeredDelivery
     * @param fromName
     * @param toName
     * @param smsSubmit
     */
    public EsmeSubmitSmDialog(long messageId,
                          int gwProcessorId, // smppGwId
                          int processorId, // processorId
                          long smMessageId,
                          byte registeredDelivery,
                          Name fromName,
                          Name toName,
                          SmsSubmit smsSubmit) {
        super(messageId, gwProcessorId, processorId);
        
        Parameters.checkNull(fromName, "fromName");
        Parameters.checkNull(toName, "toName");
        Parameters.checkNull(smsSubmit, "smsDeliver");
        
        gwProcessor = AllocatorService.getSmppGwProcessor(gwProcessorId);
        this.registeredDelivery = (registeredDelivery & Data.SM_SMSC_RECEIPT_MASK) != Data.SM_SMSC_RECEIPT_NOT_REQUESTED; 
        this.smMessageId = smMessageId;
        this.fromName = fromName;
        this.toName = toName;
        defaultValidityPeriod = AllocatorService.getConfig().getDefaultValidityPeriod();
        this.smsSubmit = smsSubmit;
    }
    
    @Override
    public void init() {
        try {
            logger.info("Create Dialog. dialogId:{} rds:{}", dialogId, registeredDelivery);
            state = DialogState.forward;
            // Prepare SmppEvent
            SubmitSmppEvent event = new SubmitSmppEvent(dialogId);
            Address sourceAddr = new Address((byte)(fromName.getType() & TpAddress.TON), (byte)(fromName.getType() & TpAddress.NPI), fromName.getName());
            event.setSourceAddr(sourceAddr);
            Address destAddr = new Address((byte)(toName.getType() & TpAddress.TON), (byte)(toName.getType() & TpAddress.NPI), toName.getName());
            event.setDestAddr(destAddr);
            // replcae if present
            event.setReplaceIfPresentFlag((byte)Data.SM_REPLACE);
            event.setData(smsSubmit.getTpdu());
            // select charset
            event.setEncoding(TpDcsUtils.resolveSmppEncoding(smsSubmit.getTpDcs().getCharSet()));
            // serviceCentre timestamp
            event.setScheduleDeliveryTime(SmppDateUtil.toSmppDatetime(smsSubmit.getTpVp() == null ? LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(defaultValidityPeriod) : smsSubmit.getTpVp()));
            // use default validity period
            ZonedDateTime validityPeriod = defaultValidityPeriod > 0 ? ZonedDateTime.now(ZoneId.systemDefault()).plus(defaultValidityPeriod, ChronoUnit.MILLIS) : ZonedDateTime.now(ZoneId.of("UTC"));
            event.setValidityPeriod(SmppDateUtil.toSmppDatetime(validityPeriod));
            // esm_class
            // default message mode, default message type
            EsmClass esmClass = new EsmClass(false, smsSubmit.getTpUdhi(), smsSubmit.getTpRp());
            event.setEsmClass(esmClass.getValue());
            // Set default protocol id
            event.setProtocolId(Data.DFLT_PROTOCOLID);
            // registered delivery
            event.setRegisteredDelivery(registeredDelivery ? Data.SM_SMSC_RECEIPT_REQUESTED : Data.SM_SMSC_RECEIPT_NOT_REQUESTED);
            // offer smppevent
            state = DialogState.forward;
            gwProcessor.getSmppProcessor(processorId).offerSmppEvent(event);
            // awaiting by response
            state = DialogState.awaiting_0;
        } catch (Throwable ex) {
            state = DialogState.failed;
            logger.warn("Failed to initialize SubmitSmDialog.", ex);
            // nack
            commandStatusCode = Data.ESME_RSYSERR;
            // finalize dialog
            invalidate();
        }
    }
    
    @Override
    public void handleSmppEvent(ServerPDUEvent ev) {
        try {
            PDU pdu = ev.getPDU();
            if (pdu.isResponse()) {
                if (pdu.isGNack()) {
                    state = DialogState.failed;
                    commandStatusCode = pdu.getCommandStatus();
                    ForwardSmORetAsyncCall fwsmoRetCall = new ForwardSmORetAsyncCall(dialogId, AppMessages.FAILED, Constants.SM_O_FAILED);
                    logger.debug("ForwardSmORetAsyncCall messageId:{} ret:{} serviceMsg:{}", fwsmoRetCall.getMessageId(), fwsmoRetCall.getRet(), fwsmoRetCall.getServiceMsg());
                    // dispatch failed message
                    mcDispatcher.dispatch(fwsmoRetCall);
                    invalidate();
                } else if (pdu.isOk() && Data.SUBMIT_SM_RESP == pdu.getCommandId()) {
                    if (registeredDelivery) {
                        // received response, go forward
                        state = DialogState.forward;
                        // dispatch ForwardSmORetAsyncCall
                        commandStatusCode = Data.ESME_ROK;
                        // register smpp message id
                        dialogService.registerMessageId(((SubmitSMResp)pdu).getMessageId(), dialogId);
                        ForwardSmORetAsyncCall fwsmoRetCall = new ForwardSmORetAsyncCall(dialogId, AppMessages.ACCEPTED, Constants.NO_SERVICE_MSG);
                        logger.debug("ForwardSmORetAsyncCall messageId:{} ret:{} serviceMsg:{}", fwsmoRetCall.getMessageId(), fwsmoRetCall.getRet(), fwsmoRetCall.getServiceMsg());
                        // dispatch async message to confirm delivery
                        mcDispatcher.dispatch(fwsmoRetCall);
                        // awaiting confirmation delivery
                        state = DialogState.awaiting_1;
                    } else {
                        // if register delivery is not true, finish transaction,
                        // no await by response.
                        state = DialogState.close;
                        tpStatus = Tpdu.TP_ST_SM_RECEIVED_BY_SME;
                        commandStatusCode = Data.ESME_ROK;
                        ForwardSmORetAsyncCall fwsmoRetCall = new ForwardSmORetAsyncCall(dialogId, AppMessages.ACCEPTED, Constants.NO_SERVICE_MSG);
                        logger.debug("ForwardSmORetAsyncCall messageId:{} ret:{} serviceMsg:{}", fwsmoRetCall.getMessageId(), fwsmoRetCall.getRet(), fwsmoRetCall.getServiceMsg());
                        // dispatch async message to confirm delivery
                        mcDispatcher.dispatch(fwsmoRetCall);
                        invalidate();
                    }
                } else {
                    // pdu is not ok
                    state = DialogState.failed;
                    tpStatus = Tpdu.TP_ST_ERROR_IN_SME;
                    // Notify to mc failed
                    commandStatusCode = pdu.getCommandStatus();
                    ForwardSmORetAsyncCall fwsmoRetCall = new ForwardSmORetAsyncCall(dialogId, AppMessages.FAILED, Constants.SM_O_FAILED);
                    logger.debug("ForwardSmORetAsyncCall messageId:{} ret:{} serviceMsg:{}", fwsmoRetCall.getMessageId(), fwsmoRetCall.getRet(), fwsmoRetCall.getServiceMsg());
                    // dispatch async message to confirm delivery
                    mcDispatcher.dispatch(fwsmoRetCall);
                    invalidate();
                }
            } else if (pdu.isRequest()) {
                // handle Report delivery response
                if (pdu.getCommandId() == Data.DELIVER_SM) {
                    // forward resp and confirmation delivery to mc
                    state = DialogState.forward;
                    // only on register_delivery
                    gwProcessor.getSmppProcessor(processorId).offerSmppEvent(new DefaultResponseOKEvent((Request)pdu));
                    commandStatusCode = Data.ESME_ROK;
                    // Close dialog, no more steps
                    state = DialogState.close;
                    tpStatus = Tpdu.TP_ST_SM_RECEIVED_BY_SME;
                    invalidate();
                }
                // ignore in other case.
            }
        } catch (Throwable ex) {
            logger.error("Failed to handle ServerPDUEvent:{}", ev.getPDU() != null ? null : ev.getPDU().debugString(), ex);
            commandStatusCode = Data.ESME_RSYSERR;
            state = DialogState.failed;
            invalidate();
        }
    }
    
    @Override
    public void handleMcMessage(McMessage msg) {
        // No op
    }
    
    @Override
    public DialogType getType() {
        return DialogType.esme_submit;
    }
    
    @Override
    public void execute() {
        logger.info("Execute Dialog. dialogId:{} rds:{} state:{}", dialogId, registeredDelivery, state);
        try {
            if (!DialogState.close.equals(state)) {
                if (DialogState.forward.equals(state)) {
                    // No in close estate means an error occurred in the work
                    // flow.
                    gwProcessor.getSmppProcessor(processorId).offerSmppEvent(new GenericNAckEvent(getCurrentSequenceNumber(), commandStatusCode == Data.ESME_ROK ? Data.ESME_RSYSERR : commandStatusCode));
                }
                else if (DialogState.awaiting_0.equals(state)) {
                    // submit_sm_resp was not received
                    commandStatusCode = Data.ESME_RSYSERR;
                    ForwardSmORetAsyncCall fwsmoRetCall = new ForwardSmORetAsyncCall(dialogId, AppMessages.FAILED, commandStatusCode);
                    logger.debug("ForwardSmORetAsyncCall messageId:{} ret:{} serviceMsg:{}", fwsmoRetCall.getMessageId(), fwsmoRetCall.getRet(), fwsmoRetCall.getServiceMsg());
                    mcDispatcher.dispatch(fwsmoRetCall);
                }
                else if (DialogState.awaiting_1.equals(state)) {
                    // delivery_sm confirmation never received
                }
                else {
                    // No op
                }
            }
            // No register delivery
            if (!registeredDelivery) return;
            TpAddress recipientAddress = new TpAddress((byte) (toName.getType() & TpAddress.TON), (byte) (toName.getType() & TpAddress.NPI), toName.getName());
            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            // SmsStatusReport
            // boolean tpUdhi, boolean tpMms, boolean tpSrq, byte tpMr, TpAddress tpRa, LocalDateTime tpScts, LocalDateTime tpDt, byte tpSt
            SmsStatusReport smsSr = new SmsStatusReport(false, false, false, (byte) 0, recipientAddress/* dest addr */, now, now, tpStatus);
            ForwardSmICall fwsmiCall = new ForwardSmICall(dialogId, smsSr.getTpdu());
            logger.debug("ForwardSmICall messageId:{} tpdu:{}", fwsmiCall.getMessageId(), fwsmiCall.getTpdu());
            Message msg = mcDispatcher.dispatchAndWait(fwsmiCall);
            // failed
            if (msg.getByte(AppMessages.RET_IE) == AppMessages.FAILED) logger.warn("Failed to dispatch confirmation delivery to MC. messageId", dialogId);
        } catch (Throwable ex) {
            logger.error("Failed to execute SubmitSmDialog...", ex);
        }
    }
    
    @Override
    protected void invalidate() {
        logger.info("Invalidate Dialog. dialogId:{} rds:{}", dialogId, registeredDelivery);
        super.invalidate();
    }
}
