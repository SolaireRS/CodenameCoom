package com.client.plugins.gpu;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

/**
 * OpenGL constants wrapper - fallback when LWJGL not available
 */
class GLConstants {
    public static final int GL_TRIANGLES = 0x0004;
    public static final int GL_LINE = 0x1B01;
    public static final int GL_FILL = 0x1B02;
    public static final int GL_SRC_ALPHA = 0x0302;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 0x0303;
    public static final int GL_DEPTH_TEST = 0x0B71;
    public static final int GL_CULL_FACE = 0x0B44;
    public static final int GL_BLEND = 0x0BE2;
    public static final int GL_TEXTURE_2D = 0x0DE1;
    public static final int GL_FRAMEBUFFER = 0x8D40;
    public static final int GL_COLOR_ATTACHMENT0 = 0x8CE0;
    public static final int GL_DEPTH_ATTACHMENT = 0x8D00;
}

/**
 * OpenGL context wrapper with graceful fallback
 */
class GPUContext {
    private boolean initialized = false;
    private boolean lwjglAvailable = false;
    
    public boolean initialize() {
        try {
            GL.createCapabilities();
            
            String version = glGetString(GL_VERSION);
            String renderer = glGetString(GL_RENDERER);
            String vendor = glGetString(GL_VENDOR);
            
            System.out.println("OpenGL Context Found:");
            System.out.println("  Version: " + version);
            System.out.println("  Renderer: " + renderer);
            System.out.println("  Vendor: " + vendor);
            
            lwjglAvailable = true;
            initialized = true;
            return true;
            
        } catch (Exception e) {
            lwjglAvailable = false;
            initialized = true;
            return true;
        }
    }
    
    public boolean isHardwareAccelerated() {
        return lwjglAvailable;
    }
    
    public void cleanup() {
        initialized = false;
        lwjglAvailable = false;
    }
    
    public void clearBuffers() {
        if (lwjglAvailable) {
            try {
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            } catch (Exception e) {
                System.err.println("OpenGL clear failed: " + e.getMessage());
            }
        }
    }
    
    public void swapBuffers() {}
    
    public void enableDepthTest() {
        if (lwjglAvailable) {
            try {
                glEnable(GL_DEPTH_TEST);
            } catch (Exception e) {
                System.err.println("OpenGL depth test enable failed: " + e.getMessage());
            }
        }
    }
    
    public void enableCulling() {
        if (lwjglAvailable) {
            try {
                glEnable(GL_CULL_FACE);
                glCullFace(GL_BACK);
            } catch (Exception e) {
                System.err.println("OpenGL culling enable failed: " + e.getMessage());
            }
        }
    }
    
    public void enableBlending() {
        if (lwjglAvailable) {
            try {
                glEnable(GL_BLEND);
            } catch (Exception e) {
                System.err.println("OpenGL blending enable failed: " + e.getMessage());
            }
        }
    }
    
    public void setBlendFunc(int src, int dst) {
        if (lwjglAvailable) {
            try {
                glBlendFunc(src, dst);
            } catch (Exception e) {
                System.err.println("OpenGL blend func failed: " + e.getMessage());
            }
        }
    }
    
    public void setClearColor(float r, float g, float b, float a) {
        if (lwjglAvailable) {
            try {
                glClearColor(r, g, b, a);
            } catch (Exception e) {
                System.err.println("OpenGL clear color failed: " + e.getMessage());
            }
        }
    }
    
    public void setViewport(int x, int y, int width, int height) {
        if (lwjglAvailable) {
            try {
                glViewport(x, y, width, height);
            } catch (Exception e) {
                System.err.println("OpenGL viewport failed: " + e.getMessage());
            }
        }
    }
    
    public void drawArrays(int mode, int first, int count) {
        if (lwjglAvailable) {
            try {
                glDrawArrays(mode, first, count);
            } catch (Exception e) {
                System.err.println("OpenGL draw arrays failed: " + e.getMessage());
            }
        }
    }
    
    public void setPolygonMode(int mode) {
        if (lwjglAvailable) {
            try {
                glPolygonMode(GL_FRONT_AND_BACK, mode);
            } catch (Exception e) {
                System.err.println("OpenGL polygon mode failed: " + e.getMessage());
            }
        }
    }
    
    public void bindTexture(int target, int texture) {
        if (lwjglAvailable) {
            try {
                glBindTexture(target, texture);
            } catch (Exception e) {
                System.err.println("OpenGL bind texture failed: " + e.getMessage());
            }
        }
    }
    
    public void useProgram(int program) {
        if (lwjglAvailable) {
            try {
                glUseProgram(program);
            } catch (Exception e) {
                System.err.println("OpenGL use program failed: " + e.getMessage());
            }
        }
    }
    
    public void bindVertexArray(int vao) {
        if (lwjglAvailable) {
            try {
                glBindVertexArray(vao);
            } catch (Exception e) {
                System.err.println("OpenGL bind VAO failed: " + e.getMessage());
            }
        }
    }
    
    public void bindBuffer(int target, int buffer) {
        if (lwjglAvailable) {
            try {
                glBindBuffer(target, buffer);
            } catch (Exception e) {
                System.err.println("OpenGL bind buffer failed: " + e.getMessage());
            }
        }
    }
    
    public void bindFramebuffer(int target, int framebuffer) {
        if (lwjglAvailable) {
            try {
                glBindFramebuffer(target, framebuffer);
            } catch (Exception e) {
                System.err.println("OpenGL bind framebuffer failed: " + e.getMessage());
            }
        }
    }
    
    public boolean checkGLError() {
        if (!lwjglAvailable) return true;
        
        try {
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                System.err.println("OpenGL Error: " + error);
                return false;
            }
            return true;
        } catch (Exception e) {
            System.err.println("OpenGL error check failed: " + e.getMessage());
            return false;
        }
    }
}

/**
 * GPU capabilities checker with graceful fallback
 */
class GPUCapabilities {
    private String renderer = "Software Renderer";
    private String version = "Software Mode";
    private String vendor = "CPU";
    private Set<String> extensions;
    private boolean hardwareAccelerated = false;
    
    public boolean check() {
        try {
            renderer = glGetString(GL_RENDERER);
            version = glGetString(GL_VERSION);
            vendor = glGetString(GL_VENDOR);
            
            extensions = new HashSet<>();
            
            if (version != null && version.length() > 0) {
                String[] versionParts = version.split("\\.");
                if (versionParts.length >= 2) {
                    try {
                        int major = Integer.parseInt(versionParts[0]);
                        int minor = Integer.parseInt(versionParts[1].split(" ")[0]);
                        
                        if (major >= 3 && (major > 3 || minor >= 3)) {
                            hardwareAccelerated = true;
                            System.out.println("Hardware acceleration available: OpenGL " + version);
                        } else {
                            System.out.println("OpenGL 3.3+ required for hardware acceleration, found: " + version);
                            System.out.println("Falling back to software mode");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Could not parse OpenGL version: " + version);
                        System.out.println("Using software mode");
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            System.out.println("Hardware acceleration not available - using software mode");
            renderer = "Software Renderer";
            version = "Software Mode";
            vendor = "CPU";
            hardwareAccelerated = false;
            extensions = new HashSet<>();
            return true;
        }
    }
    
    public boolean isHardwareAccelerated() { return hardwareAccelerated; }
    public String getRendererString() { return renderer != null ? renderer : "Software Renderer"; }
    public String getVersionString() { return version != null ? version : "Software Mode"; }
    public String getVendorString() { return vendor != null ? vendor : "CPU"; }
    public Set<String> getExtensions() { return extensions != null ? extensions : new HashSet<>(); }
}

/**
 * Camera data structure
 */
class Camera {
    public float[] position = {0, 0, 0};
    public float[] rotation = {0, 0, 0};
    public float[] viewMatrix = new float[16];
    public float[] projectionMatrix = new float[16];
    public float fov = 60.0f;
    public float nearPlane = 0.1f;
    public float farPlane = 1000.0f;
}

/**
 * Renderable object data
 */
class RenderObject {
    public int vertexArrayObject;
    public int vertexCount;
    public int textureId;
    public float[] modelMatrix = new float[16];
    public float[] color = {1, 1, 1, 1};
    public int materialId;
    public boolean castsShadows = true;
    public boolean receiveShadows = true;
}

/**
 * Light data structure
 */
class Light {
    public enum Type { DIRECTIONAL, POINT, SPOT }
    
    public Type type = Type.DIRECTIONAL;
    public float[] position = {0, 100, 0};
    public float[] direction = {0, -1, 0};
    public float[] color = {1, 1, 1};
    public float intensity = 1.0f;
    public float range = 100.0f;
    public float spotAngle = 45.0f;
}

/**
 * Shader program manager with graceful fallback
 */
class ShaderManager {
    private GPUContext glContext;
    private Map<Integer, String> programs;
    private int currentProgram = 0;
    private boolean hardwareMode = false;
    
    public ShaderManager(GPUContext context) {
        this.glContext = context;
        this.programs = new ConcurrentHashMap<>();
        this.hardwareMode = context.isHardwareAccelerated();
    }
    
    public int createProgram(String vertexSource, String fragmentSource) {
        if (!hardwareMode) {
            int dummyId = programs.size() + 1;
            programs.put(dummyId, "Software Program " + dummyId);
            return dummyId;
        }
        
        try {
            int vertexShader = compileShader(vertexSource, GL_VERTEX_SHADER);
            if (vertexShader == 0) return 0;
            
            int fragmentShader = compileShader(fragmentSource, GL_FRAGMENT_SHADER);
            if (fragmentShader == 0) {
                glDeleteShader(vertexShader);
                return 0;
            }
            
            int program = linkProgram(vertexShader, fragmentShader);
            
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            
            if (program != 0) {
                programs.put(program, "Hardware Program " + program);
            }
            
            return program;
        } catch (Exception e) {
            System.err.println("Shader program creation failed: " + e.getMessage());
            return 0;
        }
    }
    
    private int compileShader(String source, int type) {
        if (!hardwareMode) return 1;
        
        try {
            int shader = glCreateShader(type);
            glShaderSource(shader, source);
            glCompileShader(shader);
            
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                String log = glGetShaderInfoLog(shader);
                System.err.println("Shader compilation failed (" + 
                    (type == GL_VERTEX_SHADER ? "vertex" : "fragment") + "): " + log);
                glDeleteShader(shader);
                return 0;
            }
            
            return shader;
        } catch (Exception e) {
            System.err.println("Shader compilation error: " + e.getMessage());
            return 0;
        }
    }
    
    private int linkProgram(int vertexShader, int fragmentShader) {
        if (!hardwareMode) return 1;
        
        try {
            int program = glCreateProgram();
            glAttachShader(program, vertexShader);
            glAttachShader(program, fragmentShader);
            glLinkProgram(program);
            
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                String log = glGetProgramInfoLog(program);
                System.err.println("Program linking failed: " + log);
                glDeleteProgram(program);
                return 0;
            }
            
            return program;
        } catch (Exception e) {
            System.err.println("Program linking error: " + e.getMessage());
            return 0;
        }
    }
    
    public void useProgram(int program) {
        currentProgram = program;
        if (hardwareMode) {
            glContext.useProgram(program);
        }
    }
    
    public void setUniform(String name, boolean value) {
        if (!hardwareMode) return;
        
        try {
            int location = glGetUniformLocation(currentProgram, name);
            if (location != -1) {
                glUniform1i(location, value ? 1 : 0);
            }
        } catch (Exception e) {
            System.err.println("Set uniform boolean failed: " + e.getMessage());
        }
    }
    
    public void setUniform(String name, int value) {
        if (!hardwareMode) return;
        
        try {
            int location = glGetUniformLocation(currentProgram, name);
            if (location != -1) {
                glUniform1i(location, value);
            }
        } catch (Exception e) {
            System.err.println("Set uniform int failed: " + e.getMessage());
        }
    }
    
    public void setUniform(String name, float value) {
        if (!hardwareMode) return;
        
        try {
            int location = glGetUniformLocation(currentProgram, name);
            if (location != -1) {
                glUniform1f(location, value);
            }
        } catch (Exception e) {
            System.err.println("Set uniform float failed: " + e.getMessage());
        }
    }
    
    public void setUniform(String name, float[] values) {
        if (!hardwareMode) return;
        
        try {
            int location = glGetUniformLocation(currentProgram, name);
            if (location != -1) {
                if (values.length == 3) {
                    glUniform3fv(location, values);
                } else if (values.length == 4) {
                    glUniform4fv(location, values);
                } else if (values.length == 16) {
                    glUniformMatrix4fv(location, false, values);
                }
            }
        } catch (Exception e) {
            System.err.println("Set uniform array failed: " + e.getMessage());
        }
    }
    
    public void cleanup() {
        if (hardwareMode) {
            for (int program : programs.keySet()) {
                try {
                    glDeleteProgram(program);
                } catch (Exception e) {
                    System.err.println("Program cleanup failed: " + e.getMessage());
                }
            }
        }
        programs.clear();
    }
}

/**
 * Buffer manager with graceful fallback
 */
class BufferManager {
    private GPUContext glContext;
    private Set<Integer> buffers;
    private boolean hardwareMode = false;
    
    public BufferManager(GPUContext context) {
        this.glContext = context;
        this.buffers = new HashSet<>();
        this.hardwareMode = context.isHardwareAccelerated();
    }
    
    public int createVertexBuffer(FloatBuffer data) {
        if (!hardwareMode) {
            int dummyId = buffers.size() + 1;
            buffers.add(dummyId);
            return dummyId;
        }
        
        try {
            int vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
            buffers.add(vbo);
            return vbo;
        } catch (Exception e) {
            System.err.println("Vertex buffer creation failed: " + e.getMessage());
            return 0;
        }
    }
    
    public int createIndexBuffer(IntBuffer indices) {
        if (!hardwareMode) {
            int dummyId = buffers.size() + 1;
            buffers.add(dummyId);
            return dummyId;
        }
        
        try {
            int ebo = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
            buffers.add(ebo);
            return ebo;
        } catch (Exception e) {
            System.err.println("Index buffer creation failed: " + e.getMessage());
            return 0;
        }
    }
    
    public void bindVertexBuffer(int buffer) {
        if (hardwareMode) {
            glContext.bindBuffer(GL_ARRAY_BUFFER, buffer);
        }
    }
    
    public void cleanup() {
        if (hardwareMode) {
            for (int buffer : buffers) {
                try {
                    glDeleteBuffers(buffer);
                } catch (Exception e) {
                    System.err.println("Buffer cleanup failed: " + e.getMessage());
                }
            }
        }
        buffers.clear();
    }
}

/**
 * VAO manager with graceful fallback
 */
class VAOManager {
    private GPUContext glContext;
    private Set<Integer> vaos;
    private int fullscreenQuadVAO = 0;
    private boolean hardwareMode = false;
    
    public VAOManager(GPUContext context) {
        this.glContext = context;
        this.vaos = new HashSet<>();
        this.hardwareMode = context.isHardwareAccelerated();
        createFullscreenQuad();
    }
    
    public int createVAO() {
        if (!hardwareMode) {
            int dummyId = vaos.size() + 1;
            vaos.add(dummyId);
            return dummyId;
        }
        
        try {
            int vao = glGenVertexArrays();
            vaos.add(vao);
            return vao;
        } catch (Exception e) {
            System.err.println("VAO creation failed: " + e.getMessage());
            return 0;
        }
    }
    
    public void bindVAO(int vao) {
        if (hardwareMode) {
            glContext.bindVertexArray(vao);
        }
    }
    
    public void setupVertexAttribute(int index, int size, int type, boolean normalized, int stride, long offset) {
        if (!hardwareMode) return;
        
        try {
            glVertexAttribPointer(index, size, type, normalized, stride, offset);
            glEnableVertexAttribArray(index);
        } catch (Exception e) {
            System.err.println("Vertex attribute setup failed: " + e.getMessage());
        }
    }
    
    private void createFullscreenQuad() {
        if (!hardwareMode) {
            fullscreenQuadVAO = 1;
            return;
        }
        
        try {
            fullscreenQuadVAO = createVAO();
            bindVAO(fullscreenQuadVAO);
            
            float[] vertices = {
                -1.0f, -1.0f, 0.0f, 0.0f,
                 3.0f, -1.0f, 2.0f, 0.0f,
                -1.0f,  3.0f, 0.0f, 2.0f
            };
            
            FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
            vertexBuffer.put(vertices).flip();
            
            int vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
            
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
            glEnableVertexAttribArray(0);
            
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
            glEnableVertexAttribArray(1);
            
            MemoryUtil.memFree(vertexBuffer);
        } catch (Exception e) {
            System.err.println("Fullscreen quad creation failed: " + e.getMessage());
            fullscreenQuadVAO = 1;
        }
    }
    
    public void bindFullscreenQuad() {
        bindVAO(fullscreenQuadVAO);
    }
    
    public void cleanup() {
        if (hardwareMode) {
            for (int vao : vaos) {
                try {
                    glDeleteVertexArrays(vao);
                } catch (Exception e) {
                    System.err.println("VAO cleanup failed: " + e.getMessage());
                }
            }
        }
        vaos.clear();
    }
}

/**
 * Texture manager with graceful fallback
 */
class TextureManager {
    private GPUContext glContext;
    private GPUConfig config;
    private Map<Integer, TextureData> textures;
    private boolean hardwareMode = false;
    
    public TextureManager(GPUContext context, GPUConfig config) {
        this.glContext = context;
        this.config = config;
        this.textures = new ConcurrentHashMap<>();
        this.hardwareMode = context.isHardwareAccelerated();
    }
    
    public int createTexture(int width, int height, ByteBuffer data) {
        if (!hardwareMode) {
            int dummyId = textures.size() + 1;
            textures.put(dummyId, new TextureData(width, height));
            return dummyId;
        }
        
        try {
            int texture = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, texture);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
            
            setupTextureFiltering();
            
            textures.put(texture, new TextureData(width, height));
            return texture;
        } catch (Exception e) {
            System.err.println("Texture creation failed: " + e.getMessage());
            return 0;
        }
    }
    
    private void setupTextureFiltering() {
        if (!hardwareMode) return;
        
        try {
            switch (config.textureDetail) {
                case 0:
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                    break;
                case 1:
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    break;
                case 2:
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
                    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    glGenerateMipmap(GL_TEXTURE_2D);
                    break;
            }
            
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        } catch (Exception e) {
            System.err.println("Texture filtering setup failed: " + e.getMessage());
        }
    }
    
    public void bindTexture(int texture) {
        if (hardwareMode) {
            glContext.bindTexture(GL_TEXTURE_2D, texture);
        }
    }
    
    public void cleanup() {
        if (hardwareMode) {
            for (int texture : textures.keySet()) {
                try {
                    glDeleteTextures(texture);
                } catch (Exception e) {
                    System.err.println("Texture cleanup failed: " + e.getMessage());
                }
            }
        }
        textures.clear();
    }
    
    private static class TextureData {
        public final int width;
        public final int height;
        
        public TextureData(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}

/**
 * Framebuffer manager with graceful fallback
 */
class FramebufferManager {
    private GPUContext glContext;
    private GPUConfig config;
    private int mainFramebuffer = 0;
    private int postProcessFramebuffer = 0;
    private int shadowMapFramebuffer = 0;
    private boolean hardwareMode = false;
    
    public FramebufferManager(GPUContext context, GPUConfig config) {
        this.glContext = context;
        this.config = config;
        this.hardwareMode = context.isHardwareAccelerated();
        createFramebuffers();
    }
    
    private void createFramebuffers() {
        if (!hardwareMode) {
            postProcessFramebuffer = 1;
            shadowMapFramebuffer = 2;
            return;
        }
        
        try {
            postProcessFramebuffer = glGenFramebuffers();
            shadowMapFramebuffer = glGenFramebuffers();
        } catch (Exception e) {
            System.err.println("Framebuffer creation failed: " + e.getMessage());
            postProcessFramebuffer = 1;
            shadowMapFramebuffer = 2;
        }
    }
    
    public void bindMainFramebuffer() {
        if (hardwareMode) {
            glContext.bindFramebuffer(GL_FRAMEBUFFER, 0);
        }
    }
    
    public void bindPostProcessFramebuffer() {
        if (hardwareMode) {
            glContext.bindFramebuffer(GL_FRAMEBUFFER, postProcessFramebuffer);
        }
    }
    
    public void bindShadowMapFramebuffer() {
        if (hardwareMode) {
            glContext.bindFramebuffer(GL_FRAMEBUFFER, shadowMapFramebuffer);
        }
    }
    
    public void cleanup() {
        if (hardwareMode) {
            try {
                if (postProcessFramebuffer != 0) glDeleteFramebuffers(postProcessFramebuffer);
                if (shadowMapFramebuffer != 0) glDeleteFramebuffers(shadowMapFramebuffer);
            } catch (Exception e) {
                System.err.println("Framebuffer cleanup failed: " + e.getMessage());
            }
        }
    }
}

/**
 * Performance tracking
 */
class PerformanceTracker {
    private long frameStartTime;
    private long[] frameTimes = new long[60];
    private int frameIndex = 0;
    private double averageFPS = 0.0;
    
    public void startFrame() {
        frameStartTime = System.nanoTime();
    }
    
    public void endFrame() {
        long frameTime = System.nanoTime() - frameStartTime;
        frameTimes[frameIndex] = frameTime;
        frameIndex = (frameIndex + 1) % frameTimes.length;
        
        long totalTime = 0;
        for (long time : frameTimes) {
            totalTime += time;
        }
        averageFPS = 1_000_000_000.0 / (totalTime / (double) frameTimes.length);
    }
    
    public double getAverageFPS() {
        return averageFPS;
    }
    
    public long getLastFrameTime() {
        return frameTimes[(frameIndex - 1 + frameTimes.length) % frameTimes.length];
    }
}

/**
 * Scene renderer - handles rendering of 3D objects
 */
class SceneRenderer {
    private GPURenderEngine engine;
    private boolean enabled = false;
    
    public SceneRenderer(GPURenderEngine engine) {
        this.engine = engine;
    }
    
    public void enable() {
        enabled = true;
        System.out.println("Scene renderer enabled");
    }
    
    public void disable() {
        enabled = false;
        System.out.println("Scene renderer disabled");
    }
    
    public void renderScene(SceneData sceneData) {
        if (!enabled) return;
        
        setupMatrices(sceneData.camera);
        
        for (RenderObject obj : sceneData.objects) {
            renderObject(obj, sceneData);
        }
    }
    
    private void setupMatrices(Camera camera) {
        if (engine.getShaderManager() != null) {
            engine.getShaderManager().setUniform("u_viewMatrix", camera.viewMatrix);
            engine.getShaderManager().setUniform("u_projectionMatrix", camera.projectionMatrix);
            engine.getShaderManager().setUniform("u_cameraPos", camera.position);
        }
    }
    
    private void renderObject(RenderObject obj, SceneData sceneData) {
        try {
            if (engine.getShaderManager() != null) {
                engine.getShaderManager().setUniform("u_modelMatrix", obj.modelMatrix);
                
                if (obj.textureId > 0 && engine.getTextureManager() != null) {
                    engine.getTextureManager().bindTexture(obj.textureId);
                    engine.getShaderManager().setUniform("u_hasTexture", true);
                } else {
                    engine.getShaderManager().setUniform("u_hasTexture", false);
                }
            }
            
            if (engine.getVAOManager() != null && engine.getGLContext() != null) {
                engine.getVAOManager().bindVAO(obj.vertexArrayObject);
                engine.getGLContext().drawArrays(GLConstants.GL_TRIANGLES, 0, obj.vertexCount);
            }
        } catch (Exception e) {
            System.err.println("Object rendering failed: " + e.getMessage());
        }
    }
    
    public void cleanup() {
        enabled = false;
        System.out.println("Scene renderer cleaned up");
    }
}

/**
 * Scene data passed to renderer each frame
 */
class SceneData {
    public int viewportWidth;
    public int viewportHeight;
    public Camera camera;
    public List<RenderObject> objects;
    public List<Light> lights;
    public float deltaTime;
    public long frameCount;
    
    public SceneData() {
        objects = new ArrayList<>();
        lights = new ArrayList<>();
        camera = new Camera();
    }
}

/**
 * Lighting system - handles dynamic lighting calculations
 */
class LightingSystem {
    private GPURenderEngine engine;
    private boolean enabled = false;
    private List<Light> activeLights;
    
    public LightingSystem(GPURenderEngine engine) {
        this.engine = engine;
        this.activeLights = new ArrayList<>();
    }
    
    public void enable() {
        enabled = true;
        System.out.println("Lighting system enabled");
    }
    
    public void disable() {
        enabled = false;
        System.out.println("Lighting system disabled");
    }
    
    public void updateUniforms(GPUConfig config) {
        if (!enabled || engine.getShaderManager() == null) return;
        
        try {
            engine.getShaderManager().setUniform("u_ambientStrength", config.ambientStrength);
            engine.getShaderManager().setUniform("u_lightDistance", config.lightDistance);
            
            if (!activeLights.isEmpty()) {
                Light primaryLight = activeLights.get(0);
                engine.getShaderManager().setUniform("u_lightDirection", primaryLight.direction);
                engine.getShaderManager().setUniform("u_lightColor", primaryLight.color);
                engine.getShaderManager().setUniform("u_lightIntensity", primaryLight.intensity);
            }
            
            float[] ambientColor = calculateAmbientColor();
            engine.getShaderManager().setUniform("u_ambientColor", ambientColor);
        } catch (Exception e) {
            System.err.println("Lighting uniform update failed: " + e.getMessage());
        }
    }
    
    private float[] calculateAmbientColor() {
        return new float[]{0.3f, 0.4f, 0.6f};
    }
    
    public void setLights(List<Light> lights) {
        activeLights.clear();
        activeLights.addAll(lights);
    }
    
    public void cleanup() {
        activeLights.clear();
        enabled = false;
        System.out.println("Lighting system cleaned up");
    }
}

/**
 * Shadow renderer - handles shadow mapping
 */
class ShadowRenderer {
    private GPURenderEngine engine;
    private boolean enabled = false;
    private int shadowMapSize = 2048;
    private float[] lightViewMatrix = new float[16];
    private float[] lightProjectionMatrix = new float[16];
    
    public ShadowRenderer(GPURenderEngine engine) {
        this.engine = engine;
    }
    
    public void enable() {
        enabled = true;
        setupShadowMapping();
        System.out.println("Shadow renderer enabled");
    }
    
    public void disable() {
        enabled = false;
        System.out.println("Shadow renderer disabled");
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    private void setupShadowMapping() {
        System.out.println("Shadow mapping initialized with map size: " + shadowMapSize);
    }
    
    public void renderShadowMaps(SceneData sceneData) {
        if (!enabled || engine.getFramebufferManager() == null || engine.getGLContext() == null) return;
        
        try {
            engine.getFramebufferManager().bindShadowMapFramebuffer();
            engine.getGLContext().setViewport(0, 0, shadowMapSize, shadowMapSize);
            engine.getGLContext().clearBuffers();
            
            if (engine.getShaderManager() != null && engine.getShaderPrograms() != null && engine.getShaderPrograms().containsKey("shadow")) {
                engine.getShaderManager().useProgram(engine.getShaderPrograms().get("shadow"));
            }
            
            calculateLightSpaceMatrix(sceneData);
            if (engine.getShaderManager() != null) {
                engine.getShaderManager().setUniform("u_lightSpaceMatrix", 
                    multiplyMatrices(lightProjectionMatrix, lightViewMatrix));
            }
            
            for (RenderObject obj : sceneData.objects) {
                if (obj.castsShadows) {
                    renderObjectToShadowMap(obj);
                }
            }
        } catch (Exception e) {
            System.err.println("Shadow map rendering failed: " + e.getMessage());
        }
    }
    
    private void calculateLightSpaceMatrix(SceneData sceneData) {
        if (!sceneData.lights.isEmpty()) {
            Light primaryLight = sceneData.lights.get(0);
            
            float orthoSize = 100.0f;
            setOrthographicMatrix(lightProjectionMatrix, -orthoSize, orthoSize, 
                                -orthoSize, orthoSize, 1.0f, 200.0f);
            
            float[] lightPos = {
                -primaryLight.direction[0] * 100,
                -primaryLight.direction[1] * 100,
                -primaryLight.direction[2] * 100
            };
            
            setLookAtMatrix(lightViewMatrix, lightPos, new float[]{0, 0, 0}, new float[]{0, 1, 0});
        }
    }
    
    private void renderObjectToShadowMap(RenderObject obj) {
        try {
            if (engine.getShaderManager() != null) {
                engine.getShaderManager().setUniform("u_modelMatrix", obj.modelMatrix);
            }
            
            if (engine.getVAOManager() != null && engine.getGLContext() != null) {
                engine.getVAOManager().bindVAO(obj.vertexArrayObject);
                engine.getGLContext().drawArrays(GLConstants.GL_TRIANGLES, 0, obj.vertexCount);
            }
        } catch (Exception e) {
            System.err.println("Shadow map object rendering failed: " + e.getMessage());
        }
    }
    
    public void updateUniforms(GPUConfig config) {
        if (!enabled || engine.getShaderManager() == null) return;
        
        try {
            engine.getShaderManager().setUniform("u_shadowsEnabled", true);
            
            // FIXED: Use String comparison instead of switch
            if ("Low".equals(config.shadowQuality)) {
                shadowMapSize = 512;
            } else if ("Medium".equals(config.shadowQuality)) {
                shadowMapSize = 1024;
            } else if ("High".equals(config.shadowQuality)) {
                shadowMapSize = 2048;
            }
        } catch (Exception e) {
            System.err.println("Shadow uniform update failed: " + e.getMessage());
        }
    }
    
    public void cleanup() {
        enabled = false;
        System.out.println("Shadow renderer cleaned up");
    }
    
    private void setOrthographicMatrix(float[] matrix, float left, float right, 
                                     float bottom, float top, float near, float far) {
        Arrays.fill(matrix, 0);
        matrix[0] = 2.0f / (right - left);
        matrix[5] = 2.0f / (top - bottom);
        matrix[10] = -2.0f / (far - near);
        matrix[12] = -(right + left) / (right - left);
        matrix[13] = -(top + bottom) / (top - bottom);
        matrix[14] = -(far + near) / (far - near);
        matrix[15] = 1.0f;
    }
    
    private void setLookAtMatrix(float[] matrix, float[] eye, float[] center, float[] up) {
        Arrays.fill(matrix, 0);
        matrix[0] = matrix[5] = matrix[10] = matrix[15] = 1.0f;
    }
    
    private float[] multiplyMatrices(float[] a, float[] b) {
        float[] result = new float[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                result[i * 4 + j] = 0;
                for (int k = 0; k < 4; k++) {
                    result[i * 4 + j] += a[i * 4 + k] * b[k * 4 + j];
                }
            }
        }
        return result;
    }
}