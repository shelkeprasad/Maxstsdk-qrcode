/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */

package com.maxst.ar.sample.imageTracker;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.Matrix;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.ResultCode;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.sample.R;
import com.maxst.ar.sample.util.SampleUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ImageTrackerActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageTrackerRenderer imageTargetRenderer;
    private GLSurfaceView glSurfaceView;
    private int preferCameraResolution = 0;
    private SceneView sceneView;
    private Scene scene;
    private Node glacierNode;
    private Node legoNode;
    private Node blocksNode;

    public int surfaceWidth = 0;
    public int surfaceHeight = 0;

    private FrameLayout labelContainer;
    private final Map<String, TextView> labelViews = new HashMap<>();

    public float[] projectionMatrix = new float[16];
    public String activeLabel = null;
    public float[] smoothedGlacierPose = null;
    private float SMOOTH_FACTOR = 0.2f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_tracker);
        labelContainer = findViewById(R.id.container);
        findViewById(R.id.normal_tracking).setOnClickListener(this);
        findViewById(R.id.extended_tracking).setOnClickListener(this);
        findViewById(R.id.multi_tracking).setOnClickListener(this);

        imageTargetRenderer = new ImageTrackerRenderer(this, this);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(imageTargetRenderer);

        MaxstAR.init(this.getApplicationContext(), getResources().getString(R.string.app_key));

        MaxstAR.setScreenOrientation(getResources().getConfiguration().orientation);
        if (Build.MANUFACTURER.equals("vuzix")) {
            CameraDevice.getInstance().flipVideo(CameraDevice.FlipDirection.HORIZONTAL, true);
            CameraDevice.getInstance().flipVideo(CameraDevice.FlipDirection.VERTICAL, true);
        }

        TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_IMAGE);
        TrackerManager.getInstance().addTrackerData("ImageTarget/Glacier.2dmap", true);
        TrackerManager.getInstance().addTrackerData("ImageTarget/Lego.2dmap", true);
        TrackerManager.getInstance().addTrackerData("ImageTarget/Blocks.2dmap", true);
        TrackerManager.getInstance().addTrackerData("ImageTarget/FetteMachine.2dmap", true);


//		TrackerManager.getInstance().addTrackerData("{\"image\":\"add_image\",\"image_path\":\"ImageTarget/Blocks.png\",\"image_width\":0.26,\"inclusion\":[{\"x\":50, \"y\":100, \"width\":400, \"height\":400}, {\"x\":400, \"y\":80, \"width\":400, \"height\":400}], \"exclusion\":[{\"x\":200, \"y\":200, \"width\":150, \"height\":150}]}", true);
        //TrackerManager.getInstance().addTrackerData("{\"image\":\"add_image\",\"image_path\":\"ImageTarget/Blocks.png\",\"image_width\":0.26}", true);
        //TrackerManager.getInstance().addTrackerData("{\"image\":\"add_image\",\"image_path\":\"ImageTarget/Glacier.png\",\"image_width\":0.26}", true);
        //TrackerManager.getInstance().addTrackerData("{\"image\":\"add_image\",\"image_path\":\"/sdcard/Download/sample/Blocks.png\",\"image_width\":0.26}", false);
        //TrackerManager.getInstance().addTrackerData("{\"image\":\"add_image\",\"image_path\":\"/sdcard/Download/sample/Glacier.png\",\"image_width\":0.26}", false);
        TrackerManager.getInstance().loadTrackerData();

        preferCameraResolution = getSharedPreferences(SampleUtil.PREF_NAME, Activity.MODE_PRIVATE).getInt(SampleUtil.PREF_KEY_CAM_RESOLUTION, 0);

    }

    private void initSceneformOverlay() {

        sceneView = findViewById(R.id.sceneView);
        sceneView.setTransparent(true);
        sceneView.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        scene = sceneView.getScene();

        glacierNode = new Node();
        legoNode = new Node();
        blocksNode = new Node();

        glacierNode.setEnabled(false);
        legoNode.setEnabled(false);
        blocksNode.setEnabled(false);

        scene.addChild(glacierNode);
        scene.addChild(legoNode);
        scene.addChild(blocksNode);

        loadModelWithAnimation("file:///android_asset/pneumatic_engine.glb", glacierNode);
        loadModelWithAnimation("file:///android_asset/Bee.glb", legoNode);
        loadModelWithAnimation("file:///android_asset/Bee.glb", blocksNode);

    }

    private void recreateSceneView() {
        ViewGroup root = findViewById(R.id.root_layout);
        SceneView oldView = findViewById(R.id.sceneView);

        if (oldView != null) {
            oldView.pause();
            oldView.destroy();
            root.removeView(oldView);
        }

        SceneView newSceneView = new SceneView(this);
        newSceneView.setId(R.id.sceneView);
        newSceneView.setBackgroundColor(Color.TRANSPARENT);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        newSceneView.setLayoutParams(lp);
        root.addView(newSceneView, 1);
        sceneView = newSceneView;
    }


    private void loadModelWithAnimation(String path, Node node) {

        ModelRenderable.builder()
                .setSource(this, Uri.parse(path))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> {

                    node.setRenderable(renderable);

                    RenderableInstance instance = node.getRenderableInstance();
                    List<String> anims = instance.getAnimationNames();

                    if (anims.isEmpty()) {
                        Log.w("ARAnimation", "No animations in: " + path);
                        return;
                    }

                    String animName = anims.get(0);
                    ObjectAnimator animator = ModelAnimator.ofAnimation(instance, animName);
                    animator.setRepeatCount(ValueAnimator.INFINITE);
                    animator.start();

                })
                .exceptionally(t -> {
                    t.printStackTrace();
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

        if (sceneView == null) {
            initSceneformOverlay();
        }
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

        glSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                imageTargetRenderer.destroyVideoPlayer();
            }
        });

        glSurfaceView.onPause();

        TrackerManager.getInstance().stopTracker();
        CameraDevice.getInstance().stop();
        MaxstAR.onPause();
        if (sceneView != null) {
            sceneView.pause();
        }
        imageTargetRenderer.stopAllVideos();
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
    public void onConfigurationChanged(Configuration newConfig) {
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
        //   recreateSceneView();
    }


    public void updateSceneformPose(Node node, float[] m, float w, float h, boolean isModelVisible) {

        runOnUiThread(() -> {
            if (node == null) return;

            // ---- 1) Copy & convert Maxst → Sceneform ----
            float[] corrected = m.clone();

            // Flip Z (RH → LH)
            corrected[2] *= -1;
            corrected[6] *= -1;
            corrected[10] *= -1;
            corrected[14] *= -1;

            // Extract components for convenience
            float tx = corrected[12];
            float ty = corrected[13];
            float tz = corrected[14];

            // 2) DEVICE ORIENTATION FIX
            int rotation = ((WindowManager) getSystemService(WINDOW_SERVICE))
                    .getDefaultDisplay()
                    .getRotation();

            float xFix, yFix;

            switch (rotation) {

                case Surface.ROTATION_0:     // Portrait → +90° rotation
                    xFix = -ty;
                    yFix = tx;
                    break;

                case Surface.ROTATION_90:    // Landscape-right (your original working orientation)
                    xFix = tx;
                    yFix = ty;
                    break;

                case Surface.ROTATION_180:   // Reverse portrait → 180° rotation
//                    xFix = -tx;
//                    yFix = -ty;
                    xFix = ty;
                    yFix = -tx;
                    break;

                case Surface.ROTATION_270:   // Landscape-left → -90° rotation
                default:
                    xFix = -tx;
                    yFix = -ty;
//                    xFix =  ty;
//                    yFix = -tx;
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

            // ---- 3) Rotation ----
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

            // ---- 4) Scale ----
            float scale = w * 0.02f;

            if (node != null) {
                node.setWorldPosition(pos);
                node.setWorldRotation(finalRot);
                node.setWorldScale(new Vector3(scale, scale, scale));

            }
            if (isModelVisible) {
                node.setEnabled(true);
            }
        });
    }

    public void updateGlacierPose(float[] m, float w, float h, boolean ismodel) {
        updateSceneformPose(glacierNode, m, w, h, ismodel);
    }

    public void hideGlacier() {
        runOnUiThread(() -> glacierNode.setEnabled(false));
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

    public void updateLabelAtModelPoint(String name,
                                        float[] poseMatrix, float w, float h,
                                        float modelX, float modelY, float modelZ,
                                        float offsetX, float offsetY) {

        final float[] poseSnapshot = (poseMatrix == null) ? null : poseMatrix.clone();

        runOnUiThread(() -> {
            if (labelContainer == null) return;

            TextView tv = labelViews.get(name);

            if (tv == null) {
                tv = new TextView(this);
                tv.setText(name);
                tv.setTextColor(Color.WHITE);
                tv.setGravity(Gravity.CENTER);
                tv.setTypeface(Typeface.DEFAULT_BOLD);
                tv.setPadding(16, 16, 16, 16);
                tv.setBackgroundResource(R.drawable.label_bg_rounded);

                tv.setOnClickListener(v -> activeLabel = name);
                applyLabelStyle(name, tv);
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

            float finalX = screen[0] - tv.getWidth() / 2f + offsetX;
            float finalY = screen[1] - tv.getHeight() / 2f + offsetY;

            tv.setX(finalX);
            tv.setY(finalY);
            tv.setVisibility(View.VISIBLE);
        });
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

        return new float[]{sx, sy};
    }

    private void applyLabelStyle(String name, TextView tv) {

        switch (name) {

            case "View Model":
                tv.setTextSize(8f);
                setDrawableSizeModel(tv, R.drawable.viewicon, 60, 60);  // custom icon size
                setTextViewSize(tv, 270, 100); // width, height
                break;

            case "Step 1":
                tv.setTextSize(8f);
                setDrawableSize(tv, R.drawable.stepicon, 60, 60);
                setTextViewSize(tv, 240, 90);
                break;

            case "Step 2":
                tv.setTextSize(8f);
                setDrawableSize(tv, R.drawable.stepicon, 60, 60);
                setTextViewSize(tv, 240, 90);
                break;

            case "Step3":
                tv.setTextSize(10f);
                setDrawableSize(tv, R.drawable.backarrow, 70, 70);
                setTextViewSize(tv, 350, 135);
                break;

            case "Step4":
                tv.setTextSize(10f);
                setTextViewSize(tv, 240, 120);
                setDrawableSize(tv, R.drawable.backarrow, 70, 70);
                break;

            default:
                tv.setTextSize(10f);
                setTextViewSize(tv, 120, 130);
        }
    }

    private void setTextViewSize(TextView tv, int width, int height) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(width, height);
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(params);
    }


    private void setDrawableSize(TextView tv, int resId, int width, int height) {
        Drawable d = getResources().getDrawable(resId);
        d.setBounds(0, 0, width, height);
        //  d.setTint(Color.WHITE);
        tv.setCompoundDrawables(d, null, null, null);
    }

    private void setDrawableSizeModel(TextView tv, int resId, int width, int height) {
        Drawable d = getResources().getDrawable(resId);
        d.setBounds(0, 0, width, height);
        tv.setCompoundDrawables(d, null, null, null);
    }

    private void quaternionToMatrix(float[] q, float[] out) {
        float x = q[0], y = q[1], z = q[2], w = q[3];

        out[0] = 1 - 2 * y * y - 2 * z * z;
        out[1] = 2 * x * y - 2 * z * w;
        out[2] = 2 * x * z + 2 * y * w;
        out[3] = 0;

        out[4] = 2 * x * y + 2 * z * w;
        out[5] = 1 - 2 * x * x - 2 * z * z;
        out[6] = 2 * y * z - 2 * x * w;
        out[7] = 0;

        out[8] = 2 * x * z - 2 * y * w;
        out[9] = 2 * y * z + 2 * x * w;
        out[10] = 1 - 2 * x * x - 2 * y * y;
        out[11] = 0;

        out[12] = 0;
        out[13] = 0;
        out[14] = 0;
        out[15] = 1;
    }

    public float[] smoothPose(float[] oldPose, float[] newPose) {
        if (oldPose == null) return newPose.clone();

        float[] out = new float[16];
        for (int i = 0; i < 16; i++) {
            out[i] = oldPose[i] * (1f - SMOOTH_FACTOR) + newPose[i] * SMOOTH_FACTOR;
        }
        return out;
    }

    private float[] convertMaxstPoseToScreen(float[] poseMatrix, float w, float h) {

        if (projectionMatrix == null) return null;
        if (surfaceWidth == 0 || surfaceHeight == 0) return null;

        float[] center = {0, 0, 0, 1};

        float[] poseCopy = poseMatrix.clone();

        float[] pm = new float[16];
        Matrix.multiplyMM(pm, 0, projectionMatrix, 0, poseCopy, 0);   // <-- MUST USE poseCopy

        float[] clip = new float[4];
        Matrix.multiplyMV(clip, 0, pm, 0, center, 0);

        if (clip[3] == 0f) return null;

        float nx = clip[0] / clip[3];
        float ny = clip[1] / clip[3];

        float sx = (nx * 0.5f + 0.5f) * surfaceWidth;
        float sy = (1f - (ny * 0.5f + 0.5f)) * surfaceHeight;

        return new float[]{sx, sy};
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
