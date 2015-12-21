/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

/**
 * State for dialogs.
 * 
 * @author Ariel Salazar
 *
 */
public enum DialogState {
    /**
     * Dialog created, before init.
     */
    created,
    /**
     * {@link Dialog#init()} executed, it does not indicates that the method was successful.
     */
    init,
    /**
     * Dialog in close state. All process was succeeded.
     */
    close,
    /**
     * Dialog has a controled fail.
     */
    failed,
    ;
}
