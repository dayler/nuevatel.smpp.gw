/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog.server;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Data;
import org.smpp.ServerPDUEvent;
import org.smpp.pdu.Address;
import org.smpp.pdu.PDU;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.SubmitSMResp;

import com.nuevatel.common.appconn.AppMessages;
import com.nuevatel.common.appconn.Message;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.appconn.ForwardSmICall;
import com.nuevatel.mc.appconn.ForwardSmOCall;
import com.nuevatel.mc.appconn.ForwardSmORetAsyncCall;
import com.nuevatel.mc.appconn.McMessage;
import com.nuevatel.mc.appconn.Name;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppDateUtil;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.dialog.Dialog;
import com.nuevatel.mc.smpp.gw.dialog.DialogState;
import com.nuevatel.mc.smpp.gw.dialog.DialogType;
import com.nuevatel.mc.smpp.gw.event.DefaultResponseOKEvent;
import com.nuevatel.mc.smpp.gw.event.DeliverSmEvent;
import com.nuevatel.mc.smpp.gw.util.EsmClass;
import com.nuevatel.mc.smpp.gw.util.TpStatusResolver;
import com.nuevatel.mc.tpdu.SmsStatusReport;
import com.nuevatel.mc.tpdu.SmsSubmit;
import com.nuevatel.mc.tpdu.TpAddress;

/**
 * @author Ariel Salazar
 *
 */
public class SubmitSmDialog extends Dialog {
    
    private static Logger logger = LogManager.getLogger(SubmitSmDialog.class);
    
    private SubmitSM submitSm = null;
    
    private boolean registeredDelivery;
    
    private SmppGwProcessor gwProcessor;
    
    private byte ret = AppMessages.FAILED;

    private String msgId = "";

    public SubmitSmDialog(long dialogId, int processorId) {
        super(dialogId, processorId);
        Parameters.checkNull(submitSm, "submitSm");
        // select processor
        gwProcessor = AllocatorService.getSmppGwProcessor(processorId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        // init
        state = DialogState.init;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleSmppEvent(ServerPDUEvent ev) {
        PDU pdu = ev.getPDU();
        commandStatusCode = pdu.getCommandStatus();
        try {
            if (pdu.isRequest() && pdu.getCommandId() == Data.SUBMIT_SM) {
                // sumbitSm
                state = DialogState.forward;
                // no ret
                handleSubmitSm((SubmitSM) pdu);
            } else if (pdu.isResponse() && pdu.isOk() && pdu.getCommandId() == Data.DELIVER_SM_RESP) {
                // deliverSm resp
                // no ret
                handleDeliverSmResp(pdu);
            } else {
                // failed
                state = DialogState.failed;
                ret = AppMessages.FAILED;
                commandStatusCode = pdu.getCommandStatus();
                // invalidate
                invalidate();
            }
        } catch (Exception ex) {
            state = DialogState.failed;
            ret = AppMessages.FAILED;
            logger.warn("Failed on handle SmppEvent...", ex);
        }
    }
    
    private void handleDeliverSmResp(PDU pdu) {
        commandStatusCode = pdu.getCommandStatus();
        if (pdu.isResponse() && pdu.isOk() && Data.DELIVER_SM_RESP == pdu.getCommandId()) {
            ret = AppMessages.ACCEPTED;
            state = DialogState.close;
        }
        // invalidate. end transaction to receive deliver_sm_resp
        invalidate();
    }
    
    private void handleSubmitSm(SubmitSM submitSmPdu) throws Exception {
        // SmsSubmit
        SmsSubmit smsSubmit = new SmsSubmit(submitSmPdu.getShortMessageData().getBuffer(), LocalDateTime.now(ZoneId.systemDefault()));
        // send ForwardSmICall
        ForwardSmICall fwsmiCall = new ForwardSmICall(// smppServiceType
                                                      gwProcessor.getSmppGwSession().getSystemType(),
                                                      // smppScheduleDeliveryTime
                                                      StringUtils.isEmptyOrNull(submitSmPdu.getScheduleDeliveryTime()) ? null : SmppDateUtil.parseDateTime(ZonedDateTime.now(ZoneId.systemDefault()), submitSmPdu.getScheduleDeliveryTime()),
                                                      // smppReplaceIfPresentFlag
                                                      submitSmPdu.getReplaceIfPresentFlag(),
                                                      // smppGwId
                                                      gwProcessor.getSmppGwSession().getSmppGwId(),
                                                      // smppSessionId
                                                      gwProcessor.getSmppGwSession().getSmppSessionId(),
                                                      // fromName
                                                      new Name(submitSmPdu.getSourceAddr().getAddress(), (byte)(submitSmPdu.getSourceAddr().getTon() | submitSmPdu.getSourceAddr().getNpi())),
                                                      // tpdu
                                                      smsSubmit.getTpdu());
        // dispatch sync message
        Message ret = mcDispatcher.dispatchAndWait(fwsmiCall);
        if (ret == null || ret.getByte(AppMessages.RET_IE) == AppMessages.FAILED) {
            // Failed
            state = DialogState.failed;
            commandStatusCode = Data.ESME_RSYSERR;
            invalidate();
            return;
        }
        // if OK send sumbit_sm_resp
        DefaultResponseOKEvent rok = new DefaultResponseOKEvent(submitSmPdu);
        msgId = ((SubmitSMResp)rok.getResponse()).getMessageId();
        gwProcessor.offerSmppEvent(rok);
        state = DialogState.forward;
        // If registered delivery, close transaction
        if (registeredDelivery) {
            state = DialogState.close;
            commandStatusCode = Data.ESME_ROK;
            this.ret = AppMessages.ACCEPTED;
            // invalidate to end transaction
            invalidate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleMcMessage(Message msg) {
        if (msg == null || msg.getCode() != McMessage.FORWARD_SM_O_CALL) {
            // log warning
            logger.warn("Message is null or not FORWARD_SM_O_CALL code:{}...", msg == null ? null : msg.getCode());
        }
        try {
            // create fwsmo call
            ForwardSmOCall fwsmoCall = new ForwardSmOCall(msg);
            // get smsStatusRep
            SmsStatusReport smsSr = new SmsStatusReport(fwsmoCall.getTpdu());
            EsmClass esmClass = new EsmClass(true, smsSr.getTpUdhi(), false);
            // disaptch deliver_sm confirmation
            DeliverSmEvent deliverSmEv = new DeliverSmEvent(dialogId);
            deliverSmEv.setEsmClass(esmClass.getValue());
            Address sourceAddr = new Address((byte)(fwsmoCall.getFromName().getType() | TpAddress.TON), (byte)(fwsmoCall.getFromName().getType() | TpAddress.NPI), fwsmoCall.getFromName().getName());
            deliverSmEv.setSourceAddr(sourceAddr);
            Address destAddr = new Address(smsSr.getTpRa().getTon(), smsSr.getTpRa().getNpi(), smsSr.getTpRa().getAddress());
            deliverSmEv.setDestAddr(destAddr);
            deliverSmEv.setReceiptedMessageId(msgId);
            // tpSt to smpp command status
            int smppStatus = TpStatusResolver.resolveSmppCommandStatus(smsSr.getTpSt());
            deliverSmEv.setCommandStatus(smppStatus);
            // dispatch deliver_sm confirmation
            gwProcessor.offerSmppEvent(deliverSmEv);
            state = DialogState.awaiting_0;
        } catch (Throwable ex) {
            logger.warn("Failed to handle McMessage...", ex);
            state = DialogState.failed;
            commandStatusCode = Data.ESME_RSYSERR;
            invalidate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        if (registeredDelivery) {
            // notify ForwardSmORetasync
            ForwardSmORetAsyncCall fwsmoRet = new ForwardSmORetAsyncCall(dialogId, ret, this.commandStatusCode);
            mcDispatcher.dispatch(fwsmoRet);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DialogType getType() {
        return DialogType.smsc_submit;
    }
}
