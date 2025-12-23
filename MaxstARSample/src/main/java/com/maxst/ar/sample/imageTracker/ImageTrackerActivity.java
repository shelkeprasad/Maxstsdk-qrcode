/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */

package com.maxst.ar.sample.imageTracker;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.Matrix;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.ResultCode;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.sample.ModelConfig;
import com.maxst.ar.sample.R;
import com.maxst.ar.sample.TrackerConfig;
import com.maxst.ar.sample.util.SampleUtil;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class ImageTrackerActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageTrackerRenderer imageTargetRenderer;
    private GLSurfaceView glSurfaceView;
    private int preferCameraResolution = 0;
    private SceneView sceneView;
    private Scene scene;

    public int surfaceWidth = 0;
    public int surfaceHeight = 0;
    private FrameLayout labelContainer;
    private final Map<String, TextView> labelViews = new HashMap<>();

    public float[] projectionMatrix = new float[16];
    public String activeLabel = null;
    public float[] smoothedPose = null;
    private float SMOOTH_FACTOR = 0.2f;

    public Map<String, TrackerConfig> trackerConfigs = new HashMap<>();
    private Map<String, ModelConfig> modelConfigById = new HashMap<>();
    private Node rootNode;
    private Map<String, Node> modelNodes = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_tracker);
        labelContainer = findViewById(R.id.container);
        findViewById(R.id.normal_tracking).setOnClickListener(this);
        findViewById(R.id.extended_tracking).setOnClickListener(this);
        findViewById(R.id.multi_tracking).setOnClickListener(this);

        imageTargetRenderer = new ImageTrackerRenderer(this, this, trackerConfigs);
        glSurfaceView = findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        MaxstAR.init(this.getApplicationContext(), getResources().getString(R.string.app_key));

        MaxstAR.setScreenOrientation(getResources().getConfiguration().orientation);
        if (Build.MANUFACTURER.equals("vuzix")) {
            CameraDevice.getInstance().flipVideo(CameraDevice.FlipDirection.HORIZONTAL, true);
            CameraDevice.getInstance().flipVideo(CameraDevice.FlipDirection.VERTICAL, true);
        }

        loadViewRenderJson();

        TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_IMAGE);
//        TrackerManager.getInstance().addTrackerData("ImageTarget/Glacier.2dmap", true);
//        TrackerManager.getInstance().addTrackerData("ImageTarget/Lego.2dmap", true);
//        TrackerManager.getInstance().addTrackerData("ImageTarget/Blocks.2dmap", true);
        //  TrackerManager.getInstance().addTrackerData("ImageTarget/FetteMachine.2dmap", true);

        for (TrackerConfig cfg : trackerConfigs.values()) {
            if (cfg.mapPath != null && !cfg.mapPath.isEmpty()) {
                TrackerManager.getInstance().addTrackerData(cfg.mapPath, true);
            }
        }
//        TrackerManager.getInstance().loadTrackerData();

        for (TrackerConfig cfg : trackerConfigs.values()) {
            if (cfg.assets != null && cfg.assets.models != null) {
                for (ModelConfig m : cfg.assets.models) {
                    modelConfigById.put(m.id, m);
                }
            }
        }
        initSceneformOverlay();

        TrackerManager.getInstance().addTrackerData("ImageTarget/Lego.2dmap", true);

//		TrackerManager.getInstance().addTrackerData("{\"image\":\"add_image\",\"image_path\":\"ImageTarget/Blocks.png\",\"image_width\":0.26,\"inclusion\":[{\"x\":50, \"y\":100, \"width\":400, \"height\":400}, {\"x\":400, \"y\":80, \"width\":400, \"height\":400}], \"exclusion\":[{\"x\":200, \"y\":200, \"width\":150, \"height\":150}]}", true);
        //TrackerManager.getInstance().addTrackerData("{\"image\":\"add_image\",\"image_path\":\"ImageTarget/Blocks.png\",\"image_width\":0.26}", true);
        //TrackerManager.getInstance().addTrackerData("{\"image\":\"add_image\",\"image_path\":\"ImageTarget/Glacier.png\",\"image_width\":0.26}", true);
        //TrackerManager.getInstance().addTrackerData("{\"image\":\"add_image\",\"image_path\":\"/sdcard/Download/sample/Blocks.png\",\"image_width\":0.26}", false);
        //TrackerManager.getInstance().addTrackerData("{\"image\":\"add_image\",\"image_path\":\"/sdcard/Download/sample/Glacier.png\",\"image_width\":0.26}", false);
        TrackerManager.getInstance().loadTrackerData();

        preferCameraResolution = getSharedPreferences(SampleUtil.PREF_NAME, Activity.MODE_PRIVATE).getInt(SampleUtil.PREF_KEY_CAM_RESOLUTION, 0);

        imageTargetRenderer = new ImageTrackerRenderer(this, this, trackerConfigs);
        glSurfaceView.setRenderer(imageTargetRenderer);


        sceneView.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                float x = ev.getX();
                float y = ev.getY();
                return imageTargetRenderer.tapVideo(x, y, surfaceWidth, surfaceHeight);
            }
            return false;
        });
    }

    private void loadViewRenderJson() {
        try {
            InputStream is = getAssets().open("viewrender.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(json, JsonObject.class);
            JsonArray trackers = root.getAsJsonArray("trackers");
            for (JsonElement t : trackers) {
                TrackerConfig cfg = gson.fromJson(t, TrackerConfig.class);
                Log.d("TrackerConfig", "Tracker: " + cfg.name);

                if (cfg.assets != null) {
                    if (cfg.assets.models != null) {
                        Log.d("TrackerConfig", cfg.name + " models count = " + cfg.assets.models.size());
                    }
                    if (cfg.assets.videos != null) {
                        Log.d("TrackerConfig", cfg.name + " videos count = " + cfg.assets.videos.size());
                    }
                    if (cfg.assets.labels != null) {
                        Log.d("TrackerConfig", cfg.name + " labels count = " + cfg.assets.labels.size());
                    }
                } else {
                    Log.w("TrackerConfig", "No assets for " + cfg.name);
                }
                trackerConfigs.put(cfg.name, cfg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initSceneformOverlay() {
        sceneView = findViewById(R.id.sceneView);
        sceneView.setTransparent(true);
        sceneView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        scene = sceneView.getScene();
        rootNode = new Node();
        scene.addChild(rootNode);

        for (ModelConfig mcfg : modelConfigById.values()) {
            Node modelNode = new Node();
            modelNode.setEnabled(false);
            rootNode.addChild(modelNode);

            modelNodes.put(mcfg.id, modelNode);

            loadModelWithAnimation(mcfg.path, modelNode, mcfg);
        }
    }

    private void loadModelWithAnimation(String path, Node node, ModelConfig mcfg) {
        Uri uri = Uri.parse(path);
        ModelRenderable.builder()
                .setSource(this, uri)
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> {
                    node.setRenderable(renderable);
                    RenderableInstance instance = node.getRenderableInstance();
                    if (instance == null) return;
                    List<String> anims = instance.getAnimationNames();
                    if (anims.isEmpty()) {
                        Log.w("ARAnimation", "No animations in: " + path);
                        return;
                    }

                    if (mcfg.animation != null && mcfg.animation.autoPlay) {
                        int index = mcfg.animation.animationIndex;
                        if (index < 0 || index >= anims.size()) index = 0;

                        String animName = anims.get(index);

                        ObjectAnimator animator =
                                ModelAnimator.ofAnimation(instance, animName);

                        if (mcfg.animation.loop)
                            animator.setRepeatCount(ValueAnimator.INFINITE);
                        animator.setDuration(mcfg.animation.duration);
                        animator.start();
                    }
                    Log.d("Sceneform", "Loaded model: " + mcfg.id);
                })
                .exceptionally(t -> {
                    t.printStackTrace();
                    Log.e("Sceneform model render error ", "Model load failed: " + path);
                    return null;
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        glSurfaceView.onResume();
        TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_IMAGE);

        ResultCode resultCode = ResultCode.Success;
        switch (preferCameraResolution) {
            case 0:
                resultCode = CameraDevice.getInstance().start(0, 640, 480);
                break;

            case 1:
                resultCode = CameraDevice.getInstance().start(0, 1280, 720);
                break;

            case 2:
                resultCode = CameraDevice.getInstance().start(0, 1920, 1080);
                break;
        }

        if (resultCode != ResultCode.Success) {
            Toast.makeText(this, R.string.camera_open_fail, Toast.LENGTH_SHORT).show();
            finish();
        }
        MaxstAR.onResume();
        if (sceneView != null) {
            try {
                sceneView.resume();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        glSurfaceView.queueEvent(() -> imageTargetRenderer.destroyVideoPlayer());

        glSurfaceView.onPause();

        TrackerManager.getInstance().stopTracker();
        CameraDevice.getInstance().stop();
        MaxstAR.onPause();
        if (sceneView != null) {
            sceneView.pause();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TrackerManager.getInstance().destroyTracker();
        MaxstAR.deinit();
        if (sceneView != null) {
            sceneView.destroy();
            sceneView = null;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.normal_tracking) {
            TrackerManager.getInstance().setTrackingOption(TrackerManager.TrackingOption.NORMAL_TRACKING);
        } else if (view.getId() == R.id.extended_tracking) {
            TrackerManager.getInstance().setTrackingOption(TrackerManager.TrackingOption.EXTENDED_TRACKING);
        } else if (view.getId() == R.id.multi_tracking) {
            TrackerManager.getInstance().setTrackingOption(TrackerManager.TrackingOption.MULTI_TRACKING);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }

        MaxstAR.setScreenOrientation(newConfig.orientation);
        if (Build.MANUFACTURER.equals("vuzix")) {
            CameraDevice.getInstance().flipVideo(CameraDevice.FlipDirection.HORIZONTAL, true);
            CameraDevice.getInstance().flipVideo(CameraDevice.FlipDirection.VERTICAL, true);
        }
    }

    public void updateSceneformPose(float[] m, float w, float h,
                                    boolean isModelVisible,
                                    String modelId, float modelSize) {

        runOnUiThread(() -> {

            Node node = modelNodes.get(modelId);
            if (node == null) return;

            float[] corrected = m.clone();
            corrected[2] *= -1;
            corrected[6] *= -1;
            corrected[10] *= -1;
            corrected[14] *= -1;

            float tx = corrected[12];
            float ty = corrected[13];
            float tz = corrected[14];

            int rotation = ((WindowManager) getSystemService(WINDOW_SERVICE))
                    .getDefaultDisplay()
                    .getRotation();

            float xFix, yFix;
            switch (rotation) {
                case Surface.ROTATION_0:
                    xFix = -ty;
                    yFix = tx;
                    break;
                case Surface.ROTATION_90:
                    xFix = tx;
                    yFix = ty;
                    break;
                case Surface.ROTATION_180:
                    xFix = ty;
                    yFix = -tx;
                    break;
                case Surface.ROTATION_270:
                default:
                    xFix = -tx;
                    yFix = -ty;
                    break;
            }

            // Apply your scaling
            float X_MULT = 2.5f;
            float Y_MULT = -2.5f;
            float Z_MULT = 1.0f;

            Vector3 pos = new Vector3(
                    xFix * X_MULT,
                    yFix * Y_MULT,
                    tz * Z_MULT
            );

            Quaternion baseRot = quaternionFromMatrix(corrected);

            Quaternion rotFix;
            switch (rotation) {
                case Surface.ROTATION_0:     // Portrait → +90° about Z
                    rotFix = Quaternion.axisAngle(new Vector3(0, 0, 1), 90);
                    break;
                case Surface.ROTATION_90:    // Landscape-right → no rotation
                    rotFix = Quaternion.identity();
                    break;
                case Surface.ROTATION_180:   // Reverse portrait → 180° about Z
                    rotFix = Quaternion.axisAngle(new Vector3(0, 0, 1), 180);
                    break;
                case Surface.ROTATION_270:   // Landscape-left → -90° about Z
                default:
                    rotFix = Quaternion.axisAngle(new Vector3(0, 0, 1), -90);
                    break;
            }

            Quaternion finalRot = Quaternion.multiply(baseRot, rotFix);
            float scale = w * modelSize;

            node.setWorldPosition(pos);
            node.setWorldRotation(finalRot);
            node.setWorldScale(new Vector3(scale, scale, scale));
            for (Node n : modelNodes.values()) {
                n.setEnabled(false);
            }
            node.setEnabled(isModelVisible);
        });
    }

    public void updateGlacierPose(float[] m, float w, float h, boolean ismodel, String modelId, float modelSize) {
        updateSceneformPose(m, w, h, ismodel, modelId, modelSize);
    }

    public void disableModel() {
        runOnUiThread(() -> rootNode.setEnabled(false));
    }

    public void enableModel() {
        runOnUiThread(() -> rootNode.setEnabled(true));
    }

    private Quaternion quaternionFromMatrix(float[] m) {
        float trace = m[0] + m[5] + m[10];
        float w, x, y, z;
        if (trace > 0) {
            float s = (float) Math.sqrt(trace + 1.0f) * 2f;
            w = 0.25f * s;
            x = (m[9] - m[6]) / s;
            y = (m[2] - m[8]) / s;
            z = (m[4] - m[1]) / s;
        } else if ((m[0] > m[5]) && (m[0] > m[10])) {
            float s = (float) Math.sqrt(1.0f + m[0] - m[5] - m[10]) * 2f;
            w = (m[9] - m[6]) / s;
            x = 0.25f * s;
            y = (m[1] + m[4]) / s;
            z = (m[2] + m[8]) / s;
        } else if (m[5] > m[10]) {
            float s = (float) Math.sqrt(1.0f + m[5] - m[0] - m[10]) * 2f;
            w = (m[2] - m[8]) / s;
            x = (m[1] + m[4]) / s;
            y = 0.25f * s;
            z = (m[6] + m[9]) / s;
        } else {
            float s = (float) Math.sqrt(1.0f + m[10] - m[0] - m[5]) * 2f;
            w = (m[4] - m[1]) / s;
            x = (m[2] + m[8]) / s;
            y = (m[6] + m[9]) / s;
            z = 0.25f * s;
        }
        return new Quaternion(x, y, z, w);
    }

    public void updateLabelAtModelPoint(String name, String icon,
                                        float[] poseMatrix, float w, float h,
                                        float modelX, float modelY, float modelZ,
                                        float offsetX, float offsetY) {

        final float[] poseSnapshot = (poseMatrix == null) ? null : poseMatrix.clone();

        runOnUiThread(() -> {
            if (labelContainer == null) return;

            TextView tv = labelViews.get(name);

            if (tv == null) {
                tv = new TextView(this);
                applyLabelStyle(name, icon, tv);
                tv.setOnClickListener(v -> {
                    activeLabel = name;
                    glSurfaceView.requestRender();
                });
                labelContainer.addView(tv);
                labelViews.put(name, tv);
            }

            if (poseSnapshot == null) {
                tv.setVisibility(View.GONE);
                return;
            }

            float[] screen = projectModelPointToScreen(poseSnapshot, modelX, modelY, modelZ);
            if (screen == null) {
                tv.setVisibility(View.GONE);
                return;
            }
            float[] location = setLabelLocation(poseSnapshot, screen, w, h, tv, offsetX, offsetY);

//            float finalX = screen[0] + offsetX; // left
//            float finalY = screen[1] + offsetY;
//            float finalX = screen[0] - tv.getWidth() / 2f; // center
//            float finalX = screen[0] - tv.getWidth() + offsetX;
//            float finalY = screen[1] - tv.getHeight() + offsetY; // right

            tv.setX(location[0]);
            tv.setY(location[1]);
            tv.setVisibility(View.VISIBLE);
        });
    }

    private float[] setLabelLocation(float[] poseSnapshot, float[] screen, float w, float h, TextView tv, float offsetX, float offsetY) {
        float[][] screenCorners = getCoordinates(poseSnapshot, screen, w, h);
        if (screenCorners == null) {
            tv.setVisibility(View.GONE);
            return new float[2];
        }

        // screenCorners: [0]=bottom-left, [1]=bottom-right, [2]=top-right, [3]=top-left
        float left = screenCorners[0][0];
        float right = screenCorners[1][0];
//            float top    = screenCorners[3][1];
//            float bottom = screenCorners[0][1];
        float top = Math.min(screenCorners[0][1], screenCorners[2][1]);
        float bottom = Math.max(screenCorners[0][1], screenCorners[2][1]);

        float screenWidth = right - left;
        float screenHeight = bottom - top;

        float absOffsetX = offsetX / 1000 * screenWidth;
        float absOffsetY = offsetY / 1000 * screenHeight;

        float centerX = (screenCorners[0][0] + screenCorners[2][0]) / 2f;
        float centerY = (screenCorners[0][1] + screenCorners[2][1]) / 2f;

        float finalX = centerX + absOffsetX;
        float finalY = centerY + absOffsetY;

        return new float[]{finalX, finalY};
    }

    private float[][] getCoordinates(float[] poseSnapshot, float[] screen, float w, float h) {
        float halfW = w / 2f;
        float halfH = h / 2f;

        float[][] modelCorners = {
                {-halfW, -halfH, 0, 1},
                {halfW, -halfH, 0, 1},
                {halfW, halfH, 0, 1},
                {-halfW, halfH, 0, 1}
        };

        float[][] screenCorners = new float[4][2];

        for (int i = 0; i < 4; i++) {
            // Project each corner to screen space
            float[] screenPt = projectModelPointToScreen(
                    poseSnapshot,
                    modelCorners[i][0],
                    modelCorners[i][1],
                    modelCorners[i][2]
            );

            if (screenPt == null) return null;

            screenCorners[i][0] = screenPt[0];
            screenCorners[i][1] = screenPt[1];
        }
//        testCoordinates(screenCorners, screen);
        return screenCorners;
    }

    private void testCoordinates(float[][] screenCorners, float[] screen) {
        // Draw rectangle edges
        View overlay = new View(this) {
            final Paint p = new Paint();

            {
                p.setColor(Color.GREEN);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(5f);
                p.setAntiAlias(true);
            }

            @Override
            protected void onDraw(@NonNull Canvas c) {
                super.onDraw(c);
                // Draw rectangle edges
                for (int i = 0; i < 4; i++) {
                    int next = (i + 1) % 4;
                    c.drawLine(
                            screenCorners[i][0],
                            screenCorners[i][1],
                            screenCorners[next][0],
                            screenCorners[next][1],
                            p
                    );
                }
                c.drawCircle(screen[0], screen[1], 8f, p);
            }
        };
        labelContainer.removeAllViews();
        labelContainer.addView(overlay,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }

    private float[] projectModelPointToScreen(float[] poseMatrix,
                                              float modelX, float modelY, float modelZ) {

        if (projectionMatrix == null) return null;
        if (surfaceWidth == 0 || surfaceHeight == 0) return null;

        // Model point in homogeneous coordinates
        float[] modelPt = {modelX, modelY, modelZ, 1f};

        // Step 1 — world = Pose * model
        float[] worldPt = new float[4];
        Matrix.multiplyMV(worldPt, 0, poseMatrix, 0, modelPt, 0);

        // Step 2 — clip = Projection * world
        float[] clip = new float[4];
        Matrix.multiplyMV(clip, 0, projectionMatrix, 0, worldPt, 0);

        if (clip[3] == 0f) return null;

        // Perspective divide
        float nx = clip[0] / clip[3];
        float ny = clip[1] / clip[3];

        // Convert NDC → screen pixels
        float sx = (nx * 0.5f + 0.5f) * surfaceWidth;
        float sy = (1f - (ny * 0.5f + 0.5f)) * surfaceHeight;

        sx = Math.max(0, Math.min(sx, surfaceWidth - 1));
        sy = Math.max(0, Math.min(sy, surfaceHeight - 1));

        return new float[]{sx, sy};
    }

    private void applyLabelStyle(String name, String icon, TextView tv) {
        tv.setText(name);
        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(10f);

        tv.setPadding(16, 16, 16, 16);
        tv.setMinWidth(150);
        tv.setMaxWidth(300);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(layoutParams); //270, 100

        tv.setBackgroundResource(R.drawable.label_bg_rounded);
        tv.setCompoundDrawablePadding(16);
        Drawable d = getIconByType(icon);
        tv.setCompoundDrawables(d, null, null, null);
    }

    private Drawable getIconByType(String icon) {
        int resId = getResources().getIdentifier(icon.replace(".xml", ""), "drawable", getPackageName());
        Drawable drawable = ContextCompat.getDrawable(this, resId == 0 ? R.drawable.ic_baseline_info_24 : resId);
        Objects.requireNonNull(drawable).setBounds(0, 0, 30, 30);
        return drawable;
    }

    public float[] smoothPose(float[] oldPose, float[] newPose) {
        if (oldPose == null) return newPose.clone();

        float[] out = new float[16];
        for (int i = 0; i < 16; i++) {
            out[i] = oldPose[i] * (1f - SMOOTH_FACTOR) + newPose[i] * SMOOTH_FACTOR;
        }
        return out;
    }

    public void hideLabel(String name) {
        runOnUiThread(() -> {
            TextView tv = labelViews.get(name);
            if (tv != null) tv.setVisibility(View.GONE);
        });
    }

    public void updateProjection(float[] p) {
        System.arraycopy(p, 0, projectionMatrix, 0, 16);
    }

    public void updateSurfaceSize(int w, int h) {
        surfaceWidth = w;
        surfaceHeight = h;
    }
}
