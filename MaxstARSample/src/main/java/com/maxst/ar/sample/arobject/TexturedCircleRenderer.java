package com.maxst.ar.sample.arobject;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.github.mikephil.charting.renderer.Renderer;
import com.maxst.ar.sample.util.ShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class TexturedCircleRenderer extends BaseRenderer {

    private static final int SEGMENT_COUNT = 64;

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

    private FloatBuffer vertexBuffer, texCoordBuffer;
    private int shaderProgramId, positionHandle, textureCoordHandle, mvpMatrixHandle, textureHandle;
    private int[] textureNames;

    private float[] modelMatrix = new float[16];
    private float[] localMvpMatrix = new float[16];

    public TexturedCircleRenderer(Context context) {
        super();

        float[] vertices = new float[(SEGMENT_COUNT + 2) * 3];
        float[] texCoords = new float[(SEGMENT_COUNT + 2) * 2];

        // Center point of circle
        vertices[0] = 0;
        vertices[1] = 0;
        vertices[2] = 0;
        texCoords[0] = 0.5f;
        texCoords[1] = 0.5f;

        // Generate circle vertices and texture coordinates
        for (int i = 0; i <= SEGMENT_COUNT; i++) {
            double angle = 2.0 * Math.PI * i / SEGMENT_COUNT;
            float x = (float) Math.cos(angle) * 0.5f;
            float y = (float) Math.sin(angle) * 0.5f;

            vertices[(i + 1) * 3 + 0] = x;
            vertices[(i + 1) * 3 + 1] = y;
            vertices[(i + 1) * 3 + 2] = 0;

            texCoords[(i + 1) * 2 + 0] = x + 0.5f;
            texCoords[(i + 1) * 2 + 1] = y + 0.5f;
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(vertices).position(0);

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texCoordBuffer.put(texCoords).position(0);

        shaderProgramId = ShaderUtil.createProgram(VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
        positionHandle = GLES20.glGetAttribLocation(shaderProgramId, "a_position");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramId, "a_texCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramId, "u_mvpMatrix");
        textureHandle = GLES20.glGetUniformLocation(shaderProgramId, "u_texture");

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

        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);

        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.multiplyMM(modelMatrix, 0, translation, 0, rotation, 0);
        Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, scale, 0);
        Matrix.multiplyMM(modelMatrix, 0, transform, 0, modelMatrix, 0);
        Matrix.multiplyMM(localMvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, localMvpMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(textureHandle, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, SEGMENT_COUNT + 2);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void setTextureBitmap(Bitmap texture) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0);
    }
}

