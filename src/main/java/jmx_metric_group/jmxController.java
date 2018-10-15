package jmx_metric_group;


import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.coordinator.group.GroupMetadataManager;
import kafka.producer.Producer;
import kafka.api.KAFKA_1_0_IV0;
import kafka.common.TopicAndPartition;

import com.jcraft.jsch.*;
import com.jcraft.jsch.Session;
import com.rabbitmq.client.AMQP;

import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.io.PrintWriter;



import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.sql.Connection;



import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import javax.mail.*;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.avro.generic.GenericData.Record;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Increment;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.RetriesExhaustedWithDetailsException;
import org.apache.hadoop.hbase.util.Bytes;


import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.protocol.types.Type;


import org.joda.time.DateTime;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.std.StringSerializer;
import com.fasterxml.jackson.databind.ser.std.NumberSerializers.LongSerializer;

 
@SuppressWarnings("deprecation")
public class jmxController{
    
 

	public static String username;
	public static String password;
	public static String connectionString;

	public static String streamURL;
	public static String outputqueue;
	public static String logFileDirectory;
	public static String logErrorFileDirectory;
	public static String adminEmail;
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

	public static int looper=0;
	
	/* kafka related variables*/
	public static Properties props = null;
	public static String kafkaServer;
	public static String kafkaBrokers;
	
	public static String DGroupID;
	public static String consumerGroup;
	public static String uname;
	public static String pword;
	public static String topic;
	public static KafkaProducer<String,String> producer = null;
 	
	//unix remote session variables
	
	

	
	
	public static Logger logger = LoggerFactory.getLogger("jmxController");
	
       
    public static void main(String[] args) throws MessagingException, IOException, JSchException {
      	
    	String command = "";

       	long totalLag=0;
       	int exitStatus =0;
       	int counter = 0;
       	    	
    	Session session;
    	Channel channel;
    	JSch jsch;
    	    	
    	getConfigurations();
    	
		
		props = new Properties();
		props.put("bootstrap.servers", kafkaBrokers);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
	   
		producer = new KafkaProducer<String, String>(props);

    	ArrayList<String> consumerList = new ArrayList<String>();
    	
  		/**********************************************
		
					STARTING jmx CONTROLLER
		
		***********************************************/
		
			
			//errorReporting("*STARTING DISQUS STREAM CONSUMER*");
			initializeAppLogs();

			rfop.write("*STARTING JMX CONSUMER*"  + "  "  + DateTime.now().toString());
			efop.write("*STARTING JMX CONSUMER*"  + "  "  + DateTime.now().toString());

			// section of code to create a kafka consumer to read from __consumer_offsets
			// use information to calculate total lag for each consumer group
			
			Properties consumerProps = new Properties();
		    consumerProps.put("exclude.internal.topics",  false);
		    consumerProps.put("group.id" , "test");
		   // consumerProps.put("bootstrap.servers", "kafka-dev01.socialgist.local:9092,kafka-dev02.socialgist.local:9092,kafka-dev03.socialgist.local:9092");
		    consumerProps.put("bootstrap.servers", kafkaBrokers);
		    consumerProps.put("key.deserializer","org.apache.kafka.common.serialization.ByteArrayDeserializer");  
		    consumerProps.put("value.deserializer","org.apache.kafka.common.serialization.ByteArrayDeserializer");	       
	        		    
		    
	        try {
	        	
	        	while(true){
			
		        	Properties config = new Properties();
				    jsch = new JSch();
					jsch.setKnownHosts("C:\\Users\\.ssh\\ssh_host_rsa_key.pub");
			     	session = jsch.getSession("root", kafkaServer, 22);
	
			        config.put("StrictHostKeyChecking", "no");
			    
			        session.setConfig(config);
		
			        session.setPassword("F3usLX!");
			        session.connect(50000);
	
			        
			        //=======================================================================================================================
				    // code to remotely execute sh script to retrieve all topics and consumer groups
			        if(kafkaBrokers.toLowerCase().contains("prod")){
			        	command = "/usr/hdf/3.1.1.0-35/kafka/bin/kafka-consumer-groups.sh --bootstrap-server " + kafkaServer + ":9092 --list";
			        } else if(kafkaBrokers.toLowerCase().contains("dev")){
			        	command = "/usr/hdf/3.1.2.0-7/kafka/bin/kafka-consumer-groups.sh --bootstrap-server " + kafkaServer + ":9092 --list";
			        }
			        //command = "/usr/hdf/3.1.2.0-7/kafka/bin/kafka-consumer-groups.sh --bootstrap-server " + kafkaServer + ":9092 --list";
			        openConnection(session, consumerList, command);
		           
			        // End of code section to retrieve topics and consumer groups list
			        //=============================================================================================================================================================

			        Thread.sleep(600000);
			        session.disconnect();
	        	}
	        } catch (JSchException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				
			}finally {
	        	 // kconsumer.close();
			
	        }    
		    

		System.out.println("Application Completed");
			
		
	} 

    private static void openConnection(Session session, ArrayList<String> consumerList, String command) throws JSchException{
    	
    	    int counter = 0;    
    	    
    	    StringBuilder sb = new StringBuilder();
    	    Channel channel = session.openChannel("exec");
     	    String scriptOutput = "";
	        
     	    try {
				
	        	
	            ((ChannelExec) channel).setCommand(command);
	        	PrintStream out= new PrintStream(channel.getOutputStream());
			    InputStream in = channel.getInputStream();
	
			    channel.connect();
			    
			   // out.println("/usr/hdf/3.1.2.0-7/kafka/bin/kafka-consumer-groups.sh --bootstrap-server 192.168.96.103:9092 --list" + "&&" + "/usr/hdf/3.1.2.0-7/kafka/bin/kafka-consumer-groups.sh --bootstrap-server 192.168.96.103:9092 --describe -group twitter.output");
			    
			    			    
		        BufferedReader scriptReader= new BufferedReader(new InputStreamReader(in));
		        scriptOutput = scriptReader.readLine();
		       
		        sb = new StringBuilder();
	
		        while ((scriptOutput = scriptReader.readLine())!= null) {
		        	sb.append(scriptOutput + "\n");
		            counter +=1;
		            if(scriptOutput.equalsIgnoreCase("")){
		            	continue;
		            } else if(scriptOutput.equalsIgnoreCase("Note: This will not show information about old Zookeeper-based consumers.")){
		            	Thread.sleep(1110);
		            	//	createJson(consumerList,channel, scriptOutput);
		            }
		              else{
		            	consumerList.add(scriptOutput);		            	 
		            }
		        }
		        
		        in.close();
	        
	        } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        
     	    
	       
     	    System.out.println(sb.toString());
     	   
     	   if(kafkaBrokers.toLowerCase().contains("prod")){
	        	command = "/usr/hdf/3.1.1.0-35/kafka/bin/kafka-consumer-groups.sh --bootstrap-server "+ kafkaServer + ":9092 --describe -group";
	        } else if(kafkaBrokers.toLowerCase().contains("dev")){
	        	command = "/usr/hdf/3.1.2.0-7/kafka/bin/kafka-consumer-groups.sh --bootstrap-server "+ kafkaServer + ":9092 --describe -group";
	        }
     	    
	        
	        channel.disconnect();
	        createJson(consumerList,channel,command, scriptOutput, session);
 	        session.disconnect();
	        
    	
    	
    	
    }
    
    private static void createJson(ArrayList<String> consumerList, Channel channel, String command, String scriptOutput, Session session) throws JSchException{
    	  
    	  //=============================================================================================================================================================
		  // code to create json messages with consumer-group, topic, offset and lag info
	      
    	  int counter = 0;
    	  Channel channelII = session.openChannel("exec");
    	  StringBuilder sb = new StringBuilder();
    	  String origCommand = command;
    	  String partitioner = "";
    	  
    	  try {


	          for(int c=0; c < consumerList.size(); c++){
	     
	        		  consumerGroup = consumerList.get(c);

	        		  command = command + " " + consumerGroup;
	             	  ((ChannelExec) channelII).setCommand(command);
	             	  ((ChannelExec)channelII).setErrStream(System.err);
	           	      InputStream in = channelII.getInputStream();
	           		  channelII.connect();
	           		  BufferedReader scriptReader= new BufferedReader(new InputStreamReader(in));
	           		  scriptOutput = scriptReader.readLine();
	           		  counter =0;
	           		  if(scriptOutput.contains("Error:")){
	           			  System.out.println(scriptOutput);
	           			  command = origCommand;
	           			  channelII.disconnect();
					      channelII = session.openChannel("exec");
	           			  continue;
	           		  }
	           		  Map<String, String> row = new HashMap<String, String>();
					  sb = new StringBuilder();
	           		  while ((scriptOutput = scriptReader.readLine())!= null) {
	           			  sb.append(scriptOutput + "\n");
			           	  counter +=1;
				          if(scriptOutput.contains("TOPIC") || scriptOutput.contains("READING")){
				           	continue;
				          } else{
				           	 row.put(consumerGroup + counter, scriptOutput);		            	 
				          }
				      }
				        
				      lagTotal = 0;
    			      row.forEach((k,v)->{
				    		//System.out.println("Item : " + k + " Count : " + v);
				    		
				    		looper +=1;
				    		String[] splitted = v.split("\\s+");
			            	jsonMessage jMsg = new jsonMessage();
			            	
					        jMsg.setHost(kafkaServer);
					        jMsg.setConsumerGroup(consumerGroup);
					        jMsg.setPartition(splitted[1]);
					        jMsg.setCurentOffset(splitted[2]);
					        jMsg.setTopic(splitted[0]);
					        jMsg.setLogEndOffset(Long.parseLong(splitted[3]));
					        jMsg.setLag(Long.parseLong(splitted[4]));
					        
					        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
					        Date now = new Date();
					        String strDate = sdfDate.format(now);
					        
					        jMsg.setReceivedDate(strDate);
					        lagTotal = lagTotal + Long.parseLong(splitted[3]);
					        if(looper == row.size()){
					        	lagTotal = lagTotal / 1000;
					            double avgLag = lagTotal / row.size();
						        jMsg.setAverageLag(avgLag);
						        System.out.println(avgLag);
						        
					        }
					     	
					     	try {
					     		
					     		
								producer.send(new ProducerRecord<String, String>("kafka.consumer.metrix","",jMsg.toString()));
								System.out.println(jMsg.toString());
								
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		
				    	});
				       channelII.disconnect();
				       channelII = session.openChannel("exec");
	        	}
	        	
	        //	System.out.println(sb.toString());
	        
	        
		   
		        
	       
			}catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}finally {
	        	 // kconsumer.close();
				channelII.disconnect();
			
	        }    
    	  
    	     System.out.println(sb.toString());
    	  
	        // End of code section to retrieve lag info 
	        //=======================================================================================================================
    	  
    	  
    	  
    	  
    	  
    	  
      }
        
    private static void getConfigurations() {
		
		 Properties fileProp = new Properties();
		 try {
				
				
				
		    fileProp.load(jmxController.class.getClassLoader().getResourceAsStream("config.properties"));
		    System.out.println("READING CONFIG PROPERTIES FILE...");
	   		
		    consumerGroup=fileProp.getProperty("groupID");
		    connectionString = fileProp.getProperty("MySQLConnectionString");
			logFileDirectory = fileProp.getProperty("logFileDirectory");
    		logErrorFileDirectory = fileProp.getProperty("logErrorFileDirectory");
			adminEmail = fileProp.getProperty("adminEmail");
			smtp = fileProp.getProperty("smtp");
			smtpPort = fileProp.getProperty("smtpPort");
			smtp_username = fileProp.getProperty("smtp_username");
			smtp_password = fileProp.getProperty("smtp_password");

			username = fileProp.getProperty("username");
			password = fileProp.getProperty("password");
			streamURL =fileProp.getProperty("streamUrl");
	
			uname = fileProp.getProperty("uname");
			pword = fileProp.getProperty("pword");
			kafkaServer = fileProp.getProperty("kafkaServer");
			kafkaBrokers = fileProp.getProperty("kafkaBrokers");
			
			
			
			
			
			
		} catch (IOException io) {
			io.printStackTrace();
		} 		
			
		}
          
    public static void initializeAppLogs() throws MessagingException{
		  
		  String logPath = logFileDirectory;
		  String logErrPath = logErrorFileDirectory;
		  
		  SimpleDateFormat ft = new SimpleDateFormat("yyyyMMddhhmmssa");
		  Date dNow = new Date();
		  
		  rfile = new File(logPath + "\\jmx_controller_" + ft.format(dNow) + ".txt");
		  efile = new File(logErrPath + "\\jmx_controller_ERR_" + ft.format(dNow) + ".txt");
		  
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
        
    private static void errorReporting(String errMessage) throws MessagingException{
		// email client handling error messages
		String to_email="zfareed@boardreader.com";
		String from_email="zfareed@boardreader.com";
		String host_server="192.168.5.7";
		
		Properties properties = System.getProperties();
		properties.setProperty("mail.smtp.host",host_server);
		properties.setProperty("mail.smtp.user","rainmaker");
		properties.setProperty("mail.smtp.password", "97CupChamps");
		properties.setProperty("mail.smtp.port","25");
		
		javax.mail.Session session= javax.mail.Session.getDefaultInstance(properties);
		
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from_email));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(to_email));
		message.setSubject("DISQUS_PRODUCER");
		message.setContent(errMessage,"text/html");
		Transport.send(message);
		System.out.println("MESSAGE SENT - WOO WOO!");
		
	}
    
    }




