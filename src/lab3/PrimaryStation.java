package lab3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class PrimaryStation {

	// server socket
	public static ServerSocket serverSocket = null;
		
	// array of max 10 client sockets
	public static Socket[] client_sockets = new Socket[10];
	public static int socketCount = 0;
	    
	// array of print writers for max 10 clients
	public static PrintWriter[] socketOut = new PrintWriter[10];
	    
	// array of buffered readers for max 10 clients
	public static BufferedReader[] socketIn = new BufferedReader[10];
	
	// frame variables
	public static String flag = "01111110";
	public static String[] addresses = new String[10];
	public static int[] clientIDs = new int[10];
	
	// other variables
	public static int successRate = 100;
	public static int port = 5555;
	public static String inputLine = null;
	
	/*
	 *  Constructor to initialize server 
	 */
	
	public PrimaryStation() {
		try {
    		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Percentage of frames to be accepted? (between 0-100):");
            successRate = Integer.parseInt(in.readLine());
            
            serverSocket = new ServerSocket(port);
            System.out.println("Primary station (A) listening on port "+port);
            
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + port);
            System.exit(-1);
        }
	}
	
	/*
	 * Listen method to allow server socket to listen for client connections
	 */
	
	public static void listen() throws IOException {
		boolean listening = true;
		
		while (listening){
        	
        	try {        		
        		client_sockets[socketCount] = serverSocket.accept();
        		
        		if (client_sockets[socketCount] != null){
        			System.out.println("Connection from " + 
        			client_sockets[socketCount].getInetAddress() + " accepted.");
        			
        			// Set address of current client
        			clientIDs[socketCount] = socketCount+1;
        			addresses[socketCount] = "00000000"+Integer.toBinaryString(clientIDs[socketCount]);
					int len = addresses[socketCount].length();					
					addresses[socketCount] = addresses[socketCount].substring(len-8);
        			
        			System.out.println("Client ID: "+clientIDs[socketCount]);
        			System.out.println("Client address: "+addresses[socketCount]);
        			
        			// Initialize socketOut and socketIn for current client 
        			socketOut[socketCount] = new PrintWriter(client_sockets[socketCount].getOutputStream(),true);
        			socketIn[socketCount] = new BufferedReader(new InputStreamReader(client_sockets[socketCount].getInputStream()));
        			
        			// Send client address to client
        			socketOut[socketCount].println(addresses[socketCount]);
        			
        			initialization();
        			communicate();
        		}
        	}
        	catch (InterruptedIOException e) { }
		}
	}
	
	/*
	 * Method to initialize the data link between primary and secondary station
	 * 
	 *  Primary station sends SNRM command and waits for UA response from Secondary station 
	 */
	
	public static void initialization() throws IOException {
		System.out.println("Ready for initialization");
		
		String control = "11001001";
		String SNRM = flag + addresses[socketCount] + control;
		
		// Send SNRM command to secondary
		socketOut[socketCount].println(SNRM);
		System.out.println("Sent SNRM to station " + clientIDs[socketCount]);
		
		// Receive UA message from secondary
		inputLine = socketIn[socketCount].readLine();
		String UAreceived = inputLine.substring(16, 24);
		
		if (UAreceived.equals("11000110")) {
			System.out.println("Received UA ("+UAreceived+") from secondary station with ID " + clientIDs[socketCount]);
		}
		else {
			System.out.println("An error occured UA reception from secondary station with ID "+ clientIDs[socketCount]);
		}
		
		socketCount++;
	}
		
	public static void communicate() {
		
		// Loop over each client connected
		for (int i=0; i<socketCount; i++) {
			// Send < RR, *, P > message to client
			String toSend = flag + addresses[i] + "10001000";
    		socketOut[i].println(toSend);
    		
    		System.out.println("Sent < RR,*,P > to station " + clientIDs[i]);
		}
	}
	
	/***********************************************************************************
	 * Main method
	 */
	
	public static void main(String[] args) throws IOException {
		PrimaryStation primary = new PrimaryStation();
		
		primary.listen();
	}

}

