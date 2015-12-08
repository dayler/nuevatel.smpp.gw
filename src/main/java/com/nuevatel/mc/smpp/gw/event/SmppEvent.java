/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

/**
 * Mc event, can be incoming from SMPP to MC or Outgoing Mc to SMPP
 * 
 * @author Ariel Salazar
 *
 */
public abstract class SmppEvent {
    
    private long messageId;
    
    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }
    
    public long getMessageId() {
        return messageId;
    }
    
    /**
     * 
     * @return Unique code to identify the kind of the event.
     */
    public abstract SmppEventType type();
}
