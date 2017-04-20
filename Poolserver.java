
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Poolserver 
{
	public static String  HASHMAPFILE= "C:/Users/amogh/Desktop/Allocated IP Pool.txt";
	public static String CONFIGFILE = "C:/Users/amogh/Desktop/IPPool.txt";
	public ServerSocket serversocket = null;
	public Socket client = null;//get connection
	public int count;// get count of the connections
	public static List DefaultList = new ArrayList(); // list of IP during boot up 
	public static List AllocList = new ArrayList(); // list of IP during boot up 
	public ManagePool poolobj;
	
	/*
	 *  Main Program starts here
	 */
	public static void main(String[] args) throws Exception 
	{
		Poolserver poolserver = new Poolserver();
		poolserver.dowork();
	}
	
	/*
	 * Load IP Address allocated from file during start up
	 */
	public static boolean loadHashmap()throws Exception
	{
		File fp = new File(HASHMAPFILE);
		
		BufferedReader br = null;
		String sCurrentLine;	 
		br = new BufferedReader(new FileReader(fp.getAbsoluteFile()));
		System.out.println("Contents of " + fp.getAbsolutePath() + " are : " );
		while ((sCurrentLine = br.readLine()) != null)
			AllocList.add(sCurrentLine);
		
		for(int i = 0; i < AllocList.size();i++)
			System.out.println(AllocList.get(i));
		br.close();
		return true;
	}
	
	/*
	 * Load configuration information from file during start up
	 */
	public static boolean loadConfigFile()throws Exception
	{
		File fp = new File(CONFIGFILE);
		
		BufferedReader br = null;
		String sCurrentLine;	 
		br = new BufferedReader(new FileReader(fp.getAbsoluteFile()));
		System.out.println("Contents of " + fp.getAbsolutePath() + " are : " );
		while ((sCurrentLine = br.readLine()) != null)
			DefaultList.add(sCurrentLine);
		
		for(int i = 0; i < DefaultList.size();i++)
			System.out.println(DefaultList.get(i));
		br.close();
		return true;
	}
	
	/*
	 *  Real process begins
	 */
	public void dowork() throws Exception
	{
		try 
		{
			boolean ret = loadConfigFile();
			if(!ret)
			{
				System.out.println("Could not load configuration from file : " + CONFIGFILE);
				System.exit(1);
			}
			ret = loadHashmap();
			poolobj = new ManagePool(DefaultList,AllocList);
			serversocket = new ServerSocket(8888);
			while (true) 
			{
				client = serversocket.accept();
				count++;
				//ThreadHandler2 thread = new ThreadHandler2(client,DefaultList,AllocList);
				ThreadHandler2 thread = new ThreadHandler2(client,poolobj);
				System.out.println("*****************************************************");
				System.out.println("DCHP Server " + count +  " : connected to Pool Server, " + thread.toString());
				System.out.println("*****************************************************\n");
				thread.start();
			}
		} 
		catch (IOException e) 
		{
			e.getMessage();
			e.getStackTrace();
		}
	}

}

class ThreadHandler2 extends Thread 
{
	
	private Socket sock = null; 
	private ManagePool poolobj; 
	
	ThreadHandler2(Socket t1) 
	{
		this.sock = t1;
		
	}
	ThreadHandler2(Socket t1,ManagePool poolobj) 
	{
		this.sock = t1;
		this.poolobj = poolobj;
		
	}	
	
	/*
	 * Client thread execution
	 * @see java.lang.Thread#run()
	 */
	public void run()
	{
		try 
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			PrintWriter out = new PrintWriter(new BufferedOutputStream(sock.getOutputStream()));
			String readline;
			while ((readline = in.readLine()) != null) 
			{
				System.out.println("DHCP Server message recieved" );
				if(readline.equals("IPREQ"))
				{
					System.out.println("DHCP Server message IPREQ recieved" );
					
					String Ip = poolobj.getIP();
					
					System.out.println("offerinf IP : " + Ip );
					out.println(Ip);
					out.flush();
					if((readline = in.readLine()) != null)
					{
						if(readline.equals("ACK"))
							System.out.println("DHCP Server ack recieved");
						else if(readline.equals("NACK"))
						{

							System.out.println("DHCP Server negative ack recieved,release IP back to the Pool");

							// return back IP to the pool							
						
							boolean ret = poolobj.returnip(readline);/******/
							
							if(ret )
								System.out.println("Success : IP Address added back to the pool");
							else
								System.out.println("ERROR!!!! IP Address could not be added to the pool");

							
						}
						break;
					}
					else
					{
						System.out.println("Invalid Data!!!!");
						break;
					}
					
					
				}
				else if(readline.equals("IPREL"))
				{
					System.out.println("DHCP Server message IPREL recieved" );
					out.println("OK");
					out.flush();
					if((readline = in.readLine()) != null)// client sends the IP
					{
						// Function to Update IP pool
						System.out.println("DHCP Server released IP : " + readline);
						
						// return back IP to the pool							
						
						boolean ret = poolobj.returnip(readline);
						
						
						if(ret )
						{
							System.out.println("Success : IP Address added back to the pool");
							out.println("ACK");
							out.flush();
							break;
						}
						else
						{
							System.out.println("ERROR!!!! IP Address could not be added to the pool");
							out.println("NACK");
							out.flush();
							break;				
						}
					}
					else
					{
						System.out.println("Invalid Data!!!!");
						break;
					}
					
				}
				else if(readline.equals("OK"))
					break;
				
			}
			System.out.println("Closing the Client Connection");
			in.close();
			out.close();

			sock.close();
		} 
		catch (Exception e) {
			e.getMessage();
			e.printStackTrace();
		}

	}

}
