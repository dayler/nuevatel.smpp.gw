/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.Address;

/**
 * 
 * <p>The QuerySmEvent class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Model for query sm message
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class QuerySmEvent extends SmppEvent {

    /* Private variables */
    private Address sourceAddr;

    /**
     * Constructor for QuerySmEvent.
     * @param messageId
     */
    public QuerySmEvent(long messageId) {
        setMessageId(messageId);
    }
    
    /**
     * Get sourceAddr.
     * @return
     */
    public Address getSourceAddr() {
        return sourceAddr;
    }
    
    /**
     * Set sourceAddr.
     * @param sourceAddr
     */
    public void setSourceAddr(Address sourceAddr) {
        this.sourceAddr = sourceAddr;
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.QuerySmEvent;
    }
}
