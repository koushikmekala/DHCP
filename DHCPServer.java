
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;



public class DHCPServer 
{
	private static final int MAX_BUFFER_SIZE = 1024; // 1024 bytes
	private static int listenPort = 67;
	private static int clientPort = 68;
	private static DatagramSocket socket = null;

	//DHCPServer Configuration information
	private static byte[] subnet = new byte[4]; 
	private static byte[] subnetMask = new byte[]{(byte) 255,(byte) 255,(byte)255,0}; 
	private static byte[] router = new byte[]{(byte) 10,(byte) 0,(byte)0,(byte)9}; //  this should be server ip [my ip]
	private static byte[] dns = new byte[]{(byte) 10, (byte)0,(byte)0,(byte)1};
	
	//Table Data
	private static byte[][] ipTable = new byte[254][4]; //0,255 reserved(1-254 assignable)
	private static byte[][] macTable = new byte[254][6];
	private static byte[][] hostNameTable = new byte[254][];
	private static long[] leaseStartTable = new long[254];
	private static long[] leaseTimeTable = new long[254];
	private static int numAssigned = 0;
	private static long startTime;
	

	private static long defLeaseTime = 300; //change based on need
	private static String NL;
	private static boolean AccessFile = false;
	public DHCPServer() {
		NL = System.getProperty("line.separator");
		//Load from the configuration file
		for (int i=0; i < ipTable.length; i++) {
			ipTable[i][0] = 0;
			ipTable[i][1] = 0;
			ipTable[i][2] = 0;
			ipTable[i][3] = 0;
			hostNameTable[i] = new String("").getBytes();
		} 
		FileReader input;
		try {
				log("log.txt", "DHCPServer : Initialize server using Configuration File  " );
				input = new FileReader("C:/Users/amogh/Desktop/DHCP Server Storage.txt");
				BufferedReader bufRead = new BufferedReader(input);
				String myLine = null;
				int i=0 ;
				while ( (myLine = bufRead.readLine()) != null && !myLine.isEmpty())
				{
					String token[] = myLine.split("\t");
					ipTable[i] = DHCPUtility.strToIP(token[0]);
					String macAddress = token[1];
					String[] macAddressParts = macAddress.split("-");

					// convert hex string to byte values
					byte[] macAddressBytes = new byte[6];
					for(int h=0; h<6; h++)
					{
						Integer hex = Integer.parseInt(macAddressParts[h], 16);
						macAddressBytes[h] = hex.byteValue();
					}
					macTable[i]=macAddressBytes;
					hostNameTable[i] = DHCPUtility.stringToBytes(token[2]);

					SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
					Date startDate = new Date();
					Date endDate = new Date();
					try 
					{
						startDate = sdf.parse(token[3]);
						endDate =sdf.parse(token[4]);
					} 
					catch (Exception e) 
					{
					log("log.txt", "DHCPServer :: Exception Caught in DHCPServer() :  " +e.getMessage() );
					}
				 
					leaseStartTable[i] = startDate.getTime();
					leaseTimeTable[i] =  endDate.getTime();
					i++;
				 }
			
				bufRead.close();
		} 
		catch (Exception e) 
		{
			log("log.txt", "DHCPServer :: Exception Caught in DHCPServer() :  " +e.getMessage() );
		} 
		//set server start time
		startTime = System.currentTimeMillis();		
		
		//calculate netmask address
		log("log.txt", "Router IP: " + DHCPUtility.printIP(router));
		log("log.txt", "SubnetMask: " +  DHCPUtility.printIP(subnetMask));
		System.out.println("Router IP: " + DHCPUtility.printIP(router));
		System.out.println("SubnetMask: " +  DHCPUtility.printIP(subnetMask));
		subnet[0] = (byte) (subnetMask[0] & router[0]);
		subnet[1] = (byte) (subnetMask[1] & router[1]);
		subnet[2] = (byte) (subnetMask[2] & router[2]);
		subnet[3] = (byte) (subnetMask[3] & router[3]);
		System.out.println("sn & r = network addr: " + DHCPUtility.printIP(subnet));
		
		try 
		{
			socket = new DatagramSocket(listenPort);
			log("log.txt", "Listening on port " + listenPort + "..." );
			System.out.println("Listening on port " + listenPort + "...");
		} 
		catch (SocketException e) 
		{
			log("log.txt", "DHCPServer :: Exception Caught in DHCPServer() :  " +e.getMessage() );
		} // ipaddress? throws socket exception
		
	
		log("log.txt", "DHCPServer: init complete server started" );
	}

	/*
	 *  Write the data into persistent storage 
	 */
	public static void updateAssignedIp() {
		try 
		{				
			log("log.txt", "DHCPServer:updateAssignedIp() Updating Assigned IP to persistent storage " );

			while(!AccessFile)
				
			{
				AccessFile = true;
				log("log.txt", "DHCPServer:updateAssignedIp() Got access to file DHCP Server Storage.txt " );
				BufferedWriter outputStream = new BufferedWriter(new FileWriter("C:/Users/Amogh/Desktop/DHCP Server Storage.txt",false));
				for (int i=0; i < ipTable.length ; i++) 
				{
					if(!(ipTable[i][0] == 0 && ipTable[i][1] == 0 && ipTable[i][2] == 0 && ipTable[i][3] == 0))
					{
						//System.out.println("***********" + DHCPUtility.printIP(ipTable[i])+"\t"+macTable[i] != null ?DHCPUtility.printMAC(macTable[i]):0+"\t"+new String(hostNameTable[i])+"\t"+leaseStartTable != null ?new Date(leaseStartTable[i]).toString() : 0+"\t"+leaseStartTable != null ?(new Date(leaseStartTable[i] + 1000*leaseTimeTable[i]).toString()):0 + NL );
						//System.out.println("************"+DHCPUtility.printIP(ipTable[i])+"\t"+ macTable[i] != null?DHCPUtility.printMAC(macTable[i]):0+"\t"+new String(hostNameTable[i])+"\t"+new Date(leaseStartTable[i]).toString()+"\t"+(new Date(leaseStartTable[i] + 1000*leaseTimeTable[i]).toString()) + NL );
						System.out.println(DHCPUtility.printIP(ipTable[i])+"  "+DHCPUtility.printMAC(macTable[i])+"  "+new String(hostNameTable[i])+"  "+new Date(leaseStartTable[i]).toString()+"  "+(new Date(leaseStartTable[i] + 1000*leaseTimeTable[i]).toString()) + NL );
						log("log.txt",DHCPUtility.printIP(ipTable[i])+"  "+DHCPUtility.printMAC(macTable[i])+"  "+new String(hostNameTable[i])+"  "+new Date(leaseStartTable[i]).toString()+"  "+(new Date(leaseStartTable[i] + 1000*leaseTimeTable[i]).toString())  );

						outputStream.write(DHCPUtility.printIP(ipTable[i])+"\t"+DHCPUtility.printMAC(macTable[i])+"\t"+new String(hostNameTable[i])+"\t"+new Date(leaseStartTable[i]).toString()+"\t"+(new Date(leaseStartTable[i] + 1000*leaseTimeTable[i]).toString()) + NL );
						outputStream.flush();
						//outputStream.write(DHCPUtility.printIP(ipTable[i])+"\t"+ macTable[i] != null?DHCPUtility.printMAC(macTable[i]):0+"\t"+new String(hostNameTable[i])+"\t"+new Date(leaseStartTable[i]).toString()+"\t"+(new Date(leaseStartTable[i] + 1000*leaseTimeTable[i]).toString()) + NL );
						//outputStream.write(DHCPUtility.printIP(ipTable[i])+"\t"+macTable[i] != null ?DHCPUtility.printMAC(macTable[i]):0+"\t"+new String(hostNameTable[i])+"\t"+leaseStartTable != null ?new Date(leaseStartTable[i]).toString() : 0+"\t"+leaseStartTable != null ?(new Date(leaseStartTable[i] + 1000*leaseTimeTable[i]).toString()):0 + NL );						outputStream.flush();	
					}
				}
				outputStream.close();
				log("log.txt", "DHCPServer:updateAssignedIp() Release Access to file DHCP Server Storage.txt " );
			}
			AccessFile = false;
		} 
		catch (IOException e) 
		{
			log("log.txt", "DHCPServer:updateAssignedIp() Exception caught while updating into persistant storage" +e.getMessage() );
		} 
	}
	
	/*  receive packet from the client
	*/


	public static DatagramPacket receivePacket() 
	{
		byte[] payload = new byte[MAX_BUFFER_SIZE];
		int length = MAX_BUFFER_SIZE;
		DatagramPacket p = new DatagramPacket(payload, length);
		
		try 
		{
			socket.receive(p);
		} 
		catch (IOException e) 
		{
			log("log.txt", "DHCPServer:receivePacket() Exception caught " +e.getMessage());

		} // throws i/o exception
		log("log.txt", "DHCPServer:receivePacket() Connection established from " + p.getPort()+ p.getAddress() );
		System.out.println("Connection established from " + p.getPort()+ p.getAddress());
		return p;
	}
	
	public static void sendPacket(String clientIP, byte[] payload) 
	{
		assert(payload.length <= MAX_BUFFER_SIZE);
	
		try 
		{
			DatagramPacket p = new DatagramPacket(payload, payload.length, InetAddress.getByName(clientIP), clientPort);
			log("log.txt", "DHCPServer:sendPacket() Sending data: to " + p.getPort()+ p.getAddress() );
			System.out.println("Sending data: " + "to " + p.getPort() + p.getAddress());
			socket.send(p);
		} 
		catch (Exception e) {
			log("log.txt", "DHCPServer:receivePacket() Exception caught " +e.getMessage() );
		}
	}
	/*
	*Broadcasting the packet to the local subnet
	*
	*/
	public static void broadcastPacket(byte[] payload) {
		assert(payload.length <= MAX_BUFFER_SIZE);
	
		try {
			String broadcastIP = "255.255.255.255";
			DatagramPacket p = new DatagramPacket(payload, payload.length, InetAddress.getByName(broadcastIP), clientPort);
		    //System.out.println("Broadcasting data: " + Arrays.toString(p.getData()));
			log("log.txt", "DHCPServer:broadcastPacket() broadcast data to " + broadcastIP );
			socket.send(p);
		} 
		catch (Exception e) {
			log("log.txt", "DHCPServer:broadcastPacket() Exception caught " +e.getMessage());
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new DHCPServer();
		//timer to release ips
		loadTimer();
		//server is always listening
		boolean listening = true;
		while (listening) 
		{
			DatagramPacket packet = receivePacket();
			//ensure packet is on dhcpClient port 
			if (packet.getPort() == clientPort) {
				process(packet.getData());
			} 
			else {
				log("log.txt", "DHCPServer:main() Packet Recieved from invalid port " +packet.getPort() );
				//packet is dropped (silently ignored)
			}
		}
	}

	/**
	 * 
	 */
	private static void loadTimer() {
		TimerTask timerTask = new DHCPTimer();
		// running timer task as daemon thread
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(timerTask, 0, 1*60 * 1000); // Timer will be called every 2 mins
	}

	private static void process(byte[] msg) {
		DHCPMessage request = new DHCPMessage(msg);
		byte msgType = request.getOptions().getOptionData(DHCPOptions.DHCPMESSAGETYPE)[0]; 
		log("log.txt", "DHCPServer:process() processing the Recieved Packet  " );

		if (request.getOp() == DHCPMessage.DHCPREQUEST) {
			if (msgType == DHCPOptions.DHCPDISCOVER) {
				System.out.println("DHCP Discover Message Received");
				log("log.txt", "DHCPServer:process() DHCP Discover Message Received" + request.toString());
				byte[] offer = createOfferReply(request);
				System.out.println("Broadcasting Offer Reply");
				log("log.txt", "DHCPServer:process() Broadcasting Offer Reply" );
				broadcastPacket(offer);
			} else if (msgType == DHCPOptions.DHCPREQUEST) {
				System.out.println("DHCP Request Message Received");
				log("log.txt", "DHCPServer:process() DHCP Request Message Received"  + request.toString());
				byte[] ack = createRequestReply(request);
				System.out.println("Sending ACK reply to " + request.printCHAddr());
				log("log.txt", "DHCPServer:process() Sending ACK reply to " + request.printCHAddr());

				byte[] ServerIp = request.getOptions().getOptionData(DHCPOptions.DHCPSERVERIDENTIFIER);
				byte[] RequestedIp = request.getOptions().getOptionData(DHCPOptions.DHCPREQUESTIP);
				boolean valid = false;
				//compare router ip with Server Identifier field, if matches update file
				
					if((null != ServerIp) && (ServerIp[0] == router[0] && ServerIp[1] == router[1] &&
							ServerIp[2] == router[2] && ServerIp[3] == router[3]))
					{
						log("log.txt", "DHCPServer:process() DHCPSERVERIDENTIFIER matches to the router IP for  " + request.printCHAddr() + " IP Address  " + DHCPUtility.printIP(ServerIp));
						valid= true;
						updateAssignedIp();
					}
					//if it does not match 
					//then there are 2 cases
					// case 1: OPTION Server Identifier - is not present
					// In this case, we have to check the OPTION "requested IP address" field and compare it with entries in IP Table
					// if match is found then we have to update the entry into file else remove the entry from table and release the ip back to pool
					// case 2: OPTION Server Identifier is present but does not match to the router value
					// even then remove the entry from ip table
					else if(RequestedIp != null)
					{
						log("log.txt", "DHCPServer:process() DHCPSERVERIDENTIFIER doesnt matches to the router IP , Check for DHCPREQUESTIP :" + request.printCHAddr() + " IP Address  "+DHCPUtility.printIP(RequestedIp));
						valid = removeIPFromTableUsingMAC(request,RequestedIp);
					}
					else 
					{
						log("log.txt", "DHCPServer:process() DHCPSERVERIDENTIFIER and DHCPREQUESTIP options not present:" + request.printCHAddr());
						valid = true;
						updateAssignedIp();
					}
					if(valid)	
					{
							broadcastPacket(ack);
						}

			} else if (msgType == DHCPOptions.DHCPINFORM) {
					System.out.println("DHCP Inform Message Received");
					log("log.txt", "DHCPServer:process() DHCP Inform Message Received"  + request.toString());
					byte[] ack = createInformReply(request); // AMOGH
			} else if (msgType == DHCPOptions.DHCPDECLINE) {
				//client arp tested and ip is in use..
				//possibly delete record of client?
				System.out.println("DHCP Decline Message Received");
				log("log.txt", "DHCPServer:process() DHCP Decline Message Received"  + request.toString());
			} else if (msgType == DHCPOptions.DHCPRELEASE) {
				//client relinquishing lease early
				//possibly delete binding record of client?
				releaseIPFromTableUsingIP(request);
				System.out.println("DHCP Release Message Received");
				log("log.txt", "DHCPServer:process() DHCP Release Message Received"  + request.toString());
			} else {
				System.out.println("Unknown DHCP Message Type: " + msgType);
				log("log.txt", "DHCPServer:process() Unknown DHCP Message Type:" + msgType);
			}
		} else {
			System.out.println("DHCP Reply Message received, where DHCP Request was expected!");
			log("log.txt", "DHCPServer:process() DHCP Reply Message received, where DHCP Request was expected!");
		}
	}

	/**
	 * @param request
	 */
	private static void releaseIPFromTableUsingIP(DHCPMessage request) {
		byte[] CIp = request.getCIAddr();
		
		System.out.println("Calling releaseIP from releaseIPFromTableUsingIP()");
		releaseIP(CIp);
	}

	/**
	 * @param request
	 * @param CIp
	 */
	private static void releaseIP(byte[] CIp) {
		if (!(CIp[0] == 0 && CIp[1] == 0 && CIp[2] == 0 && CIp[3] == 0))
			for (int i = 0; i < ipTable.length; i++) 
			{
				if ((CIp[0] == ipTable[i][0] && CIp[1] == ipTable[i][1]
						&& CIp[2] == ipTable[i][2] && CIp[3] == ipTable[i][3])) 
				{
					log("log.txt","DHCPServer:releaseIPFromTableUsingIP() releasing IP  address  "+ DHCPUtility.printIP(CIp) + " to pool server");
					Poolclient2 cl2 = new Poolclient2();
					boolean flag = cl2.releaseIPtopool(DHCPUtility.printIP(CIp));
					if (flag) {
						ipTable[i][0] = 0;
						ipTable[i][1] = 0;
						ipTable[i][2] = 0;
						ipTable[i][3] = 0;
						macTable[i][0] = 0;
						macTable[i][1] = 0;
						macTable[i][2] = 0;
						macTable[i][3] = 0;
						macTable[i][4] = 0;
						macTable[i][5] = 0;
						hostNameTable[i] = null;
						leaseStartTable[i] = 0;
						leaseTimeTable[i] = 0;
						// have to decrease numassigned as well
						numAssigned--;
						updateAssignedIp();
					}
				break;
				}

			}
	}
	
	
	
	/**
	 * @param request
	 */
	private static boolean removeIPFromTableUsingMAC(DHCPMessage request,byte[] RequestedIp) {
		boolean retvalue = false;
		boolean match = false;
		
		byte[] ClientMACAddress =request.getCHAddr();
		for (int i=0; i < ipTable.length; i++) 
		{
			if ((macTable[i][0] == ClientMACAddress[0])
					&& (macTable[i][1] == ClientMACAddress[1])
					&& (macTable[i][2] == ClientMACAddress[2])
					&& (macTable[i][3] == ClientMACAddress[3])
					&& (macTable[i][4] == ClientMACAddress[4])
					&& (macTable[i][5] == ClientMACAddress[5]))
			{
				match= true;
				log("log.txt","DHCPSERVER : removeIPFromTableUsingMAC :"+DHCPUtility.printIP(ipTable[i])+"  "+DHCPUtility.printMAC(macTable[i])+"  "+new String(hostNameTable[i])+"  "+new Date(leaseStartTable[i]).toString()+"  "+(new Date(leaseStartTable[i] + 1000*leaseTimeTable[i]).toString())  );

				if((null != RequestedIp) && ((RequestedIp[0] == ipTable[i][0] && RequestedIp[1] == ipTable[i][1] &&
						RequestedIp[2] == ipTable[i][2] && RequestedIp[3] == ipTable[i][3])))
				{
					//update entry to file
					retvalue = true;
					updateAssignedIp();
				}
				else 
				{
					log("log.txt", "DHCPServer: releasing IP "+DHCPUtility.printIP(ipTable[i]) + " of MAC address  " + request.printCHAddr() + " to pool server");
					Poolclient2 cl2 = new Poolclient2();
					boolean flag = cl2.releaseIPtopool(DHCPUtility.printIP(ipTable[i]));
					
					if(flag)
					{
						  ipTable[i][0] = 0;
							ipTable[i][1] = 0;
							ipTable[i][2] = 0;
							ipTable[i][3] = 0;
							macTable[i][0] = 0;
							macTable[i][1] = 0;
							macTable[i][2] = 0;
							macTable[i][3] = 0;
							macTable[i][4] = 0;
							macTable[i][5] = 0;
							hostNameTable[i]= null;
							leaseStartTable[i]= 0;
							leaseTimeTable[i]=0;
							numAssigned--;
							updateAssignedIp();
					}	
				}
				break;
			}
		}
		if(!match){
			log("log.txt", "DHCPServer: No IP associated with MAC address "+ request.printCHAddr() );
		}
		return retvalue;
	}

	/**
	 * respond to DHCPClient who already has externally configured network address
	 * @param request
	 * @return
	 */
	private static byte[] createInformReply(DHCPMessage inform) {
		// compare request ip, to transaction offer ip, ensure it is still
		// unique
		int row = -1;
		for (int i=0; i < macTable.length; i++) {
			if (DHCPUtility.isEqual(inform.getCHAddr(), macTable[i])) {
				row = i;
			}
	    }	
		
		if (row >=0) { //transaction exists
			System.out.println("clients ip in table is: " + DHCPUtility.printIP(ipTable[row]));
			System.out.println(inform.getXid() + " " + DHCPUtility.printMAC(inform.getCHAddr())+ " " + DHCPUtility.printMAC(macTable[row]) + " " + DHCPUtility.printIP(ipTable[row]));

		} else { //no transaction
			System.out.println("client not encountered before");
		}	
		
		/*The servers SHOULD
		   unicast the DHCPACK reply to the address given in the 'ciaddr' field
		   of the DHCPINFORM message.
		 */
		
		//possibly reply to clients parameter requests
		//otherwise just sending basic infromation: router, subnetmask, dns 
		System.out.println("sending client information..");

		DHCPMessage ackMsg = new DHCPMessage(inform.externalize());
		ackMsg.setOp(DHCPMessage.DHCPREPLY);
		
		DHCPOptions ackOptions = new DHCPOptions();
		ackOptions.setOptionData(DHCPOptions.DHCPMESSAGETYPE, new byte[]{ DHCPOptions.DHCPACK});
		try {
			ackOptions.setOptionData(DHCPOptions.DHCPSERVERIDENTIFIER, InetAddress.getLocalHost().getAddress());
		} catch (Exception e) {
			log("log.txt", "DHCPServer:createInformReply exception caught "+e.getMessage() );
		}
		ackOptions.setOptionData(DHCPOptions.DHCPROUTER, router);
		ackOptions.setOptionData(DHCPOptions.DHCPSUBNETMASK, subnetMask);
		ackOptions.setOptionData(DHCPOptions.DHCPDNS , dns);

		ackMsg.setOptions(ackOptions);
		return ackMsg.externalize();	
	}

	private static byte[] createRequestReply(DHCPMessage request) {

		// compare request ip, to transaction offer ip, ensure it is still
		// unique
		int row = -1;
		for (int i=0; i < macTable.length; i++) {
			if (DHCPUtility.isEqual(request.getCHAddr(), macTable[i])) {
				row = i;
			}
	    }	
		
		//assert(row >= 0) : "mac address not matching";
		if (row >=0) { //transaction exists
			//use requesting clients hostname
			if (request.getOptions().getOptionData(DHCPOptions.DHCPHOSTNAME) != null) {
				hostNameTable[row] = request.getOptions().getOptionData(DHCPOptions.DHCPHOSTNAME);
			} else {
				hostNameTable[row] = new String("").getBytes();
			}
			leaseStartTable[row] = System.currentTimeMillis();
			leaseTimeTable[row] =  defLeaseTime;
			// ip is now unique
			// offer ip to requesting client

			DHCPMessage ackMsg = new DHCPMessage(request.externalize());
			System.out.println(request.getXid() + " " + DHCPUtility.printMAC(request.getCHAddr())+ " " + DHCPUtility.printMAC(macTable[row]) + " " + DHCPUtility.printIP(ipTable[row]));
			ackMsg.setOp(DHCPMessage.DHCPREPLY);
			ackMsg.setYIAddr(ipTable[row]);
			DHCPOptions ackOptions = new DHCPOptions();
			ackOptions.setOptionData(DHCPOptions.DHCPMESSAGETYPE, new byte[]{ DHCPOptions.DHCPACK});
			try {
				ackOptions.setOptionData(DHCPOptions.DHCPSERVERIDENTIFIER, InetAddress.getLocalHost().getAddress());
			} catch (Exception e) {
				log("log.txt", "DHCPServer:createRequestReply exception caught "+e.getMessage() );
			}
			ackOptions.setOptionData(DHCPOptions.DHCPROUTER, router);
			ackOptions.setOptionData(DHCPOptions.DHCPSUBNETMASK, subnetMask);
			ackOptions.setOptionData(DHCPOptions.DHCPLEASETIME, DHCPUtility.bits2Bytes(DHCPUtility.num2BitSet(defLeaseTime),4));
			ackOptions.setOptionData(DHCPOptions.DHCPDNS , dns);

			ackMsg.setOptions(ackOptions);
			return ackMsg.externalize();
		} else { //no transaction - optionally send a dhcpnak otherwise just ignore packet
			DHCPMessage nakMsg = new DHCPMessage(request.externalize());
			nakMsg.setOp(DHCPMessage.DHCPREPLY);
			DHCPOptions ackOptions = new DHCPOptions();
			ackOptions.setOptionData(DHCPOptions.DHCPMESSAGETYPE, new byte[]{ DHCPOptions.DHCPNAK});
			try {
				ackOptions.setOptionData(DHCPOptions.DHCPSERVERIDENTIFIER, InetAddress.getLocalHost().getAddress());
			} catch (Exception e) {
				log("log.txt", "DHCPServer:createRequestReply exception caught "+e.getMessage() );
			}
			/*nakOptions.setOptionData(DHCPOptions.DHCPROUTER, router);
			nakOptions.setOptionData(DHCPOptions.DHCPSUBNETMASK, subnetMask);
			nakOptions.setOptionData(DHCPOptions.DHCPLEASETIME, DHCPUtility.inttobytes((int)defLeaseTime));
			nakOptions.setOptionData(DHCPOptions.DHCPDNS , dns);*/
		

			nakMsg.setOptions(ackOptions);
			return nakMsg.externalize();
		}	
	}

	private static byte[] createOfferReply(DHCPMessage discover) {
		byte[] ip; //offer ip
		
		
		
		// compare request ip, to transaction offer ip, ensure it is still
		// unique
		int row = -1;
		if (!DHCPUtility.isEqual(discover.getCHAddr(),new byte[]{0,0,0,0,0,0})){ //ensure mac validity
			for (int i=0; i < macTable.length; i++) {
				if (DHCPUtility.isEqual(discover.getCHAddr(), macTable[i])) {
					row = i;
				}
			}	
		}
		
		
		if (row >=0) { //transaction exists
			ip = ipTable[row];
		} else {	
			//use requesting clients hostname
			if (discover.getOptions().getOptionData(DHCPOptions.DHCPHOSTNAME) != null) {
				try {
				hostNameTable[numAssigned] = discover.getOptions().getOptionData(DHCPOptions.DHCPHOSTNAME);
				}catch (Exception e ){
					log("log.txt", "DHCPServer:createOfferReply exception caught in hostNameTable[numAssigned] "+e.getMessage() );
				}
			} else {
				hostNameTable[numAssigned] = new String("").getBytes();
			}
			macTable[numAssigned] = discover.getCHAddr();
			ip =assignIP(discover.getGIAddr());
			addAssignedIP(ip);
		}
		

		
		//ip is now unique
		//offer ip to requesting client
		
		DHCPMessage offerMsg = new DHCPMessage(discover.externalize());
		offerMsg.setOp(DHCPMessage.BOOTREPLY);
		offerMsg.setYIAddr(ip);
		DHCPOptions offerOptions = new DHCPOptions();
		offerOptions.setOptionData(DHCPOptions.DHCPMESSAGETYPE, new byte[]{ DHCPOptions.DHCPOFFER});
		try {
			offerOptions.setOptionData(DHCPOptions.DHCPSERVERIDENTIFIER, InetAddress.getLocalHost().getAddress());
		} catch (Exception e) {
			log("log.txt", "DHCPServer:createOfferReply exception caught "+e.getMessage() );
		}
		offerOptions.setOptionData(DHCPOptions.DHCPROUTER, router);
		offerOptions.setOptionData(DHCPOptions.DHCPSUBNETMASK, subnetMask);
		offerOptions.setOptionData(DHCPOptions.DHCPLEASETIME, DHCPUtility.bits2Bytes(DHCPUtility.num2BitSet(defLeaseTime),4));
		offerOptions.setOptionData(DHCPOptions.DHCPRENEWT1TIME, DHCPUtility.bits2Bytes(DHCPUtility.num2BitSet((long) (defLeaseTime*.5)),4));
		offerOptions.setOptionData(DHCPOptions.DHCPRELEASET2TIME,DHCPUtility.bits2Bytes(DHCPUtility.num2BitSet((long) (defLeaseTime*.75)),4));
		
		offerMsg.setOptions(offerOptions);
		return offerMsg.externalize();
	}
	
	//generate a unique ip appropriate to gateway interface subnet address
	private static byte[] assignIP(byte[] gIAddr) {
		byte[] ip = new byte[4];
		
		//DHCPMessage originated from same subnet
		if (DHCPUtility.printIP(gIAddr).compareTo("0.0.0.0") == 0) {
			System.out.println("DHCPMessage originated from same subnet");
			
			// have to call ip pool client here
			log("log.txt", "DHCPServer:assignIP  Getting Ip from Pool, Start" );
			Poolclient testpool = new Poolclient();
			String IP = testpool.getIPfrompool();
			log("log.txt", "DHCPServer:assignIP  Getting Ip from Pool, End" );
			ip = DHCPUtility.strToIP(IP);

			//unique ip to assign is:
			log("log.txt", "DHCPServer:assignIP  Assigning ip:" + DHCPUtility.printIP(ip) );
			
		} else { //DHCPMessage originated from gIAddr subnet
			assert(false) : "Asserted in DHCPServer:assignIP "; 
			
		}
		return ip;
	}


	//verify if ip is unique in the ip table
	private static boolean isUnique(byte[] ip){
		boolean done = false;
		boolean isUnique = true;
		for (int i=0; i < ipTable.length && isUnique && !done ; i++) {
			if (ip[0] == ipTable[i][0] && ip[1] == ipTable[i][1] &&
				ip[2] == ipTable[i][2] && ip[3] == ipTable[i][3]) {
					isUnique = false;
			} else if(ipTable[i][0] == 0 && ipTable[i][1] == 0 &&
				      ipTable[i][2] == 0 && ipTable[i][3] == 0) {
					  	done = true;
			}
		}
		return isUnique;
	}
	
	//add ip to list of assigned ips
	private static void addAssignedIP(byte[] assignedIP) {
		if (isUnique(assignedIP)) { //no double entries
			boolean done = false;
			for (int i=0; i < ipTable.length && !done; i++) {
				if (ipTable[i][0] == 0 && ipTable[i][1] == 0 &&
						ipTable[i][2] == 0 && ipTable[i][3] == 0) {
					ipTable[i] = assignedIP;
					log("log.txt", "adding " + DHCPUtility.printIP(assignedIP) + " to table" );
					System.out.println("adding " + DHCPUtility.printIP(assignedIP) + " to table");
					numAssigned++;
					done = true;
				}
			}
		}
	}
	
	public static void log(String fileName, String transcript) {
		Timestamp logTime = new Timestamp(System.currentTimeMillis());
		String data = new String(transcript + NL);
		System.out.println(data);
		 try {
			BufferedWriter outputStream = new BufferedWriter(new FileWriter(fileName,true));
			outputStream.write(data);
			outputStream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("error caught in Log "+e.getMessage());
		}

	}
	

	public static void setRouter(byte[] router) {
		DHCPServer.router = router;
	}

	public static byte[] getRouter() {
		return router;
	}
	public static byte[] getSubnetMask() {
		return subnetMask;
	}

	public static void setSubnetMask(byte[] subnetMask) {
		DHCPServer.subnetMask = subnetMask;
	}
	
	public static long upTime() {
		return System.currentTimeMillis()-startTime;
	}
	
	public static String printUpTime(){
		return new String("Server Uptime: " + upTime()+"ms");
	}
	public static boolean isAccessFile() {
		return AccessFile;
	}

	public static void setAccessFile(boolean accessFile) {
		AccessFile = accessFile;
	}

	public static void IPLeaseValidate() 
	{
		// set server start time
 		long currentTime = System.currentTimeMillis();
		long endTime = defLeaseTime ;
		for (int i = 0; i < ipTable.length; i++)
		{
			if (leaseTimeTable[i] > 0 && leaseStartTable[i] > 0 ) 
			{
				System.out.println("lease end time " + (leaseStartTable[i] + 1000 *  leaseTimeTable[i]));
				
				if (currentTime > (leaseStartTable[i] + 1000 *  endTime))
				{
					System.out.println("Calling releaseIP from IPLeaseValidate()");
					releaseIP(ipTable[i]);
					// releaseIPFromTableUsingLEaseTimeOut(ipTable[i]);
				}
			}
		}
	}
}