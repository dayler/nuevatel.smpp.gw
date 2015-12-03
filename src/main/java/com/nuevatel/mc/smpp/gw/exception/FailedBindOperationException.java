/**
 * 
 */
package com.nuevatel.mc.smpp.gw.exception;

import org.smpp.pdu.BindResponse;

import com.nuevatel.common.exception.OperationRuntimeException;

/**
 * Failed bind operation
 * 
 * @author Ariel Salazar
 *
 */
public class FailedBindOperationException extends OperationRuntimeException {

    private static final long serialVersionUID = 20151203L;

    public FailedBindOperationException(BindResponse nokResp) {
        super("Failed bind operation. systemId="+ nokResp.getSystemId() + " commandStatus=" + nokResp.getCommandStatus()  + " response - " + nokResp.debugString());
    }
}
