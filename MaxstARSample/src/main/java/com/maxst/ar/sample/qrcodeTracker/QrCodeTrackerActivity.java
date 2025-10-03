/*
 * Copyright 2017 Maxst, Inc. All Rights Reserved.
 */

package com.maxst.ar.sample.qrcodeTracker;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.MimeTypes;
import com.maxst.ar.CameraDevice;
import com.maxst.ar.MaxstAR;
import com.maxst.ar.ResultCode;
import com.maxst.ar.TrackerManager;
import com.maxst.ar.sample.R;
import com.maxst.ar.sample.util.SampleUtil;
import com.maxst.ar.sample.util.TrackerResultListener;

import java.io.IOException;

public class QrCodeTrackerActivity extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener {

	private QrCodeTrackerRenderer qrCodeTargetRenderer;
	private GLSurfaceView glSurfaceView;
	private int preferCameraResolution = 0;
	private TextView recognizedQrCodeView;
	private VideoView videoView;
	private ImageView imageView;
	String videoUrl = "https://www.youtube.com/watch?v=-6dfZVpB1UM.mp4";
	private boolean isVideoStarted = false;
	Handler handler = new Handler(Looper.getMainLooper());
	private TextureView textureView;
	private MediaPlayer mediaPlayer;
	private PlayerView playerView;
	private SimpleExoPlayer exoPlayer;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_qrcode_tracker);

		qrCodeTargetRenderer = new QrCodeTrackerRenderer(this);
		glSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surface_view);
		glSurfaceView.setEGLContextClientVersion(2);
		glSurfaceView.setRenderer(qrCodeTargetRenderer);

		videoView = findViewById(R.id.videoView);
		imageView = findViewById(R.id.imageView);

		 textureView = findViewById(R.id.textureView);
		textureView.setSurfaceTextureListener(this);
		 mediaPlayer = new MediaPlayer();


		playerView = findViewById(R.id.playerView);
		exoPlayer = new SimpleExoPlayer.Builder(this).build();
		playerView.setPlayer(exoPlayer);

		recognizedQrCodeView = (TextView)findViewById(R.id.recognized_QrCode);
		qrCodeTargetRenderer.listener = resultListener;

		preferCameraResolution = getSharedPreferences(SampleUtil.PREF_NAME, Activity.MODE_PRIVATE).getInt(SampleUtil.PREF_KEY_CAM_RESOLUTION, 0);

		MaxstAR.init(this.getApplicationContext(), getResources().getString(R.string.app_key));
		MaxstAR.setScreenOrientation(getResources().getConfiguration().orientation);


		textureView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {

				if (textureView.isAvailable()) {
					Toast.makeText(QrCodeTrackerActivity.this, "is available", Toast.LENGTH_SHORT).show();
				//	onSurfaceTextureAvailable(textureView.getSurfaceTexture(), textureView.getWidth(), textureView.getHeight());
				}


				textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
			}
		});


		/*String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.fette;
		Uri videoUri = Uri.parse(videoPath);

		MediaItem mediaItem = new MediaItem.Builder()
				.setMimeType(MimeTypes.VIDEO_MP4)
				.setUri(videoUri)
				.build();

		exoPlayer.setMediaItem(mediaItem);
		exoPlayer.prepare();

		exoPlayer.play();*/
	}


	public void updateOverlayViews(Rect qrCodeBoundingBox) {
		if (qrCodeBoundingBox != null) {
			int qrX = qrCodeBoundingBox.left;
			int qrY = qrCodeBoundingBox.top;
			int qrWidth = qrCodeBoundingBox.width();
			int qrHeight = qrCodeBoundingBox.height();

			RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(650, 450);
			videoParams.leftMargin = qrX + (qrWidth / 2) - 180; // Center horizontally
			videoParams.topMargin = qrY - 650; // Move above QR Code

			/*videoView.setLayoutParams(videoParams);
			videoView.setVisibility(View.VISIBLE);*/

			playerView.setLayoutParams(videoParams);
			playerView.setVisibility(View.VISIBLE);


			if (!isVideoStarted){
				isVideoStarted = true;

				String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.fettemp4;
				Uri videoUri = Uri.parse(videoPath);

				MediaItem mediaItem = new MediaItem.Builder()
						.setMimeType(MimeTypes.VIDEO_MP4)
						.setUri(videoUri)
						.build();

				exoPlayer.setMediaItem(mediaItem);
				exoPlayer.prepare();

				exoPlayer.play();
			}




/*
			videoView.requestLayout(); // Ensure layout updates
			videoView.invalidate(); // Redraw the view


			if (!isVideoStarted) {
				isVideoStarted = true;

				String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.fetteaac;
				videoView.setVideoURI(Uri.parse(videoPath));

				videoView.setOnPreparedListener(mp -> {
					Log.d("VideoView", "Video has started playing");
					videoView.seekTo(100);
					videoView.start();

					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							videoView.start();
							videoView.seekTo(100);
						}
					}, 3000);



				});

				videoView.setOnCompletionListener(mp -> Log.d("VideoView", "Video has finished playing"));

				videoView.setOnErrorListener((mp, what, extra) -> {
					Log.e("VideoView", "Error playing video: what=" + what + ", extra=" + extra);
					return true;
				});
			}*/









			// textureview -
/*

			RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(550, 350);
			videoParams.leftMargin = qrX + (qrWidth / 2) - 150; // Center horizontally
			videoParams.topMargin = qrY - 550; // Move above QR Code
			textureView.setLayoutParams(videoParams);
			textureView.setVisibility(View.VISIBLE);

			// Initialize MediaPlayer only if it's null

			if (!isVideoStarted) {
				isVideoStarted = true;

			*//*	textureView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {

						if (textureView.isAvailable()) {
							onSurfaceTextureAvailable(textureView.getSurfaceTexture(), textureView.getWidth(), textureView.getHeight());
						}


						textureView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					}
				});*//*

				if (textureView.isAvailable()) {
					onSurfaceTextureAvailable(textureView.getSurfaceTexture(), textureView.getWidth(), textureView.getHeight());
				}

			}*/


			RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(400, 300);
			imageParams.leftMargin = qrX - 450; // Move left of QR Code
			imageParams.topMargin = qrY + (qrHeight / 2) - 75; // Center vertically
			imageView.setLayoutParams(imageParams);
			imageView.setVisibility(View.VISIBLE);


			RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			textParams.leftMargin = qrX + qrWidth + 180; // Move right of QR Code
			textParams.topMargin = qrY + (qrHeight / 2) - 10; // Center vertically
			recognizedQrCodeView.setLayoutParams(textParams);
			recognizedQrCodeView.setVisibility(View.VISIBLE);


		}
	}



	public void updateOverlayViewss(Rect qrCodeBoundingBox) {
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

			if (!isVideoStarted) {
				isVideoStarted = true;
				String videoPath = "android.resource://" + getPackageName() + "/" + R.raw.fettemp4;
				Uri videoUri = Uri.parse(videoPath);

				MediaItem mediaItem = new MediaItem.Builder()
						.setMimeType(MimeTypes.VIDEO_MP4)
						.setUri(videoUri)
						.build();

				exoPlayer.setMediaItem(mediaItem);
				exoPlayer.prepare();
				playerView.setUseController(false);
				exoPlayer.play();
			}


			RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(400, 300);
			imageParams.leftMargin = qrX - 400 - 20;  // Move left (20 is spacing)
			imageParams.topMargin = qrY + (qrHeight / 2) - (300 / 2); // Center vertically
			imageView.setLayoutParams(imageParams);
			imageView.setVisibility(View.VISIBLE);


			/*RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			textParams.leftMargin = qrX + qrWidth + 70; // Move right (20 is spacing)
			textParams.topMargin = qrY + (qrHeight / 2) - 10; // Center vertically
			recognizedQrCodeView.setLayoutParams(textParams);
			recognizedQrCodeView.setVisibility(View.VISIBLE);*/

			RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			textParams.leftMargin = qrX + qrWidth + 180; // Move right of QR Code
			textParams.topMargin = qrY + (qrHeight / 2) - 10; // Center vertically
			recognizedQrCodeView.setLayoutParams(textParams);
			recognizedQrCodeView.setVisibility(View.VISIBLE);
		}
	}


	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		Surface s = new Surface(surface);
		mediaPlayer.setSurface(s);

		try {
			AssetFileDescriptor afd = getResources().openRawResourceFd(R.raw.fetteaac);
			if (afd == null) return;

			mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
			afd.close();

			mediaPlayer.prepareAsync();
			mediaPlayer.setOnPreparedListener(mp -> {
				mp.start();
				Toast.makeText(QrCodeTrackerActivity.this, "Video Started!", Toast.LENGTH_SHORT).show();
			});

		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

	@Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

	@Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
		return true;
	}

	@Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {}



	@Override
	protected void onResume() {
		super.onResume();

		glSurfaceView.onResume();
		TrackerManager.getInstance().startTracker(TrackerManager.TRACKER_TYPE_QR_TRACKER);

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

		glSurfaceView.queueEvent(new Runnable() {
			@Override
			public void run() {
			}
		});

		glSurfaceView.onPause();

		TrackerManager.getInstance().stopTracker();
		CameraDevice.getInstance().stop();
		MaxstAR.onPause();

		// video
		if (exoPlayer != null) {
			exoPlayer.setPlayWhenReady(false);
			exoPlayer.getPlaybackState();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		TrackerManager.getInstance().destroyTracker();
		MaxstAR.deinit();

		// video
		if (exoPlayer != null) {
			exoPlayer.release();
			exoPlayer = null;
		}
	}

	@Override
	public void onClick(View view) {

	}

	private TrackerResultListener resultListener = new TrackerResultListener() {
		@Override
		public void sendData(final String metaData) {
			(QrCodeTrackerActivity.this).runOnUiThread(new Runnable(){
				@Override
				public void run() {
					recognizedQrCodeView.setText("QRCODE : " + metaData);
				}
			});
		}

		@Override
		public void sendFusionState(final int state) {
			(QrCodeTrackerActivity.this).runOnUiThread(new Runnable(){
				@Override
				public void run() {
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


/*	RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(200, 200);
			videoParams.leftMargin = qrX; // Align with QR Code
			videoParams.topMargin = qrY - 110; // Move above QR Code
			videoView.setLayoutParams(videoParams);
			videoView.setVisibility(View.VISIBLE);

			Uri videoUri = Uri.parse(videoUrl);

			// Move ImageView to the left of the QR Code
			RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(250, 250);
			imageParams.leftMargin = qrX - 60; // Move left of QR Code
			imageParams.topMargin = qrY + (qrHeight / 2) - 25; // Center vertically
			imageView.setLayoutParams(imageParams);
			imageView.setVisibility(View.VISIBLE);

			// Move TextView to the right of the QR Code
			RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			textParams.leftMargin = qrX + qrWidth + 10; // Move right of QR Code
			textParams.topMargin = qrY + (qrHeight / 2) - 10; // Center vertically
			recognizedQrCodeView.setLayoutParams(textParams);
			recognizedQrCodeView.setVisibility(View.VISIBLE);*/