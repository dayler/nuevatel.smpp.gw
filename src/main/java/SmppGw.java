
import com.nuevatel.mc.common.GenericApp;
import com.nuevatel.mc.common.MgmtApp;
import com.nuevatel.mc.common.RedundantApp;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwApp;

import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * <p>The SmppGw class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2015</p>
 *
 * @author Ariel Salazar, Jorge Vasquez, Eduardo Marin
 * @version 1.0
 * @since 1.8
 */
public class SmppGw {
    
    private static Logger logger = LogManager.getLogger("com.nuevatel.mc");

    /* constants for properties */
    public static final String APP_ID = "appId";
    /* constants for mgmt properties */
    public static final String MGMT_ID = "mgmtId";
    public static final String MGMT_WS_URL = "mgmtWsURL";
    public static final String MGMT_UNSERNAME = "mgmtUsername";
    public static final String MGMT_PASSWORD = "mgmtPassword";

    /**
     * to shutdown <code>kill -15 pid</code>
     * 
     * The main method.
     * @param args String[]
     * 
     */
    public static void main(String[] args) {
        // Print path to log4j conf
        System.out.println("Log4j2:" + System.getProperty("log4j.configurationFile"));
        logger.info("Log4j2:" + System.getProperty("log4j.configurationFile"));
        
        if (args.length == 1) {
            try (Reader reader = new FileReader(args[0])) {
                // properties
                Properties properties = new Properties();
                properties.load(reader);
                // register properties
                AllocatorService.setProperties(properties);
                // Load config properties
                AllocatorService.loadConfig(properties);
                // Initialize DialogService
                AllocatorService.initializeDialogService();
                logger.info("DialogService - start");
                // appId
                int appId;
                try {
                    appId = Integer.parseInt(properties.getProperty(APP_ID));
                } catch (NumberFormatException nfe) {
                    throw new RuntimeException("illegal " + APP_ID + " " + properties.getProperty(APP_ID), nfe);
                }
                // mgmtAppId
                int mgmtId;
                try {
                    mgmtId = Integer.parseInt(properties.getProperty(MGMT_ID));
                } catch (NumberFormatException nfe) {
                    throw new RuntimeException("illegal " + APP_ID + " " + properties.getProperty(APP_ID), nfe);
                }
                // wsURL
                String mgmtWsURL = properties.getProperty(MGMT_WS_URL);
                if (mgmtWsURL == null) {
                    throw new RuntimeException("illegal " + MGMT_WS_URL + " " + properties.getProperty(MGMT_WS_URL));
                }
                // mgmtApp
                RedundantApp<MgmtApp>mgmtApp = GenericApp.callGetMgmt(mgmtWsURL,
                                                                      mgmtId,
                                                                      properties.getProperty(MGMT_UNSERNAME), // user
                                                                      properties.getProperty(MGMT_PASSWORD)); // password
                if (mgmtApp == null) {
                    throw new RuntimeException("illegal " + MGMT_ID + " " + properties.getProperty(MGMT_ID));
                }
                SmppGwApp smppGwApp = SmppGwApp.getSmppGwApp();
                smppGwApp.setAppId(appId);
                smppGwApp.setMgmt(mgmtApp);

                // start
                smppGwApp.start();
                // Add shutdown hook
                Runtime.getRuntime().addShutdownHook(new Thread(() -> smppGwApp.interrupt()));
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
                System.exit(-1);
            }
        }
        else{
            System.err.println("usage: java SmppGw <properties>");
        }
    }
}
