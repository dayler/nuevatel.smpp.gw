
package com.nuevatel.mc.smpp.gw.util;

import java.time.Duration;
import java.time.LocalDateTime;

import com.nuevatel.common.util.Parameters;

/**
 * 
 * <p>The ValidityPeriod class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * ValidityPeriod general class utilities.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public final class ValidityPeriod {
    
    /**
     * ValidityPeriod constructor.
     */
    private ValidityPeriod() {
        // No op
    }
    
    /**
     * Get in seconds validity period.
     * 
     * @param tpVp
     * @param defaultVp
     * @param refTime
     * @return time 
     */
    public static long resolveExpireAfterWriteTime(LocalDateTime tpVp, long defaultVp, LocalDateTime refTime) {
        Parameters.checkNull(refTime, "refTime");
        
        if (tpVp == null || tpVp.equals(refTime) || tpVp.isBefore(refTime)) return defaultVp;
        // get duration between ...
        Duration duration = Duration.between(refTime, tpVp);
        if (duration.getSeconds() > 0) return duration.getSeconds();
        else return defaultVp;
    }
}
