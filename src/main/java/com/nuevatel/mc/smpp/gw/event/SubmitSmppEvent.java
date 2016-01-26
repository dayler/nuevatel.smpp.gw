
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.Data;
import org.smpp.pdu.Address;

/**
 * 
 * <p>The SubmitSmppEvent class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Model for <code>SubmitSm</code> pdu.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class SubmitSmppEvent extends SmppEvent {
    /* Private variables */
    private Address destAddr;
    
    private Address sourceAddr;
    
    private byte replaceIfPresentFlag = 0x0;
    
    private byte[] data;
    
    private String encoding = Data.ENC_GSM7BIT;

    private String scheduleDeliveryTime = "";
    
    private String validityPeriod = "";
    
    private byte esmClass = 0x0;
    
    private byte protocolId = 0x0;
    
    private byte priorityFlag = 0x0;
    
    private byte registeredDelivery = 0x0;
    
    private byte dataCoding = 0x0;
    
    private byte smDefaultMsgId = 0x0;
    
    /**
     * Creates instance of <code>SubmitSmppEvent</code>.
     * @param messageId
     */
    public SubmitSmppEvent(long messageId) {
        setMessageId(messageId);
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
     * Get replaceIfPresentFlag.
     * @return
     */
    public byte getReplaceIfPresentFlag() {
        return replaceIfPresentFlag;
    }
    
    /**
     * Set replaceIfPresentFlag.
     * @param replaceIfPresentFlag
     */
    public void setReplaceIfPresentFlag(byte replaceIfPresentFlag) {
        this.replaceIfPresentFlag = replaceIfPresentFlag;
    }
    
    /**
     * Get data.
     * @return
     */
    public byte[] getData() {
        return data;
    }
    
    /**
     * Set data.
     * @param data
     */
    public void setData(byte[] data) {
        this.data = data;
    }
    
    /**
     * Get encoding.
     * @return
     */
    public String getEncoding() {
        return encoding;
    }
    
    /**
     * Set encoding.
     * @param encoding
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
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
     * Get protocolId.
     * @return
     */
    public byte getProtocolId() {
        return protocolId;
    }
    
    /**
     * Set protocolId.
     * @param protocolId
     */
    public void setProtocolId(byte protocolId) {
        this.protocolId = protocolId;
    }
    
    /**
     * Get priorityFlag.
     * @return
     */
    public byte getPriorityFlag() {
        return priorityFlag;
    }
    
    /**
     * Set priorityFlag.
     * @param priorityFlag
     */
    public void setPriorityFlag(byte priorityFlag) {
        this.priorityFlag = priorityFlag;
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
        return SmppEventType.SubmitSmEvent;
    }
}
