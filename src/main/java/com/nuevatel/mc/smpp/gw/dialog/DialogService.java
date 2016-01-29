
package com.nuevatel.mc.smpp.gw.dialog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.nuevatel.common.cache.CacheBuilder;
import com.nuevatel.common.cache.LoadingCache;
import com.nuevatel.common.exception.OperationRuntimeException;
import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.exception.NoDialogCachedObject;

/**
 * 
 * <p>The DialogService class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Handle Dialog cache service
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class DialogService {
    
    /* Private variables */
    
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
    
    /**
     * Dialog service constructor. No need params.
     */
    public DialogService() {
        taskService = Executors.newFixedThreadPool(cfg.getDialogCacheTaskConcurrencyLevel());
        resolveLoadingCache();
    }
    
    /**
     * Find dialog Id, based on smpp message id.
     * @param msgId
     * @return
     */
    public Long findDialogIdByMessageId(String msgId) {
        return smppMsgIdToDialogMap.get(msgId);
    }
    
    /**
     * Register message id with its corresponding dialog id.
     * @param msgId smpp message id to register.
     * @param dialogId Dialog Id in which register message id.
     */
    public void registerMessageId(String msgId, long dialogId) {
        Dialog dialog = dialogCache.getUnchecked(dialogId);
        // Dialog is not defined
        if (dialog == null) return;
        // if current msd id is null, is first time
        if (!StringUtils.isEmptyOrNull(dialog.getCurrentMsgId())) smppMsgIdToDialogMap.remove(dialog.getCurrentMsgId());
        // regists new message Id for dialog
        smppMsgIdToDialogMap.put(msgId, dialogId);
        dialog.setCurrentMsgId(msgId);
    }
    
    /**
     * Find dialog by its current sequence number.
     * @param seqNumber
     * @return
     */
    public Long findDialogIdBySequenceNumber(int seqNumber) {
        return smppSeqIdToDialogMap.get(seqNumber);
    }
    
    /**
     * Register newSeqNumber for Dialog(dialogId).
     * @param newSeqNumber New Seq number to register for Dialog
     * @param dialogId Dialog Id in which register newSeqNumber
     */
    public void registerSequenceNumber(int newSeqNumber, long dialogId) {
        Dialog dialog = dialogCache.getUnchecked(dialogId);
        // Dialog not defined
        if (dialog == null) return;
        // register seq number
        if (dialog.getCurrentSequenceNumber() > 0) { // -1 if is the first state for dialog
            // remove old key
            smppSeqIdToDialogMap.remove(dialog.getCurrentSequenceNumber());
        }
        // Register seq number
        smppSeqIdToDialogMap.put(newSeqNumber, dialogId);
        dialog.setCurrentSequenceNumber(newSeqNumber);
    }
    
    /**
     * Create and initialize LoadingCahce.
     */
    private void resolveLoadingCache() {
        int size = cfg.getDialogCacheConcurrencyLevel();
        long expireAfterWriteTime = AllocatorService.getConfig().getDialogCacheExpireAfterWriteTime();
        dialogCache = CacheBuilder.newCacheBuilder().setExpireAfterReadTime(expireAfterWriteTime)
                                                    .setSize(size)
                                                    .setTimeUnit(TimeUnit.SECONDS)
                                                    .buildSimpleLoadingCache((k) -> onLoadDialog(k), (k, d) -> onRemoveDialog(k, d));
    }
    
    /**
     * Get <code>Dialog</code> to belongs dialogId. <code>null</code> if it not exists.
     * @param dialogId
     * @return 
     */
    public Dialog getDialog(Long dialogId) {
        // nullpointer in unboxing
        if (dialogId == null) return null;
        return dialogCache.get(dialogId);
    }
    
    /**
     * Register a dialog in the cache.
     * @param dialog 
     * @param expireAfterWriteTime Time in seconds after which it is forced to invalidated. 
     */
    public void putDialog(Dialog dialog, long expireAfterWriteTime) {
        if (dialog != null) {
            if (dialog.getDialogId() < 0) throw new OperationRuntimeException("dialogId == -1, not asigned dialog Id.");
            dialogCache.put(dialog.getDialogId(), dialog , expireAfterWriteTime, 0L/* disable after read */, TimeUnit.SECONDS);
        }
    }
    
    /**
     * Invalidate Dialog.
     * @param dialog
     */
    public void invalidate(Dialog dialog) {
        dialogCache.invalidate(dialog.getDialogId());
        if (dialog.getCurrentSequenceNumber() != -1) smppSeqIdToDialogMap.remove(dialog.getCurrentSequenceNumber());
        if (!StringUtils.isEmptyOrNull(dialog.getCurrentMsgId())) smppMsgIdToDialogMap.remove(dialog.getCurrentMsgId());
    }
    
    /**
     * <code>true</code> if exists in the cache a dialog with <code>dialogId</code>.
     * @param dialogId Dialog to find
     */
    public boolean containsDialog(long dialogId) {
        return dialogCache.contains(dialogId);
    }
    
    /**
     * Formal process for LoadingCache. Should never call.
     * @return
     * @throws Exception
     */
    private Dialog onLoadDialog(Long key) throws Exception {
        throw new NoDialogCachedObject();
    }
    
    /**
     * Task to execute when a {@link Dialog} is invalidating.
     * 
     * @param key
     * @param dialog
     */
    private void onRemoveDialog(Long key, Dialog dialog) {
        if (dialog != null) {
            // execute task.
            taskService.execute(()->dialog.execute());
        }
    }
}
