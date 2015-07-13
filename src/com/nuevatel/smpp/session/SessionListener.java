/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

import com.logica.smpp.Connection;
import com.logica.smpp.Data;
import com.logica.smpp.TCPIPConnection;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Luis Baldiviezo
 */
public class SessionListener implements Runnable{
    /*the default listen timeout in milliseconds*/
    private static final int LISTEN_TIMEOUT=100;

    private static final Logger logger = Logger.getLogger(SessionListener.class.getName());

    /*private variables*/
    private int port;
    private boolean isReceiving=false;
    private boolean keepReceiving=true;
    private Connection serverConnection;
    private long acceptTimeout=Data.ACCEPT_TIMEOUT;
    private int poolsize;


    public SessionListener(int port, int poolsize){
        this.port=port;
        this.poolsize=poolsize;
    }

    /**
     * Starts a new thread that listens for connections on the
     * specified port
     * @throws IOException
     */
    public synchronized void start() throws IOException{
        if (!isReceiving){
            serverConnection = new TCPIPConnection(port);
            serverConnection.setReceiveTimeout(getAcceptTimeout());
            serverConnection.open();
            keepReceiving=true;
            //Server is Asynchronous, start sessionListener as new thread
            Thread sessionListenerThread = new Thread(this);
            sessionListenerThread.start();
            logger.info("Now listening on port "+ port);
        }
    }
    /**
     * This runs an infinite loop that calls the <code>listen</code> method
     * and then waits for a short time. To adjust the time, modify the value of <code>LISTEN_TIMEOUT</code>
     */
    public void run() {
        isReceiving=true;
        try {
            while (keepReceiving){
                listen();
                try{
                    Thread.sleep(LISTEN_TIMEOUT);
                }catch (InterruptedException ex) {
                    Logger.getLogger(SessionListener.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        finally{
            isReceiving=false;
        }
    }
    /**
     * Attempts to stop the listener, this operation may take a while
     * @throws IOException
     */
    public synchronized void stop() throws IOException{
        keepReceiving=false;
        while (isReceiving){
            try {
                Thread.sleep(LISTEN_TIMEOUT);
            } catch (InterruptedException ex) {
                Logger.getLogger(SessionListener.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        serverConnection.close();
    }
    /**
     * Attempts to retrieve a new connection from the client.
     * If successful creates a new Session, sets its <code>SessionProperties</code>
     * and eventListener and then starts the Session in a new thread
     */
    private void listen(){
        try {
            Connection connection = null;
            connection = serverConnection.accept();
            if (connection!=null){
                connection.setReceiveTimeout(getAcceptTimeout());
                ServerSession serverSession = new ServerSession(connection);

//                In server mode we do not know the session properties until
//                the client has sent the  bind() request, we can't set them just now.
//                The session properties should be set as soon as the bind()
//                authentication has passed

                //serverSession.setSessionProperties(null);
                serverSession.setEventListener(new ServerEventListener(serverSession, poolsize));
                Thread sessionThread=new Thread(serverSession);
                sessionThread.setName("serverSession");
                sessionThread.start();
            }
        } catch (InterruptedIOException e) {
            // thrown when the timeout expires => it's ok, we just didn't
            // receive anything. put a log in here
            Logger.getLogger(SessionListener.class.getName()).log(Level.SEVERE, null, e);
        } catch (IOException ex) {
            // accept can throw this from various reasons
            // and we don't want to continue then (?)
            Logger.getLogger(SessionListener.class.getName()).log(Level.SEVERE, null, ex);
            keepReceiving=false;
        }
    }

    /**
     * @return the acceptTimeout
     */
    public long getAcceptTimeout() {
        return acceptTimeout;
    }

    /**
     * @param acceptTimeout the acceptTimeout to set
     */
    public void setAcceptTimeout(long acceptTimeout) {
        this.acceptTimeout = acceptTimeout;
    }

}
