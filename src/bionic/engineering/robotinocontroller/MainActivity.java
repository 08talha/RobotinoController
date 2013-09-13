package bionic.engineering.robotinocontroller;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//hello!
		hookupSensorListener();
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

	private void updateOrientation(float heading, float pitch, float roll) 
	{
		  TextView output = (TextView)findViewById(R.id.output);
		  output.setText("heading: " + heading + "\npitch: " + pitch + "\nroll: " + roll);
		  
	}
}//Andy
