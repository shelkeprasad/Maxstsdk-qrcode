/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */
package com.maxst.ar.sample.imageTracker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;
import android.widget.Toast;

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
import com.maxst.ar.sample.arobject.Yuv420spRenderer;
import com.maxst.ar.sample.arobject.ChromaKeyVideoRenderer;
import com.maxst.ar.sample.arobject.ColoredCubeRenderer;
import com.maxst.ar.sample.arobject.TexturedCubeRenderer;
import com.maxst.ar.sample.arobject.VideoRenderer;
import com.maxst.videoplayer.VideoPlayer;

import java.util.Arrays;

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
	private long lastDetectNs = 0L;

	//
	private float[] stablePose = new float[16];
	private boolean hasStablePose = false;
	private long lastStableTime = 0L;
	private static final long STABLE_TIMEOUT_NS = 150_000_000; // 150ms

	float[] cameraWorld;


	private float stableWidth = 0f;
	private float stableHeight = 0f;




	ImageTrackerRenderer(Activity activity) {
		this.activity = activity;
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

		MaxstAR.onSurfaceChanged(width, height);
	}


//	@Override
//	public void onDrawFrame(GL10 unused) {
//		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//		GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
//
//		TrackingState state = TrackerManager.getInstance().updateTrackingState();
//		TrackingResult trackingResult = state.getTrackingResult();
//		TrackedImage image = state.getImage();
//
//		float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();
//		float[] backgroundPlaneInfo = CameraDevice.getInstance().getBackgroundPlaneInfo();
//
//		// Draw camera background
//		backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo);
//
//		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//
//		// Sceneform-related variables
//		boolean glacierDetected = false;
//		float[] glacierPose = null;
//		float glacierWidth = 0f;
//		float glacierHeight = 0f;
//
//		// Video flags
//		boolean legoDetected = false;
//		boolean blocksDetected = false;
//		boolean fetteDetected = false;
//
//		// Iterate detected trackables
//		for (int i = 0; i < trackingResult.getCount(); i++) {
//			Trackable trackable = trackingResult.getTrackable(i);
//			String name = trackable.getName();
//
//			switch (name) {
//				case "Lego":
//					legoDetected = true;
//					if (videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
//							videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
//						videoRenderer.getVideoPlayer().start();
//					}
//					videoRenderer.setProjectionMatrix(projectionMatrix);
//					videoRenderer.setTransform(trackable.getPoseMatrix());
//					videoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
//					videoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
//					videoRenderer.draw();
//					break;
//
//				case "Blocks":
//					blocksDetected = true;
//					if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
//							chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
//						chromaKeyVideoRenderer.getVideoPlayer().start();
//					}
//					chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
//					chromaKeyVideoRenderer.setTransform(trackable.getPoseMatrix());
//					chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
//					chromaKeyVideoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
//					chromaKeyVideoRenderer.draw();
//					break;
//
//				case "Glacier":
//					glacierDetected = true;
//					glacierPose = trackable.getPoseMatrix();
//					glacierWidth = trackable.getWidth();
//					glacierHeight = trackable.getHeight();
//					lastDetectNs = System.nanoTime();
//
//					if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
//							chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
//						chromaKeyVideoRenderer.getVideoPlayer().start();
//					}
//					chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
//					chromaKeyVideoRenderer.setTransform(glacierPose);
//
//					float offsetX = -glacierWidth * 1.3f;
//					chromaKeyVideoRenderer.setTranslate(offsetX, 0.0f, 0.0f);
//					chromaKeyVideoRenderer.setScale(glacierWidth, glacierHeight, 1.0f);
//					chromaKeyVideoRenderer.draw();
//
//					// Save stable pose + size for freeze-after-loss behavior
//					System.arraycopy(trackable.getPoseMatrix(), 0, stablePose, 0, 16);
//					stableWidth = trackable.getWidth();
//					stableHeight = trackable.getHeight();
//					hasStablePose = true;
//					lastStableTime = System.nanoTime();
//					break;
//
//				case "FetteMachine":
//					fetteDetected = true;
//					if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
//							chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
//						chromaKeyVideoRenderer.getVideoPlayer().start();
//					}
//					chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
//					chromaKeyVideoRenderer.setTransform(trackable.getPoseMatrix());
//					chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
//					chromaKeyVideoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
//					chromaKeyVideoRenderer.draw();
//					break;
//
//				default:
//					coloredCubeRenderer.setProjectionMatrix(projectionMatrix);
//					coloredCubeRenderer.setTransform(trackable.getPoseMatrix());
//					coloredCubeRenderer.setTranslate(0, 0, -0.1f);
//					coloredCubeRenderer.setScale(trackable.getWidth(), trackable.getHeight(), -0.1f);
//					coloredCubeRenderer.draw();
//					break;
//			}
//		} // end for loop
//
//		// Freeze logic: show live pose while detected; if lost, show frozen last stable for STABLE_TIMEOUT_NS
//		if (hasStablePose) {
//			long now = System.nanoTime();
//			boolean showFrozen = false;
//
//			if (glacierDetected) {
//				// live detection — use current pose (we also update below)
//				showFrozen = false;
//			} else if (now - lastStableTime < STABLE_TIMEOUT_NS) {
//				// within timeout after loss -> re-use last stable pose
//				showFrozen = true;
//			}
//
//			if (showFrozen) {
//				((ImageTrackerActivity) activity).updateSceneformPose(stablePose, stableWidth, stableHeight);
//			} else if (!glacierDetected) {
//				// timed out and no current detection -> hide model
//				((ImageTrackerActivity) activity).hideSceneformModel();
//				hasStablePose = false;
//			}
//		}
//
//		// If target currently detected, update Sceneform with live pose (this will override frozen if both true)
//		if (glacierDetected && glacierPose != null) {
//			((ImageTrackerActivity) activity).updateSceneformPose(glacierPose, glacierWidth, glacierHeight);
//		}
//
//		// Pause videos when their targets are not visible
//		if (!legoDetected && videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
//			videoRenderer.getVideoPlayer().pause();
//		}
//		if (!blocksDetected && !fetteDetected &&
//				chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
//			chromaKeyVideoRenderer.getVideoPlayer().pause();
//		}
//	}




	@Override
	public void onDrawFrame(GL10 unused) {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);

		TrackingState state = TrackerManager.getInstance().updateTrackingState();
		TrackingResult trackingResult = state.getTrackingResult();
		TrackedImage image = state.getImage();

		float[] cameraViewMatrix = new float[16];

		float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();
		float[] backgroundPlaneInfo = CameraDevice.getInstance().getBackgroundPlaneInfo();

		backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo);

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		// -------- SCENEFORM VARIABLES ----------
		boolean glacierDetected = false;
		float[] glacierPose = null;
		float glacierWidth = 0;
		float glacierHeight = 0;

		// -------- VIDEO LOGIC VARIABLES ----------
		boolean legoDetected = false;
		boolean blocksDetected = false;
		boolean fetteDetected = false;

		// -----------------------------------------
		for (int i = 0; i < trackingResult.getCount(); i++) {
			Trackable trackable = trackingResult.getTrackable(i);

			String name = trackable.getName();

			switch (name) {

				case "Lego":
					legoDetected = true;

					if (videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
							videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
						videoRenderer.getVideoPlayer().start();
					}

					videoRenderer.setProjectionMatrix(projectionMatrix);
					videoRenderer.setTransform(trackable.getPoseMatrix());
					videoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
					videoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
					videoRenderer.draw();
					break;

				case "Blocks":
					blocksDetected = true;

					if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
							chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
						chromaKeyVideoRenderer.getVideoPlayer().start();
					}

					chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
					chromaKeyVideoRenderer.setTransform(trackable.getPoseMatrix());
					chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
					chromaKeyVideoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
					chromaKeyVideoRenderer.draw();
					break;

				case "Glacier":
					glacierDetected = true;
					glacierPose = trackable.getPoseMatrix();
					glacierWidth = trackable.getWidth();
					glacierHeight = trackable.getHeight();
					lastDetectNs = System.nanoTime();

					// --- IF you want GLACIER to also play chromaKey video ---
					if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
							chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
						chromaKeyVideoRenderer.getVideoPlayer().start();
					}

					chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
					chromaKeyVideoRenderer.setTransform(glacierPose);

					// new

					float offsetX = -glacierWidth * 1.3f;
					float offsetY = 0.0f;
					float offsetZ = 0.0f;

					chromaKeyVideoRenderer.setTranslate(offsetX, offsetY, offsetZ);

				//	chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);

					chromaKeyVideoRenderer.setScale(glacierWidth, glacierHeight, 1.0f);
					chromaKeyVideoRenderer.draw();

					glacierDetected = true;

					// Keep the pose stable when detecting
					System.arraycopy(trackable.getPoseMatrix(), 0, stablePose, 0, 16);
					hasStablePose = true;
					lastStableTime = System.nanoTime();

					glacierWidth = trackable.getWidth();
					glacierHeight = trackable.getHeight();

					float[] poseCameraSpace = glacierPose;

				 cameraWorld = new float[16];
					Matrix.invertM(cameraWorld, 0, poseCameraSpace, 0);


					System.arraycopy(trackable.getPoseMatrix(), 0, stablePose, 0, 16);
					hasStablePose = true;
					lastStableTime = System.nanoTime();
					stableWidth = trackable.getWidth();
					stableHeight = trackable.getHeight();

					break;

				case "FetteMachine":
					fetteDetected = true;

					if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
							chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
						chromaKeyVideoRenderer.getVideoPlayer().start();
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



//		if (hasStablePose) {
//			long now = System.nanoTime();
//
//			boolean useFrozen = false;
//
//			if (glacierDetected) {
//				useFrozen = true;
//			} else if (now - lastStableTime < 200_000_000) {
//				useFrozen = true;
//			}
//
//			if (useFrozen) {
//				((ImageTrackerActivity) activity).updateSceneformPose(
//						stablePose,
//						stableWidth,
//						stableHeight
//				);
//			} else {
//				((ImageTrackerActivity) activity).hideSceneformModel();
//				hasStablePose = false;
//			}
//		}





		float[] identityView = new float[16];
		Matrix.setIdentityM(identityView, 0);

		if (glacierDetected) {
			((ImageTrackerActivity) activity).updateSceneformPose(
					glacierPose,
					glacierWidth,
					glacierHeight
				//	cameraWorld
				//	projectionMatrix
				//	identityView
			);

//			Log.d("POSE_TRACKABLE",
//					"Trackable Pose: " + mat(glacierPose));

		} else {

			((ImageTrackerActivity) activity).hideSceneformModel();
		}


		if (!legoDetected && videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
			videoRenderer.getVideoPlayer().pause();
		}

		if (!blocksDetected && !fetteDetected &&
				chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
			chromaKeyVideoRenderer.getVideoPlayer().pause();
		}
	}


	private String mat(float[] m) {
		return String.format(
				"\n[%f %f %f %f]\n[%f %f %f %f]\n[%f %f %f %f]\n[%f %f %f %f]",
				m[0],m[1],m[2],m[3],
				m[4],m[5],m[6],m[7],
				m[8],m[9],m[10],m[11],
				m[12],m[13],m[14],m[15]
		);
	}


//
//	@Override
//	public void onDrawFrame(GL10 unused) {
//		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//		GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
//
//		TrackingState state = TrackerManager.getInstance().updateTrackingState();
//		TrackingResult trackingResult = state.getTrackingResult();
//
//		TrackedImage image = state.getImage();
//
//		float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();
//		float[] backgroundPlaneInfo = CameraDevice.getInstance().getBackgroundPlaneInfo();
//
//		backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo);
//
//		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//
//		// ----------- ADDED FOR SCENEFORM INTEGRATION ---------------
//		boolean targetDetected = false;
//		float[] detectedPose = null;
//		float targetWidth = 0;
//		float targetHeight = 0;
//		// ------------------------------------------------------------
//
//		for (int i = 0; i < trackingResult.getCount(); i++) {
//			Trackable trackable = trackingResult.getTrackable(i);
//
//			// ---- ADDED: detect only specific target name ----
//			if (trackable.getName().equals("Glacier")) {
//
//				targetDetected = true;
//				detectedPose = trackable.getPoseMatrix();
//				targetWidth = trackable.getWidth();
//				targetHeight = trackable.getHeight();
//				lastDetectNs = System.nanoTime();
//
//			}
//		}
//
//
//
//
//		if (targetDetected) {
////			((ImageTrackerActivity) activity)
////					.updateSceneformPose(detectedPose, targetWidth, targetHeight);
//
//
//			((ImageTrackerActivity) activity)
//					.updateSceneformPose(detectedPose, targetWidth, targetHeight, projectionMatrix);
//
//			// todo working 3
//
////			((ImageTrackerActivity) activity)
////					.updateSceneformPose(detectedPose, targetWidth, targetHeight);
//
//
//
//
//		} else {
//			((ImageTrackerActivity) activity).hideSceneformModel();
//		}
//	}

	void destroyVideoPlayer() {
		videoRenderer.getVideoPlayer().destroy();
		chromaKeyVideoRenderer.getVideoPlayer().destroy();
	}


//	@Override
//	public void onDrawFrame(GL10 unused) {
//		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//		GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
//
//		TrackingState state = TrackerManager.getInstance().updateTrackingState();
//		TrackingResult trackingResult = state.getTrackingResult();
//
//		TrackedImage image = state.getImage();
//		float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();
//		float[] backgroundPlaneInfo = CameraDevice.getInstance().getBackgroundPlaneInfo();
//
//		backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo);
//
//		boolean legoDetected = false;
//		boolean blocksDetected = false;
//
//
//		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//		for (int i = 0; i < trackingResult.getCount(); i++) {
//			Trackable trackable = trackingResult.getTrackable(i);
//
//			//Log.i(TAG, "Image width : " + trackable.getWidth() + ", height : " + trackable.getHeight());
//
//			switch (trackable.getName()) {
//				case "Lego":
//					legoDetected = true;
//					if (videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
//							videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
//						videoRenderer.getVideoPlayer().start();
//					}
//					videoRenderer.setProjectionMatrix(projectionMatrix);
//					videoRenderer.setTransform(trackable.getPoseMatrix());
//					videoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
//					videoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
//					videoRenderer.draw();
//					break;
//
//				case "Blocks":
//					blocksDetected = true;
//					if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
//							chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
//						chromaKeyVideoRenderer.getVideoPlayer().start();
//					}
//					chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
//					chromaKeyVideoRenderer.setTransform(trackable.getPoseMatrix());
//					chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
//					chromaKeyVideoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
//					chromaKeyVideoRenderer.draw();
//					break;
//
//				case "Glacier":
//					texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
//					texturedCubeRenderer.setTransform(trackable.getPoseMatrix());
//					texturedCubeRenderer.setTranslate(0, 0, -0.05f);
//					texturedCubeRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 0.1f);
//					texturedCubeRenderer.draw();
//					break;
//
//				case "FetteMachine":
//					blocksDetected = true;
//					if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_READY ||
//							chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PAUSE) {
//						chromaKeyVideoRenderer.getVideoPlayer().start();
//					}
//					chromaKeyVideoRenderer.setProjectionMatrix(projectionMatrix);
//					chromaKeyVideoRenderer.setTransform(trackable.getPoseMatrix());
//					chromaKeyVideoRenderer.setTranslate(0.0f, 0.0f, 0.0f);
//					chromaKeyVideoRenderer.setScale(trackable.getWidth(), trackable.getHeight(), 1.0f);
//					chromaKeyVideoRenderer.draw();
//					break;
//
//				default:
//					coloredCubeRenderer.setProjectionMatrix(projectionMatrix);
//					coloredCubeRenderer.setTransform(trackable.getPoseMatrix());
//					coloredCubeRenderer.setTranslate(0, 0, -0.1f);
//					coloredCubeRenderer.setScale(trackable.getWidth(), trackable.getHeight(), -0.1f);
//					coloredCubeRenderer.draw();
//			}
//		}
//
//		if (!legoDetected) {
//			if (videoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
//				videoRenderer.getVideoPlayer().pause();
//			}
//		}
//
//		if (!blocksDetected) {
//			if (chromaKeyVideoRenderer.getVideoPlayer().getState() == VideoPlayer.STATE_PLAYING) {
//				chromaKeyVideoRenderer.getVideoPlayer().pause();
//			}
//		}
//	}

}
