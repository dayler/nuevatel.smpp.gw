/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.utils;

import java.io.FileInputStream;
import java.util.Properties;

/**
 *
 * @author Marcelo Baldiviezo <marcelo.baldiviezo@nuevatel.com>
 */
public class PropertiesLoader {
        public static Properties getProperties(String propertiesPath) throws Exception{
            Properties properties = new Properties();
            FileInputStream fileInputStream = new FileInputStream(propertiesPath);
            properties.load(fileInputStream);
            return properties;
        }

}
