
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.GenericNack;

/**
 * 
 * <p>The GenericNAckEvent class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Define default NACK response.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class GenericNAckEvent extends SmppEvent {
    
    /* Private variables */
    private int sequenceNumber;
    
    private int errorCode;
    
    public GenericNAckEvent(int sequenceNumber, int errorCode) {
        this.sequenceNumber = sequenceNumber;
        this.errorCode = errorCode;
    }
    
    /**
     * Get <code>GenericNack</code> pdu.
     * @return
     */
    public GenericNack getGenericNack() {
        return new GenericNack(errorCode, sequenceNumber);
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.GenericNAckEvent;
    }

}
