
package com.nuevatel.mc.smpp.gw.dialog.client;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Data;
import org.smpp.ServerPDUEvent;
import org.smpp.pdu.DeliverSM;

import com.nuevatel.common.appconn.AppMessages;
import com.nuevatel.common.appconn.Message;
import com.nuevatel.common.util.Parameters;
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
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.event.DefaultResponseOKEvent;
import com.nuevatel.mc.smpp.gw.event.GenericNAckEvent;
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
 * <p>The EsmeDeliverSmDialog class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2015</p>
 * 
 * Handle incoming deliver message from remote smsc to local smpp client.
 * <br/>
 * (1) is enable rds.<br/>
 * (2) Create SmsDeliver.<br/>
 * (2) Send ForwardSmICall to Mc.<br/>
 * (3) if rds is enabled await confirmation to deliver from MC. In other case finish trabsaction.<br/>
 * (4) Confirmation has arrived, send ROK to remote SMSC.<br/>
 * (5) Remote SMSC responds with acknowledgment for deliver confirmation.<br/>
 * (6) Responds with ForwardSmORetAsync to confirm deliver.
 * (7) Finish the transaction.<br/>
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class EsmeDeliverSmDialog extends Dialog {
    
    /* private variables */
    private static Logger logger = LogManager.getLogger(EsmeDeliverSmDialog.class);
    
    private SmppGwProcessor gwProcessor;
    
    private boolean registeredDelivery;
    
    private DeliverSM deliverPdu = null;
    
    private Config cfg = AllocatorService.getConfig();
    
    /**
     * Delivery registered message id. Used to confirm delivery request to AppConn server (MC). -1 indicates no set value.
     */
    private long fwsmoCallMsgId = -1;
    
    /**
     * Creates a new instance of EsmeDeliverSmDialog.
     * @param gwProcessorId
     * @param processorId
     */
    public EsmeDeliverSmDialog(int gwProcessorId, int processorId) {
        super(gwProcessorId, processorId);
        // select processor
        gwProcessor = AllocatorService.getSmppGwProcessor(gwProcessorId);
    }
    
    @Override
    public void init() {
        state = DialogState.init;
    }
    
    @Override
    public void handleSmppEvent(ServerPDUEvent ev) {
        if (!(ev.getPDU() instanceof DeliverSM)) {
            logger.warn("No DeliverSM instance({})", ev.getPDU().getClass().getName());
            return;
        }
        
        try {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            state = DialogState.forward;
            deliverPdu = (DeliverSM) ev.getPDU();
            // do received ok response to remote smsc
            // get register delivery
            registeredDelivery = (deliverPdu.getRegisteredDelivery() & Data.SM_SMSC_RECEIPT_MASK) != Data.SM_SMSC_RECEIPT_NOT_REQUESTED;
            // create SmsSubmit
            TpDcs tpDcs = TpDcsUtils.resolveTpDcs(deliverPdu.getDataCoding()); // TpDcsUtils.resolveTpDcs("UTF-16BE");
            EsmClass esmClass = new EsmClass(deliverPdu.getEsmClass());
            TpUd tpud = new TpUd(esmClass.getUdhi(), // tpUdhi
                                 tpDcs, // tpDcs
                                 (byte) deliverPdu.getShortMessageData().getBuffer().length, // tpUdl
                                 TpUdUtils.fixTpUd(tpDcs.getCharSet(), deliverPdu.getShortMessageData().getBuffer())); // tpUd
            SmsSubmit smsSubmit = new SmsSubmit(// tpRd
                                                true,
                                                // tpRp
                                                (deliverPdu.getEsmClass() & Data.SM_REPLY_PATH_GSM) == Data.SM_REPLY_PATH_GSM,
                                                // tpUdhi
                                                (deliverPdu.getEsmClass() & Data.SM_UDH_GSM) == Data.SM_UDH_GSM,
                                                // tpSrr
                                                registeredDelivery,
                                                // tpMr
                                                (byte)0x0,
                                                // tpDa
                                                new TpAddress(deliverPdu.getDestAddr().getTon(), deliverPdu.getDestAddr().getNpi(), deliverPdu.getDestAddr().getAddress()),
                                                // tpPid
                                                deliverPdu.getProtocolId(),
                                                // tpDcs
                                                tpDcs,
                                                // tpVp
                                                SmppDateUtil.parseDateTime(now, deliverPdu.getValidityPeriod()).toLocalDateTime(),
                                                // tpUdl
                                                tpud.getTpUdl(),
                                                // tpud
                                                tpud.getTpUd());
            // ForwardSmICall to notify the MC, new message has arrived
            ForwardSmICall fwsmiCall = new ForwardSmICall(// smppServiceType,
                                                          gwProcessor.getSmppGwSession().getSystemType(),
                                                          // smppScheduleDeliveryTime,
                                                          StringUtils.isEmptyOrNull(deliverPdu.getScheduleDeliveryTime()) ? null : SmppDateUtil.parseDateTime(ZonedDateTime.now(ZoneId.systemDefault()), deliverPdu.getScheduleDeliveryTime()),
                                                          // smppReplaceIfPresentFlag
                                                          deliverPdu.getReplaceIfPresentFlag(),
                                                          // smppGwId
                                                          gwProcessor.getSmppGwSession().getSmppGwId(),
                                                          // smppSessionId
                                                          gwProcessor.getSmppGwSession().getSmppSessionId(),
                                                          // fromName
                                                          new Name(deliverPdu.getSourceAddr().getAddress(), (byte)(deliverPdu.getSourceAddr().getTon() | deliverPdu.getSourceAddr().getNpi())),
                                                          // tpdu
                                                          smsSubmit.getTpdu());
            if (logger.isDebugEnabled() || logger.isTraceEnabled()) {
                logger.debug("ForwardSmICall smppServiceType:{} smppScheduleDeliveryTime:{} smppGwId:{} smppSessionId:{} fromName:{} tpdu:{} | rds:{} tpDcs:{}",
                             fwsmiCall.getSmppServiceType(), fwsmiCall.getSmppScheduleDeliveryTime(), fwsmiCall.getSmppGwId(), fwsmiCall.getSmppSessionId(),
                             fwsmiCall.getFromName(), fwsmiCall.getTpdu(), registeredDelivery, tpud.getCharSet());
            }
            // dispatch sync message
            Message ret = mcDispatcher.dispatchAndWait(fwsmiCall);
            if (ret == null) {
                // failed
                state = DialogState.failed;
                commandStatusCode = Data.ESME_RSYSERR;
                invalidate();
                return;
            }
            ForwardSmIRet fwsmiRet = new ForwardSmIRet(ret);
            logger.debug("ForwardSmIRet messageId:{} ret:{}", fwsmiRet.getMessageId(), fwsmiRet.getRet());
            if (AppMessages.FAILED == fwsmiRet.getRet()) {
                // failed
                state = DialogState.failed;
                commandStatusCode = Data.ESME_RSYSERR;
                invalidate();
                return;
            }
            // Set assigned message id
            setDialogId(fwsmiRet.getMessageId());
            // Register dialog (mc assing message id)
            long tmpValidutyPeriod = SmppDateUtil.parseDateTime(now, deliverPdu.getValidityPeriod()).toEpochSecond() - now.toEpochSecond();
            dialogService.putDialog(this, tmpValidutyPeriod > 0 ? tmpValidutyPeriod : cfg.getDefaultValidityPeriod());
            logger.info("Create Dialog. dialogId:{} rds:{}", dialogId, registeredDelivery);
            if (!registeredDelivery) {
                // No await registered delivery
                state = DialogState.close;
                invalidate();
                return;
            }
            // For registered delivery enable await confirmation message from MC. No invalidate.
        } catch (Throwable ex) {
            logger.warn("Failed to initiate DeliverSmDialog. PDU:{}", deliverPdu == null ? null : deliverPdu.debugString());
            if (logger.isDebugEnabled() || logger.isTraceEnabled()) logger.warn("Exception:", ex);
            // dispatch no ok
            commandStatusCode = Data.ESME_RSYSERR;
            // finalize dialog
            invalidate();
        }
    }
    
    @Override
    protected void invalidate() {
        logger.info("Invalidate Dialog, dialogId:{} rds=:{}", dialogId, registeredDelivery);
        super.invalidate();
    }
    
    @Override
    public void handleMcMessage(McMessage msg) {
        Parameters.checkNull(msg, "msg");
        try {
            state = DialogState.forward;
            ForwardSmOCall fwsmoCall = (ForwardSmOCall) msg;
            // set delivery request confirmation id
            fwsmoCallMsgId = fwsmoCall.getMessageId();
            // SmsStatus report
            SmsStatusReport smsSr = new SmsStatusReport(fwsmoCall.getTpdu());
            commandStatusCode = TpStatusResolver.resolveSmppCommandStatus(smsSr.getTpSt());
            logger.debug("ForwardSmOCall messageId:{} tpSt:{}", fwsmoCallMsgId, smsSr.getTpSt());
            if (Data.ESME_ROK == commandStatusCode) {
                // Everything is ok, close dialog and invalidate.
                state = DialogState.close;
                invalidate();
                return;
            }
            else {
                // Failed
                state = DialogState.failed;
                invalidate();
                return;
            }
        } catch (Throwable ex) {
            logger.warn("Failed on handle local smsc message. PDU:{}", deliverPdu == null ? null : deliverPdu.debugString());
            invalidate();
        }
    }

    @Override
    public DialogType getType() {
        return DialogType.esme_deliver;
    }

    @Override
    public void execute() {
        logger.info("Execute Dialog. dialogId{} rds:{} state:{}", dialogId, registeredDelivery, state);
        // serviceMessage = smpp commandStatus
        if (DialogState.close.equals(state) && commandStatusCode == Data.ESME_ROK) {
            // deliver ROK sm response, message delivered.
            DefaultResponseOKEvent respEsmeROk = new DefaultResponseOKEvent(deliverPdu);
            gwProcessor.getSmppProcessor(processorId).offerSmppEvent(respEsmeROk);
        }
        else {
            // No in close estate means an error occurred in the work flow.
            gwProcessor.getSmppProcessor(processorId).offerSmppEvent(new GenericNAckEvent(deliverPdu.getSequenceNumber(), commandStatusCode == Data.ESME_ROK ? Data.ESME_RSYSERR : commandStatusCode));
            return;
        }
        // only if delivery status is required
        if (registeredDelivery) {
            if (DialogState.close.equals(state) && Data.ESME_ROK == commandStatusCode) {
                // notify ok
                ForwardSmORetAsyncCall fwsmo = new ForwardSmORetAsyncCall(fwsmoCallMsgId, AppMessages.ACCEPTED, commandStatusCode);
                logger.debug("ForwardSmORetAsyncCall messageId:{} ret:{} serviceMessage:{}", fwsmo.getMessageId(), fwsmo.getRet(), fwsmo.getServiceMsg());
                mcDispatcher.dispatch(fwsmo);
            }
            else {
                // failed
                ForwardSmORetAsyncCall fwsmo = new ForwardSmORetAsyncCall(fwsmoCallMsgId, AppMessages.FAILED, commandStatusCode == Data.ESME_ROK ? Data.ESME_RSYSERR : commandStatusCode);
                logger.debug("ForwardSmORetAsyncCall messageId:{} ret:{} serviceMessage:{}", fwsmo.getMessageId(), fwsmo.getRet(), fwsmo.getServiceMsg());
                mcDispatcher.dispatch(fwsmo);
            }
        }
    }
}
