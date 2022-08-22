package jmxManagers;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.ConnectException;

public class JMXConnectionManager {
    private static String targetHost = "127.0.0.1";
    private static String targetPort = "50199";
    private static JMXConnector jmxc = null;
    private static boolean connectionOpen = false;

    public static boolean getJMXConnection() {
        try {
            //create a JMX connection string and establish the connection
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + targetHost + ":" + targetPort + "/jmxrmi");
            jmxc = JMXConnectorFactory.connect(url, null);
            return true;
        } catch (MalformedURLException malformedURLException) {
            System.out.println("Could not create a valid target specification from the provided host/port: " + targetHost + ":" + targetPort);
            return false;
        } catch (java.lang.SecurityException securityException) {
            System.out.println("JMX, but auth required");
            return false;
        } catch (ConnectException connectException) {
            System.out.print("Could not connect to RMI target at: " + targetHost + ":" + targetPort);
            return false;
        } catch (Exception e) {
            System.out.println("The RMI service is unavailable at the target specified: " + targetHost + ":" + targetPort);
            return false;
        }

    }

    public static MBeanServerConnection getJMXServerConnection() {
        connectionOpen = getJMXConnection();
        try {
            if (connectionOpen)
                return jmxc.getMBeanServerConnection();
            else
                System.exit(1);
        } catch (Exception e) {
            System.out.println("Could not return MBean server connection " + e.toString());
            System.exit(1);
        }
        return null;
    }

    public static void closeJMXConnection() {
        try {
            if (connectionOpen)
                jmxc.close();
        } catch (Exception e) {
            System.out.println("Critical error " + e.toString());
            System.exit(1);
        }

    }


    public static void setTargetHost(String newHost) {
        try {
            InetAddress address = InetAddress.getByName(newHost);
            targetHost = address.getHostAddress();
        } catch (UnknownHostException unknownHostException) {
            System.out.println("Could not resolve the hostname for the target host " + unknownHostException.toString());
            System.exit(0);
        }
    }

    public static void setTargetPort(String newPort) {
        targetPort = newPort;
    }

}
