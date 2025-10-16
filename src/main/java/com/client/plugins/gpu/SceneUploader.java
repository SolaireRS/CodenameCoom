package com.client.plugins.gpu;

import com.client.Model;
import com.client.WorldController;
import org.lwjgl.opengl.GL15;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * Uploads scene geometry to GPU buffers
 * This is the core of RuneLite's GPU rendering - converts the scene into GPU-friendly format
 */
public class SceneUploader {
    
    private GpuPluginRunelite plugin;
    
    // Temporary buffers for geometry
    private List<Float> vertexData = new ArrayList<>();
    private List<Float> uvData = new ArrayList<>();
    private List<Float> normalData = new ArrayList<>();
    private List<Float> colorData = new ArrayList<>();
    
    // Maximum buffer size
    private static final int MAX_VERTICES = 500000;
    
    public SceneUploader(GpuPluginRunelite plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Upload entire scene to GPU
     */
    public void upload(WorldController scene, int plane) {
        if (scene == null) return;
        
        // Clear buffers
        vertexData.clear();
        uvData.clear();
        normalData.clear();
        colorData.clear();
        
        try {
            // Upload ground tiles
            uploadGroundTiles(scene, plane);
            
            // Upload models (game objects, NPCs, players, etc.)
            uploadSceneModels(scene, plane);
            
            // Upload all buffered data to GPU
            flushBuffers();
            
        } catch (Exception e) {
            System.err.println("Error uploading scene: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Upload ground tiles to GPU
     */
    private void uploadGroundTiles(WorldController scene, int plane) {
        // Access the tile array from WorldController
        // This matches your client's structure
        
        for (int x = 0; x < 104; x++) {
            for (int y = 0; y < 104; y++) {
                try {
                    // Get tile at position
                    // You'll need to adapt this to match your WorldController's tile access
                    uploadTile(scene, plane, x, y);
                } catch (Exception e) {
                    // Skip problematic tiles
                }
            }
        }
    }
    
    /**
     * Upload a single tile
     */
    private void uploadTile(WorldController scene, int plane, int x, int y) {
        // This needs to be adapted to your tile structure
        // For now, create a simple ground quad
        
        int tileSize = 128; // OSRS tile size
        float x1 = x * tileSize;
        float y1 = y * tileSize;
        float x2 = (x + 1) * tileSize;
        float y2 = (y + 1) * tileSize;
        float z = 0; // Ground level
        
        // Get tile height from scene
        // You'll need to adapt this to your scene's height data
        
        // Simple ground color (grass-like)
        float r = 0.2f;
        float g = 0.5f;
        float b = 0.1f;
        float a = 1.0f;
        
        // Create two triangles for the tile (quad)
        // Triangle 1
        addVertex(x1, z, y1, 0, 0, 0, 1, 0, r, g, b, a);
        addVertex(x2, z, y1, 1, 0, 0, 1, 0, r, g, b, a);
        addVertex(x1, z, y2, 0, 1, 0, 1, 0, r, g, b, a);
        
        // Triangle 2
        addVertex(x2, z, y1, 1, 0, 0, 1, 0, r, g, b, a);
        addVertex(x2, z, y2, 1, 1, 0, 1, 0, r, g, b, a);
        addVertex(x1, z, y2, 0, 1, 0, 1, 0, r, g, b, a);
    }
    
    /**
     * Upload scene models (objects, NPCs, players)
     */
    private void uploadSceneModels(WorldController scene, int plane) {
        // This is where you'd iterate through all visible models in the scene
        // You'll need to adapt this to your client's model system
        
        // Example structure (adapt to your client):
        /*
        for (Model model : scene.getVisibleModels()) {
            uploadModel(model, modelX, modelY, modelZ, modelRotation);
        }
        */
    }
    
    /**
     * Upload a single model to GPU buffers
     */
    public void uploadModel(Model model, int x, int y, int z, int rotation) {
        if (model == null) return;
        
        try {
            // Access model data
            // Adapt these to match your Model class structure
            int[] verticesX = model.verticesX;
            int[] verticesY = model.verticesY;
            int[] verticesZ = model.verticesZ;
            
            int[] trianglesX = model.trianglePointsX;
            int[] trianglesY = model.trianglePointsY;
            int[] trianglesZ = model.trianglePointsZ;
            
            short[] faceColors = model.faceColor;
            
            int triangleCount = model.triangleCount;
            
            // Upload each triangle
            for (int i = 0; i < triangleCount; i++) {
                int v1 = trianglesX[i];
                int v2 = trianglesY[i];
                int v3 = trianglesZ[i];
                
                // Get vertex positions
                float x1 = verticesX[v1] + x;
                float y1 = verticesY[v1] + y;
                float z1 = verticesZ[v1] + z;
                
                float x2 = verticesX[v2] + x;
                float y2 = verticesY[v2] + y;
                float z2 = verticesZ[v2] + z;
                
                float x3 = verticesX[v3] + x;
                float y3 = verticesY[v3] + y;
                float z3 = verticesZ[v3] + z;
                
                // Get color
                int color = faceColors != null && i < faceColors.length ? 
                           faceColors[i] : 0x7FFF;
                
                float r = ((color >> 10) & 0x1F) / 31.0f;
                float g = ((color >> 5) & 0x1F) / 31.0f;
                float b = (color & 0x1F) / 31.0f;
                
                // Calculate normal
                float nx = (y2 - y1) * (z3 - z1) - (z2 - z1) * (y3 - y1);
                float ny = (z2 - z1) * (x3 - x1) - (x2 - x1) * (z3 - z1);
                float nz = (x2 - x1) * (y3 - y1) - (y2 - y1) * (x3 - x1);
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 0) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                }
                
                // Add triangle vertices
                addVertex(x1, y1, z1, 0, 0, nx, ny, nz, r, g, b, 1.0f);
                addVertex(x2, y2, z2, 0, 0, nx, ny, nz, r, g, b, 1.0f);
                addVertex(x3, y3, z3, 0, 0, nx, ny, nz, r, g, b, 1.0f);
            }
            
        } catch (Exception e) {
            System.err.println("Error uploading model: " + e.getMessage());
        }
    }
    
    /**
     * Add a vertex to the buffer
     */
    private void addVertex(float x, float y, float z, 
                          float u, float v,
                          float nx, float ny, float nz,
                          float r, float g, float b, float a) {
        
        // Check buffer size
        if (vertexData.size() / 3 >= MAX_VERTICES) {
            System.err.println("Vertex buffer full, flushing...");
            flushBuffers();
        }
        
        // Position
        vertexData.add(x);
        vertexData.add(y);
        vertexData.add(z);
        
        // UV
        uvData.add(u);
        uvData.add(v);
        
        // Normal
        normalData.add(nx);
        normalData.add(ny);
        normalData.add(nz);
        
        // Color
        colorData.add(r);
        colorData.add(g);
        colorData.add(b);
        colorData.add(a);
    }
    
    /**
     * Flush all buffered geometry to GPU
     */
    private void flushBuffers() {
        if (vertexData.isEmpty()) return;
        
        int vertexCount = vertexData.size() / 3;
        
        // Convert lists to arrays
        float[] vertices = toFloatArray(vertexData);
        float[] uvs = toFloatArray(uvData);
        float[] normals = toFloatArray(normalData);
        float[] colors = toFloatArray(colorData);
        
        // Upload to GPU
        glBindVertexArray(plugin.getVaoHandle());
        
        // Vertex positions
        glBindBuffer(GL_ARRAY_BUFFER, plugin.getVertexBuffer());
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);
        
        // UVs
        glBindBuffer(GL_ARRAY_BUFFER, plugin.getUvBuffer());
        glBufferData(GL_ARRAY_BUFFER, uvs, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);
        
        // Normals
        glBindBuffer(GL_ARRAY_BUFFER, plugin.getNormalBuffer());
        glBufferData(GL_ARRAY_BUFFER, normals, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(2);
        
        // Colors (using attribute location 3)
        int colorBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, colorBuffer);
        glBufferData(GL_ARRAY_BUFFER, colors, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(3, 4, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(3);
        
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        
        // Update vertex count
        plugin.setVertexCount(vertexCount);
        
        System.out.println("Uploaded " + vertexCount + " vertices to GPU");
    }
    
    /**
     * Convert List<Float> to float[]
     */
    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}