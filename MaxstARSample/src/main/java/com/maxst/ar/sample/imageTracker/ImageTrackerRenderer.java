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
import com.maxst.ar.sample.arobject.VideoRenderer;
import com.maxst.videoplayer.VideoPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public String previousActiveLabel = null;
    private float[] latestProjectionMatrix = new float[16];
    private boolean hasLatestProjection = false;
    private final List<String> trackerNames = new ArrayList<>();
    private VideoRenderer videoRenderer;
    private Map<String, Map<String, VideoConfig>> videosByTracker = new HashMap<>();
    private Map<String, Map<String, ModelConfig>> modelsByTracker = new HashMap<>();
    private Map<String, Map<String, LabelConfig>> labelsByTracker = new HashMap<>();

    private Map<String, Map<String, VideoPlayer>> playersByTracker = new HashMap<>();
    private String activeTracker = null;
    private String previousTracker = null;
    private Map<String, VideoConfig> activeVideos = null;
    private Map<String, ModelConfig> activeModels = null;
    private Map<String, LabelConfig> activeLabels = null;
    private Map<String, VideoPlayer> activePlayers = null;
    private VideoPlayer legacyPlayer = null;
     private float[] rawPose ;

    ImageTrackerRenderer(Activity activity, ImageTrackerActivity imageTrackerActivity, Map<String, TrackerConfig> configs) {
        this.activity = activity;
        this.imageTrackerActivity = imageTrackerActivity;
        this.trackerConfigs = configs;

        if (configs != null) {
            for (TrackerConfig t : configs.values()) {
                trackerNames.add(t.name);

                Map<String, VideoConfig> vmap = new HashMap<>();
                Map<String, ModelConfig> mmap = new HashMap<>();
                Map<String, LabelConfig> lmap = new HashMap<>();

                if (t.assets != null) {
                    if (t.assets.videos != null) {
                        for (VideoConfig v : t.assets.videos) {
                            vmap.put(v.id, v);
                        }
                    }

                    if (t.assets.models != null) {
                        for (ModelConfig m : t.assets.models) {
                            mmap.put(m.id, m);
                        }
                    }

                    if (t.assets.labels != null) {
                        for (LabelConfig lbl : t.assets.labels) {
                            lmap.put(lbl.text, lbl);
                        }
                    }
                }

                videosByTracker.put(t.name, vmap);
                modelsByTracker.put(t.name, mmap);
                labelsByTracker.put(t.name, lmap);
            }
        }
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("MaxstAR_Cube.png", activity.getAssets());
        texturedCubeRenderer = new TexturedCubeRenderer(activity);
   //     texturedCubeRenderer.setTextureBitmap(bitmap);
        coloredCubeRenderer = new ColoredCubeRenderer();
        chromaKeyVideoRenderer = new ChromaKeyVideoRenderer();
        videoRenderer = new VideoRenderer();

        // ADDED: create per-tracker VideoPlayer instances once (avoid openVideo per frame)
        for (Map.Entry<String, Map<String, VideoConfig>> trackerEntry : videosByTracker.entrySet()) {
            String trackerName = trackerEntry.getKey();
            Map<String, VideoConfig> vids = trackerEntry.getValue();
            Map<String, VideoPlayer> playerMap = new HashMap<>();
            if (vids != null) {
                for (VideoConfig cfg : vids.values()) {
                    try {
                        String assetPath = cfg.path;
                        if (assetPath.startsWith("file:///android_asset/")) {
                            assetPath = assetPath.replace("file:///android_asset/", "");
                        }
                        VideoPlayer p = new VideoPlayer(activity);
                        p.openVideo(assetPath);
                        playerMap.put(cfg.id, p);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to open video for tracker " + trackerName + " id=" + cfg.id, e);
                    }
                }
            }
            playersByTracker.put(trackerName, playerMap);
        }

        legacyPlayer = new VideoPlayer(activity);
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
        boolean targetDetected = false;
        int count = trackingResult.getCount();
        Log.d("MAXST_COUNT", "Detected targets = " + count);

        String detectedTrackerThisFrame = null;

        for (int i = 0; i < trackingResult.getCount(); i++) {
            Trackable trackable = trackingResult.getTrackable(i);
            String name = trackable.getName();

            ImageTrackerActivity act = (ImageTrackerActivity) activity;
            rawPose = trackable.getPoseMatrix();
            act.smoothedPose = act.smoothPose(act.smoothedPose, rawPose);

            if (trackerNames.contains(name)) {
                if (detectedTrackerThisFrame == null) {
                    detectedTrackerThisFrame = name;
                    targetDetected = true;
                    width = trackable.getWidth();
                    height = trackable.getHeight();
                    pose = act.smoothedPose;
                    imageTrackerActivity.updateGlacierPose(pose, width, height, false, "", 0.5f);
                }
            } else {
                coloredCubeRenderer.setProjectionMatrix(projectionMatrix);
                coloredCubeRenderer.setTransform(trackable.getPoseMatrix());
                coloredCubeRenderer.setTranslate(0, 0, -0.1f);
                coloredCubeRenderer.setScale(trackable.getWidth(), trackable.getHeight(), -0.1f);
                coloredCubeRenderer.draw();
            }
        }

        if (!targetDetected) {
            clearActiveTracker();
        }

        if (detectedTrackerThisFrame != null) {
            if (!detectedTrackerThisFrame.equals(previousTracker)) {
                setActiveTracker(detectedTrackerThisFrame);
            }
            previousTracker = detectedTrackerThisFrame;
        } else {
            if (previousTracker != null) {
                clearActiveTracker();
                previousTracker = null;
            }
        }

        ImageTrackerActivity imageTrackerActivity = (ImageTrackerActivity) activity;

        if (targetDetected && activeLabels != null) {
            for (LabelConfig lbl : activeLabels.values()) {
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
            if (activeLabels != null) {
                for (LabelConfig lbl : activeLabels.values()) {
                    imageTrackerActivity.hideLabel(lbl.text);
                }
            }
        }

        String currentLabel = imageTrackerActivity.activeLabel;
        LabelConfig cfg = (activeLabels == null || currentLabel == null) ? null : activeLabels.get(currentLabel);
        float modelSize = 0.01f;

        if (cfg != null &&
                cfg.onClickAction != null &&
                "showModel".equals(cfg.onClickAction.type) &&
                targetDetected) {
            ModelConfig mCfg = (activeModels == null) ? null : activeModels.get(cfg.onClickAction.targetModelId);
            if (mCfg != null) {
                modelSize = mCfg.size;
            }
            pauseAllPlayers();

            chromaKeyVideoRenderer.setVideoPlayer(null);
            if (mCfg == null) {
                Log.e("ImageTrackerRenderer", "model is NULL");
                return;
            }

            if (mCfg.type == 17){
                String assetPath = mCfg.path.replace("file:///android_asset/", "");
                Bitmap bmp = MaxstARUtil.getBitmapFromAsset(assetPath, activity.getAssets());
                texturedCubeRenderer.setTextureBitmap(bmp);
                texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
                texturedCubeRenderer.setTransform(pose);
                texturedCubeRenderer.setTranslate(0, 0, -0.05f);
                texturedCubeRenderer.setScale(width, height, 0.1f);
                texturedCubeRenderer.draw();

            }else if (mCfg.type == 23){
                imageTrackerActivity.updateGlacierPose(
                        pose, width, height, true, cfg.onClickAction.targetModelId, modelSize);
            }

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
                pauseAllPlayingPlayers(); // 🔄 UPDATED
                previousActiveLabel = currentLabel;
            }
        }

        if (imageTrackerActivity.activeLabel != null && targetDetected) {

            previousActiveLabel = currentLabel;

            LabelConfig lbl = (activeLabels == null) ? null : activeLabels.get(imageTrackerActivity.activeLabel);
            if (lbl != null && lbl.onClickAction != null
                    && "playVideo".equals(lbl.onClickAction.type)) {

                String videoId = lbl.onClickAction.targetVideoId;
                VideoConfig vcfg = (activeVideos == null) ? null : activeVideos.get(videoId);
                VideoPlayer vpl = (activePlayers == null) ? null : activePlayers.get(videoId);

                if (vcfg != null && vpl != null) {

                    // 1) Pause all other players (across all trackers) except vpl
                    for (Map<String, VideoPlayer> pm : playersByTracker.values()) {
                        for (VideoPlayer p : pm.values()) {
                            if (p != vpl && p.getState() == VideoPlayer.STATE_PLAYING) {
                                p.pause();
                            }
                        }
                    }

                    if (labelChanged) {
                        chromaKeyVideoRenderer.resetSize();
                        chromaKeyVideoRenderer.setShouldUpdateVideo(true);
                    }

                    // 2) Attach player to shared renderer
                    chromaKeyVideoRenderer.setVideoPlayer(vpl);

                    // 3) Set transform
                    chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
                    float[] latestPose = imageTrackerActivity.smoothedPose;

                    if (latestPose == null) return;
                    chromaKeyVideoRenderer.setTransform(rawPose);

                    float vx = vcfg.translate.x * width;
                    float vy = vcfg.translate.y * height;
                    float vz = vcfg.translate.z;

                    // NOTE: previous code used setTranslate(0,0,0) — keep that if desired
                    chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);

                    // 4) Scale from config
                    chromaKeyVideoRenderer.setScale(width, height, 1.0f);

                    // 5) Start the selected video
                    if (vpl.getState() == VideoPlayer.STATE_READY ||
                            vpl.getState() == VideoPlayer.STATE_PAUSE) {

                        vpl.pause();
                        vpl.setPosition(0);
                        vpl.start();
                    }
                    chromaKeyVideoRenderer.draw();
                }
            }
        } else {
            imageTrackerActivity.activeLabel = null;
            pauseAllPlayingPlayers();
        }

        // If there are no labels for this tracker, handle "dynamic" video/model types
        if (activeLabels != null && !activeLabels.isEmpty()) {

        } else if (targetDetected ) {
            if (activeVideos != null && !activeVideos.isEmpty()){
                for (VideoConfig videoConfig : activeVideos.values()) {

                    if (videoConfig.type == 20) {
                        VideoPlayer vp = (activePlayers == null) ? null : activePlayers.get(videoConfig.id);
                        if (vp == null) {
                            try {
                                String assetPath = videoConfig.path.replace("file:///android_asset/", "");
                                legacyPlayer.openVideo(assetPath);
                            } catch (Exception ignored) {
                            }
                            vp = legacyPlayer;
                        }

                        videoRenderer.setVideoPlayer(vp);

                        if (videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
                                videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
                            videoRenderer.getVideoPlayer().start();
                        }
                        videoRenderer.setProjectionMatrix(projectionMatrix);
                        videoRenderer.setTransform(rawPose);
                        videoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
                        videoRenderer.setScale(width, height, 1.0f);
                        videoRenderer.draw();
                    }
                    if (videoConfig.type == 8) {
                        VideoPlayer vp = (activePlayers == null) ? null : activePlayers.get(videoConfig.id);
                        if (vp == null) {
                            try {
                                String assetPath = videoConfig.path.replace("file:///android_asset/", "");
                                legacyPlayer.openVideo(assetPath);
                            } catch (Exception ignored) {
                            }
                            vp = legacyPlayer;
                        }

                        chromaKeyVideoRenderer.setVideoPlayer(vp);

                        if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
                                chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
                            chromaKeyVideoRenderer.getVideoPlayer().start();
                        }
                        chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
                        chromaKeyVideoRenderer.setTransform(pose);
                        chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
                        chromaKeyVideoRenderer.setScale(width, height, 1.0f);
                        chromaKeyVideoRenderer.draw();
                    }
                }
            }

            if (activeModels != null) {
                for (ModelConfig modelConfig : activeModels.values()) {
                    if (modelConfig.type == 17) {
                        String assetPath = modelConfig.path.replace("file:///android_asset/", "");
                        Bitmap bitmap = MaxstARUtil.getBitmapFromAsset(assetPath, activity.getAssets());
                        texturedCubeRenderer.setTextureBitmap(bitmap);
                        texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
                        texturedCubeRenderer.setTransform(pose);
                        texturedCubeRenderer.setTranslate(0, 0, -0.05f);
                        texturedCubeRenderer.setScale(width, height, 0.1f);
                        texturedCubeRenderer.draw();
                    } else if (modelConfig.type == 23) {
                        imageTrackerActivity.updateGlacierPose(
                                pose, width, height, true, modelConfig.id, modelSize);
                    } else {
                        Log.d("type", "different type ..");
                    }
                }
            }
        } else {
            pauseVideoPlayer();
        }
    }
    private void setActiveTracker(String name) {
        if (activePlayers != null && !name.equals(activeTracker)) {
            for (VideoPlayer p : activePlayers.values()) {
                try {
                    if (p.getState() == VideoPlayer.STATE_PLAYING) p.pause();
                } catch (Exception ignored) {
                }
            }
        }
        activeTracker = name;
        activeVideos = videosByTracker.get(name);
        activeModels = modelsByTracker.get(name);
        activeLabels = labelsByTracker.get(name);
        activePlayers = playersByTracker.get(name);

        if (activePlayers != null) {
            for (VideoPlayer p : activePlayers.values()) {
                try {
                    p.pause();
                    p.setPosition(0);
                } catch (Exception ignored) {
                }
            }
        }
        Log.d(TAG, "Active tracker set to: " + name);
    }

    private void clearActiveTracker() {

        // 1) hide all label views
        if (activeLabels != null) {
            for (LabelConfig lbl : activeLabels.values()) {
                try {
                    imageTrackerActivity.hideLabel(lbl.text);
                } catch (Exception ignored) {}
            }
        }
        imageTrackerActivity.activeLabel = null;

        // 2) stop and reset all active tracker players
        if (activePlayers != null) {
            for (VideoPlayer p : activePlayers.values()) {
                try {
                    if (p.getState() == VideoPlayer.STATE_PLAYING) p.pause();
                    p.setPosition(0);
                } catch (Exception ignored) {}
            }
        }

        // 3) remove video from GL renderers
        if (chromaKeyVideoRenderer != null)
            chromaKeyVideoRenderer.setVideoPlayer(null);

        if (videoRenderer != null)
            videoRenderer.setVideoPlayer(null);

        // 4) clear active maps
        activeTracker = null;
        activeVideos = null;
        activeModels = null;
        activeLabels = null;
        activePlayers = null;

        Log.d(TAG, "Active tracker fully cleared.");
    }

    void destroyVideoPlayer() {
        for (Map<String, VideoPlayer> pm : playersByTracker.values()) {
            for (VideoPlayer p : pm.values()) {
                try {
                    p.destroy();
                } catch (Exception ignored) {
                }
            }
            pm.clear();
        }
        playersByTracker.clear();

        if (chromaKeyVideoRenderer != null && chromaKeyVideoRenderer.getVideoPlayer() != null) {
            try {
                chromaKeyVideoRenderer.getVideoPlayer().destroy();
            } catch (Exception ignored) {
            }
        }
        if (videoRenderer != null && videoRenderer.getVideoPlayer() != null) {
            try {
                videoRenderer.getVideoPlayer().destroy();
            } catch (Exception ignored) {
            }
        }
        if (legacyPlayer != null) {
            try {
                legacyPlayer.destroy();
            } catch (Exception ignored) {
            }
        }
    }
    private void pauseAllPlayers() {
        for (Map<String, VideoPlayer> pm : playersByTracker.values()) {
            for (VideoPlayer p : pm.values()) {
                try {
                    if (p.getState() == VideoPlayer.STATE_PLAYING ||
                            p.getState() == VideoPlayer.STATE_READY ||
                            p.getState() == VideoPlayer.STATE_PAUSE) {
                        p.pause();
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void pauseAllPlayingPlayers() {
        for (Map<String, VideoPlayer> pm : playersByTracker.values()) {
            for (VideoPlayer p : pm.values()) {
                try {
                    if (p.getState() == VideoPlayer.STATE_PLAYING) {
                        p.pause();
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean isPlayVideoLabel(String labelId) {
        if (activeLabels != null) {
            LabelConfig lbl = activeLabels.get(labelId);
            if (lbl != null && lbl.onClickAction != null) {
                return "playVideo".equals(lbl.onClickAction.type);
            }
        }
        for (Map<String, LabelConfig> lm : labelsByTracker.values()) {
            LabelConfig l = lm.get(labelId);
            if (l != null && l.onClickAction != null && "playVideo".equals(l.onClickAction.type))
                return true;
        }
        return false;
    }

    public boolean hitTestVideoTap(float touchX, float touchY, int screenWidth, int screenHeight) {
        if (!hasLatestProjection) return false;

        ChromaKeyVideoRenderer r = chromaKeyVideoRenderer;
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
        if (chromaKeyVideoRenderer == null) return;

        VideoPlayer vp = chromaKeyVideoRenderer.getVideoPlayer();
        if (vp == null) return;

        int state = vp.getState();
        int pos = vp.getCurrentPosition();
        int dur = vp.getDuration();
        if (pos == dur) {
            vp.setPosition(0);
            chromaKeyVideoRenderer.setShouldUpdateVideo(true);
            vp.start();
            return;
        }

        if (state == VideoPlayer.STATE_PLAYING) {
            vp.pause();
            chromaKeyVideoRenderer.setShouldUpdateVideo(false);
            return;
        }

        if (state == VideoPlayer.STATE_PAUSE) {
            chromaKeyVideoRenderer.setShouldUpdateVideo(true);
            vp.start();
            return;
        }

        if (state == VideoPlayer.STATE_READY) {
            vp.setPosition(0);
            chromaKeyVideoRenderer.setShouldUpdateVideo(true);
            vp.start();
            return;
        }
    }

    private void pauseVideoPlayer() {
        if (videoRenderer != null) {
            VideoPlayer vp = videoRenderer.getVideoPlayer();

            if (vp != null) {
                int state = vp.getState();
                if (state == VideoPlayer.STATE_PLAYING) {
                    vp.pause();
                }
            }
        }
        if (chromaKeyVideoRenderer != null) {
            VideoPlayer vp2 = chromaKeyVideoRenderer.getVideoPlayer();

            if (vp2 != null) {
                int state2 = vp2.getState();
                if (state2 == VideoPlayer.STATE_PLAYING) {
                    vp2.pause();
                }
            }
        }
    }
}
