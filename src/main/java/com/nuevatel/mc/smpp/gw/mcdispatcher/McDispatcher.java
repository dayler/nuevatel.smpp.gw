/**
 * 
 */
package com.nuevatel.mc.smpp.gw.mcdispatcher;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.nuevatel.common.appconn.Message;
import com.nuevatel.common.exception.OperationRuntimeException;
import com.nuevatel.mc.appconn.McMessage;
import com.nuevatel.mc.smpp.gw.SmppGwApp;

/**
 * Dispatch appconn messages to MC.
 * 
 * @author Ariel Salazar
 *
 */
public class McDispatcher {
    
    private ExecutorService service = null;
    
    public void execute(int size) {
        service = Executors.newFixedThreadPool(size);
    }
    
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
    
    public void dispatch(McMessage msg) {
        if (msg == null) {
            return;
        }
        service.execute(() -> {
            try {
                SmppGwApp.getSmppGwApp().getAppClient().dispatch(msg.toMessage());
            } catch (Exception ex) {
                throw new OperationRuntimeException("Failed to dispatch message", ex);
            }
        });
    }
    
    public Message dispatchAndWait(McMessage msg) throws InterruptedException, ExecutionException {
        if (msg == null) {
            return null;
        }
        Future<Message>future = service.submit(()->SmppGwApp.getSmppGwApp().getAppClient().dispatch(msg.toMessage()));
        return future.get();
    }
}
