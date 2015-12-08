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
public class QuerySmEvent extends SmppEvent {

    private Address sourceAddr;

    public QuerySmEvent(long messageId) {
        setMessageId(messageId);
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
    public SmppEventType type() {
        return SmppEventType.QuerySmEvent;
    }

}
