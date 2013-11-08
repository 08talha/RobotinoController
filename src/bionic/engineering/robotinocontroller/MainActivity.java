package bionic.engineering.robotinocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
import android.os.Handler;
import android.os.Vibrator;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
	private Handler mHandler;
	private Button mBtnDrive;
	private BtnDriveOnTouchListener mBtnDriveListener;
	private GyroVisualizer mGyroView; // Visualizing gyro on phone
	private Socket mSocket;
	private SensorManager mSensorManager;
	private float mVelocityX, mVelocityY, mVelocityZ; // Velocities in each direction
	private static final float TIMEDIFF_FACTOR = 1000f;
	private static final float TIMEDIFF_FACTOR_Z = 500f; // More sensitive on z-axis
	//private static final int SERVERPORT = 11400;
	//private static final String SERVER_IP = "10.10.1.59"; // Server receiving signals from phone
	
	private static final int SERVERPORT = 4444;
	private static final String SERVER_IP = "192.168.1.16"; // Server receiving signals from phone

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mBtnDrive = (Button) findViewById(R.id.btnDrive);
		mBtnDriveListener = new BtnDriveOnTouchListener();
		mBtnDrive.setOnTouchListener(mBtnDriveListener);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		mHandler = new Handler();

		// Test to see if cellphone has Gyroscope.
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null) {
			/**
			 * Mobil har ikke gyroscope!! Lag feilmelding og avslutt app!
			 * 
			 */
		}

		mGyroView = new GyroVisualizer(this);
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		layout.addView(mGyroView);
	}

	public void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(mGyroListener);
		if (mSocket != null) {
			try {
				sendToRobotino(999, 999, 999); // Sends 999 to close socket
				mSocket.close();
			} catch (IOException e) {
				// Have to catch this exception
			}
		}
	}

	public void onResume() {
		super.onResume();
		new Thread(new SpeakToRobotinoThread()).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private SensorEventListener mGyroListener = new SensorEventListener() {
		private static final float MIN_TIME_STEP = (1f / 40f);
		private long mLastTime = System.currentTimeMillis(); // Time of last sensorupdate

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] values = event.values;
			float x = -values[0]; // Robotino's y-axis is inverted compared to the phone's y-axis.
			float y = values[1];
			float z = values[2];

			float angularVelocityZ = z * 0.96f; // Minor adjustment to avoid drift on Nexus S

			// Calculate time diff
			long now = System.currentTimeMillis();
			float timeDiff = (now - mLastTime) / TIMEDIFF_FACTOR; // Adjust sensibility
			float timeDiffZ = (now - mLastTime) / TIMEDIFF_FACTOR_Z;
			mLastTime = now;
			if (timeDiff > 1 || timeDiffZ > 1) {
				timeDiff = timeDiffZ = MIN_TIME_STEP; // Make sure we don't go bananas after pause/resume
			}
			mVelocityX += x * timeDiff;
			mVelocityY += y * timeDiff;
			mVelocityZ += angularVelocityZ * timeDiffZ;

			// Make a zone around each axis where slow movements doesn't effect Robotino (the velocity is set to 0 in this zone).
			if (mVelocityX > -0.06f && mVelocityX < 0.06f)
				mVelocityX = 0f;
			if (mVelocityY > -0.02f && mVelocityY < 0.02f)
				mVelocityY = 0f;
			if (mVelocityZ > -0.02f && mVelocityZ < 0.02f)
				mVelocityZ = 0f;

			sendToRobotino(mVelocityY, mVelocityX, mVelocityZ); // Robotino's x-axis is the phone's y-axis
			updateOrientation(mVelocityY, mVelocityX, mVelocityZ);
		}
	};

	// Updates coordinates on the phone's screen
	private void updateOrientation(float x, float y, float z) {
		mGyroView.setGyroRotation(-x, -y, -z);
		TextView output = (TextView) findViewById(R.id.output);
		output.setText("x: " + x + "\ny: " + y + "\nz: " + z);
	}

	// Sends coordinates to server
	public void sendToRobotino(float x, float y, float z) {
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream())), true);
			out.println(x + ":" + y + ":" + z + ":");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//The thread that reads messages from Robotino
	/*class ReadFromRobotinoThread implements Runnable {
		String messageFromRobotino = "";

		@Override
		public void run() {
			try {
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
				int charsRead = 0;
				char[] buffer = new char[2048];

				while ((charsRead = inFromServer.read(buffer)) != -1)
					messageFromRobotino += new String(buffer).substring(0, charsRead);

				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mBtnDrive.setText(messageFromRobotino);
					}

				});
			}
			catch (IOException e) {
				Log.d("test","IOE");
			}
		}
	}*/

	// The thread that keeps the socket that speaks to the Robotino-server
	class SpeakToRobotinoThread implements Runnable{
		String messageFromRobotino = "";
	  		@Override 
	  		public void run(){
	  			try{ 
	  				InetAddress serverAddr = InetAddress.getByName(SERVER_IP); 
	  				mSocket = new Socket(serverAddr, SERVERPORT);
	  				//new Thread(new ReadFromRobotinoThread()).start();
	  				
	  				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
					int charsRead = 0;
					char[] buffer = new char[2048];

					while ((charsRead = inFromServer.read(buffer)) != -1)
						messageFromRobotino += new String(buffer).substring(0, charsRead);

					mHandler.post(new Runnable() {
						@Override
						public void run() {
							if(messageFromRobotino.equals("Bumper-bumper")){	//Crash-signal
								Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
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

	// Listening for changes on btnDrive; our "gas pedal"
	// We register the SensorListener when pushing down on the drive-button, and unregister it when releasing button.
	// We also set all the axis to 0 so we don't continue driving on a new "push-down".
	class BtnDriveOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View btn, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mSensorManager.registerListener(mGyroListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI);
				break;
			case MotionEvent.ACTION_UP:
				mSensorManager.unregisterListener(mGyroListener);
				mVelocityX = 0;
				mVelocityY = 0;
				mVelocityZ = 0;
				updateOrientation(mVelocityX, mVelocityY, mVelocityZ);
				mGyroView.setGyroRotation(mVelocityX, mVelocityY, mVelocityZ);
				break;
			}
			return false;
		}
	}
}
