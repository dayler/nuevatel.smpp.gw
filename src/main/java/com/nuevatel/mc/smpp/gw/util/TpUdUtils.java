/**
 * 
 */
package com.nuevatel.mc.smpp.gw.util;

import com.nuevatel.mc.tpdu.TpDcs;

/**
 * @author Ariel Salazar
 *
 */
public final class TpUdUtils {

    private TpUdUtils() {
        // no op
    }
    
    /**
     * 
     * @return tpudl for tpud.
     */
    public static byte resolveTpUdl(byte charSet, byte[] data) {
        return (byte)(TpDcs.CS_UCS2 == charSet ? data.length << 1 : data.length);
    }
}
