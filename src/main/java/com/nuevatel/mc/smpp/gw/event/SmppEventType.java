
package com.nuevatel.mc.smpp.gw.event;

/**
 * 
 * <p>The SmppEventType class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Code to identify all <code>McEvent</code>s
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public enum SmppEventType {
    SubmitSmEvent, 
    CancelSmEvent,
    DataSmEvent,
    ReplaceSmEvent,
    QuerySmEvent,
    DefaultROkResponseEvent,
    GenericNAckEvent,
    GenericROkResponseEvent,
    //
    DeliverSmEvent,
    ;
}
