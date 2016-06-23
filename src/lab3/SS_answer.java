package lab3;
import java.net.*;
import java.io.*;

public class SS_answer {
	
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
		String senderAddr = "00000000"; 
		int ns = 0; // send sequence number
		int nr = 0; //receive sequence number
		int maxFrames = 10; //size of sliding window
		int currentFrame = 0; //counter variable for the sliding window
		
		String[] messageQueue = new String[100];
		for(int i = 0; i<messageQueue.length; i++)
		{
			messageQueue[i] = null;
		}
		
		String[] receivedMsgs = new String[100];
	
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
				
				responseLine = is.readLine();
				response = responseLine.substring(16, 24);

				
				// recv SNRM msg
				if(response.equals("11000001") || response.equals("11001001")) {
					//===========================================================
					// insert codes here to send the UA msg					
					//===========================================================
					
					String str = flag + "00000000" + "11000110";
					os.println(str);
					
					System.out.println("sent UA msg");
				}

				
				// main loop; recv and send data msgs
				while (true) {
					boolean finMsg=false;
					int msgCntr = 0;
					while (!finMsg) {
						responseLine = is.readLine();
						if(responseLine.substring(responseLine.length()-1).equals("-"))
						{
							finMsg=true;
						}
						response = responseLine.substring(16, 24);
						
						System.out.println("recv msg -- control " + response);				
						
						// recv  msg
						if(response.substring(0,5).equals("10001")) {
							
							// enter data msg using keyboard
							System.out.println("Would you like to send a message? (y/n)");
							answer = in.readLine();						
							
							if(answer.toLowerCase().equals("y") || answer.toLowerCase().equals("yes")) {
								System.out.println("Please enter the destination address using 8-bits binary string (e.g. 00000001):");
								address = in.readLine();
								
								System.out.println("Please enter the message to send?");
								answer = in.readLine();
								
								//===========================================================
								// insert codes here to send an I msg;
								
								 byte[] byteArray = answer.getBytes();
								 StringBuilder binaryConversion = new StringBuilder();
								 for (byte b : byteArray)
								 {
								    int val = b;
								    for (int i = 0; i < 8; i++)
								    {
								       binaryConversion.append((val & 128) == 0 ? 0 : 1);
								       val <<= 1;
								    }
								 }
								
								 answer = binaryConversion.toString();
				
								 String substr;
								 String str;
								 
								 ns = 0;
								 nr = 0;

								 String binNs = Integer.toBinaryString(ns);
								 String binNr = Integer.toBinaryString(nr);
									
								int j = 0;
								 
								 if(answer.length()>64)
								 {
									 for(; answer.length()>64; j++)
									 {
										binNs = Integer.toBinaryString(ns);
										binNr = Integer.toBinaryString(nr);
									 	substr = answer.substring(0,64);
										answer = answer.substring(64,answer.length());

										while(binNs.length()<3)
										{
											 binNs = "0" + binNs;
										}
										 
										while(binNr.length()<3)
										{
											 binNr = "0" + binNr;
										}
										 
										str = flag + address + "0" + binNs + "1" + binNr + substr;
										messageQueue[j] = str;
										nr++;
										ns++;
									 }
									 
									binNs = Integer.toBinaryString(ns);
									binNr = Integer.toBinaryString(nr);

									while(binNs.length()<3)
									{
										 binNs = "0" + binNs;
									}
									 
									while(binNr.length()<3)
									{
										 binNr = "0" + binNr;
									}								

									str = flag + address + "0" + binNs + "1" + binNr + answer + "-";
									messageQueue[j] = str;
									j++;
									 
								 }
								 else
								 {
									 str = flag + address + "00000000" + answer + "-";
									 messageQueue[j] = str;
									 j++;
								 }
								 
								 String resp = null;
								 
								 ns = 0;
								 
								 for(int i = 0; i<j; i++)
								 {
									 os.println(messageQueue[i]);
									 currentFrame++;
									 System.out.println("Trying to send this message: " + messageQueue[i] + " from frame " + i + " in our sliding window.");
									 if(currentFrame < maxFrames){
										 try
										 {
											 resp = is.readLine();
											 System.out.println("Response received: " + resp);
										 } catch (InterruptedIOException e)
										 {}
									 }
									 else{
										System.out.println("Maximum number of frames reached. Wating on receiver.");
										resp = null;
									 }
									 
									 if(resp != null)
									 {
										 if(Integer.parseInt(resp.substring(21,24),2)==(ns+1))
										 {
											//message received
											currentFrame--;
											System.out.println("Message received by Primary");
											ns++;
											messageQueue[i] = null;
										 }
										 else
										 {
											 //must resend message
											 System.out.println("Message NOT received, NR: "+ resp.substring(21,24));
											 i--;
										 }
									 }
									 else
									 {
										 //must resend message
										 System.out.println("Message NOT received");
										 i--;
									 }
									 
								 }//end of message queue
								finMsg=true;
								//===========================================================
							
							}				
							else {
								//===========================================================
								// insert codes here to send <RR,*,F>
								
								String str = flag + "00000000" + "10000000";
								os.println(str);
								//===========================================================
							}
						}
						
						// recv an I frame
						if(response.substring(0,1).equals("0")) {
							String data = responseLine.substring(24, responseLine.length());
							System.out.println("");
							System.out.println("Received data: " + data);						
							
							senderAddr = responseLine.substring(8,16);
							String msg = decodeBinary(data);
							
							receivedMsgs[msgCntr] = msg;					
							msgCntr++;
							
							nr = Integer.parseInt(response.substring(1,4), 2) + 1;
						}
					}
					String recMsg = "";	
					for(int k=0; k<msgCntr; k++)	
					{
						recMsg = recMsg + receivedMsgs[k];
					}

					System.out.println("Received message from: " + senderAddr + " "  + recMsg);

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
	
	public static String decodeBinary(String bin)
	{

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
	
	

}// end of class SecondaryStation