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
		sm.registerListener(sel, sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI);
	}

	private final SensorEventListener sel = new SensorEventListener() 
	{
		public void onSensorChanged(SensorEvent event) 
		{ 
			if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) 
			{
				updateOrientation(event.values[0], event.values[1], event.values[2]);
		    }
		}
		   
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	};

	private void updateOrientation(float x, float y, float z) 
	{
		  TextView output = (TextView)findViewById(R.id.output);
		  output.setText("x: " + x + "\ny: " + y + "\nz: " + z);
		  
		  mGyroView.setGyroRotation(x, y, z);
		  
	}
}//Andy
