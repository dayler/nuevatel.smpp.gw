
package com.nuevatel.mc.smpp.gw;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.ServerPDUEvent;
import org.smpp.pdu.PDU;

import com.nuevatel.common.util.Parameters;
import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.smpp.gw.dialog.DialogService;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.SmppEvent;

/**
 * 
 * <p>The SmppProcessor class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Abstract class, its implementations are responsible to implement the logic to handle smpp messages (client or server). For each bind enable in
 * <code>SmppGwSession#getMaxBinds</code>, an instance of this kind is created.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public abstract class SmppProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppProcessor.class);
    
    /* Private variables */
    
    protected SmppGwSession gwSession;
    
    /*
     * Id that was assigned to this processor. <code>null</code> indicates, the processot was not asigned yet.
     */
    private Integer smppProcessorId = null;
    
    /*
     * All incoming events from the SMSC (SMPP server)
     */
    protected BlockingQueue<ServerPDUEvent>serverPduEvents = new LinkedBlockingDeque<>();
    
    /*
     * All outgoing events. Messages to submit to remote SMSC (SMPP server)
     */
    protected BlockingQueue<SmppEvent>smppEvents = new LinkedBlockingQueue<>();
    
    protected DialogService dialogService = AllocatorService.getDialogService();
    
    protected Config cfg = AllocatorService.getConfig();
    
    /**
     * Constructor, creates <code>SmppProcessor</code> from <code>SmppGwSession</code>.
     * 
     * @param gwSession
     */
    public SmppProcessor(SmppGwSession gwSession) {
        Parameters.checkNull(gwSession, "gwSession");
        
        this.gwSession = gwSession;
    }
    
    /**
     * Handle all incoming smpp messages. Each specialization must implement it.
     */
    public abstract void receive();
    
    /**
     * Handle outgoing smpp messages. Consume from <b>smppEvents</b>
     * 
     */
    public abstract void dispatch();
    
    /**
     * Get smppProcessorId.
     * 
     * @return
     */
    public Integer getSmppProcessorId() {
        return smppProcessorId;
    }
    
    /**
     * Set smppProcessorId.
     * 
     * @param smppProcessorId
     */
    public void setSmppProcessorId(Integer smppProcessorId) {
        this.smppProcessorId = smppProcessorId;
    }
    
    /**
     * Schedule a single event for delivering to remote SMSC.
     * 
     * @param event Event to schedule.
     * @return <code>true</code> if the event was scheduled.
     */
    public boolean offerServerPduEvent(ServerPDUEvent event) {
        try {
            return serverPduEvents.offer(event, Constants.TIMEOUT_OFFER_EVENT_QUEUE, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Schedule smpp event to dispatch.
     * 
     * @param event
     * @return
     */
    public boolean offerSmppEvent(SmppEvent event)  {
        try {
            return smppEvents.offer(event, Constants.TIMEOUT_OFFER_EVENT_QUEUE, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Shutdown processor. Each specialization must implement it.
     */
    public abstract void shutdown();
    
    /**
     * Assign seq number and register it on <code>smppMsgToDialogMap</code>.
     * 
     * @param pdu 
     * @param dialogId 
     */
    protected void assignSequenceNumber(PDU pdu, long dialogId) {
        pdu.assignSequenceNumber(true);
        if (dialogId < 0) {
            // no map
            return;
        }
        dialogService.registerSequenceNumber(pdu.getSequenceNumber(), dialogId);
    }
    
    /**
     * Handles all messages from remote SMPP server (remote SMSC), catch it and set in the <code>serverEvents</code> queue.
     * 
     * This is an instance of listener which obtains all PDUs received from the SMSC.
     * Application doesn't have explicitly call Session's receive() function,
     * all PDUs are passed to this application callback object.
     * See documentation in Session, Receiver and ServerPDUEventListener classes
     * form the SMPP library.
     * 
     * @param pduEvent
     */
    protected void handleServerPDUEvent(ServerPDUEvent pduEvent) {
        if (pduEvent == null) {
            logger.warn("Null ServerPDUEvent...");
            return;
        }
        try {
            if (!serverPduEvents.offer(pduEvent, Constants.TIMEOUT_OFFER_EVENT_QUEUE, TimeUnit.MILLISECONDS)) {
                // warn the queue rejects the event
                logger.warn("Failed to offer serverPDUEvent:{}", pduEvent.getPDU().debugString());
            }
        } catch (InterruptedException ex) {
            // On offer event
            logger.error("On handleServerPDUEvent...", ex);
        }
    }
    
    /**
     * Validates the string against the regular expression;
     * 
     * @param sourceAddr Strong to validate
     * @return <code>true</code> if sourceAddr match with RSourceAddrRegex.
     */
    protected boolean checkSourceAddr(String sourceAddr) {
        // if not defined regexp, always true
        if (StringUtils.isBlank(gwSession.getRSourceAddrRegex())) return true;
        // No valid empty source
        if (StringUtils.isBlank(sourceAddr)) return false;
        // match with regexp
        return sourceAddr.matches(gwSession.getRSourceAddrRegex());
    }
    
    /**
     * <code>true</code> if processor is bound.
     * 
     * @return
     */
    public abstract boolean isBound();
}
