package thecave.forge.biowatchapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements DataClient.OnDataChangedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXT_STORAGE = 1;
    private static final String PATHNAME = "/datafile";

    TextView debugger;

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

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }


    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEvents) {
        writeDebug("onDataChanged: data changed");
        for (DataEvent event : dataEvents) {
            event.freeze();
            switch (event.getType()) {
                case DataEvent.TYPE_CHANGED:
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            DataItem item = event.getDataItem();
                            if (item.getUri().getPath().equals(PATHNAME)) {
                                writeDebug("equals true");
                                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                                saveFileOnExternalStorage(dataMap);
                            } else {
                                writeDebug("equals false, valore : " + item.getUri().getPath());
                            }
                        } catch (ExecutionException e) {
                            writeErrorDebug(e.getMessage());
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            writeErrorDebug(e.getMessage());
                            e.printStackTrace();
                        } catch (IOException e) {
                            writeErrorDebug(e.getMessage());
                            e.printStackTrace();
                        }
                    });
                    break;
                case DataEvent.TYPE_DELETED:
                    writeErrorDebug("DataEvent.TYPE_DELETED");
                    break;
            }
        }
    }

    private void saveFileOnExternalStorage(DataMap dataMap) throws ExecutionException, InterruptedException, IOException {
        writeDebug("Starting save Files on external Storage");
        for (String key : dataMap.keySet()) {
            InputStream inputStream = Tasks.await(Wearable.getDataClient(this).getFdForAsset(dataMap.getAsset(key))).getInputStream();
            if (inputStream == null) {
                writeErrorDebug("saveFileOnExternalStorage: inputStream == null, request unknown Asset ");
            } else {
                Files.copy(inputStream, new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), key).toPath(), StandardCopyOption.REPLACE_EXISTING);
                writeDebug("saveFileOnExternalStorage: file \"" + key + "\" copied on " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS));
                writeDebug("File copied!");
            }
        }
        writeDebug("File save complete");
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
            debugger.setText(String.format("%s%s%s\n", "ERROR: ", debugger.getText(), message));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
    }

}
