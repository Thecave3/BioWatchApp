package thecave.forge.biowatchapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static int PERMISSIONS_REQUEST_BODY_SENSOR = 1;

    private final static String FILE_EXCHANGE_CAPABILITY_NAME = "file_exchange";

    private String transcriptionNodeId;

    private static final String FILE_EXCHANGE_PATH = "/file_exchange";

    private TextView currentHeartRateView, timeSinceBeginView;
    private Button startButton, sendDataButton, clearDataButton;

    private long timeSinceBegin = 0;
    private double currentHeartRate = 0.0f;

    private SensorManager mSensorManager;
    private Sensor heartRateSensor;

    private boolean isRecording = false;

    private FileWriter fileWriter;
    private File fileToSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.start_button);
        sendDataButton = findViewById(R.id.send_data_button);
        clearDataButton = findViewById(R.id.clear_data_button);

        currentHeartRateView = findViewById(R.id.current_heartrate);
        timeSinceBeginView = findViewById(R.id.time_passed);
        currentHeartRateView.setText(new StringBuilder().append(getString(R.string.current_heartrate_placeholder)).append(currentHeartRate));
        timeSinceBeginView.setText(new StringBuilder().append(getString(R.string.time_since_begin)).append(timeSinceBegin));

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        heartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        Log.d(TAG, "CURRENTLY USED " + heartRateSensor.getName() + " Type String " + heartRateSensor.getStringType() + " Type number :" + heartRateSensor.getType());
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
                mSensorManager.registerListener(listener, heartRateSensor, SensorManager.SENSOR_DELAY_UI);
                isRecording = true;
            }

        });

        sendDataButton.setVisibility(View.GONE);
        sendDataButton.setOnClickListener(view -> {
            if (isRecording)
                startButton.performClick();
            new Thread(this::sendData).start();
        });

        if (checkSelfPermission(Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission not granted, asking for");
            requestPermissions(
                    new String[]{Manifest.permission.BODY_SENSORS}, PERMISSIONS_REQUEST_BODY_SENSOR);
        } else {
            Log.d(TAG, "Permission already granted");
        }

        clearDataButton.setOnClickListener(view -> clearData());
        // Enables Always-on
        setAmbientEnabled();


        new Thread(() -> {
            try {
                CapabilityInfo capabilityInfo = Tasks.await(Wearable.getCapabilityClient(this).getCapability(
                        FILE_EXCHANGE_CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE));
                setupFileExchange(capabilityInfo);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupFileExchange(CapabilityInfo capabilityInfo) {
        Log.d(TAG, "setupFileExchange: setup file exchange started");
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        transcriptionNodeId = pickBestNodeId(connectedNodes);
        Log.d(TAG, "picking best node");
        runOnUiThread(() -> sendDataButton.setVisibility(View.VISIBLE));
    }


    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby())
                return node.getId();

            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private synchronized void sendData() {
        if (transcriptionNodeId != null) {

            Log.d(TAG, "sendData: Start sending " + fileToSend);
            ChannelClient channelClient = Wearable.getChannelClient(this);
            Task<ChannelClient.Channel> channelClientTask = channelClient.openChannel(transcriptionNodeId, FILE_EXCHANGE_PATH);
            try {
                ChannelClient.Channel channel = Tasks.await(channelClientTask);

                Tasks.await(channelClient.sendFile(channel, Uri.fromFile(fileToSend)));
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                Log.e(TAG, "sendData: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "sendData: transcription Node id == null!");
        }
    }

    private void initializeSaveFile() throws IOException {
        // Initialize file
        final String today = new SimpleDateFormat("MMMM_dd_yyyy 'at' HH:mm:ss z", Locale.ITALY).format(Calendar.getInstance(Locale.ITALY).getTime());
        fileToSend = new File(getFilesDir(), "filetosend.csv");

        boolean res = fileToSend.createNewFile();
        Log.d(TAG, "initializeSaveFile: new file created? " + res);

        fileWriter = new FileWriter(fileToSend);
        fileWriter.append(today).append(",Time,Value\n");
    }

    private void clearData() {
        if (fileToSend.exists()) {
            boolean res = fileToSend.delete();
            Log.d(TAG, "initializeSaveFile: file deleted? " + res);
        }
        for (File file : getFilesDir().listFiles())
            if (file.isFile())
                file.delete();
    }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        //  Log.d(TAG, "onSensorChanged: timestamp : \"" + sensorEvent.timestamp + "\", value \"" + Arrays.toString(sensorEvent.values) + "\"");
        currentHeartRate = (int) sensorEvent.values[0];
        try {
            fileWriter.append(",").append(String.valueOf(timeSinceBegin)).append(",").append(String.valueOf(currentHeartRate)).append("\n");
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
