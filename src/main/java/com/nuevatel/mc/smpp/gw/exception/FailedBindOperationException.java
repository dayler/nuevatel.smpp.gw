
package com.nuevatel.mc.smpp.gw.exception;

import org.smpp.pdu.BindResponse;

import com.nuevatel.common.exception.OperationRuntimeException;

/**
 * 
 * <p>The FailedBindOperationException class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Failed bind operation.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class FailedBindOperationException extends OperationRuntimeException {

    private static final long serialVersionUID = 20151203L;

    /**
     * FailedBindOperationException constructor.
     * @param nokResp
     */
    public FailedBindOperationException(BindResponse nokResp) {
        super("Failed bind operation. systemId="+ nokResp.getSystemId() + " commandStatus=" + nokResp.getCommandStatus()  + " response - " + nokResp.debugString());
    }
}
