package com.maxst.ar.sample;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.collision.CollisionShape;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.SceneView;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.ux.TransformationSystem;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModelActivity extends AppCompatActivity {
    private SceneView sceneView;
    String modelUrl;
    private GestureDetector gestureDetector;
    private TransformationSystem transformationSystem;

    private final List<Node> annotationNodes = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model);
        // modelUrl = getIntent().getStringExtra("model_url");

        ModelData modelData = (ModelData) getIntent().getSerializableExtra("model_data");
        sceneView = findViewById(R.id.sceneView);
        // sceneView.setBackgroundColor(Color.parseColor("#e8edf7"));
        //  sceneView.setBackgroundColor(Color.WHITE);

        transformationSystem = new TransformationSystem(
                getResources().getDisplayMetrics(),
                new FootprintSelectionVisualizer()
        );

        sceneView.getScene().addOnPeekTouchListener((hitTestResult, motionEvent) -> {
            transformationSystem.onTouch(hitTestResult, motionEvent);
        });

        if (modelData != null) {
            modelUrl = modelData.getUrl();
            List<MarkerData> markers = modelData.getMarkers();
        } else {
            Toast.makeText(ModelActivity.this, "data null ..", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            loadModelFromUrl(modelUrl, modelData.getMarkers());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadModelFromUrl(String glbFileName, List<MarkerData> marker) throws Exception {

        String url = "http://192.168.11.109:3009/" + glbFileName;
        //  String url = "http://192.168.95.42:3009/" + glbFileName;

        // String url = "https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb";
        //  String url = "https://modelviewer.dev/shared-assets/models/Astronaut.glb";

        sceneView.resume();

        ModelRenderable.builder()
                .setSource(this, Uri.parse(url))
                .setIsFilamentGltf(true)
                .build()
                .thenAccept(renderable -> {

                    Node modelNode = new Node();
                    modelNode.setRenderable(renderable);


                    CollisionShape collisionShape = renderable.getCollisionShape();
                    if (collisionShape instanceof Box) {
                        Box boundingBox = (Box) collisionShape;
                        Vector3 size = boundingBox.getSize(); // Dimensions in meters
                        Log.d("ModelSize", "Width: " + size.x + ", Height: " + size.y + ", Depth: " + size.z);
                    }

                    // TODo working

                    //  modelNode.setLocalPosition(new Vector3(0f, -0.5f, -1f));
                    //  modelNode.setLocalScale(new Vector3(0.5f, 0.5f, 0.5f));

                    // TODo working -2

                    // modelNode.setLocalPosition(new Vector3(0f, -0.5f, -3f));
                    //  modelNode.setLocalScale(new Vector3(1f, 1f, 1f));


                    //   modelNode.setLocalPosition(new Vector3(0f, 0f, -3f));

                    modelNode.setLocalPosition(new Vector3(0f, 0f, -3f));

                    // TODo  default / normal 1.0
                    //  modelNode.setLocalScale(new Vector3(1.0f, 1.0f, 1.0f));
                    //   modelNode.setLocalScale(new Vector3(0.5f, 0.5f, 0.5f));


                    sceneView.getScene().addChild(modelNode);


                    setupGestureDetection(modelNode);
                    addAnnotations(modelNode, marker, renderable);


                    // todo  all annotations always face the camera.

//
//                    sceneView.getScene().addOnUpdateListener(frameTime -> {
//                        Vector3 cameraPosition = sceneView.getScene().getCamera().getWorldPosition();
//
//                        for (Node annotationNode : annotationNodes) {
//                            Vector3 annotationPosition = annotationNode.getWorldPosition();
//                            Vector3 direction = Vector3.subtract(cameraPosition, annotationPosition).normalized();
//                            annotationNode.setWorldRotation(Quaternion.lookRotation(direction, Vector3.up()));
//                        }
//                    });


                })
                .exceptionally(throwable -> {
                    //       Toast.makeText(this, "Model load failed: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    return null;
                });
    }

    private void setupGestureDetection(Node modelNode) {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private float rotationX = 0f;
            private float rotationY = 0f;

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

//                rotationY += distanceX * 0.2f; // rotate left/right
//                rotationX += distanceY * 0.2f; // rotate up/down
//
//                Quaternion rotationXQuat = Quaternion.axisAngle(new Vector3(1f, 0f, 0f), rotationX);
//                Quaternion rotationYQuat = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), rotationY);
//
//                modelNode.setLocalRotation(Quaternion.multiply(rotationXQuat, rotationYQuat));
//                return true;

                int pointerCount = e2.getPointerCount();

                if (pointerCount == 1) {
                    //  Toast.makeText(ModelActivity.this, "1 finger  ..", Toast.LENGTH_SHORT).show();
                } else {
                    // Toast.makeText(ModelActivity.this, "2 finger  ..", Toast.LENGTH_SHORT).show();
                }

                if (pointerCount == 1 || pointerCount == 2) {
                    float rotateFactor = 0.2f;

                    rotationY += distanceX * rotateFactor; // rotate left/right
                    rotationX += distanceY * rotateFactor; // rotate up/down

                    Quaternion qx = Quaternion.axisAngle(new Vector3(1f, 0f, 0f), rotationX);
                    Quaternion qy = Quaternion.axisAngle(new Vector3(0f, 1f, 0f), rotationY);
                    modelNode.setLocalRotation(Quaternion.multiply(qx, qy));
                }

                return true;


            }
        });

        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(this, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                Vector3 scale = modelNode.getLocalScale();
                float scaleFactor = detector.getScaleFactor();
                float newScale = Math.max(0.5f, Math.min(scale.x * scaleFactor, 4.0f));
                modelNode.setLocalScale(new Vector3(newScale, newScale, newScale));
                return true;
            }
        });

        sceneView.getScene().addOnPeekTouchListener((hitTestResult, motionEvent) -> {
            gestureDetector.onTouchEvent(motionEvent);
            scaleGestureDetector.onTouchEvent(motionEvent);
        });


//        sceneView.getScene().addOnPeekTouchListener((hitTestResult, motionEvent) -> {
//            int pointerCount = motionEvent.getPointerCount();
//
//            switch (motionEvent.getActionMasked()) {
//                case MotionEvent.ACTION_MOVE:
//                    if (pointerCount == 1) {
//                        Toast.makeText(this, "1 finger ", Toast.LENGTH_SHORT).show();
//                        gestureDetector.onTouchEvent(motionEvent);
//                        scaleGestureDetector.onTouchEvent(motionEvent);
//                    } else if (pointerCount == 2) {
//                        Toast.makeText(this, "2 finger ", Toast.LENGTH_SHORT).show();
//                        gestureDetector.onTouchEvent(motionEvent);
//                        scaleGestureDetector.onTouchEvent(motionEvent);
//                    }
//                    break;
//
//                case MotionEvent.ACTION_DOWN:
//                case MotionEvent.ACTION_POINTER_DOWN:
//                case MotionEvent.ACTION_UP:
//                case MotionEvent.ACTION_POINTER_UP:
//                 /*   // Handle touch start/end if needed
//                    gestureDetector.onTouchEvent(motionEvent);
//                    scaleGestureDetector.onTouchEvent(motionEvent);*/
//                    break;
//            }
//        });


    }


    // TODO annotation 99 %

    private void addAnnotations(Node modelNode, List<MarkerData> marker, ModelRenderable renderable) {

        for (MarkerData markers : marker) {
            MarkerData.Position pos = markers.getPosition();
            String color = markers.getColor();
            String label = markers.getText() + ":" + markers.getValue();
            Vector3 markerPosition = new Vector3(pos.x, pos.y, pos.z);

            createAnnotation(modelNode, markerPosition, label, color, renderable, markers.getSensor());

            //   createAnnotation(markerPosition, label, color);


            //  createLabelPlane(modelNode,markerPosition,label,color);
            //
            //  createAnnotation(modelNode, markerPosition, label,color,renderable,false,null);


//            MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(android.graphics.Color.parseColor("#00FF00")))
//                    .thenAccept(material -> {
//                        // Once material is ready, call your method
//                        createAnnotation(modelNode, markerPosition, "", "#FFFFFF", renderable, true, material);
//                    });

        }
    }


    // TODO working 99%

    private void createAnnotation(Node parentNode, Vector3 localPosition, String labelText, String color, ModelRenderable renderable, String sensorId) {
        ViewRenderable.builder()
                .setView(this, R.layout.view_annotation)
                .build()
                .thenAccept(viewRenderable -> {

                    Node annotationNode = new Node();
                    annotationNode.setParent(parentNode);


                    // Use bounding box to get model size

                    Vector3 offset = new Vector3(0.1f, 0.3f, 0f);
                    CollisionShape collisionShape = renderable.getCollisionShape();

                    if (collisionShape instanceof Box) {
                        Box boundingBox = (Box) collisionShape;
                        Vector3 modelSize = boundingBox.getSize();
                        // Offset based on a fraction of model height/width

//                        offset = new Vector3(
//                                modelSize.x * 0.1f,
//                                modelSize.y * 0.5f,
//                                0f);

                        // todo working

//                        offset = new Vector3(
//                                modelSize.x * 0.1f,
//                                modelSize.y * 0.5f,
//                                0.1f
//                        );


//                        offset = new Vector3(
//                                modelSize.x * scaleFactor.x * 0.5f,
//                                modelSize.y * scaleFactor.y * 0.5f,
//                                0.5f * scaleFactor.z
//                        );


                        Vector3 scaleFactor = parentNode.getLocalScale();

//                        offset = new Vector3(
//                                scaleFactor.x * 0.1f,
//                                scaleFactor.y * 0.5f,
//                               0.1f
//                               // scaleFactor.z * 0.2f
//                        );


                        offset = new Vector3(
                                modelSize.x * 0.1f,
                                modelSize.y * 0.5f,
                                modelSize.z * 0.1f
                        );


                    }

                    Vector3 offsetPosition = Vector3.add(localPosition, offset);

                    viewRenderable.setRenderPriority(Renderable.RENDER_PRIORITY_FIRST);

                    annotationNodes.add(annotationNode);
                    viewRenderable.setShadowCaster(false);

                    annotationNode.setLocalPosition(offsetPosition);

                    annotationNode.setRenderable(viewRenderable);

                    // annotationNode.setLookDirection(sceneView.getScene().getCamera().getForward());


                    View view = viewRenderable.getView();
                    TextView textView = view.findViewById(R.id.annotationText);
                    textView.setText(labelText);
                    try {
                        int parsedColor = Color.parseColor(color);
                        textView.setTextColor(parsedColor);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }

                    //  annotationNode.setLookDirection(Vector3.forward());

                    annotationNode.setOnTapListener((hitTestResult, motionEvent) -> {
                        showSensorChartDialog(labelText, color, sensorId);
                    });


                });
    }


    private void showSensorChartDialog(String labelText, String color, String sensorId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sensor_chart, null);
        builder.setView(dialogView);

        TextView sensorTitle = dialogView.findViewById(R.id.sensorTitle);
        LineChart chart = dialogView.findViewById(R.id.sensorChart);

        try {
            sensorTitle.setTextColor(Color.parseColor(color));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<LiveResponse> call = apiService.getLivesFromMachine("6549f08d6bec839a13ece0ae");

        call.enqueue(new Callback<LiveResponse>() {
            @Override
            public void onResponse(Call<LiveResponse> call, Response<LiveResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<DataLive> models = response.body().getData();
                    for (DataLive model : models) {
                        if (sensorId.equals(model.get_id())) {
                            sensorTitle.setText("Sensor: " + model.getTag());

                            List<DataLive.Previous> prevList = model.getPrevious();
                            if (prevList != null && !prevList.isEmpty()) {
                                List<Entry> entries = new ArrayList<>();
                                List<String> timeLabels = new ArrayList<>();

                                for (int i = 0; i < prevList.size(); i++) {
                                    try {
                                        float v = Float.parseFloat(removeQuote(prevList.get(i).getValue()));
                                        String timeRaw = removeQuote(prevList.get(i).getTime());

                                        // Convert to HH:mm:ss
                                        SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss z", Locale.ENGLISH);
                                        Date date = inputFormat.parse(timeRaw);
                                        SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
                                        String formattedTime = outputFormat.format(date);

                                        entries.add(new Entry(i, v));
                                        timeLabels.add(formattedTime);

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Log.d("CHART_ERROR", "Error parsing value/time: " + prevList.get(i).toString());
                                    }
                                }

                                // Set up LineDataSet
                                LineDataSet dataSet = new LineDataSet(entries, "Sensor Values");
                                dataSet.setColor(Color.parseColor(color));
                                dataSet.setCircleColor(Color.parseColor(color));
                                dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
                                dataSet.setDrawValues(false);

                                LineData lineData = new LineData(dataSet);
                                chart.setData(lineData);

                                // X-axis formatting
                                XAxis xAxis = chart.getXAxis();
                                xAxis.setValueFormatter(new IndexAxisValueFormatter(timeLabels));
                                xAxis.setGranularity(1f);
                                xAxis.setLabelRotationAngle(-45f);
                                xAxis.setTextColor(Color.WHITE);
                                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                                // Y-axis formatting
                                YAxis leftAxis = chart.getAxisLeft();
                                leftAxis.setTextColor(Color.parseColor(color));
                                leftAxis.setDrawGridLines(true);
                                leftAxis.setGranularityEnabled(true);

                                chart.getAxisRight().setEnabled(false);
                                chart.getLegend().setTextColor(Color.WHITE);
                                chart.getDescription().setEnabled(false);

                                // Interactivity
                                chart.setTouchEnabled(true);
                                chart.setDragEnabled(true);
                                chart.setScaleEnabled(true);
                                chart.setPinchZoom(true);
                                chart.setHighlightPerDragEnabled(true);

                                chart.notifyDataSetChanged();
                                chart.invalidate();
                                chart.moveViewToX(entries.size());
                            }
                        }
                    }
                } else {
                    Toast.makeText(ModelActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LiveResponse> call, Throwable t) {
            }
        });

        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }



    public static String removeQuote(String s) {
        return s.replace("\"", "");
    }



    // todo working static

//    private void showSensorChartDialog(String labelText, String color, String sensorId) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_sensor_chart, null);
//        builder.setView(dialogView);
//
//        TextView sensorTitle = dialogView.findViewById(R.id.sensorTitle);
//        LineChart chart = dialogView.findViewById(R.id.sensorChart);
//
//        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
//        Call<LiveResponse> call = apiService.getLivesFromMachine("6549f08d6bec839a13ece0ae");
//
//
//        call.enqueue(new Callback<LiveResponse>() {
//            @Override
//            public void onResponse(Call<LiveResponse> call, Response<LiveResponse> response) {
//                if (response.isSuccessful() && response.body() != null) {
//                    List<DataLive> models = response.body().getData();
//                    for (DataLive model : models) {
//
//                        if (sensorId.equals(model.get_id())){
//                            sensorTitle.setText("Sensor: " + model.getTag());
//
//                            List<DataLive.Previous> prevList = model.getPrevious();
//                            if (prevList != null) {
//                                for (DataLive.Previous prev : prevList) {
//                                    Log.d("PREVIOUS_DATA", "Time: " + prev.getTime() + ", Value: " + prev.getValue());
//                                }
//                            }
//
//                        }
//                    }
//
//
//
//
//
//                    Toast.makeText(ModelActivity.this, "success ...", Toast.LENGTH_SHORT).show();
//
//                } else {
//                    Toast.makeText(ModelActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Call<LiveResponse> call, Throwable t) {
//                Log.e("API_ERROR", t.getMessage(), t);
//            }
//        });
//
//
//     //   sensorTitle.setText("Sensor: " + labelText);
//        try {
//            sensorTitle.setTextColor(Color.parseColor(color));
//        } catch (IllegalArgumentException e) {
//            e.printStackTrace();
//        }
//
//
//        List<Entry> entries = new ArrayList<>();
//        entries.add(new Entry(0, 10));
//        entries.add(new Entry(1, 20));
//        entries.add(new Entry(2, 15));
//        entries.add(new Entry(3, 30));
//
//        LineDataSet dataSet = new LineDataSet(entries, "Sensor Values");
//        dataSet.setColor(Color.RED);
//        dataSet.setValueTextColor(Color.BLACK);
//        LineData lineData = new LineData(dataSet);
//        chart.setData(lineData);
//        chart.invalidate();
//
//        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
//        builder.show();
//    }


//    private void createAnnotation(Vector3 worldPosition, String labelText, String color) {
//        ViewRenderable.builder()
//                .setView(this, R.layout.view_annotation)
//                .build()
//                .thenAccept(viewRenderable -> {
//                    Node annotationNode = new Node();
//
//                    // Add small Y offset so it appears above the model surface
//                    Vector3 offset = new Vector3(0f, 0.1f, 0f);
//                    Vector3 finalPosition = Vector3.add(worldPosition, offset);
//                    annotationNode.setWorldPosition(finalPosition);
//
//                    viewRenderable.setRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
//                    viewRenderable.setShadowCaster(false);
//                    annotationNode.setRenderable(viewRenderable);
//
//                    // Face the camera (optional if you're using onUpdate)
//                    Vector3 direction = Vector3.subtract(
//                            sceneView.getScene().getCamera().getWorldPosition(),
//                            finalPosition
//                    );
//                    annotationNode.setWorldRotation(Quaternion.lookRotation(direction, Vector3.up()));
//
//                    sceneView.getScene().addChild(annotationNode);
//
//                    // Customize the view
//                    View view = viewRenderable.getView();
//                    TextView textView = view.findViewById(R.id.annotationText);
//                    textView.setText(labelText);
//                    try {
//                        int parsedColor = Color.parseColor(color);
//                        textView.setTextColor(parsedColor);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    }
//
//                    annotationNodes.add(annotationNode);
//                });
//    }


    // todo not working

//    private void createAnnotation(Node parentNode, Vector3 localPosition, String labelText, String color, ModelRenderable renderable) {
//        ViewRenderable.builder()
//                .setView(this, R.layout.view_annotation)
//                .build()
//                .thenAccept(viewRenderable -> {
//
//                    Node annotationNode = new Node();
//                    // Attach to scene instead of model so it remains screen-facing
//                    sceneView.getScene().addChild(annotationNode);
//
//                    // Apply rotation to localPosition to get offset in world space
//                    Quaternion modelRotation = parentNode.getWorldRotation();
//                    Vector3 rotatedOffset = Quaternion.rotateVector(modelRotation, localPosition);
//                    Vector3 worldPosition = Vector3.add(parentNode.getWorldPosition(), rotatedOffset);
//
//                    // Set world position of the annotation
//                    annotationNode.setWorldPosition(worldPosition);
//
//                    // Face the camera on every frame (handled elsewhere with addOnUpdateListener)
//
//                    // Prioritize rendering above model
//                    viewRenderable.setRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
//                    viewRenderable.setShadowCaster(false);
//                    annotationNode.setRenderable(viewRenderable);
//
//                    // Keep track
//                    annotationNodes.add(annotationNode);
//
//                    // Set the label text and color
//                    View view = viewRenderable.getView();
//                    TextView textView = view.findViewById(R.id.annotationText);
//                    textView.setText(labelText);
//
//                    try {
//                        int parsedColor = Color.parseColor(color);
//                        textView.setTextColor(parsedColor);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    }
//                });
//    }


    // todo
//    private void createAnnotation(Node parentNode, Vector3 localPosition, String labelText, String color, ModelRenderable renderable) {
//        ViewRenderable.builder()
//                .setView(this, R.layout.view_annotation)
//                .build()
//                .thenAccept(viewRenderable -> {
//
//                    Node annotationNode = new Node();
//
//                    // Get model size from collision shape
//                    Vector3 offset = new Vector3(0f, 0.3f, 0f); // default offset
//                    CollisionShape collisionShape = renderable.getCollisionShape();
//
//                    if (collisionShape instanceof Box) {
//                        Box boundingBox = (Box) collisionShape;
//                        Vector3 modelSize = boundingBox.getSize();
//
//                        // Raise the annotation above the model (in model’s local up direction)
//                        offset = new Vector3(0f, modelSize.y * 0.6f, 0f);
//                    }
//
//                    // Convert local position to world position
//                    Quaternion parentRotation = parentNode.getWorldRotation();
//                    Vector3 worldLocalPosition = Vector3.add(
//                            parentNode.getWorldPosition(),
//                            Quaternion.rotateVector(parentRotation, localPosition)
//                    );
//
//                    // Rotate the offset
//                    Vector3 rotatedOffset = Quaternion.rotateVector(parentRotation, offset);
//
//                    // Final world position
//                    Vector3 worldOffsetPosition = Vector3.add(worldLocalPosition, rotatedOffset);
//
//                    // Place the annotation at the computed world position
//                    annotationNode.setWorldPosition(worldOffsetPosition);
//
//                    // Optional: Make the label always face the camera
//                    annotationNode.setLookDirection(sceneView.getScene().getCamera().getForward());
//
//                    // Set renderable
//                    annotationNode.setRenderable(viewRenderable);
//                    viewRenderable.setShadowCaster(false);
//                    viewRenderable.setRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
//
//                    // Add to scene (if you're managing nodes)
//                    annotationNodes.add(annotationNode);
//
//                    // Set text
//                    View view = viewRenderable.getView();
//                    TextView textView = view.findViewById(R.id.annotationText);
//                    textView.setText(labelText);
//                    try {
//                        int parsedColor = Color.parseColor(color);
//                        textView.setTextColor(parsedColor);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    }
//                });
//    }


    // todo using modelfactory

    private void createLabelPlane(Node parentNode, Vector3 localPosition, String labelText, String colorHex) {
        int androidColor = Color.parseColor(colorHex); // Example: "#FF0000"
        float red = Color.red(androidColor) / 255f;
        float green = Color.green(androidColor) / 255f;
        float blue = Color.blue(androidColor) / 255f;
        float alpha = Color.alpha(androidColor) / 255f;

        com.google.ar.sceneform.rendering.Color sceneformColor =
                new com.google.ar.sceneform.rendering.Color(red, green, blue, alpha);

        MaterialFactory.makeOpaqueWithColor(this, sceneformColor)
                .thenAccept(material -> {

                    ModelRenderable labelRenderable = ShapeFactory.makeCube(
                            new Vector3(0.2f, 0.02f, 0.01f),
                            Vector3.zero(),
                            material
                    );

                    Node labelNode = new Node();
                    labelNode.setParent(parentNode);
                    labelNode.setLocalPosition(Vector3.add(localPosition, new Vector3(0f, 0.5f, 0f)));
                    labelNode.setRenderable(labelRenderable);

                    // Optionally add text with ViewRenderable
                    // addTextOnTop(labelNode, labelText);
                });
    }


    private void addTextOnTop(Node parent, String text) {
        ViewRenderable.builder()
                .setView(this, R.layout.view_annotation)
                .build()
                .thenAccept(viewRenderable -> {
                    Node textNode = new Node();
                    textNode.setParent(parent);
                    textNode.setLocalPosition(new Vector3(0f, 0.05f, 0f)); // Slight offset above the plane

                    View view = viewRenderable.getView();
                    TextView textView = view.findViewById(R.id.annotationText);
                    textView.setText(text);
                    viewRenderable.setShadowCaster(false);
                    textNode.setRenderable(viewRenderable);
                });
    }


    // todo text bitmap working 99

    private void createTextAnnotation(Node parentNode, Vector3 localPosition, String text) {
        Bitmap textBitmap = createTextBitmap(text, Color.RED, 32);

        Texture.builder()
                .setSource(textBitmap)
                .build()
                .thenAccept(texture -> {
                    MaterialFactory.makeTransparentWithTexture(this, texture)
                            .thenAccept(material -> {

                                ModelRenderable quadRenderable = ShapeFactory.makeCube(
                                        new Vector3(0.3f, 0.1f, 0.001f),
                                        Vector3.zero(),
                                        material
                                );

                                Vector3 scaleFactor = parentNode.getLocalScale();

                                Vector3 offset = new Vector3(
                                        scaleFactor.x * 0.1f,
                                        scaleFactor.y * 0.9f,
                                        scaleFactor.z * 0.1f
                                );

                                Vector3 finalPosition = Vector3.add(localPosition, offset);

                                // ✅ Use custom BillboardNode instead of Node
                                BillboardNode textNode = new BillboardNode();
                                textNode.setParent(parentNode);
                                textNode.setLocalPosition(finalPosition);
                                textNode.setRenderable(quadRenderable);
                            });
                });
    }


    private Bitmap createTextBitmap(String text, int color, float textSizeSp) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        paint.setTextSize(textSizeSp * getResources().getDisplayMetrics().scaledDensity);
        paint.setTextAlign(Paint.Align.LEFT);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        Bitmap bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(text, 0, bounds.height(), paint);

        return bitmap;
    }


//    private void createAnnotation(Node parentNode, Vector3 localPosition, String labelText, String color, ModelRenderable renderable) {
//        ViewRenderable.builder()
//                .setView(this, R.layout.view_annotation)
//                .build()
//                .thenAccept(viewRenderable -> {
//
//                    Node annotationNode = new Node();
//                    annotationNode.setParent(parentNode);
//
//                    // STEP 1: Determine model size from its bounding box
//                    Vector3 modelSize = new Vector3(0f, 0.3f, 0f); // Default if bounding box fails
//                    CollisionShape collisionShape = renderable.getCollisionShape();
//                    if (collisionShape instanceof Box) {
//                        Box boundingBox = (Box) collisionShape;
//                        modelSize = boundingBox.getSize();
//                    }
//
//                    // STEP 2: Compute offset ABOVE the model
//                    Vector3 offset = new Vector3(0f, modelSize.y * 0.6f, 0f);
//
//                    // STEP 3: Apply rotation to both localPosition and offset
//                    Quaternion parentRotation = parentNode.getWorldRotation();
//
//                    Vector3 rotatedLocalPosition = Quaternion.rotateVector(parentRotation, localPosition);
//                    Vector3 rotatedOffset = Quaternion.rotateVector(parentRotation, offset);
//
//                    // STEP 4: Combine to get final world position
//                    Vector3 finalWorldPosition = Vector3.add(
//                            Vector3.add(parentNode.getWorldPosition(), rotatedLocalPosition),
//                            rotatedOffset
//                    );
//
//                    // STEP 5: Set position, renderable, and face camera
//                    annotationNode.setWorldPosition(finalWorldPosition);
//                    annotationNode.setRenderable(viewRenderable);
//                    annotationNode.setLookDirection(sceneView.getScene().getCamera().getForward());
//                    viewRenderable.setShadowCaster(false);
//
//                    // STEP 6: Set annotation text and color
//                    View view = viewRenderable.getView();
//                    TextView textView = view.findViewById(R.id.annotationText);
//                    textView.setText(labelText);
//                    try {
//                        int parsedColor = Color.parseColor(color);
//                        textView.setTextColor(parsedColor);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    }
//
//                    annotationNodes.add(annotationNode);
//                });
//    }


    // todo 2

//    private void createAnnotation(Node parentNode, Vector3 localPosition, String labelText, String color, ModelRenderable renderable) {
//        ViewRenderable.builder()
//                .setView(this, R.layout.view_annotation)
//                .build()
//                .thenAccept(viewRenderable -> {
//                    Node annotationNode = new Node();
//                    annotationNode.setParent(parentNode);
//
//                    // Calculate offset relative to model's scale
//                    Vector3 offset = new Vector3(0.1f, 0.3f, 0.5f);
//                    CollisionShape collisionShape = renderable.getCollisionShape();
//
//                    if (collisionShape instanceof Box) {
//                        Box boundingBox = (Box) collisionShape;
//                        Vector3 scaleFactor = parentNode.getLocalScale();
//
//                        offset = new Vector3(
//                                scaleFactor.x * 0.5f,
//                                scaleFactor.y * 0.5f,
//                                scaleFactor.z * 0.8f
//                        );
//                    }
//
//                    Vector3 offsetPosition = Vector3.add(localPosition, offset);
//                    viewRenderable.setRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
//
//                    annotationNode.setLocalPosition(offsetPosition);
//                    annotationNode.setRenderable(viewRenderable);
//
//                    annotationNodes.add(annotationNode);
//
//                    // Set text and color
//                    View view = viewRenderable.getView();
//                    TextView textView = view.findViewById(R.id.annotationText);
//                    textView.setText(labelText);
//                    try {
//                        int parsedColor = Color.parseColor(color);
//                        textView.setTextColor(parsedColor);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    }
//
//                    // Make the annotation always face the camera
//                    sceneView.getScene().addOnUpdateListener(frameTime -> {
//                        Vector3 cameraPosition = sceneView.getScene().getCamera().getWorldPosition();
//                        Vector3 annotationWorldPosition = annotationNode.getWorldPosition();
//                        Vector3 direction = Vector3.subtract(cameraPosition, annotationWorldPosition).normalized();
//                        annotationNode.setWorldRotation(Quaternion.lookRotation(direction, Vector3.up()));
//                    });
//                });
//    }


    // todo

    private void createAnnotation(Node parentNode, Vector3 localPosition, String labelText, String color, ModelRenderable renderable, boolean debugMode, Material yourMaterial) {
        if (debugMode) {
            // Create a small cube to visualize annotation placement
            ModelRenderable debugBox = ShapeFactory.makeCube(
                    new Vector3(0.02f, 0.02f, 0.02f),
                    Vector3.zero(),
                    yourMaterial
            );

            Node debugNode = new Node();
            debugNode.setParent(parentNode);

            Vector3 offset = new Vector3(0.1f, 0.3f, 0f);
            CollisionShape collisionShape = renderable.getCollisionShape();
            if (collisionShape instanceof Box) {
                Box boundingBox = (Box) collisionShape;
                Vector3 modelSize = boundingBox.getSize();

                // Push it slightly outwards from the model surface
                offset = new Vector3(
                        modelSize.x * 0.1f,
                        modelSize.y * 0.5f,
                        0.2f
                );
            }

            Vector3 offsetPosition = Vector3.add(localPosition, offset);
            debugNode.setLocalPosition(offsetPosition);
            debugNode.setRenderable(debugBox);

            // Billboard rotation for debug cube
            sceneView.getScene().addOnUpdateListener(frameTime -> {
                Vector3 cameraPosition = sceneView.getScene().getCamera().getWorldPosition();
                Vector3 nodePosition = debugNode.getWorldPosition();
                Vector3 direction = Vector3.subtract(cameraPosition, nodePosition).normalized();
                debugNode.setWorldRotation(Quaternion.lookRotation(direction, Vector3.up()));
            });

        } else {
            ViewRenderable.builder()
                    .setView(this, R.layout.view_annotation)
                    .build()
                    .thenAccept(viewRenderable -> {
                        Node annotationNode = new Node();
                        annotationNode.setParent(parentNode);

                        Vector3 offset = new Vector3(0.1f, 0.3f, 0f);
                        CollisionShape collisionShape = renderable.getCollisionShape();
                        if (collisionShape instanceof Box) {
                            Box boundingBox = (Box) collisionShape;
                            Vector3 modelSize = boundingBox.getSize();

                            offset = new Vector3(
                                    modelSize.x * 0.1f,
                                    modelSize.y * 0.5f,
                                    0.5f
                            );
                        }

                        Vector3 offsetPosition = Vector3.add(localPosition, offset);
                        annotationNode.setLocalPosition(offsetPosition);
                        annotationNode.setRenderable(viewRenderable);

                        // Set annotation text
                        View view = viewRenderable.getView();
                        TextView textView = view.findViewById(R.id.annotationText);
                        textView.setText(labelText);
                        try {
                            int parsedColor = Color.parseColor(color);
                            textView.setTextColor(parsedColor);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }

                        // Make annotation always face the camera
                        sceneView.getScene().addOnUpdateListener(frameTime -> {
                            Vector3 cameraPosition = sceneView.getScene().getCamera().getWorldPosition();
                            Vector3 annotationPosition = annotationNode.getWorldPosition();
                            Vector3 direction = Vector3.subtract(cameraPosition, annotationPosition).normalized();
                            annotationNode.setWorldRotation(Quaternion.lookRotation(direction, Vector3.up()));
                        });
                    });
        }
    }


//    private void createAnnotation(Node parentNode, Vector3 localPosition, String labelText, String color, ModelRenderable renderable) {
//        ViewRenderable.builder()
//                .setView(this, R.layout.view_annotation)
//                .build()
//                .thenAccept(viewRenderable -> {
//                    Node annotationNode = new Node();
//                    annotationNode.setParent(parentNode);
//
//                    // Start with local position (relative to parent)
//                    annotationNode.setLocalPosition(localPosition);
//                    annotationNode.setRenderable(viewRenderable);
//
//                    // Set the text and color
//                    View view = viewRenderable.getView();
//                    TextView textView = view.findViewById(R.id.annotationText);
//                    textView.setText(labelText);
//                    try {
//                        int parsedColor = Color.parseColor(color);
//                        textView.setTextColor(parsedColor);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    }
//
//                    // ✅ Make annotation always face the camera
//                    sceneView.getScene().addOnUpdateListener(frameTime -> {
//                        if (annotationNode.getParent() != null) {
//                            Vector3 worldPosition = annotationNode.getWorldPosition();
//                            Vector3 cameraPosition = sceneView.getScene().getCamera().getWorldPosition();
//                            Vector3 direction = Vector3.subtract(cameraPosition, worldPosition).normalized();
//                            Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
//
//                            annotationNode.setWorldRotation(lookRotation);
//                        }
//                    });
//                });
//    }


    ///


//    private void createAnnotation(Node parentNode, Vector3 localPosition, String labelText, String color, ModelRenderable renderable) {
//        ViewRenderable.builder()
//                .setView(this, R.layout.view_annotation)
//                .build()
//                .thenAccept(viewRenderable -> {
//                    Node annotationNode = new Node();
//                    annotationNode.setParent(parentNode);
//
//                    Vector3 offset = new Vector3(0.1f, 0.3f, 0f);
//                    CollisionShape collisionShape = renderable.getCollisionShape();
//                    if (collisionShape instanceof Box) {
//                        Box boundingBox = (Box) collisionShape;
//                        Vector3 modelSize = boundingBox.getSize();
//
//                        offset = new Vector3(
//                                modelSize.x * 0.1f,
//                                modelSize.y * 0.5f,
//                                0f
//                        );
//                    }
//
//                    Vector3 offsetPosition = Vector3.add(localPosition, offset);
//                    annotationNode.setLocalPosition(offsetPosition);
//                    annotationNode.setRenderable(viewRenderable);
//
//                    View view = viewRenderable.getView();
//                    TextView textView = view.findViewById(R.id.annotationText);
//                    textView.setText(labelText);
//                    try {
//                        int parsedColor = Color.parseColor(color);
//                        textView.setTextColor(parsedColor);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    }
//
//                    // Make the annotation always face the camera
//                    sceneView.getScene().addOnUpdateListener(frameTime -> {
//                        if (sceneView.getScene().getCamera() != null) {
//                            annotationNode.setLookDirection(
//                                    Vector3.subtract(
//                                            sceneView.getScene().getCamera().getWorldPosition(),
//                                            annotationNode.getWorldPosition()
//                                    )
//                            );
//                        }
//                    });
//                });
//    }


    ///


//    private void createAnnotation(Node parentNode, Vector3 localPosition, String labelText, String color, ModelRenderable renderable) {
//        ViewRenderable.builder()
//                .setView(this, R.layout.view_annotation)
//                .build()
//                .thenAccept(viewRenderable -> {
//                    Vector3 offset = new Vector3(0.1f, 0.3f, 0f);
//                    Node annotationNode = new Node();
//                    annotationNode.setParent(parentNode);
//
//                    // Offset direction: assume localPosition is center of a face
//                    Vector3 direction = localPosition.normalized();  // outward direction from model center
//                    float offsetDistance = 0.5f; // Adjust this value based on model scale
//
//                  //  Vector3 offsetPosition = Vector3.add(localPosition, direction.scaled(offsetDistance));
//
//
//                    CollisionShape collisionShape = renderable.getCollisionShape();
//                    Box boundingBox = (Box) collisionShape;
//                        Vector3 modelSize = boundingBox.getSize();
//                    offset = new Vector3(
//                               modelSize.x * 0.1f,
//                                modelSize.y * 0.5f,
//                                //modelSize.z * 0.3f,
//                                0.7f
//                        );
//                    Vector3 offsetPosition = Vector3.add(localPosition, offset);
//
//                    annotationNode.setLocalPosition(offsetPosition);
//
//                    annotationNode.setRenderable(viewRenderable);
//
//                    View view = viewRenderable.getView();
//                    TextView textView = view.findViewById(R.id.annotationText);
//                    textView.setText(labelText);
//                    try {
//                        int parsedColor = Color.parseColor(color);
//                        textView.setTextColor(parsedColor);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    }
//
//                    // Make annotation always face the camera
//                    annotationNode.setLookDirection(Vector3.forward());
//                });
//    }


    /// new working


//     private void createAnnotation(Node parentNode, Vector3 localPosition, String labelText, String color, ModelRenderable renderable) {
//         ViewRenderable.builder()
//                 .setView(this, R.layout.view_annotation)
//                 .build()
//                 .thenAccept(viewRenderable -> {
//                     Node annotationNode = new Node();
//                     annotationNode.setParent(parentNode);
//
//                     // Account for parent scale
//                     Vector3 scale = parentNode.getLocalScale();
//                     Vector3 scaledPosition = new Vector3(
//                             localPosition.x * scale.x,
//                             localPosition.y * scale.y,
//                             localPosition.z * scale.z
//                     );
//
//
//                     // Optional offset to lift it slightly (not based on model size)
//                     Vector3 offset = new Vector3(0.1f, 0.5f, 0.1f);  // small Y offset
//                     Vector3 finalPosition = Vector3.add(scaledPosition, offset);
//
//                     annotationNode.setLocalPosition(finalPosition);
//                     annotationNode.setRenderable(viewRenderable);
//
//                     View view = viewRenderable.getView();
//                     TextView textView = view.findViewById(R.id.annotationText);
//                     textView.setText(labelText);
//                     try {
//                         int parsedColor = Color.parseColor(color);
//                         textView.setTextColor(parsedColor);
//                     } catch (IllegalArgumentException e) {
//                         e.printStackTrace();
//                     }
//                 });
//     }


//    private void createAnnotation(Node parentNode, Vector3 localPosition, Vector3 normal, String labelText, String color, ModelRenderable renderable) {
//        ViewRenderable.builder()
//                .setView(this, R.layout.view_annotation)
//                .build()
//                .thenAccept(viewRenderable -> {
//                    Node annotationNode = new Node();
//                    annotationNode.setParent(parentNode);
//
//                    // Normalize the surface normal
//                    Vector3 normalizedNormal = normal.normalized();
//
//                    // Offset outward along the normal (surface direction)
//                    float offsetDistance = 0.5f; // You can tweak this
//                    Vector3 offsetPosition = Vector3.add(localPosition, normalizedNormal.scaled(offsetDistance));
//
//                    annotationNode.setLocalPosition(offsetPosition);
//                    annotationNode.setRenderable(viewRenderable);
//
//                    View view = viewRenderable.getView();
//                    TextView textView = view.findViewById(R.id.annotationText);
//                    textView.setText(labelText);
//                    try {
//                        int parsedColor = Color.parseColor(color);
//                        textView.setTextColor(parsedColor);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    }
//                });
//    }

    // new
//    private void createAnnotation(Node parentNode, Vector3 localPosition, String labelText, String color, ModelRenderable renderable) {
//        ViewRenderable.builder()
//                .setView(this, R.layout.view_annotation)
//                .build()
//                .thenAccept(viewRenderable -> {
//                    Node annotationNode = new Node();
//                    annotationNode.setParent(parentNode);
//
//                    // Step 1: Get camera world position
//                    Vector3 cameraWorldPosition = sceneView.getScene().getCamera().getWorldPosition();
//
//                    // Step 2: Get the world position of the target point on model
//                    Vector3 modelWorldPosition = Vector3.add(parentNode.getWorldPosition(), localPosition);
//
//                    // Step 3: Direction from model to camera
//                    Vector3 toCamera = Vector3.subtract(cameraWorldPosition, modelWorldPosition).normalized();
//
//                    // Step 4: Add offset in direction of camera to prevent annotation going inside model
//                    Vector3 finalWorldPosition = Vector3.add(modelWorldPosition, toCamera.scaled(0.05f)); // 5cm towards camera
//
//                    // Step 5: Convert back to local space for correct placement under modelNode
//                    Vector3 localOffsetPosition = Vector3.subtract(finalWorldPosition, parentNode.getWorldPosition());
//
//                    annotationNode.setLocalPosition(localOffsetPosition);
//                    annotationNode.setRenderable(viewRenderable);
//
//                    // Optional: make annotation face camera
//                    annotationNode.setLookDirection(toCamera.negated());
//
//                    // Set text and color
//                    View view = viewRenderable.getView();
//                    TextView textView = view.findViewById(R.id.annotationText);
//                    textView.setText(labelText);
//                    try {
//                        int parsedColor = Color.parseColor(color);
//                        textView.setTextColor(parsedColor);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    }
//                });
//    }


//    private void createAnnotation(Node parentNode, Vector3 localPosition, Vector3 normal, String labelText, String color, ModelRenderable renderable) {
//        ViewRenderable.builder()
//                .setView(this, R.layout.view_annotation)
//                .build()
//                .thenAccept(viewRenderable -> {
//                    Node annotationNode = new Node();
//                    annotationNode.setParent(parentNode);
//
//                    // Model size from collision shape
//                    Vector3 modelSize = new Vector3(1f, 1f, 1f); // default
//                    CollisionShape shape = renderable.getCollisionShape();
//                    if (shape instanceof Box) {
//                        Box box = (Box) shape;
//                        modelSize = box.getSize();
//                    }
//
//                    // Offset annotation outward using the normal direction
//                    Vector3 offset = normal.scaled(Math.max(modelSize.length() * 0.05f, 0.02f)); // 2% of model size or minimum
//
//                    Vector3 finalLocalPosition = Vector3.add(localPosition, offset);
//                    annotationNode.setLocalPosition(finalLocalPosition);
//                    annotationNode.setRenderable(viewRenderable);
//
//                    // Optional: Face the camera
//                    Vector3 toCamera = Vector3.subtract(sceneView.getScene().getCamera().getWorldPosition(),
//                            annotationNode.getWorldPosition()).normalized();
//                    annotationNode.setLookDirection(toCamera);
//
//                    // Set label text and color
//                    View view = viewRenderable.getView();
//                    TextView textView = view.findViewById(R.id.annotationText);
//                    textView.setText(labelText);
//                    try {
//                        int parsedColor = Color.parseColor(color);
//                        textView.setTextColor(parsedColor);
//                    } catch (IllegalArgumentException e) {
//                        e.printStackTrace();
//                    }
//                });
//    }


}