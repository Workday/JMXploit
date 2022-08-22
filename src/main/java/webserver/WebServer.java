package webserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jmxManagers.JMXMBeanManager;
import org.apache.commons.io.FileUtils;
import payloadManager.PayloadHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.List;


public class WebServer {
    public static String indexDirectory = "index.html"; //TODO get this in a config file
    public static int host = 0; //TODO add an interface specification

    static HttpServer server;

    public static String getIndexDirectory() {
        return indexDirectory;
    }

    public static int servePages(InetSocketAddress socket) {
        String loadDirectory = JMXMBeanManager.getProperty("lastPayloadName");
        int port = 0;
        try {
            System.out.println("Starting a server for payloads");
            server = HttpServer.create(socket, host);
            server.createContext("/" + indexDirectory, new WebRootHandlerA()); //Directory to serve the index file from
            server.createContext("/" + loadDirectory, new WebRootHandlerB()); //Directory to serve the payload from
            server.setExecutor(null);
            server.start();
            port = server.getAddress().getPort();
            System.out.println("Server started on all interfaces, port " + port);

        } catch (BindException addressInUse) {
            System.out.println("The selected address was already in use. No internal server started. Tried port " + port);
        } catch (IOException e) {
            System.out.println("Could not use the built in server, try using python or some other means. Tried port " + port + e.toString());
        }
        return port;
    }

    public static void stopServer() {
        try {
            server.stop(0);
            System.out.println("Server should be stopped");
        } catch (Exception e) {
            System.out.println("Server probably wasn't running");
        }
    }

    public static class WebRootHandlerA implements HttpHandler {

        //TODO change this to randomly serve a web page in the directory
        @Override
        public void handle(HttpExchange he) throws IOException {

            String[] extensions = new String[]{"html"};
            List<File> payloads = (List<File>) FileUtils.listFiles(new File(PayloadHandler.getOutputDirectory()), extensions, false);

            String response = FileUtils.readFileToString(payloads.get(0), "UTF-8");
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }


    public static class WebRootHandlerB implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            String[] extensions = new String[]{"jar"};
            List<File> payloads = (List<File>) FileUtils.listFiles(new File(PayloadHandler.getOutputDirectory()), extensions, false);

            byte[] temp = Files.readAllBytes(payloads.get(0).toPath());
            he.sendResponseHeaders(200, temp.length);
            OutputStream os = he.getResponseBody();
            os.write(temp);
            os.close();
        }
    }

}
