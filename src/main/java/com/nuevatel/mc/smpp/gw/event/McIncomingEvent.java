/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

/**
 * Represents an event from local SMSC (MC)
 * 
 * @author asalazar
 *
 */
public abstract class McIncomingEvent extends McEvent {
    
    private long messageId;
    
    public McIncomingEvent(long messageId) {
        this.messageId = messageId;
    }
    
    public long getMessageId() {
        return messageId;
    }
    
    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }
}
