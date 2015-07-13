/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.session;

import com.nuevatel.smpp.node.Node;

/**
 *
 * @author Luis Baldiviezo
 */
public class SessionProperties {

    /*private variables*/
    private Node node;
    private String smppNodeId;
    private int smppSessionId;
    private String smppSessionName;
    private String state;
    private String encoding;
    private int encodingInt;
    private String systemId;
    private String password;
    private String systemType;
    private int bindType;
    private int size;
//    private int enquirePeriod;
    private String address;
    private int port;
    private long messageLimit;
    private int messageLimitWindow=1;
    private boolean enableSR=false;

    /*bind type constants*/
    public static final int BIND_TYPE_RECEIVER=1;
    public static final int BIND_TYPE_TRANSMITTER=2;
    public static final int BIND_TYPE_TRANSCEIVER=9;

    /*encondig constants*/
    public static final int ENCODING_GSM=1;
    public static final int ENCODING_LATIN1=2;

    /**
     * Creates a new SessionProperties
     * @param node The SMPP node to which this session belongs
     * @param smppSessionId The SMPP Session ID
     * @param smppSessionName The SMPP Session Name
     * @param state The session state
     * @param encoding The session assigned encoding
     * @param systemId The system ID
     * @param password The password
     * @param systemType The optional System Type
     * @param bindType The bind type (r=receiver, t=transmitter, tr=transceiver)
     * @param size The session instance size
     * @param enquirePeriod The enquire link period
     * @param address The bind address
     * @param port The bind port
     * @param messageLimit The message throttling limit in messages per second
     */
    public SessionProperties(  Node node,
                                int smppSessionId,
                                String smppSessionName,
                                String state,
                                String encoding,
                                String systemId,
                                String password,
                                String systemType,
                                String bindType,
                                int size,
//                                int enquirePeriod,
                                String address,
                                int port,
                                String messageLimtiArg,
                                int enabledeliveryReceipt
                            ){
        this.node=node;
        smppNodeId = new String("smpp://"+smppSessionId+"@"+node.getMcNodeID());
        this.smppSessionId=smppSessionId;
        this.smppSessionName=smppSessionName;
        this.state=state;
        this.encoding=encoding;
        this.encodingInt=(encoding.equals("latin1"))?ENCODING_LATIN1:ENCODING_GSM;
        this.systemId=systemId;
        this.password=password;
        this.systemType=systemType;
        if (bindType.equalsIgnoreCase("r")) this.bindType=BIND_TYPE_RECEIVER;
        else if (bindType.equalsIgnoreCase("t")) this.bindType=BIND_TYPE_TRANSMITTER;
        else if (bindType.equalsIgnoreCase("tr")) this.bindType=BIND_TYPE_TRANSCEIVER;
        this.size=size;
//        this.enquirePeriod=enquirePeriod;
        this.address=address;
        this.port=port;
        if (messageLimtiArg==null) messageLimtiArg="0";
        else if(messageLimtiArg.contains("/"))
        {
            messageLimit=Long.valueOf(messageLimtiArg.split("/")[0]);
            messageLimitWindow=Integer.valueOf(messageLimtiArg.split("/")[1]);
        }
        else{
            messageLimit=Long.valueOf(messageLimtiArg);
        }
        if (enabledeliveryReceipt>0) this.enableSR=true;
    }

    /**
     * @return the mcNodeId
     */
    public Node getNode() {
        return node;
    }

    /**
     * @return the smppSessionId
     */
    public int getSmppSessionId() {
        return smppSessionId;
    }

    /**
     * @return the smppSessionName
     */
    public String getSmppSessionName() {
        return smppSessionName;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @return the encoding
     */
    public String getEncoding(){
        return encoding;
    }

    /**
     * @return the encodingInt
     */
    public int getEncodingInt(){
        return encodingInt;
    }

    /**
     * @return the systemId
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the systemType
     */
    public String getSystemType() {
        return systemType;
    }

    /**
     * @return the bindType
     */
    public int getBindType() {
        return bindType;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @return the enquirePeriod
     */
//    public int getEnquirePeriod() {
//        return enquirePeriod;
//    }

    /**
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @return the messageLimit
     */
    public long getMessageLimit() {
        return messageLimit;
    }
    /**
     * @return the messageLimitWindow
     */
    public int getMessageLimitWindow() {
        return messageLimitWindow;
    }

    public boolean enableSR(){
        return enableSR;
    }
    /**
     * Returns the Key for this SessionProperties
     * @return ServerSessionPropertiesKey
     */
    public SessionPropertiesKey getKey(){
        if (node.isClient()) return new ClientSessionPropertiesKey(smppSessionId);
        else if (node.isServer()) return new ServerSessionPropertiesKey(systemId, bindType);
        else return null;
    }

    /**
     * @return True if Session is active in DB
     */
    public boolean isActive(){
        if (state.equalsIgnoreCase("active")) return true;
        else return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.node != null ? this.node.hashCode() : 0);
        hash = 67 * hash + this.smppSessionId;
        hash = 67 * hash + (this.smppSessionName != null ? this.smppSessionName.hashCode() : 0);
        hash = 67 * hash + (this.state != null ? this.state.hashCode() : 0);
        hash = 67 * hash + (this.encoding != null ? this.encoding.hashCode() : 0);
        hash = 67 * hash + this.encodingInt;
        hash = 67 * hash + (this.systemId != null ? this.systemId.hashCode() : 0);
        hash = 67 * hash + (this.password != null ? this.password.hashCode() : 0);
        hash = 67 * hash + this.bindType;
        hash = 67 * hash + this.size;
//        hash = 67 * hash + this.enquirePeriod;
        hash = 67 * hash + (int)this.messageLimit;
        hash = 67 * hash + this.messageLimitWindow;
        hash = 67 * hash + (this.enableSR == true ? 1 : 0);
        return hash;
    }

    @Override public boolean equals(Object obj){
        if (obj==null) return false;
        if (obj==this) return true;
        if (!(obj instanceof SessionProperties)) return false;

        SessionProperties sessionProperties = (SessionProperties)obj;
        return  sessionProperties.getBindType() == bindType &&
//                sessionProperties.getEnquirePeriod() == enquirePeriod &&
                sessionProperties.getNode() == node &&
                //sessionCacheRecord.getPort() == port &&
                sessionProperties.getSize() == size &&
                sessionProperties.getSmppSessionId() == smppSessionId &&
                //sessionCacheRecord.getAddress().equals(address) &&
                sessionProperties.getEncoding().equals(encoding) &&
                sessionProperties.getEncodingInt() == encodingInt &&
                sessionProperties.getMessageLimit() == (messageLimit) &&
                sessionProperties.getMessageLimitWindow() == (messageLimitWindow) &&
                sessionProperties.getPassword().equals(password) &&
                sessionProperties.getSmppSessionName().equals(smppSessionName) &&
                sessionProperties.getState().equals(state) &&
                sessionProperties.getSystemId().equals(systemId) &&
                sessionProperties.enableSR() == enableSR;
                //sessionCacheRecord.getSystemType().equals(systemType);
    }

    /**
     * @return the smppNodeId
     */
    public String getSmppNodeId() {
        return smppNodeId;
    }
}