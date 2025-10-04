/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */

package com.maxst.ar.sample.qrcodeFusionTracker;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.MimeTypes;

import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.sample.ApiResponse;
import com.maxst.ar.sample.ApiService;
import com.maxst.ar.sample.MovieItem;
import com.maxst.ar.sample.R;
import com.maxst.ar.sample.RetrofitClient;
import com.maxst.ar.sample.util.TrackerResultListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


//
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QrCodeFusionTrackerActivity extends AppCompatActivity implements View.OnClickListener {

	private QrCodeFusionTrackerRenderer qrCodeTargetRenderer;
	private GLSurfaceView glSurfaceView;
	private TextView recognizedQrCodeView,tvLeftStep1;
	private  TextView tvRightStep1,tvRightStep2,tvRightStep3,tvRightStep4,tvRightStep5;
	private View guideView;
	private PlayerView playerView;
	private SimpleExoPlayer exoPlayer;
	private boolean isVideoStarted = false;
	private Set<String> detectedQRCodes = new HashSet<>();
	private final List<TextView> dynamicSteps = new ArrayList<>();

	boolean isTextViewLeftClicked = false;
	boolean isTextViewRightClicked = false;
	boolean istvStep1 = false;
	boolean istvStep2 = false;
	boolean istvStep3 = false;
	boolean istvStep4 = false;
	private boolean isQrCodeScanned = false;
	private ImageView imageView;
	private  TextView tvOpenFileUrl;
	private String jsonData;


	// 3d object


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_qrcode_fusion_tracker);


		RelativeLayout rootLayout = findViewById(R.id.rootLayout);

        // todo getdata


		jsonData = getIntent().getStringExtra("jsonData");

		if (jsonData != null) {
			try {
				JSONObject jsonObject = new JSONObject(jsonData);
				JSONArray textViews = jsonObject.getJSONArray("textViews");
				JSONArray videos = jsonObject.getJSONArray("videos");
				Log.d("QrCodeFusion", "Received " + textViews.length() + " items");

				for (int i = 0; i < textViews.length(); i++) {
					String tv = textViews.getString(i);
					String video = videos.getString(i);

					Log.d("QrCodeFusion", "TextView: " + tv + " | Video: " + video);
				}

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}


        qrCodeTargetRenderer = new QrCodeFusionTrackerRenderer(this);
		glSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
		glSurfaceView.setEGLContextClientVersion(2);
		glSurfaceView.setRenderer(qrCodeTargetRenderer);

		recognizedQrCodeView = (TextView)findViewById(R.id.recognized_QrCode);
		tvLeftStep1 = (TextView)findViewById(R.id.tv_leftStep1);
		imageView = findViewById(R.id.imageView);
		tvOpenFileUrl = findViewById(R.id.openFileUrl);

		// right 4 steps

		tvRightStep2 = (TextView)findViewById(R.id.tv_rightStep2);
		tvRightStep1 = (TextView)findViewById(R.id.tv_rightStep1);
		tvRightStep3 = (TextView)findViewById(R.id.tv_rightStep3);
		tvRightStep4 = (TextView)findViewById(R.id.tv_rightStep4);
		tvRightStep5 = (TextView)findViewById(R.id.tv_rightStep5);

		guideView = (View)findViewById(R.id.guideView);
		qrCodeTargetRenderer.listener = resultListener;

	//	playerView = findViewById(R.id.playerView);

		playerView = new PlayerView(this);
		playerView.setId(View.generateViewId());
		playerView.setVisibility(View.GONE);



		exoPlayer = new SimpleExoPlayer.Builder(this).build();
		playerView.setPlayer(exoPlayer);

		MaxstAR.init(this.getApplicationContext(), getResources().getString(R.string.app_key));
		MaxstAR.setScreenOrientation(getResources().getConfiguration().orientation);

		// init
		tvRightStep1.setTag("Step 1");
		tvRightStep2.setTag("Step 2");
		tvRightStep3.setTag("Step 3");
		tvRightStep4.setTag("Step 4");


	/*	ModelRenderable.builder()
			//	.setSource(this, Uri.parse("https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))  // Use your 3D model file
				.setSource(this, R.raw.fetteglb)
				.build()
				.thenAccept(renderable -> {
					modelRenderable = renderable;
					qrCodeTargetRenderer = new QrCodeFusionTrackerRenderer(this, arFragment, modelRenderable);
				})
				.exceptionally(
						throwable -> {
							Log.e("AR", "Model loading failed", throwable);
							return null;
						});*/


	/*	ModelRenderable.builder()
				.setSource(this, RenderableSource.builder()
						.setSource(this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.fetteglb),
								RenderableSource.SourceType.GLB)
						.setScale(0.5f)  // Adjust as needed
						.build())
				.setRegistryId("fetteglb")
				.build()
				.thenAccept(renderable -> {
					modelRenderable = renderable;
					qrCodeTargetRenderer = new QrCodeFusionTrackerRenderer(this, arFragment, modelRenderable);
				})
				.exceptionally(throwable -> {
					Log.e("AR", "Model loading failed", throwable);
					return null;
				});*/




	}

	@Override
	protected void onResume() {
		super.onResume();

		glSurfaceView.onResume();

		if(TrackerManager.getInstance().isFusionSupported()) {
			CameraDevice.getInstance().stop();
			CameraDevice.getInstance().setCameraApi(CameraDevice.CameraApi.Fusion);
			CameraDevice.getInstance().start(0, 1280, 720);
			TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_QR_FUSION);
			TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"Our mission is to expand human experience using virtual and augmented reality technologies.\", \"scale\":0.1}", false);
			TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"A metaverse provider that connects realistic virtual worlds with augmented reality.\", \"scale\":0.1}", false);


			TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"Step 1: Check the Functionality of Security Interlocks\", \"scale\":0.1}", false);
			TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"Step 2: Check the Electrical connections in the panel\", \"scale\":0.1}", false);
			TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"Step 3: Check the Suction manifold\", \"scale\":0.1}", false);
			TrackerManager.getInstance().addTrackerData("{\"qr_fusion\":\"set_scale\",\"content\":\"Step 4: Check the Lubrication system\", \"scale\":0.1}", false);


		} else {
			String message = TrackerManager.requestARCoreApk(this);
			if(message != null) {
				Log.i("MaxstAR", message);
			}
		}
		String message = TrackerManager.requestARCoreApk(this);
		Log.i("MaxstAR", "ARCore Installation Message: " + message);


		MaxstAR.onResume();

		if (exoPlayer != null && !exoPlayer.isPlaying()) {
			exoPlayer.play();
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		if (exoPlayer != null && exoPlayer.isPlaying()) {
			exoPlayer.pause();
		}

		glSurfaceView.queueEvent(new Runnable() {
			@Override
			public void run() {
			}
		});

		glSurfaceView.onPause();

		CameraDevice.getInstance().stop();
		TrackerManager.getInstance().stopTracker();
		MaxstAR.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (exoPlayer != null && exoPlayer.isPlaying()) {
			exoPlayer.pause();
		}

		TrackerManager.getInstance().destroyTracker();
		MaxstAR.deinit();
	}

	@Override
	public void onClick(View view) {

	}


	public void updateOverlayViewRight1(Rect qrCodeBoundingBox) {
		if (isQrCodeScanned) {
			return; // Prevent continuous updates
		}

		if (qrCodeBoundingBox != null && jsonData != null) {
			int qrX = qrCodeBoundingBox.left;
			int qrY = qrCodeBoundingBox.top;
			int qrWidth = qrCodeBoundingBox.width();
			int qrHeight = qrCodeBoundingBox.height();

			isQrCodeScanned = true;

			// Step position base (relative to QR code)
			int stepMarginLeft = qrX + qrWidth + 150; // Right of QR Code
			int stepMarginTop = qrCodeBoundingBox.top - 80; // Start vertically aligned with QR



				if (jsonData != null) {
					try {
						JSONObject jsonObject = new JSONObject(jsonData);
						JSONArray textViews = jsonObject.getJSONArray("textViews");
						JSONArray videos = jsonObject.getJSONArray("videos");

						RelativeLayout rootLayout = findViewById(R.id.rootLayout);

						// new

						for (TextView tv : dynamicSteps) {
							rootLayout.removeView(tv);

						}
						dynamicSteps.clear();

						for (int i = 0; i < textViews.length(); i++) {
							String text = textViews.getString(i);
							String videoPath = videos.optString(i); // same index for video

							TextView tv = new TextView(this);
							tv.setText(text);
							tv.setTextColor(Color.WHITE);
							tv.setTypeface(Typeface.DEFAULT_BOLD);
							tv.setTextSize(16);
							tv.setVisibility(View.VISIBLE);

							// Position below each other with spacing
							RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
									ViewGroup.LayoutParams.WRAP_CONTENT,
									ViewGroup.LayoutParams.WRAP_CONTENT
							);
							params.leftMargin = stepMarginLeft;
							params.topMargin = stepMarginTop + (i * 80);
							tv.setLayoutParams(params);

							int finalI = i;
							tv.setOnClickListener(v -> {

								// toggleStep(tv, text + (finalI + 1), videoPath);
								toggleStep(tv, text , videoPath);

							});

							rootLayout.addView(tv);
							dynamicSteps.add(tv);

						}

						RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(750, 350);
						videoParams.leftMargin = 90;
						videoParams.topMargin = qrY + qrHeight + 250;
						playerView.setLayoutParams(videoParams);

						rootLayout.addView(playerView);


					} catch (JSONException e) {
						e.printStackTrace();
					}
				}



		}
	}




	public void updateOverlayViewRight(Rect qrCodeBoundingBox) {
		if ( isQrCodeScanned) {
			return; // Prevent continuous updates
		}

		if (qrCodeBoundingBox != null) {
			int qrX = qrCodeBoundingBox.left;
			int qrY = qrCodeBoundingBox.top;
			int qrWidth = qrCodeBoundingBox.width();
			int qrHeight = qrCodeBoundingBox.height();

			isQrCodeScanned = true;

		//	TODO set visibility gone

//			tvOpenFileUrl.setText("Open File URL");
//			tvOpenFileUrl.setVisibility(View.VISIBLE);



            // todo new add 30-9-25

            tvRightStep1.setTag("Step 1");
            tvRightStep2.setTag("Step 2");
            tvRightStep3.setTag("Step 3");
            tvRightStep4.setTag("Step 4");


            tvRightStep1.setText("Step 1");
            tvRightStep2.setText("Step 2");
            tvRightStep3.setText("Step 3");
            tvRightStep4.setText("Step 4");
            tvRightStep5.setText("Step 5");

			// TODO set point random
			// working 90 %


        // TODO comment 30-9-25


/*
			Random random = new Random();

			// Define a range for randomness (adjust as needed)
			int rangeX = 300;  // Max deviation in X direction
			int rangeY = 300;  // Max deviation in Y direction

			// Generate random positions for each text view
			int randomX1 = qrX + random.nextInt(rangeX) - rangeX / 2;
			int randomY1 = qrY + random.nextInt(rangeY) - rangeY / 2;

			int randomX2 = qrX + qrWidth + random.nextInt(rangeX) - rangeX / 2;
			int randomY2 = qrY + random.nextInt(rangeY) - rangeY / 2;

			int randomX3 = qrX + random.nextInt(rangeX) - rangeX / 2;
			int randomY3 = qrY + qrHeight + random.nextInt(rangeY) - rangeY / 2;

			int randomX4 = qrX + qrWidth + random.nextInt(rangeX) - rangeX / 2;
			int randomY4 = qrY + qrHeight + random.nextInt(rangeY) - rangeY / 2;

			// Top Left (Randomized)
			RelativeLayout.LayoutParams topLeftParams = new RelativeLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			topLeftParams.leftMargin = randomX1;
			topLeftParams.topMargin = randomY1;
			tvRightStep1.setLayoutParams(topLeftParams);
			tvRightStep1.setVisibility(View.VISIBLE);

			// Top Right (Randomized)
			RelativeLayout.LayoutParams topRightParams = new RelativeLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			topRightParams.leftMargin = randomX2;
			topRightParams.topMargin = randomY2;
			tvRightStep2.setLayoutParams(topRightParams);
			tvRightStep2.setVisibility(View.VISIBLE);

			// Bottom Left (Randomized)
			RelativeLayout.LayoutParams bottomLeftParams = new RelativeLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			bottomLeftParams.leftMargin = randomX3;
			bottomLeftParams.topMargin = randomY3;
			tvRightStep3.setLayoutParams(bottomLeftParams);
			tvRightStep3.setVisibility(View.VISIBLE);

			// Bottom Right (Randomized)
			RelativeLayout.LayoutParams bottomRightParams = new RelativeLayout.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			bottomRightParams.leftMargin = randomX4;
			bottomRightParams.topMargin = randomY4;
			tvRightStep4.setLayoutParams(bottomRightParams);
			tvRightStep4.setVisibility(View.VISIBLE);*/



			//TODO set textview 4 corner

//			int topLeftX = qrX;
//			int topLeftY = qrY;
//
//			int topRightX = qrX + qrWidth;
//			int topRightY = qrY;
//
//			int bottomLeftX = qrX;
//			int bottomLeftY = qrY + qrHeight;
//
//			int bottomRightX = qrX + qrWidth;
//			int bottomRightY = qrY + qrHeight;
//
//			// Top Left
//			RelativeLayout.LayoutParams topLeftParams = new RelativeLayout.LayoutParams(
//					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//			topLeftParams.leftMargin = topLeftX - 190;  // Adjust for better positioning
//			topLeftParams.topMargin = topLeftY - 100;
//			tvRightStep1.setLayoutParams(topLeftParams);
//			tvRightStep1.setVisibility(View.VISIBLE);
//
//
//			// Top Right
//			RelativeLayout.LayoutParams topRightParams = new RelativeLayout.LayoutParams(
//					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//			topRightParams.leftMargin = topRightX + 100;
//			topRightParams.topMargin = topRightY - 100;
//			tvRightStep2.setLayoutParams(topRightParams);
//			tvRightStep2.setVisibility(View.VISIBLE);
//
//
//			// Bottom Left
//			RelativeLayout.LayoutParams bottomLeftParams = new RelativeLayout.LayoutParams(
//					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//			bottomLeftParams.leftMargin = bottomLeftX - 190;
//			bottomLeftParams.topMargin = bottomLeftY + 100;
//			tvRightStep3.setLayoutParams(bottomLeftParams);
//			tvRightStep3.setVisibility(View.VISIBLE);
//
//			// Bottom Right
//			RelativeLayout.LayoutParams bottomRightParams = new RelativeLayout.LayoutParams(
//					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//			bottomRightParams.leftMargin = bottomRightX + 100;
//			bottomRightParams.topMargin = bottomRightY + 100;
//			tvRightStep4.setLayoutParams(bottomRightParams);
//			tvRightStep4.setVisibility(View.VISIBLE);


			//TODO set textview right side of qrcode


           // TODO uncomment 30-9-25


			RelativeLayout.LayoutParams qrParams = new RelativeLayout.LayoutParams(qrWidth, qrHeight);
			qrParams.leftMargin = qrX;
			qrParams.topMargin = qrY;

			//qrCodeView.setLayoutParams(qrParams);
			//qrCodeView.setVisibility(View.VISIBLE);


			int stepWidth = ViewGroup.LayoutParams.WRAP_CONTENT;
			int stepHeight = ViewGroup.LayoutParams.WRAP_CONTENT;
			int stepMarginLeft = qrX + qrWidth + 150; // Move right of QR Code
			int stepMarginTop = qrCodeBoundingBox.top-80; // Start near QR Code's Y position

			RelativeLayout.LayoutParams step1Params = new RelativeLayout.LayoutParams(stepWidth, stepHeight);
			step1Params.leftMargin = stepMarginLeft;
			step1Params.topMargin = stepMarginTop;
			tvRightStep1.setLayoutParams(step1Params);
			tvRightStep1.setVisibility(View.VISIBLE);

			RelativeLayout.LayoutParams step2Params = new RelativeLayout.LayoutParams(stepWidth, stepHeight);
			step2Params.leftMargin = stepMarginLeft;
			step2Params.topMargin = stepMarginTop + 80;
			tvRightStep2.setLayoutParams(step2Params);
			tvRightStep2.setVisibility(View.VISIBLE);

			RelativeLayout.LayoutParams step3Params = new RelativeLayout.LayoutParams(stepWidth, stepHeight);
			step3Params.leftMargin = stepMarginLeft;
			step3Params.topMargin = stepMarginTop + 160;
			tvRightStep3.setLayoutParams(step3Params);
			tvRightStep3.setVisibility(View.VISIBLE);

			RelativeLayout.LayoutParams step4Params = new RelativeLayout.LayoutParams(stepWidth, stepHeight);
			step4Params.leftMargin = stepMarginLeft;
			step4Params.topMargin = stepMarginTop + 240;
			tvRightStep4.setLayoutParams(step4Params);
			tvRightStep4.setVisibility(View.VISIBLE);

            RelativeLayout.LayoutParams step5Params = new RelativeLayout.LayoutParams(stepWidth, stepHeight);
            step5Params.leftMargin = stepMarginLeft;
            step5Params.topMargin = stepMarginTop + 300;
            tvRightStep5.setLayoutParams(step5Params);
            tvRightStep5.setVisibility(View.VISIBLE);

			// TODO  old before 30-9-25


			RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(750, 350);
			videoParams.leftMargin = 90;  // Align with bottom
            // todo new 30
		//	videoParams.topMargin = qrY + qrHeight + 170; // Below QR Code
			videoParams.topMargin = qrY + qrHeight + 250; // Below QR Code
			playerView.setLayoutParams(videoParams);

			// TODO: Show ImageView on the left side of QR code
			// TODO comment 30-9-25

//			RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
//					350, 350); // Set the correct image size
//
//			int imageWidth = 350; // Explicitly define the image width
//			int imageHeight = 350; // Explicitly define the image height
//
//			int imageX = qrX - imageWidth - 140; // Move ImageView to the left of QR Code
//			int imageY = qrY + (qrHeight / 2) - (imageHeight / 2); // Center vertically with QR Code
//
//			imageParams.leftMargin = Math.max(0, imageX); // Prevent going off-screen
//			imageParams.topMargin = Math.max(0, imageY);
//
//			imageView.setLayoutParams(imageParams);
//			imageView.setVisibility(View.VISIBLE);



			//TODO show textview open file url


			// TODO comment 30-9-25

//			RelativeLayout.LayoutParams urlParams = new RelativeLayout.LayoutParams(
//					ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//
//			int textX = qrX + qrWidth + 60; // Position to the right with 20px padding
//			int textY = qrY + (qrHeight / 2) - (tvOpenFileUrl.getHeight() / 2); // Center vertically
//
//			urlParams.leftMargin = Math.max(0, textX);
//			urlParams.topMargin = Math.max(0, textY);
//
//			tvOpenFileUrl.setLayoutParams(urlParams);
//
//			tvOpenFileUrl.setOnClickListener(v -> {
//				String url = "https://www.google.com/search?q=what+is+android&oq=what+is+android&gs_lcrp=EgZjaHJvbWUyDwgAEEUYORiDARixAxiABDIMCAEQABgUGIcCGIAEMgcIAhAAGIAEMgcIAxAAGIAEMgcIBBAAGIAEMgwIBRAAGBQYhwIYgAQyBwgGEAAYgAQyBwgHEAAYgAQyBwgIEAAYgAQyBwgJEAAYgATSAQgzODgwajBqN6gCCLACAfEFuP2fSQGrrxPxBbj9n0kBq68T&sourceid=chrome&ie=UTF-8";
//				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//				browserIntent.setPackage("com.android.chrome");
//				try {
//					startActivity(browserIntent);
//				} catch (ActivityNotFoundException e) {
//					browserIntent.setPackage(null);
//					startActivity(browserIntent);
//				}
//			});


			//	playerView.setVisibility(View.GONE);

			// TODO old static steps


             // TODO uncomment 30-9-25

			String videoPathStep1 = "android.resource://" + getPackageName() + "/" + R.raw.step1;
			String videoPathStep2 = "android.resource://" + getPackageName() + "/" + R.raw.step2;
			String videoPathStep3 = "android.resource://" + getPackageName() + "/" + R.raw.step3;
			String videoPathStep4 = "android.resource://" + getPackageName() + "/" + R.raw.step4;
			String videoPathStep5 = "android.resource://" + getPackageName() + "/" + R.raw.step2;


			tvRightStep1.setOnClickListener(view -> toggleStep(tvRightStep1, "Step 1",videoPathStep1));
			tvRightStep2.setOnClickListener(view -> toggleStep(tvRightStep2, "Step 2",videoPathStep2));
			tvRightStep3.setOnClickListener(view -> toggleStep(tvRightStep3, "Step 3",videoPathStep3));
			tvRightStep4.setOnClickListener(view -> toggleStep(tvRightStep4, "Step 4",videoPathStep4));
			tvRightStep5.setOnClickListener(view -> toggleStep(tvRightStep5, "Step 5",videoPathStep5));


			// TODO new retrofit
			// TODO comment 30-9-25
		//	fetchDataFromApi();
		}
	}
	private void setTextViewPosition(TextView textView, int x, int y) {
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.leftMargin = x;
		params.topMargin = y;
		textView.setLayoutParams(params);
		textView.setVisibility(View.VISIBLE);
	}

	private void fetchDataFromApi() {
		ApiService apiService = RetrofitClient.getApiService();
		Call<ApiResponse> call = apiService.searchMovies("jack johnson");

		call.enqueue(new Callback<ApiResponse>() {
			@Override
			public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
				if (response.isSuccessful() && response.body() != null) {
					List<MovieItem> movieItems = response.body().getResults();
					if (!movieItems.isEmpty()) {
						updateStepsWithApiData(movieItems);
					}
				}
			}


			@Override
			public void onFailure(Call<ApiResponse> call, Throwable t) {
				Log.e("API_ERROR", "Failed to fetch data: " + t.getMessage());
			}
		});
	}

	private void updateStepsWithApiData(List<MovieItem> movies) {
		if (movies.size() >= 4) {
//			tvRightStep1.setText(movies.get(0).getTrackName());
//			tvRightStep2.setText(movies.get(1).getTrackName());
//			tvRightStep3.setText(movies.get(2).getTrackName());
//			tvRightStep4.setText(movies.get(3).getTrackName());

			tvRightStep1.setText("step1");
			tvRightStep2.setText("step2");
			tvRightStep3.setText("step3");
			tvRightStep4.setText("step4");


//			tvRightStep1.setOnClickListener(view -> toggleStepApi(tvRightStep1, movies.get(0).getTrackName(), movies.get(0).getPreviewUrl(),movies.get(0).getArtworkUrl100()));
//			tvRightStep2.setOnClickListener(view -> toggleStepApi(tvRightStep2, movies.get(1).getTrackName(), movies.get(1).getPreviewUrl(),movies.get(1).getArtworkUrl100()));
//			tvRightStep3.setOnClickListener(view -> toggleStepApi(tvRightStep3, movies.get(2).getTrackName(), movies.get(2).getPreviewUrl(),movies.get(2).getArtworkUrl100()));
//			tvRightStep4.setOnClickListener(view -> toggleStepApi(tvRightStep4, movies.get(3).getTrackName(), movies.get(3).getPreviewUrl(),movies.get(3).getArtworkUrl100()));


			tvRightStep1.setOnClickListener(view -> toggleStepApi(tvRightStep1, "step1", movies.get(0).getPreviewUrl(),movies.get(0).getArtworkUrl100()));
			tvRightStep2.setOnClickListener(view -> toggleStepApi(tvRightStep2, "step2", movies.get(1).getPreviewUrl(),movies.get(1).getArtworkUrl100()));
			tvRightStep3.setOnClickListener(view -> toggleStepApi(tvRightStep3, "step3", movies.get(2).getPreviewUrl(),movies.get(2).getArtworkUrl100()));
			tvRightStep4.setOnClickListener(view -> toggleStepApi(tvRightStep4, "step4", movies.get(3).getPreviewUrl(),movies.get(3).getArtworkUrl100()));
		}
	}

	private void toggleStepApi(TextView selectedStep, String stepText, String videoUrl,String imageUrl) {
		resetAllSteps();

		if (selectedStep.getMaxLines() == Integer.MAX_VALUE) {
			selectedStep.setText(stepText);
			selectedStep.setMaxLines(1);

			if (exoPlayer.isPlaying()) {
				exoPlayer.stop();
			}
		} else {
			//selectedStep.setText(stepText + ": Playing Now");

			selectedStep.setText(stepText);
			selectedStep.setMaxLines(Integer.MAX_VALUE);
			playerView.setVisibility(View.VISIBLE);
			setVideoStep(videoUrl);
		}

		Glide.with(this)
				.load(imageUrl)
				.placeholder(R.drawable.fette1) // Add a placeholder image in res/drawable
				//.error(R.drawable.error_image) // Add an error image in res/drawable
				.into(imageView);

	}



	private void toggleStep(TextView selectedStep, String stepText, String videoPath) {
		resetAllSteps();

		// If the selected step was already expanded, collapse it
		if (selectedStep.getMaxLines() == Integer.MAX_VALUE) {
			selectedStep.setText(stepText);
			selectedStep.setMaxLines(1);

			if (exoPlayer.isPlaying()) {
				exoPlayer.stop();
			}
			playerView.setVisibility(View.GONE);
		} else {
			selectedStep.setText(stepText + " : Showing");
			selectedStep.setMaxLines(Integer.MAX_VALUE);
			playerView.setVisibility(View.VISIBLE);
			setVideoStep(videoPath);
		}
	}

	private void resetAllSteps() {

		for (TextView step : dynamicSteps) {
			step.setMaxLines(1);
		}

		playerView.setVisibility(View.GONE);
		if (exoPlayer.isPlaying()) {
			exoPlayer.stop();
		}
	}


	// todo 10-4

	/*private void toggleStep(TextView selectedStep, String stepText, String videoPath) {
		resetAllSteps();

		// If the selected step was already expanded, collapse it
		if (selectedStep.getMaxLines() == Integer.MAX_VALUE) {
			selectedStep.setText(stepText);
			selectedStep.setMaxLines(1);

		//	playerView.setVisibility(View.GONE);

			if (exoPlayer.isPlaying()) {
				exoPlayer.stop();
			}
		} else {
			selectedStep.setText(stepText + ": Currently Showing");
			selectedStep.setMaxLines(Integer.MAX_VALUE);
			playerView.setVisibility(View.VISIBLE);
			setVideoStep(videoPath);
		}
	}

	private void resetAllSteps() {
		TextView[] steps = {tvRightStep1, tvRightStep2, tvRightStep3, tvRightStep4};

		for (TextView step : steps) {
			step.setText(step.getTag().toString());
			step.setMaxLines(1); // Shrink all steps
		}

		playerView.setVisibility(View.GONE);
		if (exoPlayer.isPlaying()) {
			exoPlayer.stop();
		}
	}*/

	private void setVideoStep(String videoPath){

		if (exoPlayer.isPlaying()) {
			exoPlayer.stop();
		}
		exoPlayer.clearMediaItems();


		// Load the video
		Uri videoUri = Uri.parse(videoPath);
		MediaItem mediaItem = new MediaItem.Builder()
				.setUri(videoUri)
				.build();

		exoPlayer.setMediaItem(mediaItem);
		exoPlayer.prepare();
		playerView.setUseController(false);
		exoPlayer.play();


		playerView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (exoPlayer.isPlaying()) {
						exoPlayer.pause();
					} else {
						exoPlayer.play();
					}
					return true;
				}
				return false;
			}
		});
	}



	/*private void toggleStep(TextView stepView, String stepText,String videoPath) {
		if (stepView.getMaxLines() == 1) {
			stepView.setText(stepText + ": Currently Showing");
			stepView.setMaxLines(Integer.MAX_VALUE);
			playerView.setVisibility(View.VISIBLE);
			setVideoStep(videoPath);
		} else {
			stepView.setText(stepText);
			stepView.setMaxLines(1);
			playerView.setVisibility(View.GONE);
			if (exoPlayer.isPlaying()) {
				exoPlayer.stop();
			}
		}
	}*/





/*	public void updateOverlayViewLeftAndRight(Rect qrCodeBoundingBox) {
		if (qrCodeBoundingBox != null) {
			int qrX = qrCodeBoundingBox.left;
			int qrY = qrCodeBoundingBox.top;
			int qrWidth = qrCodeBoundingBox.width();
			int qrHeight = qrCodeBoundingBox.height();



			RelativeLayout.LayoutParams tvStep1Params = new RelativeLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT);
			tvStep1Params.leftMargin = qrX + qrWidth + 180; // Move right of QR Code
			tvStep1Params.topMargin = qrY + (qrHeight / 2) - 10; // Center vertically
			tvRightStep1.setLayoutParams(tvStep1Params);
			tvRightStep1.setEllipsize(TextUtils.TruncateAt.END);
			tvRightStep1.setVisibility(View.VISIBLE);




			RelativeLayout.LayoutParams tvStep2Params = new RelativeLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT);
			tvStep2Params.leftMargin = qrX + qrWidth + 180; // Move right of QR Code
			tvStep2Params.topMargin = qrY + (qrHeight / 2) - 10; // Center vertically
			tvRightStep2.setLayoutParams(tvStep2Params);
			tvRightStep2.setEllipsize(TextUtils.TruncateAt.END);
			tvRightStep2.setVisibility(View.VISIBLE);

			RelativeLayout.LayoutParams tvStep3Params = new RelativeLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT);
			tvStep3Params.leftMargin = qrX + qrWidth + 180; // Move right of QR Code
			tvStep3Params.topMargin = qrY + (qrHeight / 2) - 10; // Center vertically
			tvRightStep3.setLayoutParams(tvStep3Params);
			tvRightStep3.setEllipsize(TextUtils.TruncateAt.END);
			tvRightStep3.setVisibility(View.VISIBLE);


			RelativeLayout.LayoutParams tvStep4Params = new RelativeLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT);
			tvStep4Params.leftMargin = qrX + qrWidth + 180; // Move right of QR Code
			tvStep4Params.topMargin = qrY + (qrHeight / 2) - 10; // Center vertically
			tvRightStep4.setLayoutParams(tvStep4Params);
			tvRightStep4.setEllipsize(TextUtils.TruncateAt.END);
			tvRightStep4.setVisibility(View.VISIBLE);

			RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(650, 550);
			videoParams.leftMargin = 70;  // Align with Right TextView
			playerView.setLayoutParams(videoParams);
			playerView.setVisibility(View.VISIBLE);



			tvRightStep1.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if(istvStep1){
						//This will shrink textview to 2 lines if it is expanded.
						tvRightStep1.setText("Step 1");

						tvRightStep1.setMaxLines(1);
						istvStep1 = false;
						playerView.setVisibility(View.GONE);

						if (exoPlayer != null && exoPlayer.isPlaying()) {
							exoPlayer.pause();
						}
					} else {
						tvRightStep1.setText("Step 1: Currently Showing");

						tvRightStep1.setMaxLines(Integer.MAX_VALUE);
						istvStep1 = true;

					}
				}
			});

			tvRightStep2.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if(istvStep2){
						tvRightStep2.setText("Step 2");
						tvRightStep2.setMaxLines(1);
						istvStep2 = false;
						playerView.setVisibility(View.GONE);

						if (exoPlayer.isPlaying()) {
							exoPlayer.stop();
						}
					} else {
						tvRightStep2.setText("Step 2: Currently Showing");
						tvRightStep2.setMaxLines(Integer.MAX_VALUE);
						istvStep2 = true;


					}
				}
			});

			tvRightStep3.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if(istvStep3){
						tvRightStep3.setText("Step 3");
						tvRightStep3.setMaxLines(1);
						istvStep3 = false;
						playerView.setVisibility(View.GONE);

						if (exoPlayer.isPlaying()) {
							exoPlayer.stop();
						}
					} else {
						tvRightStep3.setText("Step 3: Currently Showing");
						tvRightStep3.setMaxLines(Integer.MAX_VALUE);
						istvStep3 = true;


					}
				}
			});
			tvRightStep4.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if(istvStep4){
						tvRightStep4.setText("Step 3");
						tvRightStep4.setMaxLines(1);
						istvStep4 = false;
						playerView.setVisibility(View.GONE);

						if (exoPlayer.isPlaying()) {
							exoPlayer.stop();
						}
					} else {
						tvRightStep4.setText("Step 4: Currently Showing");
						tvRightStep4.setMaxLines(Integer.MAX_VALUE);
						istvStep4 = true;


					}
				}
			});

		}
	}*/



/*
	public void updateOverlayViewLeftAndRight(Rect qrCodeBoundingBox) {
		if (qrCodeBoundingBox != null) {
			int qrX = qrCodeBoundingBox.left;
			int qrY = qrCodeBoundingBox.top;
			int qrWidth = qrCodeBoundingBox.width();
			int qrHeight = qrCodeBoundingBox.height();

			RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(650, 450);
			videoParams.leftMargin = qrX + (qrWidth / 2) - (650 / 2);  // Center horizontally
			videoParams.topMargin = qrY - 570;  // Place above QR Code (adjust as needed)
			recognizedQrCodeView.setLayoutParams(videoParams);
			recognizedQrCodeView.setVisibility(View.VISIBLE);


			RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT);
			imageParams.leftMargin = qrX - 400 - 20;  // Move left (20 is spacing)
			imageParams.topMargin = qrY + (qrHeight / 2) - (300 / 2); // Center vertically
			tvLeftStep1.setLayoutParams(imageParams);
			tvRightStep2.setEllipsize(TextUtils.TruncateAt.END);
			tvLeftStep1.setVisibility(View.VISIBLE);



			RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(400, ViewGroup.LayoutParams.WRAP_CONTENT);
			textParams.leftMargin = qrX + qrWidth + 180; // Move right of QR Code
			textParams.topMargin = qrY + (qrHeight / 2) - 10; // Center vertically
			tvRightStep2.setLayoutParams(textParams);
			tvRightStep2.setEllipsize(TextUtils.TruncateAt.END);
			tvRightStep2.setVisibility(View.VISIBLE);



			tvLeftStep1.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if(isTextViewLeftClicked){
						//This will shrink textview to 2 lines if it is expanded.
						tvLeftStep1.setText("Step 1");

						tvLeftStep1.setMaxLines(1);
						isTextViewLeftClicked = false;
						playerView.setVisibility(View.GONE);

						if (exoPlayer != null && exoPlayer.isPlaying()) {
							exoPlayer.pause();
						}
					} else {
						tvLeftStep1.setText("Step 1: Check the Functionality of Security Interlocks ,Check the Functionality of Security Interlocks");

						tvLeftStep1.setMaxLines(Integer.MAX_VALUE);
						isTextViewLeftClicked = true;

						tvLeftStep1.post(new Runnable() {
							@Override
							public void run() {
								repositionVideoViewStep1();
							}
						});

					}
				}
			});

			tvRightStep2.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if(isTextViewRightClicked){
						//This will shrink textview to 2 lines if it is expanded.
						tvRightStep2.setText("Step 2");

						tvRightStep2.setMaxLines(1);
						isTextViewRightClicked = false;
						playerView.setVisibility(View.GONE);

						if (exoPlayer.isPlaying()) {
							exoPlayer.stop();
						}
					} else {
						tvRightStep2.setText("Step 2: Check the Electrical connections in the panel");
						tvRightStep2.setMaxLines(Integer.MAX_VALUE);
						isTextViewRightClicked = true;

						tvRightStep2.post(new Runnable() {
							@Override
							public void run() {
								repositionVideoViewRightStep2();
							}
						});

					}
				}
			});

		}
	}
*/
	private void repositionVideoViewStep1() {
		int[] location = new int[2];
		tvLeftStep1.getLocationOnScreen(location);

		int textViewX = location[0];  // X position of Left TextView
		int textViewY = location[1];  // Y position of Left TextView
		int textViewHeight = tvLeftStep1.getHeight(); // Height of Left TextView

		RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(650, 450);
		videoParams.leftMargin = textViewX;  // Align with Left TextView
		videoParams.topMargin = textViewY + textViewHeight + 20;  // Place below with spacing

		playerView.setLayoutParams(videoParams);
		playerView.setVisibility(View.VISIBLE);

		if (exoPlayer.isPlaying()) {
			exoPlayer.stop();
		}
		exoPlayer.clearMediaItems();

		// Load the video
		String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.step1;
		Uri videoUri = Uri.parse(videoPath);
		MediaItem mediaItem = new MediaItem.Builder()
				.setUri(videoUri)
				.build();

		exoPlayer.setMediaItem(mediaItem);
		exoPlayer.prepare();
		playerView.setUseController(false);
		exoPlayer.play();


		playerView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (exoPlayer.isPlaying()) {
						exoPlayer.pause();
					} else {
						exoPlayer.play();
					}
					return true;
				}
				return false;
			}
		});
	}

	private void repositionVideoViewRightStep2() {
		int[] location = new int[2];
		tvRightStep2.getLocationOnScreen(location);

		int textViewX = location[0];  // X position of Right TextView
		int textViewY = location[1];  // Y position of Right TextView
		int textViewHeight = tvRightStep2.getHeight(); // Height of Right TextView

		RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(650, 550);
		//videoParams.leftMargin = textViewX;  // Align with Right TextView
		videoParams.leftMargin = 70;  // Align with Right TextView
		videoParams.topMargin = textViewY + textViewHeight + 20;  // Place below with spacing

		playerView.setLayoutParams(videoParams);
		playerView.setVisibility(View.VISIBLE);

		if (exoPlayer.isPlaying()) {
			exoPlayer.stop();
		}
		exoPlayer.clearMediaItems();

		// Load the video
		String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.step2;
		Uri videoUri = Uri.parse(videoPath);
		MediaItem mediaItem = new MediaItem.Builder()
				.setUri(videoUri)
				.build();

		exoPlayer.setMediaItem(mediaItem);
		exoPlayer.prepare();
		playerView.setUseController(false);
		exoPlayer.play();

		playerView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (exoPlayer.isPlaying()) {
						exoPlayer.pause();
					} else {
						exoPlayer.play();
					}
					return true;
				}
				return false;
			}
		});
	}

	public void updateOverlayViews(Rect qrCodeBoundingBox) {
		if (qrCodeBoundingBox != null) {
			int qrX = qrCodeBoundingBox.left;
			int qrY = qrCodeBoundingBox.top;
			int qrWidth = qrCodeBoundingBox.width();
			int qrHeight = qrCodeBoundingBox.height();


			RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(650, 450);
			videoParams.leftMargin = qrX + (qrWidth / 2) - (650 / 2);  // Center horizontally
			videoParams.topMargin = qrY - 570;  // Place above QR Code (adjust as needed)
			playerView.setLayoutParams(videoParams);
			playerView.setVisibility(View.VISIBLE);


			RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			textParams.leftMargin = qrX + qrWidth + 180; // Move right of QR Code
			textParams.topMargin = qrY + (qrHeight / 2) - 10; // Center vertically
			recognizedQrCodeView.setLayoutParams(textParams);
			recognizedQrCodeView.setVisibility(View.VISIBLE);

		}
	}


	private TrackerResultListener resultListener = new TrackerResultListener() {
		@Override
		public void sendData(final String metaData) {
			(QrCodeFusionTrackerActivity.this).runOnUiThread(new Runnable() {
				@Override
				public void run() {

					/*recognizedQrCodeView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							String url = "https://developer.maxst.com/MD/doc/6_2_x/andr/ex/qrcodefusion";
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
							v.getContext().startActivity(intent);


						}
					});*/



					if (detectedQRCodes.contains(metaData)) {
						return;
					}
					detectedQRCodes.add(metaData);

					String videoPath;

					recognizedQrCodeView.setText("QRCODE : " + metaData);


					/*
					switch (metaData) {
						case "Step 1: Check the Functionality of Security Interlocks":
							videoPath = "android.resource://" + getPackageName() + "/" + R.raw.step1;
							break;
						case "Step 2: Check the Electrical connections in the panel":
							videoPath = "android.resource://" + getPackageName() + "/" + R.raw.step2;
							break;
						case "Step 3: Check the Suction manifold":
							videoPath = "android.resource://" + getPackageName() + "/" + R.raw.step3;
							break;
						case "Step 4: Check the Lubrication system":
							videoPath = "android.resource://" + getPackageName() + "/" + R.raw.step4;
							break;
						default:
							videoPath = "android.resource://" + getPackageName() + "/" + R.raw.fettemp4;
							break;
					}

					if (exoPlayer.isPlaying()) {
						exoPlayer.stop();
					}
					exoPlayer.clearMediaItems();

					Uri videoUri = Uri.parse(videoPath);
					MediaItem mediaItem = new MediaItem.Builder()
							.setUri(videoUri)
							.build();

					exoPlayer.setMediaItem(mediaItem);
					exoPlayer.prepare();
					playerView.setUseController(false);
					exoPlayer.play();*/
				}
			});
		}

		@Override
		public void sendFusionState(final int state) {
			(QrCodeFusionTrackerActivity.this).runOnUiThread(new Runnable() {
				@Override
				public void run() {
					guideView.setVisibility(state == 1 ? View.INVISIBLE : View.VISIBLE);
				}
			});
		}
	};



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
