/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */

package com.maxst.ar.sample.instantTracker;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.load.model.Model;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.GuideInfo;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.MaxstARUtil;
import com.maxst.ar.Trackable;
import com.maxst.ar.TrackedImage;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.TrackingResult;
import com.maxst.ar.TrackingState;
import com.maxst.ar.sample.ObjModelLoader;
import com.maxst.ar.sample.ObjRenderer;
import com.maxst.ar.sample.R;
import com.maxst.ar.sample.ShaderUtils;
import com.maxst.ar.sample.arobject.BackgroundRenderHelper;
import com.maxst.ar.sample.arobject.TexturedCircleRenderer;
import com.maxst.ar.sample.arobject.TexturedCylinderRenderer;
import com.maxst.ar.sample.arobject.TexturedSphereRenderer;
import com.maxst.ar.sample.arobject.Yuv420spRenderer;
import com.maxst.ar.sample.arobject.TexturedCubeRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class InstantTrackerRenderer implements Renderer {

    public static final String TAG = InstantTrackerRenderer.class.getSimpleName();

    private int surfaceWidth;
    private int surfaceHeight;


  //  private TexturedCubeRenderer texturedCubeRenderer;
    //	private TexturedSphereRenderer texturedCubeRenderer;

  //  private TexturedCircleRenderer texturedCubeRenderer;


    private TexturedCylinderRenderer texturedCubeRenderer;
    private float posX;
    private float posY;
    private Activity activity;

    private BackgroundRenderHelper backgroundRenderHelper;

    private static final int MAX_MODELS = 8;
    private final List<float[]> modelPositions = new ArrayList<>();

    InstantTrackerRenderer(Activity activity) {
        this.activity = activity;
    }

    private List<String> sensorTextList = Arrays.asList(
            "Temp", "Humidity", "Pressure", "CO2", "Light", "Sound", "PM2.5", "VOC"
    );

    private List<Bitmap> textures = new ArrayList<>();

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

        if (trackingResult.getCount() == 0) {
            return;
        }

        Trackable trackable = trackingResult.getTrackable(0);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        texturedCubeRenderer.setTransform(trackable.getPoseMatrix());
        //	texturedCubeRenderer.setTranslate(posX, posY, -0.05f);
        texturedCubeRenderer.setProjectionMatrix(projectionMatrix);

        // new add
        for (int i = 0; i < modelPositions.size(); i++) {
            Bitmap textBitmap = createTextBitmap(sensorTextList.get(i));
            textures.add(textBitmap);
        }


        for (int i = 0; i < modelPositions.size(); i++) {
            float[] pos = modelPositions.get(i);

        //    texturedCubeRenderer.setTranslate(pos[0], pos[1], -0.05f);

         //   texturedCubeRenderer.setTranslate(pos[0], pos[1], 2.0f);
         //   texturedCubeRenderer.setTranslate(pos[0], pos[1], 1.0f);
          //  texturedCubeRenderer.setTranslate(pos[0], pos[1], 6.0f);

            texturedCubeRenderer.setTranslate(pos[0], pos[1], pos[2]);



            //  texturedCubeRenderer.setTranslate(pos[0], pos[1], -1.5f);



         //   texturedCubeRenderer.setTranslate(pos[0], pos[1], 0.8f);

            // todo for text bitmap

            Bitmap textTexture = textures.get(i);
            texturedCubeRenderer.setTextureBitmap(textTexture);


            // todo sphare image

//            Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("sphare.png", activity.getAssets());
//            texturedCubeRenderer.setTextureBitmap(bitmap);

            texturedCubeRenderer.draw();
        }

        //	texturedCubeRenderer.draw();
    }

    public void addModel(float x, float y,float z) {
        if (modelPositions.size() < MAX_MODELS) {
            modelPositions.add(new float[]{x, y,z});
        }
    }


    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {

        surfaceWidth = width;
        surfaceHeight = height;


        ///
        // todo
        //
    //    texturedCubeRenderer.setScale(0.2f, 0.2f, 0.1f);

  //      texturedCubeRenderer.setScale(0.1f, 0.1f, 0.1f);

        // todo cylinder and sphare and cube
        texturedCubeRenderer.setScale(0.07f, 0.07f, 0.07f);

        MaxstAR.onSurfaceChanged(width, height);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);


      //  texturedCubeRenderer = new TexturedCubeRenderer(activity);

        //	texturedCubeRenderer = new TexturedSphereRenderer(activity,40, 40, 1.0f);

      //     texturedCubeRenderer = new TexturedCircleRenderer(activity);

        texturedCubeRenderer = new TexturedCylinderRenderer(activity, 60, 1.0f, 2.0f);

        Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("MaxstAR_Cube.png", activity.getAssets());
      //  texturedCubeRenderer.setTextureBitmap(bitmap);

        backgroundRenderHelper = new BackgroundRenderHelper();
     //   CameraDevice.getInstance().setClippingPlane(0.03f, 70.0f);
        CameraDevice.getInstance().setClippingPlane(0.03f, 70.0f);
    }

    void setTranslate(float x, float y) {
        posX += x;
        posY += y;
    }

    void resetPosition() {
        posX = 0;
        posY = 0;
    }


    private Bitmap createTextBitmap(String text) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(64);
        paint.setColor(Color.TRANSPARENT);
        paint.setTextAlign(Paint.Align.CENTER);

        int width = 256;
        int height = 256;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
      //  canvas.drawColor(Color.LTGRAY);

        int color = ContextCompat.getColor(activity, R.color.light_blue);
        canvas.drawColor(color);

        // Center text
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);
        int x = width / 2;
        int y = height / 2 - bounds.centerY();

        canvas.drawText(text, x, y, paint);
        return bitmap;
    }

    // todo onclick model

//    public void handleTouch(float normalizedX, float normalizedY) {
//        for (int i = 0; i < modelPositions.size(); i++) {
//            float[] pos = modelPositions.get(i);
//
//            float dx = normalizedX - pos[0];
//            float dy = normalizedY - pos[1];
//            float distance = (float) Math.sqrt(dx * dx + dy * dy);
//
//            if (distance < 0.1f) { // Adjust this threshold to match your model size
//                final String label = sensorTextList.get(i);
//
//                // Run on UI thread to show dialog
//                activity.runOnUiThread(() -> {
//                    new AlertDialog.Builder(activity)
//                            .setTitle("Sensor Info")
//                            .setMessage("You clicked on: " + label)
//                            .setPositiveButton("OK", null)
//                            .show();
//                });
//
//                break; // Only handle one model at a time
//            }
//        }
//    }


    // 2

    public void handleTouch(float touchX, float touchY) {
        for (int i = 0; i < modelPositions.size(); i++) {
            float[] pos = modelPositions.get(i);

            float dx = touchX - pos[0];
            float dy = touchY - pos[1];
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < 0.08f) {    // e.g., 0.05f, 0.08f
                String label = sensorTextList.get(i);

                Toast.makeText(activity, "click 0n: "+label, Toast.LENGTH_SHORT).show();
//                activity.runOnUiThread(() -> {
//                    new AlertDialog.Builder(activity)
//                            .setTitle("Sensor Info")
//                            .setMessage("clicked on: " + label)
//                            .setPositiveButton("OK", null)
//                            .show();
//                });


                activity.runOnUiThread(() -> {
                    View dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_video, null);
                    SurfaceView surfaceView = dialogView.findViewById(R.id.surfaceView);

                    AlertDialog dialog = new AlertDialog.Builder(activity)
                            .setTitle("Sensor Info")
                            .setView(dialogView)
                            .setPositiveButton("Close", (d, w) -> {
                            })
                            .create();

                    SurfaceHolder holder = surfaceView.getHolder();
                    holder.addCallback(new SurfaceHolder.Callback() {
                        MediaPlayer mediaPlayer;

                        @Override
                        public void surfaceCreated(SurfaceHolder holder) {
                            try {
                                AssetFileDescriptor afd = activity.getAssets().openFd("step1.mp4");
                                mediaPlayer = new MediaPlayer();
                                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                                mediaPlayer.setDisplay(holder);
                                mediaPlayer.setOnPreparedListener(mp -> mediaPlayer.start());
                                mediaPlayer.prepareAsync();  // async is safer here
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

                        @Override
                        public void surfaceDestroyed(SurfaceHolder holder) {
                            if (mediaPlayer != null) {
                                mediaPlayer.release();
                            }
                        }
                    });

                    dialog.show();
                });



                break;
            }
        }
    }



}


// todo old 2

//
//
//class InstantTrackerRenderer implements Renderer {
//
//	public static final String TAG = InstantTrackerRenderer.class.getSimpleName();
//
//	private int surfaceWidth;
//	private int surfaceHeight;
//
////	private TexturedCubeRenderer texturedCubeRenderer;
//
//	// todo circle
//	private TexturedCircleRenderer texturedCubeRenderer;
//	private float posX;
//	private float posY;
//	private Activity activity;
//
//	private BackgroundRenderHelper backgroundRenderHelper;
//	private List<float[]> cubePositions;
//
//
//	// new
//
//	private List<float[]> randomOffsets = new ArrayList<>();
//
//	private List<String> sensorTextList = Arrays.asList(
//			"Temp", "Humidity", "Pressure", "CO2", "Light", "Sound", "PM2.5", "VOC"
//	);
//
//	private List<Bitmap> textures = new ArrayList<>();
//
//
//	InstantTrackerRenderer(Activity activity) {
//		this.activity = activity;
//	}
//
//	private boolean isTrackingActive = false;
//
//	public void setTrackingActive(boolean active) {
//		isTrackingActive = active;
//	}
//	private boolean isRender = false;
//
//
//	// todo working 99%
//
//
////	@Override
////	public void onDrawFrame(GL10 unused) {
////		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
////		GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
////
////		// todo new add
////
////		if (!isTrackingActive) {
////			return; // Skip rendering
////		}
////
////
////		TrackingState state = TrackerManager.getInstance().updateTrackingState();
////		TrackingResult trackingResult = state.getTrackingResult();
////
////		TrackedImage image = state.getImage();
////		float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();
////		float[] backgroundPlaneInfo = CameraDevice.getInstance().getBackgroundPlaneInfo();
////
////		backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo);
////
////		// todo only show model without detect
////			// generateOffsetsInsideModels();
////
////			// Render objects at a fixed position if no tracking result is available
////			float[] defaultPoseMatrix = new float[16];
////			Matrix.setIdentityM(defaultPoseMatrix, 0); // Identity matrix, for fixed position
////
////			for (int i = 0; i < randomOffsets.size(); i++) {
////				float[] offset = randomOffsets.get(i);
////				Bitmap textTexture = textures.get(i);
////
////				texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
////				texturedCubeRenderer.setTransform(defaultPoseMatrix); // Render without tracking
////				texturedCubeRenderer.setTranslate(offset[0], offset[1], offset[2]); // Offsets from center
////				texturedCubeRenderer.setScale(0.1f, 0.1f, 0.1f);
////				texturedCubeRenderer.setTextureBitmap(textTexture);
////				texturedCubeRenderer.draw();
////			}
////
////		// todo end
////
////
////		if (trackingResult.getCount() == 0) {
////			return;
////		}
////
////		GuideInfo gi = TrackerManager.getInstance().getGuideInformation();
////
////		Trackable trackable = trackingResult.getTrackable(0);
////
////		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
////
//////		texturedCubeRenderer.setTransform(trackable.getPoseMatrix());
//////		texturedCubeRenderer.setTranslate(posX, posY, -0.05f);
//////		texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
//////		texturedCubeRenderer.draw();
////
////
////
////		// todo cube
////
//////		for (float[] pos : cubePositions) {
//////			texturedCubeRenderer.setTransform(trackable.getPoseMatrix());
//////			texturedCubeRenderer.setTranslate(pos[0], pos[1], pos[2]);
//////			texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
//////			texturedCubeRenderer.draw();
//////		}
////
////
////
////		//
////
////
////
////		if (trackingResult.getCount() > 0) {
////		//	Trackable trackable = trackingResult.getTrackable(0);
////			float[] poseMatrix = trackable.getPoseMatrix();
////			float[] bb = gi.getBoundingBox(); // [centerX, centerY, centerZ, sizeX, sizeY, sizeZ]
////
////			// Only generate offsets once, when tracking is detected
////			if (randomOffsets.isEmpty()) {
////				if (bb == null || bb.length < 6) {
////					Log.e("InstantTracker", "Bounding box is invalid.");
////				} else {
////				//	generateOffsetsInsideModel(bb);
////				}
////			}
////
////		}
////
////	}
//
//
//
//
//
//	@Override
//	public void onDrawFrame(GL10 unused) {
//		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
//		GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight);
//
//		// Skip rendering if tracking is not active
//		if (!isTrackingActive) {
//			return;
//		}
//
//		// Update the tracking state
//		TrackingState state = TrackerManager.getInstance().updateTrackingState();
//		TrackingResult trackingResult = state.getTrackingResult();
//
//		// Get camera pose and projection matrix
//		float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();
//		float[] cameraPose = CameraDevice.getInstance().getPoseMatrix();  // Assuming you have a method to get camera pose
//
//		// Render background if camera image is available
//		TrackedImage image = state.getImage();
//		if (image != null) {
//			float[] backgroundPlaneInfo = CameraDevice.getInstance().getBackgroundPlaneInfo();
//			backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo);
//		}
//
//		// Render objects at a fixed position if no tracking result
////		if (trackingResult.getCount() == 0) {
////			renderObjectsWithoutTracking(projectionMatrix);
////			return;
////		}
//
//		// If tracking is active, render the objects in the AR world based on tracking result
//		Trackable trackable = trackingResult.getTrackable(0);
//		if (trackable != null) {
//			GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//
//			// Get the pose of the trackable object
//			float[] trackablePoseMatrix = trackable.getPoseMatrix();
//
//			// Apply the camera pose and trackable pose to each object in the AR scene
//			for (int i = 0; i < randomOffsets.size(); i++) {
//				float[] offset = randomOffsets.get(i);
//				Bitmap textTexture = textures.get(i);
//
//				// Create the model matrix for the object
//				float[] modelMatrix = new float[16];
//				Matrix.setIdentityM(modelMatrix, 0);
//
//				// Translate the object based on the offset
//				Matrix.translateM(modelMatrix, 0, offset[0], offset[1], offset[2]);
//
//				// Combine camera pose and trackable pose to align the object with the camera view
//				float[] finalMatrix = new float[16];
//				Matrix.multiplyMM(finalMatrix, 0, cameraPose, 0, modelMatrix, 0);
//				Matrix.multiplyMM(finalMatrix, 0, trackablePoseMatrix, 0, finalMatrix, 0);
//
//				// Render the object with the updated transformation matrix
//				texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
//				texturedCubeRenderer.setTransform(finalMatrix);  // Apply the combined model-view matrix
//				texturedCubeRenderer.setTextureBitmap(textTexture);
//				texturedCubeRenderer.draw();
//			}
//
//			// Optionally, check for any tracking-specific logic like bounding box
//			GuideInfo gi = TrackerManager.getInstance().getGuideInformation();
//			float[] boundingBox = gi.getBoundingBox(); // [centerX, centerY, centerZ, sizeX, sizeY, sizeZ]
//			if (boundingBox != null) {
//				// Handle bounding box logic if necessary (e.g., placement inside the AR scene)
//			}
//		}
//	}
//
//	// Method to render objects without tracking, using a fixed position or predefined pose
//	private void renderObjectsWithoutTracking(float[] projectionMatrix) {
//		// Use default pose if tracking is not available
//		float[] defaultPoseMatrix = new float[16];
//		Matrix.setIdentityM(defaultPoseMatrix, 0);  // Identity matrix for fixed position
//
//		for (int i = 0; i < randomOffsets.size(); i++) {
//			float[] offset = randomOffsets.get(i);
//			Bitmap textTexture = textures.get(i);
//
//			// Set up object rendering with the default position
//			texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
//			texturedCubeRenderer.setTransform(defaultPoseMatrix);  // Use fixed position for non-tracking
//			texturedCubeRenderer.setTranslate(offset[0], offset[1], offset[2]); // Offsets from center
//			texturedCubeRenderer.setScale(0.1f, 0.1f, 0.1f);  // Adjust scale
//			texturedCubeRenderer.setTextureBitmap(textTexture);
//			texturedCubeRenderer.draw();
//		}
//	}
//
//
//
//
//
//
//
//
//
//
//
//	@Override
//	public void onSurfaceChanged(GL10 unused, int width, int height) {
//
//		surfaceWidth = width;
//		surfaceHeight = height;
//
//	//	texturedCubeRenderer.setScale(0.3f, 0.3f, 0.1f);
//		texturedCubeRenderer.setScale(0.3f, 0.3f, 0.1f);
//
//		MaxstAR.onSurfaceChanged(width, height);
//	}
//
//	@Override
//	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
//		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//
//	//	texturedCubeRenderer = new TexturedCubeRenderer(activity);
//
//		texturedCubeRenderer = new TexturedCircleRenderer(activity);
//		Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("MaxstAR_Cube.png", activity.getAssets());
//		texturedCubeRenderer.setTextureBitmap(bitmap);
//
//		backgroundRenderHelper = new BackgroundRenderHelper();
//		CameraDevice.getInstance().setClippingPlane(0.03f, 70.0f);
//
//		//
//		// generateRandomCubePositions();
//
//	//	generateOffsetsInsideModels();
//
//		generateStaticOffsets();
//
//	}
//
//	void setTranslate(float x, float y) {
//		posX += x;
//		posY += y;
//	}
//
//	void resetPosition() {
//		posX = 0;
//		posY = 0;
//	}
//
//
//	//
//
//
//	private void generateRandomCubePositions() {
//		cubePositions = new ArrayList<>();
//		float minMargin = 0.2f; // Minimum distance between cubes
//
//		int maxTries = 100; // To avoid infinite loops
//
//		while (cubePositions.size() < 8 && maxTries > 0) {
//			float[] pos = new float[3];
//			pos[0] = (float)(Math.random() - 0.5f) * 0.6f;  // x in [-0.3, 0.3]
//			pos[1] = (float)(Math.random() - 0.5f) * 0.6f;  // y in [-0.3, 0.3]
//			pos[2] = -0.05f;
//
//			boolean tooClose = false;
//
//			for (float[] existing : cubePositions) {
//				float dx = pos[0] - existing[0];
//				float dy = pos[1] - existing[1];
//				float distance = (float) Math.sqrt(dx * dx + dy * dy);
//
//				if (distance < minMargin) {
//					tooClose = true;
//					break;
//				}
//			}
//
//			if (!tooClose) {
//				cubePositions.add(pos);
//			} else {
//				maxTries--;
//			}
//		}
//
//		if (cubePositions.size() < 8) {
//			Log.w(TAG, "Could not place all cubes with minimum margin; placed " + cubePositions.size());
//		}
//	}
//
//	// todo working 99%
//
//	private void generateStaticOffsets() {
//		randomOffsets.clear();
//		textures.clear();
//
//		// "Temp", "Humidity", "Pressure", "CO2", "Light", "Sound", "PM2.5", "VOC"
//
//		randomOffsets.add(new float[]{-0.2f, -0.1f, 1.2f}); // Far left
//		randomOffsets.add(new float[]{-0.3f, -0.2f, 1.2f}); // Left
//		randomOffsets.add(new float[]{-0.5f, 0.0f, 1.2f}); // Center
//		randomOffsets.add(new float[]{ -0.3f, 0.0f, 1.2f}); // Right
//		randomOffsets.add(new float[]{ 0.4f, 0.0f, 1.2f}); // Far right
//
//		randomOffsets.add(new float[]{ 0.5f, 0.2f, 1.2f}); // Far right
//		randomOffsets.add(new float[]{ 0.6f, 0.3f, 1.2f}); // Far right
//		randomOffsets.add(new float[]{ 0.7f, 0.2f, 1.2f}); // Far right
//
//		// Ensure you only generate as many textures as offsets
//		int count = Math.min(randomOffsets.size(), sensorTextList.size());
//		for (int i = 0; i < count; i++) {
//			Bitmap textBitmap = createTextBitmap(sensorTextList.get(i));
//			textures.add(textBitmap);
//		}
//	}
//
//
////	private void generateStaticOffsets() {
////		randomOffsets.clear();
////		textures.clear();
////
////		randomOffsets.add(new float[]{-0.2f, -0.1f, -1.0f}); // Front-left
////		randomOffsets.add(new float[]{-0.4f, -0.2f, -1.0f}); // Left
////		randomOffsets.add(new float[]{0.0f, 0.0f, -1.0f});   // Center
////		randomOffsets.add(new float[]{0.3f, 0.1f, -1.0f});   // Right
////		randomOffsets.add(new float[]{0.4f, 0.3f, -1.0f});   // Far right
////
////		// Create textures for each text label
////		int count = Math.min(randomOffsets.size(), sensorTextList.size());
////		for (int i = 0; i < count; i++) {
////			Bitmap textBitmap = createTextBitmap(sensorTextList.get(i));
////			textures.add(textBitmap);
////		}
////	}
//
//
//
//	private Bitmap createTextBitmap(String text) {
//		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
//		paint.setTextSize(64);
//		paint.setColor(Color.BLACK);
//		paint.setTextAlign(Paint.Align.CENTER);
//
//		int width = 256;
//		int height = 256;
//		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//		Canvas canvas = new Canvas(bitmap);
//		canvas.drawColor(Color.WHITE);
//
//		// Center text
//		Rect bounds = new Rect();
//		paint.getTextBounds(text, 0, text.length(), bounds);
//		int x = width / 2;
//		int y = height / 2 - bounds.centerY();
//
//		canvas.drawText(text, x, y, paint);
//		return bitmap;
//	}
//
//
//
//}


//


// todo old

//
//class InstantTrackerRenderer implements Renderer {
//
//	public static final String TAG = InstantTrackerRenderer.class.getSimpleName();
//
//	private int surfaceWidth;
//	private int surfaceHeight;
//
//	private TexturedCubeRenderer texturedCubeRenderer;
//	private float posX;
//	private float posY;
//	private Activity activity;
//
//	private BackgroundRenderHelper backgroundRenderHelper;
//
//	private ObjModelLoader objModelLoader;
//	private int positionHandle;
//	private ObjRenderer objRenderer;
//
/// *
//	private static final String vertexShaderCode =
//			"attribute vec4 a_position;\n" +
//					"attribute vec2 a_texCoord;\n" +
//					"varying vec2 v_texCoord;\n" +
//					"uniform mat4 u_mvpMatrix;\n" +
//					"void main()							\n" +
//					"{										\n" +
//					"	gl_Position = u_mvpMatrix * a_position;\n" +
//					"	v_texCoord = a_texCoord; 			\n" +
//					"}										\n";
//
//	private static final String fragmentShaderCode =
//			"precision mediump float;\n" +
//					"varying vec2 v_texCoord;\n" +
//					"uniform sampler2D u_texture;\n" +
//
//					"void main(void)\n" +
//					"{\n" +
//					"	gl_FragColor = texture2D(u_texture, v_texCoord);\n" +
//					"}\n";*/
//
//
//	// working
//	private static final String vertexShaderCode =
//			"attribute vec4 vPosition;" +
//					"void main() {" +
//					"  gl_Position = vPosition;" +
//					"}";
//
//	private static final String fragmentShaderCode =
//			"precision mediump float;" +
//					"void main() {" +
//					"  gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);" +  // Red color
//					"}";
//
///*
//	private static final String vertexShaderCode =
//			"attribute vec4 vPosition;" +
//					"attribute vec3 vNormal;" +
//					"attribute vec2 vTexCoord;" +
//
//					"varying vec3 fragNormal;" +
//					"varying vec2 fragTexCoord;" +
//
//					"uniform mat4 uMVPMatrix;" +
//
//					"void main() {" +
//					"    fragNormal = vNormal;" +
//					"    fragTexCoord = vTexCoord;" +
//					"    gl_Position = uMVPMatrix * vPosition;" +
//					"}";
//
//	private static final String fragmentShaderCode =
//			"precision mediump float;" +
//
//					"varying vec3 fragNormal;" +
//					"varying vec2 fragTexCoord;" +
//
//					"uniform sampler2D textureSampler;" +
//					"uniform vec3 lightDirection;" +
//
//					"void main() {" +
//					"    vec3 normal = normalize(fragNormal);" +
//					"    float lighting = max(dot(normal, lightDirection), 0.0);" +
//
//					"    vec4 texColor = texture2D(textureSampler, fragTexCoord);" +
//					"    gl_FragColor = vec4(texColor.rgb * lighting, texColor.a);" +
//					"}";*/
//
//
//
//
//
//
///*
//	private static final String vertexShaderCode =
//			"attribute vec4 vPosition;" +
//					"attribute vec3 vNormal;" +
//					"attribute vec2 vTexCoord;" +
//
//					"varying vec3 fragNormal;" +
//					"varying vec2 fragTexCoord;" +
//
//					"uniform mat4 uMVPMatrix;" +
//
//					"void main() {" +
//					"    fragNormal = vNormal;" +
//					"    fragTexCoord = vTexCoord;" +
//					"    gl_Position = uMVPMatrix * vPosition;" +
//					"}";
//
//	private static final String fragmentShaderCode =
//			"precision mediump float;" +
//
//					"varying vec3 fragNormal;" +
//					"varying vec2 fragTexCoord;" +
//
//					"uniform sampler2D textureSampler;" +
//					"uniform vec3 lightDirection;" +
//
//					"void main() {" +
//					"    vec3 normal = normalize(fragNormal);" +
//					"    float lighting = max(dot(normal, lightDirection), 0.0);" +
//
//					"    vec4 texColor = texture2D(textureSampler, fragTexCoord);" +
//					"    gl_FragColor = vec4(texColor.rgb * lighting, texColor.a);" +
//					"}";*/
//
//
//	InstantTrackerRenderer(Activity activity) {
//		this.activity = activity;
//	}
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
//		float[] projectionMatrix = CameraDevice.getInstance().getProjectionMatrix();
//		float[] backgroundPlaneInfo = CameraDevice.getInstance().getBackgroundPlaneInfo();
//
//		backgroundRenderHelper.drawBackground(image, projectionMatrix, backgroundPlaneInfo);
//
//		if (trackingResult.getCount() == 0) {
//			return;
//		}
//
//		Trackable trackable = trackingResult.getTrackable(0);
//
//		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
//
//		texturedCubeRenderer.setTransform(trackable.getPoseMatrix());
//		texturedCubeRenderer.setTranslate(posX, posY, -0.05f);
//		texturedCubeRenderer.setProjectionMatrix(projectionMatrix);
//		texturedCubeRenderer.draw();
//
//		positionHandle = GLES20.glGetAttribLocation(ShaderUtils.shaderProgram, "vPosition");
//		int normalHandle = GLES20.glGetAttribLocation(ShaderUtils.shaderProgram, "vNormal");
//		int texCoordHandle = GLES20.glGetAttribLocation(ShaderUtils.shaderProgram, "vTexCoord");
//
//		// Draw the loaded model
//		float[] vpMatrix = new float[16];
//
//		objModelLoader.draw(positionHandle,normalHandle,texCoordHandle);
//
//	//	objRenderer.draw(vpMatrix);
//
//	//	objModelLoader.draw();
//
//
//	}
//
//	@Override
//	public void onSurfaceChanged(GL10 unused, int width, int height) {
//
//		surfaceWidth = width;
//		surfaceHeight = height;
//
//		texturedCubeRenderer.setScale(0.3f, 0.3f, 0.1f);
//
//		MaxstAR.onSurfaceChanged(width, height);
//	}
//
//	@Override
//	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
//		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
//
//		texturedCubeRenderer = new TexturedCubeRenderer(activity);
//
//		/*Bitmap bitmap = MaxstARUtil.getBitmapFromAsset("MaxstAR_Cube.png", activity.getAssets());
//		texturedCubeRenderer.setTextureBitmap(bitmap);*/
//
//		objModelLoader = new ObjModelLoader(activity);
//
//	//	objModelLoader.loadModel("pill.obj","12221_Cat_v1_l3.mtl");
//		objModelLoader.loadModel("inhaler.obj","12221_Cat_v1_l3.mtl");
//
//
//		objRenderer = new ObjRenderer(activity);
//		//objRenderer.loadObjModel("fette.obj");
//
//		backgroundRenderHelper = new BackgroundRenderHelper();
//		CameraDevice.getInstance().setClippingPlane(0.03f, 70.0f);
//
//		// add new
//		ShaderUtils.shaderProgram = ShaderUtils.createShaderProgram(vertexShaderCode, fragmentShaderCode);
//	}
//
//	void setTranslate(float x, float y) {
//		posX += x;
//		posY += y;
//	}
//
//	void resetPosition() {
//		posX = 0;
//		posY = 0;
//	}
//}
