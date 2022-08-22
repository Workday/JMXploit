
/**
 * This is a basic test mbean to validate that 
 * payload invocation and return is working as expected
 */
public class Test implements TestMBean
{
	public String firstMethod() {
		return "Called first method listed successfully";
	}
	public String oneParam(String param1) {
		return "You called a oneParam method with "+param1;
	}
	public String twoParam(String param1, String param2) { return "You called a twoParam method with "+param1+" and "+param2; }
	public String alphaFirst() {
		return "Should not be called";
	}
}
