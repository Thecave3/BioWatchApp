package thecave.forge.biowatchapp;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXT_STORAGE = 1;
    private static final String FILE_EXCHANGE_PATH = "/file_exchange";
    private static final int PERMISSIONS_REQUEST_BODY_SENSOR = 2;
    private TextView debugger;
    private Button sendDataButton, startRecordButton;

    private SensorManager mSensorManager;

    private File fileToSend;
    private FileWriter fileWriter;

    Sensor heartIR;
    Sensor heartRED;

    ChannelClient.ChannelCallback channelCallback;
    ChannelClient channelClient;

    String valori = "";

    int time = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        debugger = findViewById(R.id.debugger);
        startRecordButton = findViewById(R.id.start_record_data);
        sendDataButton = findViewById(R.id.send_data);


        startRecordButton.setOnClickListener(view -> {
            try {
                startRecord();
                view.setVisibility(View.GONE);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        sendDataButton.setOnClickListener(view -> {

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("data", valori);
            clipboard.setPrimaryClip(clip);
            writeDebug("I dati sono stati copiati nella clipboard");

        /*
            writeDebug("Start share file");
            try {
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                writeErrorDebug(e.getMessage());
            }

            try {
                Intent intentShareFile = new Intent(Intent.ACTION_SEND);

                if (fileToSend.exists()) {
                    intentShareFile.setType("text/csv");
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(fileToSend));
                    startActivity(Intent.createChooser(intentShareFile, "Share File"));
                    writeDebug("File sharing ongoing... completed");
                } else {
                    writeErrorDebug("fileToSend does not exists");
                }
            } catch (Exception e) {
                writeErrorDebug(e.getMessage());
            }

        */

        });

        if (!isExternalStorageWritable() || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXT_STORAGE);
            writeDebug("Need to ask permissions");
        } else {
            writeDebug("Permission granted");
        }


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


        if (checkSelfPermission(Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            writeDebug("Permission heart not granted, asking for");
            requestPermissions(
                    new String[]{Manifest.permission.BODY_SENSORS}, PERMISSIONS_REQUEST_BODY_SENSOR);
        } else {
            writeDebug("Permission heart already granted");
        }

        // change number in base of the watch
        for (Sensor currentSensor : mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            //writeDebug("Name: " + currentSensor.getName() + " Type_String: " + currentSensor.getStringType() + " /ype_number: " + currentSensor.getType());

            if (currentSensor.getType() == 65571)
                heartIR = currentSensor;

            if (currentSensor.getType() == 65572)
                heartRED = currentSensor;
        }


        if (heartIR == null || heartRED == null) {
            writeErrorDebug("heartIR == null || heartRed == null");
        } else {
            writeDebug("All set and ready to go");
        }


        channelCallback = new ChannelClient.ChannelCallback() {
            @Override
            public void onChannelOpened(@NonNull ChannelClient.Channel channel) {
                super.onChannelOpened(channel);
                writeDebug("Channel opened");
                if (channel.getPath().equals(FILE_EXCHANGE_PATH)) {
                    writeDebug("Path del channel corretto");
                    new Thread(() -> {
                        try {
                            writeDebug("Starting save Files on external Storage");
                            Tasks.await(channelClient.receiveFile(channel, Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "wear_hr_data.csv")), false));

                            writeDebug("File save complete");
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                            writeErrorDebug(e.getMessage());
                        }
                    }).start();
                } else {
                    writeErrorDebug("path del channel = " + channel.getPath());
                }
            }

            @Override
            public void onChannelClosed(@NonNull ChannelClient.Channel channel, int closeReason, int appSpecificErrorCode) {
                super.onChannelClosed(channel, closeReason, appSpecificErrorCode);
                switch (closeReason) {
                    case ChannelClient.ChannelCallback.CLOSE_REASON_DISCONNECTED:
                        writeDebug("channel closed: Disconnected");
                        break;
                    case ChannelClient.ChannelCallback.CLOSE_REASON_LOCAL_CLOSE:
                        writeDebug("channel closed: Local close");
                        break;
                    case ChannelClient.ChannelCallback.CLOSE_REASON_REMOTE_CLOSE:
                        writeDebug("channel closed: Remote close");
                        break;
                }
            }

            @Override
            public void onInputClosed(@NonNull ChannelClient.Channel channel, int closeReason, int appSpecificErrorCode) {
                super.onInputClosed(channel, closeReason, appSpecificErrorCode);
                switch (closeReason) {
                    case ChannelClient.ChannelCallback.CLOSE_REASON_NORMAL:
                        writeDebug("Input closed: Normal");
                        break;
                    case ChannelClient.ChannelCallback.CLOSE_REASON_DISCONNECTED:
                        writeDebug("Input closed: Disconnected");
                        break;
                    case ChannelClient.ChannelCallback.CLOSE_REASON_LOCAL_CLOSE:
                        writeDebug("Input closed: Local close");
                        break;
                    case ChannelClient.ChannelCallback.CLOSE_REASON_REMOTE_CLOSE:
                        writeDebug("Input closed: Remote close");
                        break;
                }
            }

            @Override
            public void onOutputClosed(@NonNull ChannelClient.Channel channel, int closeReason, int appSpecificErrorCode) {
                super.onOutputClosed(channel, closeReason, appSpecificErrorCode);
                switch (closeReason) {
                    case ChannelClient.ChannelCallback.CLOSE_REASON_NORMAL:
                        writeDebug("Output closed: Normal");
                        break;
                    case ChannelClient.ChannelCallback.CLOSE_REASON_DISCONNECTED:
                        writeDebug("Output closed: Disconnected");
                        break;
                    case ChannelClient.ChannelCallback.CLOSE_REASON_LOCAL_CLOSE:
                        writeDebug("Output closed: Local close");
                        break;
                    case ChannelClient.ChannelCallback.CLOSE_REASON_REMOTE_CLOSE:
                        writeDebug("Output closed: Remote close");
                        break;
                }
            }
        };
        channelClient = Wearable.getChannelClient(this);
        channelClient.registerChannelCallback(channelCallback);
    }

    private void startRecord() throws IOException {
        SensorEventListener listener = this;
        mSensorManager.registerListener(listener, heartIR, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(listener, heartRED, SensorManager.SENSOR_DELAY_FASTEST);
        writeDebug("Sensors initialized");
        fileToSend = new File(getFilesDir(), "raw_data.csv");

        boolean res = fileToSend.createNewFile();
        writeDebug("New file created? " + res);
        if (!res) {
            writeDebug("Deleting old file");
            writeDebug("Old file deleted? " + fileToSend.delete());
            writeDebug("New file created? " + fileToSend.createNewFile());
        }
        fileWriter = new FileWriter(fileToSend);
        String header = "Time,SensorTimestamp,Value,Sensor\n";
        fileWriter.append(header);
        valori += header;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXT_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    writeDebug("Permission granted");
                } else {
                    writeErrorDebug("Permissions NOT granted");
                }
            }
            break;
            case PERMISSIONS_REQUEST_BODY_SENSOR: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    writeDebug("Permission granted");

                } else {
                    writeErrorDebug("Permissions NOT granted");
                }
            }
            break;

        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }


    private void writeDebug(String message) {
        runOnUiThread(() -> {
            Log.d(TAG, message);
            debugger.setText(String.format("%s%s\n", debugger.getText(), message));
        });
    }


    private void writeErrorDebug(String message) {
        runOnUiThread(() -> {
            Log.e(TAG, message);
            debugger.setText(String.format("%s%s%s\n", debugger.getText(), "ERROR: ", message));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (channelClient != null && channelCallback != null)
            channelClient.registerChannelCallback(channelCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (channelClient != null && channelCallback != null)
            channelClient.unregisterChannelCallback(channelCallback);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (time > 2500) {
            time = 0;
            mSensorManager.unregisterListener(this);
            writeDebug("Finished");
        } else {
            writeDebug("" + time + "," + sensorEvent.values[0] + "," + sensorEvent.sensor.getName());
            saveData(time, sensorEvent.timestamp, sensorEvent.values[0], sensorEvent.sensor.getName());
            time++;
        }
    }

    private void saveData(int time, float timestamp, float value, String sensorName) {
        try {
            fileWriter.append(String.valueOf(time)).append(",").append(String.valueOf(timestamp)).append(",").append(String.valueOf(value)).append(",").append(sensorName).append("\n");
            valori += String.valueOf(time) + "," + String.valueOf(timestamp) + "," + String.valueOf(value) + "," + sensorName + "\n";
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        writeDebug(sensor.getName() + " has changed accuracy: " + i);
    }
}
