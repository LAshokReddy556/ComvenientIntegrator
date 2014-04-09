package com.obs.integrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;

public class ProcessCommandImpl {

	private static BufferedReader reader;
	private static PrintWriter printWriter;
	private static Socket requestSocket = null;
	static Logger logger = Logger.getLogger(Consumer.class);
	private static PropertiesConfiguration prop;
	public Timer timer;
	private String number;
	private Long id;
	public int timePeriod;
	public static int wait;

	@SuppressWarnings("static-access")
	public ProcessCommandImpl(Socket requestSocket1,
			PropertiesConfiguration prop2) {
		try {			
			this.requestSocket = requestSocket1;
			this.prop =prop2;
			wait = prop.getInt("ThreadSleep_period");
			timePeriod=prop.getInt("TimePeriod");	
			printWriter = new PrintWriter(requestSocket.getOutputStream(), true);
			reader = new BufferedReader(new InputStreamReader(requestSocket.getInputStream()));
			number=prop.getString("SmsOperatorId");
			String command1 = "1,REGIST,"+number;
			logger.info("sending Registration command to CAS server is: "+command1);	
			printWriter.println(command1);
			if (printWriter.checkError()) {
				ProcessCommandImpl.closeIOFile();
			}else{		
				Reminder(timePeriod);
				String output = reader.readLine();
				logger.info("output from CAS Server is :" + output);
				CSVReader reader1 = new CSVReader(new StringReader(output));
				String[] tokens = reader1.readNext();
				if (tokens.length > 2) {
					String message = tokens[1];		
					if (message.equalsIgnoreCase("0")) {
						logger.info("Registration command successfully executed");
					}else if(message.equalsIgnoreCase("9000")){
						logger.info("Registration command failure");
						String error = tokens[2];
						logger.error("failure: " + error);
						throw new IllegalAccessException() ;
					}else {
						logger.info("Registration command failure");
						String error = tokens[2];
						logger.error("failure: " + error);
						throw new Exception() ;
					}
				}
			}
		} catch (IllegalAccessException e) {
			logger.error("wrong SMS_operatorId:"+number+" , server connection through this SMS OperatorId is Not Possible");
		} catch (Exception e){
			logger.error("exception : " + e.getCause().getLocalizedMessage());
		}
	}

	public void processRequest(ProcessRequestData processRequestData) {
		
		try {
			if (printWriter.checkError()) {
				ProcessCommandImpl.closeIOFile();
			} else if (requestSocket != null) {
				
				synchronized (prop) {
					id = prop.getLong("srno");					
					id = id + 1;
					prop.setProperty("srno", id);				
						prop.save();					 					
				}
				
				if (processRequestData.getRequestType().equalsIgnoreCase(
						ComvenienConstants.REQ_ACTIVATION)) {
					Reminder(timePeriod);
					String command = id + ","
							+ ComvenienConstants.ACTIVATION_CMD + ","
							+ processRequestData.getProduct() + ",("
							+ processRequestData.getSmartcardId() + ")";
					logger.info(command);
					printWriter.println(command);
					id=null;
					if (printWriter.checkError()) {
						ProcessCommandImpl.closeIOFile();
					} else {					
						String output = reader.readLine();
						ProcessCommandImpl.process(output, processRequestData.getId(),processRequestData.getPrdetailsId());	
					}

				} else if (processRequestData.getRequestType()
						.equalsIgnoreCase(ComvenienConstants.REQ_RECONNECT)) {
					Reminder(timePeriod);
					String command = id + ","
							+ ComvenienConstants.ACTIVATION_CMD + ","
							+ processRequestData.getProduct() + ",("
							+ processRequestData.getSmartcardId() + ")";
					logger.info(command);
					printWriter.println(command);
					id=null;
					if (printWriter.checkError()) {
						ProcessCommandImpl.closeIOFile();
					} else {						
						String output = reader.readLine();
						ProcessCommandImpl.process(output, processRequestData.getId(),processRequestData.getPrdetailsId());	
					}
				} else if (processRequestData.getRequestType()
						.equalsIgnoreCase(ComvenienConstants.REQ_DISCONNECTION)) {
					Reminder(timePeriod);
					String command = id + ","
							+ ComvenienConstants.DISCONNECTION_CMD + ","
							+ processRequestData.getProduct() + ",("
							+ processRequestData.getSmartcardId() + ")";
					logger.info(command);
					printWriter.println(command);
					id=null;
					if (printWriter.checkError()) {
						ProcessCommandImpl.closeIOFile();
					} else {					
						String output = reader.readLine();	
						ProcessCommandImpl.process(output, processRequestData.getId(),processRequestData.getPrdetailsId());						
					}
				} else {
					Reminder(timePeriod);
					Long message_serialNo;
					synchronized (prop) {
						message_serialNo = prop.getLong("OsdMessage_SerialNo");					
						message_serialNo = message_serialNo + 1;
						prop.setProperty("OsdMessage_SerialNo", message_serialNo);				
							prop.save();					 					
					}
					String command = id + ","
							+ ComvenienConstants.MESSAGE_CMD + ","
							+ processRequestData.getSmartcardId() + ","
							+ "0,ONCE,"+ message_serialNo +",'"+ processRequestData.getProduct() +"'";
					logger.info(command);
					printWriter.println(command);
					id=null;
					if (printWriter.checkError()) {
						ProcessCommandImpl.closeIOFile();
					} else {				
						String output = reader.readLine();	
						ProcessCommandImpl.process(output, processRequestData.getId(),processRequestData.getPrdetailsId());	
					}
				}
			} 
		} catch (IOException exception) {
			logger.error("The Socket server connection is DisConnected, ReConnect to the Server");
			Consumer.getConnection();
		} catch (ConfigurationException e) {
			logger.error("(ConfigurationException) Properties file loading error.... : " + e.getMessage());
		} 

	}
	
	

	public void connectionHolding() {
		try {
			if (requestSocket != null) {
				synchronized (prop) {
					Long no = prop.getLong("srno");
					String command = no + "," + ComvenienConstants.HOLD_MESSAGE;
					no = no + 1;
					prop.setProperty("srno", no);
					prop.save();
					logger.info(command);
					printWriter.println(command);
					if (printWriter.checkError()) {
						ProcessCommandImpl.closeIOFile();
					}
				}
			} else {
				ProcessCommandImpl.closeIOFile();
			}
		} catch (ConfigurationException e) {
			logger.error("(ConfigurationException) Properties file loading error.... : " + e.getMessage());
		} 
	}


	public static void closeIOFile(){
		try{
		printWriter.close();
		reader.close();
		Thread.sleep(wait);
		throw new IOException();
		} catch(InterruptedException e){
			logger.error("thread is Interrupted for the : " + e.getCause().getLocalizedMessage());
		} catch (IOException e) {
			logger.error("The Socket server connection is DisConnected, ReConnect to the Server");
			Consumer.getConnection();
		}
	}
	
	public static void process(String value, Long id, Long prdetailsId){
		
		try{		
			logger.info("output from CAS Server is :" +value);
			if(value==null){
				throw new NullPointerException();
			}else{		
				Consumer.sendResponse(value,id,prdetailsId);
			}		
		} catch(NullPointerException e){
			logger.error("NullPointerException : Output from the Oss System Server is : " + value);
			ProcessCommandImpl.closeIOFile();
		} catch (Exception e) {
		    logger.error("Exception : " + e.getMessage());
	    }
		
	}
	
	public void Reminder(int seconds) {
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		timer.schedule(new RemindTask(), seconds);
	}

	class RemindTask extends TimerTask {
		public void run() {
			connectionHolding();
			Reminder(timePeriod);
		}
	}
}
