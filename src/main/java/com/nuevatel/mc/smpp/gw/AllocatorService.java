/**
 * 
 */
package com.nuevatel.mc.smpp.gw;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.nuevatel.mc.smpp.gw.dialog.DialogService;
import com.nuevatel.mc.smpp.gw.exception.NullDialogService;
import com.nuevatel.mc.smpp.gw.exception.NullProperties;

/**
 * Share all common services between app.
 * 
 * @author Ariel Salazar
 *
 */
public final class AllocatorService {
    
    private static Properties properties = null;
    
    private static DialogService dialogService = null;
    
    /**
     * Contains all processors registered in the app. Each processor is an instance by mc session. The sessions are defined on table <code>mc.mc_smpp_gw</code>.
     * <br/>
     * Map<smppGwId, Processor>
     */
    private static Map<Integer, SmppGwProcessor>processorMap = new HashMap<>();
    
    private AllocatorService() {
        // No op. Used to prevent instantiation
    }
    
    public static Properties getProperties() {
        if (properties == null) {
            throw new NullProperties();
        }
        return properties;
    }
    
    public static void setProperties(Properties properties) {
        AllocatorService.properties = properties;
    }
    
    public static void initializeDialogService() {
        // DialogService get properties from AllocatorService
        dialogService = new DialogService();
    }
    
    public static DialogService getDialogService() {
        if (dialogService == null) {
            throw new NullDialogService();
        }
        return dialogService;
    }
    
    public static void registerSmppGwProcessor(int smppGwId, SmppGwProcessor processor) {
        processorMap.put(smppGwId, processor);
    }
    
    public static void shutdownSmppGwProcessors() {
        processorMap.forEach((k, p)->p.shutdown(60));
    }
    
    /**
     * 
     * @param smppGwId The smpp gw id (defined on table <code>mc.mc_smpp_gw</code>)
     * @return The processor to corresponds with <code>smppGwId</code>. <code>null</code> if there are not processor to corresponds with <code>smppGwId</code>.
     */
    public static SmppGwProcessor getSmppProcessor(int smppGwId) {
        return processorMap.get(smppGwId);
    }
}
