package com.maxst.ar.sample.arobject;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.maxst.ar.sample.util.ShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class TexturedCylinderRenderer extends BaseRenderer {

    private static final String VERTEX_SHADER_SRC =
            "attribute vec4 a_position;\n" +
                    "attribute vec2 a_texCoord;\n" +
                    "varying vec2 v_texCoord;\n" +
                    "uniform mat4 u_mvpMatrix;\n" +
                    "void main() {\n" +
                    "  gl_Position = u_mvpMatrix * a_position;\n" +
                    "  v_texCoord = a_texCoord;\n" +
                    "}";

    private static final String FRAGMENT_SHADER_SRC =
            "precision mediump float;\n" +
                    "varying vec2 v_texCoord;\n" +
                    "uniform sampler2D u_texture;\n" +
                    "void main() {\n" +
                    "  gl_FragColor = texture2D(u_texture, v_texCoord);\n" +
                    "}";

    private FloatBuffer sideVertexBuffer, sideTexBuffer;
    private FloatBuffer topVertexBuffer, topTexBuffer;
    private FloatBuffer bottomVertexBuffer, bottomTexBuffer;

    private int sideVertexCount;
    private int topVertexCount;
    private int bottomVertexCount;

    private int shaderProgramId;
    private int positionHandle, textureCoordHandle, mvpMatrixHandle, textureHandle;
    private int[] textureNames;

    public TexturedCylinderRenderer(Context context, int slices, float radius, float height) {
        super();

        List<Float> sideVerts = new ArrayList<>();
        List<Float> sideTex = new ArrayList<>();

        List<Float> topVerts = new ArrayList<>();
        List<Float> topTex = new ArrayList<>();

        List<Float> bottomVerts = new ArrayList<>();
        List<Float> bottomTex = new ArrayList<>();

        float halfHeight = height / 2f;

        // -----------------------
        // Build side surface
        // -----------------------
        for (int i = 0; i <= slices; i++) {
            float theta = (float) (2.0 * Math.PI * i / slices);
            float x = (float) Math.cos(theta);
            float z = (float) Math.sin(theta);

            // Bottom
            sideVerts.add(x * radius);
            sideVerts.add(-halfHeight);
            sideVerts.add(z * radius);
            sideTex.add((float) i / slices);
            sideTex.add(1.0f);

            // Top
            sideVerts.add(x * radius);
            sideVerts.add(halfHeight);
            sideVerts.add(z * radius);
            sideTex.add((float) i / slices);
            sideTex.add(0.0f);
        }

        sideVertexCount = sideVerts.size() / 3;
        sideVertexBuffer = createFloatBuffer(sideVerts);
        sideTexBuffer = createFloatBuffer(sideTex);

        // -----------------------
        // Build top cap
        // -----------------------
        topVerts.add(0f); topVerts.add(halfHeight); topVerts.add(0f);
        topTex.add(0.5f); topTex.add(0.5f);
        for (int i = 0; i <= slices; i++) {
            float theta = (float) (2.0 * Math.PI * i / slices);
            float x = (float) Math.cos(theta);
            float z = (float) Math.sin(theta);
            topVerts.add(x * radius);
            topVerts.add(halfHeight);
            topVerts.add(z * radius);
            topTex.add(0.5f + 0.5f * x);
            topTex.add(0.5f - 0.5f * z);
        }

        topVertexCount = topVerts.size() / 3;
        topVertexBuffer = createFloatBuffer(topVerts);
        topTexBuffer = createFloatBuffer(topTex);

        // -----------------------
        // Build bottom cap
        // -----------------------
        bottomVerts.add(0f); bottomVerts.add(-halfHeight); bottomVerts.add(0f);
        bottomTex.add(0.5f); bottomTex.add(0.5f);
        for (int i = 0; i <= slices; i++) {
            float theta = (float) (-2.0 * Math.PI * i / slices); // reverse winding
            float x = (float) Math.cos(theta);
            float z = (float) Math.sin(theta);
            bottomVerts.add(x * radius);
            bottomVerts.add(-halfHeight);
            bottomVerts.add(z * radius);
            bottomTex.add(0.5f + 0.5f * x);
            bottomTex.add(0.5f - 0.5f * z);
        }

        bottomVertexCount = bottomVerts.size() / 3;
        bottomVertexBuffer = createFloatBuffer(bottomVerts);
        bottomTexBuffer = createFloatBuffer(bottomTex);

        // -----------------------
        // Shader setup
        // -----------------------
        shaderProgramId = ShaderUtil.createProgram(VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
        positionHandle = GLES20.glGetAttribLocation(shaderProgramId, "a_position");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramId, "a_texCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramId, "u_mvpMatrix");
        textureHandle = GLES20.glGetUniformLocation(shaderProgramId, "u_texture");

        // Texture setup
        textureNames = new int[1];
        GLES20.glGenTextures(1, textureNames, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    @Override
    public void draw() {
        GLES20.glUseProgram(shaderProgramId);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
        GLES20.glUniform1i(textureHandle, 0);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.multiplyMM(modelMatrix, 0, translation, 0, rotation, 0);
        Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, scale, 0);
        Matrix.multiplyMM(modelMatrix, 0, transform, 0, modelMatrix, 0);
        Matrix.multiplyMM(localMvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0);
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, localMvpMatrix, 0);

        drawPart(sideVertexBuffer, sideTexBuffer, sideVertexCount, GLES20.GL_TRIANGLE_STRIP);
        drawPart(topVertexBuffer, topTexBuffer, topVertexCount, GLES20.GL_TRIANGLE_FAN);
        drawPart(bottomVertexBuffer, bottomTexBuffer, bottomVertexCount, GLES20.GL_TRIANGLE_FAN);
    }

    private void drawPart(FloatBuffer vBuf, FloatBuffer tBuf, int count, int mode) {
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vBuf);

        GLES20.glEnableVertexAttribArray(textureCoordHandle);
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, tBuf);

        GLES20.glDrawArrays(mode, 0, count);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
    }

    public void setTextureBitmap(Bitmap texture) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0);
    }

    private FloatBuffer createFloatBuffer(List<Float> list) {
        ByteBuffer bb = ByteBuffer.allocateDirect(list.size() * 4).order(ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        for (Float f : list) buffer.put(f);
        buffer.position(0);
        return buffer;
    }
}




// todo old


//public class TexturedCylinderRenderer extends BaseRenderer {
//
//    private static final String VERTEX_SHADER_SRC =
//            "attribute vec4 a_position;\n" +
//                    "attribute vec2 a_texCoord;\n" +
//                    "varying vec2 v_texCoord;\n" +
//                    "uniform mat4 u_mvpMatrix;\n" +
//                    "void main() {\n" +
//                    "  gl_Position = u_mvpMatrix * a_position;\n" +
//                    "  v_texCoord = a_texCoord;\n" +
//                    "}";
//
//    private static final String FRAGMENT_SHADER_SRC =
//            "precision mediump float;\n" +
//                    "varying vec2 v_texCoord;\n" +
//                    "uniform sampler2D u_texture;\n" +
//                    "void main() {\n" +
//                    "  gl_FragColor = texture2D(u_texture, v_texCoord);\n" +
//                    "}";
//
//    private FloatBuffer vertexBuffer, textureBuffer;
//    private int vertexCount;
//
//    private int shaderProgramId;
//    private int positionHandle, textureCoordHandle, mvpMatrixHandle, textureHandle;
//    private int[] textureNames;
//
//    public TexturedCylinderRenderer(Context context, int slices, float radius, float height) {
//        super();
//
//        List<Float> vertices = new ArrayList<>();
//        List<Float> texCoords = new ArrayList<>();
//
//        // Build the side of the cylinder
//        for (int i = 0; i <= slices; ++i) {
//            float theta = (float) (2.0 * Math.PI * i / slices);
//            float x = (float) Math.cos(theta);
//            float z = (float) Math.sin(theta);
//
//            // Bottom vertex
//            vertices.add(x * radius);
//            vertices.add(-height / 2);
//            vertices.add(z * radius);
//            texCoords.add((float) i / slices);
//            texCoords.add(1.0f);
//
//            // Top vertex
//            vertices.add(x * radius);
//            vertices.add(height / 2);
//            vertices.add(z * radius);
//            texCoords.add((float) i / slices);
//            texCoords.add(0.0f);
//        }
//
//        vertexCount = vertices.size() / 3;
//
//        ByteBuffer vb = ByteBuffer.allocateDirect(vertices.size() * 4).order(ByteOrder.nativeOrder());
//        vertexBuffer = vb.asFloatBuffer();
//        for (Float f : vertices) vertexBuffer.put(f);
//        vertexBuffer.position(0);
//
//        ByteBuffer tb = ByteBuffer.allocateDirect(texCoords.size() * 4).order(ByteOrder.nativeOrder());
//        textureBuffer = tb.asFloatBuffer();
//        for (Float f : texCoords) textureBuffer.put(f);
//        textureBuffer.position(0);
//
//        // Setup shader
//        shaderProgramId = ShaderUtil.createProgram(VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
//        positionHandle = GLES20.glGetAttribLocation(shaderProgramId, "a_position");
//        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramId, "a_texCoord");
//        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramId, "u_mvpMatrix");
//        textureHandle = GLES20.glGetUniformLocation(shaderProgramId, "u_texture");
//
//        // Texture setup
//        textureNames = new int[1];
//        GLES20.glGenTextures(1, textureNames, 0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
//        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
//    }
//
//    @Override
//    public void draw() {
//        GLES20.glUseProgram(shaderProgramId);
//
//        GLES20.glEnableVertexAttribArray(positionHandle);
//        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
//
//        GLES20.glEnableVertexAttribArray(textureCoordHandle);
//        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
//
//        Matrix.setIdentityM(modelMatrix, 0);
//        Matrix.multiplyMM(modelMatrix, 0, translation, 0, rotation, 0);
//        Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, scale, 0);
//        Matrix.multiplyMM(modelMatrix, 0, transform, 0, modelMatrix, 0);
//        Matrix.multiplyMM(localMvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0);
//
//        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, localMvpMatrix, 0);
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
//        GLES20.glUniform1i(textureHandle, 0);
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);
//
//        GLES20.glDisableVertexAttribArray(positionHandle);
//        GLES20.glDisableVertexAttribArray(textureCoordHandle);
//    }
//
//    public void setTextureBitmap(Bitmap texture) {
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
//        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0);
//    }
//}
//
