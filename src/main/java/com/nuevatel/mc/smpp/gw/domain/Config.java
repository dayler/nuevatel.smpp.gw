
package com.nuevatel.mc.smpp.gw.domain;

import java.util.Properties;

import com.nuevatel.common.util.IntegerUtil;
import com.nuevatel.common.util.LongUtil;
import com.nuevatel.common.util.Parameters;

/**
 * 
 * <p>The Config class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Common config properties for smppgw.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class Config {
    
    /* Private variables */
    /*
     * Smpp message validity period expresed in seconds. By default 86400 seconds (1 day).
     */
    private long defaultValidityPeriod;
    
    /*
     * Beat period time (expressed in seconds). Only applicable for smpp clients. Default value 30s
     */
    private int enquireLinkPeriod;
    
    /*
     * Concurrency level for DialogCache. Default value 8.
     */
    private int dialogCacheConcurrencyLevel;
    
    /*
     * Concurrency level for DialogCache execution tasks. Default value 4.
     */
    private int dialogCacheTaskConcurrencyLevel;
    
    /*
     * Default time in seconds to keep an object in DialogCache, after insert it. It is not applicable if <b>validity period</b> is defined.
     */
    private long dialogCacheExpireAfterWriteTime;
    
    /*
     * Time(expressed in milliseconds) to await by an connection request before to pass next cycle. Only is used in smsc mode. Default value 500ms
     */
    private long serverListenerReceiveTimeout;
    
    /*
     * Time(expressed in milliseconds) to await by incoming smpp message. 
     */
    private long serverReceiverTimeout;
    
    /*
     * Period in which is reset the throttle counter. It is expressed in seconds.
     */
    private int throttleCounterTimelife;
    
    /*
     * Period for heartbeat check task.
     */
    private int heartbeatPeriod;
    
    /**
     * Load all configuration fields from Properties.
     * 
     * @param prop
     */
    public void load(Properties prop) {
        Parameters.checkNull(prop, "prop");
        defaultValidityPeriod = LongUtil.tryParse(prop.getProperty(PropName.defaultValidityPeriod.property()), 86400L);
        enquireLinkPeriod = IntegerUtil.tryParse(prop.getProperty(PropName.enquireLinkPeriod.property()), 30);
        dialogCacheConcurrencyLevel = IntegerUtil.tryParse(prop.getProperty(PropName.dialogcacheConcurrencyLevel.property()), 8);
        dialogCacheTaskConcurrencyLevel = IntegerUtil.tryParse(prop.getProperty(PropName.dialogcacheTaskConcurrencyLevel.property()), 4);
        dialogCacheExpireAfterWriteTime = LongUtil.tryParse(PropName.expireAfterWriteTime.property(), 86400L);
        serverListenerReceiveTimeout = LongUtil.tryParse(prop.getProperty(PropName.serverlistenerReceiveTimeout.property()), 500L);
        throttleCounterTimelife = IntegerUtil.tryParse(prop.getProperty(PropName.throttleCounterTimelife.property()), 1);
        heartbeatPeriod = IntegerUtil.tryParse(prop.getProperty(PropName.smppGwHeartbeatPeriod.property()), 30);
    }
    
    /**
     * Get Period in which is reset the throttle counter. It is expressed in seconds.
     * 
     * @return
     */
    public int getThrottleCounterTimelife() {
        return throttleCounterTimelife;
    }
    
    /**
     * Get Smpp message validity period expresed in seconds. By default 86400 seconds (1 day).
     * 
     * @return
     */
    public long getDefaultValidityPeriod() {
        return defaultValidityPeriod;
    }
    
    /**
     * Get Beat period time (expressed in seconds). Only applicable for smpp clients. Default value 30s
     * 
     * @return
     */
    public int getEnquireLinkPeriod() {
        return enquireLinkPeriod;
    }
    
    /**
     * Get Concurrency level for DialogCache. Default value 8.
     * 
     * @return
     */
    public int getDialogCacheConcurrencyLevel() {
        return dialogCacheConcurrencyLevel;
    }
    
    /**
     * Get Default time in seconds to keep an object in DialogCache, after insert it. It is not applicable if <b>validity period</b> is defined.
     * 
     * @return
     */
    public long getDialogCacheExpireAfterWriteTime() {
        return dialogCacheExpireAfterWriteTime;
    }
    
    /**
     * Get Concurrency level for DialogCache execution tasks. Default value 4.
     * 
     * @return
     */
    public int getDialogCacheTaskConcurrencyLevel() {
        return dialogCacheTaskConcurrencyLevel;
    }
    
    /**
     * Get Time(expressed in milliseconds) to await by an connection request before to pass next cycle. Only is used in smsc mode. Default value 500ms
     * 
     * @return
     */
    public long getServerListenerReceiveTimeout() {
        return serverListenerReceiveTimeout;
    }
    
    /**
     * Get Time(expressed in milliseconds) to await by incoming smpp message.
     * 
     * @return
     */
    public long getServerReceiverTimeout() {
        return serverReceiverTimeout;
    }
    
    /**
     *  Get Period for heartbeat check task.
     * 
     * @return
     */
    public int getHeartbeatPeriod() {
        return heartbeatPeriod;
    }
}
