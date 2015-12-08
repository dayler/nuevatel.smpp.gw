/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.Address;

/**
 * Replace message scheduled to delivery in the remote SMSC
 * 
 * @author asalazar
 *
 */
public class ReplaceSmEvent extends SmppEvent {
    
    private Address sourceAddr;
    
    private String shortMessage;
    
    private String scheduleDeliveryTime = "";
    
    private String validityPeriod = "";
    
    private byte registeredDelivery = 0x0;
    
    private byte smDefaultMsgId = 0;
    
    public ReplaceSmEvent(long messageId) {
        setMessageId(messageId);
    }
    
    public Address getSourceAddr() {
        return sourceAddr;
    }

    public void setSourceAddr(Address sourceAddr) {
        this.sourceAddr = sourceAddr;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }

    public String getScheduleDeliveryTime() {
        return scheduleDeliveryTime;
    }

    public void setScheduleDeliveryTime(String scheduleDeliveryTime) {
        this.scheduleDeliveryTime = scheduleDeliveryTime;
    }

    public String getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(String validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public byte getRegisteredDelivery() {
        return registeredDelivery;
    }

    public void setRegisteredDelivery(byte registeredDelivery) {
        this.registeredDelivery = registeredDelivery;
    }

    public byte getSmDefaultMsgId() {
        return smDefaultMsgId;
    }

    public void setSmDefaultMsgId(byte smDefaultMsgId) {
        this.smDefaultMsgId = smDefaultMsgId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SmppEventType type() {
        return SmppEventType.ReplaceSmEvent;
    }

}
