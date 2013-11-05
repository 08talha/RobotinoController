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
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity{
	Button btnDrive;
	BtnDriveOnTouchListener btnDriveListener;
	private GyroVisualizer mGyroView;						//Visualizing gyro on phone
	private Socket mSocket;									
	private SensorManager mSensorManager;
	private float mVelocityX, mVelocityY, mVelocityZ;		//Velocities in each direction
	private static final float TIMEDIFF_FACTOR = 400f;
	private static final int SERVERPORT = 11400;			
	private static final String SERVER_IP = "10.10.1.59";	//Server receiving signals from phone

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		btnDrive = (Button)findViewById(R.id.btnDrive);
		btnDriveListener = new BtnDriveOnTouchListener();
		btnDrive.setOnTouchListener(btnDriveListener);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		//Test to see if cellphone has Gyroscope.
		if(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null){
			/**
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
		mSensorManager.unregisterListener(mGyroListener);
		if(mSocket != null){
			try{
				connect(999, 999, 999);					//Sends 999 to close socket
				mSocket.close();
			}
			catch(IOException e){
				// Have to catch this exception
			}
		}
	}
	
	public void onResume(){
		super.onResume();
		new Thread(new ClientThread()).start();
		//mSensorManager.registerListener(mGyroListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private SensorEventListener mGyroListener = new SensorEventListener(){
		private static final float MIN_TIME_STEP = (1f / 40f);
		private long mLastTime = System.currentTimeMillis();	//Time of last sensorupdate

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy){
		}

		@Override
		public void onSensorChanged(SensorEvent event){
			float[] values = event.values;
			float y = -values[0];
			float x = values[1];
			float z = values[2];

			float angularVelocityZ = z * 0.96f; 		// Minor adjustment to avoid drift on Nexus S

			// Calculate time diff
			long now = System.currentTimeMillis();
			float timeDiff = (now - mLastTime) / TIMEDIFF_FACTOR;	//Adjust sensibility
			mLastTime = now;
			if(timeDiff > 1){
				timeDiff = MIN_TIME_STEP; 				// Make sure we don't go bananas after pause/resume
			}		
			mVelocityX += x * timeDiff;
			mVelocityY += y * timeDiff;
			mVelocityZ += angularVelocityZ * timeDiff;
			
			// Make an 'area' where x, y or z is 0 so it's possible to stop Robotino.
			if(mVelocityX > -0.02f && mVelocityX < 0.02f)
				mVelocityX = 0f;
			if(mVelocityY > -0.02f && mVelocityY < 0.02f)
				mVelocityY = 0f;
			if(mVelocityZ > -0.02f && mVelocityZ < 0.02f)
				mVelocityZ = 0f;
			
			mGyroView.setGyroRotation(mVelocityX, mVelocityY, mVelocityZ);	
			connect(mVelocityX,mVelocityY,mVelocityZ);						
			updateOrientation(mVelocityX, mVelocityY, mVelocityZ);
		}
	};
	
	//Updates coordinates on the phone screen
	private void updateOrientation(float x, float y, float z)
	{
		TextView output = (TextView) findViewById(R.id.output);
		output.setText("x: " + x + "\ny: " + y + "\nz: " + z);
	} 

	//Sends coordinates to server
	public void connect(float x, float y, float z){
		try{
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(mSocket.getOutputStream())),
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

	// The thread that keeps the socket that talks to the Robotino-server
	class ClientThread implements Runnable{
		@Override
		public void run(){
			try{
				InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
				mSocket = new Socket(serverAddr, SERVERPORT);
			}catch(UnknownHostException e1){
				e1.printStackTrace();
			}catch(IOException e1){
				e1.printStackTrace();
			}
		}
	}
	
	// Listening for changes on btnDrive; our gas pedal
	class BtnDriveOnTouchListener implements OnTouchListener{
		
		@Override
		public boolean onTouch(View btn, MotionEvent event) {
			switch(event.getAction()){
			case MotionEvent.ACTION_DOWN:
				mSensorManager.registerListener(mGyroListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI);
				break;
			case MotionEvent.ACTION_UP:
				mSensorManager.unregisterListener(mGyroListener);
				mVelocityX = 0; mVelocityY = 0; mVelocityZ = 0;
				break;
			}
			return false;
		}
	}
}
