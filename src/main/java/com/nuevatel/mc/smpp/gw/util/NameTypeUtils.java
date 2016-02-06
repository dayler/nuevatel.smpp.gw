
package com.nuevatel.mc.smpp.gw.util;

import com.nuevatel.mc.tpdu.TpAddress;

/**
 * <p>The NameTypeUtil class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Name type utils.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public final class NameTypeUtils {

    /**
     * Prevent instantiation.
     */
    private NameTypeUtils() {
        // No op
    }
    
    /**
     * Get name ton from smpp ton.
     * @param ton
     * @return
     */
    public static byte getNameTon(byte ton) {
        return (byte)((ton << 4) & TpAddress.TON);
    }
    
    /**
     * Get ton value used to dispatch through smpp from name type (ton >> 4 | npi).
     * @param type
     * @return
     */
    public static byte getSmppTon(byte type) {
        return (byte)((type & TpAddress.TON) >> 4);
    }
    
    /**
     * Get ton from smpp ton.
     * @param npi
     * @return
     */
    public static byte getNameNpi(byte npi) {
        return (byte)(npi & TpAddress.NPI);
    }
    
    /**
     * Get npi from smpp npi.
     * @param type
     * @return
     */
    public static byte getSmppNpi(byte type) {
        return (byte)(type & TpAddress.NPI);
    }
    
    /**
     * Get name type, based on smpp pdu.
     * @param ton
     * @param npi
     * @return
     */
    public static byte getNameType(byte ton, byte npi) {
        return (byte)((npi & TpAddress.NPI) | (ton << 4) & TpAddress.TON);
    }
}
