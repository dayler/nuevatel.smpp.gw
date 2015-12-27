/**
 * 
 */
package com.nuevatel.mc.smpp.gw.domain;

/**
 * @author Ariel Salazar
 *
 */
public enum PropName {
    dialogcacheConcurrencyLevel("smppgw.dialogcache.concurrencyLevel"),
    expireAfterWriteTime("smppgw.cache.expireAfterWriteTime"),
    dialogcacheTaskConcurrencyLevel("smppgw.dialogcache.task.concurrencyLevel"),
    enquireLinkPeriod("smppgw.enquireLinkPeriod"),
    defaultValidityPeriod("smppgw.defaultValidityPeriod"),
    serverlistenerReceiveTimeout("smppgw.serverlistener.receiveTimeout"),
    ;
    
    private String property;
    
    private PropName(String prop) {
        property = prop;
    }
    
    public String property() {
        return property;
    }
}
