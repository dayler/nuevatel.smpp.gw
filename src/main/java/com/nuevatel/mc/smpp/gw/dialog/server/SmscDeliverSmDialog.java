/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog.server;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Data;
import org.smpp.ServerPDUEvent;
import org.smpp.pdu.Address;
import org.smpp.pdu.PDU;

import com.nuevatel.common.appconn.AppMessages;
import com.nuevatel.common.appconn.Message;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.appconn.ForwardSmICall;
import com.nuevatel.mc.appconn.ForwardSmORetAsyncCall;
import com.nuevatel.mc.appconn.McMessage;
import com.nuevatel.mc.appconn.Name;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.dialog.Dialog;
import com.nuevatel.mc.smpp.gw.dialog.DialogState;
import com.nuevatel.mc.smpp.gw.dialog.DialogType;
import com.nuevatel.mc.smpp.gw.event.DeliverSmEvent;
import com.nuevatel.mc.smpp.gw.event.GenericNAckEvent;
import com.nuevatel.mc.smpp.gw.util.EsmClass;
import com.nuevatel.mc.smpp.gw.util.TpDcsUtils;
import com.nuevatel.mc.smpp.gw.util.TpStatusResolver;
import com.nuevatel.mc.tpdu.SmsDeliver;
import com.nuevatel.mc.tpdu.SmsStatusReport;
import com.nuevatel.mc.tpdu.TpAddress;
import com.nuevatel.mc.tpdu.Tpdu;

/**
 * Handle outgoing message from local MC to remote ESME.
 * <br/>
 * 
 * @author Ariel Salazar
 *
 */
public class SmscDeliverSmDialog extends Dialog {
    
    private static Logger logger = LogManager.getLogger(SmscDeliverSmDialog.class);
    
    private SmppGwProcessor gwProcessor;
    
    private boolean registeredDelivery;
    
    @SuppressWarnings("unused")
    private long smMessageId;
    
    @SuppressWarnings("unused")
    private Name fromName;
    
    private Name toName;
    
    private SmsDeliver smsDeliver;
    
    private byte tpStatus = Tpdu.TP_ST_PERMANENT_ERROR;
    
    public SmscDeliverSmDialog(Long dialogId,
                           Integer processorId,
                           Long smMessageId,
                           Name fromName,
                           Name toName,
                           SmsDeliver smsDeliver) {
        super(dialogId, processorId);
        
        Parameters.checkNull(dialogId, "dialogId");
        Parameters.checkNull(processorId, "processorId");
        
        Parameters.checkNull(fromName, "fromName");
        Parameters.checkNull(toName, "toName");
        Parameters.checkNull(smsDeliver, "smsDeliver");
        
        gwProcessor = AllocatorService.getSmppGwProcessor(processorId);
        this.registeredDelivery = smsDeliver.getTpSri();
        this.smMessageId = smMessageId == null ? 0 : smMessageId;
        this.fromName = fromName;
        this.toName = toName;
        this.smsDeliver = smsDeliver;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        state = DialogState.init;
        // Prepare smppevent.
        state = DialogState.forward;
        try {
        // create deliver_sm event
        EsmClass esmClass = new EsmClass(smsDeliver.getTpSri(), smsDeliver.getTpUdhi(), smsDeliver.getTpRp());
        DeliverSmEvent deliverSmEv = new DeliverSmEvent(dialogId);
        deliverSmEv.setEsmClass(esmClass.getValue());
        Address sourceAddr = new Address(smsDeliver.getTpOa().getTon(), smsDeliver.getTpOa().getNpi(), smsDeliver.getTpOa().getAddress());
        deliverSmEv.setSourceAddr(sourceAddr);
        Address destAddr = new Address((byte)(toName.getType() & TpAddress.TON), (byte)(toName.getType() & TpAddress.NPI), toName.getName());
        deliverSmEv.setDestAddr(destAddr);
        deliverSmEv.setRegisteredDelivery(registeredDelivery ? Data.SM_SMSC_RECEIPT_REQUESTED : Data.SM_SMSC_RECEIPT_NOT_REQUESTED);
        // set sm message
        deliverSmEv.setEncoding(TpDcsUtils.resolveSmppEncoding(smsDeliver.getTpDcs().getCharSet()));
        deliverSmEv.setData(smsDeliver.getTpdu());
        gwProcessor.offerSmppEvent(deliverSmEv);
        // awaiting by response
        state = DialogState.awaiting_0;
        } catch (Throwable ex) {
            state = DialogState.failed;
            logger.warn("Failed to initialize DeliverSmDialog.", ex);
            // nack
            commandStatusCode = Data.ESME_RSYSERR;
            // finalizing dialog
            invalidate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleSmppEvent(ServerPDUEvent ev) {
        PDU pdu = null;
        try {
            pdu = ev.getPDU();
            if (pdu.isResponse()) {
                if (pdu.isGNack()) {
                    // nack
                    commandStatusCode = pdu.getCommandStatus();
                    state = DialogState.failed;
                    invalidate();
                } else if (pdu.isResponse() && pdu.isOk() && Data.DELIVER_SM_RESP == pdu.getCommandId()) {
                    commandStatusCode = Data.ESME_ROK;
                    state = DialogState.close;
                    invalidate();
                } else {
                    commandStatusCode = Data.ESME_RINVCMDID;
                    state = DialogState.failed;
                    // pdu is not ok, or is not DELIVER_SM_RESP
                    invalidate();
                }
            } else {
                if (pdu.isRequest() && pdu.canResponse()) {
                    // invalid command id
                    GenericNAckEvent nack = new GenericNAckEvent(pdu.getSequenceNumber(), Data.ESME_RINVCMDID);
                    gwProcessor.offerSmppEvent(nack);
                }
                // Unknown condition. ret nack
                commandStatusCode = Data.ESME_RINVCMDID;
                state = DialogState.failed;
                invalidate();
            }
        } catch (Throwable ex) {
            state = DialogState.failed;
            commandStatusCode = Data.ESME_RSYSERR;
            logger.warn("Failed on ServerPDUEvent PDU:{}", pdu == null ? null : pdu.debugString());
            if (logger.isDebugEnabled() || logger.isTraceEnabled()) {
                logger.warn("Exception:", ex);
            }
            // invalidate dialog.
            invalidate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleMcMessage(McMessage msg) {
        // no op
    }
    
    private void closeDialog(byte ret) throws InterruptedException, ExecutionException, TimeoutException {
        // forwardSmOcall
        ForwardSmORetAsyncCall fwsmoRet = new ForwardSmORetAsyncCall(dialogId, ret, commandStatusCode);
        // async dispatch
        mcDispatcher.dispatch(fwsmoRet);
        logger.debug("Dispatch ForwardSmORetAsyncCall messageId:{} ret:{} serviceMsg:{}", dialogId, ret, commandStatusCode);
        if (registeredDelivery) {
            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            TpAddress destAddr = new TpAddress((byte)(toName.getType() & TpAddress.TON), (byte)(toName.getType() & TpAddress.NPI), toName.getName());
            tpStatus = TpStatusResolver.resolveTpStatus(commandStatusCode);
            SmsStatusReport smsSr = new SmsStatusReport(false, false, false, (byte) 0x0, destAddr, now, now, tpStatus);
            ForwardSmICall fwsmiCall = new ForwardSmICall(dialogId, smsSr.getTpdu());
            Message msg = mcDispatcher.dispatchAndWait(fwsmiCall);
            logger.debug("Dispatch ForwardSmICall messageId:{} tpStatus:{}", dialogId, tpStatus);
            if (msg == null || msg.getByte(AppMessages.RET_IE) == AppMessages.FAILED) {
                // failed
                logger.warn("Failed to dispatch confirmation delivery to MC. messageId:{}", dialogId);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        try {
            // check error conditions
            if (!DialogState.close.equals(state) || Data.ESME_ROK != commandStatusCode) {
                closeDialog(AppMessages.FAILED);
            }
            // ROK condition
            closeDialog(AppMessages.ACCEPTED);
            
        } catch (Throwable ex) {
            logger.error("Failed to execute DeliverSmDialog. On delivery confirmation ForwardSmORetAsync and ForwardSmIcall...", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DialogType getType() {
        return DialogType.smsc_deliver;
    }
}
