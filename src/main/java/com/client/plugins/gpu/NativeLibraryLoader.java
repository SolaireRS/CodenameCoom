package com.client.plugins.gpu;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;

public class NativeLibraryLoader {
    private static boolean loaded = false;
    
    public static void loadNatives() {
        if (loaded) return;
        
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String nativeFolder;
            String[] libs;  // Move this declaration here
            
            if (os.contains("win")) {
                nativeFolder = "natives/windows/";
                libs = new String[]{"lwjgl.dll", "lwjgl_opengl.dll", "glfw.dll"};
            } else if (os.contains("mac")) {
                nativeFolder = "natives/macos/";
                libs = new String[]{"liblwjgl.dylib", "liblwjgl_opengl.dylib", "libglfw.dylib"};
            } else {
                nativeFolder = "natives/linux/";
                libs = new String[]{"liblwjgl.so", "liblwjgl_opengl.so", "libglfw.so"};
            }
            
            // Create temp directory for natives
            Path tempDir = Files.createTempDirectory("lwjgl-natives");
            
            // Extract the correct libraries for this OS
            for (String lib : libs) {
                // Extract from JAR to temp directory
                try (InputStream in = NativeLibraryLoader.class.getResourceAsStream("/" + nativeFolder + lib)) {
                    if (in != null) {
                        Path targetPath = tempDir.resolve(lib);
                        Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("Extracted: " + lib);
                    } else {
                        System.err.println("Could not find native library: /" + nativeFolder + lib);
                    }
                }
            }
            
            // Set library path
            System.setProperty("java.library.path", tempDir.toString());
            System.setProperty("org.lwjgl.librarypath", tempDir.toString());
            
            // Force reload
            Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);
            
            loaded = true;
            System.out.println("Natives loaded from JAR to: " + tempDir);
            
        } catch (Exception e) {
            System.err.println("Failed to load natives from JAR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}