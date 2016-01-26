/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.TimeoutException;
import org.smpp.WrongSessionStateException;
import org.smpp.pdu.PDUException;

import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 * 
 * <p>The SmppClientGwProcessor class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Specialization for smpp client.<br/>
 * <li><code>smppEvents</code> defined on <code>SmppProcessor</code></li>
 * <li><code>mcEvents</code> defined on <code>SmppProcessor</code></li>
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class SmppClientGwProcessor extends SmppGwProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppClientGwProcessor.class);
    
    /* Private variables */
    
    private ExecutorService service;
    
    private Config cfg = AllocatorService.getConfig();
    
    private ScheduledExecutorService heartbeatService = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * Initialize service processor
     * 
     * @param gwSession
     */
    public SmppClientGwProcessor(SmppGwSession gwSession) {
        super(gwSession);
        service = Executors.newFixedThreadPool(gwSession.getMaxBinds() * 2);
    }
    
    @Override
    public void execute() {
        try {
            for (int i = 0; i < gwSession.getMaxBinds(); i++) {
                try {
                    // Create one processor for connection to handle
                    SmppClientProcessor processor = new SmppClientProcessor(gwSession, throtlleCounter);
                    // register smpp processor
                    registerSmppProcessor(processor);
                    // do bind
                    doBind(processor);
                    // dispatch thread
                    service.execute(() -> processor.dispatch());
                    // receive thread
                    service.execute(() -> processor.receive());
                    // health check
                    // enquire link thread
                    if (cfg.getEnquireLinkPeriod() > 0) {
                        heartbeatService.scheduleAtFixedRate(() -> doEnquireLink(processor), cfg.getEnquireLinkPeriod(), cfg.getEnquireLinkPeriod(), TimeUnit.SECONDS);
                    }
                    // auto reconnect task
                    heartbeatService.scheduleAtFixedRate(() -> tryReconnect(processor), cfg.getHeartbeatPeriod(), cfg.getHeartbeatPeriod(), TimeUnit.SECONDS);
                    logger.info("SmppClientProcessor was start [index={}]...", i);
                } catch (Throwable ex) {
                    logger.error("At index {} cannot be start SmppClientProcessor...", i, ex);
                }
            }
            logger.info("SmppClientGwProcessor[smppGwId:{}] was started. {} binds was succedded...", gwSession.getSmppGwId(), countOfBoundProcessors());
        } catch (Throwable ex) {
            logger.fatal("Failed to execute SmppClientGwProcessor[smppGwId:{}]...", gwSession.getSmppGwId(), ex);
            // try shutdown
            shutdown(60);
        }
    }
    
    /**
     * Do bind action for a single processor.
     * 
     * @param processor
     */
    private void doBind(SmppClientProcessor processor) {
        try {
            if (processor.isBound()) return;
            // Try bind op
            processor.bind();
            // bind succeeded
            updateBindCount();
        } catch (Throwable ex) {
            logger.error("Failed to bind. SmppGwId:{} with {}:{}...", gwSession.getSmppGwId(), gwSession.getSmscAddress(), gwSession.getSmscPort(), ex);
        }
    }
    
    /**
     * Try to reconect with SMSC if connection was lost.
     * 
     * @param processor
     */
    private void tryReconnect(SmppClientProcessor processor) {
        if (!processor.isBound()) {
            // try to reconnect.
            // try unbind
            try {
                processor.unbind();
            } catch (Throwable ex) {
                logger.warn("On reconnection task, failed un unbind...");
                if (logger.isTraceEnabled()) logger.warn("Exception:", ex);
            }
            // try bind
            try {
                processor.bind();
                // bind succeeded
                updateBindCount();
            } catch (Throwable ex) {
                logger.warn("On reconnection task, failed bind. SmppGwId:{} with {}:{}...", gwSession.getSmppGwId(), gwSession.getSmscAddress(), gwSession.getSmscPort());
                if (logger.isTraceEnabled()) logger.warn("Exception:", ex);
            }
        }
    }
    
    /**
     * Do enquire link.
     * 
     * @param clientProcessor
     */
    private void doEnquireLink(SmppClientProcessor clientProcessor) {
        try {
            clientProcessor.enquireLink();
            logger.debug("enquireLink...");
        } catch (TimeoutException | PDUException | WrongSessionStateException ex) {
            logger.error("Failed enquireLink...", ex);
        }
    }
    
    @Override
    public void shutdown(int ts) {
        try {
            super.shutdown(ts);
            getSmppProcessorMap().values().stream().map(p -> (SmppClientProcessor) p).forEach(processor -> {
                // unbind
                if (processor.isBound()) processor.unbind();
            });
            // stop enquirelink service
            if (heartbeatService != null) {
                heartbeatService.shutdown();
                heartbeatService.awaitTermination(60, TimeUnit.SECONDS);
            }
            // Update unbind operation on manager
            updateBindCount();
            // stop executor service
            service.shutdown();
            service.awaitTermination(ts, TimeUnit.SECONDS);
        } catch (Throwable ex) {
            logger.error("Failed to shutdown SmppClientGwProcessor[smppGwId:{}]...", gwSession.getSmppGwId(), ex);
        }
    }
}
