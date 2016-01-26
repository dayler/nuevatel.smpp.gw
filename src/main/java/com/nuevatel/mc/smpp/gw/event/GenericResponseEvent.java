
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.Data;
import org.smpp.pdu.Response;

import com.nuevatel.common.util.Parameters;

/**
 * 
 * <p>The GenericResponseEvent class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Define generic <code>Response</code>, regardless of whether this is ROK or NACK.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class GenericResponseEvent extends SmppEvent {
    
    /* Private variables */
    private Response response;
    
    /**
     * Create an instance of <code>GenericResponseEvent</code>.
     * @param response
     */
    public GenericResponseEvent(Response response) {
        this(response, Data.ESME_ROK);
    }
    
    /**
     * Create an instance of <code>GenericResponseEvent</code>.
     * @param response
     * @param commandStatus
     */
    public GenericResponseEvent(Response response, int commandStatus ) {
        Parameters.checkNull(response, "response");
        response.setCommandStatus(commandStatus);
        this.response = response;
    }
    
    /**
     * Get the Response to deliver.
     * @return
     */
    public Response getResponse() {
        return response;
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.GenericROkResponseEvent;
    }
}
