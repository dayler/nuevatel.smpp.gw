
package com.nuevatel.mc.smpp.gw;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.smpp.Connection;

import com.nuevatel.common.util.Delegate;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.smpp.gw.domain.Config;

/**
 * <p>The TcpPingService class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Check keep alive connection. Execute periodical ping task, if it fails <code>cfg.getPingTaskProbes</code> times, execute <code>onConnectionLostDelegate</code>.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class TcpPingService {

    private Config cfg = AllocatorService.getConfig();
    
    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * Address at which do ping.
     */
    private InetAddress inetAddr = null;
    
    /**
     * Count of time to fail ping, before to execute <code>onConnectionLostDelegate</code>.
     */
    private int count = 0;
    
    /**
     * Task to execute for lost connection.
     */
    private Delegate onConnectionLostDelegate;
    
    /**
     * <code>TcpPingService</code> constructor with <code>Connection</code> and <code>onConnectionLostDelegate</code>.
     * @param conn
     * @param onConnectionLostDelegate
     * @throws UnknownHostException
     */
    public TcpPingService(Connection conn, Delegate onConnectionLostDelegate) throws UnknownHostException {
        Parameters.checkNull(conn, "conn");
        Parameters.checkNull(onConnectionLostDelegate, "onConnectionLostDelegate");
        inetAddr = InetAddress.getByName(conn.getAddress());
        this.onConnectionLostDelegate = onConnectionLostDelegate;
    }
    
    /**
     * Single pint task.
     * @return
     */
    private boolean ping() {
        try {
            return inetAddr.isReachable(cfg.getPingTaskTimeout());
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Execute service.
     */
    public void execute() {
        // Execute periodical task
        service.scheduleAtFixedRate(() -> check(), cfg.getPingTaskTime(), cfg.getPingTaskTime(), TimeUnit.SECONDS);
    }
    
    /**
     * Check connection, an update count fails, or execute <code>onConnectionLostDelegate</code>.
     */
    private void check() {
        if (ping()) {
            // reset count
            count = 0;
        } else {
            // ping failed
            count++;
            if (count < cfg.getPingTaskProbes()) {
                // No op
            }
            else {
                // Execute on failed delegate. Topically stop session.
                onConnectionLostDelegate.execute();
            }
        }
    }
    
    /**
     * Shutdown service. Await 60 seconds by termination task.
     */
    public void shutdown() {
        service.shutdown();
        try {
            service.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            // No op
            ex.printStackTrace();
        }
    }
}
