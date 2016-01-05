/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.nuevatel.common.cache.CacheBuilder;
import com.nuevatel.common.cache.CacheLoader;
import com.nuevatel.common.cache.LoadingCache;
import com.nuevatel.common.cache.RemovalListener;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.exception.NoDialogCachedObject;

/**
 * Handle Dialog cache service
 * 
 * @author Ariel Salazar
 *
 */
public class DialogService {
    
    private LoadingCache<Long, Dialog>dialogCache;
    
    /**
     * Map smpp message id with its corresponding dialog id.
     */
    private Map<Integer, Long>smppMsgToDialogMap = new ConcurrentHashMap<>();
    
    private ExecutorService taskService;
    
    private Config cfg = AllocatorService.getConfig();
    
    public DialogService() {
        taskService = Executors.newFixedThreadPool(cfg.getDialogCacheTaskConcurrencyLevel());
        resolveLoadingCache();
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
        int size = AllocatorService.getConfig().getDialogCacheConcurrencyLevel();
        long expireAfterWriteTime = AllocatorService.getConfig().getDialogCacheExpireAfterWriteTime();
        dialogCache = CacheBuilder.newCacheBuilder().setExpireAfterReadTime(expireAfterWriteTime)
                                                    .setSize(size)
                                                    .setTimeUnit(TimeUnit.SECONDS)
                                                    .buildSimpleLoadingCache(new DialogLoader(), new OnRemoveDialogListener(taskService));
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
     * @param expireAfterWriteTime Time in seconds after which it is invalidated. 
     */
    public void putDialog(Dialog dialog, long expireAfterWriteTime) {
        if (dialog != null) {
            dialogCache.put(dialog.getDialogId(), dialog , expireAfterWriteTime, 0L/* disable after read */, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 
     * @param dialog Dialog to invalidate
     */
    public void invalidate(Dialog dialog) {
        dialogCache.invalidate(dialog.getDialogId());
        smppMsgToDialogMap.remove(dialog.getCurrentSequenceNumber());
    }
    
    /**
     * <code>true</code> if exists in the cache a dialog with <code>dialogId</code>.
     * 
     * @param dialogId Dialog to find
     */
    public boolean containsDialog(long dialogId) {
        return dialogCache.contains(dialogId);
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
    
    private static final class OnRemoveDialogListener implements RemovalListener<Long, Dialog> {
        
        private ExecutorService taskService;
        
        public OnRemoveDialogListener(ExecutorService taskService) {
            this.taskService = taskService;
        }
        
        @Override
        public void onRemoval(Long key, Dialog dialog) {
            // TODO remove debug string
            System.out.println("++++++++++ Release " + dialog.dialogId + " time " + ZonedDateTime.now().toString());
            if (dialog != null) {
                // execute task.
                taskService.execute(()->dialog.execute());
            }
        }
    }
}
