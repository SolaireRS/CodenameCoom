package com.client.cache;

import com.displee.cache.CacheLibrary;
import com.displee.cache.index.Index;
import com.displee.cache.index.archive.Archive;
import com.client.sign.Signlink;
import com.client.definitions.ObjectDefinition;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapDataLoader {
    
    private static final Logger LOGGER = Logger.getLogger(MapDataLoader.class.getName());
    
    private static final int REGION_SIZE = 64;
    private static final int MAX_TERRAIN_ID = 30000;
    private static final int MAP_INDEX = 4;
    private static final int UNDERLAY_OFFSET = 81;
    private static final int HEIGHT_MULTIPLIER = 8;
    private static final int DEFAULT_HEIGHT = -280;
    private static final int PLANE_COUNT = 4;
    private static final int GROUND_PLANE = 0;
    
    private static final float MIN_BRIGHTNESS = 0.7f;
    private static final float MAX_BRIGHTNESS = 1.3f;
    private static final float HEIGHT_BRIGHTNESS_FACTOR = 2000.0f;
    
    private static final Color LAVA_COLOR = new Color(0xFF4500);
    private static final Color WALL_COLOR = new Color(80, 80, 80);
    private static final Color TREE_COLOR = new Color(34, 139, 34);
    private static final Color BLACK = Color.BLACK;
    
    private static final Set<Integer> LAVA_OBJECTS = createLavaObjectSet();
    
    private static Set<Integer> createLavaObjectSet() {
        Set<Integer> set = new HashSet<>();
        set.add(11978); set.add(17334); set.add(17335); set.add(17336);
        set.add(17337); set.add(17338); set.add(17339); set.add(17340);
        set.add(17341); set.add(17342); set.add(17343); set.add(17344);
        set.add(17345); set.add(20232); set.add(20234); set.add(30997);
        set.add(31298); set.add(34515); set.add(44681); set.add(44614);
        set.add(44631); set.add(49248); set.add(55993); set.add(55995);
        set.add(55996); set.add(18519); set.add(18520); set.add(18521);
        set.add(18522); set.add(18523);
        return set;
    }
    
    private static final int OBJECT_TYPE_WALL = 0;
    private static final int OBJECT_TYPE_SCENERY = 10;
    
    private static class RegionInfo {
        int regionCoord;
        int terrainFileId;
        int objectFileId;
    }
    
    private static class MapBounds {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        
        void update(int x, int y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        
        int getWidth() {
            return (maxX - minX + 1) * REGION_SIZE;
        }
        
        int getHeight() {
            return (maxY - minY + 1) * REGION_SIZE;
        }
    }
    
    public static BufferedImage loadWorldMap() {
        return loadWorldMap(null, null);
    }
    
    public static BufferedImage loadWorldMap(String labelsPath, String iconsPath) {
        try {
            CacheLibrary cache = initializeCache();
            Index mapIndex = loadMapIndex(cache);
            
            if (mapIndex == null) {
                LOGGER.severe("Could not load map index");
                return null;
            }
            
            Map<Integer, RegionInfo> regionLookup = loadRegionMappings();
            Map<Integer, MapRegion> validRegions = loadValidRegions(mapIndex, regionLookup);
            
            if (validRegions.isEmpty()) {
                LOGGER.severe("No valid regions loaded");
                return null;
            }
            
            MapBounds bounds = calculateBounds(validRegions);
            BufferedImage mapImage = createMapImage(bounds, validRegions);
            
            // Add labels and icons if paths provided
            if (labelsPath != null || iconsPath != null) {
                mapImage = addLabelsAndIcons(mapImage, bounds, labelsPath, iconsPath);
            }
            
            return flipVertically(mapImage);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading world map", e);
            return null;
        }
    }
    
 // Add these methods to your MapDataLoader.java class
 // Place them after the flipVertically() method

 private static BufferedImage addLabelsAndIcons(BufferedImage mapImage, MapBounds bounds, 
                                                  String labelsPath, String iconsPath) {
     Graphics2D g2d = mapImage.createGraphics();
     
     // Enable anti-aliasing for text
     g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                         RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
     g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                         RenderingHints.VALUE_RENDER_QUALITY);
     
     // Draw icons first (underneath labels)
     if (iconsPath != null) {
         List<MapLabelDataLoader.MapIcon> icons = MapLabelDataLoader.loadIcons(iconsPath);
         for (MapLabelDataLoader.MapIcon icon : icons) {
             drawIcon(g2d, icon, bounds);
         }
     }
     
     // Draw labels on top
     if (labelsPath != null) {
         List<MapLabelDataLoader.MapLabel> labels = MapLabelDataLoader.loadLabels(labelsPath);
         for (MapLabelDataLoader.MapLabel label : labels) {
             drawLabel(g2d, label, bounds);
         }
     }
     
     g2d.dispose();
     return mapImage;
 }

 private static void drawLabel(Graphics2D g2d, MapLabelDataLoader.MapLabel label, MapBounds bounds) {
     // Convert game coordinates to pixel coordinates
     int pixelX = (label.getX() - bounds.minX * REGION_SIZE);
     int pixelY = (label.getY() - bounds.minY * REGION_SIZE);
     
     // Set font
     int style = label.isBold() ? Font.BOLD : Font.PLAIN;
     Font font = new Font("Arial", style, label.getSize());
     g2d.setFont(font);
     
     // Handle multi-line text
     String[] lines = label.getText().split("\\\\n");
     FontMetrics fm = g2d.getFontMetrics();
     
     for (int i = 0; i < lines.length; i++) {
         String line = lines[i];
         int textWidth = fm.stringWidth(line);
         
         int x = pixelX;
         if ("center".equals(label.getAlign())) {
             x = pixelX - textWidth / 2;
         }
         
         int y = pixelY + (i * fm.getHeight());
         
         // Draw text shadow for better visibility
         g2d.setColor(Color.BLACK);
         g2d.drawString(line, x + 1, y + 1);
         
         // Draw main text
         g2d.setColor(label.getColor());
         g2d.drawString(line, x, y);
     }
 }

 private static void drawIcon(Graphics2D g2d, MapLabelDataLoader.MapIcon icon, MapBounds bounds) {
     // Convert game coordinates to pixel coordinates
     int pixelX = (icon.getX() - bounds.minX * REGION_SIZE);
     int pixelY = (icon.getY() - bounds.minY * REGION_SIZE);
     
     Color iconColor = icon.getIconColor();
     
     // Draw icon marker (simple dot with border)
     int iconSize = 6;
     
     // Draw border
     g2d.setColor(Color.BLACK);
     g2d.fillOval(pixelX - iconSize/2 - 1, pixelY - iconSize/2 - 1, iconSize + 2, iconSize + 2);
     
     // Draw icon
     g2d.setColor(iconColor);
     g2d.fillOval(pixelX - iconSize/2, pixelY - iconSize/2, iconSize, iconSize);
 }

	private static CacheLibrary initializeCache() {
        String cachePath = Signlink.getCacheDirectory();
        LOGGER.info("Loading cache from: " + cachePath);
        return CacheLibrary.create(cachePath);
    }
    
    private static Index loadMapIndex(CacheLibrary cache) {
        return cache.index(MAP_INDEX);
    }
    
    private static Map<Integer, RegionInfo> loadRegionMappings() {
        Map<Integer, RegionInfo> lookup = new HashMap<>();
        
        try {
            int[] coords = com.client.OnDemandFetcher.getMapIndices1();
            int[] terrainIds = com.client.OnDemandFetcher.getMapIndices2();
            int[] objectIds = com.client.OnDemandFetcher.getMapIndices3();
            
            if (coords == null) {
                LOGGER.warning("OnDemandFetcher indices not loaded");
                return lookup;
            }
            
            for (int i = 0; i < coords.length; i++) {
                if (terrainIds[i] > 0 && terrainIds[i] < MAX_TERRAIN_ID) {
                    RegionInfo info = new RegionInfo();
                    info.regionCoord = coords[i];
                    info.terrainFileId = terrainIds[i];
                    info.objectFileId = objectIds[i];
                    lookup.put(coords[i], info);
                }
            }
            
            LOGGER.info("Loaded " + lookup.size() + " region mappings");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading region mappings", e);
        }
        
        return lookup;
    }
    
    private static Map<Integer, MapRegion> loadValidRegions(Index mapIndex, Map<Integer, RegionInfo> regionLookup) {
        Map<Integer, MapRegion> validRegions = new HashMap<>();
        int loadedCount = 0;
        
        for (RegionInfo info : regionLookup.values()) {
            try {
                byte[] terrainData = loadArchiveData(mapIndex, info.terrainFileId);
                byte[] objectData = loadArchiveData(mapIndex, info.objectFileId);
                
                if (terrainData != null && terrainData.length > 0) {
                    MapRegion region = new MapRegion(info.regionCoord, terrainData, objectData);
                    validRegions.put(info.regionCoord, region);
                    loadedCount++;
                    
                    if (loadedCount % 100 == 0) {
                        LOGGER.info("Loaded " + loadedCount + " regions...");
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load region " + info.regionCoord, e);
            }
        }
        
        LOGGER.info("Successfully loaded " + loadedCount + " regions");
        return validRegions;
    }
    
    private static byte[] loadArchiveData(Index index, int fileId) {
        try {
            Archive archive = index.archive(fileId);
            if (archive != null) {
                return archive.file(0).getData();
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not load archive " + fileId, e);
        }
        return null;
    }
    
    private static MapBounds calculateBounds(Map<Integer, MapRegion> regions) {
        MapBounds bounds = new MapBounds();
        
        for (int regionCoord : regions.keySet()) {
            int x = (regionCoord >> 8) & 0xFF;
            int y = regionCoord & 0xFF;
            bounds.update(x, y);
        }
        
        LOGGER.info(String.format("Region bounds: X=%d-%d, Y=%d-%d", 
            bounds.minX, bounds.maxX, bounds.minY, bounds.maxY));
        
        return bounds;
    }
    
    private static BufferedImage createMapImage(MapBounds bounds, Map<Integer, MapRegion> regions) {
        int width = bounds.getWidth();
        int height = bounds.getHeight();
        
        LOGGER.info("Creating map: " + width + "x" + height + " pixels");
        
        BufferedImage mapImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = mapImage.createGraphics();
        g2d.setColor(BLACK);
        g2d.fillRect(0, 0, width, height);
        
        drawTerrain(g2d, regions, bounds);
        drawObjects(g2d, regions, bounds);
        
        g2d.dispose();
        
        return mapImage;
    }
    
    private static void drawTerrain(Graphics2D g2d, Map<Integer, MapRegion> regions, MapBounds bounds) {
        for (Map.Entry<Integer, MapRegion> entry : regions.entrySet()) {
            int regionCoord = entry.getKey();
            MapRegion region = entry.getValue();
            
            int x = (regionCoord >> 8) & 0xFF;
            int y = regionCoord & 0xFF;
            
            int offsetX = (x - bounds.minX) * REGION_SIZE;
            int offsetY = (y - bounds.minY) * REGION_SIZE;
            
            drawRegion(g2d, region, offsetX, offsetY);
        }
    }
    
    private static void drawObjects(Graphics2D g2d, Map<Integer, MapRegion> regions, MapBounds bounds) {
        for (Map.Entry<Integer, MapRegion> entry : regions.entrySet()) {
            int regionCoord = entry.getKey();
            MapRegion region = entry.getValue();
            
            int x = (regionCoord >> 8) & 0xFF;
            int y = regionCoord & 0xFF;
            
            int offsetX = (x - bounds.minX) * REGION_SIZE;
            int offsetY = (y - bounds.minY) * REGION_SIZE;
            
            drawRegionObjects(g2d, region, offsetX, offsetY);
        }
    }
    
    private static void drawRegion(Graphics2D g2d, MapRegion region, int offsetX, int offsetY) {
        for (int x = 0; x < REGION_SIZE; x++) {
            for (int y = 0; y < REGION_SIZE; y++) {
                Color color = region.getTileColor(x, y);
                g2d.setColor(color);
                g2d.fillRect(offsetX + x, offsetY + y, 1, 1);
            }
        }
    }
    
    private static void drawRegionObjects(Graphics2D g2d, MapRegion region, int offsetX, int offsetY) {
        for (MapObject obj : region.objects) {
            Color objColor = getObjectColor(obj.id, obj.type);
            if (objColor != null) {
                g2d.setColor(objColor);
                g2d.fillRect(offsetX + obj.x, offsetY + obj.y, 1, 1);
            }
        }
    }
    
    private static Color getObjectColor(int objectId, int type) {
        if (LAVA_OBJECTS.contains(objectId)) {
            return LAVA_COLOR;
        }
        
        if (type == OBJECT_TYPE_WALL) {
            return WALL_COLOR;
        }
        
        if (type == OBJECT_TYPE_SCENERY) {
            return getSceneryColor(objectId);
        }
        
        return null;
    }
    
    private static Color getSceneryColor(int objectId) {
        try {
            ObjectDefinition def = ObjectDefinition.forID(objectId);
            if (def != null && def.name != null) {
                String name = def.name.toLowerCase();
                if (name.contains("tree")) {
                    return TREE_COLOR;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not load object definition " + objectId, e);
        }
        return null;
    }
    
    private static BufferedImage flipVertically(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        BufferedImage flipped = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = flipped.createGraphics();
        g2d.drawImage(image, 0, height, width, 0, 0, 0, width, height, null);
        g2d.dispose();
        
        LOGGER.info("Map generation complete");
        return flipped;
    }
    
    private static class MapObject {
        int id;
        int x, y;
        int type;
        int orientation;
        
        MapObject(int id, int x, int y, int type, int orientation) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.type = type;
            this.orientation = orientation;
        }
    }
    
    private static class MapRegion {
        private final short[][] underlays;
        private final short[][] overlays;
        private final byte[][] overlayTypes;
        private final int[][] tileHeights;
        private final List<MapObject> objects;
        private final boolean valid;
        
        public MapRegion(int fileId, byte[] terrainData, byte[] objectData) {
            this.underlays = new short[REGION_SIZE][REGION_SIZE];
            this.overlays = new short[REGION_SIZE][REGION_SIZE];
            this.overlayTypes = new byte[REGION_SIZE][REGION_SIZE];
            this.tileHeights = new int[REGION_SIZE][REGION_SIZE];
            this.objects = new ArrayList<>();
            this.valid = parseTerrainData(terrainData);
            
            if (objectData != null) {
                parseObjectData(objectData);
            }
        }
        
        private boolean parseTerrainData(byte[] data) {
            int offset = 0;
            
            for (int plane = 0; plane < PLANE_COUNT; plane++) {
                for (int x = 0; x < REGION_SIZE; x++) {
                    for (int y = 0; y < REGION_SIZE; y++) {
                        try {
                            while (true) {
                                if (offset + 1 >= data.length) {
                                    return plane > 0;
                                }
                                
                                int opcode = readUnsignedShort(data, offset);
                                offset += 2;
                                
                                if (opcode == 0) {
                                    if (plane == GROUND_PLANE) {
                                        tileHeights[x][y] = DEFAULT_HEIGHT;
                                    }
                                    break;
                                }
                                
                                if (opcode == 1) {
                                    if (offset >= data.length) return false;
                                    
                                    int height = data[offset++] & 0xFF;
                                    if (height == 1) height = 0;
                                    
                                    if (plane == GROUND_PLANE) {
                                        tileHeights[x][y] = -height * HEIGHT_MULTIPLIER;
                                    }
                                    break;
                                }
                                
                                if (opcode <= 49) {
                                    if (offset + 1 >= data.length) return false;
                                    
                                    short overlayId = (short) readUnsignedShort(data, offset);
                                    offset += 2;
                                    
                                    if (plane == GROUND_PLANE) {
                                        overlays[x][y] = overlayId;
                                        overlayTypes[x][y] = (byte)((opcode - 2) / 4);
                                    }
                                } else if (opcode <= 81) {
                                    // Tile flags - ignored
                                } else {
                                    if (plane == GROUND_PLANE) {
                                        underlays[x][y] = (short)(opcode - UNDERLAY_OFFSET);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.FINE, "Error parsing terrain at x=" + x + ", y=" + y, e);
                        }
                    }
                }
            }
            
            return true;
        }
        
        private void parseObjectData(byte[] data) {
            int offset = 0;
            int objectId = -1;
            
            try {
                while (offset < data.length) {
                    int idOffset = readSmart(data, offset);
                    if (idOffset == 0) break;
                    
                    offset += getSmartSize(data, offset);
                    objectId += idOffset;
                    
                    int positionHash = 0;
                    int positionOffset = readSmart(data, offset);
                    offset += getSmartSize(data, offset);
                    
                    while (positionOffset != 0) {
                        positionHash += positionOffset - 1;
                        
                        int localY = positionHash & 0x3F;
                        int localX = (positionHash >> 6) & 0x3F;
                        int plane = (positionHash >> 12) & 0x3;
                        int attributes = data[offset++] & 0xFF;
                        int type = attributes >> 2;
                        int orientation = attributes & 0x3;
                        
                        if (plane == GROUND_PLANE && isValidCoordinate(localX, localY)) {
                            objects.add(new MapObject(objectId, localX, localY, type, orientation));
                        }
                        
                        positionOffset = readSmart(data, offset);
                        offset += getSmartSize(data, offset);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Error parsing object data", e);
            }
        }
        
        private boolean isValidCoordinate(int x, int y) {
            return x >= 0 && x < REGION_SIZE && y >= 0 && y < REGION_SIZE;
        }
        
        private int readUnsignedShort(byte[] data, int offset) {
            return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
        }
        
        private int readSmart(byte[] data, int offset) {
            if (offset >= data.length) return 0;
            
            int value = data[offset] & 0xFF;
            if (value < 128) {
                return value;
            } else {
                if (offset + 1 >= data.length) return 0;
                return ((value - 128) << 8) | (data[offset + 1] & 0xFF);
            }
        }
        
        private int getSmartSize(byte[] data, int offset) {
            if (offset >= data.length) return 1;
            return (data[offset] & 0xFF) < 128 ? 1 : 2;
        }
        
        public Color getTileColor(int x, int y) {
            if (!isValidCoordinate(x, y)) {
                return BLACK;
            }
            
            short overlayId = overlays[x][y];
            short underlayId = underlays[x][y];
            int height = tileHeights[x][y];
            
            try {
                int rgb = getUnderlayColor(underlayId);
                
                if (rgb != 0) {
                    rgb = applyOverlay(rgb, overlayId);
                    return applyHeightShading(rgb, height);
                }
                
                rgb = getOverlayColor(overlayId);
                if (rgb != 0) {
                    return applyHeightShading(rgb, height);
                }
                
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Error getting tile color at x=" + x + ", y=" + y, e);
            }
            
            return BLACK;
        }
        
        private int getUnderlayColor(short underlayId) {
            if (underlayId <= 0 || underlayId > com.client.definitions.FloorUnderlayDefinition.underlays.length) {
                return 0;
            }
            
            com.client.definitions.FloorUnderlayDefinition underlay = 
                com.client.definitions.FloorUnderlayDefinition.underlays[underlayId - 1];
            
            if (underlay == null) return 0;
            
            if (underlay.anInt390 != 0) {
                return underlay.anInt390;
            }
            
            if (underlay.hsl16 > 0 && underlay.hsl16 < com.client.Rasterizer.hslToRgb.length) {
                return com.client.Rasterizer.hslToRgb[underlay.hsl16];
            }
            
            return 0;
        }
        
        private int applyOverlay(int baseRgb, short overlayId) {
            if (overlayId <= 0 || overlayId > com.client.definitions.FloorOverlayDefinition.overlays.length) {
                return baseRgb;
            }
            
            com.client.definitions.FloorOverlayDefinition overlay = 
                com.client.definitions.FloorOverlayDefinition.overlays[overlayId - 1];
            
            if (overlay == null) return baseRgb;
            
            if (overlay.rgb != 0 && overlay.rgb != 0xff00ff) {
                return overlay.rgb;
            }
            
            if (overlay.hsl16 > 0 && overlay.hsl16 < com.client.Rasterizer.hslToRgb.length) {
                return com.client.Rasterizer.hslToRgb[overlay.hsl16];
            }
            
            return baseRgb;
        }
        
        private int getOverlayColor(short overlayId) {
            if (overlayId <= 0 || overlayId > com.client.definitions.FloorOverlayDefinition.overlays.length) {
                return 0;
            }
            
            com.client.definitions.FloorOverlayDefinition overlay = 
                com.client.definitions.FloorOverlayDefinition.overlays[overlayId - 1];
            
            if (overlay == null) return 0;
            
            if (overlay.rgb != 0 && overlay.rgb != 0xff00ff) {
                return overlay.rgb;
            }
            
            if (overlay.hsl16 > 0 && overlay.hsl16 < com.client.Rasterizer.hslToRgb.length) {
                return com.client.Rasterizer.hslToRgb[overlay.hsl16];
            }
            
            return 0;
        }
        
        private Color applyHeightShading(int rgb, int height) {
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            
            float brightness = 1.0f + (height / HEIGHT_BRIGHTNESS_FACTOR);
            brightness = Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, brightness));
            
            r = Math.min(255, (int)(r * brightness));
            g = Math.min(255, (int)(g * brightness));
            b = Math.min(255, (int)(b * brightness));
            
            return new Color(r, g, b);
        }
        
        public boolean isValid() {
            return valid;
        }
    }
}