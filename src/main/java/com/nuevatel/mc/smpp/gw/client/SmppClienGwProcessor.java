/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.client;

import java.io.IOException;
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

import com.nuevatel.common.util.LongUtil;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.domain.PropName;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 * Specialization for smpp client.<br/>
 * <li><code>smppEvents</code> defined on <code>SmppProcessor</code></li>
 * <li><code>mcEvents</code> defined on <code>SmppProcessor</code></li>
 *
 * @author Ariel Salazar
 */
public class SmppClienGwProcessor extends SmppGwProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppClienGwProcessor.class);
    
    private List<SmppClientProcessor>smppClientProcessorList = new ArrayList<>();
    
    private ExecutorService service;
    
    private long enquireLinkPeriod;
    
    private ScheduledExecutorService enquireLinkService = null;
    
    /**
     * Initialize service processor
     * 
     * @param gwSession Session properties
     */
    public SmppClienGwProcessor(SmppGwSession gwSession) {
        super(gwSession);
        service = Executors.newFixedThreadPool(gwSession.getMaxBinds());
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
                    smppClientProcessorList.add(processor);
                    // do bind
                    processor.bind();
                    // dispatch thread
                    service.execute(() -> processor.dispatch());
                    // receive thread
                    service.execute(() -> processor.receive());
                    // enquire link thread
                    if (enquireLinkPeriod > 0) {
                        if (enquireLinkService == null) {
                            enquireLinkService = Executors.newSingleThreadScheduledExecutor();
                        }
                        enquireLinkService.schedule(() -> doEnquireLink(processor), enquireLinkPeriod,
                                TimeUnit.SECONDS);
                    }
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
    
    private void doEnquireLink(SmppClientProcessor clientProcessor) {
        try {
            clientProcessor.enquireLink();
            logger.debug("enquireLink...");
        } catch (TimeoutException | PDUException | WrongSessionStateException | IOException ex) {
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
                    processor.unbind();
                }
            });
            // stop enquirelink service
            if (enquireLinkService != null) {
                enquireLinkService.shutdown();
                enquireLinkService.awaitTermination(60, TimeUnit.SECONDS);
            }
            // stop executor service
            service.shutdown();
            service.awaitTermination(ts, TimeUnit.SECONDS);
        } catch (Throwable ex) {
            logger.error("Failed to shutdown SmppClientGwProcessor[smppGwId:{}]...", gwSession.getSmppGwId(), ex);
        }
    }
}
