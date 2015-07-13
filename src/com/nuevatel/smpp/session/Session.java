/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

import com.logica.smpp.Data;
import com.logica.smpp.ServerPDUEventListener;
import com.logica.smpp.TCPIPConnection;
import com.logica.smpp.Transmitter;
import com.logica.smpp.pdu.PDU;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 *
 * @author Luis Baldiviezo
 */
public class Session implements Runnable{

    /*private variables*/
    protected SessionReceiver receiver;
    protected Transmitter transmitter;
    protected TCPIPConnection connection;
    protected String remoteAddress;
    protected int remotePort;

    /*SMPP states*/
    /*Bound states match Data.BIND... states */
    public static final int STATE_OPEN=0;
    public static final int STATE_BOUND_TX=Data.BIND_TRANSMITTER;
    public static final int STATE_BOUND_RX=Data.BIND_RECEIVER;
    public static final int STATE_BOUND_TRX=Data.BIND_TRANSCEIVER;
    public static final int STATE_CLOSED=Data.UNBIND;
    
    /*The state indicator*/
    private int state=STATE_OPEN;

    /*SMPP timers*/
    private static long timerSessionInit=10000;
    //private static long timerEnquireLink=60000; this is loaded from DB to the smppSessionProperties
    private static long timerInactivity=300000;
    private static long timerResponse=300000;

    /*The SessionProperties object*/
    private SessionProperties sessionProperties;
    private Timer messageCountResetTimer;
    private long messageCount=0;
    /**
     * The message id assigned by simulator to submitted messages.
     */
    private static AtomicInteger messageId = new AtomicInteger();
    private static final Logger logger = Logger.getLogger(Session.class.getName());

    /**/
    private Date lastReceivedMessageTimestamp = new Date();

    /**
     * @return the sessionProperties
     */
    public SessionProperties getSessionProperties() {
        return sessionProperties;
    }

    /**
     * @param sessionProperties the sessionProperties to set
     */
    public void setSessionProperties(SessionProperties sessionProperties) {
        this.sessionProperties = sessionProperties;
    }

    public void run() {
        //extend this method
    }

    public void stop(String Reason) {
        //extend this method
    }
    
    /**
     * @return the state
     */
    public int getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(int state) {
        this.state = state;
    }
    /**
     * Sends the PDU to the other end
     * @param pdu
     */
    public void send(PDU pdu){
        //extend this method
    }

    public void increaseMessageCount(){
        messageCount++;
    }
    public long getMessageCount(){
        return messageCount;
    }

    private void resetMessageCount(){
        messageCount=0;
    }

    public void startMessageCounterResetTimer(){
        if (getSessionProperties()!=null){
            if (getSessionProperties().getMessageLimit()!=0){
                messageCountResetTimer = new Timer();
                TimerTask messageCountResetTask = new TimerTask() {
                    @Override public void run() {
                        resetMessageCount();
                    }
                };
                messageCountResetTimer.schedule(messageCountResetTask, 0, getSessionProperties().getMessageLimitWindow()*1000);
            }
            else {
                logger.fine("No message limit found");
            }
        }
        else {
            logger.severe("Properties not loaded, can't start messageReset timer");
        }
    }

    public void stopMessageCounterResetTimer(){
        if (messageCountResetTimer!=null)
            messageCountResetTimer.cancel();
    }

    /**
     * Creates a unique message_id for each sms sent by a client to the smsc.
     * @return unique message id
     */
    public static String getMessageId() {
        int id = messageId.addAndGet(1);
        if (id==Integer.MAX_VALUE) messageId=new AtomicInteger(0);
        String idString = String.valueOf(id);
        return idString;
    }

    public Date getLastReceivedMessageTimestamp(){
        return lastReceivedMessageTimestamp;
    }

    public void updateLastReceivedMessageTimestamp(){
        lastReceivedMessageTimestamp = new Date();
    }
}
