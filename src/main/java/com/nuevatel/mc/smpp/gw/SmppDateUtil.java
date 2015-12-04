/**
 * 
 */
package com.nuevatel.mc.smpp.gw;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.smpp.gw.exception.InvalidSmppDateFormatException;

/**
 * @author Ariel Salazar
 *
 */
public final class SmppDateUtil {
    
    private SmppDateUtil() {
        // No op. Prevent instantiation
    }
    
    /**
     * @param time Time to format.
     * @return String representation for the SmppDatetime. <b>It is always in absolute format.</b>
     */
    public static String toSmppDatetime(ZonedDateTime time) {
        return null;
    }
    
    public static ZonedDateTime parseDateTime(ZonedDateTime now, String strDateTime) {
        Parameters.checkNull(now, "now");
        // check if it is absolute or relative
        if (strDateTime == null || strDateTime.length() == 0) {
            // return now
            return now;
        }
        
        if (strDateTime.length() != 16 && strDateTime.length() != 12) {
            throw new InvalidSmppDateFormatException(strDateTime);
        }
        
        boolean longFormat = strDateTime.length() == 16;
        if (longFormat && strDateTime.endsWith("R")) {
                // is realtive time
            Period gapPeriod = Period.of(Integer.parseInt(strDateTime.substring(0, 2)),
                                         Integer.parseInt(strDateTime.substring(2, 4)),
                                         Integer.parseInt(strDateTime.substring(4, 6)));
            Duration gapDuration = Duration.ofHours(Integer.parseInt(strDateTime.substring(6, 8)))
                                           .plusMinutes(Integer.parseInt(strDateTime.substring(8, 10)))
                                           .plusSeconds(Integer.parseInt(strDateTime.substring(10, 12)));
            return now.plus(gapPeriod).plus(gapDuration);
        }
        return ZonedDateTime.of(2000 + Integer.parseInt(strDateTime.substring(0, 2)),
                                Integer.parseInt(strDateTime.substring(2, 4)),
                                Integer.parseInt(strDateTime.substring(4, 6)), 
                                Integer.parseInt(strDateTime.substring(6, 8)), 
                                Integer.parseInt(strDateTime.substring(8, 10)), 
                                Integer.parseInt(strDateTime.substring(10, 12)), 
                                0,
                                ZoneId.of("UTC"));
    }
}
