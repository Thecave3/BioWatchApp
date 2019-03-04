package thecave.forge.biowatchapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.ChannelClient;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXT_STORAGE = 1;
    private static final String FILE_EXCHANGE_PATH = "/file_exchange";
    private static final int PERMISSIONS_REQUEST_BODY_SENSOR = 2;
    private TextView debugger;


    ChannelClient.ChannelCallback channelCallback;
    ChannelClient channelClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        debugger = findViewById(R.id.debugger);


        if (!isExternalStorageWritable() || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXT_STORAGE);
            writeDebug("Need to ask permissions");
        } else {
            writeDebug("Permission granted");
        }


        if (checkSelfPermission(Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            writeDebug("Permission heart not granted, asking for");
            requestPermissions(
                    new String[]{Manifest.permission.BODY_SENSORS}, PERMISSIONS_REQUEST_BODY_SENSOR);
        } else {
            writeDebug("Permission heart already granted");
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
}
