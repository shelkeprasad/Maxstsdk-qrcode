package com.maxst.ar.sample.qrcodeFusionTracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.gson.JsonObject;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.sample.R;
import com.maxst.ar.sample.util.TrackerResultListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


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
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private int sopId;
    private String stepId,token;

    private ImageView imageViewRef;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qrcode);

        try {
            JSONObject jsonObject = new JSONObject(getIntent().getStringExtra("jsonData"));
            stepId = jsonObject.getString("stepId");
            token = jsonObject.getString("token");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://x1dwvr9k-8000.inc1.devtunnels.ms/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
            Toast.makeText(this, "Camera permission is required for scanning.", Toast.LENGTH_LONG).show();

        } else {
            initializeARComponents();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeARComponents();
            } else {
                Toast.makeText(this, "Camera permission is required for scanning.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initializeARComponents() {
      /*  jsonData = getIntent().getStringExtra("jsonData");
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
           sopId = jsonObject.getInt("sopId");
            Log.d("JSON_DEBUG", "Full JSON:\n" + jsonObject.toString(4)); // pretty print (indent 4)
        } catch (Exception e) {
            throw new RuntimeException(e);
        }*/


        qrCodeTargetRenderer = new QrCodeFusionTrackerRenderer(this);
        glSurfaceView = findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(qrCodeTargetRenderer);

        recognizedQrCodeView = findViewById(R.id.recognized_QrCode);
        guideView = findViewById(R.id.guideView);
        qrCodeTargetRenderer.listener = resultListener;

        playerView = new PlayerView(this);
        playerView.setId(View.generateViewId());
        exoPlayer = new SimpleExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        // ✅ Initialize MaxstAR only after permission granted
        MaxstAR.init(getApplicationContext(), getResources().getString(R.string.app_key));
        MaxstAR.setScreenOrientation(getResources().getConfiguration().orientation);

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


    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

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

    private String loadJSONFromAssets(String fileName) {
        String json = null;
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    private JSONObject getSopById(int sopId) {
        try {
            String jsonData = loadJSONFromAssets("sops.json");
            JSONObject root = new JSONObject(jsonData);
            JSONArray sopsArray = root.getJSONArray("sops");
            for (int i = 0; i < sopsArray.length(); i++) {
                JSONObject sopObj = sopsArray.getJSONObject(i);
                if (sopObj.getInt("sopId") == sopId) {
                    return sopObj;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONArray getAllItems() {
        try {
            String jsonData = loadJSONFromAssets("items.json");
            JSONObject root = new JSONObject(jsonData);
            return root.getJSONArray("items");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private JSONObject getItemById(String id) {
        try {
            String jsonData = loadJSONFromAssets("items.json");
            JSONObject root = new JSONObject(jsonData);
            JSONArray itemsArray = root.getJSONArray("items");

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemObj = itemsArray.getJSONObject(i);
                if (itemObj.getString("id").equals(id)) {
                    return itemObj;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public void updateOverlayViewRight(Rect qrCodeBoundingBox) {
        if (isQrCodeScanned) return;
        Call<JsonObject> call = apiService.getStepItems(stepId, token);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {

                        JSONObject apiResponse = new JSONObject(response.body().toString());

                        JSONObject dataObj = apiResponse.optJSONObject("data");
                        if (dataObj == null) return;
                        JSONObject arViewObj = dataObj.optJSONObject("arview");
                        if (arViewObj == null) return;
                        JSONArray itemsArray = arViewObj.optJSONArray("items");
                        if (itemsArray == null || qrCodeBoundingBox == null) return;

                        isQrCodeScanned = true;
                        RelativeLayout rootLayout = findViewById(R.id.rootLayout);

                        for (int i = 0; i < itemsArray.length(); i++) {
                            JSONObject itemData = itemsArray.getJSONObject(i);
                            String type = itemData.getString("type");
                            String content = itemData.getString("content");
                            JSONObject position = itemData.getJSONObject("position");
                            JSONObject props = itemData.getJSONObject("properties");

                            float x = (float) position.getDouble("x");
                            float y = (float) position.getDouble("y");

                            String visibility = props.optString("visibility", "visible");
                            int viewVisibility = visibility.equalsIgnoreCase("visible") ? View.VISIBLE : View.GONE;

                            if (type.equalsIgnoreCase("text")) {
                                TextView tv = new TextView(ScanQrcodeActivity.this);
                                tv.setText(content);
                                tv.setTextSize(props.optInt("fontSize", 16));
                                tv.setTextColor(Color.parseColor(props.optString("color", "#FFFFFF")));
                                tv.setVisibility(viewVisibility);

                                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT);
                                tv.measure(0, 0);
                                int textWidth = tv.getMeasuredWidth();
                                params.leftMargin = (int) (x - textWidth / 2);
                                params.topMargin = (int) y;

                                rootLayout.addView(tv, params);

                            } else if (type.equalsIgnoreCase("image")) {
                                TextView tvImg = new TextView(ScanQrcodeActivity.this);
                                String title = itemData.optString("title", "Tap to view image");
                                tvImg.setText(title);
                                tvImg.setTextSize(props.optInt("fontSize", 16));
                                tvImg.setTextColor(Color.parseColor(props.optString("color", "#FFFFFF")));
                                tvImg.setVisibility(View.VISIBLE);

                                RelativeLayout.LayoutParams tvImgParams = new RelativeLayout.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                );
                                tvImgParams.leftMargin = (int) x;
                                tvImgParams.topMargin = (int) y;
                                rootLayout.addView(tvImg, tvImgParams);

                                ImageView img = new ImageView(ScanQrcodeActivity.this);
                                Glide.with(ScanQrcodeActivity.this).load(content).into(img);
                                img.setVisibility(viewVisibility);
                                imageViewRef = img;

                                int width = (int) TypedValue.applyDimension(
                                        TypedValue.COMPLEX_UNIT_DIP,
                                        (float) props.optDouble("width", 200),
                                        getResources().getDisplayMetrics());
                                int height = (int) TypedValue.applyDimension(
                                        TypedValue.COMPLEX_UNIT_DIP,
                                        (float) props.optDouble("height", 200),
                                        getResources().getDisplayMetrics());

                                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
                                params.leftMargin = (int) x;
                                params.topMargin = (int) y +120;

                                rootLayout.addView(img, params);

                                tvImg.setOnClickListener(v -> {
                                    if (img.getVisibility() == View.VISIBLE) {
                                        img.setVisibility(View.GONE);
                                    } else {
                                        img.setVisibility(View.VISIBLE);
                                        img.bringToFront();
                                    }
                                });

                            } else if (type.equalsIgnoreCase("video")) {
                                TextView tvVid = new TextView(ScanQrcodeActivity.this);
                                String title = itemData.optString("title", "Tap to play video");
                                tvVid.setText(title);
                                tvVid.setTextSize(props.optInt("fontSize", 16));
                                tvVid.setTextColor(Color.parseColor(props.optString("color", "#FFFFFF")));
                                tvVid.setVisibility(View.VISIBLE);

                                RelativeLayout.LayoutParams tvVidParams = new RelativeLayout.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                );
                                tvVidParams.leftMargin = (int) x;
                                tvVidParams.topMargin = (int) y;
                                rootLayout.addView(tvVid, tvVidParams);

                                int width = (int) (props.optDouble("width", 3.0) * 150);
                                int height = (int) (props.optDouble("height", 2.0) * 150);

                                if (playerView.getParent() == null) {
                                    rootLayout.addView(playerView);
                                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
                                    params.leftMargin = (int) x;
                                    params.topMargin = (int) y;
                                    playerView.setLayoutParams(params);
                                }
                                playerView.setUseController(false);

                                Uri uri = Uri.parse(content);
                                MediaItem mediaItem = MediaItem.fromUri(uri);
                                exoPlayer.setMediaItem(mediaItem);
                                exoPlayer.setPlayWhenReady(false);
                                exoPlayer.prepare();
                                playerView.bringToFront();
                                playerView.setVisibility(viewVisibility);

                                tvVid.setOnClickListener(v -> {
                                    if (playerView.getVisibility() == View.VISIBLE) {
                                        playerView.setVisibility(View.GONE);
                                        if (exoPlayer.isPlaying()) {
                                            exoPlayer.pause();
                                        }
                                    } else {
                                        playerView.setVisibility(View.VISIBLE);
                                        playerView.bringToFront();
                                        if (!exoPlayer.isPlaying()) {
                                            exoPlayer.play();
                                        }
                                    }
                                });

                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("ScanQrcodeActivity", "updateOverlay error: " + e.getMessage());
                    }
                }else {
                    Log.e("API_ERROR", "HTTP code: " + response.code()
                            + ", message: " + response.message()
                            + ", body: " + response.errorBody());
                    Toast.makeText(ScanQrcodeActivity.this,
                            "API call failed: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(ScanQrcodeActivity.this, "API call failed", Toast.LENGTH_SHORT).show();
            }
        });

    }










    // todo asset json

  /*  public void updateOverlayViewRight(Rect qrCodeBoundingBox) {
        if (isQrCodeScanned) return;

        JSONArray itemsArray = getAllItems();

        if (qrCodeBoundingBox == null || itemsArray == null) return;
        isQrCodeScanned = true;
        try {
            RelativeLayout rootLayout = findViewById(R.id.rootLayout);

            for (int i = 0; i < itemsArray.length(); i++) {
                JSONObject itemData = itemsArray.getJSONObject(i);
                String type = itemData.getString("type");
                String content = itemData.getString("content");
                JSONObject position = itemData.getJSONObject("position");
                JSONObject props = itemData.getJSONObject("properties");

                float x = (float) position.getDouble("x");
                float y = (float) position.getDouble("y");

                String visibility = props.optString("visibility", "visible");
                int viewVisibility = visibility.equalsIgnoreCase("visible") ? View.VISIBLE : View.GONE;

                if (type.equalsIgnoreCase("text")) {
                    TextView tv = new TextView(this);
                    tv.setText(content);
                    tv.setTextSize(props.optInt("fontSize", 16));
                    tv.setTextColor(Color.parseColor(props.optString("color", "#FFFFFF")));
                    tv.setVisibility(viewVisibility);

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);


                    tv.measure(0, 0);
                    int textWidth = tv.getMeasuredWidth();

                    // Center the text horizontally on 'x'
                    params.leftMargin = (int) (x - textWidth / 2);
                    params.topMargin = (int) y;

                    rootLayout.addView(tv, params);
                } else if (type.equalsIgnoreCase("image")) {

                    TextView tvImg = new TextView(this);
                    String title = itemData.optString("title", "Tap to view image");
                    tvImg.setText(title);
                    tvImg.setTextSize(props.optInt("fontSize", 16));
                    tvImg.setTextColor(Color.parseColor(props.optString("color", "#FFFFFF")));
                    tvImg.setVisibility(View.VISIBLE);

                    RelativeLayout.LayoutParams tvImgParams = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    tvImgParams.leftMargin = (int) x;
                    tvImgParams.topMargin = (int) y;
                    rootLayout.addView(tvImg, tvImgParams);

                    ImageView img = new ImageView(this);
                    Glide.with(this).load(content).into(img);
                    img.setVisibility(viewVisibility);
                    imageViewRef = img;

                    int width = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            (float) props.optDouble("width", 200),
                            getResources().getDisplayMetrics());
                    int height = (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            (float) props.optDouble("height", 200),
                            getResources().getDisplayMetrics());

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
                    params.leftMargin = (int) x;
                    params.topMargin = (int) y +120;

                    rootLayout.addView(img, params);

                    tvImg.setOnClickListener(v -> {
                        if (img.getVisibility() == View.VISIBLE) {
                            img.setVisibility(View.GONE);
                        } else {
                            img.setVisibility(View.VISIBLE);
                            img.bringToFront();
                        }
                    });

                } else if (type.equalsIgnoreCase("video")) {

                    TextView tvVid = new TextView(this);
                    String title = itemData.optString("title", "Tap to play video");
                    tvVid.setText(title);
                    tvVid.setTextSize(props.optInt("fontSize", 16));
                    tvVid.setTextColor(Color.parseColor(props.optString("color", "#FFFFFF")));
                    tvVid.setVisibility(View.VISIBLE);

                    RelativeLayout.LayoutParams tvVidParams = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );
                    tvVidParams.leftMargin = (int) x;
                    tvVidParams.topMargin = (int) y;
                    rootLayout.addView(tvVid, tvVidParams);

                    int width = (int) (props.optDouble("width", 3.0) * 150);
                    int height = (int) (props.optDouble("height", 2.0) * 150);

                    if (playerView.getParent() == null) {
                        rootLayout.addView(playerView);
                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, height);
                        params.leftMargin = (int) x;
                        params.topMargin = (int) y;
                        playerView.setLayoutParams(params);
                    }
                    playerView.setUseController(false);

                    Uri uri = Uri.parse(content);
                    MediaItem mediaItem = MediaItem.fromUri(uri);
                    exoPlayer.setMediaItem(mediaItem);
                    exoPlayer.setPlayWhenReady(false);
                    exoPlayer.prepare();
                    playerView.bringToFront();
                    playerView.setVisibility(viewVisibility);

                    tvVid.setOnClickListener(v -> {
                        if (playerView.getVisibility() == View.VISIBLE) {
                            playerView.setVisibility(View.GONE);
                            if (exoPlayer.isPlaying()) {
                                exoPlayer.pause();
                            }
                        } else {
                            playerView.setVisibility(View.VISIBLE);
                            playerView.bringToFront();
                            if (!exoPlayer.isPlaying()) {
                                exoPlayer.play();
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("ScanQrcodeActivity", "updateOverlay error: " + e.getMessage());
        }
    }*/


/*    public void updateOverlayViewRight(Rect qrCodeBoundingBox) {
        if (isQrCodeScanned) {
            return;
        }

        JSONObject sopData = getSopById(sopId);

        if (qrCodeBoundingBox != null && jsonData != null) {
            int qrX = qrCodeBoundingBox.left;
            int qrY = qrCodeBoundingBox.top;
            int qrWidth = qrCodeBoundingBox.width();
            int qrHeight = qrCodeBoundingBox.height();

            isQrCodeScanned = true;

            try {
                if (sopData != null && qrCodeBoundingBox != null) {

                    JSONArray stepsArray = sopData.getJSONArray("steps");
                    RelativeLayout rootLayout = findViewById(R.id.rootLayout);

                    for (TextView tv : dynamicSteps) {
                        rootLayout.removeView(tv);
                    }
                    dynamicSteps.clear();

                    String position = sopData.optString("position", "bottom");

                    for (int i = 0; i < stepsArray.length(); i++) {
                        JSONObject stepObj = stepsArray.getJSONObject(i);
                        String stepText = stepObj.getString("step");
                        String videoName = stepObj.getString("video");

                        int resId = getResources().getIdentifier(videoName, "raw", getPackageName());
                        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + resId);

                        TextView tv = new TextView(this);
                        tv.setText(stepText);
                        tv.setTextColor(Color.GREEN);
                        tv.setTypeface(Typeface.DEFAULT_BOLD);
                        tv.setTextSize(16);
                        tv.setVisibility(View.VISIBLE);

                        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);

                        if (position.equalsIgnoreCase("left")) {
                            params.leftMargin = qrX - 300;
                            params.topMargin = qrY + (i * 80);
                        }
                        else if (position.equalsIgnoreCase("top")) {
                            params.leftMargin = qrX;
                            params.topMargin = qrY - (i * 80) - 60;
                        }
                        else if (position.equalsIgnoreCase("right")) {
                            params.leftMargin = qrX + qrWidth + 150;
                            params.topMargin = qrY + (i * 80);
                        }else if (position.equalsIgnoreCase("bottom")) {

                            int extraOffset = (int) TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP,
                                    100, // dp value
                                    getResources().getDisplayMetrics()
                            );


                            params.topMargin = qrY + qrHeight + extraOffset + (i * 90);
                            params.leftMargin = 50;
                            params.rightMargin = 50;
                        }

                        else if (position.equalsIgnoreCase("center")) {
                            int centerX = qrX + (qrWidth / 2);
                            int centerY = qrY + (qrHeight / 2);

                                if (i == 0) {
                                    params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                                    params.topMargin = centerY - 200;
                                } else if (i == 1) {
                                    params.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                                    params.topMargin = centerY + 100;
                                } else if (i == 2) {
                                    params.leftMargin = centerX - 400;
                                    params.topMargin = centerY;
                                } else if (i == 3) {
                                    params.leftMargin = centerX + 200;
                                    params.topMargin = centerY;
                                } else {
                                    params.leftMargin = centerX + (i * 50);
                                    params.topMargin = centerY + (i * 50);
                                }
                        }else {
                            params.leftMargin = qrX + qrWidth + 150;
                            params.topMargin = qrY + (i * 80);

                        }

                        tv.setLayoutParams(params);

                        int finalI = i;
                        tv.setOnClickListener(v -> {
                            playerView.setVisibility(View.VISIBLE);
                            exoPlayer.setMediaItem(MediaItem.fromUri(videoUri));
                            exoPlayer.prepare();
                            exoPlayer.play();
                            playerView.setUseController(false);

                        });

                        rootLayout.addView(tv);
                        dynamicSteps.add(tv);
                    }

                    if (!dynamicSteps.isEmpty()) {
                        TextView lastStep = dynamicSteps.get(dynamicSteps.size() - 1);

                        lastStep.post(() -> {
                            // Get position within parent layout
                            int[] stepLocation = new int[2];
                            int[] rootLocation = new int[2];
                            lastStep.getLocationOnScreen(stepLocation);
                            rootLayout.getLocationOnScreen(rootLocation);

                            int lastStepTopInParent = stepLocation[1] - rootLocation[1];
                            int lastStepBottom = lastStepTopInParent + lastStep.getHeight();

                            // Get lastStep width and center X
                            int stepWidth = lastStep.getWidth();
                            int stepCenterX = (stepLocation[0] - rootLocation[0]) + (stepWidth / 2);

                            // Measure video width to center properly
                            int videoWidth = 900;
                            int centeredLeft = stepCenterX - (videoWidth / 2);

                            // Place the video just below the last TextView
                            RelativeLayout.LayoutParams dynamicVideoParams = new RelativeLayout.LayoutParams(videoWidth, 450);
                            dynamicVideoParams.leftMargin = Math.max(centeredLeft, 0);
                            dynamicVideoParams.topMargin = lastStepBottom + 20; // 20px gap

                            playerView.setLayoutParams(dynamicVideoParams);

                            // Add playerView if not already added
                            if (playerView.getParent() == null) {
                                rootLayout.addView(playerView);
                            }
                        });
                    } else {
                        // Fallback: if no steps, place video below QR code
                        RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(750, 350);
                        int qrCenterX = qrX + (qrWidth / 2);
                        int centeredLeft = qrCenterX - (750 / 2);
                        videoParams.leftMargin = Math.max(centeredLeft, 0);
                        videoParams.topMargin = qrY + qrHeight + 40;
                        playerView.setLayoutParams(videoParams);
                        rootLayout.addView(playerView);
                    }


                }

            } catch (Exception e) {
                e.printStackTrace();
            }



        }
    }*/

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




/*    private void toggleStep(TextView selectedStep, String stepText, String videoPath) {
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
    }*/

    private void resetAllSteps() {

        for (TextView step : dynamicSteps) {
            String originalText = (String) step.getTag();
            if (originalText != null) {
                step.setText(originalText);
            }
            step.setMaxLines(1);
        }

        playerView.setVisibility(View.GONE);
        if (exoPlayer.isPlaying()) {
            exoPlayer.stop();
        }
    }

    private void setVideoStep(String videoPath) {
        try {
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

            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e("EXOPLAYER_ERROR", "Playback failed", error);
                }
            });

        } catch (Exception e) {
            Log.e("VIDEO_ERROR", "Exception while playing video", e);
        }
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



