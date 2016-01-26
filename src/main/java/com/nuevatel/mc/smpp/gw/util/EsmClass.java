
package com.nuevatel.mc.smpp.gw.util;

import org.smpp.Data;

/**
 * 
 * <p>The EsmClass class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Model for smpp EsmClass.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class EsmClass {
    
    /* Private variables */
    /*
     * ESME -> SMSC. Store and forward mode.
     */
    private boolean storeAndForward;
    
    /*
     * ESME -> SMSC: Short message contains ESME delivery acknowledgement.
     * <br/>
     * SMSC -> ESME: Short message contains SME delivery acknowledgement.
     */
    private boolean deliveryAck;
    
    /*
     * GSM User Data Header Indicator.
     */
    private boolean udhi;
    
    /*
     * GSM Reply Path.
     */
    private boolean replyPath;
    
    /*
     * EsmClass
     */
    private byte esmClass;
    
    /**
     * EsmClass constructor with byte esmClass.
     * @param esmClass
     */
    public EsmClass(byte esmClass) {
        this.esmClass = esmClass;
        replyPath = (esmClass & Data.SM_REPLY_PATH_GSM) == Data.SM_REPLY_PATH_GSM;
        udhi = (esmClass & Data.SM_UDH_GSM) == Data.SM_UDH_GSM;
        storeAndForward = (esmClass & Data.SM_STORE_FORWARD_MODE) == Data.SM_STORE_FORWARD_MODE;
        deliveryAck = (esmClass & Data.SM_ESME_DLV_ACK_TYPE) == Data.SM_ESME_DLV_ACK_TYPE;
    }
    
    /**
     * EsmClass constructor with <code>deliverAck</code>, <code>udhi</code> and <code>replyPath</code>.
     * @param deliveryAck
     * @param udhi
     * @param replyPath
     */
    public EsmClass(boolean deliveryAck, boolean udhi, boolean replyPath) {
        this(resolveEsmClass(deliveryAck, udhi, replyPath));
    }

    /**
     * Get byte representation for esmClass.
     * @param deliveryAck
     * @param udhi
     * @param replyPath
     * @return
     */
    private static byte resolveEsmClass(boolean deliveryAck, boolean udhi, boolean replyPath) {
        return (byte)(Data.SM_ESM_DEFAULT// default message mode
               | (byte)(deliveryAck ? Data.SM_ESME_DLV_ACK_TYPE : Data.SM_ESM_DEFAULT) // message type
               | (byte)(udhi ? Data.SM_UDH_GSM : Data.SM_ESM_DEFAULT) // gsm
               | (byte)(replyPath ? Data.SM_REPLY_PATH_GSM : Data.SM_ESM_DEFAULT)); // gsm
    }
    
    /**
     * Get storeAndForward.
     * @return
     */
    public boolean getStoreAndForward() {
        return storeAndForward;
    }

    /**
     * Get deliveryAck.
     * @return
     */
    public boolean getDeliveryAck() {
        return deliveryAck;
    }

    /**
     * Get udhi.
     * @return
     */
    public boolean getUdhi() {
        return udhi;
    }

    /**
     * Get replyPath.
     * @return
     */
    public boolean getReplyPath() {
        return replyPath;
    }

    /**
     * Get esmClass.
     * @return
     */
    public byte getValue() {
        return esmClass;
    }
}
