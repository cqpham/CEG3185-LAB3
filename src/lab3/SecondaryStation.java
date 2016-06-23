package lab3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class SecondaryStation {

	public static Socket clientSocket = null;
	public static PrintStream outputStream = null;
	public static BufferedReader inputStream = null;
	public static BufferedReader buffReader = new BufferedReader(new InputStreamReader(System.in));
	
	public static String host = "127.0.0.1";
	public static int port = 5555;
	public static String clientID = null;
	public static String responseLine;
	public static String flag = "01111110";
	
	/*
	 * Secondary station object constructor
	 * 
	 * Initializes connection to server socket
	 */
	
	public SecondaryStation() {
		try {
			clientSocket = new Socket(host, port);
			outputStream = new PrintStream(clientSocket.getOutputStream());
			inputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			
			System.out.println("Client initialised on port "+port+" and host "+host+".");
			
			// Receive client address from primary
			String addressFromPrime = null;
			addressFromPrime = inputStream.readLine();
			
			// Set clientID field (set to client address received from primary)
			clientID = addressFromPrime;
			
			System.out.println("Client ID: "+clientID);
			
			if (clientSocket != null && outputStream != null && inputStream != null) {
				initialization();
				communicate();
			}
		} 
		catch (UnknownHostException e) {
			System.err.println("Don't know about host: "+ host);
		} 
		catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to: "+host);
		}
	}
	
	/*
	 * Method to initialize the data link between primary and secondary station
	 * 
	 * Secondary station waits for SNRM command from Primary Station and then sends UA response
	 */
	
	public static void initialization() throws IOException {
		System.out.println("Ready for initialization.");
		
		
		responseLine = inputStream.readLine();
		String SNRM = responseLine.substring(16, 24);
		
		// Receive SNRM message from primary and send UA response
		if (SNRM.equals("11001001")) {
			System.out.println("Received SNRM from primary: "+SNRM);
			
			String UA = flag + "00000000" + "11000110";
			outputStream.println(UA);
			
			System.out.println("Sent UA ("+UA+") message to primary.");
		}
	}
	
	/*
	 * Method to send and received data
	 */
	
	public static void communicate() throws IOException {
		while (true) {
			boolean finished = false;
			int counter = 0;
			
			
			while (!finished) {
				responseLine = inputStream.readLine();
				
				if(responseLine.substring(responseLine.length()-1).equals("-")) {
					finished = true;
				}
				
				String control = responseLine.substring(16, 24);
				System.out.println("Received control message from primary: "+control);
				
				if(control.substring(0,5).equals("10001")) {
					String message;
					System.out.println("Enter message to send (max 64 bytes): ");
					message = buffReader.readLine();
					
					// Convert message to bytes
					message = toBytes(message);
					
					if (message ==  null) {
						System.out.println("ERROR: Message exceeds 64 bytes. Terminating client.");
						return;
					}
					
					System.out.println("Message to send: "+message);
					
				}
			}
		}
	}
	
	/*
	 * Method to convert string of characters into bytes (cannot exceed 64 bytes)
	 */
	
	public static String toBytes(String message) {
		byte[] msgBytes = message.getBytes();
		System.out.println("msgByte array length: "+msgBytes.length);
		
		if (msgBytes.length > 64) {
			return null;
		}
		
		StringBuilder msgBinary = new StringBuilder();
		
		for (byte b : msgBytes) {
			int value = b;
			
			for (int i=0; i<8; i++) {
				msgBinary.append((value & 128) == 0 ? 0 : 1);
		        value <<= 1;
			}
			msgBinary.append(" ");
		}
		
		return msgBinary.toString();
	}
	
	/***********************************************************************************
	 * Main method
	 */
	
	public static void main(String[] args) {
		new SecondaryStation();
	}

}