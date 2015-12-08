/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.nuevatel.mc.smpp.gw.server;

import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 *
 * @author asalazar
 */
public class SmppServerGwProcessor extends SmppGwProcessor {

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
