/**
 * 
 */
package com.nuevatel.mc.smpp.gw.event;

import org.smpp.Data;
import org.smpp.pdu.Response;

import com.nuevatel.common.util.Parameters;

/**
 * @author Ariel Salazar
 *
 */
public class GenericResponseEvent extends SmppEvent {
    
    private Response response;
    
    public GenericResponseEvent(Response response) {
        this(response, Data.ESME_ROK);
    }
    
    public GenericResponseEvent(Response response, int commandStatus ) {
        Parameters.checkNull(response, "response");
        response.setCommandStatus(commandStatus);
        this.response = response;
    }
    
    public Response getResponse() {
        return response;
    }
    
    @Override
    public SmppEventType type() {
        return SmppEventType.GenericResponseEvent;
    }
}
