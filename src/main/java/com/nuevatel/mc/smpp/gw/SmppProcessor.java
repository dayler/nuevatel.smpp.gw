/**
 * 
 */
package com.nuevatel.mc.smpp.gw;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import com.nuevatel.common.Processor;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.smpp.gw.dialog.SmppEvent;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.McEvent;
import com.nuevatel.mc.smpp.gw.event.McIncomingEvent;

/**
 * Abstract SmppProcessor.
 * 
 * @author Ariel Salazar
 *
 */
public abstract class SmppProcessor implements Processor {
    
    protected static final int TIME_OUT_SMPP_EVENT_QUEUE = 500;
    
    protected SmppGwSession gwSession;
    
    /**
     * All incoming events from the SMSC (SMPP server)
     */
    protected BlockingQueue<SmppEvent>smppEvents = new LinkedBlockingDeque<>();
    
    /**
     * All outgoing events. Messages to submit to remote SMSC (SMPP server)
     */
    protected BlockingQueue<McIncomingEvent>mcEvents = new LinkedBlockingQueue<>();
    
    public SmppProcessor(SmppGwSession gwSession) {
        Parameters.checkNull(gwSession, "gwSession");
        this.gwSession = gwSession;
    }
    
    public void offerSmppEvent(SmppEvent event) {
        smppEvents.add(event);
    }
    
    public void offerMcIncomingEvent(McIncomingEvent event) {
        mcEvents.add(event);
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
