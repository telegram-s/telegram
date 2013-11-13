package org.telegram.android.video;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.*;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import org.telegram.android.R;
import org.telegram.android.log.Logger;
import org.telegram.android.ui.AspectRatioContainer;
import org.telegram.android.ui.TextUtil;
import org.telegram.android.views.VideoTimerView;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 19.09.13
 * Time: 13:10
 */
public class VideoRecorderActivity extends Activity implements SurfaceHolder.Callback, SensorEventListener {

    private static final String TAG = "Video";

    private SurfaceView surfaceView;
    private AspectRatioContainer ratioContainer;
    private MediaRecorder recorder;

    private ImageView recordButton;
    private VideoTimerView videoTimerView;

    private Orientation activityOrientation;
    private Orientation realOrientation;

    private SensorManager mSensorManager;
    private long startTime;
    private Handler handler = new Handler();
    private Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            if (recorder == null)
                return;
            videoTimerView.setTime(TextUtil.formatDuration((int) (SystemClock.uptimeMillis() - startTime) / 1000));
            handler.postDelayed(updateTimer, 1000);
        }
    };
    private CameraHolder cameraHolder;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Init orientation
        Display display = getWindowManager().getDefaultDisplay();
        switch (display.getRotation()) {
            default:
            case Surface.ROTATION_0:
                activityOrientation = Orientation.PORTRAIT;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Surface.ROTATION_180:
                activityOrientation = Orientation.PORTRAIT_REVERSE;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_90:
                activityOrientation = Orientation.LANDSCAPE;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Surface.ROTATION_270:
                activityOrientation = Orientation.LANDSCAPE_REVERSE;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
        }
        realOrientation = activityOrientation;

        if (Build.VERSION.SDK_INT >= 14) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }

        // Init camera
        cameraHolder = new CameraHolder(activityOrientation);
        cameraHolder.createCamera();

        // Init views
        setContentView(R.layout.video_record);

        recordButton = (ImageView) findViewById(R.id.recordButton);
        videoTimerView = (VideoTimerView) findViewById(R.id.videoTimer);
        ratioContainer = new AspectRatioContainer(this);
        ratioContainer.setAspectRatio(cameraHolder.getSurfaceRatio());
        ((ViewGroup) findViewById(R.id.mainContainer)).addView(ratioContainer);

        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recorder != null) {
                    completeRecording();
                } else {
                    startRecording();
                }
            }
        });

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        updateUiGravity();
    }


    private void startRecording() {
        int videoAngle;
        switch (realOrientation) {
            default:
            case PORTRAIT:
                videoAngle = 90;
                break;
            case PORTRAIT_REVERSE:
                videoAngle = 270;
                break;
            case LANDSCAPE:
                videoAngle = 0;
                break;
            case LANDSCAPE_REVERSE:
                videoAngle = 180;
                break;
        }

        cameraHolder.getCamera().unlock();

        recorder = new MediaRecorder();
        recorder.setCamera(cameraHolder.getCamera());
        recorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Logger.w(TAG, "Error: " + what + ", extra: " + extra);
            }
        });
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        if (Build.VERSION.SDK_INT >= 10) {
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        } else {
            recorder.setAudioSamplingRate(8000);
            recorder.setAudioEncodingBitRate(12200);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        }
        recorder.setOrientationHint(videoAngle);
        // recorder.setVideoFrameRate(24);
        recorder.setVideoEncodingBitRate(1200000);
        recorder.setVideoSize(cameraHolder.getWidth(), cameraHolder.getHeight());
        recorder.setMaxDuration(60 * 60 * 1000);

        recorder.setOutputFile(
                ((Uri) getIntent().getParcelableExtra(android.provider.MediaStore.EXTRA_OUTPUT)).getPath());
        recorder.setPreviewDisplay(surfaceView.getHolder().getSurface());
        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            abortRecording();
            return;
        }

        recordButton.setImageResource(R.drawable.video_shutter_recording);
        Logger.d(TAG, "Starting recording");
        recorder.start();
        Logger.d(TAG, "Started recording");
        Logger.d(TAG, "Started recording");

        mSensorManager.unregisterListener(VideoRecorderActivity.this);

        startTime = SystemClock.uptimeMillis();
        handler.removeCallbacks(updateTimer);
        handler.post(updateTimer);
    }

    private void completeRecording() {
        recorder.stop();
        cameraHolder.getCamera().lock();
        recorder.reset();
        recorder.release();
        recorder = null;
        setResult(RESULT_OK);
        finish();
    }

    private void abortRecording() {
        recorder.reset();
        recorder.release();
        recorder = null;
        Toast.makeText(getApplicationContext(), "Video recording aborted", Toast.LENGTH_SHORT).show();
    }


    private void onChangedOrientation(Orientation nOrientation) {
        if (recorder != null)
            return;
        if (realOrientation == nOrientation)
            return;
        realOrientation = nOrientation;

        updateUiGravity();
    }

    private void updateUiGravity() {
        switch (activityOrientation) {
            default:
            case PORTRAIT:
                switch (realOrientation) {
                    default:
                    case PORTRAIT:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_0);
                        break;
                    case LANDSCAPE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_90);
                        break;
                    case PORTRAIT_REVERSE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_180);
                        break;
                    case LANDSCAPE_REVERSE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_270);
                        break;
                }
                break;
            case PORTRAIT_REVERSE:
                switch (realOrientation) {
                    default:
                    case PORTRAIT:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_180);
                        break;
                    case LANDSCAPE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_270);
                        break;
                    case PORTRAIT_REVERSE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_0);
                        break;
                    case LANDSCAPE_REVERSE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_90);
                        break;
                }
                break;
            case LANDSCAPE:
                switch (realOrientation) {
                    default:
                    case PORTRAIT:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_270);
                        break;
                    case LANDSCAPE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_0);
                        break;
                    case PORTRAIT_REVERSE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_90);
                        break;
                    case LANDSCAPE_REVERSE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_180);
                        break;
                }
                break;
            case LANDSCAPE_REVERSE:
                switch (realOrientation) {
                    default:
                    case PORTRAIT:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_90);
                        break;
                    case LANDSCAPE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_180);
                        break;
                    case PORTRAIT_REVERSE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_270);
                        break;
                    case LANDSCAPE_REVERSE:
                        videoTimerView.setOrientation(VideoTimerView.ORIENTATION_0);
                        break;
                }
                break;
        }

        // Move button
        int gravity;
        switch (activityOrientation) {
            default:
            case LANDSCAPE_REVERSE:
                switch (realOrientation) {
                    default:
                    case PORTRAIT:
                    case LANDSCAPE:
                        gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                        break;
                    case PORTRAIT_REVERSE:
                    case LANDSCAPE_REVERSE:
                        gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                        break;
                }
                break;
            case LANDSCAPE:
                switch (realOrientation) {
                    default:
                    case PORTRAIT:
                    case LANDSCAPE:
                        gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                        break;
                    case PORTRAIT_REVERSE:
                    case LANDSCAPE_REVERSE:
                        gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                        break;
                }
                break;
            case PORTRAIT:
                switch (realOrientation) {
                    default:
                    case PORTRAIT:
                    case LANDSCAPE:
                        gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                        break;
                    case PORTRAIT_REVERSE:
                    case LANDSCAPE_REVERSE:
                        gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                        break;
                }
                break;
            case PORTRAIT_REVERSE:
                switch (realOrientation) {
                    default:
                    case PORTRAIT:
                    case LANDSCAPE:
                        gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                        break;
                    case PORTRAIT_REVERSE:
                    case LANDSCAPE_REVERSE:
                        gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                        break;
                }
                break;
        }

        recordButton.setLayoutParams(
                new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        gravity));
    }

    private void bindCamera() {
        try {
            cameraHolder.getCamera().setPreviewDisplay(surfaceView.getHolder());
        } catch (IOException e) {
            e.printStackTrace();
        }
        cameraHolder.getCamera().startPreview();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraHolder.createCamera();
        videoTimerView.setTime("Press start...");
        recordButton.setImageResource(R.drawable.video_shutter);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        surfaceView = new SurfaceView(this);
        ratioContainer.addView(surfaceView);
        surfaceView.getHolder().addCallback(this);
        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (recorder != null) {
            abortRecording();
        }
        cameraHolder.closeCamera();

        mSensorManager.unregisterListener(this);
        surfaceView.getHolder().removeCallback(this);
        surfaceView = null;
        ratioContainer.removeAllViews();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        bindCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (recorder != null)
            return;
        float len = (float) Math.sqrt(event.values[0] * event.values[0] +
                event.values[1] * event.values[1] +
                event.values[2] * event.values[2]);
        if (len < SensorManager.GRAVITY_EARTH / 2)
            return;

        float vertical = event.values[1] / len;
        float horizontal = event.values[0] / len;
        if (vertical > 0.7f) {
            if (realOrientation == Orientation.LANDSCAPE || realOrientation == Orientation.LANDSCAPE_REVERSE) {
                if (vertical < 0.85f)
                    return;
            }
            onChangedOrientation(Orientation.PORTRAIT);
        } else if (vertical < -0.7f) {
            if (realOrientation == Orientation.LANDSCAPE || realOrientation == Orientation.LANDSCAPE_REVERSE) {
                if (vertical > -0.85f)
                    return;
            }
            onChangedOrientation(Orientation.PORTRAIT_REVERSE);
        } else if (horizontal > 0.7f) {
            if (realOrientation == Orientation.PORTRAIT || realOrientation == Orientation.PORTRAIT_REVERSE) {
                if (horizontal < 0.85f)
                    return;
            }
            onChangedOrientation(Orientation.LANDSCAPE);
        } else if (horizontal < -0.7f) {
            if (realOrientation == Orientation.PORTRAIT || realOrientation == Orientation.PORTRAIT_REVERSE) {
                if (horizontal > -0.85f)
                    return;
            }
            onChangedOrientation(Orientation.LANDSCAPE_REVERSE);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}