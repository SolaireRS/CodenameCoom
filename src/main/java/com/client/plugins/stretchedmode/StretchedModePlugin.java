package com.client.plugins.stretchedmode;

import com.client.CustomGameFrame;
import com.client.features.gameframe.ScreenMode;
import com.client.Client;
import com.client.RSImageProducer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class StretchedModePlugin {
    private CustomGameFrame gameFrame;
    private boolean enabled = false;
    private boolean initialized = false;
    
    // Scaling settings
    private boolean increasedPerformance = true; // Changed default to true for better FPS
    private boolean ultraPretty = false;
    
    public StretchedModePlugin(CustomGameFrame gameFrame) {
        this.gameFrame = gameFrame;
        System.out.println("StretchedModePlugin created");
    }
    
    public boolean initialize() {
        if (initialized) {
            return true;
        }
        
        try {
            initialized = true;
            System.out.println("StretchedModePlugin initialized successfully");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to initialize StretchedModePlugin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public void enable() {
        if (!initialized) {
            throw new IllegalStateException("Plugin not initialized");
        }
        
        enabled = true;
        
        Client client = findClient();
        if (client != null) {
            Client.stretched = true;
            
            client.setStretchedEnabled(true);
            client.setStretchedKeepAspectRatio(true);
            client.setStretchedIntegerScaling(false);
            client.setStretchedFast(increasedPerformance);
            
            if (gameFrame != null) {
                gameFrame.setResizable(true);
                int sidebarWidth = gameFrame.isSidebarVisible() ? 350 : 25;
                gameFrame.setMinimumSize(new Dimension(765 + sidebarWidth, 503 + 25));
                
                // CRITICAL FIX: Don't call refreshMode - just update the frame state
                // The stretched mode should work WITH the current screen mode
                SwingUtilities.invokeLater(() -> {
                    gameFrame.refreshStretchedMode();
                });
            }
            
            // Force scale and buffer recreation
            SwingUtilities.invokeLater(() -> {
                RSImageProducer.invalidateScaleCache();
                client.updateGameScreen();
                client.invalidateStretching(true);
            });
            
            System.out.println("StretchedModePlugin enabled (Performance mode: " + increasedPerformance + ")");
        }
    }

    public void disable() {
        enabled = false;
        Client.stretched = false;
        
        Client client = findClient();
        if (client != null) {
            client.setStretchedEnabled(false);
            
            if (gameFrame != null) {
                // Don't change resizability - let the screen mode control that
                // Only update if we're in FIXED mode (not RESIZABLE mode)
                if (client.currentScreenMode == ScreenMode.FIXED) {
                    gameFrame.setResizable(false);
                    int sidebarWidth = gameFrame.isSidebarVisible() ? 350 : 25;
                    gameFrame.setSize(765 + sidebarWidth, 503 + 25);
                }
                
                SwingUtilities.invokeLater(() -> {
                    gameFrame.refreshStretchedMode();
                });
            }
            
            // Clean up cached resources
            SwingUtilities.invokeLater(() -> {
                RSImageProducer.invalidateScaleCache();
                client.updateGameScreen();
                client.invalidateStretching(true);
            });
        }
        
        System.out.println("StretchedModePlugin disabled");
    }
    
    public void notifyFrameOfChange() {
        if (gameFrame != null) {
            SwingUtilities.invokeLater(() -> {
                gameFrame.refreshStretchedMode();
            });
        }
    }
    
    public boolean isEnabled() {
        return enabled && initialized;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public boolean isUltraPretty() {
        return ultraPretty;
    }
    
    private Client findClient() {
        Component gameClient = gameFrame.getGameClient();
        return findClientInComponent(gameClient);
    }
    
    private Client findClientInComponent(Component component) {
        if (component instanceof Client) {
            return (Client) component;
        }
        
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                Client found = findClientInComponent(child);
                if (found != null) {
                    return found;
                }
            }
        }
        
        return null;
    }
    
    public JPanel getSettingsPanel() {
        System.out.println("DEBUG: Starting getSettingsPanel()");
        
        try {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            
            Color backgroundColor = getThemeBackgroundColor();
            Color textColor = getThemeTextColor();
            Color accentColor = getThemeAccentColor();
            Color labelColor = new Color(0x9f, 0x9f, 0x9f);
            
            panel.setBackground(backgroundColor);
            panel.setBorder(new EmptyBorder(15, 20, 15, 20));
            
            // Performance mode setting (DEFAULT ON)
            JCheckBox perfCheckBox = new JCheckBox();
            perfCheckBox.setSelected(increasedPerformance);
            perfCheckBox.setBackground(backgroundColor);
            perfCheckBox.setFocusPainted(false);
            perfCheckBox.setBorderPainted(false);
            perfCheckBox.setPreferredSize(new Dimension(18, 18));
            
            perfCheckBox.addActionListener(e -> {
                increasedPerformance = perfCheckBox.isSelected();
                if (enabled) {
                    Client client = findClient();
                    if (client != null) {
                        client.setStretchedFast(increasedPerformance);
                        RSImageProducer.invalidateScaleCache();
                        client.invalidateStretching(true);
                    }
                }
            });
            
            addSettingRow(panel, "Performance Mode (Pixelated)", perfCheckBox, backgroundColor, textColor, 
                         "Fastest rendering with nearest-neighbor scaling");
            
            // Add spacing
            panel.add(Box.createVerticalStrut(15));
            
            // Description
            JLabel descLabel = new JLabel("Stretches the game display while maintaining");
            descLabel.setForeground(labelColor);
            descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(descLabel);
            
            JLabel descLabel2 = new JLabel("the classic pixelated appearance.");
            descLabel2.setForeground(labelColor);
            descLabel2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            descLabel2.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(descLabel2);
            
            panel.add(Box.createVerticalStrut(10));
            
            // Instruction text
            JLabel instructionLabel = new JLabel("Drag the client window to stretch");
            instructionLabel.setForeground(labelColor);
            instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            instructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(instructionLabel);
            
            panel.add(Box.createVerticalStrut(5));
            
            JLabel changeLabel = new JLabel("Changes apply immediately when enabled.");
            changeLabel.setForeground(labelColor);
            changeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            changeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(changeLabel);
            
            panel.setPreferredSize(new Dimension(300, 220));
            
            System.out.println("DEBUG: getSettingsPanel() completed successfully");
            return panel;
            
        } catch (Exception e) {
            System.err.println("DEBUG: Exception in getSettingsPanel(): " + e.getMessage());
            e.printStackTrace();
            
            JPanel errorPanel = new JPanel();
            errorPanel.setBackground(getThemeBackgroundColor());
            errorPanel.setPreferredSize(new Dimension(300, 200));
            JLabel errorLabel = new JLabel("Error: " + e.getMessage());
            errorLabel.setForeground(getThemeTextColor());
            errorPanel.add(errorLabel);
            return errorPanel;
        }
    }

    private void addSettingRow(JPanel parent, String labelText, JCheckBox checkBox, 
                               Color bgColor, Color textColor, String description) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(bgColor);
        row.setBorder(new EmptyBorder(8, 0, 8, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Left side: label and description
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(bgColor);
        
        JLabel label = new JLabel(labelText);
        label.setForeground(textColor);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel descLabel = new JLabel(description);
        descLabel.setForeground(new Color(0x9f, 0x9f, 0x9f));
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        leftPanel.add(label);
        leftPanel.add(descLabel);
        
        row.add(leftPanel, BorderLayout.WEST);
        row.add(checkBox, BorderLayout.EAST);
        
        parent.add(row);
    }

    private Color getThemeBackgroundColor() {
        try {
            if (gameFrame != null && gameFrame.getThemeManager() != null) {
                return gameFrame.getThemeManager().getFrameBackground();
            }
        } catch (Exception e) {}
        return new Color(0x2b, 0x2b, 0x2b);
    }

    private Color getThemeTextColor() {
        try {
            if (gameFrame != null && gameFrame.getThemeManager() != null) {
                return gameFrame.getThemeManager().getTextColor();
            }
        } catch (Exception e) {}
        return Color.WHITE;
    }

    private Color getThemeAccentColor() {
        try {
            if (gameFrame != null && gameFrame.getThemeManager() != null) {
                return gameFrame.getThemeManager().getAccentColor();
            }
        } catch (Exception e) {}
        return new Color(0x00a2e8);
    }

    public void cleanup() {
        disable();
        initialized = false;
        System.out.println("StretchedModePlugin cleaned up");
    }
}