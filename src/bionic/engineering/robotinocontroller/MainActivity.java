package bionic.engineering.robotinocontroller;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity{

	private GyroVisualizer mGyroView;
	int count = 0;	//Teller opp antall kall på connect for å ikke sende for mange oppdateringer

	private Socket socket;
	private SensorManager sensorManager;
	private static final int SERVERPORT = 11400;
	private static final String SERVER_IP = "10.10.1.59";

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//new Thread(new ClientThread()).start();
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if(sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null){	//Test to see if cellphone has Gyroscope.
			/*
			 * Mobil har ikke gyroscope!!
			 * Lag feilmelding og avslutt app!
			 * 
			 */
		}

		mGyroView = new GyroVisualizer(this);
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		layout.addView(mGyroView);
	}
	
	public void onPause(){
		super.onPause();
		sensorManager.unregisterListener(mGyroListener);
		if(socket != null){
			try{
				connect(999, 999, 999);	// To send log-of-signal
				socket.close();
			}
			catch(IOException e){
				// Have to catch this exception
			}
		}
	}
	
	public void onResume(){
		super.onResume();
		new Thread(new ClientThread()).start();
		sensorManager.registerListener(mGyroListener, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI);	//Success!
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private SensorEventListener mGyroListener = new SensorEventListener(){

		private static final float MIN_TIME_STEP = (1f / 40f);
		private long mLastTime = System.currentTimeMillis();
		private float mRotationX, mRotationY, mRotationZ;

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy){
		}

		@Override
		public void onSensorChanged(SensorEvent event){
			float[] values = event.values;
			float y = -values[0];
			float x = values[1];
			float z = values[2];

			float angularVelocity = z * 0.96f; // Minor adjustment to avoid drift on Nexus S

			// Calculate time diff
			long now = System.currentTimeMillis();
			float timeDiff = (now - mLastTime) / 400f; //1000
			mLastTime = now;
			if(timeDiff > 1){
				// Make sure we don't go bananas after pause/resume
				timeDiff = MIN_TIME_STEP;
			}		

			mRotationX += x * timeDiff;
			mRotationY += y * timeDiff;			
			mRotationZ += angularVelocity * timeDiff;
			
			// We want to make an 'area' where x, y or z is 0 so it's possible to stop Robotino.
			if(mRotationX > -0.02f && mRotationX < 0.02f)
				mRotationX = 0f;
			if(mRotationY > -0.02f && mRotationY < 0.02f)
				mRotationY = 0f;
			if(mRotationZ > -0.02f && mRotationZ < 0.02f)
				mRotationZ = 0f;
			
			mGyroView.setGyroRotation(mRotationX, mRotationY, mRotationZ);
			connect(mRotationX,mRotationY,mRotationZ);
			updateOrientation(mRotationX, mRotationY, mRotationZ);
			
		}
	};
	
	private void updateOrientation(float x, float y, float z)
	{
		TextView output = (TextView) findViewById(R.id.output);
		
		output.setText("x: " + x + "\ny: " + y + "\nz: " + z);
		//connect(x,y,z);
	} 

	public void connect(float x, float y, float z){
		try{
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(socket.getOutputStream())),
					true);
			out.println(x + ":" + y + ":" + z + ":");
		}catch(UnknownHostException e){
			e.printStackTrace();
		}catch(IOException e){
			e.printStackTrace();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	class ClientThread implements Runnable{
		@Override
		public void run(){
			try{
				InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
				socket = new Socket(serverAddr, SERVERPORT);
			}catch(UnknownHostException e1){
				e1.printStackTrace();
			}catch(IOException e1){
				e1.printStackTrace();
			}
		}
	}
}
