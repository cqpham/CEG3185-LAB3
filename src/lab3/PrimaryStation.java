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
	
	// buffered reader for user input
	public static BufferedReader buffReader = new BufferedReader(new InputStreamReader(System.in));
	
	// frame variables
	public static String flag = "01111110";
	public static String[] addresses = new String[10];
	public static int[] clientIDs = new int[10];
	public static int[] NS = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
	public static int[] NR = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	
	
	// other variables
	public static int port = 5555;
	public static String inputLine = null;
	public static int numClients = 0;
	
	// Frame buffer
	public static String[] frameBuffer = new String[20];
	
	/*
	 *  Constructor to initialize server 
	 */
	
	public PrimaryStation() {
		try {            
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
	
	public void listen() throws IOException {
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
        			pollClients();
        			
//        			System.out.println("Stop listening for new client? [y/n]");
//        			String answer;
//        			answer = buffReader.readLine();
//        			
//        			if (answer.toLowerCase().equals("y")) {
//        				listening = false;
//        			}
//        			else {
//        				System.out.println("Listening for next client...");
//        			}
        			
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
		numClients++;
	}
	
	/*
	 * Method to enable data exchange.
	 * 
	 * Primary station sends commands to Secondary station and waits for responses.
	 */
	
	public static void pollClients() {
		
		// Loop over each client connected
		for (int i=0; i<socketCount; i++) {
			String NRtoSend = threeBitBinary(NR[i]);
			
			// Send < RR, *, P > message to client: Reader to Receive (Poll) Frame 0 
			String toSend = flag + addresses[i] + "10001" + NRtoSend;
    		socketOut[i].println(toSend);
    		
    		System.out.println("Sent S-frame ("+toSend+") to station " + clientIDs[i]);
		}
	}
	
	/*
	 * 
	 */
	
	public static void communicate() throws IOException {
		// loop over each client
		for (int i=0;i<socketCount;i++) {
			boolean finished = false;
			String responseLine;
			
			// interpret message
			while (!finished) {
				responseLine = socketIn[i].readLine();
				
				if(responseLine != null) {
					// control field
					String controlField = responseLine.substring(16, 24);
					System.out.println("Control field: "+controlField);
					String addressField = responseLine.substring(8, 16);
					System.out.println("Address field: "+addressField);
					
					// determine if I Frame, U Frame or S Frame
					if (controlField.substring(0, 1).equals("0")) {
						// I-Frame
						String message = responseLine.substring(24, responseLine.length());
						
						message = decodeBinary(message);
						
						if (addressField.equals("00000000")) {
							// message meant for primary A
							System.out.println("Received the following message from "+clientIDs[i]+": "+message);
						}
						else {
							// message meant for client
							int client_id = Integer.parseInt(addressField, 2);
							
							System.out.println("Message was meant for client with ID: "+ client_id);
							
							socketOut[client_id-1].println(responseLine);
						}
						
						NS[i]++;
						NR[i]++;
						//pollClients();
					}
					else if (controlField.substring(0, 2).equals("10")) {
						// S-Frame
						
					}
					else if (controlField.substring(0, 2).equals("11")) {
						// U-Frame
						
					}
					finished = true;
				}
			}
		}
	}
	
	/*
	 * Method to convert decimal number to binary
	 */
	
	public static String threeBitBinary(int number) {
		String binaryString = Integer.toBinaryString(number);

		if (binaryString.length() == 1) {
			binaryString = "00" + binaryString;
		}
		else if (binaryString.length() == 2) {
			binaryString = "0" + binaryString;
		}
		
		return binaryString;
	}
		
	/*
	 * Method to convert binary message to readable String
	 */
	
	public static String decodeBinary(String bin) {

		char[] result = bin.toCharArray();
		
		String conversionString = "";
		String resultString = "";
		int binChar = 0;
		char asciiChar = '0';
		
		for(int i = 0; i<result.length; i++)
		{
			conversionString = conversionString+result[i];
			if(i!= 0 && (i+1)%8 == 0)
			{
				binChar = Integer.parseInt(conversionString,2);
				asciiChar = (char)binChar;
				resultString = resultString+asciiChar;
				conversionString = "";
			}
		}
		
		return resultString;
	}
	
	/***********************************************************************************
	 * Main method
	 */
	
	public static void main(String[] args) throws IOException {
		PrimaryStation primary = new PrimaryStation();
		
		primary.listen();
		primary.communicate();
	}

}

