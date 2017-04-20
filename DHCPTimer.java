import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class DHCPTimer extends TimerTask {

	@Override
	public void run() {

		doSomeWork();
	}

	// simulate a time consuming task
	private void doSomeWork() {
		try {
			if(!DHCPServer.isAccessFile()){
				DHCPServer.log("log.txt", "DHCPTimer:updateAssignedIp() Got access to file DHCP Server Storage.txt " );
				//DHCPServer.setAccessFile(true);
				
				// TO DO .. access the flag and perform task
				DHCPServer.IPLeaseValidate();
			
				//DHCPServer.setAccessFile(false);
				DHCPServer.log("log.txt", "DHCPTimer:updateAssignedIp() Released access to file DHCP Server Storage.txt " );

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
}
/*
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class TimerTaskExample extends TimerTask {

	@Override
	public void run() {
		System.out.println("Start time:" + new Date());
		doSomeWork();
		System.out.println("End time:" + new Date());
	}

	// simulate a time consuming task
	private void doSomeWork() {
		try {

			//hread.sleep(10000);
			
System.out.println("tEST");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String args[]) {

		TimerTask timerTask = new TimerTaskExample();
		// running timer task as daemon thread
		Timer timer = new Timer(true);
		timer.scheduleAtFixedRate(timerTask, 0, 1 * 1000);
		System.out.println("TimerTask begins! :" + new Date());
		// cancel after sometime
		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		timer.cancel();
		System.out.println("TimerTask cancelled! :" + new Date());
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}*/