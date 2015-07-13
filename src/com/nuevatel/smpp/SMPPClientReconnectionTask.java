/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp;

import com.logica.smpp.Connection;
import com.logica.smpp.Data;
import com.logica.smpp.TCPIPConnection;
import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.smpp.session.ClientEventListener;
import com.nuevatel.smpp.session.ClientSession;
import com.nuevatel.smpp.session.SessionProperties;
import java.util.logging.Logger;

/**
 *
 * @author luis
 */
public class SMPPClientReconnectionTask implements Runnable{
    private static final Logger logger = Logger.getLogger(SMPPClientApplication.class.getName());

    @Override public void run() {
        String remoteAddress=null;
            int remotePort=0;
        try{
            for (SessionProperties sessionProperties : CacheHandler.getCacheHandler().getSessionPropertiesCache().values()){
                int unboundSessionCount = sessionProperties.getSize() - CacheHandler.getCacheHandler().getSessionCount(sessionProperties.getSmppSessionId());
                for (int sessionCount = 0; sessionCount<unboundSessionCount; sessionCount++){
                    remoteAddress = sessionProperties.getAddress();
                    remotePort = sessionProperties.getPort();
                    Connection connection = new TCPIPConnection(remoteAddress, remotePort);
                    connection.setReceiveTimeout(Data.COMMS_TIMEOUT);
                    connection.open();
                    ClientSession clientSession = new ClientSession(connection, sessionProperties);
                    clientSession.setSessionProperties(sessionProperties);
                    clientSession.setEventListener(new ClientEventListener(clientSession,SMPPClientApplication.getSMPPClientApplication().getSmppPoolSize()));
                    Thread clientSessionThread = new Thread(clientSession);
                    clientSessionThread.start();
                }
            }
        }
        catch (Exception ex){
            logger.severe("No response from "+remoteAddress+":"+remotePort+" "+ex.getMessage());
        }
    }

}
