import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;


public class ManagePool {
// list - default/allocated/running
// fuction to intially load the running list 
// function to get IP
// function to return IP
	public static String HASHMAPFILE = "C:/Users/amogh/Desktop/Allocated IP Pool.txt";
	public static String  CONFIGFILE = "C:/Users/amogh/Desktop/IPPool.txt";
	
	public static List DefaultList = new ArrayList(); // list of IP during boot up 
	public static List AllocList = new ArrayList(); // list of IP during boot up 
	public static List ActiveList = new ArrayList(); // list of IP during boot up 
	
	ManagePool(List DefaultList,List AllocList)
	{
		this.DefaultList = DefaultList;
		this.AllocList = AllocList;
	}
	/*
	 * Fetch an IP address from IP pool, this has to be synchronized method
	 */
	public synchronized String getIP()
	{
		boolean done = false;
		boolean found = false;
		String ip=null;

		//debug
		//for(int i = 0; i < AllocList.size();i++)
		//	System.out.println("getIP " + AllocList.get(i));
		
		for(int i=0; i< DefaultList.size() && !done;i++)
		{

			if(!AllocList.contains(DefaultList.get(i)))
			{
				done = true;
				found = true;
				System.out.println("Found a Free IP : " + DefaultList.get(i));
				ip = (String) DefaultList.get(i);
				AllocList.add(ip);
				try
				{
					File fp = new File(HASHMAPFILE);
					if(fp.exists())
					{	
						FileWriter fw = new FileWriter(fp.getAbsoluteFile(),true);
						fw.write(ip);
						fw.flush();
						fw.write("\r\n");
						fw.flush();
						fw.close();
					}
					else
						System.out.println("ERROR !! updating to file " + HASHMAPFILE);
				
				}
				catch(Exception e)
				{
					System.out.println("ERROR !! Could not open the file " + HASHMAPFILE + e.getMessage());
				}
			}
				
		}
		if(found)
		{
			System.out.println("getIP returns : " + ip);
			return ip;
		}
		else
		{
			System.out.println("@@@@ No IP avaialble, increase the IP Pool");
			return null;
		}
	}
	
	/*
	 * release an IP address to IP pool, this has to be synchronized method
	 */
	public synchronized boolean returnip(String Ip)
	{	
		boolean found = false;
		boolean updatefile = false;
		//debug
		/*
		for(int i = 0; i < AllocList.size();i++)
			System.out.println("returnip " + AllocList.get(i));
		*/
		System.out.println("Releasing IP:" + Ip);
		
		if(AllocList.contains(Ip))
		{
			
			found = true;
			System.out.println("Found IP : " + Ip + " Size of IP Allocated pool before removing IP: " + Ip + " is " + AllocList.size());
			if(AllocList.remove(Ip))
			{
				updatefile = true;
				System.out.println("Size of IP Allocated pool after removing IP : "+ Ip + " is " + AllocList.size() );
				
			}
			else
				System.out.println("Could nt remove the IP from Allocated Pool");//Handle when ip is not removed from the allocated list
		}			
		if(updatefile)
		{
			try{
				
				System.out.println("updating allocated list - removing ip" );

				File fp = new File(HASHMAPFILE);
				FileWriter fw = new FileWriter(fp.getAbsoluteFile());
				for(int i = 0; i < AllocList.size();i++)
				{
					
					if(fp.exists())
					{	
						
						fw.write((String)AllocList.get(i));
						fw.flush();
						fw.write("\r\n");
						fw.flush();
					}
					else
						System.out.println("ERROR !! updating to file " + HASHMAPFILE);
					
				}
				fw.close();
			}
			catch (Exception e)
			{
				System.out.println("ERROR !! Could not open the file " + HASHMAPFILE + e.getMessage());	
			}
		}
		if(found) 
			return true;
		else
		{
			System.out.println("@@@@ IP was not Allocated, Could not release the IP");
			return false;
		}
	}
}
