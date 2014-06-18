package com.obs.integrator;

import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;

public class ComvenientThreadedQueueAdapter {

	public static void main(String[] args) {

		try {
			Queue<ProcessRequestData> queue = new ConcurrentLinkedQueue<ProcessRequestData>();
			PropertiesConfiguration prop = new PropertiesConfiguration("ComvenientIntegrator.ini");
			String logPath=prop.getString("LogFilePath");
			
			File filelocation = new File(logPath);			
			if(!filelocation.isDirectory()){
				filelocation.mkdirs();
			}
			
			Logger logger = Logger.getRootLogger();
			FileAppender appender = (FileAppender)logger.getAppender("fileAppender");
			appender.setFile(logPath+"ComvenientIntegrator.log");
			appender.activateOptions();
			
			
			ComvenientProducer p = new ComvenientProducer(queue,prop);
			ComvenientConsumer c = new ComvenientConsumer(queue,prop);
            
			
			Thread t1 = new Thread(p);
			Thread t2 = new Thread(c);

			t1.start();
			t2.start();
			
		} catch (ConfigurationException e) {
			System.out.println("(ConfigurationException) Properties file loading error.... : " + e.getMessage());
		} 

	}
}
