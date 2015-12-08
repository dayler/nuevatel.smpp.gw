/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.Address;

/**
 * cancel request from SMSC
 * 
 * @author asalazar
 *
 */
public class CancelSmEvent extends SmppEvent {

    private String serviceType = "";
    
    private Address sourceAddr;
    
    private Address destAddr;

    public CancelSmEvent(long messageId) {
        setMessageId(messageId);
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Address getSourceAddr() {
        return sourceAddr;
    }

    public void setSourceAddr(Address sourceAddr) {
        this.sourceAddr = sourceAddr;
    }

    public Address getDestAddr() {
        return destAddr;
    }

    public void setDestAddr(Address destAddr) {
        this.destAddr = destAddr;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SmppEventType type() {
        return SmppEventType.CancelSmEvent;
    }
}
