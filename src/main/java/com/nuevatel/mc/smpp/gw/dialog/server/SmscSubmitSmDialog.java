/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog.server;

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
import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.appconn.ForwardSmICall;
import com.nuevatel.mc.appconn.ForwardSmIRet;
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
import com.nuevatel.mc.smpp.gw.util.TpDcsUtils;
import com.nuevatel.mc.smpp.gw.util.TpStatusResolver;
import com.nuevatel.mc.smpp.gw.util.TpUdUtils;
import com.nuevatel.mc.tpdu.SmsStatusReport;
import com.nuevatel.mc.tpdu.SmsSubmit;
import com.nuevatel.mc.tpdu.TpAddress;
import com.nuevatel.mc.tpdu.TpDcs;
import com.nuevatel.mc.tpdu.TpUd;

/**
 * @author Ariel Salazar
 *
 */
public class SmscSubmitSmDialog extends Dialog {
    
    private static Logger logger = LogManager.getLogger(SmscSubmitSmDialog.class);
    
    private boolean registeredDelivery;
    
    private SmppGwProcessor gwProcessor;
    
    private byte ret = AppMessages.FAILED;

    private String smppMsgId = "";
    
    /**
     * Delivery registered message id. Used to confirm delivery request to AppConn server (MC). -1 indicates no set value.
     */
    private long fwsmoCallMsgId = -1;

    public SmscSubmitSmDialog(int processorId) {
        super(processorId);
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
        if (pdu.isResponse() && pdu.isOk()) {
            ret = AppMessages.ACCEPTED;
            state = DialogState.close;
        }
        // invalidate. end transaction to receive deliver_sm_resp
        invalidate();
    }
    
    private void handleSubmitSm(SubmitSM submitSmPdu) throws Exception {
        // resf time
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        // registered delivery
        registeredDelivery = (submitSmPdu.getRegisteredDelivery() & Data.SM_SMSC_RECEIPT_MASK) == Data.SM_SMSC_RECEIPT_REQUESTED;
        // SmsSubmit
        TpDcs tpDcs = TpDcsUtils.resolveTpDcs(submitSmPdu.getShortMessageEncoding()); // TpDcsUtils.resolveTpDcs("UTF-16BE");
        EsmClass esmClass = new EsmClass(submitSmPdu.getEsmClass());
        TpUd tpud = new TpUd(esmClass.getUdhi(), // tpUdhi
                             tpDcs, // tpDcs
                             TpUdUtils.resolveTpUdl(tpDcs.getCharSet(), submitSmPdu.getShortMessageData().getBuffer()), // tpUdl
                             TpUdUtils.fixTpUd(tpDcs.getCharSet(), submitSmPdu.getShortMessageData().getBuffer())); // tpUd
        SmsSubmit smsSubmit = new SmsSubmit(// tpRd
                                            true,
                                            // tpRp
                                            esmClass.getReplyPath(),
                                            // tpUdhi
                                            esmClass.getUdhi(),
                                            // tpSrr
                                            registeredDelivery,
                                            // tpMr
                                            (byte)0x0,
                                            // tpDa
                                            new TpAddress(submitSmPdu.getDestAddr().getTon(), submitSmPdu.getDestAddr().getNpi(), submitSmPdu.getDestAddr().getAddress()),
                                            // tpPi
                                            submitSmPdu.getProtocolId(),
                                            // tpDcs
                                            tpDcs,
                                            // tpVp
                                            SmppDateUtil.parseDateTime(now, submitSmPdu.getValidityPeriod()).toLocalDateTime(),
                                            // tpUdl
                                            tpud.getTpUdl(),
                                            // tpud
                                            tpud.getTpUd());
        // ForwardSmICall to MC
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
        if (ret == null) {
            // Failed
            state = DialogState.failed;
            commandStatusCode = Data.ESME_RSYSERR;
            invalidate();
            return;
        }
        ForwardSmIRet fwsmiRet = new ForwardSmIRet(ret);
        if (AppMessages.FAILED == fwsmiRet.getRet()) {
            // failed
            state = DialogState.failed;
            commandStatusCode = Data.ESME_RSYSERR;
            invalidate();
            return;
        }
        // set assigned message id
        setDialogId(fwsmiRet.getMessageId());
        // TODO debug
        System.out.println("dispatch fwsmiCall ok. messageId:" + fwsmiRet.getMessageId() + " rds=" + registeredDelivery);
        // if OK send sumbit_sm_resp
        DefaultResponseOKEvent rok = new DefaultResponseOKEvent(submitSmPdu);
        smppMsgId = ((SubmitSMResp)rok.getResponse()).getMessageId();
        gwProcessor.offerSmppEvent(rok);
        state = DialogState.forward;
        // If registered delivery is disable, close transaction
        if (!registeredDelivery) {
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
    public void handleMcMessage(McMessage msg) {
        // TODO debug
        System.out.println("+-+-+-+-+-+-+-+-+-+-+-");
        try {
            // create fwsmo call
            ForwardSmOCall fwsmoCall = (ForwardSmOCall) msg;
            // set delivery request confirmation id
            fwsmoCallMsgId = fwsmoCall.getMessageId();
            // get smsStatusRep
            SmsStatusReport smsSr = new SmsStatusReport(fwsmoCall.getTpdu());
            EsmClass esmClass = new EsmClass(true, smsSr.getTpUdhi(), false);
            // disaptch deliver_sm confirmation
            DeliverSmEvent deliverSmEv = new DeliverSmEvent(dialogId); // dialog id = message id
            deliverSmEv.setEsmClass(esmClass.getValue());
            Address sourceAddr = new Address((byte)(fwsmoCall.getFromName().getType() | TpAddress.TON), (byte)(fwsmoCall.getFromName().getType() | TpAddress.NPI), fwsmoCall.getFromName().getName());
            deliverSmEv.setSourceAddr(sourceAddr);
            Address destAddr = new Address(smsSr.getTpRa().getTon(), smsSr.getTpRa().getNpi(), smsSr.getTpRa().getAddress());
            deliverSmEv.setDestAddr(destAddr);
            deliverSmEv.setReceiptedMessageId(smppMsgId);
            // tpSt to smpp command status
            int smppStatus = TpStatusResolver.resolveSmppCommandStatus(smsSr.getTpSt());
            deliverSmEv.setCommandStatus(smppStatus);
            // set delivery Ack
            deliverSmEv.setDeliveryAck(esmClass.getDeliveryAck());
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
            ForwardSmORetAsyncCall fwsmoRet = new ForwardSmORetAsyncCall(fwsmoCallMsgId, ret, this.commandStatusCode);
            mcDispatcher.dispatch(fwsmoRet);
            logger.debug("Dispatch ForwardSmORetAsyncCall messageId:{} ret:{} commandStatusCode:{}", fwsmoCallMsgId, ret, commandStatusCode);;
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
