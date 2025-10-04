/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */
package com.maxst.ar.sample.qrcodeFusionTracker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
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
import com.maxst.ar.sample.qrcodeTracker.QrCodeTrackerActivity;
import com.maxst.ar.sample.util.TrackerResultListener;

import java.util.concurrent.atomic.AtomicReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


class QrCodeFusionTrackerRenderer implements Renderer {

	public static final String TAG = QrCodeFusionTrackerRenderer.class.getSimpleName();

	private TexturedCubeRenderer texturedCubeRenderer;

	private int surfaceWidth;
	private int surfaceHeight;
	private Yuv420spRenderer backgroundCameraQuad;

	private final Activity activity;
	private BackgroundRenderHelper backgroundRenderHelper;
	public TrackerResultListener listener = null;


	QrCodeFusionTrackerRenderer(Activity activity) {
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

		CameraDevice.getInstance().setARCoreTexture();
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

		// new 3d model

//		if (trackingResult.getCount() > 0) {
//			Trackable trackable = trackingResult.getTrackable(0);
//			float[] poseMatrix = trackable.getPoseMatrix();
//
//			// Convert poseMatrix to ARCore pose
//			Pose pose = new Pose(
//					new float[]{poseMatrix[12], poseMatrix[13], poseMatrix[14]},  // Translation
//					new float[]{poseMatrix[0], poseMatrix[1], poseMatrix[2], poseMatrix[3]} // Rotation
//			);
//
//			// Create an Anchor at the detected QR Code position
//			Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(pose);
//			place3DModel(anchor);
//		}



		TrackedImage image = state.getImage();
		float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();
		float[] backgroundPlaneInfo = CameraDevice.getInstance().getBackgroundPlaneInfo();

		backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo);

		int fusionState = TrackerManager.getInstance().getFusionTrackingState();
		int trackingCount = trackingResult.getCount();

		listener.sendFusionState(fusionState);

		boolean isFusionSupported = TrackerManager.getInstance().isFusionSupported();

		Log.d(TAG, "Fusion Supported: " + isFusionSupported);

		if (!isFusionSupported) {
			Log.e(TAG, "Fusion Tracking is NOT supported on this device!");
			return;
		}



		if(fusionState != 1) {
			return;
		}


		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		AtomicReference<Rect> lastQrCodeBoundingBox = new AtomicReference<>(null);

		for (int i = 0; i < trackingCount; i++) {
			Trackable trackable = trackingResult.getTrackable(i);

			listener.sendData(trackable.getName());


			float[] poseMatrix = trackable.getPoseMatrix();
			lastQrCodeBoundingBox.set(calculateBoundingBoxFromPose(poseMatrix, 0.1f, 0.1f));

			texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
			texturedCubeRenderer.setTransform(poseMatrix);
			texturedCubeRenderer.setTranslate(0, 0, -0.0005f);
			texturedCubeRenderer.setScale(0.1f, 0.1f, 0.001f);

		//	texturedCubeRenderer.draw();
		}

		     // TODO rendering textview and video

//		if (lastQrCodeBoundingBox.get() != null && activity instanceof QrCodeFusionTrackerActivity) {
//			QrCodeFusionTrackerActivity qrActivity = (QrCodeFusionTrackerActivity) activity;
//
//			// with video
//			//activity.runOnUiThread(() -> qrActivity.updateOverlayViews(lastQrCodeBoundingBox.get()));
//
//			// only text L & R
//		//	activity.runOnUiThread(() -> qrActivity.updateOverlayViewLeftAndRight(lastQrCodeBoundingBox.get()));
//
//			//TODO 4 step right of qrcode
//
//			 activity.runOnUiThread(() -> qrActivity.updateOverlayViewRight1(lastQrCodeBoundingBox.get()));
//		}

		// todo scanqrcode activity

		if (lastQrCodeBoundingBox.get() != null && activity instanceof ScanQrcodeActivity) {
			ScanQrcodeActivity qrActivity = (ScanQrcodeActivity) activity;


			activity.runOnUiThread(() -> qrActivity.updateOverlayViewRight(lastQrCodeBoundingBox.get()));
		}
	}


// TODO for right position set textview and other

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
		int left = Math.min(screenCorners[0].x, screenCorners[2].x);
		int top = Math.min(screenCorners[0].y, screenCorners[2].y);
		int right = Math.max(screenCorners[1].x, screenCorners[3].x);
		int bottom = Math.max(screenCorners[1].y, screenCorners[3].y);


		return new Rect(left, top, right, bottom);
	}


	// working

	private Point worldToScreen(float[] worldPoint) {
		if (worldPoint.length < 3) {
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

	//	int screenY = (int) ((clipCoords[1] + 1) * 0.5 * surfaceHeight);


		return new Point(screenX, screenY);
	}







	// TODO set qrcode corner textview
//
//	private Rect calculateBoundingBoxFromPose(float[] poseMatrix, float width, float height) {
//		float x = poseMatrix[12]; // Translation X
//		float y = poseMatrix[13]; // Translation Y
//		float z = poseMatrix[14]; // Translation Z
//
//		float halfWidth = width / 2f;
//		float halfHeight = height / 2f;
//
//		// Define corners
//		float[][] corners = {
//				{x - halfWidth, y - halfHeight, z, 1.0f}, // Top-left
//				{x + halfWidth, y - halfHeight, z, 1.0f}, // Top-right
//				{x - halfWidth, y + halfHeight, z, 1.0f}, // Bottom-left
//				{x + halfWidth, y + halfHeight, z, 1.0f}  // Bottom-right
//		};
//
//		// Convert world coordinates to screen coordinates
//		Point[] screenCorners = new Point[4];
//		for (int i = 0; i < 4; i++) {
//			screenCorners[i] = worldToScreen(corners[i]);
//		}
//
//		// Ensure correct left, right, top, bottom calculations
//		int left = Math.min(screenCorners[0].x, screenCorners[1].x);
//		int top = Math.min(screenCorners[0].y, screenCorners[2].y);
//		int right = Math.max(screenCorners[2].x, screenCorners[3].x);
//		int bottom = Math.max(screenCorners[1].y, screenCorners[3].y);
//
//		return new Rect(left, top, right, bottom);
//	}
//	private Point worldToScreen(float[] worldPoint) {
//		if (worldPoint.length < 3) {
//			throw new IllegalArgumentException("worldPoint must have at least 3 elements");
//		}
//
//		float[] worldPointFixed = {worldPoint[0], worldPoint[1], worldPoint[2], 1.0f};
//		float[] clipCoords = new float[4];
//
//		Matrix.multiplyMV(clipCoords, 0, CameraDevice.getInstance().getProjectionMatrix(), 0, worldPointFixed, 0);
//
//		// Ensure perspective division
//		if (clipCoords[3] != 0) {
//			clipCoords[0] /= clipCoords[3];
//			clipCoords[1] /= clipCoords[3];
//		}
//
//		// Convert to screen space
//		int screenX = (int) ((clipCoords[0] + 1) * 0.5 * surfaceWidth);
//		int screenY = (int) ((1 - clipCoords[1]) * 0.5 * surfaceHeight);
//
//		return new Point(screenX, screenY);
//	}



	// new add

/*	private Rect calculateBoundingBoxFromPose(float[] poseMatrix, float width, float height) {
		float x = poseMatrix[12];
		float y = poseMatrix[13];
		float z = poseMatrix[14];

		float halfWidth = width / 2f;
		float halfHeight = height / 2f;

		float[][] corners = {
				{x - halfWidth, y - halfHeight, z, 1.0f}, // Top-left
				{x + halfWidth, y - halfHeight, z, 1.0f}, // Top-right
				{x + halfWidth, y + halfHeight, z, 1.0f}, // Bottom-right
				{x - halfWidth, y + halfHeight, z, 1.0f}  // Bottom-left
		};

		Point[] screenCorners = new Point[4];
		for (int i = 0; i < 4; i++) {
			screenCorners[i] = worldToScreen(corners[i]);
		}

		int left = Math.min(screenCorners[0].x, screenCorners[1].x);
		int top = Math.min(screenCorners[0].y, screenCorners[3].y);
		int right = Math.max(screenCorners[2].x, screenCorners[3].x);
		int bottom = Math.max(screenCorners[1].y, screenCorners[2].y);

		return new Rect(left, top, right, bottom);
	}


	private Point worldToScreen(float[] worldPoint) {
		if (worldPoint.length < 3) {
			throw new IllegalArgumentException("worldPoint must have at least 3 elements");
		}

		float[] worldPointFixed = {worldPoint[0], worldPoint[1], worldPoint[2], 1.0f};

		float[] clipCoords = new float[4];
		Matrix.multiplyMV(clipCoords, 0, CameraDevice.getInstance().getProjectionMatrix(), 0, worldPointFixed, 0);

		if (clipCoords[3] != 0) {
			clipCoords[0] /= clipCoords[3];  // Normalize x
			clipCoords[1] /= clipCoords[3];  // Normalize y
		}

		// Convert normalized device coordinates to screen coordinates
		int screenX = (int) ((clipCoords[0] * 0.5f + 0.5f) * surfaceWidth);
		int screenY = (int) ((1.0f - (clipCoords[1] * 0.5f + 0.5f)) * surfaceHeight); // Flip Y axis

		return new Point(screenX, screenY);
	}*/


}
