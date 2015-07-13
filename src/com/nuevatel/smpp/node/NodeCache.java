/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.node;

import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Luis Baldiviezo
 */
public class NodeCache {
    private ConcurrentHashMap<Integer,Node> nodes = new ConcurrentHashMap<Integer,Node>();
    /**
     * Puts a new ServerNode object in the Cache
     * @param serverNode The ServerNode object
     */
    public void put(Node node){
        nodes.put(node.getMcNodeID(), node);
    }
    /**
     * Returns The ServerNode object that has the mcNodeID
     * @param mcNodeID the mcNodeID
     * @return The ServerNode object that has the mcNodeID
     */
    public Node get(Integer mcNodeID){
        return nodes.get(mcNodeID);
    }

}
