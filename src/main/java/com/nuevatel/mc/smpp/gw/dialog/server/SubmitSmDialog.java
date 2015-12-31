/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Data;
import org.smpp.ServerPDUEvent;
import org.smpp.pdu.SubmitSM;

import com.nuevatel.common.appconn.Message;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.Constants;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.dialog.Dialog;
import com.nuevatel.mc.smpp.gw.dialog.DialogState;
import com.nuevatel.mc.smpp.gw.dialog.DialogType;
import com.nuevatel.mc.tpdu.SmsSubmit;
import com.nuevatel.mc.tpdu.TpAddress;
import com.nuevatel.mc.tpdu.TpDcs;
import com.nuevatel.mc.tpdu.TpUd;

/**
 * @author Ariel Salazar
 *
 */
public class SubmitSmDialog extends Dialog {
    
    private static Logger logger = LogManager.getLogger(SubmitSmDialog.class);
    
    private SubmitSM submitSm;
    
    private boolean registeredDelivery;
    
    private SmppGwProcessor gwProcessor;
    
    public SubmitSmDialog(long dialogId, int processorId, SubmitSM submitSm) {
        super(dialogId, processorId);
        Parameters.checkNull(submitSm, "submitSm");
        // select processor
        gwProcessor = AllocatorService.getSmppGwProcessor(processorId);
        this.submitSm = submitSm;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        state = DialogState.init;
        // registered delivery
        registeredDelivery = (submitSm.getRegisteredDelivery() & Data.SM_SME_ACK_MASK) != Data.SM_SME_ACK_NOT_REQUESTED;
        // Send ForwardSmICall to MC
        TpAddress tpDestAddr = new TpAddress(submitSm.getDestAddr().getTon(), submitSm.getDestAddr().getNpi(), submitSm.getDestAddr().getAddress());
        // resolve encoding
        byte encoding;
        switch (submitSm.getShortMessageEncoding()) {
        case Constants.CS_GSM7:
            encoding = TpDcs.CS_GSM7;
            break;
        case Constants.CS_UCS2:
            encoding = TpDcs.CS_UCS2;
            break;
        default:
            encoding = StringUtils.isEmptyOrNull(submitSm.getShortMessageEncoding()) ? TpDcs.CS_8_BIT : TpDcs.CS_GSM7;
            break;
        }
        // create tpud
        TpUd tpUd = TpUd.newTpUd(encoding, );
        // SmsSubmit
        SmsSubmit smsSubmit = new SmsSubmit(// TP-Reject-Duplicates
                                            true, 
                                            // TP-Reply-Path
                                            (submitSm.getEsmClass() & Data.SM_REPLY_PATH_GSM) == Data.SM_REPLY_PATH_GSM,
                                            // TP-User-Data-Header-Indicator
                                            (submitSm.getEsmClass() & Data.SM_UDH_GSM) == Data.SM_UDH_GSM,
                                            // Tp-Status-Report-Request
                                            registeredDelivery,
                                            // TP-Message-Reference
                                            Constants.TP_DFLT_MR,
                                            // TP-Destination-TpAddress
                                            tpDestAddr,
                                            // TP-Protocol-Identifier
                                            Constants.TP_DFLT_PI,
                                            tpDcs, tpVp, tpUdl, tpUd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleSmppEvent(ServerPDUEvent ev) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleMcMessage(Message msg) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DialogType getType() {
        // TODO Auto-generated method stub
        return null;
    }
}
