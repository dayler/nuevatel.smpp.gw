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
    
    public void dispatch(RequestMcEvent event) {
        if (event == null) {
            return;
        }
        service.execute(() -> {
            try {
                SmppGwApp.getSmppGwApp().getAppClient().dispatch(event.toMessage());
            } catch (Exception ex) {
                throw new OperationRuntimeException("Failed to dispatch message", ex);
            }
        });
    }
    
    public ResponseMcEvent dispatchAndWait(RequestMcEvent event) throws InterruptedException, ExecutionException {
        if (event == null) {
            return null;
        }
        Future<Message>future = service.submit(()->SmppGwApp.getSmppGwApp().getAppClient().dispatch(event.toMessage()));
        return McEventFactory.responseFromMessage(future.get());
    }
}
