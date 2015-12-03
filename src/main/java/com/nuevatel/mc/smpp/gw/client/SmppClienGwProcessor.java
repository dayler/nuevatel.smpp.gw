/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.client;

import java.io.IOException;
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
import com.nuevatel.mc.smpp.gw.PropName;
import com.nuevatel.mc.smpp.gw.SmppProcessor;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 * Specialization for smpp client.<br/>
 * <li><code>smppEvents</code> defined on <code>SmppProcessor</code></li>
 * <li><code>mcEvents</code> defined on <code>SmppProcessor</code></li>
 *
 * @author Ariel Salazar
 */
public class SmppClienGwProcessor extends SmppProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppClientManager.class);
    
    private SmppClientManager clientMng;
    
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
        clientMng = new SmppClientManager(gwSession, smppEvents, mcEvents);
        service = Executors.newFixedThreadPool(2); // TODO
        // enquire link
        enquireLinkPeriod = LongUtil.tryParse(AllocatorService.getProperties().getProperty(PropName.enquireLinkPeriod.property()), 0L);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        try {
            clientMng.bind();
            // dispatch thread
            service.execute(()->clientMng.dispatch());
            // receive thread
            service.execute(()->clientMng.receive());
            // enquire link thread
            if (enquireLinkPeriod > 0) {
                enquireLinkService = Executors.newSingleThreadScheduledExecutor();
                enquireLinkService.scheduleWithFixedDelay(()->doEnquireLink(), enquireLinkPeriod, enquireLinkPeriod, TimeUnit.MILLISECONDS);
                logger.info("enquireLink - period:{}", enquireLinkPeriod);
            }
            logger.info("SmppClientGwProcessor[smppGwId{}] was started...", gwSession.getSmppGwId());
        } catch (Throwable ex) {
            logger.fatal("Failed to execute SmppClientGwProcessor[smppGwId:{}]...", gwSession.getSmppGwId(), ex);
            // try shutdown
            shutdown(60);
        }
    }
    
    private void doEnquireLink() {
        try {
            clientMng.enquireLink();
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
            if (clientMng.isBound()) {
                clientMng.unbind();
            }
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
