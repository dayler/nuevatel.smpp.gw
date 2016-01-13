/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.Request;
import org.smpp.pdu.Response;

/**
 * @author Ariel Salazar
 *
 */
public class DefaultResponseOKEvent extends SmppEvent {
    
    private Response resp;
    
    public DefaultResponseOKEvent(Request request) {
        if (!request.canResponse()) {
            throw new IllegalArgumentException("Request:" + request.debugString() + " cannot do response");
        }
        resp = request.getResponse();
    }
    
    public Response getDefaultResponse() {
        return resp;
    }
    
    public Response getResponse() {
        return resp;
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.DefaultROkResponseEvent;
    }

}
