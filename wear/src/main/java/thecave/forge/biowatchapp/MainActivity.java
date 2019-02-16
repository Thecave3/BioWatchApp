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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static int PERMISSIONS_REQUEST_BODY_SENSOR = 1;
    private final static int SAMPLING_PERIOD_MILLISECONDS = 2000;

    private static final String PATHNAME = "/datafile";

    private TextView currentHeartRateView, timeSinceBeginView;
    private Button startButton, sendDataButton;

    private long timeSinceBegin = 0;
    private double currentHeartRate = 0.0f;

    private SensorManager mSensorManager;
    private Sensor heartRateSensor;

    private boolean isRecording = false;

    private FileWriter fileWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.start_button);
        sendDataButton = findViewById(R.id.send_data_button);

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
                try {
                    fileWriter.close();
                    fileWriter = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
                timeSinceBegin = 0;
                isRecording = false;
            } else {
                try {
                    initializeSaveFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
                button.setText(R.string.stop_record);
                mSensorManager.registerListener(listener, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
                isRecording = true;
            }

        });

        sendDataButton.setOnClickListener(view -> {
            if (isRecording)
                startButton.performClick();
            try {
                sendData();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void sendData() throws IOException {
        sendDataButton.setVisibility(View.GONE);
        PutDataMapRequest dataMap = PutDataMapRequest.create(PATHNAME);
        PutDataRequest request;
        Task<DataItem> putTask;

        Log.d(TAG, "sendData: Start sending files");

        for (File file : getFilesDir().listFiles()) {
            if (file.isFile()) {
                Log.d(TAG, "sendData: " + file.getName());
                dataMap.getDataMap().putAsset(file.getName(), Asset.createFromBytes(Files.readAllBytes(file.toPath())));
                request = dataMap.asPutDataRequest();
                if (!request.isUrgent())
                    request.setUrgent();
                putTask = Wearable.getDataClient(this, new Wearable.WearableOptions.Builder().setLooper(getMainLooper()).build()).putDataItem(request);
                putTask.addOnSuccessListener(dataItem -> Log.d(TAG, "sendData: OnSuccess"));
                putTask.addOnFailureListener(Throwable::printStackTrace);
            }
        }

        sendDataButton.setVisibility(View.VISIBLE);
        //Toast.makeText(this, "Starting sending files", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "sendData: sent all files");
    }

    private void initializeSaveFile() throws IOException {
        // Initialize file
        final String today = new SimpleDateFormat("MMMM_dd_yyyy", Locale.ITALY).format(Calendar.getInstance(Locale.ITALY).getTime());
        int counter = 0;
        String filename = today + "_" + counter + ".csv";
        File logfile;
        do {
            logfile = new File(getFilesDir(), filename);
            counter++;
        } while (logfile.createNewFile());

        fileWriter = new FileWriter(logfile);
        fileWriter.write(SAMPLING_PERIOD_MILLISECONDS + ",Time,Value\n");
    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        //  Log.d(TAG, "onSensorChanged: timestamp : \"" + sensorEvent.timestamp + "\", value \"" + Arrays.toString(sensorEvent.values) + "\"");
        currentHeartRate = sensorEvent.values[0];
        try {
            fileWriter.write("," + timeSinceBegin + "," + currentHeartRate + "\n");
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
        timeSinceBegin++;
        timeSinceBeginView.setText(new StringBuilder().append(getString(R.string.time_since_begin)).append(timeSinceBegin));
        currentHeartRateView.setText(new StringBuilder().append(getString(R.string.current_heartrate_placeholder)).append(currentHeartRate));

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
