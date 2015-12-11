/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Data;
import org.smpp.ServerPDUEvent;
import org.smpp.pdu.DeliverSM;

import com.nuevatel.common.appconn.AppMessages;
import com.nuevatel.common.appconn.Message;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.appconn.ForwardSmOCall;
import com.nuevatel.mc.appconn.ForwardSmORetAsyncCall;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.Constants;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.event.DefaultResponseOKEvent;
import com.nuevatel.mc.smpp.gw.event.GenericNAckEvent;
import com.nuevatel.mc.tpdu.SmsDeliver;
import com.nuevatel.mc.tpdu.SmsStatusReport;
import com.nuevatel.mc.tpdu.TpDcs;
import com.nuevatel.mc.tpdu.TpUd;
import com.nuevatel.mc.tpdu.Tpdu;

/**
 * @author Ariel Salazar
 *
 */
public class DeliverSmDialog extends Dialog {
    
    private static Logger logger = LogManager.getLogger(DeliverSmDialog.class);
    
    private DeliverSM deliverPdu;
    
    private SmppGwProcessor gwProcessor;
    
    private boolean registeredDelivery;
    
    public DeliverSmDialog(long messageId, int processorId, DeliverSM deliverPdu) {
        super(messageId, processorId);
        Parameters.checkNull(deliverPdu, "deliverPdu");
        gwProcessor = AllocatorService.getSmppGwProcessor(processorId);
        this.deliverPdu = deliverPdu;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        try {
            state = DialogState.init;
            // do received ok response to remote smsc
            registeredDelivery = (deliverPdu.getRegisteredDelivery() & Data.SM_SMSC_RECEIPT_MASK) != Data.SM_SMSC_RECEIPT_NOT_REQUESTED;
            // TODO create fwsmicall and smsdeliver
            // SmsDeliver
            byte encoding;
            switch (deliverPdu.getShortMessageEncoding()) {
            case Constants.CS_GSM7:
                encoding = TpDcs.CS_GSM7;
                break;
            case Constants.CS_UCS2:
                encoding = TpDcs.CS_UCS2;
                break;
            default:
                encoding = StringUtils.isEmptyOrNull(deliverPdu.getShortMessage()) ? TpDcs.CS_8_BIT : TpDcs.CS_GSM7;
                break;
            }
            TpUd tpud = TpUd.newTpUd(encoding, deliverPdu.getShortMessage());
            // SmsDeliver smsDeliver = new SmsDeliver(tpMms, tpRp, tpUdhi, tpSri, tpOa, tpPid, tpDcs, tpScts, tpUdl, tpUd);
            
            
            
            
            
            
            if (!registeredDelivery) {
                DefaultResponseOKEvent respEv = new DefaultResponseOKEvent(deliverPdu);
                gwProcessor.offerSmppEvent(respEv);
                state = DialogState.close;
                invalidate();
            }
        } catch (Throwable ex) {
            logger.warn("Failed to initiate DeliverSmDialog. PDU:{}", deliverPdu == null ? null : deliverPdu.debugString());
            if (logger.isTraceEnabled()) {
                logger.warn("Exception:", ex);
            }
            // dispatch no ok
            errorCode = Data.ESME_RSYSERR;
            // finalize dialog
            invalidate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleSmppEvent(ServerPDUEvent ev) {
        // No op
    }

    @Override
    public void handleMessage(Message msg) {
        Parameters.checkNull(msg, "msg");
        try {
            ForwardSmOCall fwsmoCall = new ForwardSmOCall(msg);
            SmsStatusReport smsSr = new SmsStatusReport(fwsmoCall.getTpdu());
            switch (smsSr.getTpSt()) {
            case Tpdu.TP_ST_SM_RECEIVED_BY_SME:
            case Tpdu.TP_ST_SM_REPLACED_BY_SC:
                // deliver ROK sm response
                DefaultResponseOKEvent respEsmeRok = new DefaultResponseOKEvent(deliverPdu);
                gwProcessor.offerSmppEvent(respEsmeRok);
                state = DialogState.close;
                invalidate();
                return;
            case Tpdu.TP_ST_SM_FW_WO_CONFIRMATION:
                // Forward but cannot receive confirmation
                state = DialogState.failed;
                errorCode = Data.ESME_RSYSERR;
                invalidate();
                return;
            default:
                // No op
                break;
            }

            // TP_ST_TEMPORARY_ERROR_0, TP_ST_TEMPORARY_ERROR_1
            // public static final byte TP_ST_CONGESTION = 0x0;                // Congestion
            // public static final byte TP_ST_SME_BUSY = 0x1;                  // SME busy
            // public static final byte TP_ST_NO_REPONSE_FROM_SME = 0x2;       // No response from SME
            // public static final byte TP_ST_SERVICE_REJECTED = 0x3;          // Service rejected
            // public static final byte TP_ST_QOS_NOT_AVAILABLE = 0x4;         // Quality of service not available
            // public static final byte TP_ST_ERROR_IN_SME = 0x5;              // Error in SME
            byte temporaryError0 = (byte)(Tpdu.TP_ST_TEMPORARY_ERROR_0 & smsSr.getTpSt());
            byte temporaryError1 = (byte)(Tpdu.TP_ST_TEMPORARY_ERROR_1 & smsSr.getTpSt());
            if (temporaryError0 == Tpdu.TP_ST_CONGESTION
                || temporaryError1 == Tpdu.TP_ST_CONGESTION
                || temporaryError0 == Tpdu.TP_ST_SME_BUSY
                || temporaryError1 == Tpdu.TP_ST_SME_BUSY
                || temporaryError0 == Tpdu.TP_ST_NO_REPONSE_FROM_SME
                || temporaryError1 == Tpdu.TP_ST_NO_REPONSE_FROM_SME
                || temporaryError0 == Tpdu.TP_ST_SERVICE_REJECTED
                || temporaryError1 == Tpdu.TP_ST_SERVICE_REJECTED
                || temporaryError0 == Tpdu.TP_ST_ERROR_IN_SME
                || temporaryError1 == Tpdu.TP_ST_ERROR_IN_SME) {
                state = DialogState.failed;
                errorCode = Data.ESME_RX_T_APPN;
                invalidate();
                return;
            }
            // TP_ST_PERMANENT_ERROR
            // public static final byte TP_ST_REMOTE_PROCEDURE_ERROR = 0x0;    // Remote procedure error
            // public static final byte TP_ST_INCOMPATIBLE_DESTINATION = 0x1;  // Incompatible destination
            // public static final byte TP_ST_CONNECTION_REJECTED_BY_SME = 0x2;// Connection rejected by SME
            // public static final byte TP_ST_NOT_OBTAINABLE = 0x3;            // Not obtainable
            // public static final byte TP_ST_NO_IW_AVAILABLE = 0x5;           // No interworking available
            // public static final byte TP_ST_SM_VP_EXPIRED = 0x6;             // SM Validity Period Expired
            // public static final byte TP_ST_SM_DELETED_BY_ORIG_SME = 0x7;    // SM Deleted by originating SME
            // public static final byte TP_ST_SM_DELETED_BY_SC_ADM = 0x8;      // SM Deleted by SC Administration
            // public static final byte TP_ST_SM_DOES_NOT_EXIST = 0x9;         // SM does not exist
            byte permanentError = (byte)(Tpdu.TP_ST_PERMANENT_ERROR & smsSr.getTpSt());
            switch (permanentError) {
            case Tpdu.TP_ST_REMOTE_PROCEDURE_ERROR:
            case Tpdu.TP_ST_INCOMPATIBLE_DESTINATION:
            case Tpdu.TP_ST_NOT_OBTAINABLE:
            case Tpdu.TP_ST_NO_IW_AVAILABLE:
                errorCode = Data.ESME_RX_P_APPN;
                break;
            case Tpdu.TP_ST_CONNECTION_REJECTED_BY_SME:
                errorCode = Data.ESME_RX_R_APPN;
                break;
            case Tpdu.TP_ST_SM_VP_EXPIRED:
                errorCode = Data.ESME_RINVEXPIRY;
                break;
            case Tpdu.TP_ST_SM_DELETED_BY_ORIG_SME:
            case Tpdu.TP_ST_SM_DELETED_BY_SC_ADM:
            case Tpdu.TP_ST_SM_DOES_NOT_EXIST:
                errorCode = Data.ESME_RINVMSGID;
                break;

            default:
                errorCode = Data.ESME_RX_P_APPN;
                break;
            }
            // set failed state
            state = DialogState.failed;
            invalidate();
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
        return DialogType.deliverSm;
    }

    @Override
    public void execute() {
        if (errorCode == Data.ESME_ROK) {
            ForwardSmORetAsyncCall call = new ForwardSmORetAsyncCall(dialogId, AppMessages.ACCEPTED, 0);
            mcDispatcher.dispatch(call);
            return;
        }
        // TODO set service message?
        ForwardSmORetAsyncCall call = new ForwardSmORetAsyncCall(dialogId, AppMessages.ACCEPTED, errorCode);
        mcDispatcher.dispatch(call);
    }
    
    @Override
    protected void invalidate() {
        if (!DialogState.close.equals(state)) {
            // No in close estate means an error occurred in the work flow.
            gwProcessor.offerSmppEvent(new GenericNAckEvent(deliverPdu.getSequenceNumber(), errorCode == Data.ESME_ROK ? Data.ESME_RSYSERR : errorCode));
        }
        super.invalidate();
    }
}
