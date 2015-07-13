/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

import com.logica.smpp.Data;
import com.logica.smpp.ServerPDUEvent;
import com.logica.smpp.ServerPDUEventListener;
import com.logica.smpp.pdu.BindRequest;
import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.PDU;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.Response;
import com.nuevatel.smpp.dispatcher.DeliveryReceiptMCDispatcher;
import com.nuevatel.smpp.dispatcher.MCDispatcher;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

/**
 *
 * @author Luis Baldiviezo
 */
public class TestEventListener implements ServerPDUEventListener {
    /*private variables*/
    private ClientSession clientSession;
    private ScheduledThreadPoolExecutor threadPool;
    private static final Logger logger = Logger.getLogger(TestEventListener.class.getName());
    /**
     * Creates a new ServerEventListener Object
     * @param clientsession The parent serverSession
     * @param threadPoolSize The ExecutorService pool size for processing submit request
     */
    public TestEventListener(ClientSession clientsession, int threadPoolSize){
        this.clientSession=clientsession;
        ThreadFactory tf = new ThreadFactory() {

            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("MCDispatcher");
                return thread;
            }
        };
        threadPool = new ScheduledThreadPoolExecutor(threadPoolSize,tf);
    }

    /**
     * Implements logic to process incoming SMPP PDU requests and PDU responses
     * @param event the ServerPDUEvent
     */
    public void handleEvent(ServerPDUEvent event) {
        PDU pdu = event.getPDU();
//
//        if (pdu.isIncomplete() && pdu.isResponse()){
//            getThreadPool().execute(new DeliveryReceiptMCDispatcher(pdu, clientSession));
//        }

        int commandID=pdu.getCommandId();
        if (!clientSession.isBound()){
            Response response=null;
            //Can't receive requests if not bound
            if (pdu.isRequest()){
                response = ((Request)pdu).getResponse();
                response.setCommandStatus(Data.ESME_RINVBNDSTS);
                sendPDU(response);
            }

        }
        else{
            //session is bound, process SMPP PDU
            if (pdu.isRequest()){
//                Request request=(Request)pdu;
//                Response response=null;
//                if (commandID == Data.ENQUIRE_LINK){
//                    //enquire link request, send ok
//                    response=request.getResponse();
//                    response.setCommandStatus(Data.ESME_ROK);
//                    sendPDU(response);
//                }
//
//                else if (commandID == Data.DELIVER_SM){
//                    if (clientSession.getState()==Data.BIND_RECEIVER || clientSession.getState()==Data.BIND_TRANSCEIVER){
//                        DeliverSM deliverSM = (DeliverSM)pdu;
//                        if (deliverSM.getEsmClass()==4 || deliverSM.getEsmClass()==8){
//                            //delivery receipt
//                            getThreadPool().execute(new DeliveryReceiptMCDispatcher(pdu, clientSession));
//                        }
//                        else {
//                            //short message
//                            getThreadPool().execute(new MCDispatcher(pdu, clientSession));
//                        }
//                    }
//                    else {
//                        response = request.getResponse();
//                        response.setCommandStatus(Data.ESME_RINVBNDSTS);
//                        sendPDU(response);
//                    }
//                }
//
//                else if(commandID == Data.BIND_RECEIVER || commandID == Data.BIND_TRANSMITTER || commandID==Data.BIND_TRANSCEIVER){
//                    //bind request, send already bound
//                    BindRequest bindRequest = (BindRequest)request;
//                    response=request.getResponse();
//                    response.setCommandStatus(Data.ESME_RINVCMDID);
//                    sendPDU(response);
//                    logger.info("Rejected Bind Request. SystemId:"+bindRequest.getSystemId()+" Password:"+bindRequest.getPassword()+" bindType:"+bindRequest.getCommandId()+" Reason:"+response.getCommandStatus());
//                }
//
//                else if (commandID == Data.UNBIND){
//                    //unbind request, stop session
//                    response=request.getResponse();
//                    response.setCommandStatus(Data.ESME_ROK);
//                    sendPDU(response);
//                    clientSession.stop("Received remote Unbind");
//                }
//
//                else{
//                    //other request, send invalid
//                    response=request.getResponse();
//                    if (clientSession.getState()==Data.BIND_RECEIVER || clientSession.getState()==Data.BIND_TRANSCEIVER){
//                        response.setCommandStatus(Data.ESME_RINVCMDID);
//                    }
//                    else response.setCommandId(Data.ESME_RINVBNDSTS);
//                    sendPDU(response);
//                }
                System.out.println(getThreadPool().getQueue().size());
                getThreadPool().execute(new MCDispatcher(pdu, clientSession));
                
            }
            else if (pdu.isResponse()){
//                logger.finest("response for:"+pdu.getSequenceNumber());
                getThreadPool().execute(new DeliveryReceiptMCDispatcher(pdu, clientSession));
            }
        }
    }

    /**
     * @return this ServerEventListener's parent serverSession
     */
    public ClientSession getServerSession() {
        return clientSession;
    }

    /**
     * Sends any PDU using the parent ServerSession's send() method
     * @param pdu
     */
    public void sendPDU(PDU pdu){
        clientSession.send(pdu);
    }

    /**
     * @return the threadPool
     */
    public ScheduledThreadPoolExecutor getThreadPool() {
        return threadPool;
    }
}
