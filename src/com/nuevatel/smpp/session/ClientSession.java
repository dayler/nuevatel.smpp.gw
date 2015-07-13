/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

import com.logica.smpp.Connection;
import com.logica.smpp.Data;
import com.logica.smpp.NotSynchronousException;
import com.logica.smpp.ServerPDUEventListener;
import com.logica.smpp.TCPIPConnection;
import com.logica.smpp.TimeoutException;
import com.logica.smpp.Transmitter;
import com.logica.smpp.pdu.BindReceiver;
import com.logica.smpp.pdu.BindRequest;
import com.logica.smpp.pdu.BindResponse;
import com.logica.smpp.pdu.BindTransciever;
import com.logica.smpp.pdu.BindTransmitter;
import com.logica.smpp.pdu.GenericNack;
import com.logica.smpp.pdu.InvalidPDUException;
import com.logica.smpp.pdu.PDU;
import com.logica.smpp.pdu.PDUException;
import com.logica.smpp.pdu.Request;
import com.logica.smpp.pdu.Response;
import com.logica.smpp.pdu.Unbind;
import com.logica.smpp.pdu.UnknownCommandIdException;
import com.logica.smpp.pdu.ValueNotSetException;
import com.logica.smpp.util.NotEnoughDataInByteBufferException;
import com.logica.smpp.util.TerminatingZeroNotFoundException;
import com.nuevatel.smpp.SMPPClientApplication;
import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.smpp.dispatcher.ClientQueueDispatcher;
import com.nuevatel.smpp.dispatcher.SMPPDispatcher;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Luis Baldiviezo
 */
public class ClientSession extends Session{

    private static final Logger logger = Logger.getLogger(ClientSession.class.getName());
    private ClientEventListener eventListener=null;
    private ConcurrentLinkedQueue<SMPPDispatcher> dispatcherQueue = new ConcurrentLinkedQueue<SMPPDispatcher>();
    ClientQueueDispatcher clientQueueDispatcher;

    /**
     * Creates a new ServerSession object
     * @param connection The SMPP connection object, typically a TCPIPConnection object
     */
    public ClientSession(Connection connection, SessionProperties sessionProperties){
        setSessionProperties(sessionProperties);
        this.connection=(TCPIPConnection)connection;
        transmitter = new Transmitter(connection);
        receiver = new SessionReceiver(transmitter, this.connection, this, SMPPClientApplication.getSMPPClientApplication().getProcessSleepMillis());
        remoteAddress=this.connection.getSocket().getInetAddress().getHostAddress();
        remotePort=this.connection.getSocket().getPort();
        logger.fine("New ClientSession to: "+ remoteAddress+":"+remotePort);
    }

    /**
     * First sets this session's receiver with the <code>ServerPDUEventListener</code>.
     * Then starts the receiver in Asynchronous mode.
     * Always call <code>SetEventListener</code> prior to calling this method.
     */
    @Override public void run(){
        try {
            //set the eventlistener for this session befor starting receiver
            BindRequest bindRequest = null;

            switch (getSessionProperties().getBindType()){
                case SessionProperties.BIND_TYPE_RECEIVER:
                    bindRequest = new BindReceiver();
                    break;
                case SessionProperties.BIND_TYPE_TRANSMITTER:
                    bindRequest = new BindTransmitter();
                    break;
                case SessionProperties.BIND_TYPE_TRANSCEIVER:
                    bindRequest = new BindTransciever();
                    break;
            }
            bindRequest.setSystemId(getSessionProperties().getSystemId());
            bindRequest.setPassword(getSessionProperties().getPassword());
            bindRequest.setSystemType(getSessionProperties().getSystemType());
            BindResponse bindResponse = (BindResponse)send(bindRequest,false);
            if (bindResponse.getCommandStatus()!=Data.ESME_ROK){
                if (bindResponse.getCommandStatus()==Data.ESME_RALYBND){
                    Unbind unbind = new Unbind();
                    send(unbind);
                }
                stop("Bind Request Refused: "+bindResponse.getCommandStatus());
            }
            else {
                receiver.setServerPDUEventListener(eventListener);
                receiver.start();
                setState(bindRequest.getCommandId());
                CacheHandler.getCacheHandler().getSessionCache().put(getSessionProperties().getSmppSessionId(), this);
                CacheHandler.getCacheHandler().increaseSessionCount(getSessionProperties().getSmppSessionId());
                startMessageCounterResetTimer();
//                clientQueueDispatcher = new ClientQueueDispatcher(this);
                Thread clientQueThread = new Thread(new ClientQueueDispatcher(this));
                clientQueThread.start();

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    /**
     * Attempts to stop this session's receiver and also closes the connection
     */
    @Override public void stop(String reason){
        if (getState()!=STATE_CLOSED){
            try {
                if (reason!=null){
                    logger.info("Stopping ClientSession to "+ remoteAddress+":"+remotePort +" smppSessionId: "+getSessionProperties().getSmppSessionId()+" Reason: "+reason);
                }
                setState(STATE_CLOSED);
                if (getEventListener().getThreadPool()!=null) getEventListener().getThreadPool().shutdown();
                CacheHandler.getCacheHandler().getSessionCache().remove(getSessionProperties().getSmppSessionId(), this);
                CacheHandler.getCacheHandler().decreaseSessionCount(getSessionProperties().getSmppSessionId());
                stopMessageCounterResetTimer();
                if (connection!=null) connection.close();
                if (receiver!=null) receiver.stop();
            } catch (Exception ex) {
                logger.fine(ex.toString());
            }
        }
    }
    /**
     * Attempts to send the specified pdu though this session transmitter
     * @param pdu
     */
    @Override public void send(PDU pdu){
        try {
//            logger.finest("sending PDU seq "+ pdu.getSequenceNumber());
            transmitter.send(pdu);
        } catch (ValueNotSetException ex) {
            Logger.getLogger(ClientSession.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ClientSession.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Attempts to send the specified pdu though this session transmitter optionally in synchronous manner
     * @param pdu
     */
    final private Response send(Request request, boolean asynchronous) throws ValueNotSetException, TimeoutException, PDUException, IOException {
        Response response = null;
        try {
            transmitter.send(request);
        } catch (ValueNotSetException e) {
            throw e;
        }
        if ((!asynchronous) && (request.canResponse())) {
            PDU pdu = null;
            try {
                try {
                    pdu = receiver.receive(request.getResponse());
                } catch (NotSynchronousException e) {
                    logger.warning(e.getMessage());
                }
            } catch (UnknownCommandIdException e) {
                logger.warning(e.getMessage()+ " "+ e.getCommandId());
                GenericNack generickNack = new GenericNack(Data.ESME_RINVCMDID, e.getSequenceNumber());
                send(generickNack);
            } catch (InvalidPDUException e) {
                if ((e.getException() instanceof NotEnoughDataInByteBufferException) || (e.getException() instanceof TerminatingZeroNotFoundException)) {
                    logger.warning(e.getMessage());
                    GenericNack generickNack = new GenericNack(Data.ESME_RINVMSGLEN, e.getPDU().getSequenceNumber());
                    send(generickNack);
                } else {
                    throw e;
                }
            } catch (TimeoutException e) {
                logger.warning(e.getMessage());
                throw e;
            }
            if (pdu != null) response = (Response)pdu;
        }
        return response;
    }

    /**
     * @return the eventListener
     */
    public ClientEventListener getEventListener() {
        return eventListener;
    }

    /**
     * @param eventListener the eventListener to set
     */
    public void setEventListener(ClientEventListener eventListener) {
        this.eventListener = eventListener;
    }
    /**
     * @return true if the ServerSession is in bound state
     */
    public boolean isBound(){
        if (getState()==STATE_BOUND_RX || getState()==STATE_BOUND_TX || getState()==STATE_BOUND_TRX) return true;
        else return false;
    }

    /**
     * @return the dispatcherQueue
     */
    public ConcurrentLinkedQueue<SMPPDispatcher> getDispatcherQueue() {
        return dispatcherQueue;
    }
}
