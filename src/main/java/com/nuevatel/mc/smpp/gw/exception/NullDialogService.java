
package com.nuevatel.mc.smpp.gw.exception;

import com.nuevatel.common.exception.OperationRuntimeException;

/**
 * 
 * <p>The NullDialogService class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 *  <code>DialogService<code> is not initialized.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class NullDialogService extends OperationRuntimeException {

    private static final long serialVersionUID = 20151203L;

    /**
     * NullDialogService constructor.
     */
    public NullDialogService() {
        super("Null DialogService...");
    }

}
