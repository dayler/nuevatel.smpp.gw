/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

import com.nuevatel.mc.smpp.gw.AllocatorService;

/**
 * Life cycle -> init() handleEvent() ... execute()
 * 
 * @author Ariel Salazar
 *
 */
public abstract class Dialog {
    
    protected long dialogId;
    
    protected DialogService dialogService = AllocatorService.getDialogService();
    
    private int currentSequenceNumber = -1;
    
    public Dialog(long dialogId) {
        this.dialogId = dialogId;
    }
    
    public abstract void init();
    
    public abstract void handleSmppEvent(SmppEvent ev); 
    
    public void execute() {
        // No op
    }
    
    public long getDialogId() {
        return dialogId;
    }
    
    public void setCurrentSequenceNumber(int currentSequenceNumber) {
        this.currentSequenceNumber = currentSequenceNumber;
    }
    
    public int getCurrentSequenceNumber() {
        return currentSequenceNumber;
    }
    
    public abstract DialogType getType();

    protected void invalidate() {
        dialogService.invalidate(this);
    }
}
