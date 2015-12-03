package com.nuevatel.mc.smpp.gw.appconn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nuevatel.common.appconn.*;
import com.nuevatel.mc.appconn.ForwardSmOCall;
import com.nuevatel.mc.appconn.ForwardSmORet;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwApp;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.SubmitSmMcIEvent;

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
                SubmitSmMcIEvent event = SubmitSmMcIEvent.fromForwardSmOCall(fwsmoCall);
                // offer mc event
                AllocatorService.getSmppProcessor(conn.getLocalId()).offerMcIncomingEvent(event);
            } else if (SmppGwSession.SMPP_TYPE.ESME.equals(gwSession.getSmppType())) {
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
