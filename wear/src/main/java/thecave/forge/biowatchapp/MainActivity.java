package thecave.forge.biowatchapp;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private static final int SAMPLING_PERIOD_MILLISECONDS = 1000;

    private TextView currentHeartrateView, timeSinceBeginView;
    private Button startButton;

    private long timeSinceBegin = 0;
    private double currentHeartRate = 0.0f;

    private SensorManager mSensorManager;
    private Sensor heartRateSensor;

    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.start_button);

        currentHeartrateView = findViewById(R.id.current_heartrate);
        timeSinceBeginView = findViewById(R.id.time_passed);
        currentHeartrateView.setText(new StringBuilder().append(getString(R.string.current_heartrate_placeholder)).append(currentHeartRate));
        timeSinceBeginView.setText(new StringBuilder().append(getString(R.string.time_since_begin)).append(timeSinceBegin));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        heartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        final SensorEventListener listener = this;

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Button button = (Button) view;
                if (isRecording) {
                    mSensorManager.unregisterListener(listener);
                    button.setText(R.string.start_record);
                    isRecording = false;
                } else {
                    button.setText(R.string.stop_record);
                    mSensorManager.registerListener(listener, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
                    isRecording = true;
                }

            }
        });


        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {

        Log.d(TAG, "onSensorChanged: timestamp : \"" + sensorEvent.timestamp + "\", value \"" + Arrays.toString(sensorEvent.values) + "\"");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "onSensorChanged: timestamp : \"" + sensorEvent.timestamp + "\", value \"" + Arrays.toString(sensorEvent.values) + "\"", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(TAG, "onAccuracyChanged: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
