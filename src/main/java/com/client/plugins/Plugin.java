package com.client.plugins;

import javax.swing.JPanel;

/**
 * Base interface for all client plugins
 */
public abstract class Plugin {
    
    /**
     * Initialize the plugin
     * @return true if initialization was successful, false otherwise
     */
    public abstract boolean initialize();
    
    /**
     * Enable the plugin
     */
    public abstract void enable();
    
    /**
     * Disable the plugin
     */
    public abstract void disable();
    
    /**
     * Check if the plugin is currently enabled
     * @return true if enabled, false otherwise
     */
    public abstract boolean isEnabled();
    
    /**
     * Check if the plugin has been initialized
     * @return true if initialized, false otherwise
     */
    public abstract boolean isInitialized();
    
    /**
     * Get the plugin's display name
     * @return the plugin name
     */
    public abstract String getName();
    
    /**
     * Get the plugin's settings panel
     * @return JPanel containing the plugin's settings UI, or null if no settings
     */
    public abstract JPanel getSettingsPanel();
    
    /**
     * Cleanup plugin resources when shutting down
     */
    public abstract void cleanup();
    
    /**
     * Get the plugin's version (optional)
     * @return version string
     */
    public String getVersion() {
        return "1.0.0";
    }
    
    /**
     * Get the plugin's description (optional)
     * @return description string
     */
    public String getDescription() {
        return "No description available";
    }
    
    /**
     * Check if the plugin has settings
     * @return true if the plugin has configurable settings
     */
    public boolean hasSettings() {
        return getSettingsPanel() != null;
    }
}