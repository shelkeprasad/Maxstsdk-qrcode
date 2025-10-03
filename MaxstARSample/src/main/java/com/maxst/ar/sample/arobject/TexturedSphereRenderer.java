package com.maxst.ar.sample.arobject;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.maxst.ar.sample.arobject.BaseRenderer;
import com.maxst.ar.sample.util.ShaderUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class TexturedSphereRenderer extends BaseRenderer {

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

    private FloatBuffer vertexBuffer, textureBuffer;
    private int vertexCount;

    private int shaderProgramId;
    private int positionHandle, textureCoordHandle, mvpMatrixHandle, textureHandle;
    private int[] textureNames;

    public TexturedSphereRenderer(Context context, int stacks, int slices, float radius) {
        super();

        // Generate sphere data
        List<Float> vertices = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();

        for (int stack = 0; stack < stacks; ++stack) {
            float phi1 = (float) (Math.PI * stack / stacks);
            float phi2 = (float) (Math.PI * (stack + 1) / stacks);

            for (int slice = 0; slice <= slices; ++slice) {
                float theta = (float) (2.0 * Math.PI * slice / slices);

                for (int i = 0; i < 2; ++i) {
                    float phi = (i == 0) ? phi1 : phi2;
                    float x = (float) (Math.sin(phi) * Math.cos(theta));
                    float y = (float) Math.cos(phi);
                    float z = (float) (Math.sin(phi) * Math.sin(theta));
                    vertices.add(x * radius);
                    vertices.add(y * radius);
                    vertices.add(z * radius);

                    float u = (float) slice / slices;
                    float v = (i == 0) ? (float) stack / stacks : (float) (stack + 1) / stacks;
                    texCoords.add(u);
                    texCoords.add(v);
                }
            }
        }

        vertexCount = vertices.size() / 3;

        ByteBuffer vb = ByteBuffer.allocateDirect(vertices.size() * 4).order(ByteOrder.nativeOrder());
        vertexBuffer = vb.asFloatBuffer();
        for (Float f : vertices) vertexBuffer.put(f);
        vertexBuffer.position(0);

        ByteBuffer tb = ByteBuffer.allocateDirect(texCoords.size() * 4).order(ByteOrder.nativeOrder());
        textureBuffer = tb.asFloatBuffer();
        for (Float f : texCoords) textureBuffer.put(f);
        textureBuffer.position(0);

        // Setup shader
        shaderProgramId = ShaderUtil.createProgram(VERTEX_SHADER_SRC, FRAGMENT_SHADER_SRC);
        positionHandle = GLES20.glGetAttribLocation(shaderProgramId, "a_position");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramId, "a_texCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramId, "u_mvpMatrix");
        textureHandle = GLES20.glGetUniformLocation(shaderProgramId, "u_texture");

        // Texture
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

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(textureCoordHandle);
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.multiplyMM(modelMatrix, 0, translation, 0, rotation, 0);
        Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, scale, 0);
        Matrix.multiplyMM(modelMatrix, 0, transform, 0, modelMatrix, 0);
        Matrix.multiplyMM(localMvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, localMvpMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
        GLES20.glUniform1i(textureHandle, 0);

      //  GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);


        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
    }

    public void setTextureBitmap(Bitmap texture) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureNames[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture, 0);
    }
}


