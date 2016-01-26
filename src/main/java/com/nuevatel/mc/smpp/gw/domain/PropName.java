
package com.nuevatel.mc.smpp.gw.domain;

/**
 * 
 * <p>The PropName class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Definitions of configurable properties. Used to get properties from configuration file.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public enum PropName {
    dialogcacheConcurrencyLevel("smppgw.dialogcache.concurrencyLevel"),
    expireAfterWriteTime("smppgw.dialogCache.expireAfterWriteTime"),
    dialogcacheTaskConcurrencyLevel("smppgw.dialogcache.task.concurrencyLevel"),
    enquireLinkPeriod("smppgw.enquireLinkPeriod"),
    defaultValidityPeriod("smppgw.defaultValidityPeriod"),
    serverlistenerReceiveTimeout("smppgw.serverlistener.receiveTimeout"),
    serverReceiverTimeout("smppgw.serverreceiver.timeout"),
    throttleCounterTimelife("smppgw.throttleCounter.timelife"),
    smppGwHeartbeatPeriod("smppgw.heartbeatPeriod"),
    ;
    
    private String property;
    
    /**
     * Creates <code>PropName</code> from property name.
     * 
     * @param prop
     */
    private PropName(String prop) {
        property = prop;
    }
    
    /**
     * Get property name, defined in its constructor.
     * 
     * @return
     */
    public String property() {
        return property;
    }
}
