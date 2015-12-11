
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
    
    private Constants() {
        // no op. prevent instantiation.
    }
}
