/**
 * 
 */
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
 *
 */
public class EsmeDeliverSmDialog extends Dialog {
    
    private static Logger logger = LogManager.getLogger(EsmeDeliverSmDialog.class);
    
    private SmppGwProcessor gwProcessor;
    
    private boolean registeredDelivery;
    
    private DeliverSM deliverPdu = null;
    
    /**
     * Delivery registered message id. Used to confirm delivery request to AppConn server (MC). -1 indicates no set value.
     */
    private long fwsmoCallMsgId = -1;
    
    public EsmeDeliverSmDialog(int processorId) {
        super(processorId);
        // select processor
        gwProcessor = AllocatorService.getSmppGwProcessor(processorId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        state = DialogState.init;
    }

    /**
     * {@inheritDoc}
     */
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
            registeredDelivery = (deliverPdu.getRegisteredDelivery() & Data.SM_SME_ACK_MASK) != Data.SM_SME_ACK_NOT_REQUESTED;
            // create SmsSubmit
            TpDcs tpDcs = TpDcsUtils.resolveTpDcs(deliverPdu.getShortMessageEncoding()); // TpDcsUtils.resolveTpDcs("UTF-16BE");
            EsmClass esmClass = new EsmClass(deliverPdu.getEsmClass());
            TpUd tpud = new TpUd(esmClass.getUdhi(), // tpUdhi
                                 tpDcs, // tpDcs
                                 TpUdUtils.resolveTpUdl(tpDcs.getCharSet(), deliverPdu.getShortMessageData().getBuffer()), // tpUdl
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
            // dispatch sync message
            Message ret = mcDispatcher.dispatchAndWait(fwsmiCall);
            logger.debug("Disparch ForwardSmICall");
            if (ret == null) {
                // failed
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
            // Set assigned message id
            setDialogId(fwsmiRet.getMessageId());
            // TODO debug
            System.out.println("dispatch fwsmiCall ok. messageId:" + fwsmiRet.getMessageId() + " rds=" + registeredDelivery);
            
            if (!registeredDelivery) {
                // TODO debug 
                System.out.println("registeredDelivery invalidate...");
                // No await registered delivery
                state = DialogState.close;
                invalidate();
                return;
            }
            // For registered delivery enable await confirmation message from MC. No invalidate.
        } catch (Throwable ex) {
            logger.warn("Failed to initiate DeliverSmDialog. PDU:{}", deliverPdu == null ? null : deliverPdu.debugString());
            if (logger.isDebugEnabled() || logger.isTraceEnabled()) {
                logger.warn("Exception:", ex);
            }
            // dispatch no ok
            commandStatusCode = Data.ESME_RSYSERR;
            // finalize dialog
            invalidate();
        }
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
            if (Data.ESME_ROK == commandStatusCode) {
                // Everything is ok, close dialog and invalidate.
                state = DialogState.close;
                invalidate();
                return;
            } else {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public DialogType getType() {
        return DialogType.esme_deliver;
    }

    @Override
    public void execute() {
        // serviceMessage = smpp commandStatus
        if (DialogState.close.equals(state) && commandStatusCode == Data.ESME_ROK) {
            // deliver ROK sm response, message delivered.
            DefaultResponseOKEvent respEsmeROk = new DefaultResponseOKEvent(deliverPdu);
            gwProcessor.offerSmppEvent(respEsmeROk);
        } else {
            // No in close estate means an error occurred in the work flow.
            gwProcessor.offerSmppEvent(new GenericNAckEvent(deliverPdu.getSequenceNumber(), commandStatusCode == Data.ESME_ROK ? Data.ESME_RSYSERR : commandStatusCode));
            return;
        }
        // only if delivery status is required
        if (registeredDelivery) {
            if (DialogState.close.equals(state) && Data.ESME_ROK == commandStatusCode) {
                // notify ok
                ForwardSmORetAsyncCall fwsmo = new ForwardSmORetAsyncCall(fwsmoCallMsgId, AppMessages.ACCEPTED, commandStatusCode);
                mcDispatcher.dispatch(fwsmo);
            } else {
                // failed
                ForwardSmORetAsyncCall fwsmo = new ForwardSmORetAsyncCall(fwsmoCallMsgId, AppMessages.FAILED, commandStatusCode == Data.ESME_ROK ? Data.ESME_RSYSERR : commandStatusCode);
                mcDispatcher.dispatch(fwsmo);
            }
        }
    }
}
