
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.Address;

/**
 * 
 * <p>The CancelSmEvent class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Define cancelSm event. Used to create CancelSm pdu, contains its definition.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class CancelSmEvent extends SmppEvent {

    /* Private variables */
    private String serviceType = "";
    
    private Address sourceAddr;
    
    private Address destAddr;

    /**
     * Creates an instance of <code>CancelSmEvent</code>.
     * @param messageId
     */
    public CancelSmEvent(long messageId) {
        setMessageId(messageId);
    }

    /**
     * Get serviceType
     * @return
     */
    public String getServiceType() {
        return serviceType;
    }
    
    /**
     * Set serviceType.
     * @param serviceType
     */
    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
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
    
    /**
     * Get destAddr.
     * @return
     */
    public Address getDestAddr() {
        return destAddr;
    }
    
    /**
     * Set destAddr
     * @param destAddr
     */
    public void setDestAddr(Address destAddr) {
        this.destAddr = destAddr;
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.CancelSmEvent;
    }
}
