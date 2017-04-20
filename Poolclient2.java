
import java.io.*;
import java.net.*;

class Poolclient2 {
	static Socket echoSocket = null;
	static public String Ip;
	static boolean success = false;
	Poolclient2()
	{
	;	
	}
	public static boolean releaseIPtopool(String ip) {
		
		try {
			Ip = ip;
			echoSocket = new Socket("localhost", 8888);
			System.out.println("connected to server ");

			DataOutputStream os = new DataOutputStream(echoSocket.getOutputStream());
			BufferedReader is = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
			
			
			
			// Send IPREQ Message
			System.out.println("Sending... IPREL Message " );
			//Thread.sleep(5000);
			os.writeBytes("IPREL");
			os.write('\n');
			os.flush();	
			System.out.println("waiting for server to respond..");
			//Thread.sleep(5000);
			
			//Read Server Status
			String response = is.readLine();
			if(response.equals("OK"))
			{
				System.out.println("Releasing IP Address to Server : " + Ip);
				os.writeBytes(Ip);
				os.write('\n');
				os.flush();	
				if((response = is.readLine())!= null)
				{
					if(response.equals("ACK"))
					{
						success = true;
						System.out.println("ACK recieved : Success in releasing IP");
					}
					else if(response.equals("NACK"))
					{
						success = false;
						System.out.println("NACK recieved : Fail to release IP");
					}
				}
				else
				{
					System.out.println("Invalid data!!!! from server");	
				}
			}
			else
			{
				System.out.println("Invalid data!!!! from server");				
			}

			System.out.println("Closing the socket connection");
			is.close();
			os.close();
			echoSocket.close();
			
		} 
		catch (IllegalArgumentException e) 
		{
			System.err.println("IllegalArgumentException" + e.getMessage());
		} 
		catch (SecurityException e) 
		{
			System.err.println("SecurityException" + e.getMessage());
		} 
		catch (UnknownHostException e) 
		{
			System.err.println("Don't know about host: localhost");
		} 
		catch (Exception e) 
		{
			System.err.println("Couldn't get I/O for the connection to: localhost"+e.getMessage());
		}
		return success;
	}
}

