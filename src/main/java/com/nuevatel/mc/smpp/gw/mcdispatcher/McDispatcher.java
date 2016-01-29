
package com.nuevatel.mc.smpp.gw.mcdispatcher;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.nuevatel.common.appconn.Message;
import com.nuevatel.common.exception.OperationRuntimeException;
import com.nuevatel.common.util.Tic;
import com.nuevatel.mc.appconn.McMessage;
import com.nuevatel.mc.common.stat.StatService.STAT_TIME;
import com.nuevatel.mc.common.stat.StatService.STAT_VALUE;
import com.nuevatel.mc.smpp.gw.SmppGwApp;

/**
 * 
 * <p>The McDispatcher class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Dispatch appconn messages to MC.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class McDispatcher {
    
    /* Private variables */
    private ExecutorService service = null;
    
    /**
     * Execute service.
     * @param size
     */
    public void execute(int size) {
        service = Executors.newFixedThreadPool(size);
    }
    
    /**
     * Shutdown service.
     */
    public void shutdown() {
        if (service == null) {
            return;
        }
        try {
            service.shutdown();
            service.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new OperationRuntimeException("Failed to shutdown McDispatcher.", ex);
        }
    }
    
    /**
     * Dispatch AppConn message to MC.
     * @param msg Message to dispatch.
     */
    public void dispatch(McMessage msg) {
        if (msg == null) {
            return;
        }
        service.execute(() -> {
            try {
                Message rawMsg = msg.toMessage();
                SmppGwApp.getSmppGwApp().getAppClient().dispatch(rawMsg);
                // Register statistics
                addStatValue(rawMsg.getCode());
            } catch (Exception ex) {
                throw new OperationRuntimeException("Failed to dispatch message", ex);
            }
        });
    }
    
    /**
     * Dispatch AppConn message and await response.
     * @param msg Message to dispatch.
     * @param tic
     * @return Appconn message response.
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     */
    public Message dispatchAndWait(McMessage msg) throws InterruptedException, ExecutionException, TimeoutException {
        if (msg == null) {
            return null;
        }
        Message rawMsg = msg.toMessage();
        Tic tic = new Tic();
        Future<Message>future = service.submit(()->SmppGwApp.getSmppGwApp().getAppClient().dispatch(rawMsg));
        addStatTime(rawMsg.getCode(), tic);
        return future.get(10, TimeUnit.SECONDS);
    }

    /**
     * Add statValue.
     * @param code
     */
    private void addStatValue(int code) {
        if (McMessage.FORWARD_SM_O_RET_ASYNC_CALL == code) SmppGwApp.getSmppGwApp().getStatService().add(STAT_VALUE.FORWARD_SM_O_RET_ASYNC_CALL, 1);
    }

    /**
     * Add statTime.
     * @param code
     * @param tic
     */
    private void addStatTime(int code, Tic tic) {
        if (McMessage.FORWARD_SM_I_CALL == code) {
            SmppGwApp.getSmppGwApp().getStatService().add(STAT_TIME.FORWARD_SM_I_CALL, 1, tic.toc(), (long) Math.pow(tic.getElapsedTime(), 2));
        }
        else {
            // No op
        }
    }
}
