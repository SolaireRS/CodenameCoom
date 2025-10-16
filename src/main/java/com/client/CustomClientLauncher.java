package com.client;

import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import com.client.plugins.gpu.NativeLibraryLoader;
import com.client.sign.Signlink;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.AlphaComposite;
import java.awt.Toolkit;
import java.net.InetAddress;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import java.net.URL;

public class CustomClientLauncher {

    // Operating System Detection
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_MAC = OS_NAME.contains("mac");
    private static final boolean IS_LINUX = OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix");

    // Constants
    private static final int GAME_WIDTH = 765;
    private static final int GAME_HEIGHT = 503;
    private static final int BORDER_SIZE = 25;
    private static final int ICON_SIZE = 16;
    private static final int ICON_PADDING = 1;
    private static final int ICON_DRAW_SIZE = 13;
    private static final int TITLE_BAR_BUTTON_COUNT = 5;
    
    // Cache initialization constants
    private static final int FRAME_INIT_WAIT_MS = 300;
    private static final int CACHE_CHECK_INTERVAL_MS = 100;
    private static final int MAX_HEADER_WAIT_ATTEMPTS = 100;
    private static final int MAX_MODEL_LOAD_ATTEMPTS = 200;
    private static final int MODEL_CHECK_LIMIT = 1000;
    private static final int MIN_LOADED_MODELS = 50;
    private static final int FRAME_READY_TIMEOUT_SECONDS = 10;
    private static final int EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 5;
    
    // Thread-safe reference to current frame
    private static final AtomicReference<CustomGameFrame> currentFrame = new AtomicReference<>();
    
    // Icon cache with weak references to prevent memory leaks
    private static final Map<String, ImageIcon> iconCache = 
        Collections.synchronizedMap(new WeakHashMap<>());
    
    // Thread counter for naming
    private static final AtomicInteger threadCounter = new AtomicInteger(0);
    
    // Thread pool for background tasks - fixed size to prevent unbounded growth
    private static final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("ClientLauncher-Worker-" + threadCounter.incrementAndGet());
        return t;
    });

    public static void main(String[] args) {
        // Register shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            cleanup();
        }));
        
        // Configure platform-specific settings
        configurePlatformSettings();
        NativeLibraryLoader.loadNatives();
        // Show startup splash screen immediately
        SwingUtilities.invokeLater(() -> {
            StartupSplashScreen splashScreen = new StartupSplashScreen();
            splashScreen.show();
        });
    }
    
    /**
     * Configure platform-specific settings for optimal performance and appearance
     */
    private static void configurePlatformSettings() {
        System.out.println("Detected OS: " + OS_NAME);
        
        if (IS_MAC) {
            configureMacSettings();
        } else if (IS_LINUX) {
            configureLinuxSettings();
        } else if (IS_WINDOWS) {
            configureWindowsSettings();
        }
        
        // Set look and feel
        try {
            if (IS_MAC) {
                // Use system look and feel on Mac
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("apple.awt.application.name", "Phoenix Client");
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else if (IS_LINUX) {
                // Try GTK look and feel on Linux, fallback to system
                try {
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
                } catch (Exception e) {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
            } else {
                // Windows - use system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        } catch (Exception e) {
            System.err.println("Failed to set look and feel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Configure macOS-specific settings
     */
    private static void configureMacSettings() {
        // Enable Metal rendering (better than OpenGL on modern Macs)
        System.setProperty("sun.java2d.metal", "true");
        
        // GPU acceleration fallbacks
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.accthresh", "0");
        
        // Enable HiDPI/Retina support
        System.setProperty("apple.awt.graphics.UseQuartz", "true");
        
        // Set dock icon and name
        System.setProperty("apple.awt.application.name", "Phoenix Client");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Phoenix Client");
        
        // Enable full-screen mode support
        System.setProperty("apple.awt.fullscreencapturealldisplays", "false");
        
        // Smooth fonts
        System.setProperty("apple.awt.textantialiasing", "on");
        
        // Detect OS version
        try {
            String osVersion = System.getProperty("os.version");
            System.out.println("macOS version: " + osVersion);
        } catch (Exception e) {
            // Not critical
        }
        
        System.out.println("macOS optimizations enabled (Metal rendering, Retina support)");
    }

    /**
     * Configure Linux-specific settings
     */
    private static void configureLinuxSettings() {
        // OpenGL acceleration - primary method
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.accthresh", "0");
        
        // Translucent acceleration
        System.setProperty("sun.java2d.transaccel", "true");
        
        // Anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        // XRender pipeline for better rendering
        System.setProperty("sun.java2d.xrender", "true");
        
        // Detect Wayland vs X11
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        if (sessionType != null) {
            System.out.println("Linux display server: " + sessionType);
            if ("wayland".equalsIgnoreCase(sessionType)) {
                System.out.println("Wayland detected - some features may behave differently");
            }
        }
        
        System.out.println("Linux optimizations enabled (OpenGL acceleration, XRender)");
    }

    /**
     * Configure Windows-specific settings
     */
    private static void configureWindowsSettings() {
        // Direct3D acceleration (primary for Windows)
        System.setProperty("sun.java2d.d3d", "true");
        System.setProperty("sun.java2d.ddoffscreen", "true");
        System.setProperty("sun.java2d.ddforcevram", "true");
        
        // OpenGL as fallback
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.accthresh", "0");
        
        // Translucent acceleration
        System.setProperty("sun.java2d.transaccel", "true");
        
        // Disable DPI scaling issues on Windows
        System.setProperty("sun.java2d.uiScale", "1.0");
        
        // Performance tracing (optional - comment out if not needed)
        // System.setProperty("sun.java2d.trace", "timestamp,log,count");
        
        System.out.println("Windows optimizations enabled (Direct3D/OpenGL acceleration)");
    }
    /**
     * Get HiDPI scale factor for the display
     * Fixed: Returns 1.0 as safe fallback instead of assuming 2.0
     */
    private static double getDisplayScaleFactor() {
        if (IS_MAC) {
            // macOS Retina displays
            try {
                return Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
            } catch (Exception e) {
                return 1.0; // Safe fallback - not all Macs have Retina
            }
        }
        return 1.0;
    }
    
    /**
     * Cleanup resources on shutdown
     */
    private static void cleanup() {
        System.out.println("Shutting down client launcher...");
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        iconCache.clear();
        System.out.println("Cleanup complete");
    }
    
    public static void launchMainClient() {
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("Requirements check passed. Starting Phoenix Client...");

                // Pre-initialize Sprite in background thread
                executor.submit(() -> {
                    try {
                        Sprite.init();
                    } catch (Exception e) {
                        System.err.println("Error initializing sprites: " + e.getMessage());
                        e.printStackTrace();
                    }
                });

                // Create the custom frame
                CustomGameFrame customFrame = new CustomGameFrame("Phoenix Client");
                currentFrame.set(customFrame);

                // Platform-specific frame configuration
                configurePlatformFrame(customFrame);

                // Create ClientWindow FIRST
                ClientWindow clientWindow = createClientWindowWithoutInit();

                // Set Client.instance to the actual client from ClientWindow
                Client.instance = clientWindow.getActualClient();

                // Override the default frame with our custom frame
                ClientWindow.frame = customFrame;

                // Get reference to the EXP tracker from the custom frame
                ExpTrackerPanel expTracker = customFrame.getExpTracker();
                if (expTracker != null) {
                    connectExpTracker(clientWindow, expTracker);
                }

                // Create game panel and add the clientWindow
                JPanel gamePanel = new JPanel();
                gamePanel.setLayout(new BorderLayout());
                gamePanel.add(clientWindow);
                
                // Adjust size for HiDPI displays
                double scaleFactor = getDisplayScaleFactor();
                int scaledWidth = (int)(GAME_WIDTH * scaleFactor);
                int scaledHeight = (int)(GAME_HEIGHT * scaleFactor);
                gamePanel.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));

                // Set the client in our custom frame
                customFrame.setGameClient(gamePanel);

                // Make frame visible FIRST
                customFrame.setVisible(true);

                // Add border (platform-specific)
                applyPlatformBorder(customFrame);

                // NOW initialize client AFTER frame is visible
                initializeClientAfterFrameVisible(clientWindow);

                // Apply title bar icons if not on Mac (Mac uses native controls)
                if (!IS_MAC) {
                    SwingUtilities.invokeLater(() -> {
                        addTitleBarIcons(customFrame, null);
                    });
                } else {
                    System.out.println("Skipping custom title bar icons on macOS (using native controls)");
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
    
    /**
     * Configure frame based on platform
     */
    private static void configurePlatformFrame(CustomGameFrame customFrame) {
        try {
            if (IS_MAC) {
                // On Mac, don't use undecorated - use native decorations
                customFrame.setUndecorated(false);
                customFrame.setSize(GAME_WIDTH, GAME_HEIGHT + 28); // Account for title bar
                
                // Enable full-screen support
                try {
                    Class<?> fullScreenUtilities = Class.forName("com.apple.eawt.FullScreenUtilities");
                    fullScreenUtilities.getMethod("setWindowCanFullScreen", 
                        java.awt.Window.class, boolean.class).invoke(null, customFrame, true);
                    System.out.println("macOS full-screen support enabled");
                } catch (Exception e) {
                    System.out.println("Could not enable full-screen (older macOS?)");
                }
            } else if (IS_LINUX) {
                // On Linux, undecorated works but needs window manager hints
                customFrame.setUndecorated(true);
                customFrame.setSize(GAME_WIDTH + BORDER_SIZE + 2, GAME_HEIGHT + BORDER_SIZE + 2);
                
                // Set window type hint for better window manager compatibility
                try {
                    customFrame.setType(java.awt.Window.Type.NORMAL);
                } catch (Exception e) {
                    System.out.println("Could not set window type hint");
                }
            } else {
                // Windows - undecorated works fine
                customFrame.setUndecorated(true);
                customFrame.setSize(GAME_WIDTH + BORDER_SIZE + 2, GAME_HEIGHT + BORDER_SIZE + 2);
            }
            
            customFrame.setLocationRelativeTo(null);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to safe defaults
            customFrame.setSize(GAME_WIDTH, GAME_HEIGHT);
            customFrame.setLocationRelativeTo(null);
        }
    }
    
    /**
     * Apply platform-specific border styling
     */
    private static void applyPlatformBorder(CustomGameFrame customFrame) {
        // No border - clean look
        customFrame.getRootPane().setBorder(null);
    }
    
    /**
     * Connect EXP tracker to client window
     */
    private static void connectExpTracker(ClientWindow clientWindow, ExpTrackerPanel expTracker) {
        if (clientWindow == null || expTracker == null) {
            System.out.println("Cannot connect EXP tracker: null reference");
            return;
        }
        
        try {
            if (hasMethod(clientWindow.getClass(), "setExpTracker", ExpTrackerPanel.class)) {
                clientWindow.getClass().getMethod("setExpTracker", ExpTrackerPanel.class)
                    .invoke(clientWindow, expTracker);
                System.out.println("EXP tracker connected to client");
            } else {
                System.out.println("EXP tracker integration method not found - will work without it");
            }
        } catch (Exception e) {
            System.out.println("EXP tracker integration failed: " + e.getMessage());
        }
    }
    
    private static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            clazz.getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    /**
     * Optimized icon loading with caching and weak references - ORIGINAL WORKING VERSION
     */
    private static ImageIcon[] loadTitleBarIcons(String iconPack) {
        String[] iconNames = {
            "settings.png",
            "screenshot.png", 
            "minimize.png",
            "maximize.png",
            "close.png"
        };

        ImageIcon[] icons = new ImageIcon[iconNames.length];

        for (int i = 0; i < iconNames.length; i++) {
            String iconPath = (iconPack == null) ? 
                "/icons/" + iconNames[i] : 
                "/icons/" + iconPack + "/" + iconNames[i];
            
            icons[i] = loadCachedIcon(iconPath);
            
            if (icons[i] == null && iconPack != null) {
                // Fallback to default
                String fallbackPath = "/icons/" + iconNames[i];
                icons[i] = loadCachedIcon(fallbackPath);
            }
            
            if (icons[i] == null) {
                icons[i] = createEmptyIcon(ICON_SIZE);
            }
        }
        return icons;
    }
    
    /**
     * Load cached icon - ORIGINAL WORKING VERSION WITH IMPROVED ERROR MESSAGES
     */
    private static ImageIcon loadCachedIcon(String path) {
        return iconCache.computeIfAbsent(path, p -> {
            Graphics2D g2 = null;
            try {
                URL url = CustomClientLauncher.class.getResource(p);
                if (url != null) {
                    // Adjust icon size for HiDPI displays
                    double scaleFactor = getDisplayScaleFactor();
                    int iconSize = (int)(ICON_SIZE * scaleFactor);
                    
                    BufferedImage originalImg = new BufferedImage(
                        iconSize, iconSize, BufferedImage.TYPE_INT_ARGB);
                    g2 = originalImg.createGraphics();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                        RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    
                    ImageIcon tempIcon = new ImageIcon(url);
                    int padding = (int)(ICON_PADDING * scaleFactor);
                    int drawSize = (int)(ICON_DRAW_SIZE * scaleFactor);
                    g2.drawImage(tempIcon.getImage(), padding, padding, 
                        drawSize, drawSize, null);
                    
                    return new ImageIcon(originalImg);
                } else {
                    System.err.println("Icon resource not found: " + p);
                }
            } catch (Exception e) {
                System.err.println("Failed to load icon '" + p + "': " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (g2 != null) {
                    g2.dispose();
                }
            }
            return null;
        });
    }

    private static ImageIcon createEmptyIcon(int size) {
        Image img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        return new ImageIcon(img);
    }
    
    private static void addTitleBarIcons(CustomGameFrame customFrame, String iconPack) {
        try {
            searchForTitleBarButtons(customFrame, 0, customFrame, iconPack);
        } catch (Exception e) {
            System.out.println("Error in addTitleBarIcons: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void searchForTitleBarButtons(Container container, int depth, 
            CustomGameFrame customFrame, String iconPack) {
        // Safety check for max recursion depth
        if (depth > 10) {
            return;
        }
        
        Component[] components = container.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                
                if (isTitleBarControlsPanel(panel)) {
                    setTitleBarIcons(panel, customFrame, iconPack);
                    return;
                }
            }
            
            if (comp instanceof Container) {
                searchForTitleBarButtons((Container) comp, depth + 1, customFrame, iconPack);
            }
        }
    }

    private static boolean isTitleBarControlsPanel(JPanel panel) {
        Component[] components = panel.getComponents();
        int buttonCount = 0;
        for (Component comp : components) {
            if (comp instanceof JButton) buttonCount++;
        }
        
        return buttonCount == TITLE_BAR_BUTTON_COUNT && panel.getLayout() instanceof FlowLayout;
    }
    
    private static void setTitleBarIcons(JPanel panel, CustomGameFrame frame, String iconPack) {
        Component[] components = panel.getComponents();
        ImageIcon[] icons = loadTitleBarIcons(iconPack);
        int iconIndex = 0;

        for (Component comp : components) {
            if (comp instanceof JButton && iconIndex < icons.length) {
                JButton button = (JButton) comp;

                ImageIcon icon = icons[iconIndex];
                button.setIcon(icon);
                button.setText("");

                button.setOpaque(false);
                button.setContentAreaFilled(false);
                button.setBorderPainted(false);
                button.setFocusPainted(false);

                // Remove old listeners and add new optimized one
                java.awt.event.MouseListener[] oldListeners = button.getMouseListeners();
                for (java.awt.event.MouseListener listener : oldListeners) {
                    if (listener.getClass().getName().contains("$")) {
                        button.removeMouseListener(listener);
                    }
                }

                button.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        button.setContentAreaFilled(true);
                        Color hoverColor = getThemeHoverColor(frame);
                        button.setBackground(hoverColor);
                    }
                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        button.setContentAreaFilled(false);
                    }
                });

                iconIndex++;
            }
        }
        System.out.println("Applied " + iconIndex + " title bar icons for pack: " + 
            (iconPack != null ? iconPack : "default"));
    }

    public static void refreshTitleBarIcons(String iconPack) {
        // Skip on macOS as it uses native controls
        if (IS_MAC) {
            System.out.println("Icon refresh skipped on macOS (using native controls)");
            return;
        }
        
        System.out.println("Refreshing title bar icons to pack: " + iconPack);
        
        CustomGameFrame frame = currentFrame.get();
        if (frame != null) {
            SwingUtilities.invokeLater(() -> {
                try {
                    searchForTitleBarButtons(frame, 0, frame, iconPack);
                    System.out.println("Title bar icons refreshed to: " + iconPack);
                } catch (Exception e) {
                    System.err.println("Failed to refresh title bar icons: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } else {
            System.err.println("No frame reference available for title bar icon refresh");
        }
    }

    private static Color getThemeHoverColor(CustomGameFrame frame) {
        try {
            if (frame != null && frame.getThemeManager() != null) {
                return frame.getThemeManager().getHoverColor();
            }
        } catch (Exception e) {
            // Fallback to default
        }
        return new Color(0x484848);
    }

    private static ClientWindow createClientWindowWithoutInit() {
        try {
            System.out.println("Creating ClientWindow without calling init()...");
            
            String[] args = new String[0];
            
            ClientWindow clientWindow = new ClientWindow(args) {
                @Override
                public void initUI() {
                    System.out.println("Bypassing default ClientWindow.initUI() - using custom frame instead");
                    
                    try {
                        setFocusTraversalKeysEnabled(false);
                        System.out.println("Client UI setup completed (init() deferred)");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
            
            clientWindow.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
            clientWindow.setSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
            clientWindow.setVisible(true);
            clientWindow.setFocusable(true);
            
            System.out.println("ClientWindow created without init()");
            return clientWindow;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Initialize client after frame is visible using proper waiting mechanisms
     */
    private static void initializeClientAfterFrameVisible(Client client) {
        executor.submit(() -> {
            try {
                System.out.println("Initializing client AFTER frame is visible...");

                // Use CountDownLatch for proper waiting instead of Thread.sleep
                CountDownLatch frameLatch = new CountDownLatch(1);
                SwingUtilities.invokeLater(() -> {
                    frameLatch.countDown();
                });
                
                // Wait for frame to be ready with improved timeout
                if (!frameLatch.await(FRAME_READY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    System.err.println("Timeout waiting for frame initialization");
                    return;
                }

                // Small delay for frame painting (adjusted for slower systems)
                Thread.sleep(FRAME_INIT_WAIT_MS);

                // Initialize the RSApplet properly
                SwingUtilities.invokeLater(() -> {
                    client.init();
                    client.start();
                    System.out.println("Client init() and start() called successfully");
                });

                // Wait for cache to load
                waitForCacheLoad(client);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Client initialization interrupted");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Wait for cache to load with proper timeout handling
     */
    private static void waitForCacheLoad(Client client) {
        try {
            System.out.println("Waiting for cache to load...");

            int attempts = 0;
            while (Model.modelHeaders == null && attempts < MAX_HEADER_WAIT_ATTEMPTS) {
                Thread.sleep(CACHE_CHECK_INTERVAL_MS);
                attempts++;
            }

            if (Model.modelHeaders != null) {
                System.out.println("Model headers array created, waiting for models to load...");

                attempts = 0;
                while (attempts < MAX_MODEL_LOAD_ATTEMPTS) {
                    int loadedCount = countLoadedModels();

                    if (loadedCount > MIN_LOADED_MODELS) {
                        System.out.println("Cache loaded successfully with " + loadedCount + " models!");
                        break;
                    }

                    Thread.sleep(CACHE_CHECK_INTERVAL_MS);
                    attempts++;
                }
            } else {
                System.err.println("Model headers never initialized");
            }

            SwingUtilities.invokeLater(() -> {
                client.requestFocus();
                client.requestFocusInWindow();
                System.out.println("Client ready for game processing");
            });

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Cache loading interrupted");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Count loaded models safely
     */
    private static int countLoadedModels() {
        if (Model.modelHeaders == null) {
            return 0;
        }
        
        int loadedCount = 0;
        int checkLimit = Math.min(MODEL_CHECK_LIMIT, Model.modelHeaders.length);
        
        for (int i = 0; i < checkLimit; i++) {
            if (Model.modelHeaders[i] != null) {
                loadedCount++;
            }
        }
        
        return loadedCount;
    }
    
    /**
     * Utility method to check if running on macOS
     */
    public static boolean isMac() {
        return IS_MAC;
    }
    
    /**
     * Utility method to check if running on Linux
     */
    public static boolean isLinux() {
        return IS_LINUX;
    }
    
    /**
     * Utility method to check if running on Windows
     */
    public static boolean isWindows() {
        return IS_WINDOWS;
    }
}