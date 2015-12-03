package com.nuevatel.mc.smpp.gw;

import com.nuevatel.common.appconn.AppClient;
import com.nuevatel.mc.common.BaseApp;

/**
 * <p>The ProxyApp class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2015</p>
 *
 * @author Jorge Vasquez, Eduardo Marin
 * @version 1.0
 * @since 1.8
 */
public class ProxyApp extends BaseApp {

    /** The appClient. */
    private final AppClient appClient;

    /**
     * Creates a new instance of ProxyApp.
     * @param appId
     * @param state
     * @param appClient
     */
    public ProxyApp(int appId, STATE state, AppClient appClient) {
        super(appId, APP_TYPE.PROXY, state, null);
        this.appClient = appClient;
    }

    /**
     * Starts this.
     */
    public void start() {
        if (appClient != null) appClient.start();
    }

    /**
     * Interrupts this.
     */
    public void interrupt() {
        setState(STATE.OFFLINE);
        if (appClient != null) appClient.interrupt();
    }

    /**
     * Returns the appClient.
     * @return
     */
    public AppClient getAppClient() {
        return appClient;
    }
}
