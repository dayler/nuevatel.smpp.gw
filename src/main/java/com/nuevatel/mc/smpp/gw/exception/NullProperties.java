/**
 * 
 */
package com.nuevatel.mc.smpp.gw.exception;

import com.nuevatel.common.exception.OperationRuntimeException;

/**
 * INdicates the <code>SmppClientManager</code> is not initialized.
 * 
 * @author Ariel Salazar
 *
 */
public class NullProperties extends OperationRuntimeException {

    private static final long serialVersionUID = 20151128;
    
    public NullProperties() {
        super("Null app properties...");
    }
}
