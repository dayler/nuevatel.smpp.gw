/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.node;

/**
 *
 * @author Luis Baldiviezo
 */
public class ClientNode extends Node{
    /**
     * Creates a new ClientNode Object
     * @param mcNodeId The mcNodeId
     * @param bindAddress The Bind Address
     * @param port The port number
     */
    public ClientNode(int mcNodeId){
        this.mcNodeId = mcNodeId;
        isClient=true;
    }
}