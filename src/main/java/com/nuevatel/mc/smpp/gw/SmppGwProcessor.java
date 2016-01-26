
package com.nuevatel.mc.smpp.gw;

import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.nuevatel.common.Processor;
import com.nuevatel.common.exception.OperationException;
import com.nuevatel.common.util.Parameters;
import com.nuevatel.common.util.UniqueID;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 * 
 * <p>The SmppGwProcessor class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Abstract SmppProcessor. Children of this are responsible to handle smmp logic (client, server).
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public abstract class SmppGwProcessor implements Processor {
    
    /* Protected variables */
    protected SmppGwSession gwSession;
    
    /*
     * Used to generate random SmppProcessor id.
     */
    private UniqueID idGen;
    
    protected ThrotlleCounter throtlleCounter = new ThrotlleCounter();
    
    /* Private variables */
    /*
     * One processor for each binding client.
     */
    private Map<Integer, SmppProcessor>smppProcessorMap = new HashMap<>();
    
    /*
     * Iterator index.
     */
    private Integer itIdx = 0;
    
    /**
     * Constructor for <code>SmppGwProcessor</code>, assign the <code>SmppGwSession</code> to the processor.
     * 
     * @param gwSession
     */
    public SmppGwProcessor(SmppGwSession gwSession) {
        Parameters.checkNull(gwSession, "gwSession");
        this.gwSession = gwSession;
        // initialize counter
        throtlleCounter.execute();
        // Initialize id gen
        try {
            idGen = new UniqueID();
        } catch (NoSuchAlgorithmException ex) {
            // No op
        }
    }
    
    /**
     * Gets smpp processor to corresponds with <code>smppProcessorId</code>.
     * 
     * @param smppProcessorId
     * @return
     */
    public SmppProcessor getSmppProcessor(Integer smppProcessorId) {
        return smppProcessorMap.get(smppProcessorId);
    }
    
    /**
     * Selected <code>SmppProcessor</code>. Pick up a smpp processor, to handle an operation. <code>null</code> if there no have a registered
     * smpp processor, or an exception occurs when is selecting the proecssor.
     * 
     * @return 
     */
    @SuppressWarnings("unchecked")
    public synchronized Integer nextSmppProcessorId() {
        if (smppProcessorMap.isEmpty()) {
            // empty list
            return null;
        }
        Map.Entry<Integer, SmppProcessor>entry = null;
        try {
            // Select processor index
            do {
                if (++itIdx < smppProcessorMap.size()) {
                    // Can select smpp processor.
                }
                else {
                    // out bound. Need reset index
                    itIdx = 0;
                }
                // while SmppProcessor at index is not bound, and itIdx is in smppProcessorMap.size.
                entry = (Entry<Integer, SmppProcessor>) smppProcessorMap.entrySet().toArray()[itIdx];
            } while (entry != null && !entry.getValue().isBound() && itIdx + 1 < smppProcessorMap.size());
        } catch (Throwable ex) {
            itIdx = 0;
            return null;
        }
        // return selected index
        return entry == null ? null : entry.getKey();
    }
    
    /**
     * Gets current it index.
     * 
     * @return
     */
    public Integer getCurrentItIdx() {
        return itIdx;
    }
    
    /**
     * Get <code>SmppGwSession</code>.
     * @return
     */
    public SmppGwSession getSmppGwSession() {
        return gwSession;
    }
    
    @Override
    public abstract void execute();
    
    @Override
    public void shutdown(int ts) {
        throtlleCounter.shutdown();
    }
    
    /**
     * Register the <code>SmppProcessor</code> and assign a unique id.
     * 
     * @param processor
     * @throws OperationException
     */
    protected void registerSmppProcessor(SmppProcessor processor) throws OperationException {
        if (processor == null) return;
        // select id
        Integer tmpId = -1;
        int count = 0;
        do {
            tmpId = idGen.nextInt(65535); // 16 bits
            if (++count >= 255) throw new OperationException("Cannot register SmppProcessor tmpId:" + tmpId + " count:" + count + " gwSessionId:" + gwSession.getSmppGwId() + " smppSessionId:" + gwSession.getSmppSessionId());
        } while (smppProcessorMap.containsKey(tmpId));
        // register processor
        processor.setSmppProcessorId(tmpId);
        smppProcessorMap.put(tmpId, processor);
    }
    
    /**
     * Number of registered processors.
     * 
     * @return
     */
    protected int countOfProcessors() {
        return smppProcessorMap.size();
    }
    
    /**
     * Number of registered processor in bound state.
     * 
     * @return
     */
    protected int countOfBoundProcessors() {
        return (int) smppProcessorMap.values().stream().filter(p -> p.isBound()).count();
    }
    
    /**
     * Remove a registered processor.
     * 
     * @param processor
     */
    protected void unregisterSmppProcessor(SmppProcessor processor) {
        smppProcessorMap.remove(processor.getSmppProcessorId());
    }
    
    /**
     * Updates the MC with current number of processors that have already bind.
     */
    protected void updateBindCount() {
        // Notify bind operation
        SmppGwApp.getSmppGwApp().setBound(gwSession.getSmppGwId(),
                                          gwSession.getSmppSessionId(),
                                          countOfBoundProcessors());
    }
    
    /**
     * Shutdown all registered processors. It is used at shutdown GwProcessor.
     */
    protected void shutdownAllSmppProcessors() {
        // shutdown all registered smpp processors
        smppProcessorMap.values().forEach(SmppProcessor::shutdown);
    } 
    
    /**
     * Get <code>Map&lt;processorId, SmppProcessor&gt;</code> in an <b>unmodifiable collection</b>.
     * 
     * @return
     */
    public Map<Integer, SmppProcessor> getSmppProcessorMap() {
        return Collections.unmodifiableMap(smppProcessorMap);
    }
}
