
package com.nuevatel.mc.smpp.gw.dialog;

import org.smpp.Data;
import org.smpp.ServerPDUEvent;

import com.nuevatel.mc.appconn.McMessage;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.mcdispatcher.McDispatcher;

/**
 * 
 * <p>The Dialog class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Represents a transaction between MC and SMPP entity.
 * <br/>
 * Life cycle -> init() handleEvent() ... execute()
 * 
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public abstract class Dialog {
    
    /* Private variables */
    
    private int currentSequenceNumber = -1;
    
    private String currentMsgId = null;
    
    private ServerPDUEvent pduEvent = null;
    
    protected long dialogId;
    
    protected int gwProcessorId;
    
    protected int processorId;
    
    protected String smppMessageId = null;
    
    protected DialogService dialogService = AllocatorService.getDialogService();
    
    protected McDispatcher mcDispatcher = AllocatorService.getMcDispatcher();
    
    protected DialogState state = DialogState.created;
    
    protected int commandStatusCode = Data.ESME_ROK;
    
    /**
     * Creates a Dialog with <code>gwProcessorId</code> and <code>processorId</code>.
     * @param gwProcessorId
     * @param processorId
     */
    public Dialog(int gwProcessorId, int processorId) {
        // -1 to indicate, no asigned dialog id
        this(-1, gwProcessorId, processorId);
    }
    
    /**
     * Creates a Dialog with <code>dialogId</code>, <code>gwProcessorId</code> and <code>processorId</code>.
     * @param dialogId Dialog id to identify the dialog. It must to the messageId used in the MC local.
     * @param gwProcessorId Id to identify the gateway processor at which belongs the dialog.
     * @param processorId Id to identify the processor at which belongs the dialog.
     */
    public Dialog(long dialogId, int gwProcessorId, int processorId) {
        this.dialogId = dialogId;
        this.gwProcessorId = gwProcessorId;
        this.processorId = processorId;
    }
    
    /**
     * Initialize the dialog.
     */
    public abstract void init();
    
    /**
     * Handle incoming <code>ServerPDUEvent</code>.
     * @param ev
     */
    public void handleSmppEvent(ServerPDUEvent ev) {
        pduEvent = ev;
    }
    
    /**
     * Handle incoming <code>McMessage</code>.
     * @param msg
     */
    public abstract void handleMcMessage(McMessage msg);
    
    /**
     * Execute final task of dialog. This method is called after invalidate Dialog.
     */
    public abstract void execute();
    
    /**
     * The Dialog Id.
     * @return
     */
    public long getDialogId() {
        return dialogId;
    }
    
    /**
     * Set the Dialog Id.
     * @param dialogId
     */
    protected void setDialogId(long dialogId) {
        this.dialogId = dialogId;
    }
    
    /**
     * Set <code>smppMessageId</code>.
     * @param smppMessageId
     */
    public void setSmppMessageId(String smppMessageId) {
        this.smppMessageId = smppMessageId;
    }
    
    /**
     * Get <code>smppMessageId</code>.
     * @return
     */
    public String getSmppMessageId() {
        return smppMessageId;
    }
    
    /**
     * Set <code>currentSequenceNumber</code>.
     * @param currentSequenceNumber
     */
    public void setCurrentSequenceNumber(int currentSequenceNumber) {
        this.currentSequenceNumber = currentSequenceNumber;
    }
    
    /**
     * Get <code>currentSequenceNumber</code>.
     * @return
     */
    public int getCurrentSequenceNumber() {
        return currentSequenceNumber;
    }
    
    /**
     * Get <code>DialogType</code> to corresponds with Dialog implementation.
     * @return
     */
    public abstract DialogType getType();

    /**
     * Invalidate dialog.
     */
    protected void invalidate() {
        dialogService.invalidate(this);
    }
    
    /**
     * Get current state of the Dialog.
     * @return
     */
    public DialogState getState() {
        return state;
    }
    
    /**
     * Get current <code>currentMsgId</code>.
     * @return
     */
    public String getCurrentMsgId() {
        return currentMsgId;
    }
    
    /**
     * Set current <code>currentMsgId</code>.
     * @param currentMsgId
     */
    public void setCurrentMsgId(String currentMsgId) {
        this.currentMsgId = currentMsgId;
    }
    
    /** 
     * Get last <code>ServerPDUEvent</code>.
     * @return
     */
    public ServerPDUEvent getPduEvent() {
        return pduEvent;
    }
}
