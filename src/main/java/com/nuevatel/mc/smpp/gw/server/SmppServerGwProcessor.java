/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Connection;
import org.smpp.TCPIPConnection;

import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwApp;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 *
 * @author asalazar
 */
public class SmppServerGwProcessor extends SmppGwProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppServerGwProcessor.class);
    
    private List<SmppServerProcessor>smppServerProcessorList = new ArrayList<>();
    
    private Config cfg = AllocatorService.getConfig();
    
    private boolean receiving = false;
    
    private Connection serverConn = null;
    
    private ExecutorService service;
    
    private ScheduledExecutorService heartbeatService = Executors.newSingleThreadScheduledExecutor();
    
    public SmppServerGwProcessor(SmppGwSession gwSession) {
        super(gwSession);
        // Number of binds to allow
        service = Executors.newFixedThreadPool(gwSession.getMaxBinds() * 2);
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
    
    private void registerProcessor(SmppServerProcessor processor) {
        smppServerProcessorList.add(processor);
        // Notify bind operation
        SmppGwApp.getSmppGwApp().setBound(gwSession.getSmppGwId(), gwSession.getSmppSessionId(), smppServerProcessorList.size());
    }
    
    private void unregisterProcessor(SmppServerProcessor processor) {
        // remove processor from list
        smppServerProcessorList.remove(processor);
        // Notify bind operation
        SmppGwApp.getSmppGwApp().setBound(gwSession.getSmppGwId(), gwSession.getSmppSessionId(), smppServerProcessorList.size());
    }
    
    /**
     * Receive connection request. If it is allowed to create new connection, creates an instance of SmppServerProcessor.
     */
    private void receive() {
        Connection conn = null;
        try {
            conn = serverConn.accept();
            // if an connection is requested
            if (conn != null) {
                // check bind counter limit
                if (smppServerProcessorList.size() >= gwSession.getMaxBinds()) {
                    // limit was reached
                    logger.info("Connection Request rejected the limit of connections has been exceeded. MaxBinds:{} BindsSize:{}", gwSession.getMaxBinds(), smppServerProcessorList.size());
                    conn.close();
                }
                // Create smppServerProcessor
                SmppServerProcessor processor = new SmppServerProcessor(gwSession, conn, serverPduEvents, smppEvents);
                // register processor
                registerProcessor(processor);
                // set shutdown delegate. Remove processor from list
                processor.setOnShutdownDelegate(() -> unregisterProcessor(processor));
                // receive
                service.execute(() -> processor.receive());
                // dispatch
                service.execute(() -> processor.dispatch());
                // health check
                long hearbeatPeriod = cfg.getEnquireLinkPeriod() > 0 ? cfg.getEnquireLinkPeriod() : 30L;
                heartbeatService.scheduleAtFixedRate(() -> checkHealthOfProcessor(processor), hearbeatPeriod, hearbeatPeriod, TimeUnit.SECONDS);
            } else {
                // timeout defined on setReceivingTimeout
                if (logger.isTraceEnabled()) {
                    logger.trace("On receive timeout. Awaithing by new connections...");
                }
            }
        } catch (IOException ex) {
            logger.error("Failed on receive...", ex);
            try {
                if (conn != null && conn.isOpened()) {
                    conn.close();
                }
            } catch (IOException e) {
                logger.warn("Failed to close conn", e);
            }
        }
    }
    
    private void checkHealthOfProcessor(SmppServerProcessor processor) {
        if (!processor.isConnected()) {
            // shutdown processor
            processor.shutdown();
            // remove from processors list
            smppServerProcessorList.remove(processor);
        }
    }
    
    @Override
    public void shutdown(int i) {
        try {
            // stop processors
            smppServerProcessorList.forEach(p -> p.shutdown());
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
