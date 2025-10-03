package com.maxst.ar.sample;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import com.maxst.ar.TrackedImage;

import java.io.IOException;
import java.io.InputStream;
import com.maxst.ar.sample.arobject.BaseRenderer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TexturedCubeRendererNew {
/*
    private ObjModelLoader objModelLoader;
    private String modelFileName;
    private Context context;

    public TexturedCubeRendererNew(Context context, String modelFileName) {
        this.context = context;
        this.modelFileName = modelFileName;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        objModelLoader = new ObjModelLoader(context);
        objModelLoader.loadModel(modelFileName); // Load the 3D model file
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        Renderer.getInstance().begin(0.0f, 0.0f, 0.0f, 1.0f);
        objModelLoader.draw();
        Renderer.getInstance().end();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }*/
}
