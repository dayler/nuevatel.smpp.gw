/**
 * 
 */
package com.nuevatel.mc.smpp.gw.exception;

import com.nuevatel.common.exception.OperationRuntimeException;

/**
 * <code>DialogService<code> is not initialized.
 * 
 * @author Ariel Salazar
 */
public class NullDialogService extends OperationRuntimeException {

    private static final long serialVersionUID = 20151203L;

    public NullDialogService() {
        super("Null DialogService...");
    }

}
