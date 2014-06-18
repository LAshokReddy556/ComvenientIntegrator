package com.obs.integrator;

import java.io.IOException;
import java.io.StringReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Queue;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import net.sf.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import au.com.bytecode.opencsv.CSVReader;

public class ComvenientConsumer implements Runnable {

	private static Queue<ProcessRequestData> queue;
	static Socket requestSocket = null;
	private static PropertiesConfiguration prop;
	String message;
	private static HttpPost post;
	private static byte[] encoded;
	private static String tenantIdentifier;
	private static HttpClient httpClient;
	static Logger logger = Logger.getLogger("");
	private static ProcessCommandImpl processCommand;
	public static int wait;

	public static HttpClient wrapClient(HttpClient base) {

		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {
				@SuppressWarnings("unused")
				public void checkClientTrusted(X509Certificate[] xcs,
						String string) throws CertificateException {
				}

				@SuppressWarnings("unused")
				public void checkServerTrusted(X509Certificate[] xcs,
						String string) throws CertificateException {
				}

				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				@Override
				public void checkClientTrusted(
						java.security.cert.X509Certificate[] arg0, String arg1)
						throws java.security.cert.CertificateException {
				}

				@Override
				public void checkServerTrusted(
						java.security.cert.X509Certificate[] arg0, String arg1)
						throws java.security.cert.CertificateException {
				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = base.getConnectionManager();
			SchemeRegistry sr = ccm.getSchemeRegistry();
			sr.register(new Scheme("https", ssf, 443));
			return new DefaultHttpClient(ccm, base.getParams());
		} catch (Exception ex) {
			return null;
		}
	}

	public static void getConnection() {
		try {
			logger.info("Connecting with Server ...");
			wait = prop.getInt("ThreadSleep_period");
			int portNumber = prop.getInt("port_number");
			String hostAddress = prop.getString("host_address");
			requestSocket = new Socket(hostAddress, portNumber);
			logger.info("Server is Connected with in the Host: '" + hostAddress + "' and PortNumber: '" + portNumber + "'. ");
			processCommand = new ProcessCommandImpl(requestSocket, prop);		
		} catch (UnknownHostException e) {
			logger.error("UnknownHostException:" + e.getMessage() + ". The host_address or port_number is invalid . Verify the Details... ");
			return;
		} catch (IOException e) {
			logger.error(" Connection to the CAS server is Not Established .... , " + e.getMessage());
			try {
				Thread.sleep(wait);
				ComvenientConsumer.getConnection();
			} catch (InterruptedException e1) {
				logger.error("thread is Interrupted for the : " + e.getCause().getLocalizedMessage());
			}
		}catch (NullPointerException e) {
			logger.error("NullPointerException:" + e.getMessage() + " ");
			return;
		}

	}

	@SuppressWarnings("static-access")
	public ComvenientConsumer(Queue<ProcessRequestData> queue1,
			PropertiesConfiguration prop2) {
		try {
			this.queue = queue1;
			//this.prop = new PropertiesConfiguration("ComvenientIntegrator.ini");
			this.prop=prop2;
			httpClient = new DefaultHttpClient();
			httpClient = wrapClient(httpClient);
			String username = prop.getString("username");
			String password = prop.getString("password");
			tenantIdentifier = prop.getString("tenantIdentfier");
			String ashok = username.trim() + ":" + password.trim();
			encoded = Base64.encodeBase64(ashok.getBytes());
			ComvenientConsumer.getConnection();
		} catch (Exception e) {
			logger.error("Exception:" + e.getStackTrace());
		}

	}

	@Override
	public void run() {

		while (true) {
			logger.info("Consumer() class calling ...");
			try {
				synchronized (queue) {
					consume();
					queue.wait();
				}
			} catch (InterruptedException ex) {
				logger.error("thread is Interrupted for the : " + ex.getCause().getLocalizedMessage());
			}
		}
	}

	private void consume() {
		try {
			if (requestSocket != null) {		
				while (!queue.isEmpty()) {
					for (ProcessRequestData processRequestData : queue) {
						queue.poll();
						processCommand.processRequest(processRequestData);					
					}
					queue.notifyAll();
				}
			} else {
				Thread.sleep(wait);
				ComvenientConsumer.getConnection();
			}
		} catch (InterruptedException e) {
			logger.error("thread is Interrupted for the : " + e.getCause().getLocalizedMessage());
		}

	}

	public static void sendResponse(String output, Long id, Long prdetailsId) {
		
		try {
			post = new HttpPost(prop.getString("PostQuery").trim() + id);
			post.setHeader("Authorization", "Basic " + new String(encoded));
			post.setHeader("Content-Type", "application/json");
			post.addHeader("X-Mifos-Platform-TenantId", tenantIdentifier);

			CSVReader reader = new CSVReader(new StringReader(output));
			String[] tokens = reader.readNext();
			JSONObject object = new JSONObject();

			if (tokens.length > 1) {
				String mes = tokens[1];
				String message = "";
				if (mes.equalsIgnoreCase("0")) {
					message = "success";
				}else {
					String errorid= tokens[1];
					String error = tokens[2];
					message = "failure : Exception error code is : " + errorid + " , Exception/Error Message is : " + error;
				}
				object.put("receiveMessage", message);
				object.put("receivedStatus", "1");
				object.put("prdetailsId", prdetailsId);
			}
			//logger.info("The json data sending to BSS System is :"+object.toString());
			StringEntity se = new StringEntity(object.toString());
			post.setEntity(se);
			HttpResponse response = httpClient.execute(post);
			response.getEntity().consumeContent();
			if (response.getStatusLine().getStatusCode() != 200) {
				logger.error("Failed : HTTP error code : "
						+ response.getStatusLine().getStatusCode());	
				Thread.sleep(wait);
				return;
			}
			else{
				logger.info("record is Updated Successfully in Bss System");
			}
		  

		} catch (IOException e) {
			logger.error("IOException : " + e.getMessage() + ". verify the BSS system server running or not");
		} /*catch (NullPointerException e) {
			logger.error("NullPointerException : " + e.getMessage() + " is passed and Output from the Oss System Server is : " + output);
		}*/ catch (InterruptedException e) {
			logger.error("thread is Interrupted for the : " + e.getMessage());
		} catch (Exception e) {
		logger.error("Exception : " + e.getMessage());
	    }
	  
	}
}
