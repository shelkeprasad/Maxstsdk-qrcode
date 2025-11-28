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

        backgroundRenderHelper = new BackgroundRenderHelper();
        CameraDevice.getInstance().setClippingPlane(0.03f, 70.0f);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        imageTrackerActivity.surfaceWidth = width;
        imageTrackerActivity.surfaceHeight = height;

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

        boolean legoDetected = false;
        boolean blocksDetected = false;
        boolean fetteDetected = false;


        float[] legoPose = null, blocksPose = null;
        float lw = 0, lh = 0, bw = 0, bh = 0;


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
                    videoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
                    videoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
                    //	videoRenderer.draw();
                    legoDetected = true;
                    legoPose = trackable.getPoseMatrix();
                    lw = trackable.getWidth();
                    lh = trackable.getHeight();

                    break;

                case "Blocks":

                    if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
                            chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
                        //    chromaKeyVideoRenderer.getVideoPlayer().start();
                    }

                    chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
                    chromaKeyVideoRenderer.setTransform(trackable.getPoseMatrix());
                    chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
                    chromaKeyVideoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
                    //	chromaKeyVideoRenderer.draw();

                    blocksDetected = true;
                    blocksPose = trackable.getPoseMatrix();
                    bw = trackable.getWidth();
                    bh = trackable.getHeight();
                    break;

                case "Glacier":
                    glacierDetected = true;
                    glacierPose = trackable.getPoseMatrix();
                    glacierWidth = trackable.getWidth();
                    glacierHeight = trackable.getHeight();
                    glacierDetected = true;

                    if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
                            chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
                        //    chromaKeyVideoRenderer.getVideoPlayer().start();
                    }

                    chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
                    chromaKeyVideoRenderer.setTransform(glacierPose);

                    float offsetX = -glacierWidth * 1.3f;
                    float offsetY = 0.0f;
                    float offsetZ = 0.0f;

                    chromaKeyVideoRenderer.setTranslate(offsetX, offsetY, offsetZ);
                    //	chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
                    chromaKeyVideoRenderer.setScale(glacierWidth, glacierHeight, 1.0f);
                    //	chromaKeyVideoRenderer.draw();

                    break;

                case "FetteMachine":
                    fetteDetected = true;

                    if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
                            chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
                        //    chromaKeyVideoRenderer.getVideoPlayer().start();
                    }

                    chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
                    chromaKeyVideoRenderer.setTransform(trackable.getPoseMatrix());
                    chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
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

        if (glacierDetected)
            imageTrackerActivity.updateGlacierPose(glacierPose, glacierWidth, glacierHeight);
        else imageTrackerActivity.hideGlacier();

        if (legoDetected)
            imageTrackerActivity.updateLegoPose(legoPose, lw, lh);
        else imageTrackerActivity.hideLego();

        if (blocksDetected)
            imageTrackerActivity.updateBlocksPose(blocksPose, bw, bh);
        else imageTrackerActivity.hideBlocks();

        if (!legoDetected && videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
            videoRenderer.getVideoPlayer().pause();
        }

        if (!blocksDetected && !fetteDetected &&
                chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
            chromaKeyVideoRenderer.getVideoPlayer().pause();
        }
    }

    void destroyVideoPlayer() {
        videoRenderer.getVideoPlayer().destroy();
        chromaKeyVideoRenderer.getVideoPlayer().destroy();
    }
}
