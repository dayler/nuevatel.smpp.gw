/**
 * 
 */
package com.nuevatel.mc.smpp.gw.util;

import org.smpp.Data;

/**
 * @author Ariel Salazar
 *
 */
public class EsmClass {
    
    /**
     * ESME -> SMSC. Store and forward mode.
     */
    private boolean storeAndForward;
    
    /**
     * ESME -> SMSC: Short message contains ESME delivery acknowledgement.
     * <br/>
     * SMSC -> ESME: Short message contains SME delivery acknowledgement.
     */
    private boolean deliveryAck;
    
    /**
     * GSM User Data Header Indicator.
     */
    private boolean udhi;
    
    /**
     * GSM Reply Path.
     */
    private boolean replyPath;
    
    /**
     * EsmClass
     */
    private byte esmClass;
    
    public EsmClass(byte esmClass) {
        this.esmClass = esmClass;
        replyPath = (esmClass & Data.SM_REPLY_PATH_GSM) == Data.SM_REPLY_PATH_GSM;
        udhi = (esmClass & Data.SM_UDH_GSM) == Data.SM_UDH_GSM;
        storeAndForward = (esmClass & Data.SM_STORE_FORWARD_MODE) == Data.SM_STORE_FORWARD_MODE;
        deliveryAck = (esmClass & Data.SM_ESME_DLV_ACK_TYPE) == Data.SM_ESME_DLV_ACK_TYPE;
    }
    
    public EsmClass(boolean deliveryAck, boolean udhi, boolean replyPath) {
        this(resolveEsmClass(deliveryAck, udhi, replyPath));
    }

    private static byte resolveEsmClass(boolean deliveryAck, boolean udhi, boolean replyPath) {
        return (byte)(Data.SM_ESM_DEFAULT// default message mode
               | (byte)(deliveryAck ? Data.SM_ESME_DLV_ACK_TYPE : Data.SM_ESM_DEFAULT) // message type
               | (byte)(udhi ? Data.SM_UDH_GSM : Data.SM_ESM_DEFAULT) // gsm
               | (byte)(replyPath ? Data.SM_REPLY_PATH_GSM : Data.SM_ESM_DEFAULT)); // gsm
    }
    
    public boolean getStoreAndForward() {
        return storeAndForward;
    }

    public boolean getDeliveryAck() {
        return deliveryAck;
    }

    public boolean getUdhi() {
        return udhi;
    }

    public boolean isReplyPath() {
        return replyPath;
    }

    public byte getValue() {
        return esmClass;
    }
}
