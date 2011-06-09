package org.squin;

import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

public class Version {
	
    private static Properties properties = new Properties() ;
	
	static {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if ( cl == null ) {
            cl = ClassLoader.getSystemClassLoader();
            if ( cl != null ) {
                InputStream in = cl.getResourceAsStream("squin.properties");
                if ( in != null ) {
                    try { properties.load(in); } 
                    catch (InvalidPropertiesFormatException ex) {  }
                    catch (IOException ex) {  }
                }
            }
        }
	}

	public static String getVersion() {
		return properties.getProperty("org.squin.version", "unknown");
	}

}
