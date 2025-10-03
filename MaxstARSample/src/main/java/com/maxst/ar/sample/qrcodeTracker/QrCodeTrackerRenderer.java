/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */
package com.maxst.ar.sample.qrcodeTracker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;

import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.MaxstARUtil;
import com.maxst.ar.Trackable;
import com.maxst.ar.TrackedImage;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.TrackingResult;
import com.maxst.ar.TrackingState;
import com.maxst.ar.sample.arobject.BackgroundRenderHelper;
import com.maxst.ar.sample.arobject.Yuv420spRenderer;
import com.maxst.ar.sample.arobject.TexturedCubeRenderer;
import com.maxst.ar.sample.util.TrackerResultListener;

import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


class QrCodeTrackerRenderer implements Renderer {

	public static final String TAG = QrCodeTrackerRenderer.class.getSimpleName();

	private TexturedCubeRenderer texturedCubeRenderer;

	private int surfaceWidth;
	private int surfaceHeight;
	private Yuv420spRenderer backgroundCameraQuad;

	private final Activity activity;
	private BackgroundRenderHelper backgroundRenderHelper;
	public TrackerResultListener listener = null;

	QrCodeTrackerRenderer(Activity activity) {
		this.activity = activity;
	}

	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		backgroundCameraQuad = new Yuv420spRenderer();

		Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("MaxstAR_Cube.png", activity.getAssets());

		texturedCubeRenderer = new TexturedCubeRenderer(activity);
		texturedCubeRenderer.setTextureBitmap(bitmap);

		backgroundRenderHelper = new BackgroundRenderHelper();
		CameraDevice.getInstance().setClippingPlane(0.03f, 70.0f);
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		surfaceWidth = width;
		surfaceHeight = height;

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

		backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo);

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		AtomicReference<Rect> lastQrCodeBoundingBox = new AtomicReference<>(null);


		for (int i = 0; i < trackingResult.getCount(); i++) {
			Trackable trackable = trackingResult.getTrackable(i);
			listener.sendData(trackable.getName());



			float[] poseMatrix = trackable.getPoseMatrix();
			lastQrCodeBoundingBox.set(calculateBoundingBoxFromPose(poseMatrix, 0.1f, 0.1f));



			/*Rect qrCodeBoundingBox = calculateBoundingBoxFromPose(trackable.getPoseMatrix(), 0.1f, 0.1f);

			if (activity instanceof QrCodeTrackerActivity) {
				QrCodeTrackerActivity qrActivity = (QrCodeTrackerActivity) activity;
				activity.runOnUiThread(() -> qrActivity.updateOverlayViews(qrCodeBoundingBox));
			}*/


			texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
			texturedCubeRenderer.setTransform(trackable.getPoseMatrix());
			texturedCubeRenderer.setTranslate(0, 0, -0.1f);
			texturedCubeRenderer.setScale(1.0f, 1.0f, 0.1f);

			//texturedCubeRenderer.draw();
		}

		if (lastQrCodeBoundingBox.get() != null && activity instanceof QrCodeTrackerActivity) {
			QrCodeTrackerActivity qrActivity = (QrCodeTrackerActivity) activity;
			activity.runOnUiThread(() -> qrActivity.updateOverlayViewss(lastQrCodeBoundingBox.get()));
		}
	}

	private Rect calculateBoundingBoxFromPose(float[] poseMatrix, float width, float height) {
		float x = poseMatrix[12]; // Translation X
		float y = poseMatrix[13]; // Translation Y
		float z = poseMatrix[14]; // Translation Z

		float halfWidth = width / 2f;
		float halfHeight = height / 2f;

		// Define corners as 4D homogeneous coordinates
		float[][] corners = {
				{x - halfWidth, y - halfHeight, z, 1.0f}, // Top-left
				{x + halfWidth, y - halfHeight, z, 1.0f}, // Top-right
				{x + halfWidth, y + halfHeight, z, 1.0f}, // Bottom-right
				{x - halfWidth, y + halfHeight, z, 1.0f}  // Bottom-left
		};

		// Convert world coordinates to screen coordinates
		Point[] screenCorners = new Point[4];
		for (int i = 0; i < 4; i++) {
			screenCorners[i] = worldToScreen(corners[i]);
		}

		// Compute bounding box
	/*	int left = Math.min(screenCorners[0].x, screenCorners[3].x);
		int top = Math.min(screenCorners[0].y, screenCorners[1].y);
		int right = Math.max(screenCorners[1].x, screenCorners[2].x);
		int bottom = Math.max(screenCorners[2].y, screenCorners[3].y);*/


		int left = Math.min(screenCorners[0].x, screenCorners[2].x);
		int top = Math.min(screenCorners[0].y, screenCorners[2].y);
		int right = Math.max(screenCorners[1].x, screenCorners[3].x);
		int bottom = Math.max(screenCorners[1].y, screenCorners[3].y);


		return new Rect(left, top, right, bottom);
	}

	private Point worldToScreen(float[] worldPoint) {
		if (worldPoint.length < 3) { // Ensure at least x, y, z
			throw new IllegalArgumentException("worldPoint must have at least 3 elements");
		}

		// Convert 3D world point to 4D homogeneous coordinates (x, y, z, 1.0)
		float[] worldPointFixed = {worldPoint[0], worldPoint[1], worldPoint[2], 1.0f};

		float[] clipCoords = new float[4];
		Matrix.multiplyMV(clipCoords, 0, CameraDevice.getInstance().getProjectionMatrix(), 0, worldPointFixed, 0);

		// Perform perspective division
		if (clipCoords[3] != 0) {
			clipCoords[0] /= clipCoords[3];
			clipCoords[1] /= clipCoords[3];
			clipCoords[2] /= clipCoords[3];
		}

		// Convert to screen space
		int screenX = (int) ((clipCoords[0] + 1) * 0.5 * surfaceWidth);
		int screenY = (int) ((1 - clipCoords[1]) * 0.5 * surfaceHeight);

		return new Point(screenX, screenY);
	}





}
