/**
 * 
 */
package com.nuevatel.mc.smpp.gw.exception;

import com.nuevatel.common.exception.OperationRuntimeException;

/**
 * @author Ariel Salazar
 *
 */
public class NullMcDispatcher extends OperationRuntimeException {
    
    private static final long serialVersionUID = 20151208L;
    
    public NullMcDispatcher() {
        super("Null McDispatcher...");
    }
}
