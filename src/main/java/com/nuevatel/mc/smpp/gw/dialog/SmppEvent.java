/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

import org.smpp.ServerPDUEvent;

/**
 * 
 * @author Ariel Salazar
 *
 */
public class SmppEvent {
    
    private long dialogId;
    
    private ServerPDUEvent pduEvent;
    
    public SmppEvent(long dialogId, ServerPDUEvent pduEvent) {
        this.pduEvent = pduEvent;
    }
    
    public ServerPDUEvent getPduEvent() {
        return pduEvent;
    }
    
    public long getDialogId() {
        return dialogId;
    }
    
    @Override
    public String toString() {
        return String.format("SmppEvent{dialogId:%s}", dialogId);
    }
}
