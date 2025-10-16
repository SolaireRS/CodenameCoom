package com.client.features.settings;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;

import com.client.Client;
import com.client.Configuration;
import com.client.Rasterizer;
import com.client.features.gameframe.ScreenMode;
import com.client.graphics.interfaces.RSInterface;
import com.client.graphics.interfaces.builder.impl.NotificationTab;
import com.client.graphics.interfaces.impl.SettingsTabWidget;
import com.client.sign.Signlink;
import com.client.sound.SoundPlayer;
import com.client.utilities.settings.SettingsManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.extern.java.Log;

public class Preferences {

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(Preferences.class.getName());

    private static Preferences preferences = new Preferences();

    public static Preferences getPreferences() {
        return preferences;
    }

    public static void load() {
        try {
            File preferencesFile = new File(getFileLocation());
            //System.out.println("Loading preferences from: " + getFileLocation());
            //System.out.println("File exists: " + preferencesFile.exists());
            
            if (preferencesFile.exists()) {
                ObjectNode node = new ObjectMapper().readValue(preferencesFile, ObjectNode.class);

                if (node.has("soundVolume")) {
                    preferences.soundVolume = node.get("soundVolume").doubleValue();
                    //System.out.println("Loaded sound volume: " + preferences.soundVolume);
                }
                if (node.has("areaSoundVolume")) {
                    preferences.areaSoundVolume = node.get("areaSoundVolume").doubleValue();
                    //System.out.println("Loaded area sound volume: " + preferences.areaSoundVolume);
                }
                if (node.has("musicVolume")) {
                    preferences.musicVolume = node.get("musicVolume").doubleValue();
                    //System.out.println("Loaded music volume: " + preferences.musicVolume);
                }
                if (node.has("brightness"))
                    preferences.brightness = node.get("brightness").doubleValue();
                if (node.has("screenWidth"))
                    preferences.screenWidth = node.get("screenWidth").intValue();
                if (node.has("screenHeight"))
                    preferences.screenHeight = node.get("screenHeight").intValue();
                if (node.has("dragTime"))
                    preferences.dragTime = node.get("dragTime").intValue();
                if (node.has("hidePetOptions"))
                    preferences.hidePetOptions = node.get("hidePetOptions").booleanValue();
                if (node.has("pmNotifications"))
                    preferences.pmNotifications = node.get("pmNotifications").booleanValue();
                if (node.has("mode"))
                    preferences.mode = ScreenMode.valueOf(node.get("mode").textValue());
                if (node.has("groundItemTextShowMoreThan"))
                    preferences.groundItemTextShowMoreThan = node.get("groundItemTextShowMoreThan").textValue();
                if (node.has("groundItemTextShow"))
                    preferences.groundItemTextShow = node.get("groundItemTextShow").textValue();
                if (node.has("groundItemTextHide"))
                    preferences.groundItemTextHide = node.get("groundItemTextHide").textValue();
                if (node.has("groundItemAlwaysShowUntradables"))
                    preferences.groundItemAlwaysShowUntradables = node.get("groundItemAlwaysShowUntradables").booleanValue();

            } else {
                //System.out.println("Preferences file doesn't exist, creating new one");
                save();
            }
        } catch (Exception e) {
            log.severe("Error while loading preferences.");
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            //System.out.println("Saving preferences...");
            //System.out.println("Music volume: " + preferences.musicVolume);
            //System.out.println("Sound volume: " + preferences.soundVolume);
            //System.out.println("Area sound volume: " + preferences.areaSoundVolume);
            //System.out.println("File location: " + getFileLocation());
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(getFileLocation()), preferences);
            
            //System.out.println("Preferences saved successfully!");
        } catch (IOException e) {
            log.severe("Error while saving preferences.");
            e.printStackTrace();
        }
    }
    
    public static String getFileLocation() {
        return System.getProperty("user.home") + File.separator + Configuration.CLIENT_TITLE.toLowerCase() + "_properties.json";
    }

    public double musicVolume = 5.0; // Default to middle volume
    public double soundVolume = 5.0;
    public double areaSoundVolume = 5.0;
    public double brightness = 0.75;
    public ScreenMode mode = ScreenMode.FIXED;
    public int screenWidth;
    public int screenHeight;
    public int dragTime = 5;
    public boolean hidePetOptions;
    public boolean pmNotifications;
    public String groundItemTextShowMoreThan = "";
    public String groundItemTextShow = "";
    public String groundItemTextHide = "";
    public boolean groundItemAlwaysShowUntradables;

    public Preferences() { }

    public void updateClientConfiguration() {
        //System.out.println("updateClientConfiguration called");
        //System.out.println("Current music volume in preferences: " + musicVolume);
        
        // Brightness
        Rasterizer.setBrightness(brightness);
        SettingsTabWidget.brightnessSlider.setValue(brightness);
        
        // Sliders display inverted, so invert when loading
        SettingsTabWidget.musicVolumeSlider.setValue(10 - musicVolume);
        SettingsTabWidget.soundVolumeSlider.setValue(10 - soundVolume);
        SettingsTabWidget.areaSoundVolumeSlider.setValue(10 - areaSoundVolume);
        
        // Set the Signlink.midivol so it's ready when music plays
        int midiVolume = (int) ((musicVolume / 10.0) * 127);
        Signlink.midivol = midiVolume;
        //System.out.println("Set Signlink.midivol to: " + midiVolume);
        
        // Set sound effect volume on startup (inverted for SoundPlayer)
        SoundPlayer.setVolume(10 - (int) soundVolume);
        //System.out.println("Set SoundPlayer.volume to: " + (10 - (int) soundVolume));
        
        RSInterface.interfaceCache[SettingsTabWidget.HIDE_LOCAL_PET_OPTIONS].active = hidePetOptions;

        NotificationTab.instance.scrollable.update(">value", groundItemTextShowMoreThan);
        NotificationTab.instance.scrollable.update("show", groundItemTextShow);
        NotificationTab.instance.scrollable.update("hide", groundItemTextHide);
        NotificationTab.instance.scrollable.updateButtonText(NotificationTab.ALWAYS_SHOW_UNTRADABLES_BUTTON_ID);
    }

    public void updateScreenMode() {
        Client.instance.setGameMode(mode, new Dimension(screenWidth, screenHeight), false);
    }

}