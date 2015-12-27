/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.domain.Config;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 *
 * @author asalazar
 */
public class SmppServerGwProcessor extends SmppGwProcessor {
    
    private static Logger logger = LogManager.getLogger(SmppServerGwProcessor.class);
    
    private List<SmppServerProcessor>smppServerProcessor = new ArrayList<>();
    
    private Config cfg = AllocatorService.getConfig();
    
    public SmppServerGwProcessor(SmppGwSession gwSession) {
        super(gwSession);
    }
    
    @Override
    public void execute() {
        System.out.println("com.nuevatel.mc.smpp.gw.server.ServerGwProcessor.execute()");
    }

    @Override
    public void shutdown(int i) {
        System.out.println("com.nuevatel.mc.smpp.gw.server.ServerGwProcessor.shutdown()");
    }
}
