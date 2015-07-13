import com.nuevatel.smpp.SMPPApplication;
import com.nuevatel.smpp.SMPPClientApplication;
import com.nuevatel.smpp.SMPPServerApplication;
import com.nuevatel.smpp.utils.PropertiesLoader;
import java.util.Properties;
import java.util.logging.Logger;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */



/**
 *
 * @author Luis Baldiviezo
 */
public class SMPP {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        SMPPApplication smppApplication;
        Properties properties = PropertiesLoader.getProperties(args[0]);
        System.out.println("*** " + args[0] + " ***");
        if (properties.getProperty("appType").equals("server")) smppApplication = SMPPServerApplication.getSMPPServerApplication();
        else if (properties.getProperty("appType").equals("client")) smppApplication = SMPPClientApplication.getSMPPClientApplication();
        else return;
        
        if (smppApplication.loadProperties(properties)){
            smppApplication.setPropertiesFile(args[0]);
            smppApplication.start();
        }
        else{
            smppApplication.shutdown();
        }
    }
}
