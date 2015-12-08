/**
 * 
 */
package com.nuevatel.mc.smpp.gw.mcdispatcher;

import com.nuevatel.common.appconn.Message;

/**
 * @author Ariel Salazar
 *
 */
public abstract class ResponseMcEvent extends McEvent {

    public static ResponseMcEvent fromMessage(Message msg) {
        return null;
    }
}
