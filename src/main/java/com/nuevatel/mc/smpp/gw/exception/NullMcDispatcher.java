
package com.nuevatel.mc.smpp.gw.exception;

import com.nuevatel.common.exception.OperationRuntimeException;

/**
 * 
 * <p>The NullMcDispatcher class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * McDispatcher not initialized.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class NullMcDispatcher extends OperationRuntimeException {
    
    private static final long serialVersionUID = 20151208L;
    
    /**
     * NullMcDispatcher constructor.
     */
    public NullMcDispatcher() {
        super("Null McDispatcher...");
    }
}
