package bionic.engineering.robotinocontroller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;

public class SpeakToRobotinoThread implements Runnable{
	private Handler mHandler  = new Handler();
	private Socket mSocket;
	private Context context;
	String messageFromRobotino = "";
	
	public SpeakToRobotinoThread (Context con, Socket socket){
		context = con;
		mSocket = socket;
	}
	
	@Override 
	public void run(){
		try{ 
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			int charsRead = 0;
			char[] buffer = new char[2048];
	
			while ((charsRead = inFromServer.read(buffer)) != -1)
				messageFromRobotino += new String(buffer).substring(0, charsRead);
	
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if(messageFromRobotino.equals("Bumper-bumper")){	//Crash-signal
						Vibrator vib = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
						vib.vibrate(1000);	//Vibrate for 1000ms
					}
				}
			});
		}catch(UnknownHostException e1){ 
			e1.printStackTrace(); 
		} 
		catch(IOException e1){ 
			e1.printStackTrace(); 
		} 
	} 
}