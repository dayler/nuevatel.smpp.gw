/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

/**
 * @author Ariel Salazar
 *
 */
public class SubmitSmDialog extends Dialog {

    public SubmitSmDialog(long messageId) {
        super(messageId);
        // TODO Auto-generated constructor stub
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
        return DialogType.submitSm;
    }

}
