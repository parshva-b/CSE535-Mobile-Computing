package com.example.cse5351;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    DatabaseHelper databaseHelper;
    private static final long DURATION_MS = 45000;

    // Sensor related variables
    private SensorManager sensorManager;
    private Sensor sensor;
    private boolean isReadingAcc = false;
    private Handler accHandler = new Handler();
    private List<Float> accX = new ArrayList<>(), accY = new ArrayList<>(), accZ = new ArrayList<>();
    int bps = 0;

    // Camera related variables
    private boolean isRecordingHR = false;
    private CameraManager cameraManager;
    private String cameraId;
    private static final int REQUEST_CAMERA_TAKE_VIDEO = 3891;
    private String heartRate;
    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(MainActivity.this);
        executorService = Executors.newCachedThreadPool();

        setupSymptomsScreen();
        setupRespiRateButton();
        setupHeartRateButton();
        setupUploadSigns();

        try {
            setupCamera();
        } catch (CameraAccessException e) {
            Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
            Log.e("Camera Access Exception", e.getMessage() + " " + e.getStackTrace());
        }
    }

    private void setupUploadSigns() {
        Button uploadSigns = findViewById(R.id.uploadSigns);
        uploadSigns.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                databaseHelper.setHeart_rate(heartRate);
                databaseHelper.setRespiratory_rate(String.valueOf(bps));
                databaseHelper.addItems();
                Toast.makeText(MainActivity.this, "Rates have been pushed to Database", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRespiRateButton() {
        Button respiRateBtn = findViewById(R.id.computeRespiRate);
        respiRateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isReadingAcc) startRespiratorySensors(respiRateBtn);
                else stopRespiratorySensors(respiRateBtn);
            }
        });
    }

    private void setupHeartRateButton() {
        Button heartRateBtn = findViewById(R.id.computeHeartRate);
        heartRateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isRecordingHR) startRecordingForHeartRate(heartRateBtn);
                else stopRecordingForHeartRate(heartRateBtn);
            }
        });
    }

    private void setupCamera() throws CameraAccessException {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        cameraId = cameraManager.getCameraIdList()[0];
    }

    private void startRecordingForHeartRate(Button recording) {
        isRecordingHR = true;
        recording.setClickable(false);

        // code to write intent to open camera and record intents
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 45);
        startActivityForResult(intent, REQUEST_CAMERA_TAKE_VIDEO);
    }

    private void stopRecordingForHeartRate(Button recording) {
        isRecordingHR = false;
        recording.setClickable(true);
    }

    private void setRespiRatoryRate(int bps) {
        TextView respiRateTV = findViewById(R.id.respiRateTextView);
        respiRateTV.setText(String.valueOf(bps));
    }

    private void stopRespiratorySensors(Button respiRateBtn) {
        if(accX.size() > 0) {
            bps = computeRespiRateCode();
            Log.d("STOP RESPIRATORY RATE", String.valueOf(bps));
            setRespiRatoryRate(bps);
        }


        isReadingAcc = false;
        respiRateBtn.setClickable(true);
        sensorManager.unregisterListener(this);
    }

    private void startRespiratorySensors(Button respiRateBtn) {
        Toast.makeText(this, "Start breathing with phone on chest for 45s", Toast.LENGTH_SHORT).show();

        isReadingAcc = true;
        respiRateBtn.setClickable(false);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        accHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this ,"Respiratory Rate computation has been stopped", Toast.LENGTH_SHORT).show();
                stopRespiratorySensors(respiRateBtn);
            }
        }, DURATION_MS);
    }

    private void setupSymptomsScreen() {
        Button symptomsBtn = findViewById(R.id.symptoms);
        symptomsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Switching to symptoms tab!", Toast.LENGTH_SHORT).show();

                Intent curIntent = new Intent(MainActivity.this, Symptoms_screen.class);
                MainActivity.this.startActivity(curIntent);
            }
        });
    }

    private void initializeVideoViewWithVideoCapture(Uri uri) {
        // video view set preview for XML
        VideoView videoView = findViewById(R.id.videoViewXML);
        videoView.setVideoURI(uri);
//        videoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setVolume(0f,0f);
            }
        });
        videoView.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CAMERA_TAKE_VIDEO) {
            if (resultCode == RESULT_OK) {
                Uri videoUri = data.getData();

                TextView heartRateTV = findViewById(R.id.heartRateTextView);
                heartRateTV.setText("loading...");

                Toast.makeText(this, "Video saved to:\n" + videoUri, Toast.LENGTH_LONG).show();

                initializeVideoViewWithVideoCapture(videoUri);
                Toast.makeText(this, "Task has started in background", Toast.LENGTH_SHORT).show();
                // call function to executeTask in a new thread so  as to not stop current processing
                computeHeartRateInBackground(this, videoUri);
            } else if (resultCode == RESULT_CANCELED)
                Toast.makeText(this, "Video request was cancelled", Toast.LENGTH_SHORT).show();
            else Toast.makeText(this, "Failed to record video", Toast.LENGTH_SHORT).show();
        }
    }

    private int computeRespiRateCode() {
        Log.d("Compute Respi Rate Code", String.valueOf(accX.size()));
        float previousValue = 0f;
        float currentValue = 0f;
        previousValue = 10f;
        int k = 0;

        for (int i = 11; i <= 450; i++) {
            currentValue = (float) Math.sqrt(
                    Math.pow(accX.get(i), 2.0) +
                            Math.pow(accY.get(i), 2.0) +
                            Math.pow(accZ.get(i), 2.0)
            );

            if (Math.abs(previousValue - currentValue) > 0.15) {
                k++;
            }

            previousValue = currentValue;
        }

        double ret = (double) k / 45.0;
        return (int) (ret * 30);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        accX.add(sensorEvent.values[0]);
        accY.add(sensorEvent.values[1]);
        accZ.add(sensorEvent.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // NO NEED HERE
    }

    private void setHeartRateValue(String rate) {
        TextView heartRateTV = findViewById(R.id.heartRateTextView);
        heartRateTV.setText(rate);

        Toast.makeText(MainActivity.this, "Heart Rate computation done", Toast.LENGTH_SHORT).show();
    }

    private void computeHeartRateInBackground(Context context, Uri uri) {
        final String[] heartRate = new String[1];

        Handler handler = new Handler(Looper.getMainLooper());

        Runnable uiUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                setHeartRateValue(heartRate[0]);
            }
        };

        Runnable backgroundRunnable = new Runnable() {
            @Override
            public void run() {
                heartRate[0] = runTaskInBackground(context, uri);

                handler.post(uiUpdateRunnable);
            }
        };

        executorService.submit(backgroundRunnable);
    }

    private String runTaskInBackground(Context context, Uri uri) {
        Log.d("heartRate in background", "task started");
        Bitmap m_bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        List<Bitmap> frameList = new ArrayList<>();

        try {
            retriever.setDataSource(context, uri);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT);
            int aduration = Integer.parseInt(duration);
            int i = 10;

            while (i < aduration) {
                Log.d("heartRate in background", "inside loop" + i);
                Bitmap bitmap = retriever.getFrameAtIndex(i);
                frameList.add(bitmap);
                i += 5;
            }
        } catch (Exception e) {
            // Handle exception if needed
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.e("SlowTask error --", e.getMessage() + " " + e.getStackTrace());
            }
            long redBucket = 0;
            long pixelCount = 0;
            List<Long> a = new ArrayList<>();

            for (Bitmap i : frameList) {
                redBucket = 0;
                for (int y = 550; y < 650; y++) {
                    for (int x = 550; x < 650; x++) {
                        int c = i.getPixel(x, y);
                        pixelCount++;
                        redBucket += Color.red(c) + Color.blue(c) + Color.green(c);
                    }
                }
                a.add(redBucket);
            }

            List<Long> b = new ArrayList<>();
            for (int i = 0; i < a.size() - 5; i++) {
                long temp = (a.get(i) + a.get(i + 1) + a.get(i + 2) + a.get(i + 3) + a.get(i + 4)) / 4;
                b.add(temp);
            }

            long x = b.get(0);
            int count = 0;
            for (int i = 1; i < b.size(); i++) {
                long p = b.get(i);
                if ((p - x) > 3500) {
                    count++;
                }
                x = b.get(i);
            }

            int rate = (int) (((float) count / 45) * 60);
            heartRate = String.valueOf(rate / 2);
            Log.d("heartRate in background", "task ended"+ heartRate);
            return heartRate;
        }
    }
}