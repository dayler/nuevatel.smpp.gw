/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.cache;

import com.nuevatel.smpp.session.Session;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author luis
 */
public class SessionCacheController {

    private final List<Session> sessionInstanceList;
    private Iterator<Session> sessionIterator;

    public SessionCacheController(){
        sessionInstanceList = Collections.synchronizedList(new ArrayList<Session>());
        synchronized (sessionInstanceList){
            sessionIterator=sessionInstanceList.iterator();
//            System.out.println(sessionInstanceList.size());
        }

    }

    public Session getSessionInstance(){
        Session session = null;
        synchronized(sessionInstanceList){
            if (!sessionIterator.hasNext()){
                sessionIterator = sessionInstanceList.iterator();
                //System.out.println(sessionInstanceList.size());
            }
            if (sessionIterator.hasNext()){
                session = sessionIterator.next();
            }
            return session;
        }
    }

    public void putSessionInstance(Session sessionInstance){
        synchronized(sessionInstanceList){
            sessionInstanceList.add(sessionInstance);
            sessionIterator = sessionInstanceList.iterator();
//            System.out.println(sessionInstanceList.size());
        }
    }

    public void removeSessionInstance(Session sessionInstance){
        synchronized(sessionInstanceList){
            sessionInstanceList.remove(sessionInstance);
            sessionIterator = sessionInstanceList.iterator();
//            System.out.println(sessionInstanceList.size());
        }
    }
    public void removeAllInstances(){
        synchronized(sessionInstanceList){
            sessionInstanceList.clear();
            sessionIterator = sessionInstanceList.iterator();
        }
    }

    public List<Session> getInstanceList(){
            return sessionInstanceList;
    }

    public int getInstanceCount(){
        synchronized (sessionInstanceList){
            return sessionInstanceList.size();
        }
    }

}