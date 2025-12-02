/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */
package com.maxst.ar.sample.imageTracker;

import android.app.Activity;
import android.content.Context;
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
import com.maxst.ar.sample.R;
import com.maxst.ar.sample.arobject.BackgroundRenderHelper;
import com.maxst.ar.sample.arobject.ChromaKeyVideoRenderer;
import com.maxst.ar.sample.arobject.ColoredCubeRenderer;
import com.maxst.ar.sample.arobject.TexturedCubeRenderer;
import com.maxst.ar.sample.arobject.VideoRenderer;
import com.maxst.videoplayer.VideoPlayer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class ImageTrackerRenderer implements Renderer {

    public static final String TAG = ImageTrackerRenderer.class.getSimpleName();
    private TexturedCubeRenderer texturedCubeRenderer;
    private ColoredCubeRenderer coloredCubeRenderer;
    private VideoRenderer videoRenderer;
    private ChromaKeyVideoRenderer chromaKeyVideoRenderer;
    private int surfaceWidth;
    private int surfaceHeight;
    private BackgroundRenderHelper backgroundRenderHelper;
    private final Activity activity;
    private ImageTrackerActivity imageTrackerActivity;

    private ChromaKeyVideoRenderer videoGlacier;
    private ChromaKeyVideoRenderer videoLabel2;
    private ChromaKeyVideoRenderer videoLabel3;
    private ChromaKeyVideoRenderer videoLabel4;
    private ChromaKeyVideoRenderer videoMachine;

    ImageTrackerRenderer(Activity activity, ImageTrackerActivity imageTrackerActivity) {
        this.activity = activity;
        this.imageTrackerActivity = imageTrackerActivity;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("MaxstAR_Cube.png", activity.getAssets());

        texturedCubeRenderer = new TexturedCubeRenderer(activity);
        texturedCubeRenderer.setTextureBitmap(bitmap);

        coloredCubeRenderer = new ColoredCubeRenderer();

        videoRenderer = new VideoRenderer();
        VideoPlayer player = new VideoPlayer(activity);
        videoRenderer.setVideoPlayer(player);
        player.openVideo("VideoSample.mp4");

        chromaKeyVideoRenderer = new ChromaKeyVideoRenderer();
        player = new VideoPlayer(activity);
        chromaKeyVideoRenderer.setVideoPlayer(player);
        player.openVideo("FETTE.mp4");

        videoGlacier = new ChromaKeyVideoRenderer();
        VideoPlayer p1 = new VideoPlayer(activity);
        p1.openVideo("step1.mp4");
        videoGlacier.setVideoPlayer(p1);

        videoLabel2 = new ChromaKeyVideoRenderer();
        VideoPlayer p2 = new VideoPlayer(activity);
        p2.openVideo("step2.mp4");
        videoLabel2.setVideoPlayer(p2);

        videoLabel3 = new ChromaKeyVideoRenderer();
        VideoPlayer p3 = new VideoPlayer(activity);
        p3.openVideo("step2.mp4");
        //  p3.openVideo("step2.mp4");
        videoLabel3.setVideoPlayer(p3);

        videoLabel4 = new ChromaKeyVideoRenderer();
        VideoPlayer p4 = new VideoPlayer(activity);
        // p4.openVideo("FETTE.mp4");
        p4.openVideo("push.mp4");
        videoLabel4.setVideoPlayer(p4);

        videoMachine = new ChromaKeyVideoRenderer();
        VideoPlayer p5 = new VideoPlayer(activity);
        p5.openVideo("step1.mp4");
        videoMachine.setVideoPlayer(p5);


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

        float[] backgroundPlaneInfo = CameraDevice.getInstance().getBackgroundPlaneInfo();


        ImageTrackerActivity acts = (ImageTrackerActivity) activity;
        // float[] projectionMatrix = acts.projectionMatrix;

        ((ImageTrackerActivity) activity).updateProjection(projectionMatrix);


        if (Build.MANUFACTURER.equals("vuzix")) {
            backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo, true, true);
        } else {
            backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo);
        }

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        boolean glacierDetected = false;
        float[] glacierPose = null;
        float glacierWidth = 0;
        float glacierHeight = 0;

        float glacierWidthModel = 0;
        float glacierHeightModel = 0;

        boolean legoDetected = false;
        boolean blocksDetected = false;
        boolean fetteDetected = false;

        int count = trackingResult.getCount();
        Log.d("MAXST_COUNT", "Detected targets = " + count);

        for (int i = 0; i < trackingResult.getCount(); i++) {
            Trackable trackable = trackingResult.getTrackable(i);

            String name = trackable.getName();
            switch (name) {

                case "Lego":
                    legoDetected = true;

                    if (videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
                            videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
                        //    videoRenderer.getVideoPlayer().start();
                    }
                    videoRenderer.setProjectionMatrix(projectionMatrix);
                    videoRenderer.setTransform(trackable.getPoseMatrix());
                    //    videoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
                    videoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
                    videoRenderer.draw();
                    legoDetected = true;
                    break;

                case "Blocks":
                    if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
                            chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
                        chromaKeyVideoRenderer.getVideoPlayer().start();
                    }

                    chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
                    chromaKeyVideoRenderer.setTransform(trackable.getPoseMatrix());
                    chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
                    chromaKeyVideoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
                    chromaKeyVideoRenderer.draw();

                    blocksDetected = true;
                    break;

                case "FetteMachine":
                    glacierWidth = trackable.getWidth();
                    glacierHeight = trackable.getHeight();
                    glacierWidthModel = trackable.getWidth();
                    glacierHeightModel = trackable.getHeight();
                    glacierDetected = true;

                    if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
                            chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
                        //   chromaKeyVideoRenderer.getVideoPlayer().start();
                    }
                    chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
                    chromaKeyVideoRenderer.setTransform(trackable.getPoseMatrix());
                    //    chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
                    chromaKeyVideoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
                    //    chromaKeyVideoRenderer.draw();

                    float[] rawPose = trackable.getPoseMatrix();
                    ImageTrackerActivity act = (ImageTrackerActivity) activity;
                    act.smoothedGlacierPose = act.smoothPose(act.smoothedGlacierPose, rawPose);
                    glacierPose = act.smoothedGlacierPose;

                    break;

                case "Glacier":
                    fetteDetected = true;

                    if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
                            chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
                        //    chromaKeyVideoRenderer.getVideoPlayer().start();
                    }

                    chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
                    chromaKeyVideoRenderer.setTransform(trackable.getPoseMatrix());
                    //   chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
                    chromaKeyVideoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
                    chromaKeyVideoRenderer.draw();
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

        if (glacierDetected) {
            float halfW = glacierWidth / 2f;
            float halfH = glacierHeight / 2f;

            float extra = glacierWidth * 0.4f;
            float extraY = glacierWidth * 0.1f;

            // Top-left (x = -halfW, y = +halfH)
            imageTrackerActivity.updateLabelAtModelPoint("View Model", glacierPose, glacierWidth, glacierHeight,
                    -halfW - extra, halfH - extraY, 0f, 0, -10);

//            // Top-right (x = +halfW, y = +halfH)
//            imageTrackerActivity.updateLabelAtModelPoint("Step3", glacierPose, glacierWidth, glacierHeight,
//                    +halfW, halfH, 0f, 0, -10);

            // Bottom-left (x = -halfW, y = -halfH)
            imageTrackerActivity.updateLabelAtModelPoint("Step 2", glacierPose, glacierWidth, glacierHeight,
                    -halfW - extra, -halfH + extraY, 0f, 0, 10);

            // Bottom-right (x = +halfW, y = -halfH)
            imageTrackerActivity.updateLabelAtModelPoint("Step 1", glacierPose, glacierWidth, glacierHeight,
                    +halfW + extra, -halfH + extraY, 0f, 0, 10);

//            // Center
//            imageTrackerActivity.updateLabelAtModelPoint("Step4", glacierPose, glacierWidth, glacierHeight,
//                    0f, 0f, 0f, 0, 0);

            imageTrackerActivity.updateGlacierPose(glacierPose, glacierWidthModel, glacierHeightModel, false);

        } else {
            imageTrackerActivity.hideLabel("Step 1");
            imageTrackerActivity.hideLabel("Step3");
            imageTrackerActivity.hideLabel("Step 2");
            imageTrackerActivity.hideLabel("Step4");
            imageTrackerActivity.hideLabel("View Model");
            imageTrackerActivity.hideGlacier();
        }

        ImageTrackerActivity act = (ImageTrackerActivity) activity;

        if (act.activeLabel != null && glacierDetected) {

            ChromaKeyVideoRenderer vid = null;

            switch (act.activeLabel) {
                case "Step 1":
                    vid = videoGlacier;
                    break;
                case "Step3":
                    vid = videoLabel2;
                    break;
                case "Step 2":
                    vid = videoLabel3;
                    break;
                case "Step4":
                    vid = videoLabel4;
                    break;
                case "View Model": {
                    stopAllVideos();
                    act.activeLabel = null;
                    imageTrackerActivity.updateGlacierPose(glacierPose, glacierWidth, glacierHeight, true);
                }
                break;
            }

            if (vid != null) {
                imageTrackerActivity.hideGlacier();
                stopAllVideosExcept(vid);

                vid.setProjectionMatrix(projectionMatrix);
                vid.setTransform(glacierPose);
                float vx = 0, vy = 0;

                switch (act.activeLabel) {
                    case "Step 1":
                        vx = glacierWidth * 0.4f;
                        vy = -glacierHeight * 0.1f;
                        break;
                    case "Step3":
                        vx = glacierWidth * 0.3f;
                        vy = glacierHeight * 0.9f;
                        break;
                    case "Step 2":
                        vx = -glacierWidth * 0.3f;
                        vy = -glacierHeight * 0.1f;
                        break;
                    case "Step4":
                        vx = glacierWidth * 0.1f;
                        vy = glacierHeight * 0.4f;
                        break;
                }

                vid.setTranslate(vx, vy, 0f);

                // 4️⃣ Scale
                vid.setScale(glacierWidth * 1.8f, glacierHeight * 0.5f, 1f);
                //    vid.setScale(glacierWidth * 2.2f, glacierHeight * 0.5f, 1f);

                // 5️⃣ Play selected video ONLY
                VideoPlayer pl = vid.getVideoPlayer();
                if (pl.getState() == VideoPlayer.STATE_READY ||
                        pl.getState() == VideoPlayer.STATE_PAUSE) {
                    pl.start();
                }
                vid.draw();
            }
        } else {
            stopAllVideos();
            act.activeLabel = null;
        }


        if (!legoDetected && videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
            videoRenderer.getVideoPlayer().pause();
        }

        if (!blocksDetected && !fetteDetected &&
                chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
            // chromaKeyVideoRenderer.getVideoPlayer().pause();
        }
    }


    void destroyVideoPlayer() {
        videoRenderer.getVideoPlayer().destroy();
        chromaKeyVideoRenderer.getVideoPlayer().destroy();
    }

    private void stopAllVideosExcept(ChromaKeyVideoRenderer keep) {
        ChromaKeyVideoRenderer[] all = {
                videoGlacier,
                videoLabel2,
                videoLabel3,
                videoLabel4,
                videoMachine
        };

        for (ChromaKeyVideoRenderer v : all) {
            if (v == null) continue;
            if (v != keep) {
                VideoPlayer p = v.getVideoPlayer();
                if (p.getState() == VideoPlayer.STATE_PLAYING) {
                    p.pause();
                }
            }
        }
    }

    public void stopAllVideos() {
        if (videoRenderer != null && videoRenderer.getVideoPlayer() != null) {
            videoRenderer.getVideoPlayer().pause();
        }
        if (chromaKeyVideoRenderer != null && chromaKeyVideoRenderer.getVideoPlayer() != null) {
            chromaKeyVideoRenderer.getVideoPlayer().pause();
        }

        if (videoGlacier != null && videoGlacier.getVideoPlayer() != null)
            videoGlacier.getVideoPlayer().pause();
        if (videoLabel2 != null && videoLabel2.getVideoPlayer() != null)
            videoLabel2.getVideoPlayer().pause();
        if (videoLabel3 != null && videoLabel3.getVideoPlayer() != null)
            videoLabel3.getVideoPlayer().pause();
        if (videoLabel4 != null && videoLabel4.getVideoPlayer() != null)
            videoLabel4.getVideoPlayer().pause();
        if (videoMachine != null && videoMachine.getVideoPlayer() != null)
            videoMachine.getVideoPlayer().pause();
    }

}
