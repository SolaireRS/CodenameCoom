package com.client.plugins.gpu;

import javax.swing.*;

import com.client.Client;
import com.client.CustomGameFrame;

/**
 * GPU Plugin - Professional version with complete shadow system
 * Note: GPUSettingsPanel is now a separate standalone class
 */
public class GPUPlugin {
    
    public static final String PLUGIN_NAME = "GPU Renderer";
    public static final String PLUGIN_VERSION = "1.0.0";
    public static final String PLUGIN_DESCRIPTION = "Hardware-accelerated rendering with advanced effects";
    private GPUPixelProcessor gpuProcessor;
    private boolean gpuAvailable = false;
    private GPUConfig config;
    private GPURenderEngine renderEngine;
    private GPUSettingsPanel settingsPanel;
    private boolean isEnabled = false;
    private boolean isInitialized = false;
    
    private CustomGameFrame gameFrame;
    private int[] currentPixelBuffer;
    private int bufferWidth;
    private int bufferHeight;
    private int[] previousFrame = null;
    private int[] shadowBuffer = null;
    
    // Performance tracking
    private long lastFrameTime = System.nanoTime();
    private float currentFPS = 60.0f;
    private float frameTimeMs = 16.67f;
    private long frameCount = 0;
    private long[] recentFrameTimes = new long[60];
    private int frameTimeIndex = 0;
    private boolean clientLoggedIn = false;

    public void setLoggedIn(boolean loggedIn) {
        this.clientLoggedIn = loggedIn;
    }

    private boolean isLoggedIn() {
        return clientLoggedIn;
    }
    public GPUPlugin(CustomGameFrame gameFrame) {
        this.gameFrame = gameFrame;
        this.config = new GPUConfig();
        this.settingsPanel = new GPUSettingsPanel(this);
        
        for (int i = 0; i < recentFrameTimes.length; i++) {
            recentFrameTimes[i] = 16666666;
        }
    }
    
    public boolean initialize(int width, int height) {
        if (isInitialized) return true;
        
        try {
            config.screenWidth = width;
            config.screenHeight = height;
            bufferWidth = width;
            bufferHeight = height;
            
            // Try to initialize GPU acceleration
            gpuProcessor = new GPUPixelProcessor();
            gpuAvailable = gpuProcessor.initialize(width, height);
            
            if (gpuAvailable) {
                // ADD THIS LINE to enable debug output
                gpuProcessor.setDebugMode(true);
                //System.out.println("✓ GPU acceleration enabled via OpenGL compute shaders");
            } else {
                //System.out.println("✗ GPU acceleration unavailable, using CPU");
            }
            
            isInitialized = true;
            return true;
        } catch (Exception e) {
            showError("GPU Plugin initialization failed: " + e.getMessage());
            return false;
        }
    }
    
    public boolean initialize() {
        return initialize(765, 503);
    }
    
    public void enable() {
        if (!isInitialized && !initialize()) return;
        if (!isEnabled) {
            if (renderEngine != null) renderEngine.enable();
            isEnabled = true;
        }
    }
    
    public void disable() {
        if (isEnabled) {
            if (renderEngine != null) renderEngine.disable();
            isEnabled = false;
        }
    }
    
    public void setGPUDebugMode(boolean debug) {
        if (gpuProcessor != null) {
            gpuProcessor.setDebugMode(debug);
        }
    }
    public void toggle() {
        if (isEnabled) disable();
        else enable();
    }
    
    public void updatePixelBuffer(int[] pixels, int width, int height) {
        this.currentPixelBuffer = pixels;
        this.bufferWidth = width;
        this.bufferHeight = height;
    }
    /**
     * Process pixels for resizable mode with dynamic dimensions
     */
    public void processResizableFrame(int[] pixels, int width, int height) {
        if (!isEnabled || pixels == null) {
            return;
        }
        
        // Update buffer dimensions if they changed
        if (width != bufferWidth || height != bufferHeight) {
            //System.out.println("GPU: Updating buffer size from " + bufferWidth + "x" + bufferHeight + 
             //                 " to " + width + "x" + height);
            bufferWidth = width;
            bufferHeight = height;
            
            // Reinitialize GPU processor with new dimensions
            if (gpuAvailable && gpuProcessor != null) {
                gpuProcessor.initialize(width, height);
            }
            
            // Clear old buffers
            previousFrame = null;
            shadowBuffer = null;
        }
        
        // Update current buffer reference
        this.currentPixelBuffer = pixels;
        
        // Apply effects
        applyEffects();
    }
    public void applyEffects() {
        if (currentPixelBuffer == null || !isEnabled) {
            return;
        }
        
        // Check if client is logged in
        Client clientInstance = getClientInstance();
        if (clientInstance == null || !clientInstance.loggedIn) {
            return; // Don't process during login screen
        }
        
        // Check if buffer has actual content (not all black/transparent)
        boolean hasContent = false;
        int sampleSize = Math.min(1000, currentPixelBuffer.length);
        for (int i = 0; i < sampleSize; i += 10) {
            if (currentPixelBuffer[i] != 0 && currentPixelBuffer[i] != 0xFF000000) {
                hasContent = true;
                break;
            }
        }
        
        if (!hasContent) {
            return;
        }
        
        // NOW we can safely process - buffer is valid and client is logged in
        int effectWidth = bufferWidth;
        int effectHeight = bufferHeight;
        
        try {
            if (gpuAvailable && gpuProcessor != null) {
                gpuProcessor.processPixels(currentPixelBuffer, bufferWidth, bufferHeight, config);
                return;
            }
            
            // CPU fallback code...
            // (rest of existing code)
            
        } catch (Exception e) {
            //System.err.println("GPU effects error: " + e.getMessage());
        }
    }
    /**
     * Get the Client instance safely
     */
    private Client getClientInstance() {
        if (gameFrame == null) return null;
        return gameFrame.getClientInstance();
    }
    private void applyBrightnessTintBounded(int startX, int startY, int endX, int endY) {
        if (currentPixelBuffer == null) return;
        
        float brightness = config.brightnessLevel / 100.0f;
        
        for (int y = startY; y < endY && y < bufferHeight; y++) {
            for (int x = startX; x < endX && x < bufferWidth; x++) {
                int index = y * bufferWidth + x;
                
                if (index < 0 || index >= currentPixelBuffer.length) continue;
                
                int pixel = currentPixelBuffer[index];
                
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;
                
                r = (int)(r * brightness);
                g = (int)(g * brightness);
                b = (int)(b * brightness);
                
                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));
                
                currentPixelBuffer[index] = (r << 16) | (g << 8) | b;
            }
        }
    }
    
    private void applyShadowEffectBounded(int startX, int startY, int endX, int endY) {
        if (currentPixelBuffer == null || bufferWidth <= 0 || bufferHeight <= 0) return;
        
        // Determine sample rate based on quality
        int sampleRate = 1;
        if ("Low".equals(config.shadowQuality)) {
            sampleRate = 3;
        } else if ("Medium".equals(config.shadowQuality)) {
            sampleRate = 2;
        } else if ("High".equals(config.shadowQuality)) {
            sampleRate = 1;
        }
        
        // Create shadow mask to accumulate shadow information
        int[] shadowMask = new int[currentPixelBuffer.length];
        
        // Initialize shadow buffer if needed
        if (shadowBuffer == null || shadowBuffer.length != currentPixelBuffer.length) {
            shadowBuffer = new int[currentPixelBuffer.length];
        }
        System.arraycopy(currentPixelBuffer, 0, shadowBuffer, 0, currentPixelBuffer.length);
        
        // Shadow configuration with better defaults
        final int shadowDistance = Math.max(1, Math.min(10, config.shadowDistance * 2)); // Increased range
        final float shadowStrength = Math.max(0.1f, Math.min(0.8f, config.shadowStrength));
        final int edgeThreshold = Math.min(50, Math.max(15, config.shadowSensitivity / 2)); // Lower threshold
        final int blurRadius = Math.max(1, config.shadowSoftness);
        
        // Edge detection pass - detect edges that should cast shadows
        for (int y = startY + 1; y < endY - 1; y += sampleRate) {
            for (int x = startX + 1; x < endX - 1; x += sampleRate) {
                int index = y * bufferWidth + x;
                if (index >= shadowBuffer.length) continue;
                
                int centerPixel = shadowBuffer[index];
                int centerLuma = getLuminanceInt(centerPixel);
                
                // Lower the minimum luminance requirement to detect more edges
                if (centerLuma < 30) continue; // Reduced from 80 to 30
                
                // Sample surrounding pixels for edge detection
                int leftIndex = index - 1;
                int rightIndex = index + 1;
                int upIndex = index - bufferWidth;
                int downIndex = index + bufferWidth;
                
                // Bounds checking
                if (rightIndex >= shadowBuffer.length || downIndex >= shadowBuffer.length ||
                    leftIndex < 0 || upIndex < 0) continue;
                
                // Calculate luminance of surrounding pixels
                int leftLuma = getLuminanceInt(shadowBuffer[leftIndex]);
                int rightLuma = getLuminanceInt(shadowBuffer[rightIndex]);
                int upLuma = getLuminanceInt(shadowBuffer[upIndex]);
                int downLuma = getLuminanceInt(shadowBuffer[downIndex]);
                
                // Sobel edge detection
                int gx = (rightLuma - leftLuma);
                int gy = (downLuma - upLuma);
                int edgeStrength = (int)Math.sqrt(gx * gx + gy * gy);
                
                // If we found an edge, cast a shadow
                if (edgeStrength > edgeThreshold) {
                    // Calculate shadow direction (simplified - cast to bottom-right)
                    float shadowAngle = (float)(Math.PI / 4); // 45 degrees
                    int shadowOffsetX = (int)(Math.cos(shadowAngle) * shadowDistance);
                    int shadowOffsetY = (int)(Math.sin(shadowAngle) * shadowDistance);
                    
                    // Cast shadow with gradual falloff
                    for (int dist = 1; dist <= shadowDistance; dist++) {
                        int shadowX = x + (shadowOffsetX * dist) / shadowDistance;
                        int shadowY = y + (shadowOffsetY * dist) / shadowDistance;
                        
                        // Check bounds
                        if (shadowX < startX || shadowX >= endX || shadowY < startY || shadowY >= endY) 
                            continue;
                        
                        int shadowIndex = shadowY * bufferWidth + shadowX;
                        
                        if (shadowIndex >= 0 && shadowIndex < shadowMask.length) {
                            // Calculate shadow intensity with distance falloff
                            float falloff = 1.0f - (dist / (float)shadowDistance);
                            falloff = falloff * falloff; // Quadratic falloff for more natural look
                            
                            int shadowValue = (int)(255 * falloff * (edgeStrength / 255.0f));
                            shadowMask[shadowIndex] = Math.min(255, shadowMask[shadowIndex] + shadowValue);
                        }
                    }
                    
                    // Add a softer secondary shadow for more realism
                    for (int dist = 1; dist <= shadowDistance / 2; dist++) {
                        int shadowX = x + (shadowOffsetX * dist * 2) / shadowDistance;
                        int shadowY = y + (shadowOffsetY * dist * 2) / shadowDistance;
                        
                        if (shadowX < startX || shadowX >= endX || shadowY < startY || shadowY >= endY) 
                            continue;
                        
                        int shadowIndex = shadowY * bufferWidth + shadowX;
                        
                        if (shadowIndex >= 0 && shadowIndex < shadowMask.length) {
                            float falloff = 1.0f - (dist * 2.0f / shadowDistance);
                            falloff = falloff * falloff * falloff; // Cubic falloff for soft shadow
                            
                            int shadowValue = (int)(128 * falloff * (edgeStrength / 255.0f));
                            shadowMask[shadowIndex] = Math.min(255, shadowMask[shadowIndex] + shadowValue / 2);
                        }
                    }
                }
            }
        }
        
        // Apply gaussian blur to shadow mask for softer shadows
        int[] blurredShadowMask = gaussianBlur(shadowMask, bufferWidth, bufferHeight, 
                                               startX, startY, endX, endY, blurRadius);
        
        // Apply the shadow mask to the original image
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int index = y * bufferWidth + x;
                if (index >= currentPixelBuffer.length) continue;
                
                int shadowAmount = blurredShadowMask[index];
                if (shadowAmount == 0) continue;
                
                // Normalize shadow amount and apply curve for more natural look
                float normalizedShadow = shadowAmount / 255.0f;
                float shadowCurve = (float)Math.pow(normalizedShadow, 1.2); // Slightly less aggressive curve
                float shadowMultiplier = 1.0f - (shadowCurve * shadowStrength);
                
                // Ensure minimum visibility in shadows
                shadowMultiplier = Math.max(0.4f, Math.min(1.0f, shadowMultiplier));
                
                int originalPixel = currentPixelBuffer[index];
                
                // Apply shadow to RGB channels
                int r = (int)((originalPixel >> 16 & 0xFF) * shadowMultiplier);
                int g = (int)((originalPixel >> 8 & 0xFF) * shadowMultiplier);
                int b = (int)((originalPixel & 0xFF) * shadowMultiplier);
                
                // Add subtle blue tint to shadows if enabled
                if (config.shadowColorTint) {
                    b = Math.min(255, (int)(b * 1.1f + 5)); // Subtle blue boost
                    g = Math.max(0, (int)(g * 0.98f)); // Slight green reduction
                }
                
                // Clamp values and set pixel
                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));
                
                currentPixelBuffer[index] = (r << 16) | (g << 8) | b;
            }
        }
    }

    private int[] gaussianBlur(int[] input, int width, int height, 
                              int startX, int startY, int endX, int endY, int radius) {
        int[] output = new int[input.length];
        
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int sum = 0;
                int count = 0;
                
                for (int dx = -radius; dx <= radius; dx++) {
                    int sampleX = x + dx;
                    if (sampleX >= startX && sampleX < endX) {
                        int index = y * width + sampleX;
                        if (index >= 0 && index < input.length) {
                            sum += input[index];
                            count++;
                        }
                    }
                }
                
                int index = y * width + x;
                if (index < output.length) {
                    output[index] = count > 0 ? sum / count : 0;
                }
            }
        }
        
        int[] finalOutput = new int[input.length];
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int sum = 0;
                int count = 0;
                
                for (int dy = -radius; dy <= radius; dy++) {
                    int sampleY = y + dy;
                    if (sampleY >= startY && sampleY < endY) {
                        int index = sampleY * width + x;
                        if (index >= 0 && index < output.length) {
                            sum += output[index];
                            count++;
                        }
                    }
                }
                
                int index = y * width + x;
                if (index < finalOutput.length) {
                    finalOutput[index] = count > 0 ? sum / count : 0;
                }
            }
        }
        
        return finalOutput;
    }
    
    private void applyAntiAliasingBounded(int startX, int startY, int endX, int endY) {
        if (bufferWidth <= 2 || bufferHeight <= 2 || currentPixelBuffer == null) return;
        
        switch (config.antiAliasingType) {
            case FXAA:
                applyFXAABounded(startX, startY, endX, endY);
                break;
            case MSAA:
                applyMSAABounded(startX, startY, endX, endY);
                break;
            case TAA:
                applyTemporalAABounded(startX, startY, endX, endY);
                break;
            case COMBINED:
                applyFXAABounded(startX, startY, endX, endY);
                applyTemporalAABounded(startX, startY, endX, endY);
                break;
            default:
                applySimpleAABounded(startX, startY, endX, endY);
                break;
        }
    }
    
    private void applyFXAABounded(int startX, int startY, int endX, int endY) {
        int[] tempBuffer = new int[currentPixelBuffer.length];
        System.arraycopy(currentPixelBuffer, 0, tempBuffer, 0, currentPixelBuffer.length);
        
        float threshold = 0.125f * config.antiAliasingStrength;
        
        for (int y = Math.max(1, startY); y < Math.min(bufferHeight - 1, endY); y++) {
            for (int x = Math.max(1, startX); x < Math.min(bufferWidth - 1, endX); x++) {
                int index = y * bufferWidth + x;
                
                if (index >= tempBuffer.length - bufferWidth - 1) continue;
                
                int center = tempBuffer[index];
                int north = (index - bufferWidth >= 0) ? tempBuffer[index - bufferWidth] : center;
                int south = (index + bufferWidth < tempBuffer.length) ? tempBuffer[index + bufferWidth] : center;
                int east = (index + 1 < tempBuffer.length && x + 1 < endX) ? tempBuffer[index + 1] : center;
                int west = (index - 1 >= 0 && x - 1 >= startX) ? tempBuffer[index - 1] : center;
                
                float centerLuma = getLuminance(center);
                float northLuma = getLuminance(north);
                float southLuma = getLuminance(south);
                float eastLuma = getLuminance(east);
                float westLuma = getLuminance(west);
                
                float lumaMin = Math.min(centerLuma, Math.min(Math.min(northLuma, southLuma), Math.min(eastLuma, westLuma)));
                float lumaMax = Math.max(centerLuma, Math.max(Math.max(northLuma, southLuma), Math.max(eastLuma, westLuma)));
                float lumaRange = lumaMax - lumaMin;
                
                if (lumaRange < Math.max(threshold, lumaMax * 0.125f)) {
                    continue;
                }
                
                float gradientHorizontal = Math.abs((westLuma + centerLuma) * 0.5f - (eastLuma + centerLuma) * 0.5f);
                float gradientVertical = Math.abs((northLuma + centerLuma) * 0.5f - (southLuma + centerLuma) * 0.5f);
                
                boolean isHorizontal = gradientHorizontal >= gradientVertical;
                
                if (isHorizontal) {
                    int r = ((north >> 16 & 0xFF) + (center >> 16 & 0xFF) * 2 + (south >> 16 & 0xFF)) / 4;
                    int g = ((north >> 8 & 0xFF) + (center >> 8 & 0xFF) * 2 + (south >> 8 & 0xFF)) / 4;
                    int b = ((north & 0xFF) + (center & 0xFF) * 2 + (south & 0xFF)) / 4;
                    currentPixelBuffer[index] = (r << 16) | (g << 8) | b;
                } else {
                    int r = ((west >> 16 & 0xFF) + (center >> 16 & 0xFF) * 2 + (east >> 16 & 0xFF)) / 4;
                    int g = ((west >> 8 & 0xFF) + (center >> 8 & 0xFF) * 2 + (east >> 8 & 0xFF)) / 4;
                    int b = ((west & 0xFF) + (center & 0xFF) * 2 + (east & 0xFF)) / 4;
                    currentPixelBuffer[index] = (r << 16) | (g << 8) | b;
                }
            }
        }
    }
    
    private void applyMSAABounded(int startX, int startY, int endX, int endY) {
        int samples = config.antiAliasingMode;
        int[] tempBuffer = new int[currentPixelBuffer.length];
        System.arraycopy(currentPixelBuffer, 0, tempBuffer, 0, currentPixelBuffer.length);
        
        for (int y = Math.max(1, startY); y < Math.min(bufferHeight - 1, endY); y++) {
            for (int x = Math.max(1, startX); x < Math.min(bufferWidth - 1, endX); x++) {
                int index = y * bufferWidth + x;
                
                if (index >= tempBuffer.length) continue;
                
                long totalR = 0, totalG = 0, totalB = 0;
                int validSamples = 0;
                
                int sampleSize = (int)Math.sqrt(samples);
                for (int sy = 0; sy < sampleSize; sy++) {
                    for (int sx = 0; sx < sampleSize; sx++) {
                        int sampleX = Math.max(startX, Math.min(endX - 1, x + sx - sampleSize/2));
                        int sampleY = Math.max(startY, Math.min(endY - 1, y + sy - sampleSize/2));
                        int sampleIndex = sampleY * bufferWidth + sampleX;
                        
                        if (sampleIndex < tempBuffer.length) {
                            int sampleColor = tempBuffer[sampleIndex];
                            totalR += (sampleColor >> 16) & 0xFF;
                            totalG += (sampleColor >> 8) & 0xFF;
                            totalB += sampleColor & 0xFF;
                            validSamples++;
                        }
                    }
                }
                
                if (validSamples > 0) {
                    int r = (int)(totalR / validSamples);
                    int g = (int)(totalG / validSamples);
                    int b = (int)(totalB / validSamples);
                    currentPixelBuffer[index] = (r << 16) | (g << 8) | b;
                }
            }
        }
    }
    
    private void applyTemporalAABounded(int startX, int startY, int endX, int endY) {
        if (previousFrame == null || previousFrame.length != currentPixelBuffer.length) {
            previousFrame = new int[currentPixelBuffer.length];
            System.arraycopy(currentPixelBuffer, 0, previousFrame, 0, currentPixelBuffer.length);
            return;
        }
        
        float blendFactor = 0.1f * config.antiAliasingStrength;
        
        for (int y = startY; y < Math.min(bufferHeight, endY); y++) {
            for (int x = startX; x < Math.min(bufferWidth, endX); x++) {
                int index = y * bufferWidth + x;
                if (index >= currentPixelBuffer.length) continue;
                
                int current = currentPixelBuffer[index];
                int previous = previousFrame[index];
                
                int currR = (current >> 16) & 0xFF;
                int currG = (current >> 8) & 0xFF;
                int currB = current & 0xFF;
                
                int prevR = (previous >> 16) & 0xFF;
                int prevG = (previous >> 8) & 0xFF;
                int prevB = previous & 0xFF;
                
                int r = (int)(currR * (1.0f - blendFactor) + prevR * blendFactor);
                int g = (int)(currG * (1.0f - blendFactor) + prevG * blendFactor);
                int b = (int)(currB * (1.0f - blendFactor) + prevB * blendFactor);
                
                currentPixelBuffer[index] = (r << 16) | (g << 8) | b;
            }
        }
        
        System.arraycopy(currentPixelBuffer, 0, previousFrame, 0, currentPixelBuffer.length);
    }
 // Add these methods to the GPUPlugin class:

    /**
     * Draw a GPU-accelerated rectangle directly to the pixel buffer
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Rectangle width
     * @param height Rectangle height
     * @param color Color in RGB format (0xRRGGBB)
     */
    public void drawAcceleratedRectangle(int x, int y, int width, int height, int color) {
        if (currentPixelBuffer == null || !isEnabled) {
            return;
        }
        
        // Bounds checking
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(bufferWidth, x + width);
        int endY = Math.min(bufferHeight, y + height);
        
        // Draw the rectangle
        for (int py = startY; py < endY; py++) {
            for (int px = startX; px < endX; px++) {
                int index = py * bufferWidth + px;
                if (index >= 0 && index < currentPixelBuffer.length) {
                    currentPixelBuffer[index] = color;
                }
            }
        }
        
        // Apply GPU effects to the drawn area if enabled
        if (config.antiAliasing || config.brightnessTint || config.shadows) {
            // Apply effects only to the rectangle area for performance
            if (config.antiAliasing) {
                applyAntiAliasingBounded(startX, startY, endX, endY);
            }
            if (config.brightnessTint) {
                applyBrightnessTintBounded(startX, startY, endX, endY);
            }
            if (config.shadows) {
                applyShadowEffectBounded(startX, startY, endX, endY);
            }
        }
    }

    /**
     * Draw a GPU-accelerated transparent rectangle with alpha blending
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Rectangle width
     * @param height Rectangle height
     * @param color Color in RGB format (0xRRGGBB)
     * @param opacity Opacity level (0-255, where 255 is fully opaque)
     */
    public void drawAcceleratedTransparentRectangle(int x, int y, int width, int height, int color, int opacity) {
        if (currentPixelBuffer == null || !isEnabled) {
            return;
        }
        
        // Clamp opacity to valid range
        opacity = Math.max(0, Math.min(255, opacity));
        float alpha = opacity / 255.0f;
        float invAlpha = 1.0f - alpha;
        
        // Extract color components
        int newR = (color >> 16) & 0xFF;
        int newG = (color >> 8) & 0xFF;
        int newB = color & 0xFF;
        
        // Bounds checking
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(bufferWidth, x + width);
        int endY = Math.min(bufferHeight, y + height);
        
        // Draw the transparent rectangle with alpha blending
        for (int py = startY; py < endY; py++) {
            for (int px = startX; px < endX; px++) {
                int index = py * bufferWidth + px;
                if (index >= 0 && index < currentPixelBuffer.length) {
                    // Get existing pixel
                    int existingPixel = currentPixelBuffer[index];
                    int existingR = (existingPixel >> 16) & 0xFF;
                    int existingG = (existingPixel >> 8) & 0xFF;
                    int existingB = existingPixel & 0xFF;
                    
                    // Alpha blend
                    int blendedR = (int)(newR * alpha + existingR * invAlpha);
                    int blendedG = (int)(newG * alpha + existingG * invAlpha);
                    int blendedB = (int)(newB * alpha + existingB * invAlpha);
                    
                    // Clamp to valid range
                    blendedR = Math.min(255, Math.max(0, blendedR));
                    blendedG = Math.min(255, Math.max(0, blendedG));
                    blendedB = Math.min(255, Math.max(0, blendedB));
                    
                    // Set the blended pixel
                    currentPixelBuffer[index] = (blendedR << 16) | (blendedG << 8) | blendedB;
                }
            }
        }
        
        // Apply GPU effects to the drawn area if enabled
        if (config.antiAliasing || config.brightnessTint || config.shadows) {
            // Apply effects only to the rectangle area for performance
            if (config.antiAliasing) {
                applyAntiAliasingBounded(startX, startY, endX, endY);
            }
            if (config.brightnessTint) {
                applyBrightnessTintBounded(startX, startY, endX, endY);
            }
            if (config.shadows) {
                applyShadowEffectBounded(startX, startY, endX, endY);
            }
        }
    }

    /**
     * Helper method to draw a rectangle outline (border only)
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Rectangle width
     * @param height Rectangle height
     * @param color Color in RGB format
     * @param thickness Border thickness in pixels
     */
    public void drawAcceleratedRectangleOutline(int x, int y, int width, int height, int color, int thickness) {
        if (currentPixelBuffer == null || !isEnabled) {
            return;
        }
        
        // Draw top and bottom borders
        for (int t = 0; t < thickness; t++) {
            // Top border
            drawAcceleratedRectangle(x, y + t, width, 1, color);
            // Bottom border
            drawAcceleratedRectangle(x, y + height - 1 - t, width, 1, color);
        }
        
        // Draw left and right borders
        for (int t = 0; t < thickness; t++) {
            // Left border
            drawAcceleratedRectangle(x + t, y, 1, height, color);
            // Right border
            drawAcceleratedRectangle(x + width - 1 - t, y, 1, height, color);
        }
    }
    private void applySimpleAABounded(int startX, int startY, int endX, int endY) {
        int[] tempBuffer = new int[currentPixelBuffer.length];
        System.arraycopy(currentPixelBuffer, 0, tempBuffer, 0, currentPixelBuffer.length);
        
        float strength = config.antiAliasingStrength;
        
        for (int y = Math.max(1, startY); y < Math.min(bufferHeight - 1, endY); y++) {
            for (int x = Math.max(1, startX); x < Math.min(bufferWidth - 1, endX); x++) {
                int index = y * bufferWidth + x;
                
                if (index >= tempBuffer.length - 1) continue;
                
                int center = tempBuffer[index];
                int right = (x + 1 < endX && index + 1 < tempBuffer.length) ? tempBuffer[index + 1] : center;
                int down = (y + 1 < endY && index + bufferWidth < tempBuffer.length) ? tempBuffer[index + bufferWidth] : center;
                
                int r = (int)(((center >> 16) & 0xFF) * (1 - strength) + (((right >> 16) & 0xFF) + ((down >> 16) & 0xFF)) / 2 * strength);
                int g = (int)(((center >> 8) & 0xFF) * (1 - strength) + (((right >> 8) & 0xFF) + ((down >> 8) & 0xFF)) / 2 * strength);
                int b = (int)((center & 0xFF) * (1 - strength) + ((right & 0xFF) + (down & 0xFF)) / 2 * strength);
                
                r = Math.min(255, Math.max(0, r));
                g = Math.min(255, Math.max(0, g));
                b = Math.min(255, Math.max(0, b));
                
                currentPixelBuffer[index] = (r << 16) | (g << 8) | b;
            }
        }
    }
    
    private float getLuminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f;
    }
    
    private int getLuminanceInt(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int)(0.299f * r + 0.587f * g + 0.114f * b);
    }
    
    public void cleanup() {
        disable();
        
        // Clean up GPU processor
        if (gpuProcessor != null) {
            gpuProcessor.cleanup();
            gpuProcessor = null;
        }
        
        if (renderEngine != null) renderEngine.cleanup();
        currentPixelBuffer = null;
        previousFrame = null;
        shadowBuffer = null;
        isInitialized = false;
    }
    
    // Getters
    public boolean isEnabled() { return isEnabled; }
    public boolean isInitialized() { return isInitialized; }
    public GPUConfig getConfig() { return config; }
    public JPanel getSettingsPanel() { return settingsPanel; }
    public int[] getProcessedPixels() { return currentPixelBuffer; }
    
    public void preRender() {}
    public void postRender() { applyEffects(); }
    
    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(gameFrame, message, "GPU Plugin Error", JOptionPane.ERROR_MESSAGE);
        });
    }
}