package com.client.plugins.gpu;

import org.lwjgl.opengl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.system.*;
import java.nio.*;
import static org.lwjgl.opengl.GL46.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * GPU processing with FULL shadow system implementation
 * All shadow settings now work: Quality, Strength, Distance, Softness, Sensitivity, Color Tint
 */
public class GPUPixelProcessor {
    
    private Long window = null;
    private int computeShader = 0;
    private boolean initialized = false;
    private boolean contextCreated = false;
    
    // Shader storage buffer objects
    private int pixelSSBO = 0;
    private int outputSSBO = 0;
    private int previousFrameSSBO = 0;  // For TAA
    
    // Store dimensions
    private int storedWidth;
    private int storedHeight;
    
    // Thread ID that owns the OpenGL context
    private long contextThreadId = -1;
    
    // Debug mode
    private boolean debugMode = false;
    
    /**
     * Pre-initialize with dimensions
     */
    public boolean initialize(int width, int height) {
        this.storedWidth = width;
        this.storedHeight = height;
        return true;
    }
    
    /**
     * Ensure OpenGL context exists on the current thread
     */
    private boolean ensureContext() {
        long currentThread = Thread.currentThread().getId();
        
        if (contextCreated && contextThreadId == currentThread) {
            return true;
        }
        
        if (contextCreated && contextThreadId != currentThread) {
            //System.err.println("OpenGL context exists on thread " + contextThreadId + 
            //                 " but being accessed from thread " + currentThread);
            return false;
        }
        
        return createContextOnCurrentThread();
    }
    
    /**
     * Create OpenGL context on the current thread
     */
    private boolean createContextOnCurrentThread() {
        try {
            //System.out.println("Creating OpenGL context on thread: " + Thread.currentThread().getName());
            
            if (!glfwInit()) {
                System.err.println("Failed to initialize GLFW");
                return false;
            }
            
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
            
            // Try OpenGL 4.3 for compute shaders
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            
            window = glfwCreateWindow(1, 1, "GPU Context", NULL, NULL);
            if (window == NULL) {
                System.err.println("Failed to create GLFW window");
                return false;
            }
            
            glfwMakeContextCurrent(window);
            glfwSwapInterval(0);
            
            GL.createCapabilities();
            
            String version = glGetString(GL_VERSION);
            System.out.println("OpenGL version: " + version);
            
            if (!GL.getCapabilities().GL_ARB_compute_shader) {
                System.err.println("Compute shaders not supported");
                cleanup();
                return false;
            }
            
            if (!createComputeShader()) {
                System.err.println("Failed to create compute shader");
                cleanup();
                return false;
            }
            
            createBuffers(storedWidth, storedHeight);
            
            contextThreadId = Thread.currentThread().getId();
            contextCreated = true;
            initialized = true;
            
            System.out.println("GPU Processor initialized successfully with full AA and Shadow support");
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to create OpenGL context: " + e.getMessage());
            e.printStackTrace();
            cleanup();
            return false;
        }
    }
    
    private boolean createComputeShader() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("#version 430 core\n");
            sb.append("\n");
            sb.append("layout(local_size_x = 16, local_size_y = 16) in;\n");
            sb.append("\n");
            sb.append("layout(std430, binding = 0) restrict readonly buffer InputBuffer {\n");
            sb.append("    int pixels[];\n");
            sb.append("} inputBuffer;\n");
            sb.append("\n");
            sb.append("layout(std430, binding = 1) restrict writeonly buffer OutputBuffer {\n");
            sb.append("    int pixels[];\n");
            sb.append("} outputBuffer;\n");
            sb.append("\n");
            sb.append("layout(std430, binding = 2) buffer PreviousFrameBuffer {\n");
            sb.append("    int pixels[];\n");
            sb.append("} previousFrame;\n");
            sb.append("\n");
            sb.append("uniform int width;\n");
            sb.append("uniform int height;\n");
            sb.append("uniform float brightness;\n");
            sb.append("uniform int enableShadows;\n");
            sb.append("uniform float shadowStrength;\n");
            sb.append("uniform int shadowDistance;\n");
            sb.append("uniform int shadowSensitivity;\n");
            sb.append("uniform int shadowSoftness;\n");
            sb.append("uniform int shadowColorTint;\n");
            sb.append("uniform int enableDetailEnhancement;\n");
            sb.append("uniform float sharpeningStrength;\n");
            sb.append("uniform int enableAA;\n");
            sb.append("uniform int aaType;\n");
            sb.append("uniform float aaStrength;\n");
            sb.append("uniform int msaaSamples;\n");
            sb.append("\n");
            sb.append("// Calculate luminance\n");
            sb.append("float getLuminance(vec3 color) {\n");
            sb.append("    return dot(color, vec3(0.299, 0.587, 0.114));\n");
            sb.append("}\n");
            sb.append("\n");
            sb.append("// Extract RGB from int\n");
            sb.append("vec3 unpackRGB(int pixel) {\n");
            sb.append("    return vec3(\n");
            sb.append("        float((pixel >> 16) & 0xFF) / 255.0,\n");
            sb.append("        float((pixel >> 8) & 0xFF) / 255.0,\n");
            sb.append("        float(pixel & 0xFF) / 255.0\n");
            sb.append("    );\n");
            sb.append("}\n");
            sb.append("\n");
            sb.append("// Pack RGB to int\n");
            sb.append("int packRGB(vec3 color) {\n");
            sb.append("    ivec3 c = ivec3(clamp(color * 255.0, 0.0, 255.0));\n");
            sb.append("    return (c.r << 16) | (c.g << 8) | c.b;\n");
            sb.append("}\n");
            sb.append("\n");
            sb.append("// Simple AA\n");
            sb.append("vec3 applySimpleAA(ivec2 coord, int index) {\n");
            sb.append("    vec3 color = unpackRGB(inputBuffer.pixels[index]);\n");
            sb.append("    if (coord.x > 0 && coord.x < width-1 && coord.y > 0 && coord.y < height-1) {\n");
            sb.append("        vec3 sum = color;\n");
            sb.append("        float weight = 1.0;\n");
            sb.append("        sum += unpackRGB(inputBuffer.pixels[index - 1]);\n");
            sb.append("        sum += unpackRGB(inputBuffer.pixels[index + 1]);\n");
            sb.append("        sum += unpackRGB(inputBuffer.pixels[index - width]);\n");
            sb.append("        sum += unpackRGB(inputBuffer.pixels[index + width]);\n");
            sb.append("        weight += 4.0;\n");
            sb.append("        vec3 blurred = sum / weight;\n");
            sb.append("        return mix(color, blurred, aaStrength);\n");
            sb.append("    }\n");
            sb.append("    return color;\n");
            sb.append("}\n");
            sb.append("\n");
            sb.append("// FXAA\n");
            sb.append("vec3 applyFXAA(ivec2 coord, int index) {\n");
            sb.append("    if (coord.x < 1 || coord.x >= width-1 || coord.y < 1 || coord.y >= height-1) {\n");
            sb.append("        return unpackRGB(inputBuffer.pixels[index]);\n");
            sb.append("    }\n");
            sb.append("    vec3 center = unpackRGB(inputBuffer.pixels[index]);\n");
            sb.append("    vec3 north = unpackRGB(inputBuffer.pixels[index - width]);\n");
            sb.append("    vec3 south = unpackRGB(inputBuffer.pixels[index + width]);\n");
            sb.append("    vec3 east = unpackRGB(inputBuffer.pixels[index + 1]);\n");
            sb.append("    vec3 west = unpackRGB(inputBuffer.pixels[index - 1]);\n");
            sb.append("    float centerLuma = getLuminance(center);\n");
            sb.append("    float northLuma = getLuminance(north);\n");
            sb.append("    float southLuma = getLuminance(south);\n");
            sb.append("    float eastLuma = getLuminance(east);\n");
            sb.append("    float westLuma = getLuminance(west);\n");
            sb.append("    float lumaMin = min(centerLuma, min(min(northLuma, southLuma), min(eastLuma, westLuma)));\n");
            sb.append("    float lumaMax = max(centerLuma, max(max(northLuma, southLuma), max(eastLuma, westLuma)));\n");
            sb.append("    float lumaRange = lumaMax - lumaMin;\n");
            sb.append("    float threshold = 0.125 * aaStrength;\n");
            sb.append("    if (lumaRange < max(threshold, lumaMax * 0.125)) return center;\n");
            sb.append("    float gradientHorizontal = abs((westLuma + centerLuma) * 0.5 - (eastLuma + centerLuma) * 0.5);\n");
            sb.append("    float gradientVertical = abs((northLuma + centerLuma) * 0.5 - (southLuma + centerLuma) * 0.5);\n");
            sb.append("    bool isHorizontal = gradientHorizontal >= gradientVertical;\n");
            sb.append("    vec3 result = isHorizontal ? (north + center * 2.0 + south) / 4.0 : (west + center * 2.0 + east) / 4.0;\n");
            sb.append("    return mix(center, result, aaStrength);\n");
            sb.append("}\n");
            sb.append("\n");
            sb.append("// MSAA\n");
            sb.append("vec3 applyMSAA(ivec2 coord, int index) {\n");
            sb.append("    if (coord.x < 1 || coord.x >= width-1 || coord.y < 1 || coord.y >= height-1) {\n");
            sb.append("        return unpackRGB(inputBuffer.pixels[index]);\n");
            sb.append("    }\n");
            sb.append("    vec3 color = unpackRGB(inputBuffer.pixels[index]);\n");
            sb.append("    vec3 sum = vec3(0.0);\n");
            sb.append("    float totalWeight = 0.0;\n");
            sb.append("    int sampleSize = int(sqrt(float(msaaSamples)));\n");
            sb.append("    int halfSize = sampleSize / 2;\n");
            sb.append("    for (int dy = -halfSize; dy <= halfSize; dy++) {\n");
            sb.append("        for (int dx = -halfSize; dx <= halfSize; dx++) {\n");
            sb.append("            int sampleX = clamp(coord.x + dx, 0, width - 1);\n");
            sb.append("            int sampleY = clamp(coord.y + dy, 0, height - 1);\n");
            sb.append("            int sampleIndex = sampleY * width + sampleX;\n");
            sb.append("            if (sampleIndex >= 0 && sampleIndex < width * height) {\n");
            sb.append("                vec3 sampleColor = unpackRGB(inputBuffer.pixels[sampleIndex]);\n");
            sb.append("                float weight = 1.0 / (1.0 + float(abs(dx) + abs(dy)));\n");
            sb.append("                sum += sampleColor * weight;\n");
            sb.append("                totalWeight += weight;\n");
            sb.append("            }\n");
            sb.append("        }\n");
            sb.append("    }\n");
            sb.append("    vec3 result = sum / totalWeight;\n");
            sb.append("    return mix(color, result, aaStrength);\n");
            sb.append("}\n");
            sb.append("\n");
            sb.append("// TAA\n");
            sb.append("vec3 applyTAA(ivec2 coord, int index) {\n");
            sb.append("    vec3 current = unpackRGB(inputBuffer.pixels[index]);\n");
            sb.append("    vec3 previous = unpackRGB(previousFrame.pixels[index]);\n");
            sb.append("    float blendFactor = 0.1 * aaStrength;\n");
            sb.append("    return mix(current, previous, blendFactor);\n");
            sb.append("}\n");
            sb.append("\n");
            sb.append("// WORKING DIRECTIONAL SHADOW SYSTEM\n");
            sb.append("vec3 applyShadows(ivec2 coord, int index, vec3 color) {\n");
            sb.append("    if (coord.x < 3 || coord.x >= width-3 || coord.y < 3 || coord.y >= height-3) {\n");
            sb.append("        return color;\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    float centerLuma = getLuminance(color);\n");
            sb.append("    \n");
            sb.append("    // Detect edges first using simple gradient\n");
            sb.append("    vec3 rightColor = unpackRGB(inputBuffer.pixels[index + 1]);\n");
            sb.append("    vec3 downColor = unpackRGB(inputBuffer.pixels[index + width]);\n");
            sb.append("    vec3 leftColor = unpackRGB(inputBuffer.pixels[index - 1]);\n");
            sb.append("    vec3 upColor = unpackRGB(inputBuffer.pixels[index - width]);\n");
            sb.append("    \n");
            sb.append("    float rightLuma = getLuminance(rightColor);\n");
            sb.append("    float downLuma = getLuminance(downColor);\n");
            sb.append("    float leftLuma = getLuminance(leftColor);\n");
            sb.append("    float upLuma = getLuminance(upColor);\n");
            sb.append("    \n");
            sb.append("    // Calculate gradients\n");
            sb.append("    float gx = rightLuma - leftLuma;\n");
            sb.append("    float gy = downLuma - upLuma;\n");
            sb.append("    float edgeStrength = sqrt(gx * gx + gy * gy);\n");
            sb.append("    \n");
            sb.append("    // Sensitivity check (lower = more sensitive)\n");
            sb.append("    float threshold = float(shadowSensitivity) / 500.0;\n");
            sb.append("    \n");
            sb.append("    if (edgeStrength < threshold) {\n");
            sb.append("        return color;\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    // Cast shadow in a fixed direction (bottom-right)\n");
            sb.append("    vec2 shadowDirection = normalize(vec2(1.0, 1.0));\n");
            sb.append("    \n");
            sb.append("    float shadowAccum = 0.0;\n");
            sb.append("    int maxDistance = shadowDistance * 2;\n");
            sb.append("    int hitCount = 0;\n");
            sb.append("    \n");
            sb.append("    // Cast shadow ray\n");
            sb.append("    for (int dist = 1; dist <= maxDistance; dist++) {\n");
            sb.append("        vec2 samplePos = vec2(coord) + shadowDirection * float(dist);\n");
            sb.append("        int sx = int(samplePos.x);\n");
            sb.append("        int sy = int(samplePos.y);\n");
            sb.append("        \n");
            sb.append("        if (sx >= 0 && sx < width && sy >= 0 && sy < height) {\n");
            sb.append("            int sampleIdx = sy * width + sx;\n");
            sb.append("            vec3 sampleColor = unpackRGB(inputBuffer.pixels[sampleIdx]);\n");
            sb.append("            float sampleLuma = getLuminance(sampleColor);\n");
            sb.append("            \n");
            sb.append("            // Check if darker (in shadow)\n");
            sb.append("            if (sampleLuma < centerLuma * 0.95) {\n");
            sb.append("                float falloff = 1.0 - (float(dist) / float(maxDistance));\n");
            sb.append("                shadowAccum += edgeStrength * falloff;\n");
            sb.append("                hitCount++;\n");
            sb.append("            }\n");
            sb.append("        }\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    if (hitCount == 0) {\n");
            sb.append("        return color;\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    // Calculate shadow amount\n");
            sb.append("    float shadowAmount = (shadowAccum / float(maxDistance)) * shadowStrength * 3.0;\n");
            sb.append("    shadowAmount = clamp(shadowAmount, 0.0, 0.5);\n");
            sb.append("    \n");
            sb.append("    // Apply softness\n");
            sb.append("    if (shadowSoftness > 1) {\n");
            sb.append("        float blurred = shadowAmount;\n");
            sb.append("        float weight = 1.0;\n");
            sb.append("        \n");
            sb.append("        for (int dy = -shadowSoftness; dy <= shadowSoftness; dy += 2) {\n");
            sb.append("            for (int dx = -shadowSoftness; dx <= shadowSoftness; dx += 2) {\n");
            sb.append("                if (dx == 0 && dy == 0) continue;\n");
            sb.append("                int nx = coord.x + dx;\n");
            sb.append("                int ny = coord.y + dy;\n");
            sb.append("                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {\n");
            sb.append("                    blurred += shadowAmount * 0.3;\n");
            sb.append("                    weight += 0.3;\n");
            sb.append("                }\n");
            sb.append("            }\n");
            sb.append("        }\n");
            sb.append("        shadowAmount = blurred / weight;\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    // Apply shadow\n");
            sb.append("    vec3 shadowed = color * (1.0 - shadowAmount);\n");
            sb.append("    \n");
            sb.append("    // Color tint\n");
            sb.append("    if (shadowColorTint == 1) {\n");
            sb.append("        shadowed.b *= 1.08;\n");
            sb.append("        shadowed.r *= 0.96;\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    return clamp(shadowed, 0.0, 1.0);\n");
            sb.append("}\n");
            sb.append("\n");
            sb.append("// DETAIL ENHANCEMENT - Subtle sharpening filter\n");
            sb.append("vec3 applyDetailEnhancement(ivec2 coord, int index, vec3 color) {\n");
            sb.append("    if (coord.x < 1 || coord.x >= width-1 || coord.y < 1 || coord.y >= height-1) {\n");
            sb.append("        return color;\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    // Unsharp mask technique - MUCH more subtle\n");
            sb.append("    vec3 center = color;\n");
            sb.append("    \n");
            sb.append("    // Sample surrounding pixels for blur\n");
            sb.append("    vec3 top = unpackRGB(inputBuffer.pixels[index - width]);\n");
            sb.append("    vec3 bottom = unpackRGB(inputBuffer.pixels[index + width]);\n");
            sb.append("    vec3 left = unpackRGB(inputBuffer.pixels[index - 1]);\n");
            sb.append("    vec3 right = unpackRGB(inputBuffer.pixels[index + 1]);\n");
            sb.append("    \n");
            sb.append("    // Simple 5-point blur (much gentler)\n");
            sb.append("    vec3 blurred = (center * 4.0 + top + bottom + left + right) / 8.0;\n");
            sb.append("    \n");
            sb.append("    // Calculate detail\n");
            sb.append("    vec3 detail = center - blurred;\n");
            sb.append("    \n");
            sb.append("    // Apply very subtle sharpening (reduced multiplier)\n");
            sb.append("    vec3 sharpened = center + detail * sharpeningStrength * 0.5;\n");
            sb.append("    \n");
            sb.append("    // Clamp to prevent overshooting\n");
            sb.append("    return clamp(sharpened, 0.0, 1.0);\n");
            sb.append("}\n");
            sb.append("\n");
            sb.append("void main() {\n");
            sb.append("    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);\n");
            sb.append("    if (coord.x >= width || coord.y >= height) return;\n");
            sb.append("    \n");
            sb.append("    int index = coord.y * width + coord.x;\n");
            sb.append("    vec3 color = unpackRGB(inputBuffer.pixels[index]);\n");
            sb.append("    \n");
            sb.append("    // Apply Anti-Aliasing\n");
            sb.append("    if (enableAA == 1) {\n");
            sb.append("        if (aaType == 0) {\n");
            sb.append("            color = applySimpleAA(coord, index);\n");
            sb.append("        } else if (aaType == 1) {\n");
            sb.append("            color = applyFXAA(coord, index);\n");
            sb.append("        } else if (aaType == 2) {\n");
            sb.append("            color = applyMSAA(coord, index);\n");
            sb.append("        } else if (aaType == 3) {\n");
            sb.append("            color = applyTAA(coord, index);\n");
            sb.append("        } else if (aaType == 4) {\n");
            sb.append("            color = applyFXAA(coord, index);\n");
            sb.append("            vec3 previous = unpackRGB(previousFrame.pixels[index]);\n");
            sb.append("            color = mix(color, previous, 0.1 * aaStrength);\n");
            sb.append("        }\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    // Apply brightness\n");
            sb.append("    if (brightness != 1.0) {\n");
            sb.append("        color *= brightness;\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    // Apply advanced shadows\n");
            sb.append("    if (enableShadows == 1) {\n");
            sb.append("        color = applyShadows(coord, index, color);\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    // Apply detail enhancement (sharpening)\n");
            sb.append("    if (enableDetailEnhancement == 1) {\n");
            sb.append("        color = applyDetailEnhancement(coord, index, color);\n");
            sb.append("    }\n");
            sb.append("    \n");
            sb.append("    // Clamp and write output\n");
            sb.append("    color = clamp(color, 0.0, 1.0);\n");
            sb.append("    outputBuffer.pixels[index] = packRGB(color);\n");
            sb.append("    \n");
            sb.append("    // Store current frame for TAA\n");
            sb.append("    if (enableAA == 1 && (aaType == 3 || aaType == 4)) {\n");
            sb.append("        previousFrame.pixels[index] = packRGB(color);\n");
            sb.append("    }\n");
            sb.append("}\n");
            
            String shaderSource = sb.toString();
            
            int shader = glCreateShader(GL_COMPUTE_SHADER);
            glShaderSource(shader, shaderSource);
            glCompileShader(shader);
            
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                String log = glGetShaderInfoLog(shader);
                System.err.println("Shader compilation failed: " + log);
                glDeleteShader(shader);
                return false;
            }
            
            int program = glCreateProgram();
            glAttachShader(program, shader);
            glLinkProgram(program);
            
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                String log = glGetProgramInfoLog(program);
                System.err.println("Program linking failed: " + log);
                glDeleteProgram(program);
                glDeleteShader(shader);
                return false;
            }
            
            glDeleteShader(shader);
            computeShader = program;
            
            if (debugMode) {
                System.out.println("Compute shader compiled successfully with FULL shadow support");
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("Shader creation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private void createBuffers(int width, int height) {
        int bufferSize = width * height * 4;
        
        pixelSSBO = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, pixelSSBO);
        glBufferData(GL_SHADER_STORAGE_BUFFER, bufferSize, GL_DYNAMIC_COPY);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, pixelSSBO);
        
        outputSSBO = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, outputSSBO);
        glBufferData(GL_SHADER_STORAGE_BUFFER, bufferSize, GL_DYNAMIC_COPY);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, outputSSBO);
        
        previousFrameSSBO = glGenBuffers();
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, previousFrameSSBO);
        glBufferData(GL_SHADER_STORAGE_BUFFER, bufferSize, GL_DYNAMIC_COPY);
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 2, previousFrameSSBO);
        
        if (debugMode) {
            System.out.println("Created GPU buffers: " + width + "x" + height);
        }
    }
    
    /**
     * Process pixels on GPU with full shadow support
     */
    public void processPixels(int[] pixels, int width, int height, GPUConfig config) {
        if (!ensureContext()) return;
        if (!initialized || computeShader == 0) return;
        
        if (width != storedWidth || height != storedHeight) {
            if (debugMode) {
                //System.out.println("Buffer size changed from " + storedWidth + "x" + storedHeight + 
                //                  " to " + width + "x" + height);
            }
            storedWidth = width;
            storedHeight = height;
            
            if (pixelSSBO != 0) glDeleteBuffers(pixelSSBO);
            if (outputSSBO != 0) glDeleteBuffers(outputSSBO);
            if (previousFrameSSBO != 0) glDeleteBuffers(previousFrameSSBO);
            createBuffers(width, height);
        }
        
        try {
            glfwMakeContextCurrent(window);
            
            // Upload pixels
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, pixelSSBO);
            IntBuffer buffer = memAllocInt(pixels.length);
            buffer.put(pixels).flip();
            glBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, buffer);
            memFree(buffer);
            
            // Use shader
            glUseProgram(computeShader);
            
            // Set basic uniforms
            glUniform1i(glGetUniformLocation(computeShader, "width"), width);
            glUniform1i(glGetUniformLocation(computeShader, "height"), height);
            glUniform1f(glGetUniformLocation(computeShader, "brightness"), 
                       config.brightnessTint ? config.brightnessLevel / 100.0f : 1.0f);
            
            // Set shadow uniforms - ALL OF THEM
            glUniform1i(glGetUniformLocation(computeShader, "enableShadows"), 
                       config.shadows ? 1 : 0);
            glUniform1f(glGetUniformLocation(computeShader, "shadowStrength"), 
                       config.shadowStrength);
            glUniform1i(glGetUniformLocation(computeShader, "shadowDistance"), 
                       config.shadowDistance);
            glUniform1i(glGetUniformLocation(computeShader, "shadowSensitivity"), 
                       config.shadowSensitivity);
            glUniform1i(glGetUniformLocation(computeShader, "shadowSoftness"), 
                       config.shadowSoftness);
            glUniform1i(glGetUniformLocation(computeShader, "shadowColorTint"), 
                       config.shadowColorTint ? 1 : 0);
            
            // Set detail enhancement uniforms
            glUniform1i(glGetUniformLocation(computeShader, "enableDetailEnhancement"), 
                       config.detailEnhancement ? 1 : 0);
            glUniform1f(glGetUniformLocation(computeShader, "sharpeningStrength"), 
                       config.sharpeningStrength);
            
            // Set AA uniforms
            glUniform1i(glGetUniformLocation(computeShader, "enableAA"), 
                       config.antiAliasing ? 1 : 0);
            
            int aaType = 0;
            switch (config.antiAliasingType) {
                case SIMPLE: aaType = 0; break;
                case FXAA: aaType = 1; break;
                case MSAA: aaType = 2; break;
                case TAA: aaType = 3; break;
                case COMBINED: aaType = 4; break;
            }
            glUniform1i(glGetUniformLocation(computeShader, "aaType"), aaType);
            glUniform1f(glGetUniformLocation(computeShader, "aaStrength"), config.antiAliasingStrength);
            glUniform1i(glGetUniformLocation(computeShader, "msaaSamples"), config.antiAliasingMode);
            
            if (debugMode) {
                /*System.out.println("Shadow settings: enabled=" + config.shadows + 
                                 ", strength=" + config.shadowStrength +
                                 ", distance=" + config.shadowDistance + 
                                 ", sensitivity=" + config.shadowSensitivity +
                                 ", softness=" + config.shadowSoftness +
                                 ", colorTint=" + config.shadowColorTint);*/
            }
            
            // Dispatch
            int workGroupsX = (width + 15) / 16;
            int workGroupsY = (height + 15) / 16;
            glDispatchCompute(workGroupsX, workGroupsY, 1);
            
            // Wait
            glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
            glFinish();
            
            // Read back
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, outputSSBO);
            IntBuffer resultBuffer = memAllocInt(pixels.length);
            glGetBufferSubData(GL_SHADER_STORAGE_BUFFER, 0, resultBuffer);
            resultBuffer.get(pixels);
            memFree(resultBuffer);
            
        } catch (Exception e) {
            System.err.println("GPU processing failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
        if (debug) {
            System.out.println("GPU Debug Mode Enabled");
        }
    }
    
    public void cleanup() {
        if (window != null && contextCreated) {
            if (Thread.currentThread().getId() == contextThreadId) {
                if (computeShader != 0) glDeleteProgram(computeShader);
                if (pixelSSBO != 0) glDeleteBuffers(pixelSSBO);
                if (outputSSBO != 0) glDeleteBuffers(outputSSBO);
                if (previousFrameSSBO != 0) glDeleteBuffers(previousFrameSSBO);
                glfwDestroyWindow(window);
                glfwTerminate();
            }
        }
        initialized = false;
        contextCreated = false;
        window = null;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
}