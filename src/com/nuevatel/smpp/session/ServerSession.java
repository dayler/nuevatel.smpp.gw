/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

import com.logica.smpp.Connection;
import com.logica.smpp.ServerPDUEventListener;
import com.logica.smpp.TCPIPConnection;
import com.logica.smpp.Transmitter;
import com.logica.smpp.pdu.PDU;
import com.nuevatel.smpp.SMPPServerApplication;
import com.nuevatel.smpp.cache.CacheHandler;
import java.util.logging.Logger;

/**
 *
 * @author Luis Baldiviezo
 */
public class ServerSession extends Session{

    private ServerEventListener eventListener=null;
    private static final Logger logger = Logger.getLogger(ServerSession.class.getName());
    /**
     * Creates a new ServerSession object
     * @param connection The SMPP connection object, typically a TCPIPConnection object
     */
    public ServerSession(Connection connection){
        this.connection=(TCPIPConnection)connection;
        transmitter = new Transmitter(connection);
        receiver = new SessionReceiver(transmitter, this.connection, this, SMPPServerApplication.getSMPPServerApplication().getProcessSleepMillis());
        remoteAddress=this.connection.getSocket().getInetAddress().getHostAddress();
        remotePort=this.connection.getSocket().getPort();
        logger.fine("New ServerSession from: "+ remoteAddress+":"+remotePort);
    }

    /**
     * First sets this session's receiver with the <code>ServerPDUEventListener</code>.
     * Then starts the receiver in Asynchronous mode.
     * Always call <code>SetEventListener</code> prior to calling this method.
     */
    @Override public void run(){
        try {
            //set the eventlistener for this session befor starting receiver
            receiver.setServerPDUEventListener(eventListener);
            receiver.start();
        } catch (InterruptedException ex) {
            logger.severe(ex.getMessage());
        }
    }
    /**
     * Attempts to stop this session's receiver and also closes the connection
     */
    @Override public void stop(String reason){
        if (getState()!=STATE_CLOSED){
            try {
                if (reason!=null){
                    if (getSessionProperties()!=null)
                        logger.info("Stopping ServerSession from "+ remoteAddress+":"+remotePort +" smppSessionId: "+getSessionProperties().getSmppSessionId()+" Reason: "+reason);
                    else
                        logger.info("Stopping ServerSession from "+ remoteAddress+":"+remotePort +" smppSessionId: "+" Reason: "+reason);

                }
                setState(STATE_CLOSED);
                if (getEventListener().getThreadPool()!=null) getEventListener().getThreadPool().shutdown();
                if (getSessionProperties()!=null){
                    CacheHandler.getCacheHandler().getSessionCache().remove(getSessionProperties().getSmppSessionId(), this);
                    CacheHandler.getCacheHandler().decreaseSessionCount(getSessionProperties().getSmppSessionId());
                }
                stopMessageCounterResetTimer();
                if (connection!=null) connection.close();
                if (receiver!=null) receiver.stop();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    /**
     * Attempts to send the specified pdu though this session transmitter
     * @param pdu
     */
    @Override public void send(PDU pdu){
        try {
            transmitter.send(pdu);
        } catch (Exception ex) {
            logger.warning(pdu.getSequenceNumber() + " " +ex.getMessage());
        }
    } 

    /**
     * @return the eventListener
     */
    public ServerEventListener getEventListener() {
        return eventListener;
    }

    /**
     * @param eventListener the eventListener to set
     */
    public void setEventListener(ServerEventListener eventListener) {
        this.eventListener = eventListener;
    }
    /**
     * @return true if the ServerSession is in bound state
     */
    public boolean isBound(){
        if (getState()==STATE_BOUND_RX || getState()==STATE_BOUND_TX || getState()==STATE_BOUND_TRX) return true;
        else return false;
    }

}
