/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.PDU;
import org.smpp.pdu.Request;
import org.smpp.pdu.Response;

/**
 * @author Ariel Salazar
 *
 */
public class DefaultResponseEvent extends SmppEvent {
    
    private Response resp;
    
    public DefaultResponseEvent(PDU pdu) {
        // Get default resp
        this.resp = ((Request)pdu).getResponse();
    }
    
    public Response getDefaultResponse() {
        return resp;
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.DefaultResponseEvent;
    }

}
