/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.GenericNack;

/**
 * @author Ariel Salazar
 *
 */
public class GenericNAckEvent extends SmppEvent {
    
    private int sequenceNumber;
    
    private int errorCode;
    
    public GenericNAckEvent(int sequenceNumber, int errorCode) {
        this.sequenceNumber = sequenceNumber;
        this.errorCode = errorCode;
    }
    
    public GenericNack getGenericNack() {
        return new GenericNack(errorCode, sequenceNumber);
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.GenericNAckEvent;
    }

}
