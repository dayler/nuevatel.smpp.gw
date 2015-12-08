/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

import org.smpp.ServerPDUEvent;

import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.mcdispatcher.McDispatcher;

/**
 * @author Ariel Salazar
 *
 */
public class SubmitSmDialog extends Dialog {
    
    public SubmitSmDialog(long messageId, int processorId) {
        super(messageId, processorId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        // Create SubmitSm
        
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
        return DialogType.submitSm;
    }

    @Override
    public void execute() {
        // TODO Auto-generated method stub
        
    }

}
