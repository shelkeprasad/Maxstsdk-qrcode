/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */
package com.maxst.ar.sample.imageTracker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Build;
import android.util.Log;

import com.maxst.ar.CameraDevice;
import com.maxst.ar.Matrix;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.MaxstARUtil;
import com.maxst.ar.Trackable;
import com.maxst.ar.TrackedImage;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.TrackingResult;
import com.maxst.ar.TrackingState;
import com.maxst.ar.sample.LabelConfig;
import com.maxst.ar.sample.ModelConfig;
import com.maxst.ar.sample.TrackerConfig;
import com.maxst.ar.sample.VideoConfig;
import com.maxst.ar.sample.arobject.BackgroundRenderHelper;
import com.maxst.ar.sample.arobject.ChromaKeyVideoRenderer;
import com.maxst.ar.sample.arobject.ColoredCubeRenderer;
import com.maxst.ar.sample.arobject.TexturedCubeRenderer;
import com.maxst.videoplayer.VideoPlayer;

import java.util.HashMap;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class ImageTrackerRenderer implements Renderer {

    public static final String TAG = ImageTrackerRenderer.class.getSimpleName();
    private TexturedCubeRenderer texturedCubeRenderer;
    private ColoredCubeRenderer coloredCubeRenderer;
    private ChromaKeyVideoRenderer chromaKeyVideoRenderer;
    private int surfaceWidth;
    private int surfaceHeight;
    private BackgroundRenderHelper backgroundRenderHelper;
    private final Activity activity;
    private ImageTrackerActivity imageTrackerActivity;

    private Map<String, TrackerConfig> trackerConfigs;

    private final Map<String, VideoPlayer> playerById = new HashMap<>();
    private final Map<String, VideoConfig> videoConfigById = new HashMap<>();
    private Map<String, LabelConfig> labelConfigById = new HashMap<>();
    private final Map<String, ModelConfig> modelConfigById = new HashMap<>();
    private ChromaKeyVideoRenderer sharedChromaRenderer;
    public String previousActiveLabel = null;
    private float[] latestProjectionMatrix = new float[16];
    private boolean hasLatestProjection = false;

    ImageTrackerRenderer(Activity activity, ImageTrackerActivity imageTrackerActivity, Map<String, TrackerConfig> configs) {
        this.activity = activity;
        this.imageTrackerActivity = imageTrackerActivity;
        this.trackerConfigs = configs;
        if (configs != null) {
            for (TrackerConfig t : configs.values()) {
                if (t.assets != null && t.assets.videos != null) {
                    for (VideoConfig v : t.assets.videos) {
                        videoConfigById.put(v.id, v);
                    }

                    for (LabelConfig lbl : t.assets.labels) {
                        labelConfigById.put(lbl.text, lbl);
                    }
                    if (t.assets.models != null) {
                        for (ModelConfig m : t.assets.models) {
                            modelConfigById.put(m.id, m);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("MaxstAR_Cube.png", activity.getAssets());


        texturedCubeRenderer = new TexturedCubeRenderer(activity);
        texturedCubeRenderer.setTextureBitmap(bitmap);
        coloredCubeRenderer = new ColoredCubeRenderer();

        sharedChromaRenderer = new ChromaKeyVideoRenderer();

        for (VideoConfig cfg : videoConfigById.values()) {
            String assetPath = cfg.path.replace("file:///android_asset/", "");
            VideoPlayer player = new VideoPlayer(activity);
            player.openVideo(assetPath);
            playerById.put(cfg.id, player);
        }

        backgroundRenderHelper = new BackgroundRenderHelper();
        CameraDevice.getInstance().setClippingPlane(0.03f, 70.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        ((ImageTrackerActivity) activity).updateSurfaceSize(width, height);
        MaxstAR.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);

        TrackingState state = TrackerManager.getInstance().updateTrackingState();
        TrackingResult trackingResult = state.getTrackingResult();
        TrackedImage image = state.getImage();

        float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();
        System.arraycopy(projectionMatrix, 0, latestProjectionMatrix, 0, 16);
        hasLatestProjection = true;
        float[] backgroundPlaneInfo = CameraDevice.getInstance().getBackgroundPlaneInfo();

        ((ImageTrackerActivity) activity).updateProjection(projectionMatrix);


        if (Build.MANUFACTURER.equals("vuzix")) {
            backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo, true, true);
        } else {
            backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo);
        }
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        float[] pose = null;
        float width = 0;
        float height = 0;
        boolean fetteDetected = false;
        int count = trackingResult.getCount();
        Log.d("MAXST_COUNT", "Detected targets = " + count);

        for (int i = 0; i < trackingResult.getCount(); i++) {
            Trackable trackable = trackingResult.getTrackable(i);
            String name = trackable.getName();
            ImageTrackerActivity act = (ImageTrackerActivity) activity;
            float[] rawPose = trackable.getPoseMatrix();
            act.smoothedPose = act.smoothPose(act.smoothedPose, rawPose);

            switch (name) {

                case "FetteMachine":
                    width = trackable.getWidth();
                    height = trackable.getHeight();
                    fetteDetected = true;
                    pose = act.smoothedPose;
                    imageTrackerActivity.updateGlacierPose(pose, width, height, false, "", 0.5f);
                    break;

                case "Lego":
                    width = trackable.getWidth();
                    height = trackable.getHeight();
                    fetteDetected = true;
                    pose = act.smoothedPose;
                    imageTrackerActivity.updateGlacierPose(pose, width, height, false, "", 0.5f);
                    break;
                case "Blocks":
                    width = trackable.getWidth();
                    height = trackable.getHeight();
                    fetteDetected = true;
                    pose = act.smoothedPose;
                    imageTrackerActivity.updateGlacierPose(pose, width, height, false, "", 0.5f);
                    break;

                case "Glacier":
                    width = trackable.getWidth();
                    height = trackable.getHeight();
                    fetteDetected = true;
                    pose = act.smoothedPose;
                    imageTrackerActivity.updateGlacierPose(pose, width, height, false, "", 0.5f);
                    break;

                default:
                    coloredCubeRenderer.setProjectionMatrix(projectionMatrix);
                    coloredCubeRenderer.setTransform(trackable.getPoseMatrix());
                    coloredCubeRenderer.setTranslate(0, 0, -0.1f);
                    coloredCubeRenderer.setScale(trackable.getWidth(), trackable.getHeight(), -0.1f);
                    coloredCubeRenderer.draw();
            }
        }

        float[] identityView = new float[16];
        Matrix.setIdentityM(identityView, 0);
        ImageTrackerActivity imageTrackerActivity = (ImageTrackerActivity) activity;
        if (fetteDetected) {
            for (LabelConfig lbl : labelConfigById.values()) {
                float modelX = lbl.anchor.xRel * width;
                float modelY = lbl.anchor.yRel * height;
                float modelZ = lbl.anchor.zRel;

                imageTrackerActivity.updateLabelAtModelPoint(
                        lbl.text,
                        pose,
                        width,
                        height,
                        modelX,
                        modelY,
                        modelZ,
                        lbl.offsetPx.x,
                        lbl.offsetPx.y
                );
            }

        } else {
            for (LabelConfig lbl : labelConfigById.values()) {
                imageTrackerActivity.hideLabel(lbl.text);
            }
        }
        String currentLabel = imageTrackerActivity.activeLabel;
        LabelConfig cfg = labelConfigById.get(currentLabel);
        float modelSize = 1.0f;
        if (cfg != null &&
                cfg.onClickAction != null &&
                "showModel".equals(cfg.onClickAction.type) &&
                fetteDetected) {

            ModelConfig mCfg = modelConfigById.get(cfg.onClickAction.targetModelId);
            if (mCfg != null) {
                modelSize = mCfg.size;
            }
            for (VideoPlayer p : playerById.values()) {
                if (p.getState() == VideoPlayer.STATE_PLAYING ||
                        p.getState() == VideoPlayer.STATE_READY ||
                        p.getState() == VideoPlayer.STATE_PAUSE) {
                    p.pause();
                }
            }
            sharedChromaRenderer.setVideoPlayer(null);

            imageTrackerActivity.updateGlacierPose(
                    pose, width, height, true, cfg.onClickAction.targetModelId, modelSize);

            previousActiveLabel = currentLabel;
            imageTrackerActivity.enableModel();
            return;
        } else {
            imageTrackerActivity.disableModel();
        }
        boolean labelChanged = false;
        if (currentLabel != null) {

            if (isPlayVideoLabel(currentLabel)) {

                if (previousActiveLabel == null || !previousActiveLabel.equals(currentLabel)) {
                    labelChanged = true;
                }
                previousActiveLabel = currentLabel;

            } else {
                labelChanged = false;
                for (VideoPlayer p : playerById.values()) {
                    if (p.getState() == VideoPlayer.STATE_PLAYING) {
                        p.pause();
                    }
                }
                previousActiveLabel = currentLabel;
            }
        }
        if (imageTrackerActivity.activeLabel != null && fetteDetected) {

            previousActiveLabel = currentLabel;

            LabelConfig lbl = labelConfigById.get(imageTrackerActivity.activeLabel);
            if (lbl != null && lbl.onClickAction != null
                    && "playVideo".equals(lbl.onClickAction.type)) {

                String videoId = lbl.onClickAction.targetVideoId;
                VideoConfig vcfg = videoConfigById.get(videoId);
                VideoPlayer vpl = playerById.get(videoId);

                if (vcfg != null && vpl != null) {

                    // 1) Pause all others
                    for (VideoPlayer p : playerById.values()) {
                        if (p != vpl && p.getState() == VideoPlayer.STATE_PLAYING) {
                            p.pause();
                        }
                    }
                    if (labelChanged) {
                        sharedChromaRenderer.resetSize();
                        sharedChromaRenderer.setShouldUpdateVideo(true);

                    }
                    // 2) Attach player to shared renderer
                    sharedChromaRenderer.setVideoPlayer(vpl);

                    // 3) Set transform
                    sharedChromaRenderer.setProjectionMatrix(projectionMatrix);
                    float[] latestPose = imageTrackerActivity.smoothedPose;

                    if (latestPose == null) return;
                    sharedChromaRenderer.setTransform(pose);

                    float vx = vcfg.translate.xFactor * width;
                    float vy = vcfg.translate.yFactor * height;
                    float vz = vcfg.translate.z;

               //     sharedChromaRenderer.setTranslate(vx, vy, vz);
                    sharedChromaRenderer.setTranslate(0.0f, 0.0f, 0.0f);

                    // 4) Scale from config
//                    sharedChromaRenderer.setScale(
//                            width * vcfg.scale.widthFactor,
//                            height * vcfg.scale.heightFactor,
//                            1f
//                    );

                    sharedChromaRenderer.setScale(width, height, 1.0f);

                    // 5) Start the selected video
                    if (vpl.getState() == VideoPlayer.STATE_READY ||
                            vpl.getState() == VideoPlayer.STATE_PAUSE) {

                        vpl.pause();
                        vpl.setPosition(0);
                        vpl.start();
                    }
                    sharedChromaRenderer.draw();
                }
            }
        } else {
            imageTrackerActivity.activeLabel = null;
            for (VideoPlayer p : playerById.values()) {
                if (p.getState() == VideoPlayer.STATE_PLAYING) {
                    p.pause();
                }
            }
        }
    }

    void destroyVideoPlayer() {
        if (sharedChromaRenderer.getVideoPlayer() != null) {
            sharedChromaRenderer.getVideoPlayer().destroy();
        }
    }

    private boolean isPlayVideoLabel(String labelId) {
        LabelConfig lbl = labelConfigById.get(labelId);
        if (lbl == null || lbl.onClickAction == null) return false;
        return "playVideo".equals(lbl.onClickAction.type);
    }

    public boolean hitTestVideoTap(float touchX, float touchY, int screenWidth, int screenHeight) {
        if (!hasLatestProjection) return false;

        ChromaKeyVideoRenderer r = sharedChromaRenderer;
        if (r == null) return false;

        VideoPlayer v = r.getVideoPlayer();
        if (v == null) return false;

        // get model matrix for current video quad
        float[] modelMat = r.getCurrentModelMatrix(); // 4x4
        if (modelMat == null) return false;

        // compute MVP = projection * model
        float[] mvp = new float[16];
        Matrix.multiplyMM(mvp, 0, latestProjectionMatrix, 0, modelMat, 0);

        // model-space quad corners (same as VERTEX_BUF in renderer)
        float[][] cornersModel = {
                {-0.5f, 0.5f, 0.0f, 1.0f}, // top-left (v0)
                {-0.5f, -0.5f, 0.0f, 1.0f}, // bottom-left (v1)
                {0.5f, -0.5f, 0.0f, 1.0f}, // bottom-right (v2)
                {0.5f, 0.5f, 0.0f, 1.0f}, // top-right (v3)
        };

        // Project corners to screen pixels
        float[][] screenPts = new float[4][2];
        for (int i = 0; i < 4; i++) {
            float[] clip = new float[4];
            Matrix.multiplyMV(clip, 0, mvp, 0, cornersModel[i], 0);

            // ignore if behind camera
            if (clip[3] == 0) return false;
            float ndcX = clip[0] / clip[3];
            float ndcY = clip[1] / clip[3];

            float sx = (ndcX * 0.5f + 0.5f) * screenWidth;
            float sy = (-ndcY * 0.5f + 0.5f) * screenHeight; // invert Y for Android touch coords

            screenPts[i][0] = sx;
            screenPts[i][1] = sy;
        }

        // Point-in-quad test: split into two triangles (0,1,2) and (0,2,3)
        if (pointInTriangle(touchX, touchY, screenPts[0], screenPts[1], screenPts[2]) ||
                pointInTriangle(touchX, touchY, screenPts[0], screenPts[2], screenPts[3])) {
            // It's a hit — toggle play/pause
            togglePlayPauseAttachedVideo();
            return true;
        }

        return false;
    }

    private boolean pointInTriangle(float px, float py, float[] a, float[] b, float[] c) {
        float v0x = c[0] - a[0];
        float v0y = c[1] - a[1];
        float v1x = b[0] - a[0];
        float v1y = b[1] - a[1];
        float v2x = px - a[0];
        float v2y = py - a[1];

        float dot00 = v0x * v0x + v0y * v0y;
        float dot01 = v0x * v1x + v0y * v1y;
        float dot02 = v0x * v2x + v0y * v2y;
        float dot11 = v1x * v1x + v1y * v1y;
        float dot12 = v1x * v2x + v1y * v2y;

        float denom = dot00 * dot11 - dot01 * dot01;
        if (denom == 0f) return false;
        float invDenom = 1.0f / denom;
        float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        return (u >= 0) && (v >= 0) && (u + v <= 1);
    }

    private void togglePlayPauseAttachedVideo() {
        if (sharedChromaRenderer == null) return;

        VideoPlayer vp = sharedChromaRenderer.getVideoPlayer();
        if (vp == null) return;

        int state = vp.getState();
        int pos = vp.getCurrentPosition();
        int dur = vp.getDuration();
        if (pos == dur) {
            vp.setPosition(0);
            sharedChromaRenderer.setShouldUpdateVideo(true);
            vp.start();
            return;
        }

        if (state == VideoPlayer.STATE_PLAYING) {
            vp.pause();
            sharedChromaRenderer.setShouldUpdateVideo(false);
            return;
        }

        if (state == VideoPlayer.STATE_PAUSE) {
            sharedChromaRenderer.setShouldUpdateVideo(true);
            vp.start();
            return;
        }

        if (state == VideoPlayer.STATE_READY) {
            vp.setPosition(0);
            sharedChromaRenderer.setShouldUpdateVideo(true);
            vp.start();
            return;
        }
    }
}
