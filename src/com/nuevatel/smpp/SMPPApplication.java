/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp;

import com.logica.smpp.SmppObject;
import com.logica.smpp.debug.Debug;
import com.logica.smpp.debug.DefaultDebug;
import com.logica.smpp.debug.DefaultEvent;
import com.logica.smpp.debug.Event;
import com.nuevatel.base.db.ConnectionPool;
import com.nuevatel.mc.appconn.MCClient;
import com.nuevatel.smpp.utils.PropertiesLoader;
import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author luis
 */
public abstract class SMPPApplication implements Runnable{

    public static final int APP_TYPE_SERVER=1;
    public static final int APP_TYPE_CLIENT=2;
    /**
     * @return the appType
     */
    public int getAppType() {
        return appType;
    }

    protected volatile boolean isRunning;
    /** The DB connection*/
    protected static int appType;
    protected Integer mcNodeId;
    protected MCClient mcClient;
    protected ConnectionPool dbConnectionPool;
    /** The AppClient Properties*/
    protected Properties appClientProperties;
    /** The log level */
    protected String logLevel;
    /* The SMPP stack log level*/
    protected boolean smppStackLogging=false;
    /**The mcPoolSize*/
    protected Integer mcPoolSize;
    /**The smppPoolSize*/
    protected Integer smppPoolSize;
    /**The serviceType*/
    protected byte serviceType;
    /**The expiration default timeout*/
    private Integer expirationThreadPoolSize;
    private Integer expirationDefaultTimeout;
    private String propertiesFile;
    private Integer clientReconnectionTimeout;
    private Integer clientEnquireLinkPeriod;
    private Integer clientNoActivityTimeout;
    private Integer processSleepMillis;
    private String emptyUDString=null;
    private Integer statPrintRate;
    private boolean fileLog;

    /*ton and npi*/
    private Byte smppSourceTon=null;
    private Byte smppSourceNpi=null;
    private Byte smppDestinationTon=null;
    private Byte smppDestinationNpi=null;

    // logger
    private final static LoggerHandler loggerHandler=new LoggerHandler();
    private final static Logger logger=Logger.getLogger("");
    static {
        for(Handler handler : logger.getHandlers()) logger.removeHandler(handler);
        logger.addHandler(loggerHandler);
        logger.setLevel(Level.INFO);
        logger.setUseParentHandlers(false);
    }

    private static class LoggerHandler extends Handler {
        @Override public void publish(LogRecord record) {
            String processID = ManagementFactory.getRuntimeMXBean().getName();
            String logRecord="";
            if(record.getLevel().intValue() < Level.OFF.intValue())
                logRecord=processID.substring(0, processID.indexOf("@"))+": " +
                          String.format("%1$tF %1$tT", new Date(record.getMillis())) + " " +
                          record.getLevel().getName() + " " +
                          record.getSourceClassName() + " " +
                          record.getSourceMethodName() + " " +
                          record.getMessage();
            else logRecord=processID.substring(0, processID.indexOf("@"))+": " +
                           String.format("%1$tF %1$tT", new Date(record.getMillis())) + " " +
                           record.getSourceMethodName() + " " +
                           record.getMessage();
            if(record.getLevel().intValue() > Level.INFO.intValue()) System.err.println(logRecord);
            else System.out.println(logRecord);
        }


        @Override public void flush() {}


        @Override public void close() throws SecurityException {}
    }

    public void start(){
        isRunning = true;
        Thread newAppThread = new Thread(this, getClass().getName());
        newAppThread.start();
    }

    public void shutdown(){
        isRunning = false;
    }

    public boolean isRunning(){
        return isRunning;
    }

    public boolean loadProperties(Properties properties){
        appType = properties.getProperty("appType").equals("client") ? APP_TYPE_CLIENT : APP_TYPE_SERVER;
        mcNodeId = Integer.valueOf(properties.getProperty("mcNodeId"));
        String dbHostname = properties.getProperty("DBHostname", "127.0.0.1");
        String dbSchema = properties.getProperty("DBSchema", "mc");
        String dbUsername = properties.getProperty("DBUsername", "mc");
        String dbPassword = properties.getProperty("DBPassword", "mcU5R");
        logLevel = properties.getProperty("logLevel", "INFO");
        smppStackLogging = Boolean.parseBoolean(properties.getProperty("smppStackLogging","false"));
        String appClientPropertiesFile = properties.getProperty("appClientProperties");
        if (properties.getProperty("smppSourceTon")!=null) smppSourceTon = Byte.valueOf(properties.getProperty("smppSourceTon"));
        if (properties.getProperty("smppSourceNpi")!=null) smppSourceNpi = Byte.valueOf(properties.getProperty("smppSourceNpi"));
        if (properties.getProperty("smppDestinationTon")!=null) smppDestinationTon = Byte.valueOf(properties.getProperty("smppDestinationTon"));
        if (properties.getProperty("smppDestinationNpi")!=null) smppDestinationNpi = Byte.valueOf(properties.getProperty("smppDestinationNpi"));
        mcPoolSize = Integer.valueOf(properties.getProperty("mcPoolSize","4"));
        smppPoolSize = Integer.valueOf(properties.getProperty("smppPoolSize","4"));
        serviceType = Byte.valueOf(properties.getProperty("serviceType","2"));
        expirationThreadPoolSize = Integer.valueOf(properties.getProperty("expirationThreadPoolSize","4"));
        expirationDefaultTimeout = Integer.valueOf(properties.getProperty("expirationDefaultTimeout","30"));
        clientReconnectionTimeout = Integer.valueOf(properties.getProperty("clientReconnectionTimeout","30"));
        clientEnquireLinkPeriod = Integer.valueOf(properties.getProperty("clientEnquireLinkPeriod","0"));
        clientNoActivityTimeout = Integer.valueOf(properties.getProperty("clientNoActivityTimeout","0"));
        processSleepMillis = Integer.valueOf(properties.getProperty("processSleepMillis","50"));
        emptyUDString = properties.getProperty("emptyUDString");
        setStatPrintRate(Integer.valueOf(properties.getProperty("statPrintRate", "0")));
        setFileLog(Boolean.parseBoolean(properties.getProperty("logDeliveryReceipt", "false")));


        
        // SMPP App logging
        setLogLevel(logLevel);
        
        //Logica SMPP stack logging
        Debug debug = new DefaultDebug();
        Event event = new DefaultEvent();
        SmppObject.setDebug(debug);
        SmppObject.setEvent(event);
        if (smppStackLogging){
            debug.activate();
            event.activate();
        }
        else{
            debug.deactivate();
            event.deactivate();
        }
        if (getAppType()>0 && getMcNodeId()!=null && dbHostname!=null && dbSchema!=null &&dbUsername!=null
                && dbPassword!=null && getLogLevel()!=null && appClientPropertiesFile!=null){
            try{
                dbConnectionPool = new ConnectionPool("com.mysql.jdbc.Driver", "jdbc:mysql://"+dbHostname+"/"+dbSchema, dbUsername, dbPassword, 1);
                appClientProperties = PropertiesLoader.getProperties(appClientPropertiesFile);
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
            return true;
        }
        else return false;
    }


    /**
     * @return the mcNodeId
     */
    public Integer getMcNodeId() {
        return mcNodeId;
    }

    /**
     * @return the mcClient
     */
    public MCClient getMcClient() {
        return mcClient;
    }

    /**
     * @return the dbConnectionPool
     */
    public ConnectionPool getDbConnectionPool() {
        return dbConnectionPool;
    }

    /**
     * @return the logLevel
     */
    public String getLogLevel() {
        return logLevel;
    }

    /**
     * @return the mcPoolSize
     */
    public Integer getMcPoolSize() {
        return mcPoolSize;
    }
    /**
     * @return the smppPoolSize
     */
    public Integer getSmppPoolSize() {
        return smppPoolSize;
    }

    /**
     * @return the serviceType
     */
    public byte getServiceType() {
        return serviceType;
    }

    /**
     * @return the expirationThreadPoolSize
     */
    public Integer getExpirationThreadPoolSize() {
        return expirationThreadPoolSize;
    }

    /**
     * @return the expirationDefaultTimeout
     */
    public Integer getExpirationDefaultTimeout() {
        return expirationDefaultTimeout;
    }

    /**
     * @return the propertiesFile
     */
    public String getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * @param propertiesFile the propertiesFile to set
     */
    public void setPropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    public Integer getClientReconnectionTimeout(){
        return clientReconnectionTimeout;
    }

    /**
     * @return the clientEnquireLinkPeriod
     */
    public Integer getClientEnquireLinkPeriod() {
        return clientEnquireLinkPeriod;
    }

    /**
     * @return the clientNoActivityTimeout
     */
    public Integer getClientNoActivityTimeout() {
        return clientNoActivityTimeout;
    }


    public String getEmptyUDString(){
        return emptyUDString;
    }

    public void setLogLevel (String logLevel){
        this.logLevel=logLevel;
        logger.setLevel(Level.parse(this.logLevel));
        logger.info("Log Level set to "+this.logLevel+". SMPP Stack level set to "+smppStackLogging);
    }
    
    /**
     * @return the statPrintRate
     */
    public Integer getStatPrintRate() {
        return statPrintRate;
    }

    /**
     * @param statPrintRate the statPrintRate to set
     */
    public void setStatPrintRate(Integer statPrintRate) {
        this.statPrintRate = statPrintRate;
    }

    /**
     * @return the smppSourceTon
     */
    public Byte getSmppSourceTon() {
        return smppSourceTon;
    }

    /**
     * @return the smppSourceNpi
     */
    public Byte getSmppSourceNpi() {
        return smppSourceNpi;
    }

    /**
     * @return the smppDestinationTon
     */
    public Byte getSmppDestinationTon() {
        return smppDestinationTon;
    }

    /**
     * @return the smppDestinationNpi
     */
    public Byte getSmppDestinationNpi() {
        return smppDestinationNpi;
    }

    /**
     * @return the processingSleepMillis
     */
    public Integer getProcessSleepMillis() {
        return processSleepMillis;
    }
    
    /**
     * @return the logFile
     */
    public boolean isFileLog() {
        return fileLog;
    }

    /**
     * @param logFile the logFile to set
     */
    public void setFileLog(boolean logFile) {
        this.fileLog = logFile;
    }

    
}
