/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Connection;
import org.smpp.TCPIPConnection;

import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 *
 * @author asalazar
 */
public class SmppServerGwProcessor extends SmppGwProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppServerGwProcessor.class);
    
    private List<SmppServerProcessor>smppServerProcessor = new ArrayList<>();
    
    private Config cfg = AllocatorService.getConfig();
    
    private boolean receiving = false;
    
    private Connection serverConn = null;
    
    public SmppServerGwProcessor(SmppGwSession gwSession) {
        super(gwSession);
    }
    
    /**
     * Initialize SmppServerListener, open serverConn. Start receiving routine.
     */
    @Override
    public void execute() {
        try {
            if (!isReceiving()) {
                // Not receiving and serverConn == null
                serverConn = new TCPIPConnection(gwSession.getSmscPort());
                serverConn.setReceiveTimeout(cfg.getServerListenerReceiveTimeout());
                serverConn.open();
                // ready to receive
                receiving = true;
            }
            
            if (isReceiving()) {
                logger.info("SmppGwSessionId:{} Ready to receive connection requests...", gwSession.getSmppGwId());
            } else {
                logger.warn("SmppGwSessionId:{} SmppServerListener is not initialized properly...", gwSession.getSmppGwId());
            }
            // receive
            while (isReceiving()) {
                receive();
            }
        } catch (IOException ex) {
            logger.error("Failed on execute SmppServerListener. Listener is Finalizing...", ex);
        }
    }
    
    /**
     * Receive connection request. If it is allowed to create new connection, creates an instance of SmppServerProcessor.
     */
    private void receive() {
        try {
            receiving = false;
            Connection conn = null;
            serverConn.setReceiveTimeout(cfg.getServerListenerReceiveTimeout());
            conn = serverConn.accept();
            // if an connection is requested
            if (conn != null) {
                // TODO Create smppServerProcessor
            }
        } catch (IOException ex) {
            logger.error("Failed on receive...", ex);
        }
    }
    
    @Override
    public void shutdown(int i) {
        try {
            if (serverConn != null && serverConn.isOpened()) {
                // Try to close server connection listener
                serverConn.close();
            }
        } catch (IOException ex) {
            logger.warn("Failed to close serverConn...");
        }
    }
    
    public boolean isReceiving() {
        return receiving;
    }
}
