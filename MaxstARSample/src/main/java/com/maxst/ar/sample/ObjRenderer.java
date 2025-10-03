package com.maxst.ar.sample;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class ObjRenderer {
    private static final String TAG = "ObjRenderer";

    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer textureBuffer;
    private int vertexCount;

    private int shaderProgram;
    private int positionHandle;
    private int normalHandle;
    private int textureHandle;
    private int mvpMatrixHandle;

    private float[] modelMatrix = new float[16];
    private Context context;

    public ObjRenderer() {
        Matrix.setIdentityM(modelMatrix, 0);
    }
    public ObjRenderer(Context context) {
        this.context = context;
        Matrix.setIdentityM(modelMatrix, 0);
    }

    public void loadObjModel( String objFileName) {
        try {
            InputStream inputStream = context.getAssets().open(objFileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            ArrayList<Float> vertices = new ArrayList<>();
            ArrayList<Float> normals = new ArrayList<>();
            ArrayList<Float> textures = new ArrayList<>();
            ArrayList<Integer> indices = new ArrayList<>();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length == 0) continue;

                switch (parts[0]) {
                    case "v": // Vertex position
                        vertices.add(Float.parseFloat(parts[1]));
                        vertices.add(Float.parseFloat(parts[2]));
                        vertices.add(Float.parseFloat(parts[3]));
                        break;
                    case "vn": // Vertex normal
                        normals.add(Float.parseFloat(parts[1]));
                        normals.add(Float.parseFloat(parts[2]));
                        normals.add(Float.parseFloat(parts[3]));
                        break;
                    case "vt": // Texture coordinate
                        textures.add(Float.parseFloat(parts[1]));
                        textures.add(Float.parseFloat(parts[2]));
                        break;
                    case "f": // Face
                        for (int i = 1; i < parts.length; i++) {
                            String[] indicesStr = parts[i].split("/");
                            indices.add(Integer.parseInt(indicesStr[0]) - 1);
                        }
                        break;
                }
            }
            reader.close();

            // Convert ArrayList to float array
            float[] vertexArray = new float[indices.size() * 3];
            for (int i = 0; i < indices.size(); i++) {
                int idx = indices.get(i) * 3;
                vertexArray[i * 3] = vertices.get(idx);
                vertexArray[i * 3 + 1] = vertices.get(idx + 1);
                vertexArray[i * 3 + 2] = vertices.get(idx + 2);
            }

            vertexCount = indices.size();

            // Allocate buffers
            ByteBuffer vb = ByteBuffer.allocateDirect(vertexArray.length * 4);
            vb.order(ByteOrder.nativeOrder());
            vertexBuffer = vb.asFloatBuffer();
            vertexBuffer.put(vertexArray);
            vertexBuffer.position(0);

            Log.d(TAG, "Loaded OBJ Model: " + objFileName);

        } catch (Exception e) {
            Log.e(TAG, "Error loading OBJ file: " + e.getMessage());
        }
    }

    public void draw(float[] vpMatrix) {
        GLES20.glUseProgram(shaderProgram);

        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, vpMatrix, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        GLES20.glDisableVertexAttribArray(positionHandle);
    }
}

