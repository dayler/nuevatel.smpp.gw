
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.pdu.Request;
import org.smpp.pdu.Response;

/**
 * 
 * <p>The DefaultResponseOKEvent class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Model for <code>Default ROK Response</code>.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class DefaultResponseOKEvent extends SmppEvent {
    
    /* Private Variables */
    private Response resp;
    
    /**
     * Constructor for <code>DefaultResponseOKEvent</code>.
     * @param request
     */
    public DefaultResponseOKEvent(Request request) {
        if (!request.canResponse()) {
            throw new IllegalArgumentException("Request:" + request.debugString() + " cannot do response");
        }
        resp = request.getResponse();
    }
    
    /**
     * Get Response.
     * @return
     */
    public Response getDefaultResponse() {
        return resp;
    }
    
    /**
     * Get Response.
     * @return
     */
    public Response getResponse() {
        return resp;
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.DefaultROkResponseEvent;
    }
}
