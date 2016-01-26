package com.nuevatel.mc.smpp.gw.appconn;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nuevatel.common.appconn.AppMessages;
import com.nuevatel.common.appconn.Conn;
import com.nuevatel.common.appconn.Message;
import com.nuevatel.common.appconn.Task;
import com.nuevatel.mc.appconn.ForwardSmOCall;
import com.nuevatel.mc.appconn.ForwardSmORet;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwApp;
import com.nuevatel.mc.smpp.gw.dialog.Dialog;
import com.nuevatel.mc.smpp.gw.dialog.DialogService;
import com.nuevatel.mc.smpp.gw.dialog.client.EsmeSubmitSmDialog;
import com.nuevatel.mc.smpp.gw.dialog.server.SmscDeliverSmDialog;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.util.ValidityPeriod;
import com.nuevatel.mc.tpdu.SmsDeliver;
import com.nuevatel.mc.tpdu.SmsSubmit;

/**
 * 
 * <p>The ForwardSmOTask class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Handles <code>ForwardSmOCall</code> appconn message.This kind of message is received when is delivered a short
 * message from local SMSC(MC) to remote SMSC.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class ForwardSmOTask implements Task {
    
    private static Logger logger = LogManager.getLogger(ForwardSmOTask.class);
    
    private DialogService dialogService = AllocatorService.getDialogService();
    
    private Config cfg = AllocatorService.getConfig();
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Message execute(Conn conn, Message msg) throws Exception {
        if (msg == null) {
            logger.warn("Null Message. appId:{}", conn.getLocalId());
            // Failed
            return new ForwardSmORet(AppMessages.FAILED).toMessage();
        }
        
        try {
            LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
            ForwardSmOCall fwsmoCall = new ForwardSmOCall(msg);
            // try to in dialog to handle it.
            logger.debug("ForwardSmOCall messageId:{} smMessageId:{}", fwsmoCall.getMessageId(), fwsmoCall.getSmMessageId());
            Dialog dialog = dialogService.getDialog(fwsmoCall.getSmMessageId());
            if (dialog != null) {
                logger.trace("Found dialog for dialogId:{}", fwsmoCall.getSmMessageId());
                // find dialog
                dialog.handleMcMessage(fwsmoCall);
                return new ForwardSmORet(AppMessages.ACCEPTED).toMessage();
            }
            // Create dialog if it does not exists
            SmppGwSession gwSession = SmppGwApp.getSmppGwApp().getSmppGwSession(fwsmoCall.getSmppSessionId());
            if (SmppGwSession.SMPP_TYPE.ESME.equals(gwSession.getSmppType())) {
                // esme submit_sm 
                SmsSubmit smsSubmit = new SmsSubmit(fwsmoCall.getTpdu(), now);
                EsmeSubmitSmDialog esmeSubmitDialog = new EsmeSubmitSmDialog(// dialogId
                                                                             fwsmoCall.getMessageId(),
                                                                             // gwProcessorId
                                                                             fwsmoCall.getSmppSessionId(),
                                                                             // processorId
                                                                             AllocatorService.getSmppGwProcessor(fwsmoCall.getSmppSessionId()).nextSmppProcessorId(),
                                                                             // smMessageId
                                                                             fwsmoCall.getSmMessageId() == null ? 0 : fwsmoCall.getSmMessageId(),
                                                                             // registeredDelivery
                                                                             fwsmoCall.getRegisteredDelivery(),
                                                                             // fromName
                                                                             fwsmoCall.getFromName(),
                                                                             // toName,
                                                                             fwsmoCall.getToName(),
                                                                             smsSubmit);
                dialogService.putDialog(esmeSubmitDialog, ValidityPeriod.resolveExpireAfterWriteTime(smsSubmit.getTpVp(), cfg.getDefaultValidityPeriod(), now));
                esmeSubmitDialog.init();
                logger.trace("Not found EsmeSubmitSmDialog. Created new dialog for dialogId:{}", esmeSubmitDialog.getDialogId());
                return new ForwardSmORet(AppMessages.ACCEPTED).toMessage();
            } else if (SmppGwSession.SMPP_TYPE.SMSC.equals(gwSession.getSmppType())) {
                // smsc deliver_sm
                SmsDeliver smsDeliver = new SmsDeliver(fwsmoCall.getTpdu());
                SmscDeliverSmDialog smscDeliverDialog = new SmscDeliverSmDialog(// dialogId
                                                                                fwsmoCall.getMessageId(),
                                                                                // gwProcessorId
                                                                                fwsmoCall.getSmppSessionId(),
                                                                                // processorId
                                                                                AllocatorService.getSmppGwProcessor(fwsmoCall.getSmppSessionId()).nextSmppProcessorId(),
                                                                                // smMessageId
                                                                                fwsmoCall.getSmMessageId(),
                                                                                // fromName
                                                                                fwsmoCall.getFromName(),
                                                                                // toName
                                                                                fwsmoCall.getToName(),
                                                                                smsDeliver);
                dialogService.putDialog(smscDeliverDialog, cfg.getDefaultValidityPeriod());
                smscDeliverDialog.init();
                logger.trace("Not found SmscDeliverSmDialog. Created new dialog for dialogId:{}", smscDeliverDialog.getDialogId());
                return new ForwardSmORet(AppMessages.ACCEPTED).toMessage();
            } else {
                // No op
                logger.warn("Unknow SmppGwSession.SMPP_TYPE");
                // Failed
                return new ForwardSmORet(AppMessages.FAILED).toMessage();
            }
        } catch (Throwable ex) {
            // Failed
            logger.warn(ex.getMessage(), ex);
            return new ForwardSmORet(AppMessages.FAILED).toMessage();
        }
    }
}
