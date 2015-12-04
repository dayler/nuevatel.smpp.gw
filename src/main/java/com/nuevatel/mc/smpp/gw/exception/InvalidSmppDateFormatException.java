/**
 * 
 */
package com.nuevatel.mc.smpp.gw.exception;

import com.nuevatel.common.exception.OperationRuntimeException;

/**
 * Invalid smpp date format.<br/>
 * <br/>
 * In this interface, all time/date related fields will be in ASCII with the following format: “YYMMDDhhmmsstnnp” where <br/><br/>
 * 
 * ‘YY’        last two digits of the year (00-99)<br/>
 * ‘MM’        month (01-12)<br/>
 * ‘DD’        day (01-31)<br/>
 * ‘hh’        hour (00-23)<br/>
 * ‘mm’        minute (00-59)<br/>
 * ‘ss’        second (00-59)<br/>
 * ‘t’         tenths of second (0-9)<br/>
 * ‘nn’        Time difference in quarter hours between local time (as expressed in the first 13 octets) and UTC (Universal Time Constant) time (00-48).<br/>
 * ‘p’ - “+”   Local time is in quarter hours advanced in relation to UTC time.<br/>
 * “-”         Local time is in quarter hours retarded in relation to UTC time.<br/>
 * “R”         Local time is relative to the current SMSC time.<br/><br/>
 * 
 * Note:<br/>
 * Where responses are reported by the SMSC the local time of the SMSC will be given, and the format will be “YYMMDDhhmmss”, with the same definitions
 * as above.<br/>
 * 
 * @author Ariel Salazar
 *
 */
public class InvalidSmppDateFormatException extends OperationRuntimeException {

    private static final long serialVersionUID = 20151204L;

    public InvalidSmppDateFormatException(String s) {
        super("Date string incorrect length: " + s);
    }
}
