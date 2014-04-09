package com.obs.integrator;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;

public class ThreadedQueueAdapter {

	public static void main(String[] args) {

		try {
			Queue<ProcessRequestData> queue = new ConcurrentLinkedQueue<ProcessRequestData>();
			PropertiesConfiguration prop = new PropertiesConfiguration("ComvenientIntegrator.ini");
			String logPath=prop.getString("LogFilePath");
			
			File filelocation = new File(logPath);			
			if(!filelocation.isDirectory()){
				filelocation.mkdirs();
			}
	
			/*PropertiesConfiguration prop1 = new PropertiesConfiguration("log4j.properties"); 
			prop1.setProperty("MY_HOME", logPath );		
			prop1.save();*/
			
			Logger logger = Logger.getRootLogger();
			FileAppender appender = (FileAppender)logger.getAppender("fileAppender2");
			appender.setFile(logPath+"consumer.log");
			appender.activateOptions();
			FileAppender appender1 = (FileAppender)logger.getAppender("fileAppender3");
			appender1.setFile(logPath+"producer.log");
			appender1.activateOptions();
			
			Producer p = new Producer(queue,prop);
			Consumer c = new Consumer(queue,prop);
            
			
			Thread t1 = new Thread(p);
			Thread t2 = new Thread(c);

			t1.start();
			t2.start();
			
		} catch (ConfigurationException e) {
			System.out.println("(ConfigurationException) Properties file loading error.... : " + e.getMessage());
		} catch (NullPointerException e) {
			System.out.println("AuthenticationException :  BSS system server username (or) password you entered is incorrect . check in the Integrator.properties file");
			return;
		} catch (IllegalAccessError e) {
			System.out.println("Resource NotFound Exception :  BSS server system get or post url error");
			return;
		} 
		catch (Exception e){
			System.out.println(e.getMessage());
			return;
		}

	}
}