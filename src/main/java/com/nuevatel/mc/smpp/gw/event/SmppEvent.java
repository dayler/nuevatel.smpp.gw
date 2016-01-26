
package com.nuevatel.mc.smpp.gw.event;

/**
 * 
 * <p>The SmppEvent class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Mc event, for incoming from SMPP to MC or Outgoing Mc to SMPP
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public abstract class SmppEvent {
    /* Private variables */
    private long messageId;
    
    /**
     * Set messageId.
     * @param messageId
     */
    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }
    
    /**
     * Get messageId.
     * @return
     */
    public long getMessageId() {
        return messageId;
    }
    
    /**
     * Unique code to identify the kind of the event.
     * @return 
     */
    public abstract SmppEventType type();
}
