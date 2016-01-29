
package com.nuevatel.mc.smpp.gw.server;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.Connection;
import org.smpp.TCPIPConnection;

import com.nuevatel.common.exception.OperationException;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 * 
 * <p>The SmppServerGwProcessor class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Smpp Server Gateway processor, is creating an instance of this class for each Smpp Session defined.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class SmppServerGwProcessor extends SmppGwProcessor {
    
    /* Constants */
    private static final int BACKLOG = 4;
    
    private static Logger logger = LogManager.getLogger(SmppServerGwProcessor.class);
    
    /* Private variables */
    private Config cfg = AllocatorService.getConfig();
    
    private boolean receiving = false;
    
    private Connection serverConn = null;
    
    private ExecutorService service;
    
    private ScheduledExecutorService heartbeatService = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * SmppServerGwProcessor constructor.
     * @param gwSession
     */
    public SmppServerGwProcessor(SmppGwSession gwSession) {
        super(gwSession);
        // Number of binds to allow. 4 is the backup to responds rejected connections.
        service = Executors.newFixedThreadPool(gwSession.getMaxBinds() * 2 + BACKLOG);
    }
    
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
                logger.info("SmppGwSessionId:{} Ready to receive connection requests [ADDR:{} PORT:{}]...", gwSession.getSmppGwId(), gwSession.getSmscAddress(), gwSession.getSmscPort());
            }
            else {
                logger.warn("SmppGwSessionId:{} SmppServerListener is not initialized properly...", gwSession.getSmppGwId());
            }
            // receive
            while (isReceiving()) {
                receive();
            }
        } catch (IOException ex) {
            logger.fatal("Failed on execute SmppServerListener. Listener is Finalizing...", ex);
            shutdown(60);
        }
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
                // Exceeded backlog, close connection
                if (countOfProcessors() > gwSession.getMaxBinds() + BACKLOG) conn.close();
                // Create smppServerProcessor
                SmppServerProcessor processor = new SmppServerProcessor(gwSession, conn, throtlleCounter);
                // authorized bind op
                processor.setAuthorizedBind(gwSession.getMaxBinds() == 0 || countOfBoundProcessors() < gwSession.getMaxBinds());
                // register processor
                registerSmppProcessor(processor);
                // set shutdown delegate. Remove processor from list and update bind count
                processor.setOnShutdownDelegate(() -> { unregisterSmppProcessor(processor); updateBindCount();} );
                // update bind count
                processor.setOnBindDelegate(() -> updateBindCount());
                // receive
                service.execute(() -> processor.receive());
                // dispatch
                service.execute(() -> processor.dispatch());
                // health check
                heartbeatService.scheduleAtFixedRate(() -> checkHealthOfProcessor(processor), cfg.getHeartbeatPeriod(), cfg.getHeartbeatPeriod(), TimeUnit.SECONDS);
            }
            else {
                // timeout defined on setReceivingTimeout
                if (logger.isTraceEnabled()) logger.trace("On receive timeout. Awaithing by new connections...");
            }
        } catch (IOException | OperationException ex) {
            logger.error("Failed on receive...", ex);
            try {
                if (conn != null) conn.close();
            } catch (IOException e) {
                logger.warn("Failed to close conn", e);
            }
        }
    }
    
    /**
     * Check connection health. 
     * @param processor
     */
    private void checkHealthOfProcessor(SmppServerProcessor processor) {
        // shutdown processor. On shutdown delegate will remove it from map
        if (!processor.isConnected()) {
            processor.shutdown();
            unregisterSmppProcessor(processor);
        } 
    }
    
    @Override
    public void shutdown(int i) {
        try {
            // stop processors
            shutdownAllSmppProcessors();
            // Try to close server connection listener
            if (serverConn != null && serverConn.isOpened()) serverConn.close();
        } catch (IOException ex) {
            logger.warn("Failed to close serverConn...");
        }
    }
    
    /**
     * <code>true</code> if the processor is receiving smpp requests.
     * @return
     */
    public boolean isReceiving() {
        return receiving;
    }
}
