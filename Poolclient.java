import java.io.*;
import java.net.*;

class Poolclient {
	static Socket echoSocket = null;
	Poolclient()
	{
	;	
	}
	public static String getIPfrompool() {
		
		try {

			echoSocket = new Socket("127.0.0.1", 8888);
			System.out.println("connected to server ");

			DataOutputStream os = new DataOutputStream(echoSocket.getOutputStream());
			BufferedReader is = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
			
			// Send IPREQ Message
			System.out.println("Sending... IPREQ Message " );
			//Thread.sleep(5000);
			os.writeBytes("IPREQ");
			os.write('\n');
			os.flush();	
			System.out.println("waiting for server to respond... ");
			//Thread.sleep(5000);
			
			//Read Acknowledgement from server
			String Ip = is.readLine();
			System.out.println("IP received from Server : " + Ip);
			
			System.out.println("Sending out ACK message : " + Ip);
			os.writeBytes("ACK");
			os.write('\n');
			os.flush();	

			System.out.println("Closing the socket connection");
			is.close();
			os.close();
			echoSocket.close();
			return Ip;
			
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
		return null;
	}
}

