package com.nuevatel.mc.smpp.gw;

import com.nuevatel.common.ShutdownHook;
import com.nuevatel.common.appconn.AppClient;
import com.nuevatel.common.wsconn.*;
import com.nuevatel.mc.appconn.McMessage;
import com.nuevatel.mc.common.BaseApp;
import com.nuevatel.mc.common.GenericApp;
import com.nuevatel.mc.smpp.gw.appconn.ForwardSmOTask;
import com.nuevatel.mc.smpp.gw.client.SmppClienGwProcessor;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;
import com.nuevatel.mc.smpp.gw.mcdispatcher.McDispatcher;
import com.nuevatel.mc.smpp.gw.server.SmppServerGwProcessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>The SmppGwApp class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2015</p>
 *
 * @author Ariel Salazar, Jorge Vasquez, Eduardo Marin
 * @version 1.0
 * @since 1.8
 */
public class SmppGwApp extends GenericApp {
    
    private static Logger logger  = LogManager.getLogger(SmppGwApp.class);

    /** The smppGwApp. */
    private static final SmppGwApp smppGwApp = new SmppGwApp();

    /** The smppGwSessionMap. */
    private Map<Integer, SmppGwSession>smppGwSessionMap = new HashMap<>();
    
    /**
     * Service to execute gw processors (client and server). It is initialized on start()
     */
    private ExecutorService service = null;
    
    /**
     * Creates a new instance of SmppGwApp.
     */
    private SmppGwApp() {
        super();
        setAppType(APP_TYPE.SMPP_GW);
        mgmtAppList = null;
    }
    
    @Override
    public void start() throws Exception {
        try {
            setProperties();
            super.start();
            // Initialize McDispatcher
            AllocatorService.startMcDispatcher(smppGwSessionMap.size());
            // Initialize processors
            ShutdownHook hook = new ShutdownHook(60, 1); // 60 timeout 1 thread
            service = Executors.newFixedThreadPool(smppGwSessionMap.size());
            smppGwSessionMap.forEach((k, gwSession) -> {
                SmppGwProcessor processor = makeGwProcessor(gwSession.getSmppType(), gwSession);
                // Initialize gw processors
                service.execute(()->processor.execute());
                hook.appendProcess(processor);
                // register processors
                AllocatorService.registerSmppGwProcessor(k, processor);
            });
            // proxyAppMap
            remoteBaseAppMap.values().stream().filter((baseApp) -> baseApp.getState() == STATE.ONLINE)
                                              .map((baseApp) -> (ProxyApp) baseApp)
                                              .forEach((proxyApp) -> proxyApp.getAppClient().start());
            // updateState
            setState(STATE.ONLINE);
            BaseApp mgmtApp = mgmtApp();
            if (mgmtApp != null) {
                callUpdateState(mgmtApp.getWsURL(), getAppId(), getAppId(), getState());
            }
            logger.info("start", "smppGwId " + getAppId() + " state " + getState().getName());
            // Register hook
            Runtime.getRuntime().addShutdownHook(hook);
        } catch (Throwable ex) {
            logger.error("Failed on start...", ex);
            interrupt();
            throw ex;
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        // Shutdown processors
        AllocatorService.shutdownSmppGwProcessors(); // await 60 seconds to finish all
        smppGwSessionMap.forEach((smppGwSessionId, smppGwSession) -> smppGwSession.setBound(0));
        AllocatorService.shutdownMcDispatcher();
        logger.warn("state " + getState().getName());
    }

    @Override
    protected byte updateState(int fromAppId, int appId, STATE state) {
        if (fromAppId != getAppId()) {
            // mgmt
            for (BaseApp mgmtApp : mgmtAppList) {
                if (mgmtApp.getAppId() == appId && mgmtApp.getState() != state) {
                    mgmtApp.setState(state);
                    logger.log(state == STATE.OFFLINE ? Level.WARN : Level.INFO, "appId " + mgmtApp.getAppId() + " state " + state.getName());
                    return WsConn.ACCEPTED;
                }
            }
            // proxy
            BaseApp proxyApp = remoteBaseAppMap.get(appId);
            if (proxyApp != null && proxyApp.getState() != state) {
                if ((proxyApp.getState() == STATE.UNKNOWN
                    || proxyApp.getState() == STATE.OFFLINE)
                    && !(state == STATE.UNKNOWN
                    || state == STATE.OFFLINE)) {
                    ((ProxyApp) proxyApp).getAppClient().start();
                } else if (state == STATE.UNKNOWN
                        || state == STATE.OFFLINE) {
                    ((ProxyApp) proxyApp).getAppClient().interrupt();
                }
                proxyApp.setState(state);
                logger.log((state == STATE.OFFLINE) ? Level.WARN : Level.INFO, "appId " + proxyApp.getAppId() + " state " + state.getName());
                return WsConn.ACCEPTED;
            }
            // this
            if (appId == getAppId()) {
                if (state != getState()) {
                    // offline
                    if (state == STATE.OFFLINE) {
                        setState(state);
                        logger.warn("state " + state.getName());
                        scheduleInterrupt(4000);
                        return WsConn.ACCEPTED;
                    }
                }
            }
        }
        return WsConn.FAILED;
    }

    /**
     * Returns the smppGwApp.
     * @return
     */
    public static SmppGwApp getSmppGwApp() {
        return smppGwApp;
    }
    
    /**
     * 
     * @return AppClient instance.
     */
    public AppClient getAppClient() {
        return ((ProxyApp)getRemoteBaseApp(nextRemoteBaseAppId())).getAppClient();
    }
    /**
     * Sets the bound.
     * @param smppGwId
     * @param smppSessionId
     * @param bound
     */
    public void setBound(int smppGwId, int smppSessionId, int bound) {
        WsClient wsClient = new WsClient(mgmtApp().getWsURL());
        try {
            wsClient.call(GenericApp.WS_METHOD.SET_BOUND.getName(),
                          TIMEOUT,
                          new IntIe(WS_IE.APP_ID.getName(), smppGwId),
                          new IntIe(WS_IE.SMPP_SESSION_ID.getName(), smppSessionId),
                          new IntIe(WS_IE.BOUND.getName(), bound));
        } catch (Exception ex) {
            logger.error("Failed on wsClient.call", ex);
        }
    }
    
    public SmppGwSession getSmppGwSession(int smppGwId) {
        return smppGwSessionMap.get(smppGwId);
    }
    
    /**
     * Sets the properties.
     */
    private void setProperties() {
        // mgmtApp
        BaseApp mgmtApp = mgmtApp();
        if (mgmtApp == null) throw new RuntimeException("null mgmtApp");

        WsClient wsClient = new WsClient(mgmtApp.getWsURL());
        try {
            // smppGwPropertiesList
            List<Ie> ies = wsClient.call(WS_METHOD.GET_SMPP_GW_PROPERTIES_LIST.getName(), TIMEOUT, new IntIe(WS_IE.APP_ID.getName(), getAppId()));
            if (ies != null && ies.size() > 0) {
                ies.stream().map((ie) -> (CompositeIe) ie).forEach((smppGwProperties) -> {
                    SmppGwSession session = new SmppGwSession(getAppId(),
                                                                Integer.parseInt(smppGwProperties.getString(WS_IE.SMPP_SESSION_ID.getName())),
                                                                smppGwProperties.getString(WS_IE.SMPP_SESSION_NAME.getName()),
                                                                Integer.parseInt(smppGwProperties.getString(WS_IE.MC_ID.getName())),
                                                                SmppGwSession.MGMT_STATE.getMgmtState(smppGwProperties.getString(WS_IE.MGMT_STATE.getName())),
                                                                SmppGwSession.SMPP_TYPE.getSmppType(smppGwProperties.getString(WS_IE.SMPP_TYPE.getName())),
                                                                Boolean.parseBoolean(smppGwProperties.getString(WS_IE.REGISTERED_DELIVERY.getName())),
                                                                smppGwProperties.getString(WS_IE.SMSC_ADDRESS.getName()),
                                                                Integer.parseInt(smppGwProperties.getString(WS_IE.SMSC_PORT.getName())),
                                                                SmppGwSession.BIND_TYPE.getBindType(smppGwProperties.getString(WS_IE.BIND_TYPE.getName())),
                                                                smppGwProperties.getString(WS_IE.SYSTEM_ID.getName()),
                                                                smppGwProperties.getString(WS_IE.PASSWORD.getName()),
                                                                smppGwProperties.getString(WS_IE.SYSTEM_TYPE.getName()),
                                                                smppGwProperties.getString(WS_IE.R_SOURCE_ADDR_REGEX.getName()),
                                                                Integer.parseInt(smppGwProperties.getString(WS_IE.MAX_BINDS.getName())),
                                                                (smppGwProperties.getString(WS_IE.THROTTLE_LIMIT.getName()) != null) ? Integer.parseInt(smppGwProperties.getString(WS_IE.THROTTLE_LIMIT.getName())) : null,
                                                                Integer.parseInt(smppGwProperties.getString(WS_IE.BOUND.getName())));
                            smppGwSessionMap.put(session.getSmppSessionId(), session);
                            });
            }
            else throw new RuntimeException("invalid ret for " + WS_METHOD.GET_SMPP_GW_PROPERTIES_LIST.getName());

            // wsServerProperties
            wsServerProperties = callGetProperties(mgmtApp.getWsURL(), WS_METHOD.GET_WS_SERVER_PROPERTIES, new IntIe(WS_IE.APP_ID.getName(), getAppId()));

            // proxyAppList
            ies = wsClient.call(WS_METHOD.GET_PROXY_APP_LIST.getName(), TIMEOUT);
            if (ies != null && ies.size() > 0) {
                // appClientTaskSet
                com.nuevatel.common.appconn.TaskSet appClientTaskSet = new com.nuevatel.common.appconn.TaskSet();
                appClientTaskSet.add(McMessage.FORWARD_SM_O_CALL, new ForwardSmOTask());
                
                for (Ie ie : ies) {
                    Integer proxyId = ((CompositeIe) ie).getInt(WS_IE.APP_ID.getName());
                    remoteBaseAppMap.put(proxyId, // Proxy Id
                                         new ProxyApp(proxyId, STATE.getState(((CompositeIe) ie).getString(WS_IE.STATE.getName())), // BaseApp
                                         new AppClient(getAppId(), proxyId, appClientTaskSet, callGetProperties(mgmtApp.getWsURL(), WS_METHOD.GET_APP_CLIENT_PROPERTIES, new IntIe(WS_IE.FROM_APP_ID.getName(), getAppId()), new IntIe(WS_IE.APP_ID.getName(), proxyId)))));
                }
            }
            else throw new RuntimeException("invalid ret for " + WS_METHOD.GET_GMAP_GW_APP_LIST.getName());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * 
     * @param smppType
     * @param gwSession
     * @return ServerGwProcessor for SMPP_TYPE.ESME SmppClientProcessor in other case.
     */
    private SmppGwProcessor makeGwProcessor(SmppGwSession.SMPP_TYPE smppType, SmppGwSession gwSession) {
        if (SmppGwSession.SMPP_TYPE.SMSC.equals(smppType)) {
            // Server
            return new SmppServerGwProcessor(gwSession);
        }
        // Client
        return new SmppClienGwProcessor(gwSession);
    }
}
