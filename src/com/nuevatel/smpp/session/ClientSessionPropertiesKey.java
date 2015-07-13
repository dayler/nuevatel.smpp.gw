/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

/**
 *
 * @author Luis Baldiviezo
 */
public class ClientSessionPropertiesKey extends SessionPropertiesKey {
    /*private variables */
    
    private int smppSessionId;

    /**
     * Creates a new ClientSessionPropertiesKey Object
     * @param smppSessionId The SMPP session ID
     */
    public ClientSessionPropertiesKey(int smppSessionId){
        this.smppSessionId=smppSessionId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.smppSessionId;
        return hash;
    }

    @Override public boolean equals(Object obj){
        if (obj==null) return false;
        if (obj==this) return true;
        if (!(obj instanceof ClientSessionPropertiesKey)) return false;

        ClientSessionPropertiesKey sessionCacheRecordKey = (ClientSessionPropertiesKey)obj;
        return  sessionCacheRecordKey.getSmppSessionId()==(smppSessionId);
    }

    /**
     * @return the smppSessionId
     */
    public int getSmppSessionId() {
        return smppSessionId;
    }
    
}
