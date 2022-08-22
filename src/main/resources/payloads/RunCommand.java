
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RunCommand implements RunCommandMBean{

	public String runCommand()
	{
		return "You should call with -method runCommand -params commandToRun";
	}

	
	public String runCommand(String command) 
	{
		String toReturn="";
		
		try
		{
			Runtime rt = Runtime.getRuntime();
			String[] commands = command.split(" ");
			Process proc = rt.exec(commands);

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

			BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

			// Read the output from the command
			String s = null;
		
			while ((s = stdInput.readLine()) != null) 
			{
				toReturn+=s + "\n";
			}

			// Read any errors from the attempted command
			while ((s = stdError.readLine()) != null) 
			{
				toReturn+=s + "\n";
			}
		}
		catch (Exception e)
		{
			toReturn+=e.toString();
		}
		return "\n"+toReturn;
	}

}
