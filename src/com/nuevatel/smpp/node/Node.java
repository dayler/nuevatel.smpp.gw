/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.node;

/**
 *
 * @author Luis Baldiviezo
 */
public class Node {
    protected int mcNodeId;
    protected boolean isServer=false;
    protected boolean isClient=false;

    /**
     * @return the mcNodeID
     */
    public int getMcNodeID() {
        return mcNodeId;
    }

    /**
     * @return true if node is SMPP Server
     */
    public boolean isServer(){
        return isServer;
    }

    /**
     * @return true if node is SMPP Client
     */
    public boolean isClient(){
        return isClient;
    }
}
