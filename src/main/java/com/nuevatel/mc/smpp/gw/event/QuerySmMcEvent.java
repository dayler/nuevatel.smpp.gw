/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.Address;

/**
 * Model for query sm message
 * 
 * @author Ariel Salazar
 *
 */
public class QuerySmMcEvent extends McIncomingEvent {

    private Address sourceAddr;

    public QuerySmMcEvent(long messageId) {
        super(messageId);
    }
    
    public Address getSourceAddr() {
        return sourceAddr;
    }

    public void setSourceAddr(Address sourceAddr) {
        this.sourceAddr = sourceAddr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public McEventType type() {
        return McEventType.QuerySmMcEvent;
    }

}
