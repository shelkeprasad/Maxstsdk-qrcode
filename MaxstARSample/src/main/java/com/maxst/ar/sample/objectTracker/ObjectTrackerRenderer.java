/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */

package com.maxst.ar.sample.objectTracker;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;

import com.maxst.ar.CameraDevice;
import com.maxst.ar.GuideInfo;
import com.maxst.ar.MaxstAR;

import com.maxst.ar.MaxstARUtil;
import com.maxst.ar.TagAnchor;
import com.maxst.ar.Trackable;
import com.maxst.ar.TrackedImage;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.TrackingResult;
import com.maxst.ar.TrackingState;
import com.maxst.ar.sample.arobject.AxisRenderer;
import com.maxst.ar.sample.arobject.BackgroundRenderHelper;
import com.maxst.ar.sample.arobject.BoundingShapeRenderer;
import com.maxst.ar.sample.arobject.FeaturePointRenderer;
import com.maxst.ar.sample.arobject.SphereRenderer;
import com.maxst.ar.sample.arobject.TexturedCircleRenderer;
import com.maxst.ar.sample.arobject.TexturedCubeRenderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class ObjectTrackerRenderer implements Renderer {

	public int surfaceWidth;
	public int surfaceHeight;
	private final Activity activity;

	//private TexturedCubeRenderer texturedCubeRenderer;
	private TexturedCircleRenderer texturedCubeRenderer;
	private FeaturePointRenderer featurePointRenderer;
	private SphereRenderer sphereRenderer;
	private BoundingShapeRenderer boundingShapeRenderer;
	private BackgroundRenderHelper backgroundRenderHelper;
	private List<float[]> randomOffsets = new ArrayList<>();

	private List<String> sensorTextList = Arrays.asList(
			"Temp", "Humidity", "Pressure", "CO2", "Light", "Sound", "PM2.5", "VOC"
	);

	private List<Bitmap> textures = new ArrayList<>();



	ObjectTrackerRenderer(Activity activity) {
		this.activity = activity;
	}

	// todo working earliear

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
//		featurePointRenderer.setProjectionMatrix(projectionMatrix);
//		GuideInfo gi = TrackerManager.getInstance().getGuideInformation();
//		featurePointRenderer.draw(gi, trackingResult);
//		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//
////		if (trackingResult.getCount() > 0) {
////			Trackable trackable = trackingResult.getTrackable(0);
////
////
////			float[] bb = gi.getBoundingBox();
////
////			boundingShapeRenderer.setProjectionMatrix(projectionMatrix);
////			boundingShapeRenderer.setTransform(trackable.getPoseMatrix());
////			boundingShapeRenderer.setTranslate(bb[0], bb[1], bb[2]);
////			boundingShapeRenderer.setScale(bb[3], bb[4], bb[5]);
////			boundingShapeRenderer.draw();
////
////			int anchorCount = gi.getTagAnchorCount();
////
////			if(anchorCount > 0) {
////				TagAnchor[] anchors = gi.getTagAnchors();
////				for(int i=0; i<anchorCount; i++) {
////					TagAnchor eachAnchor = anchors[i];
////					sphereRenderer.setProjectionMatrix(projectionMatrix);
////					sphereRenderer.setTransform(trackable.getPoseMatrix());
////					sphereRenderer.setTranslate((float)eachAnchor.positionX, (float)eachAnchor.positionY, (float)eachAnchor.positionZ);
////					sphereRenderer.setScale(0.02f, 0.02f, 0.02f);
////					sphereRenderer.setRotation(-90.0f, 1.0f, 0.0f, 0.0f);
////					sphereRenderer.draw();
////				}
////			}
////		}
//
//
//		// new
//
//		if (trackingResult.getCount() > 0) {
//			Trackable trackable = trackingResult.getTrackable(0);
//			float[] poseMatrix = trackable.getPoseMatrix();
//
//			for (float[] offset : randomOffsets) {
//				texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
//				texturedCubeRenderer.setTransform(poseMatrix);
//				texturedCubeRenderer.setTranslate(offset[0], offset[1], offset[2]);
//			//	texturedCubeRenderer.setScale(0.01f, 0.01f, 0.01f);
//			//	texturedCubeRenderer.setScale(0.09f, 0.09f, 0.09f);
//				texturedCubeRenderer.setScale(0.2f, 0.2f, 0.2f); // Larger cube
//
//
//				texturedCubeRenderer.draw();
//			}
//		}
//
//
//	}


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

		featurePointRenderer.setProjectionMatrix(projectionMatrix);
		GuideInfo gi = TrackerManager.getInstance().getGuideInformation();
		featurePointRenderer.draw(gi, trackingResult);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		if (trackingResult.getCount() > 0) {
			Trackable trackable = trackingResult.getTrackable(0);
			float[] poseMatrix = trackable.getPoseMatrix();
			float[] bb = gi.getBoundingBox(); // [centerX, centerY, centerZ, sizeX, sizeY, sizeZ]

			// Only generate offsets once, when tracking is detected
			if (randomOffsets.isEmpty()) {
				generateOffsetsInsideModel(bb);
			}

//			for (float[] offset : randomOffsets) {
//				texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
//				texturedCubeRenderer.setTransform(poseMatrix);
//
//				// Translate relative to center of model + offset within bounding box
//				texturedCubeRenderer.setTranslate(bb[0] + offset[0], bb[1] + offset[1], bb[2] + offset[2]);
//				texturedCubeRenderer.setScale(0.2f, 0.2f, 0.2f); // Optional: adjust cube size
//				texturedCubeRenderer.draw();
//			}


			for (int i = 0; i < randomOffsets.size(); i++) {
				float[] offset = randomOffsets.get(i);
				Bitmap textTexture = textures.get(i);

				texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
				texturedCubeRenderer.setTransform(poseMatrix);
				texturedCubeRenderer.setTranslate(bb[0] + offset[0], bb[1] + offset[1], bb[2] + offset[2]);
				texturedCubeRenderer.setScale(0.2f, 0.2f, 0.2f);

				texturedCubeRenderer.setTextureBitmap(textTexture); // ← new texture per cube
				texturedCubeRenderer.draw();
			}

		}
	}


	// Generates offsets inside the bounding box volume of the model



//	private void generateOffsetsInsideModel(float[] boundingBox) {
//		randomOffsets.clear();
//		Random random = new Random();
//
//		float sizeX = boundingBox[3];
//		float sizeY = boundingBox[4];
//		float sizeZ = boundingBox[5];
//
//		for (int i = 0; i < 8; i++) {
//			// Random offset within half size in each direction (centered)
//			float xOffset = (random.nextFloat() - 0.5f) * sizeX;
//			float yOffset = (random.nextFloat() - 0.5f) * sizeY;
//			float zOffset = (random.nextFloat() - 0.5f) * sizeZ;
//
//			randomOffsets.add(new float[]{xOffset, yOffset, zOffset});
//		}
//	}


	private void generateOffsetsInsideModel(float[] boundingBox) {
		randomOffsets.clear();
		textures.clear();

		Random random = new Random();
		float sizeX = boundingBox[3];
		float sizeY = boundingBox[4];
		float sizeZ = boundingBox[5];

		for (int i = 0; i < sensorTextList.size(); i++) {
			float xOffset = (random.nextFloat() - 0.5f) * sizeX;
			float yOffset = (random.nextFloat() - 0.5f) * sizeY;
			float zOffset = (random.nextFloat() - 0.5f) * sizeZ;
			randomOffsets.add(new float[]{xOffset, yOffset, zOffset});

			// Generate bitmap texture from text
			Bitmap textBitmap = createTextBitmap(sensorTextList.get(i));
			textures.add(textBitmap); // Store bitmap per cube
		}
	}



	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		surfaceWidth = width;
		surfaceHeight = height;

		MaxstAR.onSurfaceChanged(width, height);
	}

	// todo working earliear

//	@Override
//	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
//		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//
//		texturedCubeRenderer = new TexturedCubeRenderer(activity);
//		Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("MaxstAR_Cube.png", activity.getAssets());
//		texturedCubeRenderer.setTextureBitmap(bitmap);
//
//		featurePointRenderer = new FeaturePointRenderer();
//		Bitmap blueBitmap = MaxstARUtil.getBitmapFromAsset("bluedot.png", activity.getAssets());
//		Bitmap redBitmap = MaxstARUtil.getBitmapFromAsset("reddot.png", activity.getAssets());
//		featurePointRenderer.setFeatureImage(blueBitmap, redBitmap);
//
//		sphereRenderer = new SphereRenderer(1.0f, 0.0f, 0.0f, 1.0f);
//
//		boundingShapeRenderer = new BoundingShapeRenderer();
//		backgroundRenderHelper = new BackgroundRenderHelper();
//		CameraDevice.getInstance().setClippingPlane(0.03f, 70.0f);
//
//		//
//		generateRandomOffsets();
//	}


	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

	//	texturedCubeRenderer = new TexturedCubeRenderer(activity);
		// todo need change
		texturedCubeRenderer = new TexturedCircleRenderer(activity);

		Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("MaxstAR_Cube.png", activity.getAssets());
		texturedCubeRenderer.setTextureBitmap(bitmap);

		featurePointRenderer = new FeaturePointRenderer();
		Bitmap blueBitmap = MaxstARUtil.getBitmapFromAsset("bluedot.png", activity.getAssets());
		Bitmap redBitmap = MaxstARUtil.getBitmapFromAsset("reddot.png", activity.getAssets());
		featurePointRenderer.setFeatureImage(blueBitmap, redBitmap);

		sphereRenderer = new SphereRenderer(1.0f, 0.0f, 0.0f, 1.0f);
		boundingShapeRenderer = new BoundingShapeRenderer();
		backgroundRenderHelper = new BackgroundRenderHelper();
		CameraDevice.getInstance().setClippingPlane(0.03f, 70.0f);
	}



	// new
	private void generateRandomOffsets() {
		randomOffsets.clear();
		Random random = new Random();

		for (int i = 0; i < 8; i++) {
			float x = (random.nextFloat() - 0.1f) * 1.5f;
			float y = (random.nextFloat() - 0.1f) * 1.5f;
			float z = (random.nextFloat() - 0.1f) * 1.5f;
			randomOffsets.add(new float[]{x, y, z});
		}
	}

	private Bitmap createTextBitmap(String text) {
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setTextSize(64);
		paint.setColor(Color.BLACK);
		paint.setTextAlign(Paint.Align.CENTER);

		int width = 256;
		int height = 256;
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);

		// Center text
		Rect bounds = new Rect();
		paint.getTextBounds(text, 0, text.length(), bounds);
		int x = width / 2;
		int y = height / 2 - bounds.centerY();

		canvas.drawText(text, x, y, paint);
		return bitmap;
	}


}
