package jmx_metric_group;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.joda.time.DateTime;
import org.springframework.jdbc.datasource.DriverManagerDataSource;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.mysql.jdbc.Connection;


public class jmxMetrics {

	private static String hostName = "";
	private static int port = 9999;
	private static String objectGridName = "library";
	private static String mapSetName = "ms1";
	
	
	public static String connectionString;

	public static String streamURL;
	public static String outputqueue;
	public static String logFileDirectory;
	public static String logErrorFileDirectory;
	public static String activeLogDirectory;
	public static String adminEmail;
	public static String toEmail;
	public static String smtp;
	public static String smtpPort;
	public static String smtp_username;
	public static String smtp_password;
	

	public static Connection conn;
	
	public static int postCount=0;
	public static int msgCount=0;
	
	public static File rfile;
	public static File efile;
	public static FileWriter rfop;
	public static FileWriter efop;
	public static DriverManagerDataSource dataSource;
	 
	public static long lagTotal; 
 	public static long partitionSum;
	public static String targetConsumerGroup;
	
	public static double thresholdLimit;
	

	public static int looper=0;
	public static int counter = 0;
	public static int partitionCounter = 0;
	/* kafka related variables*/
	public static Properties props = null;
	
	public static String kafkaServer;
	public static String kafkaBrokers;
	public static String kafkaJMX;
	public static String kafkaJMXMetricPaths;
	public static String kafkaBeanName;
	public static String kafkaBeanNames;
	public static String kafkaServerIPS;
		
	public static String kafkaClientServer;
	public static String kafkaClientPort;
	public static String kafkaJMXPath;
	
	
	public static String kafkabroker1;
	public static String kafkabroker2;
	public static String kafkabroker3;
	
	public static String kafkaServers1;
	public static String kafkaServers2;
	public static String kafkaServers3;
	
	public static String influxDBConnectionString;
	public static String influxDataBase;
	public static String influxDBRetention;
	
	
	public static String DGroupID;
	public static String consumerGroup;
	public static String uname;
	public static String pword;
	public static String topic;
	public static String lastTopic="";
	public static String lastConsumerGroup;
	public static String password;
	public static String username;
	
	
	public static KafkaProducer<String,String> producer = null;
 	
	//unix remote session variables
    public static FileWriter fwriter;
    
	public static Map<String, String> metricsTopics = new HashMap<>();

	public static boolean partitionFlag = false;
	public static boolean pTotalFlag = false;
	public static boolean timeoutFlag = false;
   	public static File file;

	
	public jmxMetrics(){

	}
	
	public static void main(String[] args) throws IOException, MessagingException {
	    ArrayList<String> brokerList = new ArrayList<String>();
		    getConfigurations();
		initializeAppLogs();
		rfop.write("*STARTING JMX CONSUMER*"  + "  "  + DateTime.now().toString());
		efop.write("*STARTING JMX CONSUMER*"  + "  "  + DateTime.now().toString());
			if(kafkaServers1 != null) {
			brokerList.add(kafkaServers1);
		} 
		if(kafkaServers2 != null) {
			brokerList.add(kafkaServers2);
		} 
		if(kafkaServers3 != null) {
			brokerList.add(kafkaServers3);
		}
		ArrayList<String> serverList = new ArrayList<String>();
		for(int x=0;x<brokerList.size();x++){
			String[] serverBrokers = brokerList.get(x).split(",");
			for(int y=0;y<serverBrokers.length;y++) {
				serverList.add(serverBrokers[y]);
			}
		}
   		
    	ArrayList<String> filterList = new ArrayList<String>();
		rfop.write("*STARTING JMX CONSUMER*"  + "  "  + DateTime.now().toString());
		efop.write("*STARTING JMX CONSUMER*"  + "  "  + DateTime.now().toString());
		rfop.flush();
   		efop.flush();
    		
		file = new File(activeLogDirectory);
		file.createNewFile();
		fwriter = new FileWriter(file);
		
			
		String[] labels;
		String[] metrics =  kafkaJMX.split(","); 
		for(int c=0; c < metrics.length; c++){
			String metricProperty = metrics[c];
			filterList.add(metrics[c]);
		}
	
		
	    String[] kafkaBeansList = null;
	      
	  
	     if(kafkaBeanNames != null && !kafkaBeanNames.isEmpty()){
	    	 kafkaBeansList = kafkaBeanNames.split(";");
	     } else{
	    	 
	     }
		
		
		
		Executor executor = Executors.newFixedThreadPool(serverList.size());
		
				
   		for (final String server : serverList) {
   			
   		    //for (final String kafkaBean : kafkaBeansList) {

   			
    			executor.execute(new Runnable() {
    				@Override
    				public void run() {
    					while (!Thread.currentThread().isInterrupted()) {
    						try {
    							
    							final Session session;
    					    	final Channel channel;
    					    	final JSch jsch;		
    					    	final String command;
    					    	final String assignedServer;
    					    	
    					    	
    				    		InetAddress addr = InetAddress.getByName(server);
    				    		assignedServer = addr.getHostName();
    							Properties config = new Properties();
    						
    						
    							for (String filterItem : filterList){
    								if(!timeoutFlag){
    										
    									
    									
    									Instant instant = Instant.now();
    				
    									fwriter.write("--Starting collection process on server: " + server + " ---" +  instant);
    									fwriter.append("\n");
    									fwriter.write("working with thread - "  + Thread.currentThread().getId());
    									fwriter.append("\n");
    									fwriter.flush();
    									String kafkaMetric = retrieveJMXMetrics(filterItem,server,assignedServer,kafkaBeanName);
    									
    								} else {
    										timeoutFlag = false;
    										break;
    								}
    							}
    						
    							//errorReporting("Ending server JMX Full Kafka monitoring:");
    				    		rfop.write("Ending server JMX Full Kafka monitoring:");
    				    		rfop.flush();
    							Thread.sleep(30000);	
    				
		    						
		    				} catch (InterruptedException | IOException e) {
		    								// TODO Auto-generated catch block
		    								e.printStackTrace();
		    								StringWriter sw = new StringWriter();
		    								e.printStackTrace(new PrintWriter(sw));
		    								
		    								try {
		    									efop.write(sw.toString());
												efop.flush();
											} catch (IOException e1) {
												// TODO Auto-generated catch block
												e1.printStackTrace();
											}
		    				}
    						
    			
    			
    				
    					}
    				}
    				private void printError(Exception e) {
    					try {
    						e.printStackTrace();
    						StringWriter sw = new StringWriter();
    						e.printStackTrace(new PrintWriter(sw));
    						efop.write(sw.toString());
    						efop.flush();
    					} catch (IOException fileException) {
    						fileException.printStackTrace();
    					}
    				}
    			});
    		//}
   		}
	}

	
	public static void initializeAppLogs() throws MessagingException{
		  
		  String logPath = logFileDirectory;
		  String logErrPath = logErrorFileDirectory;
		  
		  SimpleDateFormat ft = new SimpleDateFormat("yyyyMMddhhmmssa");
		  Date dNow = new Date();
		  
		  rfile = new File(logPath + "jmx_controller_" + ft.format(dNow) + ".txt");
		  efile = new File(logErrPath + "jmx_controller_ERR_" + ft.format(dNow) + ".txt");
		  
		  try {
			  
			rfop = new FileWriter(rfile);
			efop = new FileWriter(efile);
			
			rfile.createNewFile();
			efile.createNewFile();
	       
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//errorReporting(e.getMessage().toString());
		}
		  
	 }
			
	private static void getConfigurations() {
				
				 Properties fileProp = new Properties();
				 try {
						
						
						
				    fileProp.load(jmxMetrics.class.getClassLoader().getResourceAsStream("config.properties"));
				    System.out.println("READING CONFIG PROPERTIES FILE...");
			   		
				    				   
					logFileDirectory = fileProp.getProperty("logFileDirectory");
		   		    logErrorFileDirectory = fileProp.getProperty("logErrorFileDirectory");
					activeLogDirectory = fileProp.getProperty("activeLogDirectory");

		   		    adminEmail = fileProp.getProperty("adminEmail");
					smtp = fileProp.getProperty("smtp");
					smtpPort = fileProp.getProperty("smtpPort");
					smtp_username = fileProp.getProperty("smtp_username");
					smtp_password = fileProp.getProperty("smtp_password");
		
					username = fileProp.getProperty("username");
					password = fileProp.getProperty("password");
					
					uname = fileProp.getProperty("uname");
					pword = fileProp.getProperty("pword");
					
					
					
					influxDBConnectionString=fileProp.getProperty("influxDBConnectionString");
					influxDataBase=fileProp.getProperty("influxDB");
					influxDBRetention=fileProp.getProperty("influxDBRetention");
					
					kafkaServer = fileProp.getProperty("kafkaServer");
					kafkaBrokers = fileProp.getProperty("kafkaBrokers");
					kafkaServerIPS = fileProp.getProperty("kafkaServerIPS");
					targetConsumerGroup = fileProp.getProperty("targetConsumerGroup");
					toEmail = fileProp.getProperty("toEmail");
					
					kafkaClientServer=fileProp.getProperty("kafkaClientServer");
					kafkaClientPort=fileProp.getProperty("kafkaClientPort");
					kafkaJMXPath=fileProp.getProperty("kafkaJMXPath");
					
					kafkaServers1 = fileProp.getProperty("kafkaServers1");
					kafkaServers2 = fileProp.getProperty("kafkaServers2");
					kafkaServers3 = fileProp.getProperty("kafkaServers3");
					kafkaBrokers = fileProp.getProperty("kafkaBrokers");
					
					kafkaJMX = fileProp.getProperty("kafkaJMXMetrics");
					kafkaJMXMetricPaths = fileProp.getProperty("kafkaJMXMetricPaths");
					kafkabroker1 = fileProp.getProperty("kafkaBroker1");
					kafkabroker2 = fileProp.getProperty("kafkaBroker2");
					kafkabroker3 = fileProp.getProperty("kafkaBroker3");
					kafkaBeanName = fileProp.getProperty("kafkaBeanName");
					kafkaBeanNames = fileProp.getProperty("kafkaBeanNames");
					
					String thresholdConfig = fileProp.getProperty("thresholdLimit");
					thresholdLimit = Double.parseDouble(thresholdConfig);
					
					System.out.println(kafkaJMX);
							
					
				} catch (IOException io) {
					io.printStackTrace();
				} 		
					
				}
		         
	private static void errorReporting(String errMessage) throws MessagingException{
				// email client handling error messages
				String to_email=toEmail;
				String from_email="StreamAlert@boardreader.com";
				String host_server="192.168.5.7";
				
				Properties properties = System.getProperties();
				properties.setProperty("mail.smtp.host",host_server);
				properties.setProperty("mail.smtp.user","rainmaker");
				properties.setProperty("mail.smtp.password", "97CupChamps");
				properties.setProperty("mail.smtp.port","25");
				
				javax.mail.Session session= javax.mail.Session.getDefaultInstance(properties);
				
				String preFix = "The JMX Metrics value has exceeded reasonable limit. ";
				
				
				MimeMessage message = new MimeMessage(session);
				message.setFrom(new InternetAddress(from_email));
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(to_email));
				message.addRecipient(Message.RecipientType.CC, new InternetAddress("zfareed@boardreader.com"));
				message.setSubject("Chronograf alert");
				message.setContent(preFix + errMessage,"text/html");
				Transport.send(message);
				System.out.println("MESSAGE SENT - WOO WOO!");
				
			}
			
	public static String retrieveJMXMetrics(String filterMask, String kServer, String kDomain, String kBean) throws IOException{
			
				JMXConnector jmxCon = null;
				Object consumerBeanII = null;
				//http://192.168.7.157:8086
				InfluxDB influxDB = InfluxDBFactory.connect(influxDBConnectionString, uname, pword);
				influxDB.enableBatch(100, 200, TimeUnit.MILLISECONDS);
				
				influxDB.setRetentionPolicy(influxDBRetention);
				influxDB.setDatabase(influxDataBase);
				try{
					
					String serviceURL = "service:jmx:rmi:///jndi/rmi://" + kServer + ":" + port + "/jmxrmi";
					JMXServiceURL jmxURL = new JMXServiceURL(serviceURL);
					jmxCon = JMXConnectorFactory.connect(jmxURL);
					MBeanServerConnection catalogServerConnection = jmxCon.getMBeanServerConnection();
								
					Object memoryMbean = null;
					Object osMbean = null;
					Object consumerBean = null;
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode rootNode = mapper.createObjectNode();
					
					
					long cpuBefore = 0;
					long tempMemory = 0;
					CompositeData cd = null;
					ObjectName filterName = new ObjectName(filterMask);
					Set<ObjectName> beanSet = catalogServerConnection.queryNames(filterName, null);
		           // Set<ObjectName> beanSet = catalogServerConnection.queryNames(ObjectName.WILDCARD, null);
				   
					 /*for (Map.Entry<String, String> entry : metricsTopics.entrySet()) {
					   System.out.println(entry.getKey() + " = " + entry.getValue());
		
					   String beanThing = entry.getValue();
					   consumerBeanII = jmxCon.getMBeanServerConnection().getAttribute(new ObjectName(beanThing),"Value");
			 	       System.out.println(" Message Mean Rate: " + consumerBeanII); 
			            
			 	       
			 	       jsonMessage jMsg = new jsonMessage();
			 	       jMsg.setHost(kafkaServer);
			 	       jMsg.setBeanName("kafka.server:type=ReplicaManager,name=UnderReplicatedPartitions");
				       jMsg.setAttribute("Value");
				       String attValue = consumerBeanII.toString();
				       if(attValue.contains("SECONDS")){
				        	   continue;
			  	       }
				       if(StringUtils.isNumeric(attValue)){
				        	jMsg.setAttributeValue(attValue);
			
				       }else{
				       	   continue;
				       }
				                          	           
				       SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");               	           
					   Date now = new Date();
				       String strDate = sdfDate.format(now);
				       jMsg.setReceivedDate(strDate);
				       String msgCheck = jMsg.toString();
		                         
					}*/
				    ArrayNode arrayNode = rootNode.putArray("beans");
		            
		            for (ObjectName beanName : beanSet) {
		                            
		                   ObjectNode itemNode = mapper.createObjectNode();
		                   MBeanInfo info = catalogServerConnection.getMBeanInfo(beanName);
		                   MBeanAttributeInfo[] attrInfo = info.getAttributes();
		                   itemNode.put("beanName", beanName.getCanonicalName());
		
		                   ObjectNode attrNode = itemNode.putObject("attributes");
		
		                   String beanThings = beanName.getCanonicalName();
		        	       String beanItems = beanName.toString();
		        	       System.out.println(beanThings);
		        	       System.out.println(beanItems);
		        	       String beanTest = "";
	        	           if(!kBean.isEmpty()) {
	        	        	   if(beanThings.contains(kBean) || beanItems.contains(kBean)){
			        	    	   System.out.println("Found it");
			        	       } else {
			        	    	 //  continue;
			        	       }
			        	       
			        	      
			        	        
	        	           }
		        	       
		        	       
	                   //   System.out.println("Attributes for object: " + beanName +":\n");
		                   for (MBeanAttributeInfo attr : attrInfo)
		                   {
		                	   
		                	   
		                	   try{
		                		   
		                	   
		                	       System.out.println(beanName + "  " + attr.getName() + " - " + catalogServerConnection.getAttribute(beanName, attr.getName()));
		                	       String beanThing = beanName.getCanonicalName();
		                	       String beanItem = beanName.toString();
		                	                      	       
		                	       jsonMessage jMsg = new jsonMessage();
		                	       jMsg.setHost(kServer);
		                	       jMsg.setBeanName(beanName.getCanonicalName());
		               	           jMsg.setAttribute(attr.getName());
		               	           String attValue = String.valueOf(catalogServerConnection.getAttribute(beanName, attr.getName()));
		               	           if(attValue.contains("SECONDS")){
		               	        	   continue;
		                 	       }
		               	           if(beanItem.contains("MessagesInPerSec")){
			               	           if(StringUtils.isNumeric(attValue) || attr.getName().toString().equalsIgnoreCase(kafkaJMX)|| attr.getName().toString().equalsIgnoreCase("MeanRate") || attr.getName().toString().equalsIgnoreCase("OneMinuteRate") || attr.getName().toString().equalsIgnoreCase("FiveMinuteRate") || attr.getName().toString().equalsIgnoreCase("FifteenMinuteRate")){
			               	        	jMsg.setAttributeValue(attValue);
			
			               	           }else{
			               	        	   continue;
			               	           }
			               	           SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");               	           
			               	           Date now = new Date();
			               	           String strDate = sdfDate.format(now);
			               	           jMsg.setReceivedDate(strDate);
			               	           String msgCheck = jMsg.toString();
			                	       System.out.println(msgCheck);
			                	       double attrValue = Double.valueOf(attValue);
			                	       String putSuccess = putInfluxDB(beanName.getCanonicalName(),attr.getName(),attrValue,strDate,influxDB,kDomain );
		               	           }  else if(beanItem.contains(kafkaBeanName)){
		               	        	   System.out.println(beanItem);
		               	        	   SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");               	           
			               	           Date now = new Date();
			               	           String strDate = sdfDate.format(now);
			               	           jMsg.setReceivedDate(strDate);
			               	           String msgCheck = jMsg.toString();
			                	       System.out.println(msgCheck);
			                	       if(StringUtils.isNumeric(attValue)){
			                	    	   double attrValue = Double.valueOf(attValue);
				                	       String putSuccess = putInfluxDB(beanName.getCanonicalName(),attr.getName(),attrValue,strDate,influxDB,kDomain );

			                	       } else{
				                	       String putSuccess = putInfluxDB2(beanName.getCanonicalName(),attr.getName(),attValue,strDate,influxDB,kDomain );

			                	       }
		               	           }
		               	           	  else {
		               	               System.out.println(beanItem);
		               	        	   SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");               	           
			               	           Date now = new Date();
			               	           String strDate = sdfDate.format(now);
			               	           jMsg.setReceivedDate(strDate);
			               	           String msgCheck = jMsg.toString();
			                	       System.out.println(msgCheck);
			                	       if(StringUtils.isNumeric(attValue)){
			                	    	   double attrValue = Double.valueOf(attValue);
				                	       String putSuccess = putInfluxDB(beanName.getCanonicalName(),attr.getName(),attrValue,strDate,influxDB,kDomain );

			                	       } else{
				                	       String putSuccess = putInfluxDB2(beanName.getCanonicalName(),attr.getName(),attValue,strDate,influxDB,kDomain );

			                	       }		               	           }
		               	           
		                	      // producer.send(new ProducerRecord<String, String>("kafka.jmx.metrics","",jMsg.toString()));
		               			
		                	       
		                	   } catch (Exception e) {
			               			if(jmxCon != null) {
			               				jmxCon.close();
			               			}
			               			e.printStackTrace();
			               			StringWriter sw = new StringWriter();
			               			e.printStackTrace(new PrintWriter(sw));
			               			System.out.println(sw.toString());
			               			timeoutFlag = true;
			               			efop.write(sw.toString());
			    					efop.flush();
		               			
		               		}
		               	           
		                   	}                                           
		      
		                   influxDB.close();
		                  
		            }
					
					
		            /*String attrNodeName = attr.getName();
		 	       String attrNodeValue = "Unavailable";                   
		            System.out.print("  " + attr.getName());
		 	       try {
		 	    	   attrNodeValue = catalogServerConnection.getAttribute(beanName, attrNodeName).toString();
		                Object s  = mbeanConn.getAttribute(beanName, attrNodeName);
		 	       }
		        			catch (Exception e) { 
		        	    }
		 	       attrNode.put(attrNodeName, attrNodeValue);
		           System.out.println("  " + attrNodeName + " - " + attrNodeValue);
			       // System.out.println(jMsg.toString()); 
					
					// call the garbage collector before thex test using the Memory Mbean
					/*jmxCon.getMBeanServerConnection().invoke(new ObjectName("java.lang:type=Memory"), "gc", null, null);
					
					for (int i = 0; i < 100; i++) {
						 
							
						//get an instance of the kafka metrics Mbean
						consumerBeanII = jmxCon.getMBeanServerConnection().getAttribute(new ObjectName("kafka.log:type=Log,name=LogEndOffset,topic=disqus.fullfeed.in,partition=1"),"Value");
						System.out.println(" Log-End-Offset: " + consumerBeanII); //print memory usage
						cd = (CompositeData) consumerBeanII;
						Thread.sleep(1000); //delay for one second
				    
					}
		
					
					System.out.println(catalogServerConnection);*/
		            
					
				} catch (Exception e) {
					if(jmxCon != null) {
						jmxCon.close();
					}
					e.printStackTrace();
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					System.out.println(sw.toString());
					timeoutFlag = true;
					efop.write(sw.toString());
					efop.flush();
				}
		
				 if(jmxCon != null) {
						jmxCon.close();
					}
				
			return "";
		
		}
		
	public static String putInfluxDB(String BeanName, String Attribute, Double Att_Value, String Received_date,InfluxDB influxDB, String kServer) throws IOException {
		
		try {
			
		
		
			String receivedDate = String.valueOf(System.currentTimeMillis());
	
			Point point = Point.measurement("ops_kafkajmx_metrics")
					  .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
					  .tag("beanName", BeanName.toString())
					  .tag("attribute", Attribute)
					  //.tag("attrValue",String.valueOf(Att_Value))
					  .tag("host",kServer)
					  //.addField("host", kafkaServer)
					  //.addField("beanName", BeanName.toString())
					  //.addField("attribute", Attribute)
					  .addField("attr_value", Att_Value)
					  .addField("received_date", receivedDate)
					  .build();
			
			Thread.sleep(2000);	
			
			if(Attribute.equalsIgnoreCase(kafkaJMXPath) && Att_Value > thresholdLimit){
				String message = "Consumer Group: {" + consumerGroup + "} on Topic: " + topic + "} lag{" + Att_Value + "}";
				//errorReporting(message); 
				System.out.println("Sending email...");
			}
			
			influxDB.write(point);
		
		} catch (Exception e) {
			influxDB.close(); 
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			System.out.println(sw.toString());
			efop.write(sw.toString());
			efop.flush();
			
		}
		
		return "success";
		
		
	}
	
	
public static String putInfluxDB2(String BeanName, String Attribute, String Att_Value, String Received_date,InfluxDB influxDB, String kServer) throws IOException {
		
		try {
			
		
		
			String receivedDate = String.valueOf(System.currentTimeMillis());
	        
			
			
			Point point = Point.measurement("ops_kafkajmx_metrics")
					  .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
					  .tag("beanName", BeanName.toString())
					  .tag("attribute", Attribute)
					  //.tag("attrValue",String.valueOf(Att_Value))
					  .tag("host",kServer)
					  //.addField("host", kafkaServer)
					  //.addField("beanName", BeanName.toString())
					  //.addField("attribute", Attribute)
					  .addField("gen_attr_value", Att_Value)
					  .addField("received_date", receivedDate)
					  .build();
			
			Thread.sleep(2000);	
			
			if(Attribute.equalsIgnoreCase(kafkaJMXPath)){
				String message = "Consumer Group: {" + consumerGroup + "} on Topic: " + topic + "} lag{" + Att_Value + "}";
				//errorReporting(message); 
				System.out.println("Sending email...");
			}
			
			influxDB.write(point);
		
		} catch (Exception e) {
			influxDB.close(); 
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			System.out.println(sw.toString());
			efop.write(sw.toString());
			efop.flush();
			
		}
		
		return "success";
		
		
	
	
	
	}
}
