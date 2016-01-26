
package com.nuevatel.mc.smpp.gw;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.nuevatel.mc.smpp.gw.domain.Config;

/**
 * 
 * <p>The ThrotlleCounter class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Count and validate throttle by unit of time.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class ThrotlleCounter {

    /* Private variables */
    private Config cfg = AllocatorService.getConfig();
    
    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * Count messages per unit of time, defined in config.
     */
    private int count = 0;
    
    /**
     * Execute service.
     */
    public void execute() {
        // disable if timelife is 0 or less
        if (cfg.getThrottleCounterTimelife() > 0) service.scheduleAtFixedRate(() -> reset(), cfg.getThrottleCounterTimelife(), cfg.getThrottleCounterTimelife(), TimeUnit.SECONDS);
    }
    
    /**
     * Shutdown service.
     */
    public void shutdown() {
        service.shutdown();
        try {
            service.awaitTermination(2 * cfg.getThrottleCounterTimelife(), TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            //
        }
    }
    
    /**
     * INcrements count in +1
     */
    public void inc(){
        count++;
    }
    
    /**
     * <code>true</code> if count is greater to max
     * 
     * @param max
     * @return
     */
    public boolean exceededLimit(int max) {
        if (cfg.getThrottleCounterTimelife() > 0 && max > 0) return count >= max;
        // no limit
        return false;
    }
    
    /**
     * Set count to 0;
     */
    public void reset() {
        count = 0;
    }
}
