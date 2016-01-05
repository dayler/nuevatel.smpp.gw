/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

import org.smpp.Data;
import org.smpp.ServerPDUEvent;

import com.nuevatel.mc.appconn.McMessage;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.mcdispatcher.McDispatcher;

/**
 * Life cycle -> init() handleEvent() ... execute()
 * 
 * @author Ariel Salazar
 *
 */
public abstract class Dialog {
    
    private int currentSequenceNumber = -1;
    
    protected long dialogId;
    
    protected int processorId;
    
    protected String smppMessageId = null;
    
    protected DialogService dialogService = AllocatorService.getDialogService();
    
    protected McDispatcher mcDispatcher = AllocatorService.getMcDispatcher();
    
    protected DialogState state = DialogState.created;
    
    protected int commandStatusCode = Data.ESME_ROK;
    
    /**
     * 
     * @param dialogId Dialog id to identify the dialog. It must to the messageId used in the MC local.
     * @param processorId Id to identify the processor at which belongs the dialog.
     */
    public Dialog(long dialogId, int processorId) {
        this.dialogId = dialogId;
        this.processorId = processorId;
    }
    
    public abstract void init();
    
    public abstract void handleSmppEvent(ServerPDUEvent ev);
    
    public abstract void handleMcMessage(McMessage msg);
    
    public abstract void execute();
    
    public long getDialogId() {
        return dialogId;
    }
    
    public void setSmppMessageId(String smppMessageId) {
        this.smppMessageId = smppMessageId;
    }
    
    public String getSmppMessageId() {
        return smppMessageId;
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
    
    public DialogState getState() {
        return state;
    }
}
