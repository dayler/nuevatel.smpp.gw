/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.Data;
import org.smpp.pdu.Address;

/**
 * @author Ariel Salazar
 *
 */
public class DeliverSmEvent extends SmppEvent {
    
    private byte esmClass;
    
    private Address sourceAddr;
    
    private Address destAddr;
    
    private byte[] data;
    
    private String encoding;
    
    private byte registeredDelivery = 0x0;
    
    private byte protocolId = Data.DFLT_PROTOCOLID;
    
    private String receiptedMessageId = "";
    
    private int commandStatus = Data.ESME_ROK;
    
    public DeliverSmEvent(long messageId) {
        setMessageId(messageId);
    }
    
    public byte getEsmClass() {
        return esmClass;
    }
    
    public void setEsmClass(byte esmClass) {
        this.esmClass = esmClass;
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
    
    public byte[] getData() {
        return data;
    }
    
    public void setData(byte[] data) {
        this.data = data;
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
    
    public byte getRegisteredDelivery() {
        return registeredDelivery;
    }
    
    public void setRegisteredDelivery(byte registeredDelivery) {
        this.registeredDelivery = registeredDelivery;
    }
    
    public byte getProtocolId() {
        return protocolId;
    }
    
    public void setProtocolId(byte protocolId) {
        this.protocolId = protocolId;
    }
    
    public void setReceiptedMessageId(String receiptedMessageId) {
        this.receiptedMessageId = receiptedMessageId;
    }
    
    public String getReceiptedMessageId() {
        return receiptedMessageId;
    }
    
    public void setCommandStatus(int commandStatus) {
        this.commandStatus = commandStatus;
    }
    
    public int getCommandStatus() {
        return commandStatus;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public SmppEventType type() {
        return SmppEventType.DeliverSmEvent;
    }

}
