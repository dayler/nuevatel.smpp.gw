/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.node;

/**
 *
 * @author Luis Baldiviezo
 */
public class ServerNode extends Node{
    /*private variables*/
    private String bindAddress;
    private int port;
    /**
     * Creates a new ServerNode Object
     * @param mcNodeId The mcNodeId
     * @param bindAddress The Bind Address
     * @param port The port number
     */
    public ServerNode(int mcNodeId, String bindAddress, int port){
        this.mcNodeId = mcNodeId;
        this.bindAddress = bindAddress;
        this.port=port;
        isServer=true;
    }

    /**
     * @return the bindAddress
     */
    public String getBindAddress() {
        return bindAddress;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }
    
}