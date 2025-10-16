package com.client.cache;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.awt.Color;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads map labels and POI icons from JSON configuration files
 */
public class MapLabelDataLoader {
    
    private static final Logger LOGGER = Logger.getLogger(MapLabelDataLoader.class.getName());
    private static final Gson GSON = new Gson();
    
    /**
     * Represents a text label on the map
     */
    public static class MapLabel {
        private String text;
        private int x;
        private int y;
        private int size;
        private String align;
        private boolean bold;
        private String colour;
        
        public String getText() { 
            return text; 
        }
        
        public int getX() { 
            return x; 
        }
        
        public int getY() { 
            return y; 
        }
        
        public int getSize() { 
            return size; 
        }
        
        public String getAlign() { 
            return align; 
        }
        
        public boolean isBold() { 
            return bold; 
        }
        
        /**
         * Parses the colour string and returns a Color object
         * Supports hex (#RRGGBB) and rgb(r,g,b) formats
         */
        public Color getColor() {
            if (colour == null) {
                return Color.WHITE;
            }
            
            try {
                // Hex color format: #RRGGBB
                if (colour.startsWith("#")) {
                    return Color.decode(colour);
                } 
                // RGB format: rgb(255, 255, 255)
                else if (colour.startsWith("rgb(")) {
                    String[] parts = colour.substring(4, colour.length() - 1).split(",");
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    return new Color(r, g, b);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to parse color: " + colour, e);
            }
            
            return Color.WHITE;
        }
    }
    
    /**
     * Represents a POI (Point of Interest) icon on the map
     */
    public static class MapIcon {
        private String type;
        private String name;
        private int x;
        private int y;
        
        public String getType() { 
            return type; 
        }
        
        public String getName() { 
            return name; 
        }
        
        public int getX() { 
            return x; 
        }
        
        public int getY() { 
            return y; 
        }
        
        /**
         * Returns the color for this icon based on its type
         */
        public Color getIconColor() {
            if (type == null) {
                return Color.WHITE;
            }
            
            switch (type.toLowerCase()) {
                case "bank":
                    return new Color(255, 215, 0); // Gold
                case "shop":
                    return new Color(135, 206, 250); // Light blue
                case "altar":
                    return new Color(138, 43, 226); // Purple
                case "furnace":
                    return new Color(255, 69, 0); // Red-orange
                case "anvil":
                    return new Color(169, 169, 169); // Gray
                case "mine":
                    return new Color(139, 69, 19); // Brown
                case "teleport":
                    return new Color(0, 255, 255); // Cyan
                case "dungeon":
                    return new Color(128, 0, 128); // Purple
                case "quest":
                    return new Color(255, 255, 0); // Yellow
                case "agility":
                    return new Color(34, 139, 34); // Green
                case "fishing":
                    return new Color(30, 144, 255); // Dodger blue
                case "cooking":
                    return new Color(255, 140, 0); // Dark orange
                case "crafting":
                    return new Color(210, 180, 140); // Tan
                case "slayer":
                    return new Color(220, 20, 60); // Crimson
                case "farming":
                    return new Color(124, 252, 0); // Lawn green
                default:
                    return Color.WHITE;
            }
        }
    }
    
    /**
     * Loads map labels from a JSON file
     * 
     * @param filePath Path to the labels JSON file
     * @return List of MapLabel objects, empty list if file not found or error
     */
    public static List<MapLabel> loadLabels(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Type listType = new TypeToken<ArrayList<MapLabel>>(){}.getType();
            List<MapLabel> labels = GSON.fromJson(reader, listType);
            
            if (labels == null) {
                labels = new ArrayList<>();
            }
            
            LOGGER.info("Loaded " + labels.size() + " map labels from " + filePath);
            return labels;
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load labels from " + filePath + 
                      " (file may not exist, will generate map without labels)", e);
            return new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing labels JSON from " + filePath, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Loads map icons from a JSON file
     * 
     * @param filePath Path to the icons JSON file
     * @return List of MapIcon objects, empty list if file not found or error
     */
    public static List<MapIcon> loadIcons(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Type listType = new TypeToken<ArrayList<MapIcon>>(){}.getType();
            List<MapIcon> icons = GSON.fromJson(reader, listType);
            
            if (icons == null) {
                icons = new ArrayList<>();
            }
            
            LOGGER.info("Loaded " + icons.size() + " map icons from " + filePath);
            return icons;
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load icons from " + filePath + 
                      " (file may not exist, will generate map without icons)", e);
            return new ArrayList<>();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error parsing icons JSON from " + filePath, e);
            return new ArrayList<>();
        }
    }
}