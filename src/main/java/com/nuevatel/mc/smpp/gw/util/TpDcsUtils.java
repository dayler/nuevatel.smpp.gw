
package com.nuevatel.mc.smpp.gw.util;

import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.smpp.gw.Constants;
import com.nuevatel.mc.tpdu.TpDcs;

/**
 * 
 * <p>The TpDcsUtils class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * General purpose utilities for TpDcs.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public final class TpDcsUtils {
    /**
     * To prevent instantiation.
     */
    private TpDcsUtils() {
        // No op.
    }
    
    /**
     * Get smpp string representation.
     * @param charSet
     * @return From TpDcs 
     */
    public static String resolveSmppEncoding(byte charSet) {
        switch (charSet) {
        case TpDcs.CS_GSM7:
            return Constants.CS_GSM7;
        case TpDcs.CS_UCS2:
            return Constants.CS_UTF_16BE;
        case TpDcs.CS_8_BIT:
            return "";
        default:
            return Constants.CS_GSM7;
        }
    }
    
    /**
     * Based on smppEncoding (from smpp pdu) get corresponding <code>TpDcs</code>. <code>TpDcs</code> to corresponds with smppEncoding. <b>By default
     * return TpDcs.CS_GSM7.</b>
     * 
     * @param smppEncoding 
     * @return 
     */
    public static TpDcs resolveTpDcs(String smppEncoding) {
        if (StringUtils.isEmptyOrNull(smppEncoding)) {
            return new TpDcs(TpDcs.CS_GSM7);
        }
        
        switch (smppEncoding) {
        case Constants.CS_GSM7:
            return new TpDcs(TpDcs.CS_GSM7);
        case Constants.CS_UTF_16BE:
        case Constants.CS_UCS_2:
            return new TpDcs(TpDcs.CS_UCS2);
        default:
            return new TpDcs(StringUtils.isEmptyOrNull(smppEncoding) ? TpDcs.CS_8_BIT : TpDcs.CS_GSM7);
        }
    }
    
    /**
     * Based on smppEncoding (from smpp pdu) get corresponding <code>TpDcs</code>. <code>TpDcs</code> to corresponds with smppEncoding. <b>By default
     * return TpDcs.CS_GSM7</b>.
     * 
     * @param smppEncoding
     * @return 
     */
    public static TpDcs resolveTpDcs(byte charSet) {
        switch (charSet) {
        case TpDcs.CS_GSM7:
            return new TpDcs(TpDcs.CS_GSM7);
        case TpDcs.CS_UCS2:
            return new TpDcs(TpDcs.CS_UCS2);
        default:
            return new TpDcs(TpDcs.CS_UCS2);
        }
    }
}
