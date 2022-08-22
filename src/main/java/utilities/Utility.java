package utilities;

import runnable.JMXploiter;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Utility {
    static String BLANK_CONFIG_FILE = "loaderMBeanName=java.util.logging\\:type\\=pineapple\n" +
            "payloadMBeanName=java.util.logging\\:type\\=pineappleTwo\n";

    public static void generateNewConfg() {
        try {
            try (PrintWriter out = new PrintWriter(JMXploiter.getPropertiesFileName())) {
                out.write(BLANK_CONFIG_FILE);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not generate a default config. Maybe due to write permissions? " + e.toString());
        }
    }

    /**
     * This may not work in complex environments as there is no way to know which IP address is correct. If the target
     * specifies the IP address to connect to, it's not a problem because the server listens at 0.0.0.0
     *
     * @return
     */
    public static String getHostIPAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException unknownHostException) {
            System.out.println("Could not get the IP address from the local host, you will need to specify with a flag " + unknownHostException.toString());
        }

        return "127.0.0.1";
    }

}
