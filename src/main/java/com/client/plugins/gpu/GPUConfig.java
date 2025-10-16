package com.client.plugins.gpu;

/**
 * GPU Plugin Configuration - Complete with all missing fields
 */
public class GPUConfig {
    
    // Anti-aliasing type enum
    public enum AntiAliasingType {
        SIMPLE, FXAA, MSAA, TAA, COMBINED
    }
    
    // Fog type enum - 6 distinct types
    public enum FogType {
        OFF, GREY, SISLE, DARK, MAROON, RAINBOW
    }
    
    // Rasterization modes
    public enum RasterizationMode {
        FAST, SKIP_DISTANT, REDUCE_PRECISION, DYNAMIC_LOD
    }
    
    // Screen dimensions
    public int screenWidth = 765;
    public int screenHeight = 503;
    
    // Client anti-aliasing integration
    public boolean clientAntiAliasing = false;
    
    // GPU Plugin effects - DISABLED BY DEFAULT
    public boolean brightnessTint = false;
    public int brightnessLevel = 100; // 50-150 range (100 = normal)
    
    public boolean shadows = false;
    
    // Fog control - controls game's existing fog system
    public boolean fog = false;
    public FogType fogType = FogType.OFF;
    
    // Performance settings
    public boolean polygonRasterization = false;
    public RasterizationMode rasterizationMode = RasterizationMode.FAST;
    public int rasterizationLevel = 2;
    
    // Advanced settings
    public boolean debugMode = false;
    public boolean showFPS = false;
    
    // NEW PERFORMANCE FEATURES
    public boolean detailEnhancement = false;
    public float sharpeningStrength = 0.5f;
    public boolean frameRateUnlocking = false;
    public boolean vsyncBypass = false;
    public boolean smoothFramePacing = false;
    public int targetFPS = 144; // Target frame rate when unlocked
    
    // FPS Display options
    public boolean showFPSCounter = false;
    public int fpsDisplayPosition = 0; // 0=top-left, 1=top-right, 2=bottom-left, 3=bottom-right
    public boolean showFrameTime = false;
    public boolean showDetailedStats = false;
    
    // MISSING FIELDS FROM ERROR LOG - Added here
    public boolean wireframeMode = false;
    public int textureDetail = 1; // 0=Low, 1=Medium, 2=High
    public boolean showGPUInfo = false;
    public float lightDistance = 50.0f;
    public float drawDistance = 100.0f;
    public AntiAliasingType antiAliasingType = AntiAliasingType.FXAA;
    public float antiAliasingStrength = 0.5f;
    public int antiAliasingMode = 4; // Sample count for MSAA
    public boolean antiAliasing = false; // Master AA toggle
    public float ambientStrength = 0.3f;
    
    // Shadow Settings - FIXED: Only ONE declaration of each field
    public int shadowSensitivity = 80; // 20-150, higher = only strong edges cast shadows
    public int shadowMinBrightness = 100; // 0-255, only bright objects cast shadows
    public float shadowStrength = 0.4f; // 0.0-1.0, how dark the shadows are
    public int shadowDistance = 2; // 1-6, how far shadows are cast
    public int shadowSoftness = 2; // 1-5, blur radius for shadow smoothness
    public boolean shadowColorTint = false; // Add blue tint to shadows
    public String shadowQuality = "Medium"; // Low, Medium, High - ONLY ONE DECLARATION
    
    /**
     * Constructor - everything DISABLED by default
     */
    public GPUConfig() {
        // All effects start DISABLED - user must enable manually
        clientAntiAliasing = false;
        brightnessTint = false;
        shadows = false;
        fog = false;
        fogType = FogType.OFF;
        polygonRasterization = false;
        debugMode = false;
        showFPS = false;
        
        // Initialize missing fields
        wireframeMode = false;
        textureDetail = 1;
        showGPUInfo = false;
        lightDistance = 50.0f;
        drawDistance = 100.0f;
        antiAliasingType = AntiAliasingType.FXAA;
        antiAliasingStrength = 0.5f;
        antiAliasingMode = 4;
        antiAliasing = false;
        ambientStrength = 0.3f;
        
        // Initialize NEW performance fields
        detailEnhancement = false;
        sharpeningStrength = 0.5f;
        frameRateUnlocking = false;
        vsyncBypass = false;
        smoothFramePacing = false;
        targetFPS = 144;
        
        // Initialize FPS display fields
        showFPSCounter = false;
        fpsDisplayPosition = 0;
        showFrameTime = false;
        showDetailedStats = false;
        
        // Initialize shadow fields
        shadowSensitivity = 80;
        shadowMinBrightness = 100;
        shadowStrength = 0.4f;
        shadowDistance = 2;
        shadowSoftness = 2;
        shadowColorTint = false;
        shadowQuality = "Medium";
    }
    
    /**
     * Reset to safe defaults
     */
    public void resetToDefaults() {
        clientAntiAliasing = false;
        brightnessTint = false;
        brightnessLevel = 100;
        shadows = false;
        fog = false;
        fogType = FogType.OFF;
        polygonRasterization = false;
        rasterizationMode = RasterizationMode.FAST;
        rasterizationLevel = 2;
        debugMode = false;
        showFPS = false;
        
        // Reset additional fields
        wireframeMode = false;
        textureDetail = 1;
        showGPUInfo = false;
        lightDistance = 50.0f;
        drawDistance = 100.0f;
        antiAliasingType = AntiAliasingType.FXAA;
        antiAliasingStrength = 0.5f;
        antiAliasingMode = 4;
        antiAliasing = false;
        ambientStrength = 0.3f;
        
        // Reset shadow fields
        shadowSensitivity = 80;
        shadowMinBrightness = 100;
        shadowStrength = 0.4f;
        shadowDistance = 2;
        shadowSoftness = 2;
        shadowColorTint = false;
        shadowQuality = "Medium";
        
        System.out.println("GPU Config: Reset to defaults (all effects disabled)");
    }
    
    /**
     * Enable performance preset for testing
     */
    public void enablePerformancePreset() {
        clientAntiAliasing = true;
        fog = true;
        fogType = FogType.GREY;
        textureDetail = 1; // Medium
        shadowQuality = "Medium";
        antiAliasing = true;
        antiAliasingType = AntiAliasingType.FXAA;
        System.out.println("GPU Config: Performance preset enabled");
    }
    
    /**
     * Enable quality preset for testing
     */
    public void enableQualityPreset() {
        clientAntiAliasing = true;
        brightnessTint = true;
        brightnessLevel = 110;
        shadows = true;
        fog = true;
        fogType = FogType.SISLE;
        textureDetail = 2; // High
        shadowQuality = "High";
        antiAliasing = true;
        antiAliasingType = AntiAliasingType.COMBINED;
        antiAliasingStrength = 0.7f;
        System.out.println("GPU Config: Quality preset enabled");
    }
    
    /**
     * Disable all effects
     */
    public void disableAllEffects() {
        clientAntiAliasing = false;
        brightnessTint = false;
        shadows = false;
        fog = false;
        fogType = FogType.OFF;
        polygonRasterization = false;
        antiAliasing = false;
        wireframeMode = false;
        showGPUInfo = false;
        showFPS = false;
        debugMode = false;
        System.out.println("GPU Config: All effects disabled");
    }
    
    /**
     * Get count of enabled effects - FIXED to include performance features
     */
    public int getEnabledEffectsCount() {
        int count = 0;
        if (clientAntiAliasing) count++;
        if (brightnessTint) count++;
        if (shadows) count++;
        if (fog) count++;
        if (polygonRasterization) count++;
        if (antiAliasing) count++;
        
        // ADD PERFORMANCE FEATURES TO COUNT
        if (detailEnhancement) count++;
        if (showFPSCounter) count++;
        if (frameRateUnlocking) count++;
        if (vsyncBypass) count++;
        if (smoothFramePacing) count++;
        
        return count;
    }
    
    /**
     * Check if any effects are enabled
     */
    public boolean hasAnyEffectsEnabled() {
        return getEnabledEffectsCount() > 0 || showFPSCounter || detailEnhancement;
    }
    
    /**
     * Get configuration summary
     */
    public String getConfigSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("GPU Config (").append(getEnabledEffectsCount()).append(" effects enabled):\n");
        sb.append("  Client Anti-aliasing: ").append(clientAntiAliasing).append("\n");
        sb.append("  GPU Anti-aliasing: ").append(antiAliasing).append(antiAliasing ? " (" + antiAliasingType + ", strength: " + antiAliasingStrength + ")" : "").append("\n");
        sb.append("  Brightness: ").append(brightnessTint).append(brightnessTint ? " (level: " + brightnessLevel + ")" : "").append("\n");
        sb.append("  Shadows: ").append(shadows).append(shadows ? " (quality: " + shadowQuality + ")" : "").append("\n");
        sb.append("  Fog: ").append(fog).append(fog ? " (" + fogType + ")" : "").append("\n");
        sb.append("  Texture Detail: ").append(textureDetail).append(" (0=Low, 1=Med, 2=High)\n");
        sb.append("  Draw Distance: ").append(drawDistance).append("\n");
        sb.append("  Performance Opts: ").append(polygonRasterization).append("\n");
        sb.append("  Debug Mode: ").append(debugMode).append("\n");
        sb.append("  Wireframe: ").append(wireframeMode).append("\n");
        return sb.toString();
    }
    
    /**
     * Validate configuration values
     */
    public void validateConfig() {
        // Clamp brightness to valid range
        brightnessLevel = Math.max(50, Math.min(150, brightnessLevel));
        
        // Ensure valid rasterization level
        rasterizationLevel = Math.max(1, Math.min(4, rasterizationLevel));
        
        // Clamp texture detail
        textureDetail = Math.max(0, Math.min(2, textureDetail));
        
        // Clamp anti-aliasing strength
        antiAliasingStrength = Math.max(0.0f, Math.min(1.0f, antiAliasingStrength));
        
        // Validate MSAA sample count
        if (antiAliasingMode != 2 && antiAliasingMode != 4 && antiAliasingMode != 8 && antiAliasingMode != 16) {
            antiAliasingMode = 4; // Default to 4x MSAA
        }
        
        // Clamp distances
        lightDistance = Math.max(10.0f, Math.min(200.0f, lightDistance));
        drawDistance = Math.max(50.0f, Math.min(500.0f, drawDistance));
        ambientStrength = Math.max(0.0f, Math.min(1.0f, ambientStrength));
        
        // Validate shadow settings
        shadowSensitivity = Math.max(20, Math.min(150, shadowSensitivity));
        shadowMinBrightness = Math.max(0, Math.min(255, shadowMinBrightness));
        shadowStrength = Math.max(0.1f, Math.min(1.0f, shadowStrength));
        shadowDistance = Math.max(1, Math.min(6, shadowDistance));
        shadowSoftness = Math.max(1, Math.min(5, shadowSoftness));
        
        if (!shadowQuality.equals("Low") && !shadowQuality.equals("Medium") && !shadowQuality.equals("High")) {
            shadowQuality = "Medium";
        }
        
        System.out.println("GPU Config: Configuration validated");
    }
    
    /**
     * Save configuration (placeholder)
     */
    public void save() {
        validateConfig();
        System.out.println("GPU Config: Saving configuration...");
        // TODO: Implement file/registry saving
    }
    
    /**
     * Load configuration (placeholder)
     */
    public void load() {
        System.out.println("GPU Config: Loading configuration...");
        // TODO: Implement file/registry loading
        validateConfig();
    }
}