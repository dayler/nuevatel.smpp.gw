/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

import org.smpp.ServerPDUEvent;

/**
 * @author Ariel Salazar
 *
 */
public class DeliverSmDialog extends Dialog {

    public DeliverSmDialog(long messageId) {
        super(messageId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleSmppEvent(ServerPDUEvent ev) {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DialogType getType() {
        return DialogType.deliverSm;
    }
}
