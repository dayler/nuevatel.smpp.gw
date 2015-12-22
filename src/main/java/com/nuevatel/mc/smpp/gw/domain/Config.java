/**
 * 
 */
package com.nuevatel.mc.smpp.gw.domain;

import java.util.Properties;

import com.nuevatel.common.util.IntegerUtil;
import com.nuevatel.common.util.LongUtil;
import com.nuevatel.common.util.Parameters;

/**
 * Common config properties for smppgw.
 * 
 * @author Ariel Salazar
 *
 */
public class Config {
    
    /**
     * Smpp message validity period expresed in seconds. By default 86400 seconds (1 day).
     */
    private long defaultValidityPeriod;
    
    /**
     * Beat period time (expressed in seconds). Only applicable for smpp clients. Default value 30s
     */
    private int enquireLinkPeriod;
    
    /**
     * Concurrency level for DialogCache. Default value 8.
     */
    private int dialogCacheConcurrencyLevel;
    
    /**
     * Concurrency level for DialogCache execution tasks. Default value 4.
     */
    private int dialogCacheTaskConcurrencyLevel;
    
    /**
     * Default time in seconds to keep an object in DialogCache, after insert it. It is not applicable if <b>validity period</b> is defined.
     */
    private long dialogCacheExpireAfterWriteTime;
    
    public void load(Properties prop) {
        Parameters.checkNull(prop, "prop");
        defaultValidityPeriod = LongUtil.tryParse(prop.getProperty(PropName.defaultValidityPeriod.property()), 86400L);
        enquireLinkPeriod = IntegerUtil.tryParse(prop.getProperty(PropName.enquireLinkPeriod.property()), 30);
        dialogCacheConcurrencyLevel = IntegerUtil.tryParse(prop.getProperty(PropName.dialogcacheConcurrencyLevel.property()), 8);
        dialogCacheTaskConcurrencyLevel = IntegerUtil.tryParse(prop.getProperty(PropName.dialogcacheTaskConcurrencyLevel.property()), 4);
        dialogCacheExpireAfterWriteTime = LongUtil.tryParse(PropName.expireAfterWriteTime.property(), 86400L);
    }
    
    public long getDefaultValidityPeriod() {
        return defaultValidityPeriod;
    }

    public int getEnquireLinkPeriod() {
        return enquireLinkPeriod;
    }

    public int getDialogCacheConcurrencyLevel() {
        return dialogCacheConcurrencyLevel;
    }

    public long getDialogCacheExpireAfterWriteTime() {
        return dialogCacheExpireAfterWriteTime;
    }

    public int getDialogCacheTaskConcurrencyLevel() {
        return dialogCacheTaskConcurrencyLevel;
    }
}
