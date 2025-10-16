package com.client;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class StartupSplashScreen {
    private JDialog splashDialog;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JLabel phoenixIconLabel;
    
    // Required Java version
    private static final String REQUIRED_JAVA_VERSION = "1.8.0_181";
    private static final int REQUIRED_MAJOR = 1;
    private static final int REQUIRED_MINOR = 8;
    private static final int REQUIRED_PATCH = 181;
    
    // Timing constants
    private static final int INITIAL_DELAY_MS = 800;
    private static final int CHECK_DELAY_MS = 500;
    private static final int DOWNLOAD_COMPLETE_DELAY_MS = 1000;
    private static final int LAUNCH_DELAY_MS = 1200;
    
    // Download constants
    private static final int DOWNLOAD_BUFFER_SIZE = 8192;
    private static final int PROGRESS_UPDATE_INTERVAL_MS = 250; // Update UI every 250ms
    private static final int CONNECT_TIMEOUT_MS = 10000; // 10 seconds
    private static final int READ_TIMEOUT_MS = 30000; // 30 seconds
    private static final long BYTES_PER_MB = 1024 * 1024;
    
    // Platform detection
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_MAC = OS_NAME.contains("mac");
    private static final boolean IS_LINUX = OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix");
    
    // Download URLs for each platform
    private static final String JAVA8_WINDOWS_URL = "https://javadl.oracle.com/webapps/download/AutoDL?BundleId=245479_d3c52aa6bfa54d3ca74e617f18309292";
    private static final String JAVA8_MAC_URL = "https://javadl.oracle.com/webapps/download/AutoDL?BundleId=245050_d3c52aa6bfa54d3ca74e617f18309292";
    private static final String JAVA8_LINUX_URL = "https://javadl.oracle.com/webapps/download/AutoDL?BundleId=245051_d3c52aa6bfa54d3ca74e617f18309292";
    
    // Track downloaded file for cleanup
    private File downloadedFile = null;
    
    public StartupSplashScreen() {
        createSplashScreen();
    }
    
    private void createSplashScreen() {
        splashDialog = new JDialog((Frame) null, "Phoenix Client", false);
        splashDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        splashDialog.setUndecorated(true);
        splashDialog.setAlwaysOnTop(true);
        
        // Load the background image first to get dimensions
        ImageIcon backgroundIcon = loadBackgroundImage();
        if (backgroundIcon == null) {
            createFallbackSplash();
            return;
        }
        
        // Adjust for HiDPI displays
        double scaleFactor = getDisplayScaleFactor();
        int imageWidth = (int)(backgroundIcon.getIconWidth() / scaleFactor);
        int imageHeight = (int)(backgroundIcon.getIconHeight() / scaleFactor);
        
        splashDialog.setSize(imageWidth, imageHeight);
        splashDialog.setLocationRelativeTo(null);
        
        // Create a custom panel that paints the background image and text
        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                // Draw the background image
                g2d.drawImage(backgroundIcon.getImage(), 0, 0, getWidth(), getHeight(), null);
                
                // Draw "Phoenix Client" text on the image
                g2d.setColor(new Color(0x00a2e8));
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
                FontMetrics fm = g2d.getFontMetrics();
                String title = " ";
                int titleX = (getWidth() - fm.stringWidth(title)) / 2;
                int titleY = getHeight() / 2 + 40;
                g2d.drawString(title, titleX, titleY);
                
                // Draw status text
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                fm = g2d.getFontMetrics();
                String status = getCurrentStatusText();
                int statusX = (getWidth() - fm.stringWidth(status)) / 2;
                int statusY = titleY + 50;
                g2d.drawString(status, statusX, statusY);
                
                // Draw progress bar if visible
                if (progressBar.isVisible()) {
                    int barWidth = 200;
                    int barHeight = 20;
                    int barX = (getWidth() - barWidth) / 2;
                    int barY = statusY + 25;
                    
                    // Progress bar background
                    g2d.setColor(new Color(0x3c, 0x3c, 0x3c));
                    g2d.fillRoundRect(barX, barY, barWidth, barHeight, 10, 10);
                    
                    // Progress bar fill
                    g2d.setColor(new Color(0x00a2e8));
                    int fillWidth = (int) ((progressBar.getValue() / 100.0) * barWidth);
                    if (fillWidth > 0) {
                        g2d.fillRoundRect(barX, barY, fillWidth, barHeight, 10, 10);
                    }
                    
                    // Progress text
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    fm = g2d.getFontMetrics();
                    String progressText = progressBar.getString();
                    if (progressText != null && !progressText.isEmpty()) {
                        int progressTextX = (getWidth() - fm.stringWidth(progressText)) / 2;
                        int progressTextY = barY + 15;
                        g2d.drawString(progressText, progressTextX, progressTextY);
                    }
                }
            }
        };
        
        imagePanel.setOpaque(false);
        imagePanel.setLayout(null);
        
        statusLabel = new JLabel("Checking requirements...");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setVisible(false);
        
        try {
            splashDialog.setBackground(new Color(0, 0, 0, 0));
        } catch (Exception e) {
            System.out.println("Could not set transparent background: " + e.getMessage());
        }
        
        splashDialog.add(imagePanel);
    }
    
    /**
     * Get HiDPI scale factor for the display
     * Fixed: Returns 1.0 as safe fallback instead of assuming 2.0
     */
    private double getDisplayScaleFactor() {
        if (IS_MAC) {
            try {
                return Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
            } catch (Exception e) {
                return 1.0; // Safe fallback - not all Macs have Retina
            }
        }
        return 1.0;
    }
    
    private ImageIcon loadBackgroundImage() {
        try {
            URL iconUrl = getClass().getResource("/icons/startuper.png");
            if (iconUrl != null) {
                return new ImageIcon(iconUrl);
            } else {
                System.err.println("Could not find startuper.png");
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error loading startuper.png: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private String getCurrentStatusText() {
        return statusLabel.getText();
    }
    
    private void createFallbackSplash() {
        splashDialog.setSize(400, 200);
        splashDialog.setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x00a2e8), 2),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));
        mainPanel.setBackground(new Color(0x2b, 0x2b, 0x2b));
        
        JLabel titleLabel = new JLabel("Phoenix Client");
        titleLabel.setForeground(new Color(0x00a2e8));
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        statusLabel = new JLabel("Checking requirements...");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("");
        progressBar.setForeground(new Color(0x00a2e8));
        progressBar.setBackground(new Color(0x3c, 0x3c, 0x3c));
        progressBar.setVisible(false);
        
        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBackground(new Color(0x2b, 0x2b, 0x2b));
        contentPanel.add(titleLabel, BorderLayout.NORTH);
        contentPanel.add(statusLabel, BorderLayout.CENTER);
        contentPanel.add(progressBar, BorderLayout.SOUTH);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        splashDialog.add(mainPanel);
    }
    
    public void show() {
        System.out.println("DEBUG: show() called on " + OS_NAME);
        splashDialog.setVisible(true);
        startRequirementsCheck();
    }
    
    private void startRequirementsCheck() {
        System.out.println("DEBUG: startRequirementsCheck() called");
        
        new Thread(() -> {
            try {
                System.out.println("DEBUG: Thread started, checking requirements");
                Thread.sleep(INITIAL_DELAY_MS);
                
                SwingUtilities.invokeLater(() -> {
                    System.out.println("DEBUG: Updating to 'Checking Java...'");
                    statusLabel.setText("Checking Java...");
                });
                
                Thread.sleep(CHECK_DELAY_MS);
                
                // First check current Java version
                String currentJavaVersion = System.getProperty("java.version");
                System.out.println("DEBUG: Current Java version: " + currentJavaVersion);
                
                boolean hasCorrectJava = checkJavaVersion(currentJavaVersion);
                
                if (!hasCorrectJava) {
                    System.out.println("DEBUG: Current Java insufficient, searching system...");
                    
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Searching for Java 8...");
                    });
                    
                    // Search for Java 8 installations on the system
                    String java8Path = searchForJava8();
                    
                    if (java8Path != null) {
                        System.out.println("DEBUG: Found Java 8 at: " + java8Path);
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Found Java 8, restarting...");
                        });
                        Thread.sleep(DOWNLOAD_COMPLETE_DELAY_MS);
                        
                        // Restart application with found Java 8
                        restartWithJava8(java8Path);
                        return;
                    } else {
                        System.out.println("DEBUG: Java 8 not found, need to download");
                        
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Java 8 required");
                        });
                        Thread.sleep(DOWNLOAD_COMPLETE_DELAY_MS);
                        
                        // Prompt user to download
                        boolean userWantsDownload = promptUserForDownload();
                        if (!userWantsDownload) {
                            showError("Java 8 is required to run Phoenix Client.");
                            return;
                        }
                        
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Downloading Java 8...");
                            progressBar.setVisible(true);
                            progressBar.setValue(0);
                            progressBar.setString("Connecting...");
                            splashDialog.revalidate();
                        });
                        
                        boolean downloadSuccess = downloadJava8();
                        if (!downloadSuccess) {
                            return;
                        }
                    }
                } else {
                    System.out.println("DEBUG: Java version is sufficient");
                }
                
                SwingUtilities.invokeLater(() -> {
                    System.out.println("DEBUG: Updating to 'Launching Phoenix Client...'");
                    statusLabel.setText("Launching Phoenix Client...");
                });
                
                Thread.sleep(LAUNCH_DELAY_MS);
                
                SwingUtilities.invokeLater(() -> {
                    System.out.println("DEBUG: Closing splash and launching main client");
                    splashDialog.setVisible(false);
                    splashDialog.dispose();
                    
                    System.out.println("DEBUG: About to call CustomClientLauncher.launchMainClient()");
                    CustomClientLauncher.launchMainClient();
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("DEBUG: Requirements check interrupted: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    showError("Startup interrupted: " + e.getMessage());
                });
            } catch (Exception e) {
                System.err.println("DEBUG: Exception in requirements check thread: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    showError("Startup failed: " + e.getMessage());
                });
            }
        }, "RequirementsCheck-Thread").start();
    }
    
    private boolean checkJavaVersion(String version) {
        try {
            System.out.println("Checking Java version: " + version);
            
            // Parse version string (handles both old and new formats)
            // Old: 1.8.0_181
            // New: 8.0.181, 11.0.1, etc.
            
            Pattern oldPattern = Pattern.compile("1\\.(\\d+)\\.(\\d+)_(\\d+)");
            Matcher oldMatcher = oldPattern.matcher(version);
            
            if (oldMatcher.find()) {
                int minor = Integer.parseInt(oldMatcher.group(1));
                int patch = Integer.parseInt(oldMatcher.group(3));
                
                // Must be Java 8 (1.8.x) with patch >= 181
                boolean isValid = minor == 8 && patch >= REQUIRED_PATCH;
                System.out.println("Version check (old format): Java 1." + minor + " patch " + patch + " -> " + isValid);
                return isValid;
            }
            
            // Check new version format (Java 9+)
            Pattern newPattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)");
            Matcher newMatcher = newPattern.matcher(version);
            
            if (newMatcher.find()) {
                int major = Integer.parseInt(newMatcher.group(1));
                // We specifically need Java 8, not newer versions
                boolean isValid = major == 8;
                System.out.println("Version check (new format): Java " + major + " -> " + isValid);
                return isValid;
            }
            
            System.out.println("Could not parse version string: " + version);
            return false;
        } catch (NumberFormatException e) {
            System.err.println("Error parsing Java version numbers: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error checking Java version: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Search the system for Java 8 installations
     */
    private String searchForJava8() {
        System.out.println("Searching for Java 8 installations on " + OS_NAME);
        
        List<String> searchPaths = new ArrayList<>();
        
        if (IS_WINDOWS) {
            searchPaths.add("C:\\Program Files\\Java");
            searchPaths.add("C:\\Program Files (x86)\\Java");
            String programFiles = System.getenv("ProgramFiles");
            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            if (programFiles != null) searchPaths.add(programFiles + "\\Java");
            if (programFilesX86 != null) searchPaths.add(programFilesX86 + "\\Java");
        } else if (IS_MAC) {
            searchPaths.add("/Library/Java/JavaVirtualMachines");
            searchPaths.add(System.getProperty("user.home") + "/Library/Java/JavaVirtualMachines");
            searchPaths.add("/System/Library/Java/JavaVirtualMachines");
        } else if (IS_LINUX) {
            searchPaths.add("/usr/lib/jvm");
            searchPaths.add("/usr/java");
            searchPaths.add("/opt/java");
            searchPaths.add(System.getProperty("user.home") + "/.jdks");
        }
        
        for (String basePath : searchPaths) {
            if (basePath == null) continue;
            
            File baseDir = new File(basePath);
            if (!baseDir.exists() || !baseDir.isDirectory()) {
                continue;
            }
            
            File[] subdirs = baseDir.listFiles();
            if (subdirs == null) continue;
            
            for (File dir : subdirs) {
                if (!dir.isDirectory()) continue;
                
                String dirName = dir.getName().toLowerCase();
                if (dirName.contains("1.8") || dirName.contains("jdk1.8") || 
                    dirName.contains("jre1.8") || dirName.contains("java-8")) {
                    
                    // Find java executable
                    String javaExec = findJavaExecutable(dir);
                    if (javaExec != null) {
                        // Verify it's actually Java 8
                        if (verifyJavaVersion(javaExec)) {
                            return javaExec;
                        }
                    }
                }
            }
        }
        
        System.out.println("No Java 8 installation found in standard locations");
        return null;
    }
    
    private String findJavaExecutable(File javaHome) {
        String[] possiblePaths;
        
        if (IS_WINDOWS) {
            possiblePaths = new String[] {
                "bin\\java.exe",
                "jre\\bin\\java.exe"
            };
        } else {
            possiblePaths = new String[] {
                "bin/java",
                "jre/bin/java",
                "Contents/Home/bin/java"  // macOS specific
            };
        }
        
        for (String path : possiblePaths) {
            File javaExec = new File(javaHome, path);
            if (javaExec.exists() && javaExec.canExecute()) {
                return javaExec.getAbsolutePath();
            }
        }
        
        return null;
    }
    
    /**
     * Verify Java version with proper resource management
     */
    private boolean verifyJavaVersion(String javaPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(javaPath, "-version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("version")) {
                        System.out.println("Found Java version info: " + line);
                        
                        Pattern pattern = Pattern.compile("\"(.+?)\"");
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            String version = matcher.group(1);
                            return checkJavaVersion(version);
                        }
                    }
                }
            }
            
            process.waitFor();
        } catch (IOException e) {
            System.err.println("IO error verifying Java at " + javaPath + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while verifying Java at " + javaPath);
        } catch (Exception e) {
            System.err.println("Error verifying Java at " + javaPath + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    private void restartWithJava8(String java8Path) {
        try {
            System.out.println("Restarting with Java 8: " + java8Path);
            
            // Get current JAR path
            String jarPath = new File(
                StartupSplashScreen.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).getPath();
            
            List<String> command = new ArrayList<>();
            command.add(java8Path);
            command.add("-jar");
            command.add(jarPath);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.start();
            
            System.exit(0);
        } catch (Exception e) {
            System.err.println("Error restarting with Java 8: " + e.getMessage());
            e.printStackTrace();
            showError("Failed to restart with Java 8: " + e.getMessage());
        }
    }
    
    private boolean promptUserForDownload() {
        final boolean[] result = new boolean[1];
        
        try {
            SwingUtilities.invokeAndWait(() -> {
                String message = "Phoenix Client requires Java 8 (version " + REQUIRED_JAVA_VERSION + " or higher).\n\n" +
                               "Would you like to download and install it now?";
                
                int choice = JOptionPane.showConfirmDialog(
                    splashDialog,
                    message,
                    "Java 8 Required",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                result[0] = (choice == JOptionPane.YES_OPTION);
            });
        } catch (Exception e) {
            System.err.println("Error showing download prompt: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        return result[0];
    }
    
    /**
     * Download Java 8 with proper timeout handling and progress updates
     */
    private boolean downloadJava8() {
        InputStream inputStream = null;
        FileOutputStream fos = null;
        
        try {
            System.out.println("DEBUG: Starting Java 8 download for " + OS_NAME);
            
            String downloadUrl;
            String fileName;
            
            if (IS_WINDOWS) {
                downloadUrl = JAVA8_WINDOWS_URL;
                fileName = "jre-8u181-windows-x64.exe";
            } else if (IS_MAC) {
                downloadUrl = JAVA8_MAC_URL;
                fileName = "jre-8u181-macosx-x64.dmg";
            } else if (IS_LINUX) {
                downloadUrl = JAVA8_LINUX_URL;
                fileName = "jre-8u181-linux-x64.tar.gz";
            } else {
                showError("Unsupported operating system: " + OS_NAME);
                return false;
            }
            
            downloadedFile = new File(System.getProperty("java.io.tmpdir"), fileName);
            System.out.println("DEBUG: Downloading to: " + downloadedFile.getAbsolutePath());
            
            URL website = new URL(downloadUrl);
            URLConnection connection = website.openConnection();
            
            // Set timeouts to prevent hanging
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            
            // Get file size for progress calculation
            long totalBytes = connection.getContentLengthLong();
            long downloadedBytes = 0;
            
            System.out.println("DEBUG: Download file size: " + totalBytes + " bytes (" + 
                (totalBytes / BYTES_PER_MB) + " MB)");
            
            if (totalBytes <= 0) {
                System.err.println("Warning: Could not determine download size");
            }
            
            // Download with time-based progress updates
            inputStream = connection.getInputStream();
            fos = new FileOutputStream(downloadedFile);
            
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            int bytesRead;
            long lastUpdateTime = System.currentTimeMillis();
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;
                
                // Update progress based on time interval, not byte count
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdateTime >= PROGRESS_UPDATE_INTERVAL_MS) {
                    lastUpdateTime = currentTime;
                    
                    if (totalBytes > 0) {
                        final int progress = (int) ((downloadedBytes * 100) / totalBytes);
                        final long mbDownloaded = downloadedBytes / BYTES_PER_MB;
                        final long mbTotal = totalBytes / BYTES_PER_MB;
                        
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(progress);
                            progressBar.setString(progress + "% (" + mbDownloaded + "MB / " + mbTotal + "MB)");
                            splashDialog.repaint();
                        });
                    } else {
                        // Unknown size, just show downloaded amount
                        final long mbDownloaded = downloadedBytes / BYTES_PER_MB;
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setIndeterminate(true);
                            progressBar.setString("Downloaded " + mbDownloaded + "MB...");
                            splashDialog.repaint();
                        });
                    }
                }
            }
            
            System.out.println("DEBUG: Download completed successfully. Total: " + downloadedBytes + " bytes");
            
            // Verify download completed
            if (totalBytes > 0 && downloadedBytes != totalBytes) {
                throw new IOException("Download incomplete: expected " + totalBytes + " bytes, got " + downloadedBytes);
            }
            
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(100);
                progressBar.setString("Complete");
                statusLabel.setText("Download complete. Launching installer...");
                splashDialog.repaint();
            });
            
            Thread.sleep(DOWNLOAD_COMPLETE_DELAY_MS);
            
            // Launch installer based on platform
            launchInstaller(downloadedFile);
            
            return false;
            
        } catch (IOException e) {
            System.err.println("DEBUG: Download failed (IO): " + e.getMessage());
            e.printStackTrace();
            cleanupDownloadedFile();
            SwingUtilities.invokeLater(() -> {
                showError("Failed to download Java 8: " + e.getMessage() + 
                    "\n\nPlease check your internet connection and try again.");
            });
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("DEBUG: Download interrupted: " + e.getMessage());
            cleanupDownloadedFile();
            SwingUtilities.invokeLater(() -> {
                showError("Download was interrupted.");
            });
            return false;
        } catch (Exception e) {
            System.err.println("DEBUG: Download failed (unexpected): " + e.getMessage());
            e.printStackTrace();
            cleanupDownloadedFile();
            SwingUtilities.invokeLater(() -> {
                showError("Failed to download Java 8: " + e.getMessage());
            });
            return false;
        } finally {
            // Ensure streams are closed
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                System.err.println("Error closing input stream: " + e.getMessage());
            }
            try {
                if (fos != null) fos.close();
            } catch (IOException e) {
                System.err.println("Error closing output stream: " + e.getMessage());
            }
        }
    }
    
    /**
     * Clean up partially downloaded file
     */
    private void cleanupDownloadedFile() {
        if (downloadedFile != null && downloadedFile.exists()) {
            try {
                if (downloadedFile.delete()) {
                    System.out.println("Cleaned up partial download: " + downloadedFile.getAbsolutePath());
                } else {
                    System.err.println("Could not delete partial download: " + downloadedFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("Error cleaning up download: " + e.getMessage());
            }
        }
    }
    
    private void launchInstaller(File installerFile) throws IOException {
        System.out.println("DEBUG: Launching installer: " + installerFile.getAbsolutePath());
        
        if (IS_WINDOWS) {
            // Windows: Run .exe installer
            ProcessBuilder pb = new ProcessBuilder(installerFile.getAbsolutePath());
            pb.start();
            
            showInstallationMessage(
                "The Java 8 installer has been launched.\n\n" +
                "Please complete the installation and restart Phoenix Client."
            );
            
        } else if (IS_MAC) {
            // macOS: Open .dmg file
            ProcessBuilder pb = new ProcessBuilder("open", installerFile.getAbsolutePath());
            pb.start();
            
            showInstallationMessage(
                "The Java 8 disk image has been opened.\n\n" +
                "Please drag Java to Applications and restart Phoenix Client."
            );
            
        } else if (IS_LINUX) {
            // Linux: Extract and show instructions
            showLinuxInstallInstructions(installerFile);
        }
    }
    
    private void showLinuxInstallInstructions(File tarFile) {
        SwingUtilities.invokeLater(() -> {
            splashDialog.setVisible(false);
            
            String message = "Java 8 has been downloaded to:\n" +
                           tarFile.getAbsolutePath() + "\n\n" +
                           "To install, run these commands in terminal:\n\n" +
                           "sudo mkdir -p /usr/lib/jvm\n" +
                           "sudo tar -xzf " + tarFile.getAbsolutePath() + " -C /usr/lib/jvm\n" +
                           "sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/jre1.8.0_181/bin/java 1\n\n" +
                           "Then restart Phoenix Client.";
            
            JTextArea textArea = new JTextArea(message);
            textArea.setEditable(false);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(600, 250));
            
            JOptionPane.showMessageDialog(null, scrollPane, 
                "Java 8 Installation Instructions", JOptionPane.INFORMATION_MESSAGE);
            
            System.exit(0);
        });
    }
    
    private void showInstallationMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            splashDialog.setVisible(false);
            
            String fullMessage = message + "\n\nThe application will now exit.";
            
            JOptionPane.showMessageDialog(null, fullMessage, "Java Installation", 
                                        JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        });
    }
    
    private void showError(String message) {
        splashDialog.setVisible(false);
        JOptionPane.showMessageDialog(null, message, "Startup Error", 
                                    JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}