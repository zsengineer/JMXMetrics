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
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.mysql.jdbc.Connection;


public class jmxMetrics {

	
	private static int port = 9999;
	
	public static String connectionString= "";
	public static String streamURL= "";
	public static String outputqueue= "";
	public static String logFileDirectory= "";
	public static String logErrorFileDirectory= "";
	public static String activeLogDirectory= "";
	public static String adminEmail= "";
	public static String toEmail= "";
	public static String smtp= "";
	public static String smtpPort= "";
	public static String smtp_username= "";
	public static String smtp_password= "";
	public static String targetConsumerGroup="";
	
	public static Connection conn;

	public static File rfile;
	public static File efile;
	public static FileWriter rfop;
	public static FileWriter efop;
	public static DriverManagerDataSource dataSource;
	 
	public static long lagTotal; 
 	public static long partitionSum;

	public static double thresholdLimit;
	
	public static int postCount=0;
	public static int msgCount=0;
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
    
	protected static Map<String, String> metricsTopics = new HashMap<>();

	public static boolean partitionFlag = false;
	public static boolean pTotalFlag = false;
	public static boolean timeoutFlag = false;
   	public static File file;

    static Logger LOGGER = LoggerFactory.getLogger(jmxMetrics.class);


	public static void main(String[] args) throws IOException, MessagingException {
	  
		LOGGER.info("*STARTING JMX CONSUMER*"  + "  "  + DateTime.now().toString());

		ArrayList<String> brokerList = new ArrayList<>();
		getConfigurations();

		if(kafkaServers1 != null) {
			brokerList.add(kafkaServers1);
		} 
		if(kafkaServers2 != null) {
			brokerList.add(kafkaServers2);
		} 
		if(kafkaServers3 != null) {
			brokerList.add(kafkaServers3);
		}
		ArrayList<String> serverList = new ArrayList<>();
		
		for(int x=0;x<brokerList.size();x++){
			List<String> serverBrokers = Arrays.asList(brokerList.get(x).split(","));
		   serverList.addAll(serverBrokers);
		}
   		
    	ArrayList<String> filterList = new ArrayList<>();
	
		file = new File(activeLogDirectory);
		if(file.createNewFile()) {
			fwriter = new FileWriter(file);
		}

		List<String> metrics =  Arrays.asList(kafkaJMX.split(",")); 
		filterList.addAll(metrics);
		
	    String[] kafkaBeansList = null;

	     if(kafkaBeanNames != null && !kafkaBeanNames.isEmpty()){
	    	 kafkaBeansList = kafkaBeanNames.split(";");
	     } else{
	     }	
		
		
		Executor executor = Executors.newFixedThreadPool(serverList.size());

   		for (final String server : serverList) {
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
	    								LOGGER.info("BEGIN Thread Cycle...");

    	    							String kafkaMetric = retrieveJMXMetrics(filterItem,server,assignedServer,kafkaBeanName);
    									
    								} else {
    										timeoutFlag = false;
    										break;
    								}
    							}

    							Thread.sleep(30000);	
	
		    				} catch (InterruptedException | IOException e) {
		    								LOGGER.error("ERROR: " + e);
		    				}
    					}
    				}
    				private void printError(Exception e) {
    					LOGGER.error("ERROR: " + e);
    				}
    			});
   		}
	}

	
				
	private static void getConfigurations() {
				
				 Properties fileProp = new Properties();
				 try {
						
						
						
				    fileProp.load(jmxMetrics.class.getClassLoader().getResourceAsStream("config.properties"));
				    LOGGER.info("READING CONFIG PROPERTIES FILE...");

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
					
				} catch (IOException io) {
					LOGGER.error("ERROR: " + io);
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
							
			}
			
	public static String retrieveJMXMetrics(String filterMask, String kServer, String kDomain, String kBean) throws IOException{
			
				JMXConnector jmxCon = null;
				Object consumerBeanII = null;
				InfluxDB influxDB = InfluxDBFactory.connect(influxDBConnectionString, uname, pword);
				influxDB.enableBatch(100, 200, TimeUnit.MILLISECONDS);
				
				influxDB.setRetentionPolicy(influxDBRetention);
				influxDB.setDatabase(influxDataBase);
				try{
					
					String serviceURL = "service:jmx:rmi:///jndi/rmi://" + kServer + ":" + port + "/jmxrmi";
					JMXServiceURL jmxURL = new JMXServiceURL(serviceURL);
					jmxCon = JMXConnectorFactory.connect(jmxURL);
					MBeanServerConnection catalogServerConnection = jmxCon.getMBeanServerConnection();
					ObjectMapper mapper = new ObjectMapper();
					ObjectNode rootNode = mapper.createObjectNode();

					CompositeData cd = null;
					ObjectName filterName = new ObjectName(filterMask);
					Set<ObjectName> beanSet = catalogServerConnection.queryNames(filterName, null);
		           
				    ArrayNode arrayNode = rootNode.putArray("beans");
		            
		            for (ObjectName beanName : beanSet) {
		                            
		                   ObjectNode itemNode = mapper.createObjectNode();
		                   MBeanInfo info = catalogServerConnection.getMBeanInfo(beanName);
		                   MBeanAttributeInfo[] attrInfo = info.getAttributes();
		                   itemNode.put("beanName", beanName.getCanonicalName());
		                   ObjectNode attrNode = itemNode.putObject("attributes");
		                   String beanThings = beanName.getCanonicalName();
		        	       String beanItems = beanName.toString();
		        	       String beanTest = "";
	        	           if(!kBean.isEmpty()) {
	        	        	   if(beanThings.contains(kBean) || beanItems.contains(kBean)){
	        	        		   LOGGER.info("FOUND" + kBean);
			        	       } else {

			        	       }
  
	        	           }
	        	           
	        	           for (MBeanAttributeInfo attr : attrInfo) {
	        	        	   
	        	        	   parseMetrix(catalogServerConnection, beanName, kServer, attr, influxDB,kDomain,  jmxCon);
	        	           }
		                   
	        	           
                           influxDB.close();
		                  
		            }
					
		            
					
				} catch (Exception e) {
					if(jmxCon != null) {
						jmxCon.close();
					}
					LOGGER.error("ERROR: " + e);
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
					  .tag("host",kServer)
					  .addField("attr_value", Att_Value)
					  .addField("received_date", receivedDate)
					  .build();
			
			Thread.sleep(2000);	
			
			if(Attribute.equalsIgnoreCase(kafkaJMXPath) && Att_Value > thresholdLimit){
				String message = "Consumer Group: {" + consumerGroup + "} on Topic: " + topic + "} lag{" + Att_Value + "}";
			}
			
			influxDB.write(point);
		
		} catch (Exception e) {
				LOGGER.error("ERROR: " + e);
			
		}
		
		return "success";
		
		
	}
	
	
	
	public static void parseMetrix(MBeanServerConnection catalogServerConnection, ObjectName beanName, String kServer, MBeanAttributeInfo attr, InfluxDB influxDB, String kDomain, JMXConnector jmxCon ) {
	
	   try{
		   
    	   
	     
		       String beanThing = beanName.getCanonicalName();
		       String beanItem = beanName.toString();
		                      	       
		       jsonMessage jMsg = new jsonMessage();
		       jMsg.setHost(kServer);
		       jMsg.setBeanName(beanName.getCanonicalName());
	           jMsg.setAttribute(attr.getName());
	           String attValue = String.valueOf(catalogServerConnection.getAttribute(beanName, attr.getName()));
	           if(attValue.contains("SECONDS")){
	        	   return;
	           }
	           if(beanItem.contains("MessagesInPerSec")){
		   	           if(StringUtils.isNumeric(attValue) || attr.getName().toString().equalsIgnoreCase(kafkaJMX)|| attr.getName().toString().equalsIgnoreCase("MeanRate") || attr.getName().toString().equalsIgnoreCase("OneMinuteRate") || attr.getName().toString().equalsIgnoreCase("FiveMinuteRate") || attr.getName().toString().equalsIgnoreCase("FifteenMinuteRate")){
		   	        	jMsg.setAttributeValue(attValue);
		
		   	           }else{
		   	        	   return;
		   	           }
		   	           SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");               	           
		   	           Date now = new Date();
		   	           String strDate = sdfDate.format(now);
		   	           jMsg.setReceivedDate(strDate);
		   	           String msgCheck = jMsg.toString();
		    	       
		    	       double attrValue = Double.valueOf(attValue);
		    	       String putSuccess = putInfluxDB(beanName.getCanonicalName(),attr.getName(),attrValue,strDate,influxDB,kDomain );
			          
	           }else if(beanItem.contains(kafkaBeanName)){
		        	   SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");      
		        	   
		   	           Date now = new Date();
		   	           String strDate = sdfDate.format(now);
		   	           jMsg.setReceivedDate(strDate);
		   	           String msgCheck = jMsg.toString();
		
		    	       if(StringUtils.isNumeric(attValue)){
		    	    	   double attrValue = Double.valueOf(attValue);
		        	       String putSuccess = putInfluxDB(beanName.getCanonicalName(),attr.getName(),attrValue,strDate,influxDB,kDomain );
		
		    	       } else{
		        	       String putSuccess = putInfluxDB2(beanName.getCanonicalName(),attr.getName(),attValue,strDate,influxDB,kDomain );
		
		    	       }
		    	       
			  }else {
	               
			           SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");               	           
		   	           Date now = new Date();
		   	           String strDate = sdfDate.format(now);
		   	           jMsg.setReceivedDate(strDate);
		   	           String msgCheck = jMsg.toString();
		    	       if(StringUtils.isNumeric(attValue)){
		    	    	   double attrValue = Double.valueOf(attValue);
		        	       String putSuccess = putInfluxDB(beanName.getCanonicalName(),attr.getName(),attrValue,strDate,influxDB,kDomain );
		
		    	       } else{
		        	       String putSuccess = putInfluxDB2(beanName.getCanonicalName(),attr.getName(),attValue,strDate,influxDB,kDomain );
		
		    	       }		               	           
		    	       
			  }

	   } catch (Exception e) {
   			if(jmxCon != null) {
   				try {
   					
					jmxCon.close();
					
				} catch (IOException e1) {
			 		LOGGER.error("ERROR: " + e1);
				}
   			}
   		LOGGER.error("ERROR: " + e);
			
		}
	           
	
	
	
}
	
	
	
public static String putInfluxDB2(String BeanName, String Attribute, String Att_Value, String Received_date,InfluxDB influxDB, String kServer) throws IOException {
		
		try {
			
		
		
			String receivedDate = String.valueOf(System.currentTimeMillis());
	        
			
			
			Point point = Point.measurement("ops_kafkajmx_metrics")
					  .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
					  .tag("beanName", BeanName.toString())
					  .tag("attribute", Attribute)
					  .tag("host",kServer)
					  .addField("gen_attr_value", Att_Value)
					  .addField("received_date", receivedDate)
					  .build();
			
			Thread.sleep(2000);	
			
			if(Attribute.equalsIgnoreCase(kafkaJMXPath)){
				String message = "Consumer Group: {" + consumerGroup + "} on Topic: " + topic + "} lag{" + Att_Value + "}";
			}
			
			influxDB.write(point);
		
		} catch (Exception e) {
	 		LOGGER.error("ERROR: " + e);
			
		}
		
		return "success";
		
	}
}
