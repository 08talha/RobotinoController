package bionic.engineering.robotinocontroller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static final float TIMEDIFF_FACTOR = 1000f;
	private static final float TIMEDIFF_FACTOR_Z = 500f; // More sensitive on z-axis
	private static final int SERVERPORT = 11400;
	private static final String SERVER_IP = "10.10.1.59"; // Server receiving signals from phone
	
	private Handler mHandler;
	private Button mBtnDrive;
	private BtnDriveOnTouchListener mBtnDriveListener;
	private GyroVisualizer mGyroView; // Visualizing gyro on phone
	private Socket mSocket;
	private SensorManager mSensorManager;
	private GyroListener mGyroListener;
	private float mVelocityX, mVelocityY, mVelocityZ; // Velocities in each direction
	private boolean mIsConnected, mGoToPreferenceActivity, mIsDummyDisconnected;
	private SharedPreferences mPreferences;
	private PreferenceListener preferenceListener;
	private boolean mShowCoordinates, mXEnabled, mYEnabled, mZEnabled, mOfflineMode;
	private ProgressDialog mProgressDialog;
	private TextView mOutput;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mGoToPreferenceActivity = false;
		mIsConnected = false;
		mIsDummyDisconnected = false;
		mProgressDialog = null;
		
		mBtnDrive = (Button) findViewById(R.id.btnDrive);
		mBtnDriveListener = new BtnDriveOnTouchListener();
		mBtnDrive.setOnTouchListener(mBtnDriveListener);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		mHandler = new Handler();

		// Test to see if cellphone has Gyroscope.
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null) {
			showFinishDialog();
		}

		mGyroView = new GyroVisualizer(this);
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		layout.addView(mGyroView);
		
		mGyroListener = new GyroListener();

		mPreferences = getSharedPreferences("bionic.engineering.robotinocontroller_preferences", MODE_MULTI_PROCESS);
		preferenceListener = new PreferenceListener();
		mPreferences.registerOnSharedPreferenceChangeListener(preferenceListener);
		
		mShowCoordinates = mPreferences.getBoolean("showCoordinates", true);
		mXEnabled = mPreferences.getBoolean("isXEnabled", true);
		mYEnabled = mPreferences.getBoolean("isYEnabled", true);
		mZEnabled = mPreferences.getBoolean("isZEnabled", true);
		mOfflineMode = mPreferences.getBoolean("offlineMode", false);
		
		if (mOfflineMode)
		{
			mIsConnected = mOfflineMode;
			mBtnDrive.setBackgroundResource(R.drawable.green_button_state);
        	mBtnDrive.setText(getString(R.string.btnOffline));
		}
		
		mOutput = (TextView) findViewById(R.id.output);
		
		if (!mShowCoordinates)
			mOutput.setText("");
		else
			mOutput.setText("x: 0.0\ny: 0.0\nz: 0.0");
	}

	@Override
	public void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(mGyroListener);
		if (!mGoToPreferenceActivity && mSocket != null) {
			try {
				sendToRobotino(999, 999, 999); // Sends 999 to close socket
				mSocket.close();
				mIsConnected = false;
				mBtnDrive.setBackgroundResource(R.drawable.red_button_state);
				mBtnDrive.setText(getString(R.string.btnConnectText));
			} catch (IOException e) {
				// Have to catch this exception
			}
		}
		mGoToPreferenceActivity = false;
		
		if (mProgressDialog != null)
		{
			mProgressDialog.dismiss();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		switch(item.getItemId())
		{
			case R.id.action_settings : showSettingsPreferenceActivity();
									 	return true;
		}
		
		return false;
	}
	
	// If the device does not have Gyroscope this is called and when the user clicks ok the app closes.
	private void showFinishDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.finish_dialog_title);
		builder.setMessage(R.string.finish_dialog_message);
		builder.setPositiveButton(getString(R.string.ok_button), new OnClickListener()
		{
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				finish();
			}
		});
		
		builder.setCancelable(false);
		builder.create().show();
	}
	
	// The method is called when you click on settings
	private void showSettingsPreferenceActivity()
	{
		mGoToPreferenceActivity = true;
		Intent intent = new Intent(this, SettingsPreferenceActivity.class);
		startActivity(intent);
	}

	// Updates coordinates on the phone's screen
	private void updateOrientation(float x, float y, float z) {
		mGyroView.setGyroRotation(-x, -y, -z);
		if (mShowCoordinates)
		{
			// Robotino's x-axis is the phone's y-axis
			mOutput.setText("x: " + y + "\ny: " + x + "\nz: " + z);
		}
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
	
	// Connects to server.
	class ConnectToBrain extends AsyncTask<String, String, String>
	{
		String messageFromRobotino = "";
		
        @Override
        protected void onPreExecute() 
        {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage(getString(R.string.connect_to_server_message));
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }
 
        @Override
        protected String doInBackground(String... args) 
        {
        	try
            { 
  				SocketAddress serverAddr = new InetSocketAddress(SERVER_IP, SERVERPORT);
  				mSocket = new Socket();
  				// Try connect with 6 s timeout
  				mSocket.connect(serverAddr, 6000);
  			
  				new Thread(new SpeakToRobotinoThread()).start();
            }
            catch(UnknownHostException e1)
            { 
  				return e1.toString();
  			} 
  			catch(IOException e1)
  			{ 
  				return e1.toString(); 
  			}
        	
            return "ok";
        }
 
        @Override
        protected void onPostExecute(String message) 
        {
        	mProgressDialog.dismiss();
        	mProgressDialog = null;
        	
  			if(!message.equals("ok")){
            	mIsConnected = false;
            	mBtnDrive.setBackgroundResource(R.drawable.red_button_state);
            	mBtnDrive.setText(getString(R.string.btnConnectText));
            	
            	Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG);
            	toast.setGravity(Gravity.CENTER, 0, 0);
            	toast.show();
            }
        }
    }

	
	/* Listening for changes on btnDrive; our "gas pedal"
	 * We register the SensorListener when pushing down on the drive-button, and unregister it when releasing button.
	 * We also set all the axis to 0 so we don't continue driving on a new "push-down".
	 */
	class BtnDriveOnTouchListener implements OnTouchListener {

		@Override
		public boolean onTouch(View btn, MotionEvent event) {
			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if(!mIsConnected && !mOfflineMode)
					new ConnectToBrain().execute();
				else if (mIsDummyDisconnected)
				{
					mIsDummyDisconnected = false;
	  				mBtnDrive.setBackgroundResource(R.drawable.green_button_state);
	            	mBtnDrive.setText(getString(R.string.btnDriveText));
				}
				else
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
	
	
	// Listening for changes on gyroscope sensor.
	class GyroListener implements SensorEventListener {
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
			
			if (mXEnabled) mVelocityX += x * timeDiff;
			else mVelocityX = 0f;
				
			if (mYEnabled) mVelocityY += y * timeDiff;
			else mVelocityY = 0f;
			
			if (mZEnabled) mVelocityZ += angularVelocityZ * timeDiffZ;
			else mVelocityZ = 0f;

			// Make a zone around each axis where slow movements doesn't effect Robotino (the velocity is set to 0 in this zone).
			if (mVelocityX > -0.06f && mVelocityX < 0.06f)
				mVelocityX = 0f;
			if (mVelocityY > -0.02f && mVelocityY < 0.02f)
				mVelocityY = 0f;
			if (mVelocityZ > -0.02f && mVelocityZ < 0.02f)
				mVelocityZ = 0f;

			if (!mOfflineMode)
				sendToRobotino(mVelocityY, mVelocityX, mVelocityZ); // Robotino's x-axis is the phone's y-axis
			
			updateOrientation(mVelocityY, mVelocityX, mVelocityZ);
		}
	}
	
	class SpeakToRobotinoThread implements Runnable{
		String messageFromRobotino = "";
		
		@Override 
		public void run(){
			try{
				while(mSocket.isConnected()){
					BufferedReader inFromServer = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
					String lineRead = "";
			
					lineRead = inFromServer.readLine();
					messageFromRobotino = lineRead;
			
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							if(messageFromRobotino == null){	//Crash-signal
								try{
									mSocket.close();
									mIsConnected = false;
								}
								catch(IOException io){
									io.printStackTrace();
								}
								
								Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
								vib.vibrate(1000);	//Vibrate for 1000ms
								mBtnDrive.setBackgroundResource(R.drawable.red_button_state);
								mBtnDrive.setText(getString(R.string.btnConnectText));
							}
							else if(messageFromRobotino.equals("bumper")){
								Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
								vib.vibrate(1000);	//Vibrate for 1000ms
								mBtnDrive.setBackgroundResource(R.drawable.red_button_state);
								mBtnDrive.setText(getString(R.string.btnConnectText));
								mIsDummyDisconnected = true;
								mSensorManager.unregisterListener(mGyroListener);
								Toast toast = Toast.makeText(MainActivity.this, getString(R.string.crash_message), Toast.LENGTH_LONG);
				            	toast.setGravity(Gravity.CENTER, 0, 0);
				            	toast.show();
							}
							else if(messageFromRobotino.equals("connected")){
				  				mIsConnected = true;
				  				mBtnDrive.setBackgroundResource(R.drawable.green_button_state);
				            	mBtnDrive.setText(getString(R.string.btnDriveText));
				            	Toast toast = Toast.makeText(MainActivity.this, getString(R.string.connected_message), Toast.LENGTH_LONG);
				            	toast.setGravity(Gravity.CENTER, 0, 0);
				            	toast.show();
							}
						}
					});
				}
			}catch(UnknownHostException e1){ 
				e1.printStackTrace(); 
			} 
			catch(IOException e1){ 
				e1.printStackTrace(); 
			}
			catch(Exception e1){
				e1.printStackTrace(); 
			}
		} 
	}
	
	class PreferenceListener implements OnSharedPreferenceChangeListener
	{
		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) 
		{
			if (key.equals("showCoordinates"))
			{
				mShowCoordinates = mPreferences.getBoolean("showCoordinates", true);
				
				if (!mShowCoordinates)
				{
					mOutput.setText("");
				}
				else
					mOutput.setText("x: 0.0\ny: 0.0\nz: 0.0");
				
			}
			else if (key.equals("isXEnabled"))
			{
				mXEnabled = mPreferences.getBoolean("isXEnabled", true);
			}
			else if (key.equals("isYEnabled"))
			{
				mYEnabled = mPreferences.getBoolean("isYEnabled", true);
			}
			else if (key.equals("isZEnabled"))
			{
				mZEnabled = mPreferences.getBoolean("isZEnabled", true);
			}
			else if (key.equals("offlineMode"))
			{
				mOfflineMode = mPreferences.getBoolean("offlineMode", false);
				
				if (mOfflineMode)
				{
					mIsConnected = mOfflineMode;
	  				mBtnDrive.setBackgroundResource(R.drawable.green_button_state);
	            	mBtnDrive.setText(getString(R.string.btnOffline));
	            	
	            	if (mSocket != null) {
	        			try {
	        				sendToRobotino(999, 999, 999); // Sends 999 to close socket
	        				mSocket.close();
	        				mIsConnected = false;
	        			} catch (IOException e) {
	        				// Have to catch this exception
	        			}
	        		}
				}
				else
				{
					mIsConnected = mOfflineMode;
	  				mBtnDrive.setBackgroundResource(R.drawable.red_button_state);
	            	mBtnDrive.setText(getString(R.string.btnConnectText));
				}
			}
		}
	}
}
