package jmx_metric_group;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class jmxMetrics {

	private static String hostName = "192.168.96.103";
	private static int port = 9999;
	private static String objectGridName = "library";
	private static String mapSetName = "ms1";
	
	
	public jmxMetrics(){

	}

	public String retrieveJMXMetrics() throws IOException{
	
	
	/*
	//test code for jolokia
	*/
   
		JMXConnector jmxCon = null;
		Object consumerBeanII = null;
		
		
	try{
		
		String serviceURL = "service:jmx:rmi:///jndi/rmi://" + hostName + ":" + port + "/jmxrmi";
		JMXServiceURL jmxURL = new JMXServiceURL(serviceURL);
		jmxCon = JMXConnectorFactory.connect(jmxURL);
		MBeanServerConnection catalogServerConnection = jmxCon.getMBeanServerConnection();
					
		Object memoryMbean = null;
		Object osMbean = null;
		Object consumerBean = null;
		
		long cpuBefore = 0;
		long tempMemory = 0;
		CompositeData cd = null;
		
		// call the garbage collector before the test using the Memory Mbean
		jmxCon.getMBeanServerConnection().invoke(new ObjectName("java.lang:type=Memory"), "gc", null, null);
		
		for (int i = 0; i < 100; i++) {
			 
				
			//get an instance of the kafka metrics Mbean
			consumerBeanII = jmxCon.getMBeanServerConnection().getAttribute(new ObjectName("kafka.log:type=Log,name=LogEndOffset,topic=disqus.fullfeed.in,partition=1"),"Value");
			System.out.println(" Log-End-Offset: " + consumerBeanII); //print memory usage
			cd = (CompositeData) consumerBeanII;
			Thread.sleep(1000); //delay for one second
	    
		}

		
		System.out.println(catalogServerConnection);
		
		
	} catch (Exception e) {
		if(jmxCon != null) {
			jmxCon.close();
	}

		
	}
	
		return String.valueOf(consumerBeanII);
	
	}
	
	}
