
import com.nuevatel.mc.common.BaseApp;
import com.nuevatel.mc.common.GenericApp;
import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwApp;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
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
    public static final String MGMT_APP_PROPERTIES_LIST = "mgmtAppPropertiesList";
    /* constants for mgmt properties */
    public static final String WS_URL = "wsURL";

    /**
     * to shutdown kill -15 pid
     * 
     * The main method.
     * @param args String[]
     * 
     * String propVal = System.getProperty("log4j.configurationFile");
     *  System.out.println("Log4j2: " + propVal);
     *  logger.info("Log4j2: {}", propVal);
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

                // mgmtAppList
                List<BaseApp> mgmtAppList = new ArrayList<>();
                // mgmtAppPropertiesList
                String mgmtAppPropertiesList = properties.getProperty(MGMT_APP_PROPERTIES_LIST);
                String[] filenameArray = mgmtAppPropertiesList.split("\\s");
                for (String filename : filenameArray) {
                    Properties mgmtAppProperties = new Properties();
                    try (Reader mgmtAppPropertiesReader = new FileReader(filename)) {
                        mgmtAppProperties.load(mgmtAppPropertiesReader);
                    } catch (IOException | IllegalArgumentException e) {
                        throw e;
                    }
                    // mgmtAppId
                    int mgmtAppId;
                    try {
                        mgmtAppId = Integer.parseInt(mgmtAppProperties.getProperty(APP_ID));
                    } catch (NumberFormatException nfe) {
                        throw new RuntimeException("illegal " + APP_ID + " " + mgmtAppProperties.getProperty(APP_ID), nfe);
                    }
                    // wsURL
                    String wsURL = mgmtAppProperties.getProperty(WS_URL);
                    if (wsURL == null) {
                        throw new RuntimeException("illegal " + WS_URL + " " + mgmtAppProperties.getProperty(WS_URL));
                    }
                    // state
                    BaseApp.STATE state = GenericApp.callGetState(wsURL);
                    mgmtAppList.add(new BaseApp(mgmtAppId, BaseApp.APP_TYPE.MGMT, state, wsURL));
                }
                if (mgmtAppList.isEmpty()) {
                    throw new RuntimeException("illegal " + MGMT_APP_PROPERTIES_LIST);
                }

                // smppGwApp
                SmppGwApp smppGwApp = SmppGwApp.getSmppGwApp();
                smppGwApp.setAppId(appId);
                smppGwApp.setMgmtAppList(mgmtAppList);

                // start
                smppGwApp.start();
                // Add shutdown hook
                Runtime.getRuntime().addShutdownHook(new Thread(()->smppGwApp.interrupt()));
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
