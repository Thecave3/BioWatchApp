package thecave.forge.biowatchapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static int PERMISSIONS_REQUEST_BODY_SENSOR = 1;
    private final static int SAMPLING_PERIOD_MILLISECONDS = 2000;

    private TextView currentHeartRateView, timeSinceBeginView;
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

        currentHeartRateView = findViewById(R.id.current_heartrate);
        timeSinceBeginView = findViewById(R.id.time_passed);
        currentHeartRateView.setText(new StringBuilder().append(getString(R.string.current_heartrate_placeholder)).append(currentHeartRate));
        timeSinceBeginView.setText(new StringBuilder().append(getString(R.string.time_since_begin)).append(timeSinceBegin));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        heartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        final SensorEventListener listener = this;

        startButton.setOnClickListener(view -> {
            Button button = (Button) view;
            if (isRecording) {
                mSensorManager.unregisterListener(listener);
                button.setText(R.string.start_record);
                isRecording = false;
            } else {
                initializeSaveFile();
                button.setText(R.string.stop_record);
                timeSinceBegin = System.nanoTime();
                mSensorManager.registerListener(listener, heartRateSensor, SAMPLING_PERIOD_MILLISECONDS);
                isRecording = true;
            }

        });
        if (checkSelfPermission(Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission not granted, asking for");
            requestPermissions(
                    new String[]{Manifest.permission.BODY_SENSORS}, PERMISSIONS_REQUEST_BODY_SENSOR);
        } else {
            Log.d(TAG, "Permission already granted");
        }
        // Enables Always-on
        setAmbientEnabled();
    }

    private void initializeSaveFile() {
        // Initialize file

        //final String filename = "" + +".csv";

    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        //  Log.d(TAG, "onSensorChanged: timestamp : \"" + sensorEvent.timestamp + "\", value \"" + Arrays.toString(sensorEvent.values) + "\"");
        runOnUiThread(() -> {
            timeSinceBeginView.setText(new StringBuilder().append(getString(R.string.time_since_begin)).append(sensorEvent.timestamp - timeSinceBegin));
            currentHeartRate = sensorEvent.values[0];
            currentHeartRateView.setText(new StringBuilder().append(getString(R.string.current_heartrate_placeholder)).append(currentHeartRate));
        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged: " + sensor + " new accuracy " + accuracy);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_BODY_SENSOR:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                } else {
                    Log.e(TAG, "onRequestPermissionsResult: permission not granted");
                }
                return;

            default:
                Log.e(TAG, "onRequestPermissionsResult: code request not identified");
        }
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
