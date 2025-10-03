/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */

package com.maxst.ar.sample.instantTracker;

import android.app.Activity;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bumptech.glide.load.model.Model;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.ResultCode;
import com.maxst.ar.SensorDevice;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.sample.R;
import com.maxst.ar.sample.util.SampleUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class InstantTrackerActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener {

    private InstantTrackerRenderer instantTargetRenderer;
    private int preferCameraResolution = 0;
    private Button startTrackingButton;
    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_instant_tracker);

        startTrackingButton = (Button) findViewById(R.id.start_tracking);
        startTrackingButton.setOnClickListener(this);

        instantTargetRenderer = new InstantTrackerRenderer(this);
        glSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(instantTargetRenderer);
        glSurfaceView.setOnTouchListener(this);

        preferCameraResolution = getSharedPreferences(SampleUtil.PREF_NAME, Activity.MODE_PRIVATE).getInt(SampleUtil.PREF_KEY_CAM_RESOLUTION, 0);

        MaxstAR.init(this.getApplicationContext(), getResources().getString(R.string.app_key));
        MaxstAR.setScreenOrientation(getResources().getConfiguration().orientation);
    }

    @Override
    protected void onResume() {
        super.onResume();

        glSurfaceView.onResume();
        SensorDevice.getInstance().start();
        TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_INSTANT);

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
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();

        TrackerManager.getInstance().quitFindingSurface();
        TrackerManager.getInstance().stopTracker();
        CameraDevice.getInstance().stop();
        SensorDevice.getInstance().stop();

        MaxstAR.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TrackerManager.getInstance().destroyTracker();
        MaxstAR.deinit();
    }

    private static final float TOUCH_TOLERANCE = 5;
    private float touchStartX;
    private float touchStartY;
    private float translationX;
    private float translationY;

    @Override
    public boolean onTouch(View v, final MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                touchStartX = x;
                touchStartY = y;

                final float[] screen = new float[2];
                screen[0] = x;
                screen[1] = y;

                final float[] world = new float[3];

                TrackerManager.getInstance().getWorldPositionFromScreenCoordinate(screen, world);
                translationX = world[0];
                translationY = world[1];

                // todo model click

                // Convert screen coordinates to normalized OpenGL (-1 to 1)


//                float normalizedX = (x / glSurfaceView.getWidth()) * 2 - 1;
//                float normalizedY = -((y / glSurfaceView.getHeight()) * 2 - 1); // y is inverted in OpenGL
//
//                instantTargetRenderer.handleTouch(normalizedX, normalizedY);

                // 2

                float[] screens = new float[]{x, y};
                float[] worlds = new float[3];

                TrackerManager.getInstance().getWorldPositionFromScreenCoordinate(screens, worlds);
                instantTargetRenderer.handleTouch(worlds[0], worlds[1]);


                break;
            }

            case MotionEvent.ACTION_MOVE: {
                float dx = Math.abs(x - touchStartX);
                float dy = Math.abs(y - touchStartY);
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    touchStartX = x;
                    touchStartY = y;

                    final float[] screen = new float[2];
                    screen[0] = x;
                    screen[1] = y;

                    final float[] world = new float[3];

                    TrackerManager.getInstance().getWorldPositionFromScreenCoordinate(screen, world);
                    float posX = world[0];
                    float posY = world[1];

                    instantTargetRenderer.setTranslate(posX - translationX, posY - translationY);
                    translationX = posX;
                    translationY = posY;
                }
                break;
            }

//			case MotionEvent.ACTION_UP:
//				break;

//			case MotionEvent.ACTION_UP: {
//				float[] screen = new float[]{x, y};
//				float[] world = new float[3];
//
//				TrackerManager.getInstance().getWorldPositionFromScreenCoordinate(screen, world);
//				instantTargetRenderer.addModel(world[0], world[1]);
//
//				break;
//			}

        }

        return true;
    }


    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.start_tracking) {
            String text = startTrackingButton.getText().toString();

            if (text.equals(getResources().getString(R.string.start_tracking))) {
                TrackerManager.getInstance().findSurface();
                instantTargetRenderer.resetPosition();

                // todo working

//
//                instantTargetRenderer.addModel(-0.1875f, 0.18f); // Top-left
//                instantTargetRenderer.addModel(0.1875f, 0.18f); // Top-right
//
//// B
//                instantTargetRenderer.addModel(-0.1875f, -0.18f); // Bottom-left
//                instantTargetRenderer.addModel(0.1875f, -0.18f); // Bottom-right



                // 2

//                instantTargetRenderer.addModel(-0.25f,  0.25f); // Top-left
//                instantTargetRenderer.addModel( 0.25f,  0.25f); // Top-right
//                instantTargetRenderer.addModel( 0.25f, -0.25f); // Bottom-right
//                instantTargetRenderer.addModel(-0.25f, -0.25f); // Bottom-left
//                instantTargetRenderer.addModel( 0.0f,   0.0f);  // Center

//                private List<String> sensorTextList = Arrays.asList(
//                        "Temp", "Humidity", "Pressure", "CO2", "Light", "Sound", "PM2.5", "VOC"
//                );




           /*     instantTargetRenderer.addModel(-0.1f,  0.1f,-0.1f);

                instantTargetRenderer.addModel( 0.1f,  0.5f,1.09f);
                instantTargetRenderer.addModel( 0.3f, -0.1f,2.5f);
                instantTargetRenderer.addModel(-0.1f, -0.1f,3.5f);
                instantTargetRenderer.addModel( 0.0f,  0.0f,-0.03f);
                instantTargetRenderer.addModel( 0.1f,  0.0f,2.9f);
                instantTargetRenderer.addModel( 0.1f,  0.2f,-0.09f);*/



            /*    instantTargetRenderer.addModel(0.1f,  0.1f,-0.02f);
                instantTargetRenderer.addModel( 0.1f,  0.3f,-0.03f);
                instantTargetRenderer.addModel( 0.3f, -0.1f,-0.02f);
                instantTargetRenderer.addModel( 0.0f,  0.0f,-0.03f);*/


                instantTargetRenderer.addModel(0.1f,  0.2f,-0.5f);
                instantTargetRenderer.addModel( 0.1f,  0.2f,-0.04f);
                instantTargetRenderer.addModel( 0.1f, 0.2f,-0.7f);
                instantTargetRenderer.addModel( 0.1f,  0.2f,-0.9f);



                startTrackingButton.setText(getResources().getString(R.string.stop_tracking));
            } else {
                TrackerManager.getInstance().quitFindingSurface();
                startTrackingButton.setText(getResources().getString(R.string.start_tracking));
            }
        }
    }


//    @Override
//    public void onClick(View v) {
//        if (v.getId() == R.id.start_tracking) {
//            String text = startTrackingButton.getText().toString();
//
//            if (text.equals(getResources().getString(R.string.start_tracking))) {
//                TrackerManager.getInstance().findSurface();
//                instantTargetRenderer.resetPosition();
//
//                // Define bounding area for random positions
//                float minX = -0.25f;
//                float maxX = 0.25f;
//                float minY = -0.25f;
//                float maxY = 0.25f;
//
//                Random random = new Random();
//
//                // Place 4 models at random positions
//                for (int i = 0; i < 4; i++) {
//                    float randomX = minX + random.nextFloat() * (maxX - minX);
//                    float randomY = minY + random.nextFloat() * (maxY - minY);
//                    instantTargetRenderer.addModel(randomX, randomY);
//                }
//
//                startTrackingButton.setText(getResources().getString(R.string.stop_tracking));
//            } else {
//                TrackerManager.getInstance().quitFindingSurface();
//                startTrackingButton.setText(getResources().getString(R.string.start_tracking));
//            }
//        }
//    }


    // todo working 99

//    @Override
//    public void onClick(View v) {
//        if (v.getId() == R.id.start_tracking) {
//            String text = startTrackingButton.getText().toString();
//
//            if (text.equals(getResources().getString(R.string.start_tracking))) {
//                TrackerManager.getInstance().findSurface();
//                instantTargetRenderer.resetPosition();
//                instantTargetRenderer.resetPositionModel();
//
//                // Bounding box limits (adjust as needed)
//                float minX = -0.3f;
//                float maxX = 0.3f;
//                float minY = -0.3f;
//                float maxY = 0.3f;
//
//                float minDistance = 0.35f; // Minimum distance (margin) between models
//
//                Random random = new Random();
//                List<float[]> placedPositions = new ArrayList<>();
//
//                int modelCount = 4;
//                int attemptsLimit = 100;
//
//                for (int i = 0; i < modelCount; i++) {
//                    boolean placed = false;
//                    int attempts = 0;
//
//                    while (!placed && attempts < attemptsLimit) {
//                        float x = minX + random.nextFloat() * (maxX - minX);
//                        float y = minY + random.nextFloat() * (maxY - minY);
//
//                        boolean tooClose = false;
//                        for (float[] pos : placedPositions) {
//                            float dx = x - pos[0];
//                            float dy = y - pos[1];
//                            float distance = (float) Math.sqrt(dx * dx + dy * dy);
//                            if (distance < minDistance) {
//                                tooClose = true;
//                                break;
//                            }
//                        }
//
//                        if (!tooClose) {
//                            instantTargetRenderer.addModel(x, y);
//                            placedPositions.add(new float[]{x, y});
//                            placed = true;
//                        }
//
//                        attempts++;
//                    }
//                }
//
//                startTrackingButton.setText(getResources().getString(R.string.stop_tracking));
//            } else {
//                TrackerManager.getInstance().quitFindingSurface();
//                startTrackingButton.setText(getResources().getString(R.string.start_tracking));
//            }
//        }
//    }





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
}


// todo old

//public class InstantTrackerActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener {
//
//	private InstantTrackerRenderer instantTargetRenderer;
//	private int preferCameraResolution = 0;
//	private Button startTrackingButton;
//	private GLSurfaceView glSurfaceView;
//
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//
//		setContentView(R.layout.activity_instant_tracker);
//
//		startTrackingButton = (Button) findViewById(R.id.start_tracking);
//		startTrackingButton.setOnClickListener(this);
//
//		instantTargetRenderer = new InstantTrackerRenderer(this);
//		glSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
//		glSurfaceView.setEGLContextClientVersion(2);
//		glSurfaceView.setRenderer(instantTargetRenderer);
//		glSurfaceView.setOnTouchListener(this);
//
//		preferCameraResolution = getSharedPreferences(SampleUtil.PREF_NAME, Activity.MODE_PRIVATE).getInt(SampleUtil.PREF_KEY_CAM_RESOLUTION, 0);
//
//		MaxstAR.init(this.getApplicationContext(), getResources().getString(R.string.app_key));
//		MaxstAR.setScreenOrientation(getResources().getConfiguration().orientation);
//	}
//
//	@Override
//	protected void onResume() {
//		super.onResume();
//
//		glSurfaceView.onResume();
//		SensorDevice.getInstance().start();
//		TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_INSTANT);
//
//
////		TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_OBJECT);
////		TrackerManager.getInstance().addTrackerData("ObjectTarget/bus.3dmap", true);
////		TrackerManager.getInstance().addTrackerData("ObjectTarget/fette.3dmap", true);
////		TrackerManager.getInstance().loadTrackerData();
//
//		ResultCode resultCode = ResultCode.Success;
//
//		switch (preferCameraResolution) {
//			case 0:
//				resultCode = CameraDevice.getInstance().start(0, 640, 480);
//				break;
//
//			case 1:
//				resultCode = CameraDevice.getInstance().start(0, 1280, 720);
//				break;
//
//			case 2:
//				resultCode = CameraDevice.getInstance().start(0, 1920, 1080);
//				break;
//		}
//
//		if (resultCode != ResultCode.Success) {
//			Toast.makeText(this, R.string.camera_open_fail, Toast.LENGTH_SHORT).show();
//			finish();
//		}
//
//		MaxstAR.onResume();
//	}
//
//	@Override
//	protected void onPause() {
//		super.onPause();
//		glSurfaceView.onPause();
//
//		TrackerManager.getInstance().quitFindingSurface();
//		TrackerManager.getInstance().stopTracker();
//		CameraDevice.getInstance().stop();
//		SensorDevice.getInstance().stop();
//
//		MaxstAR.onPause();
//	}
//
//	@Override
//	protected void onDestroy() {
//		super.onDestroy();
//		TrackerManager.getInstance().destroyTracker();
//		MaxstAR.deinit();
//	}
//
//	private static final float TOUCH_TOLERANCE = 5;
//	private float touchStartX;
//	private float touchStartY;
//	private float translationX;
//	private float translationY;
//
//	@Override
//	public boolean onTouch(View v, final MotionEvent event) {
//		float x = event.getX();
//		float y = event.getY();
//
//		switch (event.getAction()) {
//			case MotionEvent.ACTION_DOWN: {
//				touchStartX = x;
//				touchStartY = y;
//
//				final float[] screen = new float[2];
//				screen[0] = x;
//				screen[1] = y;
//
//				final float[] world = new float[3];
//
//				TrackerManager.getInstance().getWorldPositionFromScreenCoordinate(screen, world);
//				translationX = world[0];
//				translationY = world[1];
//				break;
//			}
//
//			case MotionEvent.ACTION_MOVE: {
//				float dx = Math.abs(x - touchStartX);
//				float dy = Math.abs(y - touchStartY);
//				if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
//					touchStartX = x;
//					touchStartY = y;
//
//					final float[] screen = new float[2];
//					screen[0] = x;
//					screen[1] = y;
//
//					final float[] world = new float[3];
//
//					TrackerManager.getInstance().getWorldPositionFromScreenCoordinate(screen, world);
//					float posX = world[0];
//					float posY = world[1];
//
//					instantTargetRenderer.setTranslate(posX - translationX, posY - translationY);
//					translationX = posX;
//					translationY = posY;
//				}
//				break;
//			}
//
//			case MotionEvent.ACTION_UP:
//				break;
//		}
//
//		return true;
//	}
//
//	@Override
//	public void onClick(View v) {
//
//		if (v.getId() == R.id.start_tracking) {
//			String text = startTrackingButton.getText().toString();
//
//			if (text.equals(getResources().getString(R.string.start_tracking))) {
//				TrackerManager.getInstance().findSurface();
//				instantTargetRenderer.resetPosition();
//				instantTargetRenderer.setTrackingActive(true);
//				startTrackingButton.setText(getResources().getString(R.string.stop_tracking));
//			} else {
//				TrackerManager.getInstance().quitFindingSurface();
//				instantTargetRenderer.setTrackingActive(false);
//				startTrackingButton.setText(getResources().getString(R.string.start_tracking));
//			}
//		}
//	}
//
//	@Override
//	public void onConfigurationChanged(Configuration newConfig) {
//		super.onConfigurationChanged(newConfig);
//
//		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//			Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
//		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//			Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
//		}
//
//		MaxstAR.setScreenOrientation(newConfig.orientation);
//	}
//}
