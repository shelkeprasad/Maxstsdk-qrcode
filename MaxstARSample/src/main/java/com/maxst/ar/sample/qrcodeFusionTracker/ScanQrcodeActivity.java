package com.maxst.ar.sample.qrcodeFusionTracker;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.sample.R;
import com.maxst.ar.sample.util.TrackerResultListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ScanQrcodeActivity extends AppCompatActivity implements View.OnClickListener {

    private QrCodeFusionTrackerRenderer qrCodeTargetRenderer;
    private GLSurfaceView glSurfaceView;
    private TextView recognizedQrCodeView;
    private View guideView;
    private PlayerView playerView;
    private SimpleExoPlayer exoPlayer;
    private Set<String> detectedQRCodes = new HashSet<>();

    private boolean isQrCodeScanned = false;

    private String jsonData;
    private final List<TextView> dynamicSteps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan_qrcode);


        jsonData = getIntent().getStringExtra("jsonData");

        qrCodeTargetRenderer = new QrCodeFusionTrackerRenderer(this);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(qrCodeTargetRenderer);

        recognizedQrCodeView = (TextView) findViewById(R.id.recognized_QrCode);

        guideView = (View) findViewById(R.id.guideView);
        qrCodeTargetRenderer.listener = resultListener;

        playerView = new PlayerView(this);
        playerView.setId(View.generateViewId());
        playerView.setVisibility(View.GONE);


        exoPlayer = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        MaxstAR.init(this.getApplicationContext(), getResources().getString(R.string.app_key));
        MaxstAR.setScreenOrientation(getResources().getConfiguration().orientation);

    }

    @Override
    protected void onResume() {
        super.onResume();

        glSurfaceView.onResume();

        if (TrackerManager.getInstance().isFusionSupported()) {
            CameraDevice.getInstance().stop();
            CameraDevice.getInstance().setCameraApi(CameraDevice.CameraApi.Fusion);
            CameraDevice.getInstance().start(0, 1280, 720);
            TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_QR_FUSION);
            TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"Our mission is to expand human experience using virtual and augmented reality technologies.\", \"scale\":0.1}", false);
            TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"A metaverse provider that connects realistic virtual worlds with augmented reality.\", \"scale\":0.1}", false);


            TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"Step 1: Check the Functionality of Security Interlocks\", \"scale\":0.1}", false);
            TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"Step 2: Check the Electrical connections in the panel\", \"scale\":0.1}", false);
            TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"Step 3: Check the Suction manifold\", \"scale\":0.1}", false);
            TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"Step 4: Check the Lubrication system\", \"scale\":0.1}", false);


        } else {
            String message = TrackerManager.requestARCoreApk(this);
            if (message != null) {
                Log.i("MaxstAR", message);
            }
        }
        String message = TrackerManager.requestARCoreApk(this);
        Log.i("MaxstAR", "ARCore Installation Message: " + message);


        MaxstAR.onResume();

        if (exoPlayer != null && !exoPlayer.isPlaying()) {
            exoPlayer.play();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.pause();
        }

        glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
            }
        });

        glSurfaceView.onPause();

        CameraDevice.getInstance().stop();
        TrackerManager.getInstance().stopTracker();
        MaxstAR.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.pause();
        }

        TrackerManager.getInstance().destroyTracker();
        MaxstAR.deinit();
    }

    @Override
    public void onClick(View view) {

    }


    public void updateOverlayViewRight(Rect qrCodeBoundingBox) {
        if (isQrCodeScanned) {
            return;
        }

        if (qrCodeBoundingBox != null && jsonData != null) {
            int qrX = qrCodeBoundingBox.left;
            int qrY = qrCodeBoundingBox.top;
            int qrWidth = qrCodeBoundingBox.width();
            int qrHeight = qrCodeBoundingBox.height();

            isQrCodeScanned = true;
            if (jsonData != null) {
                try {
                    JSONObject jsonObject = new JSONObject(jsonData);
                    JSONArray textViews = jsonObject.getJSONArray("textViews");
                    JSONArray videos = jsonObject.getJSONArray("videos");
                    String position = jsonObject.optString("position", "right");
                    RelativeLayout rootLayout = findViewById(R.id.rootLayout);
                    for (TextView tv : dynamicSteps) {
                        rootLayout.removeView(tv);
                    }
                    dynamicSteps.clear();

                    for (int i = 0; i < textViews.length(); i++) {
                        String text = textViews.getString(i);
                        String videoPath = videos.optString(i); // same index for video

                        TextView tv = new TextView(this);
                        tv.setText(text);
                        tv.setTextColor(Color.WHITE);
                        tv.setTypeface(Typeface.DEFAULT_BOLD);
                        tv.setTextSize(16);
                        tv.setVisibility(View.VISIBLE);

                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);

                        if (position.equalsIgnoreCase("left")) {
                            params.leftMargin = qrX - 300;
                            params.topMargin = qrY + (i * 80);
                        } else if (position.equalsIgnoreCase("top")) {
                            params.leftMargin = qrX;
                            params.topMargin = qrY - (i * 80) - 60;
                        } else if (position.equalsIgnoreCase("right")) {
                            params.leftMargin = qrX + qrWidth + 150;
                            params.topMargin = qrY + (i * 80);
                        } else if (position.equalsIgnoreCase("bottom")) {
                            params.leftMargin = qrX;
                            params.topMargin = qrY + qrHeight + (i * 80);
                        } else if (position.equalsIgnoreCase("center")) {
                            int centerX = qrX + (qrWidth / 2);
                            int centerY = qrY + (qrHeight / 2);

                            switch (i) {
                                case 0:
                                    params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                                    params.topMargin = centerY - 200;
                                    break;
                                case 1:
                                    params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                                    params.topMargin = centerY + 100;
                                    break;
                                case 2:
                                    params.leftMargin = centerX - 400;
                                    params.topMargin = centerY;
                                    break;
                                case 3:
                                    params.leftMargin = centerX + 200;
                                    params.topMargin = centerY;
                                    break;
                                default:
                                    params.leftMargin = centerX + (i * 50);
                                    params.topMargin = centerY + (i * 50);
                                    break;
                            }
                        }


                        tv.setLayoutParams(params);

                        int finalI = i;
                        tv.setOnClickListener(v -> {
                            toggleStep(tv, text, videoPath);

                        });

                        rootLayout.addView(tv);
                        dynamicSteps.add(tv);

                    }

                    RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(750, 350);
                    videoParams.leftMargin = 90;
                    if (position.equalsIgnoreCase("bottom")) {
                        int spacing = 80;
                        int stepsHeight = textViews.length() * spacing;
                        videoParams.topMargin = qrY + qrHeight + stepsHeight + 100;
                    } else {
                        videoParams.topMargin = qrY + qrHeight + 400;
                    }

                    playerView.setLayoutParams(videoParams);

                    rootLayout.addView(playerView);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void toggleStep(TextView selectedStep, String stepText, String videoPath) {
        resetAllSteps();

        // If the selected step was already expanded, collapse it
        if (selectedStep.getMaxLines() == Integer.MAX_VALUE) {
            selectedStep.setText(stepText);
            selectedStep.setMaxLines(1);

            if (exoPlayer.isPlaying()) {
                exoPlayer.stop();
            }
            playerView.setVisibility(View.GONE);
        } else {
            selectedStep.setText(stepText + " : Showing");
            selectedStep.setMaxLines(Integer.MAX_VALUE);
            playerView.setVisibility(View.VISIBLE);
            setVideoStep(videoPath);
        }
    }

    private void resetAllSteps() {

        for (TextView step : dynamicSteps) {
            step.setMaxLines(1);
        }

        playerView.setVisibility(View.GONE);
        if (exoPlayer.isPlaying()) {
            exoPlayer.stop();
        }
    }

    private void setVideoStep(String videoPath) {

        if (exoPlayer.isPlaying()) {
            exoPlayer.stop();
        }
        exoPlayer.clearMediaItems();


        Uri videoUri = Uri.parse(videoPath);
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(videoUri)
                .build();

        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        playerView.setUseController(false);
        exoPlayer.play();


        playerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (exoPlayer.isPlaying()) {
                        exoPlayer.pause();
                    } else {
                        exoPlayer.play();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private TrackerResultListener resultListener = new TrackerResultListener() {
        @Override
        public void sendData(final String metaData) {
            (ScanQrcodeActivity.this).runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (detectedQRCodes.contains(metaData)) {
                        return;
                    }
                    detectedQRCodes.add(metaData);

                    recognizedQrCodeView.setText("QRCODE : " + metaData);
                }
            });
        }

        @Override
        public void sendFusionState(final int state) {
            (ScanQrcodeActivity.this).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    guideView.setVisibility(state == 1 ? View.INVISIBLE : View.VISIBLE);
                }
            });
        }
    };


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }

        MaxstAR.setScreenOrientation(newConfig.orientation);
    }
}



