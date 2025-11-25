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




import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;




import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
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
    private FloatBuffer uvBuffer;
    private int vertexCount;

    private int shaderProgram;
    private int positionHandle;
    private int texCoordHandle;
    private int mvpMatrixHandle;
    private int textureHandle;

    private final Context context;
    private int[] textureIds = new int[1];

    // Optional normal storage (not used in simple shader)
    ArrayList<Float> normals = new ArrayList<>();
    ArrayList<Integer> normalIndices = new ArrayList<>();

    public ObjRenderer(Context context) {
        this.context = context;
    }

    // ---------------------------------------------------------------
    //   Load OBJ FILE + FIXED FOR MIRROR Z PROBLEM
    // ---------------------------------------------------------------
    public void loadObjModel(String objFileName) {
        try {
            InputStream inputStream = context.getAssets().open(objFileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            ArrayList<Float> vertices = new ArrayList<>();
            ArrayList<Float> uvs = new ArrayList<>();
            ArrayList<Integer> vertexIndices = new ArrayList<>();
            ArrayList<Integer> uvIndices = new ArrayList<>();
            String mtlFileName = null;

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length < 1) continue;

                switch (parts[0]) {

                    // Optional normals (OBJ may contain it)
                    case "vn":
                        normals.add(Float.parseFloat(parts[1]));
                        normals.add(Float.parseFloat(parts[2]));
                        normals.add(Float.parseFloat(parts[3]));
                        break;

                    case "v":
                        vertices.add(Float.parseFloat(parts[1]));
                        vertices.add(Float.parseFloat(parts[2]));
                        // FIX MIRROR: invert Z Axis
                        vertices.add(-Float.parseFloat(parts[3]));
                        break;

                    case "vt":
                        uvs.add(Float.parseFloat(parts[1]));
                        uvs.add(1.0f - Float.parseFloat(parts[2]));  // V flipped
                        break;

                    case "f":
                        for (int i = 1; i < parts.length; i++) {
                            String[] face = parts[i].split("/");

                            vertexIndices.add(Integer.parseInt(face[0]) - 1);
                            uvIndices.add(Integer.parseInt(face[1]) - 1);

                            if (face.length > 2 && !face[2].isEmpty()) {
                                normalIndices.add(Integer.parseInt(face[2]) - 1);
                            }
                        }
                        break;

                    case "mtllib":
                        mtlFileName = parts[1];
                        break;
                }
            }
            reader.close();

            // Build arrays
            float[] vertexArray = new float[vertexIndices.size() * 3];
            float[] uvArray = new float[uvIndices.size() * 2];

            for (int i = 0; i < vertexIndices.size(); i++) {
                int vi = vertexIndices.get(i) * 3;
                vertexArray[i * 3] = vertices.get(vi);
                vertexArray[i * 3 + 1] = vertices.get(vi + 1);
                vertexArray[i * 3 + 2] = vertices.get(vi + 2);

                int ui = uvIndices.get(i) * 2;
                uvArray[i * 2] = uvs.get(ui);
                uvArray[i * 2 + 1] = uvs.get(ui + 1);
            }

            vertexCount = vertexIndices.size();

            vertexBuffer = ByteBuffer.allocateDirect(vertexArray.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            vertexBuffer.put(vertexArray).position(0);

            uvBuffer = ByteBuffer.allocateDirect(uvArray.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            uvBuffer.put(uvArray).position(0);

            setupShaderProgram();

            if (mtlFileName != null) {
                parseMTLAndLoadTexture(mtlFileName);
            }

            Log.d(TAG, "✅ OBJ Loaded: " + objFileName + " (" + vertexCount + " vertices)");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading OBJ: " + e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    //   PARSE MTL & LOAD TEXTURE
    // ---------------------------------------------------------------
    private void parseMTLAndLoadTexture(String mtlFileName) {
        try {
            InputStream inputStream = context.getAssets().open(mtlFileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            String textureFile = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                Log.d(TAG, "MTL: " + line);

                if (line.startsWith("map_Kd")) {
                    String[] tokens = line.split("\\s+");
                    if (tokens.length >= 2) {
                        textureFile = tokens[1];
                    }
                }
            }
            reader.close();

            if (textureFile == null) {
                Log.w(TAG, "⚠ No texture found in MTL file!");
                return;
            }

            Log.d(TAG, "Loading texture: " + textureFile);

            Bitmap bitmap = BitmapFactory.decodeStream(context.getAssets().open(textureFile));

            GLES20.glGenTextures(1, textureIds, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            bitmap.recycle();
            textureHandle = textureIds[0];

            Log.d(TAG, "✅ Texture Loaded OK, handle=" + textureHandle);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading MTL texture: " + e, e);
        }
    }

    // ---------------------------------------------------------------
    //   SHADERS
    // ---------------------------------------------------------------
    private void setupShaderProgram() {

        String vertexShaderCode =
                "attribute vec4 vPosition;" +
                        "attribute vec2 aTexCoord;" +
                        "uniform mat4 uMVPMatrix;" +
                        "varying vec2 vTexCoord;" +
                        "void main() {" +
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "  vTexCoord = aTexCoord;" +
                        "}";

        String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform sampler2D uTexture;" +
                        "varying vec2 vTexCoord;" +
                        "void main() {" +
                        "  gl_FragColor = texture2D(uTexture, vTexCoord);" +
                        "}";

        int vs = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vs);
        GLES20.glAttachShader(shaderProgram, fs);
        GLES20.glLinkProgram(shaderProgram);
    }

    private int loadShader(int type, String code) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);
        return shader;
    }

    // ---------------------------------------------------------------
    //   DRAW
    // ---------------------------------------------------------------
    public void draw(float[] mvpMatrix) {
        if (shaderProgram == 0 || vertexBuffer == null || uvBuffer == null)
            return;

        GLES20.glUseProgram(shaderProgram);

        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
        GLES20.glEnableVertexAttribArray(texCoordHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);

        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        int texUniform = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
        GLES20.glUniform1i(texUniform, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }
}



   // todo 90%

//public class ObjRenderer {
//
//    private static final String TAG = "ObjRenderer";
//
//    private FloatBuffer vertexBuffer;
//    private FloatBuffer uvBuffer;
//    private int vertexCount;
//
//    private int shaderProgram;
//    private int positionHandle;
//    private int texCoordHandle;
//    private int mvpMatrixHandle;
//    private int textureHandle;
//
//    private final Context context;
//    private int[] textureIds = new int[1];
//    ArrayList<Float> normals = new ArrayList<>();
//    ArrayList<Integer> normalIndices = new ArrayList<>();
//
//
//    public ObjRenderer(Context context) {
//        this.context = context;
//    }
//
//    public void loadObjModel(String objFileName) {
//        try {
//            InputStream inputStream = context.getAssets().open(objFileName);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            ArrayList<Float> vertices = new ArrayList<>();
//            ArrayList<Float> uvs = new ArrayList<>();
//            ArrayList<Integer> vertexIndices = new ArrayList<>();
//            ArrayList<Integer> uvIndices = new ArrayList<>();
//            String mtlFileName = null;
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                String[] parts = line.trim().split("\\s+");
//                if (parts.length < 1) continue;
//
//                switch (parts[0]) {
//
//
//                    case "v":
//                        vertices.add(Float.parseFloat(parts[1]));
//                        vertices.add(Float.parseFloat(parts[2]));
//                        vertices.add(Float.parseFloat(parts[3]));
//                        break;
//                    case "vt":
//                        uvs.add(Float.parseFloat(parts[1]));
//                        uvs.add(1.0f - Float.parseFloat(parts[2])); // flip V
//                        break;
//                    case "f":
//                        for (int i = 1; i < parts.length; i++) {
//                            String[] faceParts = parts[i].split("/");
//                            vertexIndices.add(Integer.parseInt(faceParts[0]) - 1);
//                            uvIndices.add(Integer.parseInt(faceParts[1]) - 1);
//                        }
//                        break;
//                    case "mtllib":
//                        mtlFileName = parts[1];
//                        break;
//                }
//            }
//            reader.close();
//
//            // Build buffers
//            float[] vertexArray = new float[vertexIndices.size() * 3];
//            float[] uvArray = new float[uvIndices.size() * 2];
//
//            for (int i = 0; i < vertexIndices.size(); i++) {
//                int vi = vertexIndices.get(i) * 3;
//                vertexArray[i * 3] = vertices.get(vi);
//                vertexArray[i * 3 + 1] = vertices.get(vi + 1);
//                vertexArray[i * 3 + 2] = vertices.get(vi + 2);
//
//                int ui = uvIndices.get(i) * 2;
//                uvArray[i * 2] = uvs.get(ui);
//                uvArray[i * 2 + 1] = uvs.get(ui + 1);
//            }
//
//            vertexCount = vertexIndices.size();
//
//            vertexBuffer = ByteBuffer.allocateDirect(vertexArray.length * 4)
//                    .order(ByteOrder.nativeOrder())
//                    .asFloatBuffer();
//            vertexBuffer.put(vertexArray).position(0);
//
//            uvBuffer = ByteBuffer.allocateDirect(uvArray.length * 4)
//                    .order(ByteOrder.nativeOrder())
//                    .asFloatBuffer();
//            uvBuffer.put(uvArray).position(0);
//
//            setupShaderProgram();
//
//            // Load texture from MTL
//            if (mtlFileName != null) {
//                parseMTLAndLoadTexture(mtlFileName);
//            }
//
//            Log.d(TAG, "✅ OBJ Loaded: " + objFileName + " (" + vertexCount + " vertices)");
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Error loading OBJ: " + e.getMessage(), e);
//        }
//    }
//
//
//    private void parseMTLAndLoadTexture(String mtlFileName) {
//        try {
//            InputStream inputStream = context.getAssets().open(mtlFileName);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            String line;
//            String textureFile = null;
//
//            while ((line = reader.readLine()) != null) {
//                line = line.trim();   // <<< IMPORTANT FIX
//                Log.d(TAG, "MTL: " + line);
//
//                if (line.startsWith("map_Kd")) {
//                    String[] tokens = line.split("\\s+");
//                    if (tokens.length >= 2) {
//                        textureFile = tokens[1];
//                    }
//                }
//            }
//            reader.close();
//
//            if (textureFile == null) {
//                Log.w(TAG, "⚠ No texture found in MTL file!");
//                return;
//            }
//
//            Log.d(TAG, "Loading texture: " + textureFile);
//
//            Bitmap bitmap = BitmapFactory.decodeStream(context.getAssets().open(textureFile));
//
//            GLES20.glGenTextures(1, textureIds, 0);
//            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
//
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//
//            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//
//            bitmap.recycle();
//            textureHandle = textureIds[0];
//
//            Log.d(TAG, "✅ Texture Loaded OK, handle=" + textureHandle);
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Error loading MTL texture: " + e, e);
//        }
//    }
//
//    private void setupShaderProgram() {
//
//        String vertexShaderCode =
//                "attribute vec4 vPosition;" +
//                        "attribute vec2 aTexCoord;" +
//                        "uniform mat4 uMVPMatrix;" +
//                        "varying vec2 vTexCoord;" +
//                        "void main() {" +
//                        "  gl_Position = uMVPMatrix * vPosition;" +
//                        "  vTexCoord = aTexCoord;" +
//                        "}";
//
//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 vTexCoord;" +
//                        "void main() {" +
//                        "  gl_FragColor = texture2D(uTexture, vTexCoord);" +
//                        "}";
//
//        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
//        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
//
//        shaderProgram = GLES20.glCreateProgram();
//        GLES20.glAttachShader(shaderProgram, vertexShader);
//        GLES20.glAttachShader(shaderProgram, fragmentShader);
//        GLES20.glLinkProgram(shaderProgram);
//    }
//
//    private int loadShader(int type, String code) {
//        int shader = GLES20.glCreateShader(type);
//        GLES20.glShaderSource(shader, code);
//        GLES20.glCompileShader(shader);
//        return shader;
//    }
//
//    public void draw(float[] mvpMatrix) {
//        if (shaderProgram == 0 || vertexBuffer == null || uvBuffer == null) return;
//
//        GLES20.glUseProgram(shaderProgram);
//
//        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
//        GLES20.glEnableVertexAttribArray(positionHandle);
//        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
//
//        texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
//        GLES20.glEnableVertexAttribArray(texCoordHandle);
//        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);
//
//        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
//        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
//        int texUniformHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
//        GLES20.glUniform1i(texUniformHandle, 0);
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
//
//        GLES20.glDisableVertexAttribArray(positionHandle);
//        GLES20.glDisableVertexAttribArray(texCoordHandle);
//    }
//}





//
//public class ObjRenderer {
//
//    private static final String TAG = "ObjRenderer";
//
//    private FloatBuffer vertexBuffer;
//    private FloatBuffer uvBuffer;
//    private int vertexCount;
//
//    private int shaderProgram;
//    private int positionHandle;
//    private int texCoordHandle;
//    private int mvpMatrixHandle;
//    private int textureHandle;
//
//    private final Context context;
//
//    private int[] textureIds = new int[1];
//
//    public ObjRenderer(Context context) {
//        this.context = context;
//    }
//
//    public void loadObjModel(String objFileName) {
//        try {
//            InputStream inputStream = context.getAssets().open(objFileName);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            ArrayList<Float> vertices = new ArrayList<>();
//            ArrayList<Float> uvs = new ArrayList<>();
//            ArrayList<Integer> vertexIndices = new ArrayList<>();
//            ArrayList<Integer> uvIndices = new ArrayList<>();
//
//            String mtlFileName = null;
//            String line;
//            while ((line = reader.readLine()) != null) {
//                String[] parts = line.trim().split("\\s+");
//                if (parts.length < 1) continue;
//
//                switch (parts[0]) {
//                    case "v":
//                        vertices.add(Float.parseFloat(parts[1]));
//                        vertices.add(Float.parseFloat(parts[2]));
//                        vertices.add(Float.parseFloat(parts[3]));
//                        break;
//                    case "vt":
//                        uvs.add(Float.parseFloat(parts[1]));
//                        uvs.add(1.0f - Float.parseFloat(parts[2])); // flip V
//                        break;
//                    case "f":
//                        for (int i = 1; i < parts.length; i++) {
//                            String[] faceParts = parts[i].split("/");
//                            vertexIndices.add(Integer.parseInt(faceParts[0]) - 1);
//                            uvIndices.add(Integer.parseInt(faceParts[1]) - 1);
//                        }
//                        break;
//                    case "mtllib":
//                        mtlFileName = parts[1];
//                        break;
//                }
//            }
//            reader.close();
//
//            float[] vertexArray = new float[vertexIndices.size() * 3];
//            float[] uvArray = new float[uvIndices.size() * 2];
//
//            for (int i = 0; i < vertexIndices.size(); i++) {
//                int vi = vertexIndices.get(i) * 3;
//                vertexArray[i * 3] = vertices.get(vi);
//                vertexArray[i * 3 + 1] = vertices.get(vi + 1);
//                vertexArray[i * 3 + 2] = vertices.get(vi + 2);
//
//                int ui = uvIndices.get(i) * 2;
//                uvArray[i * 2] = uvs.get(ui);
//                uvArray[i * 2 + 1] = uvs.get(ui + 1);
//            }
//
//            vertexCount = vertexIndices.size();
//
//            vertexBuffer = ByteBuffer.allocateDirect(vertexArray.length * 4)
//                    .order(ByteOrder.nativeOrder())
//                    .asFloatBuffer();
//            vertexBuffer.put(vertexArray).position(0);
//
//            uvBuffer = ByteBuffer.allocateDirect(uvArray.length * 4)
//                    .order(ByteOrder.nativeOrder())
//                    .asFloatBuffer();
//            uvBuffer.put(uvArray).position(0);
//
//            setupShaderProgram();
//
//            if (mtlFileName != null) parseMTLAndLoadTexture(mtlFileName);
//
//            Log.d(TAG, "✅ OBJ Loaded: " + objFileName + " (" + vertexCount + " vertices)");
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Error loading OBJ: " + e.getMessage(), e);
//        }
//    }
//
//    private void parseMTLAndLoadTexture(String mtlFileName) {
//        try {
//            InputStream inputStream = context.getAssets().open(mtlFileName);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//            String line;
//            String textureFile = null;
//
//            while ((line = reader.readLine()) != null) {
//                if (line.startsWith("map_Kd")) {
//                    textureFile = line.split("\\s+")[1];
//                    break;
//                }
//            }
//            reader.close();
//
//            if (textureFile != null) {
//                Bitmap bitmap = BitmapFactory.decodeStream(context.getAssets().open(textureFile));
//                GLES20.glGenTextures(1, textureIds, 0);
//                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
//                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
//                bitmap.recycle();
//                textureHandle = textureIds[0];
//            }
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Error loading MTL texture: " + e.getMessage(), e);
//        }
//    }
//
//    private void setupShaderProgram() {
//        String vertexShaderCode =
//                "attribute vec4 vPosition;" +
//                        "attribute vec2 aTexCoord;" +
//                        "uniform mat4 uMVPMatrix;" +
//                        "varying vec2 vTexCoord;" +
//                        "void main() {" +
//                        "  gl_Position = uMVPMatrix * vPosition;" +
//                        "  vTexCoord = aTexCoord;" +
//                        "}";
//
//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "uniform sampler2D uTexture;" +
//                        "varying vec2 vTexCoord;" +
//                        "void main() {" +
//                        "  gl_FragColor = texture2D(uTexture, vTexCoord);" +
//                        "}";
//
//        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
//        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
//
//        shaderProgram = GLES20.glCreateProgram();
//        GLES20.glAttachShader(shaderProgram, vertexShader);
//        GLES20.glAttachShader(shaderProgram, fragmentShader);
//        GLES20.glLinkProgram(shaderProgram);
//    }
//
//    private int loadShader(int type, String code) {
//        int shader = GLES20.glCreateShader(type);
//        GLES20.glShaderSource(shader, code);
//        GLES20.glCompileShader(shader);
//        return shader;
//    }
//
//    public void draw(float[] mvpMatrix) {
//        if (shaderProgram == 0 || vertexBuffer == null || uvBuffer == null) return;
//
//        GLES20.glUseProgram(shaderProgram);
//
//        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
//        GLES20.glEnableVertexAttribArray(positionHandle);
//        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
//
//        texCoordHandle = GLES20.glGetAttribLocation(shaderProgram, "aTexCoord");
//        GLES20.glEnableVertexAttribArray(texCoordHandle);
//        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, uvBuffer);
//
//        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
//        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
//        int texUniformHandle = GLES20.glGetUniformLocation(shaderProgram, "uTexture");
//        GLES20.glUniform1i(texUniformHandle, 0);
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
//
//        GLES20.glDisableVertexAttribArray(positionHandle);
//        GLES20.glDisableVertexAttribArray(texCoordHandle);
//    }
//}











// TODO working 80



//
//
//public class ObjRenderer {
//
//    private static final String TAG = "ObjRenderer";
//
//    private FloatBuffer vertexBuffer;
//    private int vertexCount;
//
//    private int shaderProgram;
//    private int positionHandle;
//    private int mvpMatrixHandle;
//
//    private final float[] modelMatrix = new float[16];
//    private final Context context;
//
//    public ObjRenderer(Context context) {
//        this.context = context;
//        Matrix.setIdentityM(modelMatrix, 0);
//    }
//
//    public void loadObjModel(String objFileName) {
//        try {
//            InputStream inputStream = context.getAssets().open(objFileName);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            ArrayList<Float> vertices = new ArrayList<>();
//            ArrayList<Integer> indices = new ArrayList<>();
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                String[] parts = line.trim().split("\\s+");
//                if (parts.length < 1) continue;
//
//                switch (parts[0]) {
//                    case "v": // vertex
//                        vertices.add(Float.parseFloat(parts[1]));
//                        vertices.add(Float.parseFloat(parts[2]));
//                        vertices.add(Float.parseFloat(parts[3]));
//                        break;
//
//                    case "f": // face
//                        for (int i = 1; i < parts.length; i++) {
//                            String[] faceParts = parts[i].split("/");
//                            indices.add(Integer.parseInt(faceParts[0]) - 1);
//                        }
//                        break;
//                }
//            }
//            reader.close();
//
//            float[] vertexArray = new float[indices.size() * 3];
//            for (int i = 0; i < indices.size(); i++) {
//                int idx = indices.get(i) * 3;
//                vertexArray[i * 3] = vertices.get(idx);
//                vertexArray[i * 3 + 1] = vertices.get(idx + 1);
//                vertexArray[i * 3 + 2] = vertices.get(idx + 2);
//            }
//
//            vertexCount = indices.size();
//
//            ByteBuffer vb = ByteBuffer.allocateDirect(vertexArray.length * 4);
//            vb.order(ByteOrder.nativeOrder());
//            vertexBuffer = vb.asFloatBuffer();
//            vertexBuffer.put(vertexArray);
//            vertexBuffer.position(0);
//
//            setupShaderProgram();
//
//            Log.d(TAG, "✅ OBJ Loaded: " + objFileName + " (" + vertexCount + " vertices)");
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Error loading OBJ: " + e.getMessage(), e);
//        }
//    }
//
//    private void setupShaderProgram() {
//        String vertexShaderCode =
//                "attribute vec4 vPosition;" +
//                        "uniform mat4 uMVPMatrix;" +
//                        "void main() {" +
//                        "  gl_Position = uMVPMatrix * vPosition;" +
//                        "}";
//
//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "void main() {" +
//                        "  gl_FragColor = vec4(0.8, 0.8, 0.9, 1.0);" +
//                        "}";
//
//        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
//        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
//
//        shaderProgram = GLES20.glCreateProgram();
//        GLES20.glAttachShader(shaderProgram, vertexShader);
//        GLES20.glAttachShader(shaderProgram, fragmentShader);
//        GLES20.glLinkProgram(shaderProgram);
//    }
//
//    private int loadShader(int type, String code) {
//        int shader = GLES20.glCreateShader(type);
//        GLES20.glShaderSource(shader, code);
//        GLES20.glCompileShader(shader);
//        return shader;
//    }
//
//    public void draw(float[] mvpMatrix) {
//        if (shaderProgram == 0 || vertexBuffer == null) return;
//
//        GLES20.glUseProgram(shaderProgram);
//
//        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
//        GLES20.glEnableVertexAttribArray(positionHandle);
//        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
//
//        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
//        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
//
//        GLES20.glDisableVertexAttribArray(positionHandle);
//    }
//}






//public class ObjRenderer {
//
//    private static final String TAG = "ObjRenderer";
//
//    private FloatBuffer vertexBuffer;
//    private int vertexCount;
//
//    private int shaderProgram;
//    private int positionHandle;
//    private int mvpMatrixHandle;
//
//    private float[] modelMatrix = new float[16];
//    private final Context context;
//
//    public ObjRenderer(Context context) {
//        this.context = context;
//        Matrix.setIdentityM(modelMatrix, 0);
//    }
//
//    public void loadObjModel(String objFileName) {
//        try {
//            InputStream inputStream = context.getAssets().open(objFileName);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            ArrayList<Float> vertices = new ArrayList<>();
//            ArrayList<Integer> indices = new ArrayList<>();
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                String[] parts = line.trim().split("\\s+");
//                if (parts.length == 0) continue;
//
//                switch (parts[0]) {
//                    case "v": // Vertex position
//                        vertices.add(Float.parseFloat(parts[1]));
//                        vertices.add(Float.parseFloat(parts[2]));
//                        vertices.add(Float.parseFloat(parts[3]));
//                        break;
//
//                    case "f": // Face (triangle)
//                        for (int i = 1; i < parts.length; i++) {
//                            String[] idx = parts[i].split("/");
//                            indices.add(Integer.parseInt(idx[0]) - 1);
//                        }
//                        break;
//                }
//            }
//            reader.close();
//
//            float[] vertexArray = new float[indices.size() * 3];
//            for (int i = 0; i < indices.size(); i++) {
//                int idx = indices.get(i) * 3;
//                vertexArray[i * 3] = vertices.get(idx);
//                vertexArray[i * 3 + 1] = vertices.get(idx + 1);
//                vertexArray[i * 3 + 2] = vertices.get(idx + 2);
//            }
//
//            vertexCount = indices.size();
//
//            ByteBuffer vb = ByteBuffer.allocateDirect(vertexArray.length * 4);
//            vb.order(ByteOrder.nativeOrder());
//            vertexBuffer = vb.asFloatBuffer();
//            vertexBuffer.put(vertexArray);
//            vertexBuffer.position(0);
//
//            setupShaderProgram();
//
//            Log.d(TAG, "Loaded OBJ Model: " + objFileName + " (" + vertexCount + " vertices)");
//
//        } catch (Exception e) {
//            Log.e(TAG, "Error loading OBJ: " + e.getMessage());
//        }
//    }
//
//    private void setupShaderProgram() {
//        String vertexShaderCode =
//                "attribute vec4 vPosition;" +
//                        "uniform mat4 uMVPMatrix;" +
//                        "void main() {" +
//                        "  gl_Position = uMVPMatrix * vPosition;" +
//                        "}";
//
//        String fragmentShaderCode =
//                "precision mediump float;" +
//                        "void main() {" +
//                        "  gl_FragColor = vec4(0.2, 0.7, 0.9, 1.0);" +
//                        "}";
//
//        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
//        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
//
//        shaderProgram = GLES20.glCreateProgram();
//        GLES20.glAttachShader(shaderProgram, vertexShader);
//        GLES20.glAttachShader(shaderProgram, fragmentShader);
//        GLES20.glLinkProgram(shaderProgram);
//    }
//
//    private int loadShader(int type, String shaderCode) {
//        int shader = GLES20.glCreateShader(type);
//        GLES20.glShaderSource(shader, shaderCode);
//        GLES20.glCompileShader(shader);
//        return shader;
//    }
//
//    public void draw(float[] vpMatrix) {
//        if (shaderProgram == 0 || vertexBuffer == null) return;
//
//        GLES20.glUseProgram(shaderProgram);
//
//        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
//        GLES20.glEnableVertexAttribArray(positionHandle);
//        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
//
//        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
//        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, vpMatrix, 0);
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
//        GLES20.glDisableVertexAttribArray(positionHandle);
//    }
//}




















//
//import android.content.Context;
//import android.opengl.GLES20;
//import android.opengl.Matrix;
//import android.util.Log;
//
//import java.io.BufferedReader;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.nio.ByteBuffer;
//import java.nio.ByteOrder;
//import java.nio.FloatBuffer;
//import java.util.ArrayList;
//
//public class ObjRenderer {
//    private static final String TAG = "ObjRenderer";
//
//    private FloatBuffer vertexBuffer;
//    private FloatBuffer normalBuffer;
//    private FloatBuffer textureBuffer;
//    private int vertexCount;
//
//    private int shaderProgram;
//    private int positionHandle;
//    private int normalHandle;
//    private int textureHandle;
//    private int mvpMatrixHandle;
//
//    private float[] modelMatrix = new float[16];
//    private Context context;
//
//    public ObjRenderer() {
//        Matrix.setIdentityM(modelMatrix, 0);
//    }
//    public ObjRenderer(Context context) {
//        this.context = context;
//        Matrix.setIdentityM(modelMatrix, 0);
//    }
//
//    public void loadObjModel( String objFileName) {
//        try {
//            InputStream inputStream = context.getAssets().open(objFileName);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            ArrayList<Float> vertices = new ArrayList<>();
//            ArrayList<Float> normals = new ArrayList<>();
//            ArrayList<Float> textures = new ArrayList<>();
//            ArrayList<Integer> indices = new ArrayList<>();
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                String[] parts = line.split("\\s+");
//                if (parts.length == 0) continue;
//
//                switch (parts[0]) {
//                    case "v": // Vertex position
//                        vertices.add(Float.parseFloat(parts[1]));
//                        vertices.add(Float.parseFloat(parts[2]));
//                        vertices.add(Float.parseFloat(parts[3]));
//                        break;
//                    case "vn": // Vertex normal
//                        normals.add(Float.parseFloat(parts[1]));
//                        normals.add(Float.parseFloat(parts[2]));
//                        normals.add(Float.parseFloat(parts[3]));
//                        break;
//                    case "vt": // Texture coordinate
//                        textures.add(Float.parseFloat(parts[1]));
//                        textures.add(Float.parseFloat(parts[2]));
//                        break;
//                    case "f": // Face
//                        for (int i = 1; i < parts.length; i++) {
//                            String[] indicesStr = parts[i].split("/");
//                            indices.add(Integer.parseInt(indicesStr[0]) - 1);
//                        }
//                        break;
//                }
//            }
//            reader.close();
//
//            // Convert ArrayList to float array
//            float[] vertexArray = new float[indices.size() * 3];
//            for (int i = 0; i < indices.size(); i++) {
//                int idx = indices.get(i) * 3;
//                vertexArray[i * 3] = vertices.get(idx);
//                vertexArray[i * 3 + 1] = vertices.get(idx + 1);
//                vertexArray[i * 3 + 2] = vertices.get(idx + 2);
//            }
//
//            vertexCount = indices.size();
//
//            // Allocate buffers
//            ByteBuffer vb = ByteBuffer.allocateDirect(vertexArray.length * 4);
//            vb.order(ByteOrder.nativeOrder());
//            vertexBuffer = vb.asFloatBuffer();
//            vertexBuffer.put(vertexArray);
//            vertexBuffer.position(0);
//
//            Log.d(TAG, "Loaded OBJ Model: " + objFileName);
//
//        } catch (Exception e) {
//            Log.e(TAG, "Error loading OBJ file: " + e.getMessage());
//        }
//    }
//
//    public void draw(float[] vpMatrix) {
//        GLES20.glUseProgram(shaderProgram);
//
//        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
//        GLES20.glEnableVertexAttribArray(positionHandle);
//        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
//
//        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");
//        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, vpMatrix, 0);
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
//        GLES20.glDisableVertexAttribArray(positionHandle);
//    }
//}
//
