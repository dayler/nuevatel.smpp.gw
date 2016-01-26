
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.Address;

/**
 * 
 * <p>The ReplaceSmEvent class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Replace message scheduled to delivery in the remote SMSC
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class ReplaceSmEvent extends SmppEvent {
    
    /* Private Variables */
    private Address sourceAddr;
    
    private String shortMessage;
    
    private String scheduleDeliveryTime = "";
    
    private String validityPeriod = "";
    
    private byte registeredDelivery = 0x0;
    
    private byte smDefaultMsgId = 0;
    
    /**
     * Creates an instance of ReplaceSmEvent.
     * @param messageId
     */
    public ReplaceSmEvent(long messageId) {
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
    
    /**
     * Get shortMessage.
     * @return
     */
    public String getShortMessage() {
        return shortMessage;
    }
    
    /**
     * Set shortMessage.
     * @param shortMessage
     */
    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }
    
    /**
     * Get scheduleDeliveryTime.
     * @return
     */
    public String getScheduleDeliveryTime() {
        return scheduleDeliveryTime;
    }
    
    /**
     * Set scheduleDeliveryTime.
     * @param scheduleDeliveryTime
     */
    public void setScheduleDeliveryTime(String scheduleDeliveryTime) {
        this.scheduleDeliveryTime = scheduleDeliveryTime;
    }
    /**
     * Get validityPeriod.
     * @return
     */
    public String getValidityPeriod() {
        return validityPeriod;
    }
    
    /**
     * Set validityPeriod.
     * @param validityPeriod
     */
    public void setValidityPeriod(String validityPeriod) {
        this.validityPeriod = validityPeriod;
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
     * Get smDefaultMsgId.
     * @return
     */
    public byte getSmDefaultMsgId() {
        return smDefaultMsgId;
    }
    
    /**
     * Set smDefaultMsgId.
     * @param smDefaultMsgId
     */
    public void setSmDefaultMsgId(byte smDefaultMsgId) {
        this.smDefaultMsgId = smDefaultMsgId;
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.ReplaceSmEvent;
    }
}
