package com.maxst.ar.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class ObjModelLoader {
    private FloatBuffer vertexBuffer, texCoordBuffer, normalBuffer;
    private int vertexCount;
    private Context context;
    private HashMap<String, Integer> materialTextureMap = new HashMap<>();

    public ObjModelLoader(Context context) {
        this.context = context;
    }


 /*   public void loadModel(String fileName, String materialFile) {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> texCoords = new ArrayList<>();
        ArrayList<Float> normals = new ArrayList<>();
        ArrayList<Integer> vertexIndices = new ArrayList<>();
        ArrayList<Integer> texIndices = new ArrayList<>();
        ArrayList<Integer> normalIndices = new ArrayList<>();

        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");

                if (parts.length == 0) continue;  // Skip empty lines

                switch (parts[0]) {
                    case "v":  // Vertex position
                        try {
                            vertices.add(Float.parseFloat(parts[1]));
                            vertices.add(Float.parseFloat(parts[2]));
                            vertices.add(Float.parseFloat(parts[3]));
                        } catch (Exception e) {
                            Log.e("ObjLoader", "Invalid vertex data: " + line);
                        }
                        break;

                    case "vt":  // Texture coordinates
                        try {
                            texCoords.add(Float.parseFloat(parts[1]));
                            texCoords.add(Float.parseFloat(parts[2]));
                        } catch (Exception e) {
                            Log.e("ObjLoader", "Invalid texture coordinate: " + line);
                        }
                        break;

                    case "vn":  // Normal vectors
                        try {
                            normals.add(Float.parseFloat(parts[1]));
                            normals.add(Float.parseFloat(parts[2]));
                            normals.add(Float.parseFloat(parts[3]));
                        } catch (Exception e) {
                            Log.e("ObjLoader", "Invalid normal vector: " + line);
                        }
                        break;

                    case "f":  // Faces
                        processFace(parts, vertexIndices, texIndices, normalIndices);
                        break;
                }
            }
            reader.close();
        } catch (IOException e) {
            Log.e("ObjModelLoader", "Error loading model: " + e.getMessage());
        }

        buildBuffers(vertices, texCoords, normals, vertexIndices, texIndices, normalIndices);
    }

    // Handles different face formats in .obj files
    private void processFace(String[] parts, ArrayList<Integer> vertexIndices,
                             ArrayList<Integer> texIndices, ArrayList<Integer> normalIndices) {
        int faceCount = parts.length - 1;
        if (faceCount < 3) return;  // Invalid face data

        int[] indices = new int[faceCount];

        for (int i = 1; i <= faceCount; i++) {
            String[] faceParts = parts[i].split("/");
            try {
                indices[i - 1] = parseFace(faceParts, vertexIndices, texIndices, normalIndices);
            } catch (Exception e) {
                Log.e("ObjLoader", "Invalid face data: " + parts[i]);
            }
        }

        // Convert quads to triangles
        if (faceCount == 4) {
            addTriangle(vertexIndices, texIndices, normalIndices, indices[0], indices[1], indices[2]);
            addTriangle(vertexIndices, texIndices, normalIndices, indices[0], indices[2], indices[3]);
        } else {
            addTriangle(vertexIndices, texIndices, normalIndices, indices[0], indices[1], indices[2]);
        }
    }

    // Parses face components and adds them to the respective lists
    private int parseFace(String[] faceParts, ArrayList<Integer> vertexIndices,
                          ArrayList<Integer> texIndices, ArrayList<Integer> normalIndices) {
        int vIndex = Integer.parseInt(faceParts[0]) - 1;
        vertexIndices.add(vIndex);

        if (faceParts.length > 1 && !faceParts[1].isEmpty()) {
            texIndices.add(Integer.parseInt(faceParts[1]) - 1);
        } else {
            texIndices.add(-1);
        }

        if (faceParts.length > 2 && !faceParts[2].isEmpty()) {
            normalIndices.add(Integer.parseInt(faceParts[2]) - 1);
        } else {
            normalIndices.add(-1);
        }

        return vIndex;
    }

    // Adds a triangle to index lists
    private void addTriangle(ArrayList<Integer> vertexIndices, ArrayList<Integer> texIndices,
                             ArrayList<Integer> normalIndices, int i1, int i2, int i3) {
        vertexIndices.add(i1);
        vertexIndices.add(i2);
        vertexIndices.add(i3);
    }

    // Builds FloatBuffers for OpenGL rendering
    private void buildBuffers(ArrayList<Float> vertices, ArrayList<Float> texCoords,
                              ArrayList<Float> normals, ArrayList<Integer> vertexIndices,
                              ArrayList<Integer> texIndices, ArrayList<Integer> normalIndices) {

        vertexCount = vertexIndices.size();
        ByteBuffer vBuffer = ByteBuffer.allocateDirect(vertexCount * 3 * 4).order(ByteOrder.nativeOrder());
        ByteBuffer tBuffer = ByteBuffer.allocateDirect(vertexCount * 2 * 4).order(ByteOrder.nativeOrder());
        ByteBuffer nBuffer = ByteBuffer.allocateDirect(vertexCount * 3 * 4).order(ByteOrder.nativeOrder());

        vertexBuffer = vBuffer.asFloatBuffer();
        texCoordBuffer = tBuffer.asFloatBuffer();
        normalBuffer = nBuffer.asFloatBuffer();

        for (int i = 0; i < vertexIndices.size(); i++) {
            int vIndex = vertexIndices.get(i);
            int tIndex = (texIndices.size() > i) ? texIndices.get(i) : -1; // Safe check
            int nIndex = (normalIndices.size() > i) ? normalIndices.get(i) : -1; // Safe check

            // ✅ Validate index before accessing
            if (vIndex < 0 || vIndex >= vertices.size() / 3) {
                continue;
            }
            if (tIndex != -1 && (tIndex < 0 || tIndex >= texCoords.size() / 2)) {
                tIndex = -1; // Ignore invalid texture indices
            }
            if (nIndex != -1 && (nIndex < 0 || nIndex >= normals.size() / 3)) {
                nIndex = -1; // Ignore invalid normal indices
            }

            // ✅ Safe Vertex Positions
            vertexBuffer.put(vertices.get(vIndex * 3));
            vertexBuffer.put(vertices.get(vIndex * 3 + 1));
            vertexBuffer.put(vertices.get(vIndex * 3 + 2));

            // ✅ Safe Texture Coordinates (or default 0.0f if missing)
            if (tIndex != -1) {
                texCoordBuffer.put(texCoords.get(tIndex * 2));
                texCoordBuffer.put(texCoords.get(tIndex * 2 + 1));
            } else {
                texCoordBuffer.put(0.0f);
                texCoordBuffer.put(0.0f);
            }

            // ✅ Safe Normal Vectors (or default 0.0f if missing)
            if (nIndex != -1) {
                normalBuffer.put(normals.get(nIndex * 3));
                normalBuffer.put(normals.get(nIndex * 3 + 1));
                normalBuffer.put(normals.get(nIndex * 3 + 2));
            } else {
                normalBuffer.put(0.0f);
                normalBuffer.put(0.0f);
                normalBuffer.put(0.0f);
            }
        }

        vertexBuffer.position(0);
        texCoordBuffer.position(0);
        normalBuffer.position(0);
    }*/






   /* public void loadModel(String fileName, String materialFile) {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> texCoords = new ArrayList<>();
        ArrayList<Float> normals = new ArrayList<>();
        ArrayList<Integer> vertexIndices = new ArrayList<>();
        ArrayList<Integer> texIndices = new ArrayList<>();
        ArrayList<Integer> normalIndices = new ArrayList<>();

        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");

                if (line.startsWith("v ")) {  // Read vertex positions
                    vertices.add(Float.parseFloat(parts[1]));
                    vertices.add(Float.parseFloat(parts[2]));
                    vertices.add(Float.parseFloat(parts[3]));
                } else if (line.startsWith("vt ")) {  // Read texture coordinates
                    texCoords.add(Float.parseFloat(parts[1]));
                    texCoords.add(Float.parseFloat(parts[2]));
                } else if (line.startsWith("vn ")) {  // Read normal vectors
                    normals.add(Float.parseFloat(parts[1]));
                    normals.add(Float.parseFloat(parts[2]));
                    normals.add(Float.parseFloat(parts[3]));
                } else if (line.startsWith("f ")) {  // Read faces
                    if (parts.length == 4) {  // Triangle
                        processFace(parts, vertexIndices, texIndices, normalIndices);
                    } else if (parts.length == 5) {  // Quad (split into two triangles)
                        String[] face1 = {parts[0], parts[1], parts[2], parts[3]};
                        String[] face2 = {parts[0], parts[3], parts[4], parts[1]};
                        processFace(face1, vertexIndices, texIndices, normalIndices);
                        processFace(face2, vertexIndices, texIndices, normalIndices);
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            Log.e("ObjModelLoader", "Error loading model: " + e.getMessage());
        }

        // Convert vertex indices to FloatBuffer
        vertexCount = vertexIndices.size();
        ByteBuffer buffer = ByteBuffer.allocateDirect(vertexCount * 3 * 4);
        buffer.order(ByteOrder.nativeOrder());
        vertexBuffer = buffer.asFloatBuffer();

        ByteBuffer texBuffer = ByteBuffer.allocateDirect(vertexCount * 2 * 4);
        texBuffer.order(ByteOrder.nativeOrder());
        texCoordBuffer = texBuffer.asFloatBuffer();

        ByteBuffer normBuffer = ByteBuffer.allocateDirect(vertexCount * 3 * 4);
        normBuffer.order(ByteOrder.nativeOrder());
        normalBuffer = normBuffer.asFloatBuffer();

        for (int i = 0; i < vertexIndices.size(); i++) {
            int vIndex = vertexIndices.get(i) * 3;
            int tIndex = texIndices.isEmpty() ? -1 : texIndices.get(i) * 2;
            int nIndex = normalIndices.isEmpty() ? -1 : normalIndices.get(i) * 3;

            // Add vertex position
            vertexBuffer.put(vertices.get(vIndex));
            vertexBuffer.put(vertices.get(vIndex + 1));
            vertexBuffer.put(vertices.get(vIndex + 2));

            // Add texture coordinates (check if available)
            if (tIndex >= 0 && tIndex < texCoords.size()) {
                texCoordBuffer.put(texCoords.get(tIndex));
                texCoordBuffer.put(texCoords.get(tIndex + 1));
            } else {
                texCoordBuffer.put(0.0f);
                texCoordBuffer.put(0.0f);
            }

            // Add normal vector (check if available)
            if (nIndex >= 0 && nIndex < normals.size()) {
                normalBuffer.put(normals.get(nIndex));
                normalBuffer.put(normals.get(nIndex + 1));
                normalBuffer.put(normals.get(nIndex + 2));
            } else {
                normalBuffer.put(0.0f);
                normalBuffer.put(0.0f);
                normalBuffer.put(0.0f);
            }
        }

        vertexBuffer.position(0);
        texCoordBuffer.position(0);
        normalBuffer.position(0);

        // loadMaterial(materialFile);
    }

    // Helper function to process a face
    private void processFace(String[] parts, ArrayList<Integer> vertexIndices,
                             ArrayList<Integer> texIndices, ArrayList<Integer> normalIndices) {
        for (int i = 1; i <= 3; i++) { // Assuming triangles
            String[] faceParts = parts[i].split("/");

            // Parse vertex index
            vertexIndices.add(Integer.parseInt(faceParts[0]) - 1);

            // Parse texture index (handle missing values)
            if (faceParts.length > 1 && !faceParts[1].isEmpty()) {
                texIndices.add(Integer.parseInt(faceParts[1]) - 1);
            } else {
                texIndices.add(-1);
            }

            // Parse normal index (handle missing values)
            if (faceParts.length > 2 && !faceParts[2].isEmpty()) {
                normalIndices.add(Integer.parseInt(faceParts[2]) - 1);
            } else {
                normalIndices.add(-1);
            }
        }
    }*/












    // working 80%

    public void loadModel(String fileName,String materialFile) {
        ArrayList<Float> vertices = new ArrayList<>();
        ArrayList<Float> texCoords = new ArrayList<>();
        ArrayList<Float> normals = new ArrayList<>();
        ArrayList<Integer> vertexIndices = new ArrayList<>();
        ArrayList<Integer> texIndices = new ArrayList<>();
        ArrayList<Integer> normalIndices = new ArrayList<>();

        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");

                if (line.startsWith("v ")) { // Read vertex positions
                    vertices.add(Float.parseFloat(parts[1]));
                    vertices.add(Float.parseFloat(parts[2]));
                    vertices.add(Float.parseFloat(parts[3]));
                } else if (line.startsWith("vt ")) { // Read texture coordinates
                    texCoords.add(Float.parseFloat(parts[1]));
                    texCoords.add(Float.parseFloat(parts[2]));
                } else if (line.startsWith("vn ")) { // Read normal vectors
                    normals.add(Float.parseFloat(parts[1]));
                    normals.add(Float.parseFloat(parts[2]));
                    normals.add(Float.parseFloat(parts[3]));
                } else if (line.startsWith("f ")) { // Read faces
                    for (int i = 1; i <= 3; i++) { // Assuming triangles
                        String[] faceParts = parts[i].split("/");
                        vertexIndices.add(Integer.parseInt(faceParts[0]) - 1);  // Vertex index
                        texIndices.add(Integer.parseInt(faceParts[1]) - 1);     // Texture index
                        normalIndices.add(Integer.parseInt(faceParts[2]) - 1);  // Normal index
                    }
                }
            }
            reader.close();
        } catch (IOException e) {
            Log.e("ObjModelLoader", "Error loading model: " + e.getMessage());
        }

        // Convert vertex indices to FloatBuffer
        vertexCount = vertexIndices.size();
        ByteBuffer buffer = ByteBuffer.allocateDirect(vertexCount * 3 * 4);
        buffer.order(ByteOrder.nativeOrder());
        vertexBuffer = buffer.asFloatBuffer();

        ByteBuffer texBuffer = ByteBuffer.allocateDirect(vertexCount * 2 * 4);
        texBuffer.order(ByteOrder.nativeOrder());
        texCoordBuffer = texBuffer.asFloatBuffer();

        ByteBuffer normBuffer = ByteBuffer.allocateDirect(vertexCount * 3 * 4);
        normBuffer.order(ByteOrder.nativeOrder());
        normalBuffer = normBuffer.asFloatBuffer();

        for (int i = 0; i < vertexIndices.size(); i++) {
            int vIndex = vertexIndices.get(i) * 3;
            int tIndex = texIndices.get(i) * 2;
            int nIndex = normalIndices.get(i) * 3;

            // Add vertex position
            vertexBuffer.put(vertices.get(vIndex));
            vertexBuffer.put(vertices.get(vIndex + 1));
            vertexBuffer.put(vertices.get(vIndex + 2));

            // Add texture coordinates
            texCoordBuffer.put(texCoords.get(tIndex));
            texCoordBuffer.put(texCoords.get(tIndex + 1));

            // Add normal vector
            normalBuffer.put(normals.get(nIndex));
            normalBuffer.put(normals.get(nIndex + 1));
            normalBuffer.put(normals.get(nIndex + 2));
        }

        vertexBuffer.position(0);
        texCoordBuffer.position(0);
        normalBuffer.position(0);

      //  loadMaterial(materialFile);
    }






















    public void loadMaterial(String fileName) {
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            String currentMaterial = null;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");

                if (line.startsWith("newmtl")) {
                    currentMaterial = parts[1];  // Store material name
                } else if (line.startsWith("map_Kd") && currentMaterial != null) {
                    String textureFile = parts[1];
                    int textureID = loadTexture(textureFile);
                    materialTextureMap.put(currentMaterial, textureID);
                    Log.d("ObjModelLoader", "Loaded texture for " + currentMaterial + ": " + textureFile);
                }
            }
            reader.close();
        } catch (IOException e) {
            Log.e("ObjModelLoader", "Error loading .mtl file: " + e.getMessage());
        }
    }

    public int loadTexture(String fileName) {
        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;

            try (InputStream inputStream = context.getAssets().open(fileName)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap == null) {
                    Log.e("ObjModelLoader", "Bitmap failed to load: " + fileName);
                    return 0;
                }

                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                bitmap.recycle();

            } catch (IOException e) {
                Log.e("ObjModelLoader", "Error loading texture: " + e.getMessage());
                return 0;
            }
        } else {
            Log.e("ObjModelLoader", "Texture generation failed!");
            return 0;
        }
        return textureHandle[0];
    }




    // working

   /* public void loadMaterial(String fileName) {
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            String currentMaterial = "";

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");

                if (line.startsWith("newmtl")) {  // New material
                    currentMaterial = parts[1];
                } else if (line.startsWith("map_Kd")) {  // Diffuse texture
                    String textureFile = parts[1];
                    loadTexture(textureFile);
                }
            }
            reader.close();
        } catch (IOException e) {
            Log.e("ObjModelLoader", "Error loading .mtl file: " + e.getMessage());
        }
    }

    public int loadTexture(String fileName) {
        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;
            InputStream inputStream;

            try {
                inputStream = context.getAssets().open(fileName);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
                GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                bitmap.recycle();
            } catch (IOException e) {
                Log.e("ObjModelLoader", "Error loading texture: " + e.getMessage());
            }
        }
        return textureHandle[0];
    }*/



    // working

    public void draw(int positionHandle, int normalHandle, int texCoordHandle) {
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        GLES20.glEnableVertexAttribArray(normalHandle);
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);

        GLES20.glEnableVertexAttribArray(texCoordHandle);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(normalHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }





  /*  public void draw() {

        // Bind texture
        Integer textureID = materialTextureMap.get("Cat");
        if (textureID != null) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureID);
        }

        // Enable vertex attributes
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glEnableVertexAttribArray(1);
        GLES20.glEnableVertexAttribArray(2);

        // Bind buffers and pass data
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);
        GLES20.glVertexAttribPointer(2, 3, GLES20.GL_FLOAT, false, 0, normalBuffer);

        // Draw the model
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);

        // Disable vertex attributes
        GLES20.glDisableVertexAttribArray(0);
        GLES20.glDisableVertexAttribArray(1);
        GLES20.glDisableVertexAttribArray(2);

        GLES20.glDisable(GLES20.GL_TEXTURE_2D);
    }*/



}




