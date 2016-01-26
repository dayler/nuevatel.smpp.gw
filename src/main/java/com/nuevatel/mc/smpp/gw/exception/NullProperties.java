
package com.nuevatel.mc.smpp.gw.exception;

import com.nuevatel.common.exception.OperationRuntimeException;

/**
 * 
 * <p>The NullProperties class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Configuration properties was not loaded.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class NullProperties extends OperationRuntimeException {

    private static final long serialVersionUID = 20151128;
    
    /**
     * NullProperties constructor.
     */
    public NullProperties() {
        super("Null app properties...");
    }
}
