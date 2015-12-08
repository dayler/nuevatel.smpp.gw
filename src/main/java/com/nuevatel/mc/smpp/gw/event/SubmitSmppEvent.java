/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.Data;
import org.smpp.pdu.Address;

import com.nuevatel.mc.appconn.ForwardSmOCall;

/**
 * 

import com.nuevatel.common.util.Parameters;
del for a message to send by local SMSC (MC)
 * 
 * @author Ariel Salazar
 *
 */
public class SubmitSmppEvent extends SmppEvent {
    
    private Address destAddr;
    
    private Address sourceAddr;
    
    private byte replaceIfPresentFlag = 0x0;
    
    private String shortMessage = "";
    
    private String encoding = Data.ENC_GSM7BIT;

    private String scheduleDeliveryTime = "";
    
    private String validityPeriod = "";
    
    private byte esmClass = 0x0;
    
    private byte protocolId = 0x0;
    
    private byte priorityFlag = 0x0;
    
    private byte registeredDelivery = 0x0;
    
    private byte dataCoding = 0x0;
    
    private byte smDefaultMsgId = 0x0;
    
    public SubmitSmppEvent(long messageId) {
        setMessageId(messageId);
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

    public byte getReplaceIfPresentFlag() {
        return replaceIfPresentFlag;
    }

    public void setReplaceIfPresentFlag(byte replaceIfPresentFlag) {
        this.replaceIfPresentFlag = replaceIfPresentFlag;
    }

    public String getShortMessage() {
        return shortMessage;
    }

    public void setShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
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

    public byte getEsmClass() {
        return esmClass;
    }

    public void setEsmClass(byte esmClass) {
        this.esmClass = esmClass;
    }

    public byte getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(byte protocolId) {
        this.protocolId = protocolId;
    }

    public byte getPriorityFlag() {
        return priorityFlag;
    }

    public void setPriorityFlag(byte priorityFlag) {
        this.priorityFlag = priorityFlag;
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
        return SmppEventType.SubmitSmEvent;
    }
    
    public static SubmitSmppEvent fromForwardSmOCall(ForwardSmOCall fwsmoCall) {
        return null;
    }
}
