package com.nuevatel.mc.smpp.gw.domain;

import com.nuevatel.common.util.StringUtils;
import com.nuevatel.mc.smpp.gw.SmppGwApp;

/**
 * <p>The SmppGwSession class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2015</p>
 *
 * @author Jorge Vasquez, Eduardo Marin
 * @version 1.0
 * @since 1.8
 */
public class SmppGwSession {

    private final int smppGwId;
    private final int smppSessionId;
    private final String smppSessionName;
    private final int mcId;
    private final MGMT_STATE mgmtState;
    private final SMPP_TYPE smppType;
    private final boolean registeredDelivery;
    private final String smscAddress;
    private final int smscPort;
    private final BIND_TYPE bindType;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String rSourceAddrRegex;
    private final int maxBinds;
    private final Integer throttleLimit;
    private int bound;

    /**
     * Creates a new instance of SmppGwSession.
     * @param smppGwId
     * @param smppSessionId
     * @param smppSessionName
     * @param mcId
     * @param mgmtState
     * @param smppType
     * @param registeredDelivery
     * @param smscAddress
     * @param smscPort
     * @param bindType
     * @param systemId
     * @param password
     * @param systemType
     * @param rSourceAddrRegex
     * @param maxBinds
     * @param throttleLimit
     * @param bound
     */
    public SmppGwSession(int smppGwId,
                         int smppSessionId,
                         String smppSessionName,
                         int mcId,
                         MGMT_STATE mgmtState,
                         SMPP_TYPE smppType,
                         boolean registeredDelivery,
                         String smscAddress,
                         int smscPort,
                         BIND_TYPE bindType,
                         String systemId,
                         String password,
                         String systemType,
                         String rSourceAddrRegex,
                         int maxBinds,
                         Integer throttleLimit,
                         int bound) {
        this.smppGwId = smppGwId;
        this.smppSessionId = smppSessionId;
        this.smppSessionName = smppSessionName;
        this.mcId = mcId;
        this.mgmtState = mgmtState;
        this.smppType = smppType;
        this.registeredDelivery = registeredDelivery;
        this.smscAddress = smscAddress;
        this.smscPort = smscPort;
        this.bindType = bindType;
        this.systemId = StringUtils.isEmptyOrNull(systemId) ? "" : systemId;
        this.password = password;
        this.systemType = systemType;
        this.rSourceAddrRegex = rSourceAddrRegex;
        this.maxBinds = maxBinds;
        this.throttleLimit = throttleLimit == null ? 0 : throttleLimit;
        this.bound = bound;
    }

    /**
     * Returns the smppGwId.
     * @return
     */
    public int getSmppGwId() {
        return smppGwId;
    }

    /**
     * Returns the smppSessionId.
     * @return
     */
    public int getSmppSessionId() {
        return smppSessionId;
    }

    /**
     * Returns the smppSessionName.
     * @return
     */
    public String getSmppSessionName() {
        return smppSessionName;
    }

    /**
     * Returns the mcId.
     * @return
     */
    public int getMcId() {
        return mcId;
    }

    /**
     * Returns the mgmtState.
     * @return
     */
    public MGMT_STATE getMgmtState() {
        return mgmtState;
    }

    /**
     * Returns the smppType.
     * @return
     */
    public SMPP_TYPE getSmppType() {
        return smppType;
    }

    /**
     * Returns the registeredDelivery.
     * @return
     */
    public boolean isRegisteredDelivery() {
        return registeredDelivery;
    }

    /**
     * Returns the smscAddress.
     * @return
     */
    public String getSmscAddress() {
        return smscAddress;
    }

    /**
     * Returns the smscPort.
     * @return
     */
    public int getSmscPort() {
        return smscPort;
    }

    /**
     * Returns the bindType.
     * @return
     */
    public BIND_TYPE getBindType() {
        return bindType;
    }

    /**
     * Returns the systemId.
     * @return
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * Returns the password.
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the systemType.
     * @return
     */
    public String getSystemType() {
        return systemType;
    }

    /**
     * Returns the rSourceAddrRegex.
     * @return
     */
    public String getRSourceAddrRegex() {
        return rSourceAddrRegex;
    }

    /**
     * Returns the maxBinds.
     * @return
     */
    public int getMaxBinds() {
        return maxBinds;
    }

    /**
     * Returns the throttleLimit.
     * @return
     */
    public Integer getThrottleLimit() {
        return throttleLimit;
    }

    /**
     * Sets the bound.
     * @param bound
     */
    public void setBound(int bound) {
        this.bound = bound;
        SmppGwApp.getSmppGwApp().setBound(smppGwId, smppSessionId, bound);
    }

    /**
     * Returns the bound.
     * @return
     */
    public int getBound() {
        return bound;
    }

    /**
     * The MGMT_STATE enum.
     */
    public enum MGMT_STATE {

        IDLE("idle"),
        ACTIVE("active"),
        LOCKED("locked");

        /** The name. */
        private final String name;

        MGMT_STATE(String name) {
            this.name = name;
        }

        /**
         * Returns the name.
         * @return
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the MGMT_STATE for the given name.
         * @param name
         * @return
         */
        public static MGMT_STATE getMgmtState(String name) {
            for (MGMT_STATE mgmtState : MGMT_STATE.values()) {
                if (mgmtState.getName().equals(name)) return mgmtState;
            }
            return null;
        }
    }

    /**
     * The SMPP_TYPE enum.
     */
    public enum SMPP_TYPE {

        SMSC("smsc"),
        ESME("esme");

        /** The name. */
        private final String name;

        SMPP_TYPE(String name) {
            this.name = name;
        }

        /**
         * Returns the name.
         * @return
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the SMPP_TYPE for the given name.
         * @param name
         * @return
         */
        public static SMPP_TYPE getSmppType(String name) {
            for (SMPP_TYPE smppType : SMPP_TYPE.values()) {
                if (smppType.getName().equals(name)) return smppType;
            }
            return null;
        }
    }

    /**
     * The BIND_TYPE enum.
     */
    public enum BIND_TYPE {

        T("t"),
        R("r"),
        TR("tr");

        /** The name. */
        private final String name;

        BIND_TYPE(String name) {
            this.name = name;
        }

        /**
         * Returns the name.
         * @return
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the BIND_TYPE for the given name.
         * @param name
         * @return
         */
        public static BIND_TYPE getBindType(String name) {
            for (BIND_TYPE bindType : BIND_TYPE.values()) {
                if (bindType.getName().equals(name)) return bindType;
            }
            return null;
        }
    }
}
