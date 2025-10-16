package com.client.plugins.gpu;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * OpenGL utility methods
 * Helper functions for common OpenGL operations
 */
public class GLUtil {
    
    /**
     * Create and compile a shader
     */
    public static int createShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        
        // Check for errors
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed: " + log);
        }
        
        return shader;
    }
    
    /**
     * Create and link a shader program
     */
    public static int createProgram(int vertexShader, int fragmentShader) {
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        
        // Check for errors
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new RuntimeException("Program linking failed: " + log);
        }
        
        return program;
    }
    
    /**
     * Create a VAO
     */
    public static int createVAO() {
        return glGenVertexArrays();
    }
    
    /**
     * Create a VBO
     */
    public static int createVBO() {
        return glGenBuffers();
    }
    
    /**
     * Upload float data to VBO
     */
    public static void uploadFloatData(int vbo, float[] data, int usage) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, usage);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    /**
     * Upload int data to VBO
     */
    public static void uploadIntData(int vbo, int[] data, int usage) {
        IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, usage);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
    
    /**
     * Create a framebuffer with texture and depth attachment
     */
    public static int createFramebuffer(int width, int height, 
                                       IntBuffer textureOut, IntBuffer depthOut) {
        int fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        
        // Color texture
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 
                    0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, 
                              GL_TEXTURE_2D, texture, 0);
        
        // Depth renderbuffer
        int depth = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depth);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, 
                                 GL_RENDERBUFFER, depth);
        
        // Check status
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer incomplete: " + status);
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
        if (textureOut != null) textureOut.put(0, texture);
        if (depthOut != null) depthOut.put(0, depth);
        
        return fbo;
    }
    
    /**
     * Check for OpenGL errors
     */
    public static void checkGLError(String location) {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            String errorString = getErrorString(error);
            System.err.println("OpenGL Error at " + location + ": " + errorString);
        }
    }
    
    /**
     * Get error string from error code
     */
    private static String getErrorString(int error) {
        switch (error) {
            case GL_INVALID_ENUM: return "GL_INVALID_ENUM";
            case GL_INVALID_VALUE: return "GL_INVALID_VALUE";
            case GL_INVALID_OPERATION: return "GL_INVALID_OPERATION";
            case GL_OUT_OF_MEMORY: return "GL_OUT_OF_MEMORY";
            case GL_INVALID_FRAMEBUFFER_OPERATION: return "GL_INVALID_FRAMEBUFFER_OPERATION";
            default: return "Unknown error: " + error;
        }
    }
    
    /**
     * Get GPU info string
     */
    public static String getGPUInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("OpenGL Vendor: ").append(glGetString(GL_VENDOR)).append("\n");
        sb.append("OpenGL Renderer: ").append(glGetString(GL_RENDERER)).append("\n");
        sb.append("OpenGL Version: ").append(glGetString(GL_VERSION)).append("\n");
        sb.append("GLSL Version: ").append(glGetString(GL_SHADING_LANGUAGE_VERSION));
        return sb.toString();
    }
    
    /**
     * Print GPU capabilities
     */
    public static void printCapabilities() {
        System.out.println("=== GPU Capabilities ===");
        System.out.println(getGPUInfo());
        System.out.println("Max Texture Size: " + glGetInteger(GL_MAX_TEXTURE_SIZE));
        System.out.println("Max Vertex Attributes: " + glGetInteger(GL_MAX_VERTEX_ATTRIBS));
        System.out.println("Max Texture Units: " + glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS));
        System.out.println("Max Viewport Dims: " + 
            glGetInteger(GL_MAX_VIEWPORT_DIMS) + "x" + 
            glGetInteger(GL_MAX_VIEWPORT_DIMS));
        System.out.println("========================");
    }
    
    /**
     * Create orthographic projection matrix
     */
    public static float[] createOrthographicMatrix(float left, float right, 
                                                   float bottom, float top, 
                                                   float near, float far) {
        float[] matrix = new float[16];
        
        matrix[0] = 2.0f / (right - left);
        matrix[5] = 2.0f / (top - bottom);
        matrix[10] = -2.0f / (far - near);
        matrix[12] = -(right + left) / (right - left);
        matrix[13] = -(top + bottom) / (top - bottom);
        matrix[14] = -(far + near) / (far - near);
        matrix[15] = 1.0f;
        
        return matrix;
    }
    
    /**
     * Create perspective projection matrix
     */
    public static float[] createPerspectiveMatrix(float fov, float aspect, 
                                                  float near, float far) {
        float[] matrix = new float[16];
        
        float f = (float) (1.0 / Math.tan(fov / 2.0));
        
        matrix[0] = f / aspect;
        matrix[5] = f;
        matrix[10] = (far + near) / (near - far);
        matrix[11] = -1.0f;
        matrix[14] = (2.0f * far * near) / (near - far);
        
        return matrix;
    }
    
    /**
     * Convert RGB color to float array
     */
    public static float[] colorToFloat(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0f;
        float g = ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (rgb & 0xFF) / 255.0f;
        return new float[] { r, g, b, 1.0f };
    }
    
    /**
     * Convert HSL to RGB
     */
    public static int hslToRgb(int hsl) {
        int h = (hsl >> 10) & 0x3F;
        int s = (hsl >> 7) & 0x7;
        int l = hsl & 0x7F;
        
        // Simple HSL to RGB conversion (can be improved)
        float hue = h / 63.0f;
        float sat = s / 7.0f;
        float lum = l / 127.0f;
        
        float r, g, b;
        
        if (sat == 0) {
            r = g = b = lum;
        } else {
            float q = lum < 0.5f ? lum * (1 + sat) : lum + sat - lum * sat;
            float p = 2 * lum - q;
            
            r = hueToRgb(p, q, hue + 1.0f/3.0f);
            g = hueToRgb(p, q, hue);
            b = hueToRgb(p, q, hue - 1.0f/3.0f);
        }
        
        int red = (int)(r * 255);
        int green = (int)(g * 255);
        int blue = (int)(b * 255);
        
        return (red << 16) | (green << 8) | blue;
    }
    
    private static float hueToRgb(float p, float q, float t) {
        if (t < 0) t += 1;
        if (t > 1) t -= 1;
        if (t < 1.0f/6.0f) return p + (q - p) * 6 * t;
        if (t < 1.0f/2.0f) return q;
        if (t < 2.0f/3.0f) return p + (q - p) * (2.0f/3.0f - t) * 6;
        return p;
    }
}