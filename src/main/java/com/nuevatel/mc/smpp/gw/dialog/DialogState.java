
package com.nuevatel.mc.smpp.gw.dialog;

/**
 * 
 * <p>The DialogState class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Define the possible states for a dialog.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
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
     * Dialog is awaiting by resp/req(delivery recipient).
     */
    awaiting_0,
    awaiting_1,
    awaiting_2,
    /**
     * Dialog is forwarding to next step.
     */
    forward,
    /**
     * Dialog in close state. All process was succeeded.
     */
    close,
    /**
     * Dialog has a controlled fail.
     */
    failed,
    ;
}
