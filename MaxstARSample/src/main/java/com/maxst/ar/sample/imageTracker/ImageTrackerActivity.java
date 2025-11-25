/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */

package com.maxst.ar.sample.imageTracker;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;


//
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.animation.ModelAnimator;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import com.bumptech.glide.load.model.Model;
import com.google.ar.sceneform.rendering.RenderableInstance;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.ResultCode;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.sample.R;
import com.maxst.ar.sample.util.SampleUtil;

import java.util.Arrays;
import java.util.List;

public class ImageTrackerActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageTrackerRenderer imageTargetRenderer;
    private GLSurfaceView glSurfaceView;
    private int preferCameraResolution = 0;
    private Sceneform sceneformView;
    private SurfaceView sceneformSurfaceView;
    private SceneView sceneView;
    private Scene scene;
    private Node modelNode;

    private ModelRenderable modelRenderable;
    private ModelAnimator modelAnimator;

    private volatile long lastSeenNs = 0L;
    private static final long HIDE_DELAY_NS = 200L * 1000000L; // 200 ms

    private static final float SCALE_FACTOR = 1.0f;

    private float[] lastWorldMatrix = new float[16];


    //
    // last seen timestamp to quickly hide when lost
    private volatile long lastSeenMs = 0L;

    // stored last transform for smoothing & preventing jumps when lost (keeps last stable pose)
    private boolean hasLastWorld = false;

    private static final long HIDE_DELAY_MS = 200;        // CHANGED: hide delay after target lost (ms)
    private static final float SMOOTHING = 0.35f;

    private float[] lastStableWorld = new float[16];   // last stable pose
    private boolean hasStable = false;

    private long lastSeenTime = 0;     // Timestamp of last valid detection
    private static final long LOST_TIMEOUT_MS = 150;   // hide after 150ms


    private float[] stablePose = new float[16];
    private boolean hasStablePose = false;
    private long lastStableTime = 0L;
    private static final long STABLE_TIMEOUT_NS = 150_000_000; // 150ms


    private Vector3 lastPos = null;
    private Quaternion lastRot = null;

    private final float SMOOTH_POS = 0.15f;
    private final float SMOOTH_ROT = 0.15f;


    private float offsetX = 0f;
    private float offsetY = 0f;
    private float offsetZ = 0f;


    private static final float DISTANCE_MOVE_THRESHOLD = 0.03f;
    private static final float OFFSET_STEP_FORWARD = 0.02f;
    private static final float OFFSET_STEP_BACK = 0.01f;
    private static final float OFFSET_MAX = 0.8f;


    private boolean cameraMovingAway = false;


    private float initialX = 0;
    private float initialY = 0;
    private float initialZ = 0;
    private boolean initialPoseSaved = false;

    private float lastDistance = 0;


    float centerX = 0;
    boolean centerSaved = false;

    boolean sentStart = false;
    boolean sentLeft = false;
    boolean sentRight = false;
    boolean sentCenter = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_image_tracker);

        findViewById(R.id.normal_tracking).setOnClickListener(this);
        findViewById(R.id.extended_tracking).setOnClickListener(this);
        findViewById(R.id.multi_tracking).setOnClickListener(this);

        imageTargetRenderer = new ImageTrackerRenderer(this);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(imageTargetRenderer);

        MaxstAR.init(this.getApplicationContext(), getResources().getString(R.string.app_key));

        MaxstAR.setScreenOrientation(getResources().getConfiguration().orientation);

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
        sceneView.setTransparent(true);

        scene = sceneView.getScene();


        //


        modelNode = new Node();
        scene.addChild(modelNode);


        // ---------- DISABLE SCENEFORM CAMERA MOTION ----------

//        sceneView.getScene().addOnUpdateListener(frameTime -> {
//            Camera cam = sceneView.getScene().getCamera();
//
//            // Freeze the camera so Sceneform stops moving it
//            cam.setWorldPosition(new Vector3(0, 0, 0));
//            cam.setWorldRotation(Quaternion.identity());
//        });


        ModelRenderable.builder()
                //.setSource(this, Uri.parse("file:///android_asset/Jumping.glb"))
                //	.setSource(this, Uri.parse("file:///android_asset/Bee.glb"))
                //	.setSource(this, Uri.parse("file:///android_asset/vending_machine.glb"))
                //		.setSource(this, Uri.parse("file:///android_asset/generator.glb"))
                .setSource(this, Uri.parse("file:///android_asset/pneumatic_engine.glb"))


                //	.setSource(this, Uri.parse("file:///android_asset/platen_press.glb"))
                //	.setSource(this, Uri.parse("file:///android_asset/operating_machine.glb"))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> {
                    modelRenderable = renderable;
                    modelNode.setRenderable(renderable);

                    RenderableInstance instance = modelNode.getRenderableInstance();
                    modelNode.setEnabled(false);
                    // Get the animation name from instance
                    List<String> animNames = instance.getAnimationNames();
                    if (animNames.isEmpty()) {
                        Log.w("ARAnimation", "No animations found in the model");
                        return;
                    }
                    String animationName = animNames.get(0); // or pick based on your glb

                    // Use ObjectAnimator from maintained Sceneform
                    ObjectAnimator animator = ModelAnimator.ofAnimation(instance, animationName);
                    animator.setRepeatCount(ValueAnimator.INFINITE);
                    //	animator.start();

                })
                .exceptionally(throwable -> {
                    // handle load error
                    throwable.printStackTrace();
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
    }

    // todo working 2

//	public void updateSceneformPose(float[] poseMatrix_cameraSpace, float width, float height, float[] cameraViewMatrix) {
//		runOnUiThread(() -> {
//			if (modelNode == null) return;
//
//
//// Invert camera view -> camera world matrix
//			float[] cameraWorld = new float[16];
//			boolean ok = Matrix.invertM(cameraWorld, 0, cameraViewMatrix, 0);
//			if (!ok) {
//				Log.w("ImageTracker", "Failed to invert camera view matrix");
//				return;
//			}
//
//
//// worldMatrix = cameraWorld * poseMatrix_cameraSpace
//			float[] worldMatrix = new float[16];
//			// todo new add ..
//
//			Matrix.invertM(cameraWorld, 0, cameraViewMatrix, 0);
//			Matrix.multiplyMM(worldMatrix, 0, cameraWorld, 0, poseMatrix_cameraSpace, 0);
//
//
//// Extract translation from worldMatrix
//			float tx = worldMatrix[12];
//			float ty = worldMatrix[13];
//			float tz = worldMatrix[14];
//
//
//// CHANGED: apply handedness flip if needed (you used this previously)
//			tz = -tz; // flip Z to match Sceneform coordinate system
//
//
//// Extract rotation quaternion
//			Quaternion rot = quaternionFromMatrix(worldMatrix);
//			rot = new Quaternion(-rot.x, -rot.y, rot.z, rot.w); // CHANGED: preserve your previous handedness adjustments
//
//
//			modelNode.setEnabled(true);
//			modelNode.setWorldPosition(new Vector3(tx, ty, tz)); // CHANGED: set WORLD position (prevents drift)
//			modelNode.setWorldRotation(rot); // CHANGED: set WORLD rotation
//
//
//			float scale = width * 0.02f;
//			modelNode.setWorldScale(new Vector3(scale, scale, scale)); // CHANGED: world scale
//
//
//			Log.d("POSE_MODEL",
//					"Model Pos: tx=" + tx + " ty=" + ty + " tz=" + tz);
//
//			Log.d("POSE_MODEL_MATRIX",
//					"WorldMatrix: " + mat(worldMatrix));
//
//		});
//	}


//public void updateSceneformPose(float[] worldMatrix, float width, float height) {
//
//	runOnUiThread(() -> {
//		if (modelNode == null) return;
//
//		// Extract raw translation
//		float tx = worldMatrix[12];
//		float ty = worldMatrix[13];
//		float tz = worldMatrix[14];
//
//		// Save INITIAL pose for anchor
//		if (!initialPoseSaved) {
//			initialX = tx;
//			initialY = ty;
//			initialZ = -tz;   // Sceneform flip
//			initialPoseSaved = true;
//		}
//
//		// Distance camera→target
//		float dist = (float) Math.sqrt(tx*tx + ty*ty + tz*tz);
//
//		// Detect backward motion
//		float diff = (lastDistance > 0 ? dist - lastDistance : 0);
//		lastDistance = dist;
//		cameraMovingAway = diff > 0.01f;
//
//		// Flip Z for Sceneform
//		tz = -tz;
//
//		// Compute offset only when moving AWAY
//		float offsetX = 0f;
//		if (cameraMovingAway) {
//			offsetX = dist * 0.5f;
//
//			// Detect left/right from raw TX
//			boolean cameraRight = (tx > 0);
//			boolean cameraLeft  = (tx < 0);
//
//			if (cameraRight) offsetX = +offsetX  ;   // move model left
//			if (cameraLeft)  offsetX = -offsetX;   // move model right
//		}
//
//		// Final position = initial pose + offset

    /// /		float finalX = initialX + offsetX;
    /// /		float finalY = initialY;
    /// /		float finalZ = initialZ;
//
//     //   float finalX = tx + offsetX;
//        float finalX = tx + 0.20f;
//        float finalY = ty;
//        float finalZ = tz;
//
//		// Apply final pose
//		modelNode.setEnabled(true);
//		modelNode.setWorldPosition(new Vector3(finalX, finalY, finalZ));
//
//		Quaternion rot = quaternionFromMatrix(worldMatrix);
//		rot = new Quaternion(-rot.x, -rot.y, rot.z, rot.w); // Sceneform fix
//		modelNode.setWorldRotation(rot);
//
//		float scale = width * 0.02f;
//		modelNode.setWorldScale(new Vector3(scale, scale, scale));
//
//		Log.d("UPDATE_POSE",
//				"tx=" + tx +
//						" movingAway=" + cameraMovingAway +
//						" offsetX=" + offsetX);
//	});
//}


    // todo
    public void updateSceneformPose(float[] worldMatrix, float width, float height) {

        runOnUiThread(() -> {
            if (modelNode == null) return;

            float tx_raw = worldMatrix[12];
            float ty_raw = worldMatrix[13];
            float tz_raw = worldMatrix[14];  // Maxst Z forward

            float tx_raw1 = worldMatrix[12];
            float ty_raw1 = worldMatrix[13];
            float tz_raw1 = worldMatrix[14];  // Maxst Z forward

            tz_raw1 = -tz_raw1;


            Log.d("MAXST_RAW",
                    "tx=" + tx_raw + "  ty=" + ty_raw + "  tz=" + tz_raw);


            if (!initialPoseSaved) {

                initialX = tx_raw;
                initialY = ty_raw;
                initialZ = -tz_raw;   // convert to Sceneform Z

                centerX = tx_raw;
                centerSaved = true;

                initialPoseSaved = true;

                Log.d("ANCHOR_SAVED",
                        "MODEL_ANCHOR  X=" + initialX +
                                "  Y=" + initialY +
                                "  Z=" + initialZ);


                sendEvent("START", tx_raw, ty_raw, tz_raw, tx_raw1, ty_raw1, tz_raw1);
                sentStart = true;
            }

            if (centerSaved) {

                float dx = tx_raw - centerX;


                if (dx > 0.02f) {
                    sendEvent("MOVE RIGHT", tx_raw, ty_raw, tz_raw, tx_raw1, ty_raw1, tz_raw1);

                    sentRight = true;
                    sentLeft = false;
                    sentCenter = false;
                }

                // ---------- MOVE LEFT ----------
                if (dx < -0.02f) {
                    sendEvent("MOVE LEFT", tx_raw, ty_raw, tz_raw, tx_raw1, ty_raw1, tz_raw1);

                    sentLeft = true;
                    sentRight = false;
                    sentCenter = false;
                }

                // ---------- BACK TO CENTER ----------
                if (Math.abs(dx) < 0.02f) {
                    sendEvent("MOVE CENTER", tx_raw, ty_raw, tz_raw, tx_raw1, ty_raw1, tz_raw1);

                    sentCenter = true;
                    sentLeft = false;
                    sentRight = false;
                }
            }

//           float finalX = initialX;
//           float finalY = initialY;
//           float finalZ = initialZ;


            float finalX = tx_raw1;
            float finalY = ty_raw1;
            float finalZ = tz_raw1;


//           float dx = tx_raw - initialX;
//           float dy = ty_raw - initialY;
//           float dz = tz_raw - initialZ;
//
//           float finalX = initialX + dx;
//           float finalY = initialY + dy;
//           float finalZ = initialZ - dz; // convert Z


            modelNode.setEnabled(true);
            modelNode.setWorldPosition(new Vector3(finalX, finalY, finalZ));

            //    modelNode.setLocalPosition(new Vector3(finalX, finalY, finalZ));

            Log.d("MODEL_POSITION",
                    "X=" + finalX + "  Y=" + finalY + "  Z=" + finalZ);

            // ---------------- ROTATION ----------------
            Quaternion rot = quaternionFromMatrix(worldMatrix);
            rot = new Quaternion(-rot.x, -rot.y, rot.z, rot.w);
            //  modelNode.setWorldRotation(rot);

            // ---------------- SCALE ----------------
            float scale = width * 0.02f;
            modelNode.setWorldScale(new Vector3(scale, scale, scale));

        });
    }

    private void sendEvent(String eventType,
                           float mx, float my, float mz,
                           float sx, float sy, float sz) {

        Log.e("EVENT_TRIGGERED",
                eventType +
                        " | MAXST = (" + mx + ", " + my + ", " + mz + ")" +
                        " | MODEL = (" + sx + ", " + sy + ", " + sz + ")");
    }


//	public void hideSceneformModel() {
//		runOnUiThread(() -> modelNode.setEnabled(false));
//	}


    public void hideSceneformModel() {
        long now = System.currentTimeMillis();

        if (now - lastSeenTime > LOST_TIMEOUT_MS) {
            runOnUiThread(() -> {
                modelNode.setEnabled(false);
                hasStable = false;
            });
        }
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

}
