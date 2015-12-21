/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

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

import com.nuevatel.common.appconn.AppMessages;
import com.nuevatel.common.appconn.Message;
import com.nuevatel.common.util.LongUtil;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.appconn.ForwardSmICall;
import com.nuevatel.mc.appconn.ForwardSmORetAsyncCall;
import com.nuevatel.mc.appconn.Name;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.Constants;
import com.nuevatel.mc.smpp.gw.PropName;
import com.nuevatel.mc.smpp.gw.SmppDateUtil;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.event.DefaultResponseOKEvent;
import com.nuevatel.mc.smpp.gw.event.SubmitSmppEvent;
import com.nuevatel.mc.tpdu.SmsDeliver;
import com.nuevatel.mc.tpdu.SmsStatusReport;
import com.nuevatel.mc.tpdu.TpAddress;
import com.nuevatel.mc.tpdu.TpDcs;
import com.nuevatel.mc.tpdu.TpUd;
import com.nuevatel.mc.tpdu.Tpdu;

/**
 * Handle outgoig message rom local MC to remote SMSC.
 * <br/>
 * 
 * @author Ariel Salazar
 *
 */
public class SubmitSmDialog extends Dialog {
    
    private static Logger logger= LogManager.getLogger(SubmitSmDialog.class);
    
    private SmppGwProcessor gwProcessor;
    
    private boolean registeredDelivery;
    
    @SuppressWarnings("unused")
    private long smMessageId;
    
    @SuppressWarnings("unused")
    private Name fromName;
    
    private Name toName;
    
    private SmsDeliver smsDeliver;
    
    private long defaultValidityPeriod;
    
    private byte tpStatus = Tpdu.TP_ST_ERROR_IN_SME;
    
    public SubmitSmDialog(long messageId,
                          int processorId, // smppGwId
                          long smMessageId,
                          byte registeredDelivery,
                          Name fromName,
                          Name toName,
                          SmsDeliver smsDeliver) {
        super(messageId, processorId);
        
        Parameters.checkNull(fromName, "fromName");
        Parameters.checkNull(toName, "toName");
        Parameters.checkNull(smsDeliver, "smsDeliver");
        
        gwProcessor = AllocatorService.getSmppGwProcessor(processorId);
        this.registeredDelivery = (registeredDelivery & Data.SM_SMSC_RECEIPT_MASK) != Data.SM_SMSC_RECEIPT_NOT_REQUESTED; 
        this.smMessageId = smMessageId;
        this.fromName = fromName;
        this.toName = toName;
        this.smsDeliver = smsDeliver;
        defaultValidityPeriod = LongUtil.tryParse(AllocatorService.getProperties().getProperty(PropName.defaultValidityPeriod.property()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        try {
            state = DialogState.init;
            // Prepare SmppEvent
            SubmitSmppEvent event = new SubmitSmppEvent(dialogId);
            Address sourceAddr = new Address(smsDeliver.getTpOa().getTon(), smsDeliver.getTpOa().getNpi(), smsDeliver.getTpOa().getAddress());
            event.setSourceAddr(sourceAddr);
            Address destAddr = new Address((byte)(toName.getType() & TpAddress.TON), (byte)(toName.getType() & TpAddress.NPI), toName.getName());
            event.setDestAddr(destAddr);
            // replcae if present
            event.setReplaceIfPresentFlag((byte)Data.SM_REPLACE);
            event.setData(smsDeliver.getTpdu());
            // get tpdu
            TpUd tpUd = new TpUd(smsDeliver.getTpUdhi(), smsDeliver.getTpDcs(), smsDeliver.getTpUdl(), smsDeliver.getTpdu());
            // select charset
            switch (tpUd.getCharSet()) {
            case TpDcs.CS_GSM7:
                event.setEncoding(Constants.CS_GSM7);
                break;
            case TpDcs.CS_UCS2:
                event.setEncoding(Constants.CS_UCS2);
                break;
            case TpDcs.CS_8_BIT:
                // no op
                break;
            default:
                event.setEncoding(Constants.CS_GSM7);
                break;
            }
            // serviceCentre timestamp
            event.setScheduleDeliveryTime(SmppDateUtil.toSmppDatetime(smsDeliver.getTpScts()));
            // use default validity period
            ZonedDateTime validityPeriod = defaultValidityPeriod > 0 ? ZonedDateTime.now(ZoneId.of("UTC")).plus(defaultValidityPeriod, ChronoUnit.MILLIS) : ZonedDateTime.now(ZoneId.of("UTC"));
            event.setValidityPeriod(SmppDateUtil.toSmppDatetime(validityPeriod));
            // esm_class
            // default message mode, default message type
            byte esmClass = (byte)(0x00 /* default message mode */
                            | 0x00 /* default message type */
                            | (smsDeliver.getTpUdhi() ? 0x40/* udhi present */ : 0x00) | (smsDeliver.getTpRp() ? 0x80 /* reply path present */ : 0x0));
            event.setEsmClass(esmClass);
            // Set default protocol id
            event.setProtocolId(Data.DFLT_PROTOCOLID);
        } catch (Throwable ex) {
            logger.warn("Failed to initialize SubmitSmDialog.", ex);
            // nack
            commandStatusCode = Data.ESME_RSYSERR;
            // finalize dialog
            invalidate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleSmppEvent(ServerPDUEvent ev) {
        try {
            PDU pdu = ev.getPDU();
            if (pdu.isResponse()) {
                if (pdu.isGNack()) {
                    state = DialogState.failed;
                    commandStatusCode = pdu.getCommandStatus();
                    ForwardSmORetAsyncCall fwsmoRetCall = new ForwardSmORetAsyncCall(dialogId, AppMessages.FAILED, commandStatusCode);
                    // dispatch failed message
                    mcDispatcher.dispatch(fwsmoRetCall);
                    invalidate();
                } else if (pdu.isOk()) {
                    if (!registeredDelivery) {
                        // if register delivery is not true, finish transaction,
                        // no await by response.
                        state = DialogState.close;
                        tpStatus = Tpdu.TP_ST_SM_RECEIVED_BY_SME;
                        commandStatusCode = Data.ESME_ROK;
                        invalidate();
                    } else {
                        // dispatch ForwardSmORetAsyncCall
                        commandStatusCode = Data.ESME_ROK;
                        ForwardSmORetAsyncCall fwsmoRetCall = new ForwardSmORetAsyncCall(dialogId, AppMessages.ACCEPTED, commandStatusCode);
                        // dispatch async message
                        mcDispatcher.dispatch(fwsmoRetCall);
                    }
                }
            } else if (pdu.isRequest()) {
                // handle Report delivery response
                if (pdu.getCommandId() == Data.DELIVER_SM) {
                    // only on register_delivery
                    gwProcessor.offerSmppEvent(new DefaultResponseOKEvent(pdu));
                    commandStatusCode = Data.ESME_ROK;
                    state = DialogState.close;
                    tpStatus = Tpdu.TP_ST_SM_RECEIVED_BY_SME;
                    invalidate();
                }
            }
        } catch (Throwable ex) {
            logger.error("Failed to handle ServerPDUEvent:{}", ev.getPDU() != null ? null : ev.getPDU().debugString(), ex);
            commandStatusCode = Data.ESME_RSYSERR;
            state = DialogState.failed;
            invalidate();
        }
    }

    @Override
    public void handleMcMessage(Message msg) {
        // No op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DialogType getType() {
        return DialogType.submitSm;
    }
    
    @Override
    public void execute() {
        if (!registeredDelivery) {
            // No register delivery
            return;
        }
        try {
            TpAddress recipientAddress = new TpAddress((byte) (toName.getType() & TpAddress.TON), (byte) (toName.getType() & TpAddress.NPI), toName.getName());
            LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
            // SmsStatusReport
            // boolean tpUdhi, boolean tpMms, boolean tpSrq, byte tpMr,
            // TpAddress tpRa, LocalDateTime tpScts, LocalDateTime tpDt, byte tpSt
            SmsStatusReport smsSr = new SmsStatusReport(false, false, false, (byte) 0, recipientAddress/* dest addr */, now, now, tpStatus);
            ForwardSmICall fwsmiCall = new ForwardSmICall(dialogId, smsSr.getTpdu());
            Message msg = mcDispatcher.dispatchAndWait(fwsmiCall);
            if (msg.getByte(AppMessages.RET_IE) == AppMessages.FAILED) {
                // failed
                logger.warn("Failed to dispatch confirmation delivery to MC. messageId", dialogId);
            }
        } catch (Throwable ex) {
            logger.error("Failed to execute SubmitSmDialog...", ex);
        }
    }

}
