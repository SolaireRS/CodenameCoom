package com.client.plugins.gpu;

import com.client.Client;
import com.client.WorldController;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

import javax.swing.*;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * RuneLite-style GPU Plugin for hardware-accelerated 3D rendering
 * This replaces software rasterization with OpenGL rendering
 */
public class GpuPluginRunelite {
    
    private Client client;
    private boolean enabled = false;
    private boolean initialized = false;
    
    // OpenGL objects
    private int fboSceneHandle = 0;
    private int rboSceneHandle = 0;
    private int textureSceneHandle = 0;
    
    // Shader program
    private int glProgram;
    private int uniformProjectionMatrix;
    private int uniformBrightness;
    
    // Geometry buffers
    private SceneUploader sceneUploader;
    private int vaoHandle;
    private int vertexBuffer;
    private int uvBuffer;
    private int normalBuffer;
    
    // Scene data
    private int vertexCount = 0;
    private int targetBufferOffset = 0;
    
    // Configuration
    private int viewportWidth = 765;
    private int viewportHeight = 503;
    
    public GpuPluginRunelite(Client client) {
        this.client = client;
    }
    
    /**
     * Initialize OpenGL context and resources
     */
    public boolean initialize() {
        if (initialized) return true;
        
        try {
            // Create OpenGL context (assumes LWJGL context already exists from client)
            GLCapabilities caps = GL.createCapabilities();
            
            System.out.println("OpenGL Version: " + glGetString(GL_VERSION));
            System.out.println("GLSL Version: " + glGetString(GL_SHADING_LANGUAGE_VERSION));
            
            // Check OpenGL version
            String version = glGetString(GL_VERSION);
            if (!version.startsWith("3.") && !version.startsWith("4.")) {
                System.err.println("OpenGL 3.0+ required, found: " + version);
                return false;
            }
            
            // Initialize components
            initShaders();
            initBuffers();
            initFramebuffer();
            
            sceneUploader = new SceneUploader(this);
            
            initialized = true;
            System.out.println("GPU Plugin initialized successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to initialize GPU plugin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Initialize shader program
     */
    private void initShaders() {
        // Vertex shader
        String vertexShaderSource = 
            "#version 330 core\n" +
            "layout(location = 0) in vec3 vertexPosition;\n" +
            "layout(location = 1) in vec2 vertexUV;\n" +
            "layout(location = 2) in vec3 vertexNormal;\n" +
            "layout(location = 3) in vec4 vertexColor;\n" +
            "\n" +
            "out vec2 UV;\n" +
            "out vec4 Color;\n" +
            "out vec3 Normal;\n" +
            "\n" +
            "uniform mat4 projectionMatrix;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = projectionMatrix * vec4(vertexPosition, 1.0);\n" +
            "    UV = vertexUV;\n" +
            "    Color = vertexColor;\n" +
            "    Normal = vertexNormal;\n" +
            "}\n";
        
        // Fragment shader
        String fragmentShaderSource =
            "#version 330 core\n" +
            "in vec2 UV;\n" +
            "in vec4 Color;\n" +
            "in vec3 Normal;\n" +
            "\n" +
            "out vec4 FragColor;\n" +
            "\n" +
            "uniform float brightness;\n" +
            "\n" +
            "void main() {\n" +
            "    // Simple lighting calculation\n" +
            "    vec3 lightDir = normalize(vec3(0.5, -1.0, -0.5));\n" +
            "    float diffuse = max(dot(Normal, -lightDir), 0.3);\n" +
            "    \n" +
            "    FragColor = vec4(Color.rgb * diffuse * brightness, Color.a);\n" +
            "}\n";
        
        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);
        
        glProgram = glCreateProgram();
        glAttachShader(glProgram, vertexShader);
        glAttachShader(glProgram, fragmentShader);
        glLinkProgram(glProgram);
        
        // Check linking
        if (glGetProgrami(glProgram, GL_LINK_STATUS) == GL_FALSE) {
            String error = glGetProgramInfoLog(glProgram);
            throw new RuntimeException("Shader linking failed: " + error);
        }
        
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        
        // Get uniform locations
        uniformProjectionMatrix = glGetUniformLocation(glProgram, "projectionMatrix");
        uniformBrightness = glGetUniformLocation(glProgram, "brightness");
        
        System.out.println("Shaders compiled and linked successfully");
    }
    
    /**
     * Compile a shader
     */
    private int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String error = glGetShaderInfoLog(shader);
            throw new RuntimeException("Shader compilation failed: " + error);
        }
        
        return shader;
    }
    
    /**
     * Initialize vertex buffers
     */
    private void initBuffers() {
        vaoHandle = glGenVertexArrays();
        glBindVertexArray(vaoHandle);
        
        // Create VBOs
        vertexBuffer = glGenBuffers();
        uvBuffer = glGenBuffers();
        normalBuffer = glGenBuffers();
        
        glBindVertexArray(0);
        
        System.out.println("Buffers initialized");
    }
    
    /**
     * Initialize framebuffer for off-screen rendering
     */
    private void initFramebuffer() {
        fboSceneHandle = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboSceneHandle);
        
        // Create texture for color attachment
        textureSceneHandle = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureSceneHandle);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, viewportWidth, viewportHeight, 
                     0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        
        // Create renderbuffer for depth
        rboSceneHandle = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboSceneHandle);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, 
                             viewportWidth, viewportHeight);
        
        // Attach to framebuffer
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, 
                              GL_TEXTURE_2D, textureSceneHandle, 0);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, 
                                 GL_RENDERBUFFER, rboSceneHandle);
        
        // Check framebuffer status
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer incomplete: " + status);
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
        System.out.println("Framebuffer initialized");
    }
    
    /**
     * Main render method - called each frame
     */
    public void render() {
        if (!enabled || !initialized) return;
        
        try {
            // Bind framebuffer
            glBindFramebuffer(GL_FRAMEBUFFER, fboSceneHandle);
            
            // Clear
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            // Enable depth testing
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS);
            
            // Use shader program
            glUseProgram(glProgram);
            
            // Upload scene geometry
            uploadScene();
            
            // Set uniforms
            setProjectionMatrix();
            glUniform1f(uniformBrightness, 1.0f);
            
            // Draw
            if (vertexCount > 0) {
                glBindVertexArray(vaoHandle);
                glDrawArrays(GL_TRIANGLES, 0, vertexCount);
                glBindVertexArray(0);
            }
            
            // Unbind
            glUseProgram(0);
            glBindFramebuffer(GL_FRAMEBUFFER, 0);
            
            // Copy framebuffer to client pixel buffer
            copyFramebufferToPixels();
            
        } catch (Exception e) {
            System.err.println("Render error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Upload scene geometry to GPU
     */
    private void uploadScene() {
        if (client.scene == null) return;
        
        // Reset counters
        vertexCount = 0;
        targetBufferOffset = 0;
        
        // Let the scene uploader handle geometry extraction
        if (sceneUploader != null) {
            sceneUploader.upload(client.scene, client.plane);
        }
    }
    
    /**
     * Set projection matrix uniform
     */
    private void setProjectionMatrix() {
        // Create orthographic projection matrix for OSRS-style rendering
        float left = 0;
        float right = viewportWidth;
        float bottom = viewportHeight;
        float top = 0;
        float near = -1000;
        float far = 1000;
        
        float[] matrix = new float[16];
        matrix[0] = 2.0f / (right - left);
        matrix[5] = 2.0f / (top - bottom);
        matrix[10] = -2.0f / (far - near);
        matrix[12] = -(right + left) / (right - left);
        matrix[13] = -(top + bottom) / (top - bottom);
        matrix[14] = -(far + near) / (far - near);
        matrix[15] = 1.0f;
        
        glUniformMatrix4fv(uniformProjectionMatrix, false, matrix);
    }
    
    /**
     * Copy rendered framebuffer to client pixel buffer
     */
    private void copyFramebufferToPixels() {
        // Read pixels from framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, fboSceneHandle);
        glReadBuffer(GL_COLOR_ATTACHMENT0);
        
        // Get pixel buffer from client
        int[] clientPixels = getClientPixelBuffer();
        if (clientPixels != null && clientPixels.length == viewportWidth * viewportHeight) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer pixels = stack.mallocInt(viewportWidth * viewportHeight);
                glReadPixels(0, 0, viewportWidth, viewportHeight, 
                           GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                
                // Copy to client buffer (flip vertically)
                for (int y = 0; y < viewportHeight; y++) {
                    for (int x = 0; x < viewportWidth; x++) {
                        int srcIdx = y * viewportWidth + x;
                        int dstIdx = (viewportHeight - 1 - y) * viewportWidth + x;
                        clientPixels[dstIdx] = pixels.get(srcIdx);
                    }
                }
            }
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Get client pixel buffer (override to match your client's structure)
     */
    private int[] getClientPixelBuffer() {
        // This needs to access your client's pixel array
        // Adjust based on your Client.java structure
        try {
            return client.mainGameGraphicsBuffer.pixels;
        } catch (Exception e) {
            System.err.println("Could not access client pixel buffer");
            return null;
        }
    }
    
    /**
     * Update buffer data
     */
    public void updateBuffer(int buffer, float[] data, int usage) {
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glBufferData(GL_ARRAY_BUFFER, data, usage);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    /**
     * Resize viewport
     */
    public void resize(int width, int height) {
        if (width == viewportWidth && height == viewportHeight) return;
        
        viewportWidth = width;
        viewportHeight = height;
        
        if (initialized) {
            // Recreate framebuffer with new dimensions
            destroyFramebuffer();
            initFramebuffer();
        }
    }
    
    /**
     * Enable the GPU plugin
     */
    public void enable() {
        if (!initialized && !initialize()) return;
        enabled = true;
        System.out.println("GPU rendering enabled");
    }
    
    /**
     * Disable the GPU plugin
     */
    public void disable() {
        enabled = false;
        System.out.println("GPU rendering disabled");
    }
    
    /**
     * Cleanup OpenGL resources
     */
    public void cleanup() {
        if (!initialized) return;
        
        destroyFramebuffer();
        
        if (vaoHandle != 0) glDeleteVertexArrays(vaoHandle);
        if (vertexBuffer != 0) glDeleteBuffers(vertexBuffer);
        if (uvBuffer != 0) glDeleteBuffers(uvBuffer);
        if (normalBuffer != 0) glDeleteBuffers(normalBuffer);
        if (glProgram != 0) glDeleteProgram(glProgram);
        
        initialized = false;
        System.out.println("GPU Plugin cleaned up");
    }
    
    private void destroyFramebuffer() {
        if (fboSceneHandle != 0) glDeleteFramebuffers(fboSceneHandle);
        if (rboSceneHandle != 0) glDeleteRenderbuffers(rboSceneHandle);
        if (textureSceneHandle != 0) glDeleteTextures(textureSceneHandle);
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public boolean isInitialized() { return initialized; }
    public int getVertexBuffer() { return vertexBuffer; }
    public int getUvBuffer() { return uvBuffer; }
    public int getNormalBuffer() { return normalBuffer; }
    public int getVaoHandle() { return vaoHandle; }
    
    public void setVertexCount(int count) { this.vertexCount = count; }
    public int getVertexCount() { return vertexCount; }
}