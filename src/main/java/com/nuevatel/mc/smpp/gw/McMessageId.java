/**
 * 
 */
package com.nuevatel.mc.smpp.gw;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Create new message ids.
 * 
 * @author Ariel Salazar
 *
 */
public class McMessageId {
    
    private ReentrantLock lck = new ReentrantLock();
    
    private int msgIdNumber = 0;
    
    public long newMcMessageId(LocalDateTime ldt, int mcId) {
        long messageId = ((long) (ldt.getYear() & 0x7) << 45);
        messageId |= ((long) ldt.getMonthValue() << 41);
        messageId |= ((long) ldt.getDayOfMonth() << 36);
        messageId |= ((long) ldt.get(ChronoField.SECOND_OF_DAY) << 19);
        messageId |= ((mcId & 0x7) << 16);
        lck.lock();
        try {
            messageId |= msgIdNumber;
            msgIdNumber = (msgIdNumber < 0xffff) ? ++msgIdNumber : 0;
        } finally {
            lck.unlock();
        }
        return messageId;
    }
    
}
