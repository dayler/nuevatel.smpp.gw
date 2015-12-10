package com.nuevatel.mc.smpp.gw.appconn;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nuevatel.common.appconn.*;
import com.nuevatel.mc.appconn.ForwardSmICall;
import com.nuevatel.mc.appconn.ForwardSmOCall;
import com.nuevatel.mc.appconn.ForwardSmORet;
import com.nuevatel.mc.appconn.ForwardSmORetAsyncCall;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.ProxyApp;
import com.nuevatel.mc.smpp.gw.SmppGwApp;
import com.nuevatel.mc.smpp.gw.dialog.DialogService;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.SubmitSmppEvent;
import com.nuevatel.mc.tpdu.SmsStatusReport;
import com.nuevatel.mc.tpdu.TpAddress;

/**
 * <p>The ForwardSmOTask class.</p> Handles <code>ForwardSmOCall</code> appconn message.This kind of message is received when is delivered a short
 * message from local SMSC(MC) to remote SMSC.
 * <br/>
 * 
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2015</p>
 *
 * @author Ariel Salazar, Jorge Vasquez, Eduardo Marin
 * @version 1.0
 * @since 1.8
 */
public class ForwardSmOTask implements Task {
    
    private static Logger logger = LogManager.getLogger(ForwardSmOTask.class);
    
    private DialogService dialogService = AllocatorService.getDialogService();
    
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
            ForwardSmOCall fwsmoCall = new ForwardSmOCall(msg);
            SmppGwSession gwSession = SmppGwApp.getSmppGwApp().getSmppGwSession(fwsmoCall.getSmppSessionId());
            
            if (SmppGwSession.SMPP_TYPE.ESME.equals(gwSession.getSmppType())) {
                // submit_sm
                SubmitSmppEvent event;
                if (dialogService.containsDialog(fwsmoCall.getMessageId())) {
                    // already exists a dialog.
                    event = SubmitSmppEvent.fromSmsStatusReport(fwsmoCall);
                } else {
                    event = SubmitSmppEvent.fromSmsDelivery(fwsmoCall);
                }
                // offer mc event
                AllocatorService.getSmppGwProcessor(conn.getLocalId()).offerSmppEvent(event);
            } else if (SmppGwSession.SMPP_TYPE.SMSC.equals(gwSession.getSmppType())) {
                // TODO deliver_sm
            } else {
                // No op
                logger.warn("Unknow SmppGwSession.SMPP_TYPE");
                // Failed
                return new ForwardSmORet(AppMessages.FAILED).toMessage();
            }
            // Ret Accepted
            return new ForwardSmORet(AppMessages.ACCEPTED).toMessage();
        } catch (Throwable ex) {
            // Failed
            logger.warn(ex.getMessage(), ex);
            return new ForwardSmORet(AppMessages.FAILED).toMessage();
        }

    }
}
