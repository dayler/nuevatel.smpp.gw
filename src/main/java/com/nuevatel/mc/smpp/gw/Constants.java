
/**
 * 
 */
package com.nuevatel.mc.smpp.gw;

/**
 * @author Ariel Salazar
 *
 */
public final class Constants {
    
    public static final String CS_GSM7 = "X-Gsm7Bit";
    public static final String CS_UCS2 = "UTF-16BE";
    
    /**
     * Timeout (in milliseconds) to poll event from queue.
     */
    public static final long TIMEOUT_POLL_EVENT_QUEUE = 500L;
    
    public static final long TIMEOUT_OFFER_EVENT_QUEUE = 500L;
    
    /**
     * ESM_CLASS message mode mask (1-0)
     */
    public static final int SM_MESSAGE_MODE_MASK = 0x03;
    /**
     * ESM_CLASS message type mask (5-2)
     */
    public static final int SM_MESSAGE_TYPE_MASK = 0x3c;
    /**
     * ESM_CLASS GSM network specific features (7-6)
     */
    public static final int SM_GSM_MASK = 0Xc0;
    
    /**
     * For SmppClientProcessor/SmppServerProcessor indicates the SmppEvent is not allowed to dispatch.
     */
    public static final int DISPATCH_EV_SOURCE_NOT_ALLOWED = 1;
    
    /**
     * For SmppClientProcessor/SmppServerProcessor indicates the SmppEvent was scheduled to dispatch.
     */
    public static final int DISPATCH_EV_OK = 0;
    
    /**
     * TP-Message-Reference
     */
    public static final byte TP_DFLT_MR = 0;
    public static final byte TP_DFLT_PI = 0;
    
    private Constants() {
        // no op. prevent instantiation.
    }
}
