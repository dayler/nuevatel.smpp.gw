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
import com.nuevatel.common.exception.OperationRuntimeException;
import com.nuevatel.common.util.StringUtils;
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
     * Map smpp sequence number with its corresponding dialog id.
     */
    private Map<Integer, Long>smppSeqIdToDialogMap = new ConcurrentHashMap<>();
    
    /**
     * Map smpp message id with dialog Id.
     */
    private Map<String, Long>smppMsgIdToDialogMap = new ConcurrentHashMap<>();
    
    private ExecutorService taskService;
    
    private Config cfg = AllocatorService.getConfig();
    
    public DialogService() {
        taskService = Executors.newFixedThreadPool(cfg.getDialogCacheTaskConcurrencyLevel());
        resolveLoadingCache();
    }
    
    public Long findDialogIdByMessageId(String msgId) {
        return smppMsgIdToDialogMap.get(msgId);
    }
    
    /**
     * Register message id with its corresponding dilog id.
     * 
     * @param msgId smpp message id to register.
     * @param dialogId Dialog Id in which register message id.
     */
    public void registerMessageId(String msgId, long dialogId) {
        Dialog dialog = dialogCache.getUnchecked(dialogId);
        if (dialog == null) {
            // Dialog is not defined
            return;
        }
        // if current msd id is null, is first time
        if (!StringUtils.isEmptyOrNull(dialog.getCurrentMsgId())) {
            smppMsgIdToDialogMap.remove(dialog.getCurrentMsgId());
        }
        // regists new message Id for dialog
        smppMsgIdToDialogMap.put(msgId, dialogId);
        dialog.setCurrentMsgId(msgId);
    }
    
    public Long findDialogIdBySequenceNumber(int seqNumber) {
        return smppSeqIdToDialogMap.get(seqNumber);
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
            smppSeqIdToDialogMap.remove(dialog.getCurrentSequenceNumber());
        }
        // Register seq number
        smppSeqIdToDialogMap.put(newSeqNumber, dialogId);
        dialog.setCurrentSequenceNumber(newSeqNumber);
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
    public Dialog getDialog(Long dialogId) {
        if (dialogId == null) {
            // nullpointer in unboxing
            return null;
        }
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
            if (dialog.getDialogId() < 0) {
                throw new OperationRuntimeException("dialogId == -1, not asigned dialog Id.");
            }
            dialogCache.put(dialog.getDialogId(), dialog , expireAfterWriteTime, 0L/* disable after read */, TimeUnit.SECONDS);
        }
    }
    
    /**
     * 
     * @param dialog Dialog to invalidate
     */
    public void invalidate(Dialog dialog) {
        dialogCache.invalidate(dialog.getDialogId());
        if (dialog.getCurrentSequenceNumber() != -1) {
            smppSeqIdToDialogMap.remove(dialog.getCurrentSequenceNumber());
        }
        if (!StringUtils.isEmptyOrNull(dialog.getCurrentMsgId())) {
            smppMsgIdToDialogMap.remove(dialog.getCurrentMsgId());
        }
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
