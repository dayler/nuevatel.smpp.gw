/**
 * 
 */
package com.nuevatel.mc.smpp.gw;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.nuevatel.mc.smpp.gw.dialog.DialogService;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.exception.NullDialogService;
import com.nuevatel.mc.smpp.gw.exception.NullMcDispatcher;
import com.nuevatel.mc.smpp.gw.exception.NullProperties;
import com.nuevatel.mc.smpp.gw.mcdispatcher.McDispatcher;

/**
 * 
 * <p>The AllocatorService class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Share all common services through app.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public final class AllocatorService {
    /* Private variables */
    private static Properties properties = null;
    
    private static DialogService dialogService = null;
    
    private static McDispatcher mcDispatcher = new McDispatcher();
    
    private static Config config = new Config();
    
    /**
     * Contains all processors registered in the app. Each processor is an instance by mc session. The sessions are defined on table <code>mc.mc_smpp_gw</code>.
     * <br/>
     * Map<smppGwId, Processor>
     */
    private static Map<Integer, SmppGwProcessor>processorMap = new HashMap<>();
    
    /**
     * Private constructor, used to prevent instantiation of this class.
     */
    private AllocatorService() {
        // No op. Used to prevent instantiation
    }
    
    /**
     * App configuration properties.
     * 
     * @return
     */
    public static Properties getProperties() {
        if (properties == null) {
            throw new NullProperties();
        }
        return properties;
    }
    
    /**
     * Set app configuration properties.
     * 
     * @param properties
     */
    public static void setProperties(Properties properties) {
        AllocatorService.properties = properties;
    }
    
    /**
     * Initialize the <code>DialogService</code>. This step is needed before to use any method for <code>DialogService</code>.
     */
    public static void initializeDialogService() {
        // DialogService get properties from AllocatorService
        dialogService = new DialogService();
    }
    
    /**
     * Gets <code>DialogService</code> instance.
     * 
     * @return
     */
    public static DialogService getDialogService() {
        if (dialogService == null) {
            throw new NullDialogService();
        }
        return dialogService;
    }
    
    /**
     * Register <code>SmppGwProcessor</code>, gateway instance.
     * 
     * @param smppGwId The gateway id, with which is registered the Processor.
     * @param processor The processor to register.
     */
    public static void registerSmppGwProcessor(int smppGwId, SmppGwProcessor processor) {
        processorMap.put(smppGwId, processor);
    }
    
    /**
     * Shutdown all <code>SmppGwProcessor</code>s. This is used when the is shutdown.
     */
    public static void shutdownSmppGwProcessors() {
        processorMap.forEach((k, p)->p.shutdown(60));
    }
    
    /**
     * Gets processor to corresponds with <code>smppGwId</code>. <code>null</code> if there are not processor to corresponds with <code>smppGwId</code>.
     * 
     * @param smppGwId The smpp gw id (defined on table <code>mc.mc_smpp_gw</code>)
     * @return 
     */
    public static SmppGwProcessor getSmppGwProcessor(int smppGwId) {
        return processorMap.get(smppGwId);
    }
    
    /**
     * Initialize <code>McDispatcher</code>, this step is needed before to use any service of <code>McDispatcher</code>.
     * 
     * @param size
     */
    public static void startMcDispatcher(int size) {
        AllocatorService.mcDispatcher.execute(size);
    }
    
    /**
     * Gets <code>McDispatcher</code> instance.
     * 
     * @return
     */
    public static McDispatcher getMcDispatcher() {
        return mcDispatcher;
    }
    
    /**
     * Shutdown <code>McDispatcher</code>.
     */
    public static void shutdownMcDispatcher() {
        if (mcDispatcher == null) {
            throw new NullMcDispatcher();
        }
        mcDispatcher.shutdown();
    }
    
    /**
     * Gets configuration of app.
     * 
     * @return
     */
    public static Config getConfig() {
        return config;
    }
    
    /**
     * Load configuration app properties.
     * 
     * @param prop
     */
    public static void loadConfig(Properties prop) {
        config.load(prop);
    }
}
