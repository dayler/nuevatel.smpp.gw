
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.Address;

/**
 * 
 * <p>The DataSmEvent class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Model for <code>SubmitSm</code> pdu.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class DataSmEvent extends SmppEvent {
    
    /* Private variables */
    private String serviceType = "";
    
    private Address destAddr;
    
    private Address sourceAddr;
    
    private byte esmClass = 0x0;
    
    private byte registeredDelivery = 0x0;
    
    private byte dataCoding = 0;
    
    /**
     * Constructor for <code>DataSmEvent</code>.
     * @param messageId
     */
    public DataSmEvent(long messageId) {
        setMessageId(messageId);
    }
    
    /**
     * Get serviceType.
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
     * Get destAddr.
     * @return
     */
    public Address getDestAddr() {
        return destAddr;
    }
    
    /**
     * Set destAddr.
     * @param destAddr
     */
    public void setDestAddr(Address destAddr) {
        this.destAddr = destAddr;
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
     * Get esmClass.
     * @return
     */
    public byte getEsmClass() {
        return esmClass;
    }
    
    /**
     * Set esmClass.
     * @param esmClass
     */
    public void setEsmClass(byte esmClass) {
        this.esmClass = esmClass;
    }
    
    /**
     * Get registeredDelivery.
     * @return
     */
    public byte getRegisteredDelivery() {
        return registeredDelivery;
    }
    
    /**
     * Set registeredDelivery.
     * @param registeredDelivery
     */
    public void setRegisteredDelivery(byte registeredDelivery) {
        this.registeredDelivery = registeredDelivery;
    }
    
    /**
     * Get dataCoding.
     * @return
     */
    public byte getDataCoding() {
        return dataCoding;
    }
    
    /**
     * Set dataCoding.
     * @param dataCoding
     */
    public void setDataCoding(byte dataCoding) {
        this.dataCoding = dataCoding;
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.DataSmEvent;
    }
}
