package lab3;

import java.net.*;
import java.io.*;

public class PS {	
	
    public static void main(String[] args) throws IOException {
        
        //
        // sockets and other variables declaration
        //
        // maximum number of clients connected: 10
        //
               
        ServerSocket serverSocket = null;
        Socket[] client_sockets;
        client_sockets = new Socket[10];
        PrintWriter[] s_out;
        s_out = new PrintWriter[10];
        BufferedReader[] s_in;
        s_in = new BufferedReader[10];
        
        int[] ns; // send sequence number
        ns = new int[10];
        
        int[] nr; // receive sequence number
        nr = new int[10];
        
        String inputLine = null;
        String outputLine = null;
        
        //
        //get port number from the command line
        //
        int nPort = 4444; // default port number
        //nPort = Integer.parseInt(args[0]);        
        
        String flag = "01111110";
        String[] address;
        address = new String[10];
        int[] clientID;
        clientID = new int[10];
        
        String control = null;
        String information = "";
        
        boolean bListening = true;
        
        String[] sMessages; // frame buffer
        sMessages = new String[20];
        int nMsg = 0;        
        
        boolean bAlive = false;
		
        String response = null; // control field of the input
        //
        // initialize some var's for array handling
        //
        int s_count = 0;
        int i = 0;       
        
        //
        // create server socket
        //
        try {
            serverSocket = new ServerSocket(nPort);
            System.out.println("Primary station started listening on port "+nPort);
            
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + args[0]);
            System.exit(-1);
        }
        
        //
        // this variable defines how many clients are connected
        //
        int nClient = 0;
                
        //
        // set timeout on the socket so the program does not
        // hang up
        //
        serverSocket.setSoTimeout(1000);
        
        //
        // main server loop
        //
        while (bListening){
        	
        	try {        		
        		//
        		// trying to listen to the socket to accept
        		// clients
        		// if there is nobody to connect, exception will be
        		// thrown - set by setSoTimeout()
        		//
        		client_sockets[s_count]=serverSocket.accept();
        		
        		//
        		// connection got accepted
        		//
        		
        		if (client_sockets[s_count]!=null){
        		
        			System.out.println("Connection from " + client_sockets[s_count].getInetAddress() + " accepted.");
        			
        			s_out[s_count] = new PrintWriter(client_sockets[s_count].getOutputStream(),true);
        			s_in[s_count] = new BufferedReader(new InputStreamReader(client_sockets[s_count].getInputStream()));
					
        			clientID[s_count] = s_count+1;		
        			
					address[s_count] = "00000000"+Integer.toBinaryString(clientID[s_count]);
					int len = address[s_count].length();					
					address[s_count] = address[s_count].substring(len-8);				
					        			
        			System.out.println("Client address: " + address[s_count]);
        			System.out.println("Client ID: "+clientID[s_count]);
        			
        			// send client address to the new client
        			s_out[s_count].println(address[s_count]);
        			
        			//
                	// initialization
                	// 
        			
        			// ===========================================================
        			// insert codes here to send SNRM message
        			//        			
        			String SNRM;
        			SNRM = flag + address[s_count] + "11001001";
        			s_out[s_count].println(SNRM);
        			System.out.println("Sent SNRM ("+SNRM+") to station " + clientID[s_count]);            		
            		// ===============================================================
        			
            		// recv UA message
            		inputLine = s_in[s_count].readLine();
            		response = inputLine.substring(16, 24);
            		
            		if(response.equals("11000110") || response.equals("11001110")) {
            			System.out.println("Received UA from station " + clientID[s_count]);
            		}
            		else {
            			System.out.println("UA error -- station " + clientID[s_count]);
            		}       
        			
            		// initialize ns and nr
            		ns[s_count] = -1;
            		nr[s_count] = 0;
            		
            		//            		 
        			// increment count of clients
        			//
        			s_count++;
        			nClient = s_count;
        			bAlive = true;
        		}
        	}
        	catch (InterruptedIOException e) {}
		
        	for (i=0;i<s_count;i++) {

        		// ==============================================================
        		// insert codes here to send â€œRR,*,Pâ€ msg     		
        		String frameToSend;
        		int cnt = 0;
        		frameToSend = flag + address[i] + "10001000";
        		s_out[i].println(frameToSend);
        		
        		System.out.println("Sent < RR,*,P > to station " + clientID[i]);
        		// ==============================================================
        		
        		// recv response from the client
        		boolean finished = false;
        		int count1 = 0;
        		int count2 = 0;
        		
        		while (!finished) {
        			inputLine = s_in[i].readLine();
        			System.out.println("Response received from client: "+inputLine);
        			if(inputLine != null) {		
        		
        				// get control field of the response frame
        				response = inputLine.substring(16, 24);
        		
        				if(response.substring(0,4).equals("1000")) {
        					// recv â€œRR,*,Fâ€, no data to send from B
        					System.out.println("Response from station "+clientID[i]+": "+inputLine);
        					System.out.println("Receive RR, *, F from station " + clientID[i]);
        					finished = true;
        				}
        				else if(response.substring(0, 1).equals("0")) {
        					// ==============================================================
        					// insert codes here to handle the frame â€œI, *, *â€ received
        				
        					String addressToSendTo = inputLine.substring(8, 16);
        					String msgPortion = inputLine.substring(24, inputLine.length());
        					System.out.println("Message portion of frame received: "+msgPortion);
        					//if the frame is to the primary station; consume it        					
        					
        					if (addressToSendTo.equals("00000000")) {
        						System.out.println("Message received from station "+clientID[i]+": "+decodeBinary(msgPortion));
        					}
        				
        					//if the frame is to the secondary station; buffer the frame to send
        					else {
        						String controlStr = inputLine.substring(16,24);
								String recNs = controlStr.substring(1,4);
								int recNsInt = Integer.parseInt(recNs,2);
								int recNrInt = recNsInt+1;
								String recNr = Integer.toBinaryString(recNrInt);
								
								while(recNs.length()<3)
								{
									recNs = "0" + recNs;
								}
									 
								while(recNr.length()<3)
								{
									recNr = "0" + recNr;
								}

								//reply to the client sending the message

								s_out[i].println(flag + address[i] + "0" + recNs + "0" + recNr); 
								System.out.println("Sending the following reply: " + flag + address[i] + "0" + recNs + "0" + recNr);
								//send the message forward to the recipient
								String binAddress = inputLine.substring(8,16); 
										
								String message = inputLine.substring(24, inputLine.length());
								sMessages[cnt] = flag + binAddress + "00000000" + message;
								cnt++;
        					}
        				}
                		// ==============================================================
        			}
        		}
        	}
        	
    		// ==============================================================
        	// insert codes here to send frames in the buffer       	
        	        		
        	// send I frame
        	int counter = 0;
        	
        	while(sMessages[counter]!=null)
        	{
        		int decimalAddress = Integer.parseInt(sMessages[counter].substring(8,16),2);
        		System.out.println(decimalAddress);
        		s_out[decimalAddress-1].println(sMessages[counter]);
			System.out.println("Sending: " + sMessages[counter]);
        		sMessages[counter]=null; //clear after we're done sending it
        		counter++;
        	}
    		// ==============================================================
			
		//
		// stop server automatically when
		// all clients disconnect
		//
		// no active clients
		//
			if (!bAlive && s_count > 0){
				System.out.println("All clients are disconnected - stopping");
				bListening = false;
			}
			
		}// end of while loop
		
		//
		// close all sockets
		//
		
		for (i=0;i<s_count;i++){
			client_sockets[i].close();
		}
        
        serverSocket.close();
        
    }// end main 
    
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
}// end of class PrimaryStation