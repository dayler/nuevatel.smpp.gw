/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.server;

import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.smpp.ServerPDUEvent;
import org.smpp.Session;

import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.dialog.DialogService;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.event.SmppEvent;

/**
 * 
 * @author Ariel Salazar
 *
 */
public class SmppServerProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppServerGwProcessor.class);
    
    /**
     * All incoming events from the SMSC (SMPP server)
     */
    private BlockingQueue<ServerPDUEvent>serverPduEvents;
    
    /**
     * All outgoing events. Messages to submit to remote SMSC (SMPP server)
     */
    private BlockingQueue<SmppEvent>smppEvents;
    
    private SmppGwSession gwSession;
    
    /**
     * Smpp session, bind from client to server.
     */
    private Session smppSession = null;
    
    private DialogService dialogService = AllocatorService.getDialogService();
    
    private boolean running = false;

    public SmppServerProcessor(SmppGwSession gwSession, BlockingQueue<ServerPDUEvent> serverPduEvents,
            BlockingQueue<SmppEvent> smppEvents) {
        Parameters.checkNull(gwSession, "gwSession");
        Parameters.checkNull(serverPduEvents, "serverPduEvents");
        Parameters.checkNull(smppEvents, "smppEvents");
        
        this.gwSession = gwSession;
        this.serverPduEvents = serverPduEvents;
        this.smppEvents = smppEvents;
    }
    
    /**
     * Handle incoming smpp messages. Consume from <b>serverPduEvents</b>.
     */
    public void receive() {
        // 
    }
    
    /**
     * Handle outgoing smpp messages. Consume from  <b>smppEvents</b>
     */
    public void dispatch() {
        // 
    }
    
    public boolean isRunning() {
        return running;
    }
}
