/**
 * 
 */
package com.nuevatel.mc.smpp.gw;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.smpp.ServerPDUEvent;

import com.nuevatel.common.Processor;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.SmppEvent;

/**
 * Abstract SmppProcessor.
 * 
 * @author Ariel Salazar
 *
 */
public abstract class SmppGwProcessor implements Processor {
    
    protected SmppGwSession gwSession;
    
    /**
     * All incoming events from the SMSC (SMPP server)
     */
    protected BlockingQueue<ServerPDUEvent>serverPduEvents = new LinkedBlockingDeque<>();
    
    /**
     * All outgoing events. Messages to submit to remote SMSC (SMPP server)
     */
    protected BlockingQueue<SmppEvent>smppEvents = new LinkedBlockingQueue<>();
    
    public SmppGwProcessor(SmppGwSession gwSession) {
        Parameters.checkNull(gwSession, "gwSession");
        this.gwSession = gwSession;
    }
    
    public boolean offerServerPduEvent(ServerPDUEvent event) {
        try {
            return serverPduEvents.offer(event, Constants.TIMEOUT_OFFER_EVENT_QUEUE, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
    
    public boolean offerSmppEvent(SmppEvent event)  {
        try {
            return smppEvents.offer(event, Constants.TIMEOUT_OFFER_EVENT_QUEUE, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }
    
    public SmppGwSession getSmppGwSession() {
        return gwSession;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void execute();

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract void shutdown(int ts);

}
