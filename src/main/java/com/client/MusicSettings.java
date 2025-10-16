package com.client;

import java.io.*;

import com.client.sign.Signlink;

public class MusicSettings {
    
    private static final String FILE_NAME = "music_settings.dat";
    
    // Music mute setting
    private boolean loginMusicMuted = false;
    
    // Singleton instance
    private static MusicSettings instance;
    
    private MusicSettings() {
        // Private constructor for singleton
    }
    
    public static MusicSettings getInstance() {
        if (instance == null) {
            instance = new MusicSettings();
        }
        return instance;
    }
    
    /**
     * Reads music settings from file
     */
    public void read() {
        File file = new File(Signlink.getCacheDirectory() + FILE_NAME);
        
        if (!file.exists()) {
            System.out.println("Music settings file not found, using defaults");
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("login_music_muted=")) {
                    loginMusicMuted = Boolean.parseBoolean(line.substring("login_music_muted=".length()));
                }
            }
            System.out.println("Music settings loaded successfully");
        } catch (IOException e) {
            System.err.println("Error reading music settings: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Writes music settings to file
     */
    public void write() {
        File file = new File(Signlink.getCacheDirectory() + FILE_NAME);
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("login_music_muted=" + loginMusicMuted);
            writer.newLine();
            
            System.out.println("Music settings saved successfully");
        } catch (IOException e) {
            System.err.println("Error writing music settings: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Getters and Setters
    
    public boolean isLoginMusicMuted() {
        return loginMusicMuted;
    }
    
    public void setLoginMusicMuted(boolean loginMusicMuted) {
        this.loginMusicMuted = loginMusicMuted;
    }
}