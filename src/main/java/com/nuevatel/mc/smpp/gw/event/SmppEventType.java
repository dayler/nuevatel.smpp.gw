/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

/**
 * Code to identify all <code>McEvent</code>s
 * 
 * @author asalazar
 *
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
