/**
 * 
 */
package com.nuevatel.mc.smpp.gw;

/**
 * @author Ariel Salazar
 *
 */
public enum PropName {
    dialogcacheConcurrencyLevel("smppgw.dialogcache.concurrencyLevel"),
    expireAfterWriteTime("smppgw.cache.expireAfterWriteTime"),
    dialogcacheTaskConcurrencyLevel("smppgw.dialogcache.task.concurrencyLevel"),
    enquireLinkPeriod("smppgw.enquireLinkPeriod"),
    ;
    
    private String property;
    
    private PropName(String prop) {
        property = prop;
    }
    
    public String property() {
        return property;
    }
}
