/**
 * 
 */
package com.nuevatel.mc.smpp.gw.server;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Connection;
import org.smpp.TCPIPConnection;

import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 * Await and create for new incoming sessions from clients.
 * 
 * @author Ariel Salazar
 *
 */
public class SmppServerListener {
    
    private static Logger logger = LogManager.getLogger(SmppServerListener.class);
    
    private Connection serverConn = null;
    
    private boolean receiving;
    
    private SmppGwSession gwSession;
    
    private Config cfg = AllocatorService.getConfig();

    public SmppServerListener(SmppGwSession gwSession) {
        Parameters.checkNull(gwSession, "gwSession");
        this.gwSession = gwSession;
    }
    
    public void execute() {
        try {
            while (isReceiving()) {
                serverConn = new TCPIPConnection(gwSession.getSmscPort());
                serverConn.setReceiveTimeout(cfg.getServerListenerReceiveTimeout());
                serverConn.open();
                receiving = true;
                // TODO SMSCListenerImpl#78
            }
        } catch (IOException ex) {
            logger.error("Failed on execute SmppServerListener. Listener is Finalizing...", ex);
        }
    }
    
    private void shutdown() {
        // TODO
    }
    
    private boolean isReceiving() {
        return receiving;
    }
}
