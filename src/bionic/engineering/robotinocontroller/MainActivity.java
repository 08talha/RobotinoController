package bionic.engineering.robotinocontroller;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity{
	
	private GyroVisualizer mGyroView;

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		hookupSensorListener();
		
		mGyroView = new GyroVisualizer(this);
		//setContentView(mGyroView);
		LinearLayout layout = (LinearLayout) findViewById(R.id.layout);
		layout.addView(mGyroView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void hookupSensorListener() 
	{
		SensorManager sm = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		sm.registerListener(mGyroListener, sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI);
	}

	private SensorEventListener mGyroListener = new SensorEventListener() {

        private static final float MIN_TIME_STEP = (1f / 40f);
        private long mLastTime = System.currentTimeMillis();
        private float mRotationX, mRotationY, mRotationZ;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] values = event.values;
            float x = values[0];
            float y = values[1];
            float z = values[2];
            int i = 1;

            float angularVelocity = z * 0.96f; // Minor adjustment to avoid drift on Nexus S

            // Calculate time diff
            long now = System.currentTimeMillis();
            float timeDiff = (now - mLastTime) / 1000f;
            mLastTime = now;
            if (timeDiff > 1) {
                // Make sure we don't go bananas after pause/resume
                timeDiff = MIN_TIME_STEP;
            } 

            mRotationX += x * timeDiff;
            if (mRotationX > 0.5f)
                mRotationX = 0.5f;
            else if (mRotationX < -0.5f)
                mRotationX = -0.5f;

            mRotationY += y * timeDiff;
            if (mRotationY > 0.5f)
                mRotationY = 0.5f;
            else if (mRotationY < -0.5f)
                mRotationY = -0.5f;

            mRotationZ += angularVelocity * timeDiff;

            mGyroView.setGyroRotation(mRotationX, mRotationY, mRotationZ);
            updateOrientation(mRotationX, mRotationY, mRotationZ);
        }
    };

	private void updateOrientation(float x, float y, float z) 
	{
		  TextView output = (TextView)findViewById(R.id.output);
		  output.setText("x: " + x + "\ny: " + y + "\nz: " + z);
		  
	}
}//Andy
