/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

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
    public void handleSmppEvent(SmppEvent ev) {
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
