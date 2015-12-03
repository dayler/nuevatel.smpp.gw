/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.Address;

/**
 * Data sm message.
 * 
 * @author asalazar
 *
 */
public class DataSmMcEvent extends McIncomingEvent {

    private String serviceType = "";
    
    private Address destAddr;
    
    private Address sourceAddr;
    
    private byte esmClass = 0x0;
    
    private byte registeredDelivery = 0x0;
    
    private byte dataCoding = 0;

    public DataSmMcEvent(long messageId) {
        super(messageId);
    }
    
    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Address getDestAddr() {
        return destAddr;
    }

    public void setDestAddr(Address destAddr) {
        this.destAddr = destAddr;
    }

    public Address getSourceAddr() {
        return sourceAddr;
    }

    public void setSourceAddr(Address sourceAddr) {
        this.sourceAddr = sourceAddr;
    }

    public byte getEsmClass() {
        return esmClass;
    }

    public void setEsmClass(byte esmClass) {
        this.esmClass = esmClass;
    }

    public byte getRegisteredDelivery() {
        return registeredDelivery;
    }

    public void setRegisteredDelivery(byte registeredDelivery) {
        this.registeredDelivery = registeredDelivery;
    }

    public byte getDataCoding() {
        return dataCoding;
    }

    public void setDataCoding(byte dataCoding) {
        this.dataCoding = dataCoding;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public McEventType type() {
        return McEventType.DataSmMcEvent;
    }
}
