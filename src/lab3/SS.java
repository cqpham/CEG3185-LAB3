package lab3;

import java.net.*;
import java.io.*;

public class SS {
	
	public static void main(String[] args) {
//		 declaration section:
//		 os: output stream
//		 is: input stream
		Socket clientSocket = null;
		PrintStream os = null;
		DataInputStream is = null;
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		
		String id = null;
		String information = null;
		String control = null;
		String flag = "01111110";
		String address = null;  
		String response = null; // control field of the socket input
		int ns = -1; // send sequence number
		int nr = 0; //receive sequence number
		
		//
		String answer = null; // input using keyboard
		//
		
       
        //		
//		 Initialization section:
//		 Try to open a socket on port 4444
//		 Try to open input and output streams
		try {
			clientSocket = new Socket("127.0.0.1", 4444);
			os = new PrintStream(clientSocket.getOutputStream());
			is = new DataInputStream(clientSocket.getInputStream());
			System.out.println("Client started on port 4444");
		} 
		catch (UnknownHostException e) {
			System.err.println("Don't know about host: hostname");
		} 
		catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to: hostname");
		}
				
		if (clientSocket != null && os != null && is != null) {			
			
			try {				
					
				String responseLine;				
				responseLine = is.readLine();
				
				//receive client address from the primary station
				//
				id = responseLine;
				System.out.println("Client address: " + id);
				
				responseLine = is.readLine();
				response = responseLine.substring(16, 24);

				// recv SNRM msg
				if(response.equals("11000001") || response.equals("11001001")) {
					//===========================================================
					// insert codes here to send the UA msg					
					//===========================================================
					String UA = null;
					UA = flag + "00000000" + "11000110";
					os.println(UA);
					System.out.println("Sent UA message ("+UA+") to primary");
				}
				
				// main loop; recv and send data msgs
				while (true) {
					responseLine = is.readLine();
					response = responseLine.substring(16, 24);
					
					System.out.println("Received message -- control " + response);				
					
					// recv ??RR,*,P?? msg
					if(response.substring(0,5).equals("10001")) {
						
						// enter data msg using keyboard 
						System.out.println("Is there any message to send? (y/n)");
						answer = in.readLine();						
						
						if(answer.toLowerCase().equals("y") || answer.toLowerCase().equals("yes")) {
							System.out.println("Please enter the destination address using 8-bits binary string (e.g. 00000001):");
							address = in.readLine();
							
							System.out.println("Please enter the message to send?");
							answer = in.readLine();
							answer = toBytes(answer);
							//===========================================================
							// insert codes here to send an I msg;
							
							//===========================================================
						
							ns = 0;
							nr = 0;
							String IFrameToSend = flag + address + "0" + threeBitBinary(ns) + "0" + threeBitBinary(nr) + answer;
							System.out.println("Sending I-Frame: "+IFrameToSend);
							
							os.println(IFrameToSend);

						}				
						else {
							//===========================================================
							// insert codes here to send ??RR,*,F??
							
							//===========================================================
							
							String SFrameToSend = flag + "00000000" + "10000" + threeBitBinary(nr);
							System.out.println("Sending S-Frame: "+SFrameToSend);
							
							os.println(SFrameToSend);
						}
					}
					
					// recv an I frame
					if(response.substring(0,1).equals("0")) {
						String data = responseLine.substring(24, responseLine.length()-8);
						System.out.println("");
						System.out.println("Received data: " + data);						
						
						nr = Integer.parseInt(response.substring(1,4), 2) + 1;
						System.out.println("nr: " + nr);
					}
				}
			} 
			catch (UnknownHostException e) {
				System.err.println("Trying to connect to unknown host: " + e);
			} 
			catch (IOException e) {
				System.err.println("IOException: " + e);
			}	
		}
	}
	/**********************************************************************************************/
    /* TOOL METHODS */
	
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

}// end of class SecondaryStation
