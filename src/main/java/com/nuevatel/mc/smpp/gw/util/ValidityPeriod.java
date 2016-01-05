/**
 * 
 */
package com.nuevatel.mc.smpp.gw.util;

import java.time.Duration;
import java.time.LocalDateTime;

import com.nuevatel.common.util.Parameters;

/**
 * @author Ariel Salazar
 *
 */
public final class ValidityPeriod {
    
    private ValidityPeriod() {
        // No op
    }
    
    /**
     * 
     * @param tpVp
     * @param defaultVp
     * @param refTime
     * @return time in seconds after which dialog is invalidated.
     */
    public static long resolveExpireAfterWriteTime(LocalDateTime tpVp, long defaultVp, LocalDateTime refTime) {
        Parameters.checkNull(refTime, "refTime");
        
        if (tpVp == null || tpVp.equals(refTime) || tpVp.isBefore(refTime)) {
            return defaultVp;
        }
        // get duration between ...
        Duration duration = Duration.between(refTime, tpVp);
        if (duration.getSeconds() > 0) {
            return duration.getSeconds();
        } else {
            return defaultVp;
        }
    }
}
