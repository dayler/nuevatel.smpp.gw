/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.Data;
import org.smpp.pdu.Address;

import com.nuevatel.mc.appconn.ForwardSmOCall;
import com.nuevatel.mc.smpp.gw.Constants;
import com.nuevatel.mc.tpdu.SmsDeliver;
import com.nuevatel.mc.tpdu.TpDcs;
import com.nuevatel.mc.tpdu.TpUd;

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
    
    public static SubmitSmppEvent fromSmsStatusReport(ForwardSmOCall fwsmoCall) {
        return null;
    }
    
    public static SubmitSmppEvent fromSmsDelivery(ForwardSmOCall fwsmoCall) throws Exception {
        SmsDeliver smsDeliver = new SmsDeliver(fwsmoCall.getTpdu());
        SubmitSmppEvent event = new SubmitSmppEvent(fwsmoCall.getMessageId());
        Address sourceAddr = new Address(smsDeliver.getTpOa().getTon(), smsDeliver.getTpOa().getNpi(), smsDeliver.getTpOa().getAddress());
        event.setSourceAddr(sourceAddr);
        // TODO
        Address destAddr = new Address(smsDeliver.getTpOa().getTon(), smsDeliver.getTpOa().getNpi(), smsDeliver.getTpOa().getAddress());
        event.setDestAddr(destAddr);
        TpUd tpUd = new TpUd(smsDeliver.getTpUdhi(), smsDeliver.getTpDcs(), smsDeliver.getTpUdl(), smsDeliver.getTpdu());
        String encoding;
        switch (tpUd.getCharSet()) {
        case TpDcs.CS_GSM7:
            encoding = Constants.CS_GSM7;
            break;
        case TpDcs.CS_UCS2:
            encoding = Constants.CS_UTF_16BE;
            break;
        case TpDcs.CS_8_BIT:
            encoding = "";
            break;
        default:
            encoding = "X-Gsm7Bit";
            break;
        }
        event.setEncoding(encoding);
        // If gsm7 sm = tpud + headers. ucs2 = only text
        event.setData(tpUd.getSm());
        // TODO pending to add schedule delivery time and validity period
        event.setScheduleDeliveryTime(Data.DFLT_SCHEDULE);
        event.setValidityPeriod(Data.DFLT_VALIDITY);
        byte esmClass = Data.DFLT_ESM_CLASS;
        if (smsDeliver.getTpUdhi()) {
            esmClass |= Data.SM_UDH_GSM;
        }
        if (smsDeliver.getTpRp()) {
            esmClass |= Data.SM_REPLY_PATH_GSM;
        }
        event.setEsmClass(esmClass);
        
        return null;
    }
}
