/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.cache;

import com.nuevatel.smpp.session.Session;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 *
 * @author luis
 */
public class SessionCache {

    private ConcurrentHashMap<Integer,SessionCacheController> sessionMap;
    private SessionCacheController currentSessionController;
    static final Logger logger = Logger.getLogger(SessionCache.class.getName());
    public SessionCache(){
        sessionMap = new ConcurrentHashMap<Integer, SessionCacheController>();
    }

    public synchronized void put(int smppSessionId, Session sessionInstance){
        currentSessionController = sessionMap.get(smppSessionId);
        if (currentSessionController == null){
            //if not in the list create and initialize
            currentSessionController = new SessionCacheController();
            currentSessionController.putSessionInstance(sessionInstance);
            sessionMap.put(smppSessionId, currentSessionController);
        }
        else {
            //if found, then add sessionInstance
            currentSessionController.putSessionInstance(sessionInstance);
        }
        CacheHandler.getCacheHandler().updateOnlineCount(smppSessionId, currentSessionController.getInstanceCount());
        logger.fine("Added SessionInstance to SMPP sessionID: "+smppSessionId+" SessionInstance count is:"+currentSessionController.getInstanceCount());
    }
    public synchronized Session get(int smppSessionID){
        currentSessionController = sessionMap.get(smppSessionID);
        if (currentSessionController == null){
            return null;
        }
        else{
            return currentSessionController.getSessionInstance();
        }
    }

    public synchronized void remove(int smppSessionId, Session sessionInstance){
        currentSessionController = sessionMap.get(smppSessionId);
        if (currentSessionController!=null){
            currentSessionController.removeSessionInstance(sessionInstance);
            CacheHandler.getCacheHandler().updateOnlineCount(smppSessionId, currentSessionController.getInstanceCount());
            logger.fine("Removed SessionInstance from SMPP sessionID: "+smppSessionId+" SessionInstance count is:"+currentSessionController.getInstanceCount());
            if (currentSessionController.getInstanceCount()==0){
                //if it was the last instance, remove controller as well
                sessionMap.remove(smppSessionId);
            }
        }
    }

    public int getInstanceCount(int smppSessionID){
        currentSessionController = sessionMap.get(smppSessionID);
        if (currentSessionController == null){
            return 0;
        }
        else{
            return currentSessionController.getInstanceCount();
        }
    }

    public List<List<Session>> getSessionInstanceListList(){
        List<List<Session>> sessionList = new ArrayList<List<Session>>();
        for (SessionCacheController controller : sessionMap.values()){
            sessionList.add(controller.getInstanceList());
        }
        return sessionList;
    }
}
