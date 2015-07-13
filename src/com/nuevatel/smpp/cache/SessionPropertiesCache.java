/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.cache;

import com.nuevatel.smpp.session.ClientSessionPropertiesKey;
import com.nuevatel.smpp.session.ServerSessionPropertiesKey;
import com.nuevatel.smpp.session.SessionProperties;
import com.nuevatel.smpp.session.SessionPropertiesKey;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Luis Baldiviezo
 */
public class SessionPropertiesCache {
    /*private variables*/
    private ConcurrentHashMap<SessionPropertiesKey, SessionProperties> sessionPropertiesMap = new ConcurrentHashMap<SessionPropertiesKey, SessionProperties>();
    /**
     * Puts a new SessionProperties object in the cache
     * @param sessionProperties The SessionProperties object
     */
    public void put(SessionProperties sessionProperties){
        sessionPropertiesMap.put(sessionProperties.getKey(), sessionProperties);
    }
    /**
     * Looks up the Cache for the SessionProperties that matches systemID and bindType
     * This method should be used when dealing with server sessions
     * @param systemID The SMPP systemID
     * @param bindType The SMPP bind type
     * @return the SessionProperties object that matches systemID and bindType
     */
    public SessionProperties get(String systemID, int bindType){
        ServerSessionPropertiesKey key = new ServerSessionPropertiesKey(systemID, bindType);
        System.out.println("Looking by systemId:" + systemID + " bindType:" + bindType);
        SessionProperties props = sessionPropertiesMap.get(key);
        if (props == null) {
            System.out.println("SessionProperties is null...");
        }
        return props;
    }

    /**
     * Looks up the Cache for the SessionProperties that matches smppSessionId
     * This method should be used when dealing with client sessions
     * @param systemID The SMPP systemID
     * @param bindType The SMPP bind type
     * @return the SessionProperties object that matches systemID and bindType
     */
    public SessionProperties get(int smppSessionId){
        ClientSessionPropertiesKey key = new ClientSessionPropertiesKey(smppSessionId);
        return sessionPropertiesMap.get(key);
    }

    /**
     * Looks up the Cache for the SessionProperties that matches sessionPropertiesKey
     * This method can be used for both client and server sessions
     * @param sessionPropertiesKey The ServerSessionPropertiesKey
     * @return The SessionProperties object that matches sessionPropertiesKey
     */
    public SessionProperties get(SessionPropertiesKey sessionPropertiesKey){
        return sessionPropertiesMap.get(sessionPropertiesKey);
    }
    /**
     * @return the cache size
     */
    public int size(){
        return sessionPropertiesMap.size();
    }

    public Enumeration<SessionProperties> elements(){
        return sessionPropertiesMap.elements();
    }

    public Collection<SessionProperties> values(){
        return sessionPropertiesMap.values();
    }


}
