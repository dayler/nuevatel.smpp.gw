/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

import com.logica.smpp.Data;
import com.logica.smpp.ServerPDUEvent;
import com.logica.smpp.ServerPDUEventListener;
import com.logica.smpp.pdu.BindRequest;
import com.logica.smpp.pdu.PDU;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.Response;
import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.smpp.dispatcher.DeliveryReceiptMCDispatcher;
import com.nuevatel.smpp.dispatcher.MCDispatcher;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

/**
 *
 * @author Luis Baldiviezo
 */
public class ServerEventListener implements ServerPDUEventListener {
    /*private variables*/
    private ServerSession serverSession;
    private ScheduledThreadPoolExecutor threadPool;
    private int threaPoolSize;
    private static final Logger logger = Logger.getLogger(ServerEventListener.class.getName());
    /**
     * Creates a new ServerEventListener Object
     * @param serverSession The parent serverSession
     * @param threadPoolSize The ExecutorService pool size for processing submit request
     */
    public ServerEventListener(ServerSession serverSession, int threadPoolSize){
        this.serverSession=serverSession;
        this.threaPoolSize=threadPoolSize;
    }

    /**
     * Implements logic to process incoming SMPP PDU requests and PDU responses
     * @param event the ServerPDUEvent
     */
    public void handleEvent(ServerPDUEvent event) {
        PDU pdu = event.getPDU();
        int commandID=pdu.getCommandId();
        if (!serverSession.isBound()){
            //session is not bound, check if it wants to bind
            Response response=null;
            if (commandID == Data.BIND_RECEIVER || commandID == Data.BIND_TRANSMITTER || commandID == Data.BIND_TRANSCEIVER){
                BindRequest bindRequest = (BindRequest)pdu;
                response = bindRequest.getResponse();
                int commandStatus=authenticateBindRequest(bindRequest);
                if (commandStatus == Data.ESME_ROK){
                    //authenticated, create thread pool  and update the sessionProperties
                    logger.info("Authenticaded Bind Request. SystemId:"+bindRequest.getSystemId()+" Password:"+bindRequest.getPassword()+" bindType:"+bindRequest.getCommandId() + " smppSessionId:"+serverSession.getSessionProperties().getSmppSessionId());
                    ThreadFactory tf = new ThreadFactory() {
                        public Thread newThread(Runnable r) {
                            Thread thread = new Thread(r);
                            thread.setName("MCDispatcherPool-"+getServerSession().getSessionProperties().getNode().getMcNodeID()+"-"+getServerSession().getSessionProperties().getSmppSessionId());
                            return thread;
                        }
                    };
                    threadPool = new ScheduledThreadPoolExecutor(getThreaPoolSize(),tf);
                    serverSession.setState(commandID);
                    response.setCommandStatus(commandStatus);
                    sendPDU(response);
                }
                else {
                    logger.info("Rejected Bind Request. SystemId:"+bindRequest.getSystemId()+" Password:"+bindRequest.getPassword()+" bindType:"+bindRequest.getCommandId()+" Reason:"+commandStatus);
                    response.setCommandStatus(commandStatus);
                    sendPDU(response);
                    serverSession.stop(null);
                }
                
            }
            else{
                //Can't make requests if not bound
                if (pdu.isRequest()){
                    response = ((Request)pdu).getResponse();
                    response.setCommandStatus(Data.ESME_RINVBNDSTS);
                    sendPDU(response);
                }
            }
        }
        else{
            //session is bound, process SMPP PDU
            if (pdu.isRequest()){
                Request request=(Request)pdu;
                Response response=null;
                if (commandID == Data.ENQUIRE_LINK){
                    //enquire link request, send ok
                    response=request.getResponse();
                    response.setCommandStatus(Data.ESME_ROK);
                    sendPDU(response);
                }

                else if (commandID == Data.SUBMIT_SM){
                    if (serverSession.getState()==Data.BIND_TRANSMITTER || serverSession.getState()==Data.BIND_TRANSCEIVER){
                        //schedule short message
                        threadPool.submit(new MCDispatcher(pdu, serverSession));
                    }
                    else {
                        response = request.getResponse();
                        response.setCommandStatus(Data.ESME_RINVBNDSTS);
                        sendPDU(response);
                    }

                }
//                else if (commandID == Data.DELIVER_SM){
                    //this may be a delivery receipt. but it is unsupported
//                    threadPool.submit(new DeliveryReceiptMCDispatcher(pdu, serverSession));
//                }

                else if(commandID == Data.BIND_RECEIVER || commandID == Data.BIND_TRANSMITTER || commandID==Data.BIND_TRANSCEIVER){
                    //bind request, send already bound
                    BindRequest bindRequest = (BindRequest)request;
                    response=request.getResponse();
                    response.setCommandStatus(Data.ESME_RALYBND);
                    sendPDU(response);
                    logger.info("Rejected Bind Request. SystemId:"+bindRequest.getSystemId()+" Password:"+bindRequest.getPassword()+" bindType:"+bindRequest.getCommandId()+" Reason:"+response.getCommandStatus());
                }

                else if (commandID == Data.UNBIND){
                    //unbind request, stop session
                    response=request.getResponse();
                    response.setCommandStatus(Data.ESME_ROK);
                    sendPDU(response);
                    serverSession.stop("Received remote Unbind");
                }

                else{
                    //other request, send invalid
                    response=request.getResponse();
                    if (serverSession.getState()==Data.BIND_TRANSMITTER || serverSession.getState()==Data.BIND_TRANSCEIVER){
                        response.setCommandStatus(Data.ESME_RINVCMDID);
                    }
                    else response.setCommandStatus(Data.ESME_RINVBNDSTS);
                    sendPDU(response);
                }
            }
            else if (pdu.isResponse()){
                //Schedule as delivery recipt because the DeliverSMResp IS the deliveryReceipt from the MC poit of view
                threadPool.submit(new DeliveryReceiptMCDispatcher(pdu, serverSession));
            }
        }
    }

    /**
     * @return this ServerEventListener's parent serverSession
     */
    public ServerSession getServerSession() {
        return serverSession;
    }

    /**
     * Sends any PDU using the parent ServerSession's send() method
     * @param pdu
     */
    public void sendPDU(PDU pdu){
        serverSession.send(pdu);
    }

    /**
     * Authenticates a bindRequest against the global sessionPropertiesCache
     * @param bindRequest
     * @return the commandStatus to be included in a bindResponse
     */
    private int authenticateBindRequest(BindRequest bindRequest){
        int bindResponseCommandId = Data.ESME_ROK;
        //Prior to a Bind Request serverSession.getSessionProperties() is null
        //We need to get the SessionProperties from the global cache
        SessionProperties tmpSessionProperties = CacheHandler.getCacheHandler().getSessionPropertiesCache().get(bindRequest.getSystemId(), bindRequest.getCommandId());
        if (tmpSessionProperties!=null){
            //found session in cache, if it is active
            String state = tmpSessionProperties.getState();
            if (state.equalsIgnoreCase("active")){
                //check password
                String password = tmpSessionProperties.getPassword();
                if (password!=null){
                    if (!bindRequest.getPassword().equals(password)) bindResponseCommandId = Data.ESME_RINVPASWD;
                    else{ //password OK, check system type
                        String systemType = tmpSessionProperties.getSystemType();
                        if ((systemType!=null && !bindRequest.getSystemType().equals(systemType)) || (systemType==null && !bindRequest.getSystemType().equals("")) ) bindResponseCommandId = Data.ESME_RINVSYSTYP;
                        else{ //sytem Type OK, check max size
                            if (CacheHandler.getCacheHandler().getSessionCount(tmpSessionProperties.getSmppSessionId())>=tmpSessionProperties.getSize()){
                                //Max size reached, log something
                                bindResponseCommandId = Data.ESME_RALYBND;
                            }
                            else{
                                //SystemID, commandId password and systemType OK, log something
                                CacheHandler.getCacheHandler().getSessionCache().put(tmpSessionProperties.getSmppSessionId(), serverSession);
                                CacheHandler.getCacheHandler().increaseSessionCount(tmpSessionProperties.getSmppSessionId());
                                serverSession.setSessionProperties(tmpSessionProperties);
                                serverSession.startMessageCounterResetTimer();
                            }
                        }
                    }
                }
                else{
                    logger.warning("Password for "+bindRequest.getSystemId()+"set to NULL in DB");
                    bindResponseCommandId = Data.ESME_RSYSERR;
                }
            }
            //session is inactive in DB
            else bindResponseCommandId = Data.ESME_RBINDFAIL;
        }
        else {
            //session not found in cache, log something
            bindResponseCommandId = Data.ESME_RINVSYSID;
        }
        return bindResponseCommandId;
    }

    /**
     * @return the threaPoolSize
     */
    public int getThreaPoolSize() {
        return threaPoolSize;
    }
    
    public ScheduledThreadPoolExecutor getThreadPool(){
        return threadPool;
    }

}
