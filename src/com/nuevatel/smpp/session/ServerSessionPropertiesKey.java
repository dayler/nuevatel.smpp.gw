/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

/**
 *
 * @author Luis Baldiviezo
 */
public class ServerSessionPropertiesKey extends SessionPropertiesKey {
    /*private variables*/
    private String systemId;
    private int bindType;

    /**
     * Creates a new ServerSessionPropertiesKey Object
     * @param systemId The SMPP systemID
     * @param bindType The SMPP bind type
     */
    public ServerSessionPropertiesKey(String systemId, int bindType){
        this.systemId=systemId;
        this.bindType=bindType;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + (this.systemId != null ? this.systemId.hashCode() : 0);
        hash = 89 * hash + this.bindType;
        return hash;
    }

    @Override public boolean equals(Object obj){
        if (obj==null) return false;
        if (obj==this) return true;
        if (!(obj instanceof ServerSessionPropertiesKey)) return false;

        ServerSessionPropertiesKey sessionCacheRecordKey = (ServerSessionPropertiesKey)obj;
        return  sessionCacheRecordKey.getSystemId().equals(systemId) &&
                sessionCacheRecordKey.getBindType()==(bindType);
    }

    /**
     * @return the systemId
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * @return the bindType
     */
    public int getBindType() {
        return bindType;
    }

}
