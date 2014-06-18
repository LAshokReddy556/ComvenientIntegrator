package com.obs.integrator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;

import au.com.bytecode.opencsv.CSVReader;

public class Server {
	 private static PrintWriter printWriter;
	public static void main(String[] args) throws Exception {
	    // création de la socket
	    int port = 5989;
	  
	    ServerSocket serverSocket = new ServerSocket(port);
	    System.err.println("Serveur lancé sur le port : " + port);
      
	    // repeatedly wait for connections, and process
	    while (true) {
	        // on reste bloqué sur l'attente d'une demande client
	        Socket clientSocket = serverSocket.accept();
	        System.err.println("Nouveau client connecté");

	        // on ouvre un flux de converation

	        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
	        printWriter = new PrintWriter(clientSocket.getOutputStream(), true);
	     

	        // chaque fois qu'une donnée est lue sur le réseau on la renvoi sur
	        // le flux d'écriture.
	        // la donnée lue est donc retournée exactement au même client.
	        String output;
	        while ((output = in.readLine()) != null) {
	            System.out.println(output);
	            CSVReader reader1 = new CSVReader(new StringReader(output));
				String[] tokens = reader1.readNext();
				if (tokens.length > 2) {
					String command= tokens[1];
					if (command.equalsIgnoreCase("REGIST")){
						System.out.println("Registration command: "+output);		 
						String message = tokens[2];		
					   if (message.equalsIgnoreCase("000005")) {
						  printWriter.println("ok,0,Registration success");
					   }else{
						  printWriter.println("1,9000,Registration not ok");
					   }
				    } else{
				    	System.out.println("in else block for normal commands");
				    	   printWriter.println("ok,0");
				    }
				}
	         
	            if (output.isEmpty()) {
	                break;
	            }
	        }
	        

	        // on ferme les flux.
	      /*  System.err.println("Connexion avec le client terminée");
	        out.close();
	        in.close();
	        clientSocket.close();*/
	    }
	}

}

/*#https://localhost:8443/mifosng-provider/api/v1/entitlements/
#https://192.168.1.101:8443/mifosng-provider/api/v1/entitlements/
#https://spark.openbillingsystem.com:8443/mifosng-provider/api/v1/entitlements/
*/