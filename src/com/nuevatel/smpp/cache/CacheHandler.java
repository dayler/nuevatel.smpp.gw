/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.cache;

import com.nuevatel.base.db.Connection;
import com.nuevatel.base.db.ConnectionPool;
import com.nuevatel.smpp.node.ClientNode;
import com.nuevatel.smpp.node.Node;
import com.nuevatel.smpp.node.NodeCache;
import com.nuevatel.smpp.node.ServerNode;
import com.nuevatel.smpp.session.SessionProperties;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.logging.Logger;

/**
 *
 * @author Luis Baldiviezo
 */
public class CacheHandler {
    /**
     * the CacheHandler Object
     */
    private static CacheHandler cacheHandler;
    /*
     * The application caches
     */
    private SessionPropertiesCache sessionPropertiesCache=null;
    private NodeCache nodeCache = null;
    private ConcurrentHashMap<Integer, Integer> sessionCountMap = new ConcurrentHashMap<Integer, Integer>();
    private ConcurrentHashMap<Integer, SessionProperties> unboundSessionPropertiesMap = new ConcurrentHashMap<Integer, SessionProperties>();
    private static final Logger logger = Logger.getLogger(CacheHandler.class.getName());
    private ConcurrentHashMap<Integer, String> pendingResponsesMap = new ConcurrentHashMap<Integer, String>();
    private ConcurrentHashMap<String, Integer> pendingResponsesSRIMap = new ConcurrentHashMap<String, Integer>();
    private SessionCache sessionCache = new SessionCache();
    private ConcurrentHashMap<String, RunnableScheduledFuture> expirationTaksMap = new ConcurrentHashMap<String, RunnableScheduledFuture>();
    private ConcurrentHashMap<String, DeliveryReceiptAddressHolder> deliveryReceiptAddressMap = new ConcurrentHashMap<String, DeliveryReceiptAddressHolder>();
    private String emptyUDString=null;

    /*smpp ton and npi*/
    private Byte smppSourceTon=null;
    private Byte smppSourceNpi=null;
    private Byte smppDestinationTon=null;
    private Byte smppDestinationNpi=null;
    /*mc ton and npi*/
    private Byte mcSourceTon=null;
    private Byte mcSourceNpi=null;
    private Byte mcDestinationTon=null;
    private Byte mcDestinationNpi=null;
    
    //The cache handler needs the dbConnection and MCNodeID to update the status in DB
    //ALWAYS set this prior to establish new sessions client or server
    private ConnectionPool dbConnectionPool;
    private int MCNodeId;
    private boolean fileLog;

    private String nodeCacheQuery = "SELECT * FROM `smpp_server_node` sn WHERE sn.`mc_node_id` =?";
    private String sessionCacheQuery = "SELECT * FROM `smpp_session` ss WHERE ss.`mc_node_id` = ?;";
    private String onlineCountQuery="UPDATE `smpp_session` SET `online`=? WHERE `mc_node_id`=? AND `smpp_session_id`=?";

    private CacheHandler(){
    }

    /**
     * Return the CacheHandler Object
     * @return CacheHandler
     */

    public static CacheHandler getCacheHandler(){
        if (cacheHandler==null) cacheHandler = new CacheHandler();
        return cacheHandler;
    }

    /**
     * Loads the SMPP Session cache for the specified MC Node ID
     * @param connection The DB Connection
     * @param mcNodeId  The MC Node ID
     * @return SessionPropertiesCache
     */
    public SessionPropertiesCache loadSessionCache(int mcNodeId){
        sessionPropertiesCache = new SessionPropertiesCache();
        try{
            ResultSet resultSet = null;
            PreparedStatement nodeCacheStatement = null;
            PreparedStatement sessionCacheStatement = null;
            Connection dbConnection=null;

            try {
                dbConnection = getDBConnectionPool().getConnection();
                nodeCacheStatement = dbConnection.prepareStatement(nodeCacheQuery);
                nodeCacheStatement.setInt(1, mcNodeId);
                resultSet = nodeCacheStatement.executeQuery();
                Node node = null;
                nodeCache = new NodeCache();
                if (resultSet.next()){
                    node = new ServerNode(resultSet.getInt("mc_node_id"),
                                          resultSet.getString("bind_address"),
                                          resultSet.getInt("port"));
                }
                else{
                    node = new ClientNode(mcNodeId);
                }
                nodeCache.put(node);
                nodeCacheStatement.close();

                sessionCacheStatement = dbConnection.prepareStatement(sessionCacheQuery);
                sessionCacheStatement.setInt(1, mcNodeId);
                resultSet = sessionCacheStatement.executeQuery();
                while (resultSet.next()) {
                    SessionProperties sessionProperties =
                            new SessionProperties(
                                node,
                                resultSet.getInt("smpp_session_id"),
                                resultSet.getString("smpp_session_name"),
                                resultSet.getString("state"),
                                resultSet.getString("encoding"),
                                resultSet.getString("system_id"),
                                resultSet.getString("password"),
                                resultSet.getString("system_type"),
                                resultSet.getString("bind_type"),
                                resultSet.getInt("size"),
    //                            resultSet.getInt("enquire_period"),
                                resultSet.getString("address"),
                                resultSet.getInt("port"),
                                resultSet.getString("message_limit"),
                                resultSet.getInt("enable_sr"));
                    sessionPropertiesCache.put(sessionProperties);
                }
                logger.info("sessionPropertiesCache loaded for mcNodeId: "+mcNodeId+". Size "+sessionPropertiesCache.size());
                if (sessionPropertiesCache.size()==0) {
                    logger.warning("sessionPropertiesCache empty for mcNodeId: "+mcNodeId);
                }
            }
            catch(Exception e){
                logger.severe("cannot load cache for mcNodeId: " + mcNodeId + " " + e.getMessage());
            }
            finally{
                nodeCacheStatement.close();
                sessionCacheStatement.close();
                dbConnection.setState(Connection.IDLE);
                for (SessionProperties sessionProperties:getSessionPropertiesCache().values()){
                    updateOnlineCount(sessionProperties.getSmppSessionId(), 0);
                }
            }
        }
        catch (Exception ex){
            logger.severe(ex.getMessage());
        }
        return sessionPropertiesCache;
    }

    public void updateOnlineCount(int smppSessionId, int count){
        try{
            Connection dbConnection = null;
            PreparedStatement updateCountStatement=null;
            try {
                dbConnection = getDBConnectionPool().getConnection();
                updateCountStatement = dbConnection.prepareStatement(onlineCountQuery);
                updateCountStatement.setInt(1, count);
                updateCountStatement.setInt(2, getMCNodeId());
                updateCountStatement.setInt(3, smppSessionId);
                updateCountStatement.execute();
            } catch (SQLException e) {
                logger.severe("Cannot update online count in DB "+smppSessionId +" "+count+ " "+e.getMessage());
            }
            finally{
                updateCountStatement.close();
                dbConnection.setState(Connection.IDLE);
            }
        }
        catch(Exception ex){
            logger.severe(ex.getMessage());
        }
        
    }
    /**
     * returns the SessionPropertiesCache object
     * @return SessionPropertiesCache
     */

    public SessionPropertiesCache getSessionPropertiesCache(){
        return sessionPropertiesCache;
    }

    public NodeCache getNodeCache(){
        return nodeCache;
    }

    public synchronized void increaseSessionCount(Integer smppSessionId){
        Integer currentCount = getSessionCount(smppSessionId);
        if (currentCount==0) sessionCountMap.put(smppSessionId, 1);
        else sessionCountMap.replace(smppSessionId, ++currentCount);
        logger.finer("sessionCountSize for "+smppSessionId+": "+getSessionCount(smppSessionId));
    }
    public synchronized void decreaseSessionCount(Integer smppSessionId){
        Integer currentCount = getSessionCount(smppSessionId);
        if (currentCount==0) sessionCountMap.put(smppSessionId, 0);
        else sessionCountMap.replace(smppSessionId, --currentCount);
        logger.finer("sessionCountSize for "+smppSessionId+": "+getSessionCount(smppSessionId));
    }
    public synchronized Integer getSessionCount(Integer smppSessionId){
        Integer currentCount = sessionCountMap.get(smppSessionId);
        if (currentCount!=null){
            return currentCount;
        }
        else return 0;
    }

    public ConcurrentHashMap<Integer, String> getPendingResponsesMap(){
        return pendingResponsesMap;
    }

    public ConcurrentHashMap<String, Integer> getPendingResponsesSRIMap(){
        return pendingResponsesSRIMap;
    }

    public ConcurrentHashMap<Integer, SessionProperties> getUnboundSessionPropertiesMap(){
        return unboundSessionPropertiesMap;
    }

    public SessionCache getSessionCache(){
        return sessionCache;
    }

    public ConcurrentHashMap<String, RunnableScheduledFuture> getExpirationTasksMap(){
        return expirationTaksMap;
    }

    public void setDBConnection(ConnectionPool dbConnectionPool){
        this.dbConnectionPool=dbConnectionPool;
    }

    public ConnectionPool getDBConnectionPool(){
        return this.dbConnectionPool;
    }

    public void setMCNodeID(int MCNodeId){
        this.MCNodeId=MCNodeId;
    }

    public int getMCNodeId(){
        return MCNodeId;
    }

    /**
     * @return the deliveryReceiptAddressMap
     */
    public ConcurrentHashMap<String, DeliveryReceiptAddressHolder> getDeliveryReceiptAddressMap() {
        return deliveryReceiptAddressMap;
    }

    /**
     * @return the emptyUDString
     */
    public String getEmptyUDString() {
        return emptyUDString;
    }

    /**
     * @param emptyUDString the emptyUDString to set
     */
    public void setEmptyUDString(String emptyUDString) {
        this.emptyUDString = emptyUDString;
    }

    /**
     * @return the smppSourceTon
     */
    public Byte getSmppSourceTon() {
        return smppSourceTon;
    }

    /**
     * @param smppSourceTon the smppSourceTon to set
     */
    public void setSmppSourceTon(Byte smppSourceTon) {
        this.smppSourceTon = smppSourceTon;
    }

    /**
     * @return the smppSourceNpi
     */
    public Byte getSmppSourceNpi() {
        return smppSourceNpi;
    }

    /**
     * @param smppSourceNpi the smppSourceNpi to set
     */
    public void setSmppSourceNpi(Byte smppSourceNpi) {
        this.smppSourceNpi = smppSourceNpi;
    }

    /**
     * @return the smppDestinationTon
     */
    public Byte getSmppDestinationTon() {
        return smppDestinationTon;
    }

    /**
     * @param smppDestinationTon the smppDestinationTon to set
     */
    public void setSmppDestinationTon(Byte smppDestinationTon) {
        this.smppDestinationTon = smppDestinationTon;
    }

    /**
     * @return the smppDestinationNpi
     */
    public Byte getSmppDestinationNpi() {
        return smppDestinationNpi;
    }

    /**
     * @param smppDestinationNpi the smppDestinationNpi to set
     */
    public void setSmppDestinationNpi(Byte smppDestinationNpi) {
        this.smppDestinationNpi = smppDestinationNpi;
    }

    /**
     * @return the mcSourceTon
     */
    public Byte getMcSourceTon() {
        return mcSourceTon;
    }

    /**
     * @param mcSourceTon the mcSourceTon to set
     */
    public void setMcSourceTon(Byte mcSourceTon) {
        this.mcSourceTon = mcSourceTon;
    }

    /**
     * @return the mcSourceNpi
     */
    public Byte getMcSourceNpi() {
        return mcSourceNpi;
    }

    /**
     * @param mcSourceNpi the mcSourceNpi to set
     */
    public void setMcSourceNpi(Byte mcSourceNpi) {
        this.mcSourceNpi = mcSourceNpi;
    }

    /**
     * @return the mcDestinationTon
     */
    public Byte getMcDestinationTon() {
        return mcDestinationTon;
    }

    /**
     * @param mcDestinationTon the mcDestinationTon to set
     */
    public void setMcDestinationTon(Byte mcDestinationTon) {
        this.mcDestinationTon = mcDestinationTon;
    }

    /**
     * @return the mcDestinationNpi
     */
    public Byte getMcDestinationNpi() {
        return mcDestinationNpi;
    }

    /**
     * @param mcDestinationNpi the mcDestinationNpi to set
     */
    public void setMcDestinationNpi(Byte mcDestinationNpi) {
        this.mcDestinationNpi = mcDestinationNpi;
    }

    /**
     * @return the fileLog
     */
    public boolean isFileLog() {
        return fileLog;
    }

    /**
     * @param fileLog the fileLog to set
     */
    public void setFileLog(boolean fileLog) {
        this.fileLog = fileLog;
    }

    

}
