package com.client.plugins.gpu;

/**
 * GPU Plugin Configuration
 * Matches RuneLite's GPU plugin settings
 */
public class GpuConfigRunelite {
    
    // Rendering
    public boolean unlockFps = false;
    public int targetFps = 60;
    public boolean vsync = true;
    public int antiAliasingMode = 4; // MSAA samples: 0, 2, 4, 8, 16
    public int anisotropicFilteringLevel = 0; // 0, 2, 4, 8, 16
    
    // Graphics
    public boolean fogEnabled = true;
    public int fogDepth = 50;
    public boolean groundFog = false;
    
    public boolean shadows = false;
    public int shadowQuality = 2; // 0=low, 1=medium, 2=high, 3=very high
    public int shadowDistance = 40;
    
    public boolean atmosphericLighting = false;
    public int brightness = 100;
    public int contrast = 100;
    public int saturation = 100;
    
    // HD Features
    public boolean hdTextures = false;
    public boolean hdObjectTextures = false;
    public boolean hdNpcTextures = false;
    public boolean hdGroundTextures = false;
    
    public boolean smoothBanding = true;
    public boolean colorCorrection = false;
    
    // Water
    public boolean waterEffects = false;
    public boolean reflections = false;
    
    // Advanced
    public int drawDistance = 50;
    public boolean dynamicLights = false;
    public boolean parallaxOcclusionMapping = false;
    
    public boolean enableSkybox = false;
    public String skyboxColor = "#0080C0";
    
    // Performance
    public boolean computeShaders = true;
    public int maxDynamicLights = 8;
    public boolean preferredGpu = false; // Use dedicated GPU if available
    
    // Debug
    public boolean showFps = false;
    public boolean showGpuInfo = false;
    
    // Methods
    public boolean isAntiAliasingEnabled() {
        return antiAliasingMode > 0;
    }
    
    public boolean isAnisotropicFilteringEnabled() {
        return anisotropicFilteringLevel > 0;
    }
    
    public float getBrightnessMultiplier() {
        return brightness / 100.0f;
    }
    
    public float getContrastMultiplier() {
        return contrast / 100.0f;
    }
    
    public float getSaturationMultiplier() {
        return saturation / 100.0f;
    }
}