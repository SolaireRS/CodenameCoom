package com.client;

import com.client.plugins.gpu.GPUPlugin;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class GPUDrawingAreaIntegration {
    
    private static GPUPlugin gpuPlugin;
    private static boolean gpuEnabled = false;
    private static boolean initializationAttempted = false;
    private static int screenWidth;
    private static int screenHeight;
    private static BufferedImage backBuffer;
    private static Graphics2D graphics;
    
    // GPU context initialization - now deferred and safer
    public static void initializeGPUContext(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        
        // Don't initialize OpenGL/GLFW here - just store the dimensions
        // The actual GPU context will be initialized later when the plugin is set
        //System.out.println("GPU Drawing Area dimensions set: " + width + "x" + height);
    }
    
    public static void setGPUPlugin(GPUPlugin plugin) {
        gpuPlugin = plugin;
        
        if (plugin != null && !initializationAttempted) {
            initializationAttempted = true;
            
            try {
                // Try to initialize the plugin in a safe way
                gpuEnabled = plugin.initialize(screenWidth, screenHeight);
                if (gpuEnabled) {
                    //System.out.println("GPU Plugin successfully integrated");
                    
                    // Create back buffer for software fallback
                    if (screenWidth > 0 && screenHeight > 0) {
                        backBuffer = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
                        graphics = backBuffer.createGraphics();
                    }
                } else {
                    //System.out.println("GPU Plugin failed to initialize - using software rendering");
                }
            } catch (Exception e) {
                //System.out.println("GPU Plugin initialization failed: " + e.getMessage());
                gpuEnabled = false;
            }
        } else {
            gpuEnabled = false;
        }
    }
    
    public static boolean isGPUEnabled() {
        return gpuEnabled && gpuPlugin != null;
    }
    
    public static void renderFrame() {
        // CRITICAL NULL CHECKS ADDED HERE
        if (!isGPUEnabled() || gpuPlugin == null) {
            return;
        }
        
        // Check if DrawingArea and its pixels exist before accessing
        if (DrawingArea.pixels == null) {
            System.err.println("DrawingArea.pixels is null - skipping GPU render frame");
            return;
        }
        
        // Validate DrawingArea dimensions
        if (DrawingArea.width <= 0 || DrawingArea.height <= 0) {
            System.err.println("Invalid DrawingArea dimensions - skipping GPU render frame");
            return;
        }
        
        // Validate pixel array length matches dimensions
        if (DrawingArea.pixels.length < (DrawingArea.width * DrawingArea.height)) {
            System.err.println("DrawingArea.pixels array too small for dimensions - skipping GPU render frame");
            return;
        }
        
        try {
            // Pre-render hook
            gpuPlugin.preRender();
            
            // Convert pixel buffer to GPU format - now with null safety
            gpuPlugin.updatePixelBuffer(DrawingArea.pixels, DrawingArea.width, DrawingArea.height);
            
            // Apply GPU effects
            gpuPlugin.applyEffects();
            
            // Post-render hook
            gpuPlugin.postRender();
            
        } catch (Exception e) {
            System.err.println("GPU rendering error: " + e.getMessage());
            e.printStackTrace();
            // Don't disable GPU on single frame errors, just log them
        }
    }
    
    // Method to get processed pixels back from GPU
    public static int[] getProcessedPixels() {
        // NULL CHECK ADDED
        if (!isGPUEnabled() || gpuPlugin == null) {
            return DrawingArea.pixels; // This could still be null, so check it too
        }
        
        // Additional safety check for DrawingArea.pixels
        if (DrawingArea.pixels == null) {
            System.err.println("DrawingArea.pixels is null in getProcessedPixels");
            return null;
        }
        
        try {
            int[] processedPixels = gpuPlugin.getProcessedPixels();
            // Return processed pixels if valid, otherwise fall back to original
            return (processedPixels != null) ? processedPixels : DrawingArea.pixels;
        } catch (Exception e) {
            System.err.println("Failed to get processed pixels: " + e.getMessage());
            return DrawingArea.pixels;
        }
    }
    
    // GPU-accelerated drawing methods with fallbacks
    public static void drawGPUBox(int x, int y, int width, int height, int color) {
        // NULL CHECK ADDED
        if (isGPUEnabled() && gpuPlugin != null) {
            try {
                gpuPlugin.drawAcceleratedRectangle(x, y, width, height, color);
                return;
            } catch (Exception e) {
                System.err.println("GPU rectangle drawing failed: " + e.getMessage());
            }
        }
        
        // Fallback to software rendering - but check if DrawingArea methods exist
        try {
            DrawingArea.drawBox(x, y, width, height, color);
        } catch (Exception e) {
            System.err.println("Software fallback drawing also failed: " + e.getMessage());
        }
    }
    
    public static void drawGPUTransparentBox(int x, int y, int width, int height, int color, int opacity) {
        // NULL CHECK ADDED
        if (isGPUEnabled() && gpuPlugin != null) {
            try {
                gpuPlugin.drawAcceleratedTransparentRectangle(x, y, width, height, color, opacity);
                return;
            } catch (Exception e) {
                System.err.println("GPU transparent rectangle drawing failed: " + e.getMessage());
            }
        }
        
        // Fallback to software rendering - but check if DrawingArea methods exist
        try {
            DrawingArea.drawTransparentBox(x, y, width, height, color, opacity);
        } catch (Exception e) {
            System.err.println("Software transparent fallback drawing also failed: " + e.getMessage());
        }
    }
    
    // Cleanup resources
    public static void cleanup() {
        try {
            if (gpuPlugin != null) {
                gpuPlugin.cleanup();
            }
            
            if (graphics != null) {
                graphics.dispose();
                graphics = null;
            }
            
            if (backBuffer != null) {
                backBuffer.flush();
                backBuffer = null;
            }
            
            gpuEnabled = false;
            initializationAttempted = false;
            gpuPlugin = null;
            
            //System.out.println("GPU Integration cleaned up");
        } catch (Exception e) {
            //System.err.println("GPU cleanup error: " + e.getMessage());
        }
    }
}