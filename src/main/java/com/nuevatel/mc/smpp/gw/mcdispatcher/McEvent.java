/**
 * 
 */
package com.nuevatel.mc.smpp.gw.mcdispatcher;

import com.nuevatel.common.appconn.Message;

/**
 * To categorize mc events, in going or out going.
 * 
 * @author Ariel Salazar
 *
 */
public abstract class McEvent {
    
    public abstract Message toMessage();
    
}
