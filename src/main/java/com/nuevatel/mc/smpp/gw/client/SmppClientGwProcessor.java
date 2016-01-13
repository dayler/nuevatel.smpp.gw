/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.client;

import java.util.ArrayList;
import java.util.List;
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
import com.nuevatel.mc.smpp.gw.SmppGwApp;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 * Specialization for smpp client.<br/>
 * <li><code>smppEvents</code> defined on <code>SmppProcessor</code></li>
 * <li><code>mcEvents</code> defined on <code>SmppProcessor</code></li>
 *
 * @author Ariel Salazar
 */
public class SmppClientGwProcessor extends SmppGwProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppClientGwProcessor.class);
    
    /**
     * One processor for each binding client.
     */
    private List<SmppClientProcessor>smppClientProcessorList = new ArrayList<>();
    
    private ExecutorService service;
    
    private long enquireLinkPeriod;
    
    private ScheduledExecutorService heartbeatService = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * Initialize service processor
     * 
     * @param gwSession Session properties
     */
    public SmppClientGwProcessor(SmppGwSession gwSession) {
        super(gwSession);
        service = Executors.newFixedThreadPool(gwSession.getMaxBinds() * 2);
        // enquire link
        enquireLinkPeriod = AllocatorService.getConfig().getEnquireLinkPeriod();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        try {
            for (int i = 0; i < gwSession.getMaxBinds(); i++) {
                try {
                    // Create one processor for connection to handle
                    SmppClientProcessor processor = new SmppClientProcessor(gwSession, serverPduEvents, smppEvents);
                    // register smpp processor
                    smppClientProcessorList.add(processor);
                    // do bind
                    doBind(processor);
                    // dispatch thread
                    service.execute(() -> processor.dispatch());
                    // receive thread
                    service.execute(() -> processor.receive());
                    // health check
                    // enquire link thread
                    if (enquireLinkPeriod > 0) {
                        heartbeatService.scheduleAtFixedRate(() -> doEnquireLink(processor), enquireLinkPeriod, enquireLinkPeriod, TimeUnit.SECONDS);
                    }
                    // auto reconnect task
                    long autoReConnectPeriod = enquireLinkPeriod > 0 ? enquireLinkPeriod : 30L; // default 30s
                    heartbeatService.scheduleAtFixedRate(() -> tryReconnect(processor), autoReConnectPeriod, autoReConnectPeriod, TimeUnit.SECONDS);
                    logger.info("SmppClientProcessor was start [index={}]...", i);
                } catch (Throwable ex) {
                    logger.error("At index {} cannot be start SmppClientProcessor...", i, ex);
                }
            }
            logger.info("SmppClientGwProcessor[smppGwId{}] was started...", gwSession.getSmppGwId());
        } catch (Throwable ex) {
            logger.fatal("Failed to execute SmppClientGwProcessor[smppGwId:{}]...", gwSession.getSmppGwId(), ex);
            // try shutdown
            shutdown(60);
        }
    }
    
    /**
     * Updates bind succeeded count in the mc manager.
     */
    private void updateBindCountToMcManager() {
        SmppGwApp.getSmppGwApp().setBound(gwSession.getSmppGwId(),
                                          gwSession.getSmppSessionId(),
                                          (int)smppClientProcessorList.stream().filter(p -> p.isBound()).count());
    }
    
    private void doBind(SmppClientProcessor processor) {
        try {
            if (processor.isBound()) {
                return;
            }
            // Try bind op
            processor.bind();
            // bind succeeded
            updateBindCountToMcManager();
        } catch (Throwable ex) {
            logger.error("Failed to bind. SmppGwId:{} with {}:{}...", gwSession.getSmppGwId(), gwSession.getSmscAddress(), gwSession.getSmscPort(), ex);
        }
    }
    
    private void tryReconnect(SmppClientProcessor processor) {
        if (!processor.isBound()) {
            // try to reconnect.
            // try unbind
            try {
                processor.unbind();
            } catch (Throwable ex) {
                logger.warn("On reconnection task, failed un unbind...");
                if (logger.isTraceEnabled()) {
                    logger.warn("Exception:", ex);
                }
            }
            // try bind
            try {
                processor.bind();
                // bind succeeded
                updateBindCountToMcManager();
            } catch (Throwable ex) {
                logger.warn("On reconnection task, failed bind. SmppGwId:{} with {}:{}...", gwSession.getSmppGwId(), gwSession.getSmscAddress(), gwSession.getSmscPort());
                if (logger.isTraceEnabled()) {
                    logger.warn("Exception:", ex);
                }
            }
        }
    }
    
    private void doEnquireLink(SmppClientProcessor clientProcessor) {
        try {
            clientProcessor.enquireLink();
            logger.debug("enquireLink...");
        } catch (TimeoutException | PDUException | WrongSessionStateException ex) {
            logger.error("Failed enquireLink...", ex);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown(int ts) {
        try {
            smppClientProcessorList.stream().forEach(processor -> {
                if (processor.isBound()) {
                    // unbind
                    processor.unbind();
                }
            });
            // stop enquirelink service
            if (heartbeatService != null) {
                heartbeatService.shutdown();
                heartbeatService.awaitTermination(60, TimeUnit.SECONDS);
            }
            // Update unbind operation on manager
            updateBindCountToMcManager();
            // stop executor service
            service.shutdown();
            service.awaitTermination(ts, TimeUnit.SECONDS);
        } catch (Throwable ex) {
            logger.error("Failed to shutdown SmppClientGwProcessor[smppGwId:{}]...", gwSession.getSmppGwId(), ex);
        }
    }
}
