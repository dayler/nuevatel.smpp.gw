/**
 * 
 */
package com.nuevatel.mc.smpp.gw;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.smpp.gw.exception.InvalidSmppDateFormatException;

/**
 * 
 * <p>The SmppDateUtil class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Converts MC date format to Smpp date format and Smpp date format to MC date format.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public final class SmppDateUtil {
    
    private static DateTimeFormatter format = DateTimeFormatter.ofPattern("YYMMddHHmmss");
    
    /**
     * Prevent instantiation.
     */
    private SmppDateUtil() {
        // No op. Prevent instantiation
    }
    
    /**
     * Gets String representation for the SmppDatetime. <b>It is always in absolute format.</b>
     * 
     * @param time
     * @return 
     */
    public static String toSmppDatetime(LocalDateTime time) {
        return format.format(time) + "000+";
    }
    
    /**
     * Gets string representation for the SmppDatetime. <b>It is always in absolute format.</b>
     * 
     * @param time
     * @return 
     */
    public static String toSmppDatetime(ZonedDateTime time) {
        return format.format(time) + "000+";
    }
    
    /**
     * Parse smpp string date time, to <code>ZonedDateTime</code>.
     * 
     * @param now
     * @param strDateTime
     * @return
     */
    public static ZonedDateTime parseDateTime(ZonedDateTime now, String strDateTime) {
        Parameters.checkNull(now, "now");
        // check if it is absolute or relative
        if (strDateTime == null || strDateTime.length() == 0) return now; // return now
        // check validity length
        if (strDateTime.length() != 16 && strDateTime.length() != 12) throw new InvalidSmppDateFormatException(strDateTime);
        
        boolean longFormat = strDateTime.length() == 16;
        if (longFormat && strDateTime.endsWith("R")) {
            // is relative time
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
                                ZoneId.systemDefault());
    }
}
