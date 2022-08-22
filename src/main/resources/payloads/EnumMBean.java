
public interface EnumMBean
{
	String info();
	String invokeMethod(String clazz, String method, String parameter);
	String invokeMethod(String clazz, String method);
	String listJVMClasses();
	String listJVMMethods(String clazz);
	String listBasicProperties();
	String enumBasic();
	String enumJVM();
	String enumAll();
}
