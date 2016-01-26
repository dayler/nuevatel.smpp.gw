
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.Data;
import org.smpp.pdu.Address;

/**
 * 
 * <p>The DeliverSmEvent class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Model for <code>DeliverSm</code> pdu.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class DeliverSmEvent extends SmppEvent {
    
    /* Private variables */
    private byte esmClass;
    
    private Address sourceAddr;
    
    private Address destAddr;
    
    private byte[] data;
    
    private String encoding;
    
    private byte registeredDelivery = 0x0;
    
    private byte protocolId = Data.DFLT_PROTOCOLID;
    
    private String receiptedMessageId = "";
    
    private int commandStatus = Data.ESME_ROK;
    
    private boolean deliveryAck = false;
    
    /**
     * Constructor for <code>DeliverSmEvent</code>.
     * @param messageId
     */
    public DeliverSmEvent(long messageId) {
        setMessageId(messageId);
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
     * Set destAddr.
     * @param destAddr
     */
    public void setDestAddr(Address destAddr) {
        this.destAddr = destAddr;
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
     * Set receiptedMessageId.
     * @param receiptedMessageId
     */
    public void setReceiptedMessageId(String receiptedMessageId) {
        this.receiptedMessageId = receiptedMessageId;
    }
    
    /**
     * Get receiptedMessageId.
     * @return
     */
    public String getReceiptedMessageId() {
        return receiptedMessageId;
    }
    
    /**
     * Set commandStatus.
     * @param commandStatus
     */
    public void setCommandStatus(int commandStatus) {
        this.commandStatus = commandStatus;
    }
    
    /**
     * Get commandStatus.
     * @return
     */
    public int getCommandStatus() {
        return commandStatus;
    }
    
    /**
     * Get deliveryAck.
     * @return
     */
    public boolean isDeliveryAck() {
        return deliveryAck;
    }
    
    /**
     * Set deliveryAck.
     * @param deliveryAck
     */
    public void setDeliveryAck(boolean deliveryAck) {
        this.deliveryAck = deliveryAck;
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.DeliverSmEvent;
    }
}
