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
	public static String flag = "01111110";
	public static String inputLine = null;
	public static int NS = 0; // send sequence number
	public static int NR = 0; // receive sequence number
	public static String primAddr = "00000000";
	
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
		
		
		inputLine = inputStream.readLine();
		String SNRM = inputLine.substring(16, 24);
		
		// Receive SNRM message from primary and send UA response
		if (SNRM.equals("11001001")) {
			System.out.println("Received SNRM from primary: "+SNRM);
			
			String UA = flag + primAddr + "11000110";
			outputStream.println(UA);
			
			System.out.println("Sent UA ("+UA+") message to primary.");
		}
	}
	
	/*
	 * Method to enable data exchange.
	 * 
	 * NOTE: Secondary station only responds to commands from the Primary (NRM).
	 */
	
	public static void createResponse() throws IOException {
		while (true) {
			boolean finished = false;
			String responseLine = null;
			String control = null;
			String address = null;
			
			while (!finished) {
				while (responseLine == null) {
					//System.out.println("response line null");
					responseLine = inputStream.readLine();
				}
				
				if (responseLine != null) {
					control = responseLine.substring(16, 24);
					address = responseLine.substring(8,16);
					System.out.println("Received control message from primary: "+control);
					System.out.println("Address in message from primary: "+address);
				}
				
				// Interpret message from primary
				if (control.substring(0,1).equals("0")) {
					// I-Frame, i.e. I, *, *
					String message = responseLine.substring(24, responseLine.length());
					
					message = decodeBinary(message);
					
					System.out.println("Received the following message from "+address+": "+message);
					finished = true;
				}
				else if (control.substring(0,2).equals("10")) {
					// S-Frame, i.e. RR, *, P
					//System.out.println("Entered s-frame treatment.");
					if (control.substring(4,5) == "1") {
						// Send acknowledgement
						//System.out.println("Entered polling reaction.");
						String frameToSend = flag + primAddr + "1000" + NR;
						outputStream.println(frameToSend);
					}
					finished = true;
				}
				else if (control.substring(0,2).equals("11")) {
					// U-Frame, i.e. UA/SNRM
					finished = true;
				}
			}
			
			System.out.println("Send message? [y/n]");
			String answer;
			answer = buffReader.readLine();
			
			if (answer.toLowerCase().equals("y")) {
				sendIFrame();
			}
			else {
				// continue
			}
		}
	}
	
	/*
	 * Method to send an IFrame to the primary
	 */
	
	public static void sendIFrame() throws IOException {
		String message = null;
		String address = null;
		boolean valid = false;
		
		while (!valid) {
			System.out.println("Enter address to send to: ");
			address = buffReader.readLine();
			
			if (validateAddress(address)) {
				valid = true;
			}
			else {
				System.out.println("Please enter valid address.");
			}
		}
		
		
		while(message == null) {
			System.out.println("Enter message to send (max 64 bytes): ");
			message = buffReader.readLine();
			
			// Convert message to bytes
			message = toBytes(message);
		}
		
		System.out.println("Message to send: "+message);
		String frameToSend = flag + address + "0" + threeBitBinary(NS) + "0" + threeBitBinary(NR) + message;
		System.out.println("Frame to send: "+frameToSend);
		
		outputStream.println(frameToSend);
	}
	
	
	/*
	 * Method to convert string of characters into bytes (cannot exceed 64 bytes)
	 */
	
	public static String toBytes(String message) {
		byte[] msgBytes = message.getBytes();
		
		if (msgBytes.length > 64) {
			System.out.println("Message exceeds 64 bytes! Try again.");
			return null;
		}
		
		StringBuilder msgBinary = new StringBuilder();
		
		for (byte b : msgBytes) {
			int value = b;
			
			for (int i=0; i<8; i++) {
				msgBinary.append((value & 128) == 0 ? 0 : 1);
		        value <<= 1;
			}
		}
		
		return msgBinary.toString();
	}
	
	/*
	 * Method to validate correct binary address
	 */
	
	public static boolean validateAddress(String address) {
		if (address.length() != 8) {
			System.out.println("Must enter address of 8-bit length.");
			return false;
		}
		
		for (int i=0; i<address.length(); i++) {
			if (address.charAt(i) != '0' && address.charAt(i) != '1') {
				System.out.println("Found invalid character in bit string.");
			}
		}
		
		return true;
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
	 * @throws IOException 
	 */
	
	public static void main(String[] args) throws IOException {
		SecondaryStation secondary = new SecondaryStation();
		
		secondary.createResponse();
	}

}