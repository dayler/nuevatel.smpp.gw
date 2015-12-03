/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.nuevatel.common.cache.CacheBuilder;
import com.nuevatel.common.cache.CacheLoader;
import com.nuevatel.common.cache.LoadingCache;
import com.nuevatel.common.util.IntegerUtil;
import com.nuevatel.common.util.LongUtil;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.PropName;
import com.nuevatel.mc.smpp.gw.exception.NoDialogCachedObject;

/**
 * Handle Dialog cache service
 * 
 * @author Ariel Salazar
 *
 */
public class DialogService {
    
    private LoadingCache<Long, Dialog>dialogCache;
    
    private Properties properties = AllocatorService.getProperties();
    
    /**
     * Map smpp message id with its corresponding dialog id.
     */
    private Map<Integer, Long>smppMsgToDialogMap = new ConcurrentHashMap<>();
    
    private ExecutorService taskService;
    
    public DialogService() {
        resolveLoadingCache();
        taskService = Executors.newFixedThreadPool(IntegerUtil.tryParse(properties.getProperty(PropName.dialogcacheTaskConcurrencyLevel.property()), 2));
    }
    
    public Long findDialogIdBySequenceNumber(int seqNumber) {
        return smppMsgToDialogMap.get(seqNumber);
    }
    
    /**
     * Register newSeqNumber for Dialog(dialogId)
     * 
     * @param newSeqNumber New Seq number to register for Dialog
     * @param dialogId Dialog Id in which register newSeqNumber
     */
    public void registerSequenceNumber(int newSeqNumber, long dialogId) {
        Dialog dialog = dialogCache.getUnchecked(dialogId);
        if (dialog == null) {
            // Dialog not defined
            return;
        }
        // register seq number
        if (dialog.getCurrentSequenceNumber() > 0) { // -1 if is the first state for dialog
            // remove old key
            smppMsgToDialogMap.remove(dialog.getCurrentSequenceNumber());
        }
        // Register seq number
        smppMsgToDialogMap.put(newSeqNumber, dialogId);
    }
    
    private void resolveLoadingCache() {
        int size = IntegerUtil.tryParse(properties.getProperty(PropName.dialogcacheConcurrencyLevel.property()), 4);
        long expireAfterWriteTime = LongUtil.tryParse(properties.getProperty(PropName.expireAfterWriteTime.property()), 3600000L);
        dialogCache = CacheBuilder.newCacheBuilder().setExpireAfterReadTime(expireAfterWriteTime).setSize(size).buildSimpleLoadingCache(new DialogLoader());;
    }
    
    /**
     * 
     * @param dialogId Id to identify the dialog.
     * @return <code>Dialog</code> to belongs dialogId. <code>null</code> if it not exists.
     */
    public Dialog getDialog(long dialogId) {
        return dialogCache.get(dialogId);
    }
    
    /**
     * Register a dialog in the cache.
     * 
     * @param dialog Dialog to register.
     * @param expireAfterWriteTime Time in milliseconds after which it is invalidated. 
     */
    public void putDialog(Dialog dialog, long expireAfterWriteTime) {
        if (dialog != null) {
            dialogCache.put(dialog.getDialogId(), dialog , expireAfterWriteTime, 0L/* disable after read */);
        }
    }
    
    /**
     * 
     * @param dialog Dialog to invalidate
     */
    public void invalidate(Dialog dialog) {
        dialogCache.invalidate(dialog.getDialogId());
        smppMsgToDialogMap.remove(dialog.getCurrentSequenceNumber());
        // execute 
        taskService.execute(()->dialog.execute());
    }
    
    /**
     * 
     * @return Time from now in which the 
     */
    public long calculateValidityPeriod() {
        return 0L;
    }
    
    /**
     * No action should not call never.
     */
    private static final class DialogLoader implements CacheLoader<Long, Dialog> {

        @Override
        public Dialog load(Long key) throws Exception {
            throw new NoDialogCachedObject();
        }
    }
}
