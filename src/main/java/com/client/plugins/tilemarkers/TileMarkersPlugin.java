package com.client.plugins.tilemarkers;

import com.client.Client;
import com.client.DrawingArea;
import com.client.Model;
import com.client.Player;
import com.client.Rasterizer;
import com.client.WorldController;
import com.client.plugins.Plugin;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tile Markers Plugin - Mimics RuneLite's tile marker functionality
 * Allows players to mark tiles with colored overlays for various purposes
 */
public class TileMarkersPlugin extends Plugin {
    private static final String PLUGIN_NAME = "Tile Markers";
    
    // Client references - cleaned up duplicates
    private Client clientInstance;
    private Player currentPlayer;
    private int currentPlane;
    
    // Color constants for different marker types
    public static final Color DEFAULT_MARKER_COLOR = new Color(255, 0, 0, 100); // Red with transparency
    public static final Color[] PRESET_COLORS = {
        new Color(255, 0, 0, 100),   // Red
        new Color(0, 255, 0, 100),   // Green  
        new Color(0, 0, 255, 100),   // Blue
        new Color(255, 255, 0, 100), // Yellow
        new Color(255, 0, 255, 100), // Magenta
        new Color(0, 255, 255, 100), // Cyan
        new Color(255, 165, 0, 100), // Orange
        new Color(128, 0, 128, 100), // Purple
        new Color(255, 255, 255, 100), // White
        new Color(0, 0, 0, 100)      // Black
    };
    
    // UI Colors matching your theme
    private static final Color BACKGROUND_COLOR = new Color(0x1e, 0x1e, 0x1e);
    private static final Color PANEL_COLOR = new Color(0x2b, 0x2b, 0x2b);
    private static final Color TEXT_COLOR = new Color(0x00, 0xa2, 0xe8);
    private static final Color MUTED_TEXT = new Color(0x9f, 0x9f, 0x9f);
    
    // Plugin state
    private boolean isEnabled = false;
    private boolean isInitialized = false;
    private Map<TileCoordinate, TileMarker> tileMarkers;
    private JPanel settingsPanel;
    
    // Current marking state
    private boolean markingMode = false;
    private Color currentMarkerColor = DEFAULT_MARKER_COLOR;
    private String currentMarkerLabel = "";
    
    // Keyboard shortcuts
    private boolean shiftHeld = false;
    private boolean ctrlHeld = false;
    
    // Client access - you'll need to adapt these to your client
    private Component gameCanvas;
    private int viewportWidth = 765;
    private int viewportHeight = 503;
    
    public TileMarkersPlugin() {
        this.tileMarkers = new ConcurrentHashMap<>();
        //System.out.println("TileMarkersPlugin created");
    }
    
    @Override
    public boolean initialize() {
        try {
            //System.out.println("Initializing Tile Markers Plugin...");
            
            // Try to get game canvas - adapt this to your client structure
            try {
                this.gameCanvas = null; // Set appropriately for your client
            } catch (Exception e) {
                System.err.println("Could not get game canvas, some features may not work: " + e.getMessage());
            }
            
            // Create settings panel - ensure this always succeeds
            try {
                this.settingsPanel = new TileMarkersSettingsPanel();
                if (settingsPanel instanceof TileMarkersSettingsPanel) {
                    ((TileMarkersSettingsPanel) settingsPanel).setPlugin(this);
                }
            } catch (Exception e) {
                System.err.println("Failed to create settings panel: " + e.getMessage());
                e.printStackTrace();
                // Create a fallback panel
                this.settingsPanel = createFallbackSettingsPanel();
            }
            
            // Load saved tile markers
            loadTileMarkers();
            
            this.isInitialized = true;
            //System.out.println("Tile Markers Plugin initialized successfully");
            return true;
            
        } catch (Exception e) {
            System.err.println("Failed to initialize Tile Markers Plugin: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void enable() {
        if (!isInitialized) {
            if (!initialize()) {
                throw new RuntimeException("Failed to initialize Tile Markers Plugin");
            }
        }
        
        this.isEnabled = true;
        attachEventListeners();
        //System.out.println("Tile Markers Plugin enabled");
    }
    
    @Override
    public void disable() {
        this.isEnabled = false;
        detachEventListeners();
        this.markingMode = false;
        //System.out.println("Tile Markers Plugin disabled");
    }
    
    @Override
    public boolean isEnabled() {
        return isEnabled;
    }
    
    @Override
    public boolean isInitialized() {
        return isInitialized;
    }
    
    @Override
    public String getName() {
        return PLUGIN_NAME;
    }
    
    // Client data setters
    public void setClientInstance(Client client) {
        this.clientInstance = client;
    }
    
    public void updatePlayerData(Player player, int plane) {
        this.currentPlayer = player;
        this.currentPlane = plane;
    }
    
    @Override
    public JPanel getSettingsPanel() {
        if (settingsPanel == null) {
            try {
                TileMarkersSettingsPanel panel = new TileMarkersSettingsPanel();
                panel.setPlugin(this);
                settingsPanel = panel;
            } catch (Exception e) {
                System.err.println("Failed to create TileMarkersSettingsPanel, using fallback: " + e.getMessage());
                settingsPanel = createFallbackSettingsPanel();
            }
        }
        return settingsPanel;
    }
    
    @Override
    public void cleanup() {
        disable();
        saveTileMarkers();
        if (tileMarkers != null) {
            tileMarkers.clear();
        }
        //System.out.println("Tile Markers Plugin cleaned up");
    }
    
    /**
     * Called by the game client to render tile markers
     * You'll need to call this from your main render loop
     */
    public void render(Graphics2D g2d) {
        if (!isEnabled || tileMarkers.isEmpty()) {
            //System.out.println("DEBUG: render - plugin disabled or no markers (enabled=" + isEnabled + ", markers=" + tileMarkers.size() + ")");
            return;
        }
        
        //System.out.println("DEBUG: render - starting with " + tileMarkers.size() + " markers");
        
        try {
            // Get current player position and plane info
            int playerX = getPlayerX();
            int playerY = getPlayerY();
            int plane = getCurrentPlane();
            
            //System.out.println("DEBUG: render - player at (" + playerX + "," + playerY + ") plane " + plane);
            
            // Render each tile marker
            for (TileMarker marker : tileMarkers.values()) {
                //System.out.println("DEBUG: render - checking marker at (" + marker.getWorldX() + "," + marker.getWorldY() + ") plane " + marker.getPlane());
                
                if (marker.getPlane() != plane) {
                    //System.out.println("DEBUG: render - skipping marker on different plane");
                    continue; // Only render markers on current plane
                }
                
                // Convert world coordinates to screen coordinates
                Point screenPos = worldToScreen(marker.getWorldX(), marker.getWorldY());
                if (screenPos != null && isOnScreen(screenPos)) {
                    //System.out.println("DEBUG: render - rendering marker at screen " + screenPos);
                    renderTileMarker(g2d, screenPos, marker);
                } else {
                    //System.out.println("DEBUG: render - marker off screen or conversion failed");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error rendering tile markers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Render a single tile marker with improved label rendering and debugging
     */
    private void renderTileMarker(Graphics2D g2d, Point screenPos, TileMarker marker) {
        // Save original composite and stroke
        Composite originalComposite = g2d.getComposite();
        Stroke originalStroke = g2d.getStroke();
        Color originalColor = g2d.getColor();
        
        try {
            // Calculate tile bounds (adjust tileSize based on your game)
            int tileSize = 32; // Change this to match your game's tile size
            Rectangle tileBounds = new Rectangle(
                screenPos.x - tileSize/2, 
                screenPos.y - tileSize/2, 
                tileSize, 
                tileSize
            );
            
            // Set transparency for tile fill
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
            
            // Fill the tile with marker color
            g2d.setColor(marker.getColor());
            g2d.fillRect(tileBounds.x, tileBounds.y, tileBounds.width, tileBounds.height);
            
            // Draw border (slightly less transparent)
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.setColor(marker.getColor().darker());
            g2d.drawRect(tileBounds.x, tileBounds.y, tileBounds.width, tileBounds.height);
            
            // Draw label if present
            String label = marker.getLabel();
            if (label != null && !label.trim().isEmpty()) {
                // Reset composite for full opacity text
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                
                // Set up font
                Font labelFont = new Font("Arial", Font.BOLD, 11);
                g2d.setFont(labelFont);
                FontMetrics fm = g2d.getFontMetrics();
                
                String trimmedLabel = label.trim();
                int textWidth = fm.stringWidth(trimmedLabel);
                int textHeight = fm.getHeight();
                int textAscent = fm.getAscent();
                
                // Position text below the tile
                int textX = tileBounds.x + (tileBounds.width - textWidth) / 2;
                int textY = tileBounds.y + tileBounds.height + textAscent + 2; // 2px gap below tile
                
                // Draw text background (dark semi-transparent rectangle)
                int bgPadding = 2;
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.fillRect(textX - bgPadding, textY - textAscent, 
                            textWidth + (bgPadding * 2), textHeight);
                
                // Draw the text (bright white)
                g2d.setColor(Color.WHITE);
                g2d.drawString(trimmedLabel, textX, textY);
            }
            
        } catch (Exception e) {
            System.err.println("Error rendering tile marker: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Restore original graphics state
            g2d.setComposite(originalComposite);
            g2d.setStroke(originalStroke);
            g2d.setColor(originalColor);
        }
    }
    
    public void handleMouseClick(MouseEvent e) {
        try {
            System.out.println("DEBUG: handleMouseClick called with button=" + e.getButton() + " event.shift=" + e.isShiftDown() + " Client.shift=" + Client.shiftDown);
            
            // Check the event's shift state directly - more reliable than Client.shiftDown
            if (!SwingUtilities.isRightMouseButton(e) || !e.isShiftDown()) {
                System.out.println("DEBUG: Not shift+right-click, ignoring");
                return;
            }
            
            System.out.println("DEBUG: Right click + shift detected");
            
            // Get mouse coordinates
            int mouseX = e.getX() - 4;
            int mouseY = e.getY() - 4;
            
            if (Client.instance.getUserSettings().isAntiAliasing()) {
                mouseX <<= 1;
                mouseY <<= 1;
            }
            
            // Get the scene
            WorldController scene = Client.instance.getScene();
            
            // Calculate the tile by searching nearby tiles
            Player player = Client.myPlayer;
            if (player == null) return;
            
            int playerLocalX = player.x >> 7;
            int playerLocalY = player.y >> 7;
            
            int bestTileX = -1;
            int bestTileY = -1;
            double bestDistance = Double.MAX_VALUE;
            
            // Search in a radius around the player
            for (int dx = -15; dx <= 15; dx++) {
                for (int dy = -15; dy <= 15; dy++) {
                    int testLocalX = playerLocalX + dx;
                    int testLocalY = playerLocalY + dy;
                    
                    // Check bounds
                    if (testLocalX < 0 || testLocalX >= 104 || testLocalY < 0 || testLocalY >= 104) {
                        continue;
                    }
                    
                    // Calculate world coordinates for this tile
                    int worldX = testLocalX * 128 + 64;
                    int worldY = testLocalY * 128 + 64;
                    int height = scene.getTileHeights()[Client.plane][testLocalX][testLocalY];
                    
                    // Project to screen using same math as the game
                    int relX = worldX - scene.xCameraPos;
                    int relY = height - scene.zCameraPos;
                    int relZ = worldY - scene.yCameraPos;
                    
                    int rotX = relZ * scene.camLeftRightY + relX * scene.camLeftRightX >> 16;
                    int rotZ = relZ * scene.camLeftRightX - relX * scene.camLeftRightY >> 16;
                    int finalY = relY * scene.camUpDownX - rotZ * scene.camUpDownY >> 16;
                    int finalZ = relY * scene.camUpDownY + rotZ * scene.camUpDownX >> 16;
                    
                    if (finalZ < 50) continue;
                    
                    int screenX = Rasterizer.textureInt1 + (rotX * WorldController.focalLength) / finalZ;
                    int screenY = Rasterizer.textureInt2 + (finalY * WorldController.focalLength) / finalZ;
                    
                    // Calculate distance from mouse to this screen position
                    double distance = Math.sqrt(Math.pow(screenX - mouseX, 2) + Math.pow(screenY - mouseY, 2));
                    
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestTileX = testLocalX;
                        bestTileY = testLocalY;
                    }
                }
            }
            
            if (bestTileX == -1 || bestTileY == -1) {
                System.out.println("DEBUG: Could not find tile");
                return;
            }
            
            int worldTileX = Client.baseX + bestTileX;
            int worldTileY = Client.baseY + bestTileY;
            int plane = Client.plane;
            
            System.out.println("DEBUG: Placing marker at world (" + worldTileX + "," + worldTileY + ")");
            
            TileCoordinate coord = new TileCoordinate(worldTileX, worldTileY, plane);
            
            if (tileMarkers.containsKey(coord)) {
                tileMarkers.remove(coord);
                System.out.println("DEBUG: Removed marker");
            } else {
            	TileMarker marker = new TileMarker(coord, getCurrentMarkerColor(), getCurrentMarkerLabel());
                tileMarkers.put(coord, marker);
                System.out.println("DEBUG: Added marker");
            }
            
            System.out.println("DEBUG: Total markers: " + tileMarkers.size());
            updateSettingsPanelCounter();
            saveTileMarkers();
            
        } catch (Exception ex) {
            System.err.println("DEBUG: Exception in handleMouseClick: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    private int[] screenToTile(int mouseX, int mouseY) {
       
        int x = mouseX - 4;
        int y = mouseY - 4;
        
        if (Client.instance.getUserSettings().isAntiAliasing()) {
            x <<= 1;
            y <<= 1;
        }
        
        return Client.instance.getScene().getClickedTile(x, y);
    }

    /**
     * Handle keyboard shortcuts
     */
    public void handleKeyPress(KeyEvent e) {
        if (!isEnabled) return;
        
        int keyCode = e.getKeyCode();
        
        // Update modifier key states
        shiftHeld = e.isShiftDown();
        ctrlHeld = e.isControlDown();
        
        // Clear all markers with Ctrl+Shift+C
        if (keyCode == KeyEvent.VK_C && ctrlHeld && shiftHeld) {
            clearAllMarkers();
        }
    }
    

    public void addTileMarker(TileCoordinate coord, Color color, String label) {
        //System.out.println("DEBUG: addTileMarker called with coord=" + coord + " color=" + color + " label='" + label + "'");
        
        TileMarker marker = new TileMarker(coord, color, label);
        tileMarkers.put(coord, marker);
        
        //System.out.println("DEBUG: Added marker. Total markers now: " + tileMarkers.size());
        //System.out.println("DEBUG: Marker details - worldX=" + marker.getWorldX() + " worldY=" + marker.getWorldY() + " plane=" + marker.getPlane());
        
        updateSettingsPanelCounter();
        saveTileMarkers();
    }
    
    /**
     * Remove a tile marker at the specified coordinate
     */
    public void removeTileMarker(TileCoordinate coord) {
        TileMarker removed = tileMarkers.remove(coord);
        if (removed != null) {
            //System.out.println("Removed tile marker at " + coord);
            updateSettingsPanelCounter();
            saveTileMarkers();
        }
    }

    /**
     * Clear all tile markers
     */
    public void clearAllMarkers() {
        int count = tileMarkers.size();
        tileMarkers.clear();
        
        updateSettingsPanelCounter();
        saveTileMarkers();
        
        //System.out.println("Cleared " + count + " tile markers");
        
        // Show confirmation
        if (settingsPanel != null) {
            JOptionPane.showMessageDialog(settingsPanel, 
                "Cleared " + count + " tile markers", 
                "Tile Markers", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void updateSettingsPanelCounter() {
        try {
            if (settingsPanel != null && settingsPanel instanceof TileMarkersSettingsPanel) {
                SwingUtilities.invokeLater(() -> {
                    ((TileMarkersSettingsPanel) settingsPanel).updateMarkerCountDisplay();
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to update settings panel counter: " + e.getMessage());
        }
    }
    
    /**
     * Toggle marking mode on/off (kept for settings panel compatibility)
     */
    public void toggleMarkingMode() {
        markingMode = !markingMode;
        //System.out.println("Marking mode: " + (markingMode ? "ON" : "OFF"));
        
        // Update UI if available
        if (settingsPanel != null && settingsPanel instanceof TileMarkersSettingsPanel) {
            ((TileMarkersSettingsPanel) settingsPanel).updateMarkingModeDisplay();
        }
    }
    
    /**
     * Show context menu for tile operations
     */
    private void showContextMenu(Point screenPos, TileCoordinate coord) {
        if (gameCanvas == null) return;
        
        JPopupMenu contextMenu = new JPopupMenu();
        contextMenu.setBackground(PANEL_COLOR);
        
        // Add marker submenu with color options
        JMenu addMarkerMenu = new JMenu("Add Marker");
        addMarkerMenu.setBackground(PANEL_COLOR);
        addMarkerMenu.setForeground(TEXT_COLOR);
        
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            Color color = PRESET_COLORS[i];
            String colorName = getColorName(color);
            
            JMenuItem colorItem = new JMenuItem(colorName);
            colorItem.setBackground(PANEL_COLOR);
            colorItem.setForeground(TEXT_COLOR);
            colorItem.addActionListener(e -> {
                String label = JOptionPane.showInputDialog(settingsPanel, 
                    "Enter label for marker (optional):", 
                    "Add Tile Marker", 
                    JOptionPane.PLAIN_MESSAGE);
                addTileMarker(coord, color, label != null ? label : "");
            });
            
            addMarkerMenu.add(colorItem);
        }
        
        contextMenu.add(addMarkerMenu);
        contextMenu.show(gameCanvas, screenPos.x, screenPos.y);
    }
    
    /**
     * Get display name for color
     */
    private String getColorName(Color color) {
        if (color.equals(PRESET_COLORS[0])) return "Red";
        if (color.equals(PRESET_COLORS[1])) return "Green";
        if (color.equals(PRESET_COLORS[2])) return "Blue";
        if (color.equals(PRESET_COLORS[3])) return "Yellow";
        if (color.equals(PRESET_COLORS[4])) return "Magenta";
        if (color.equals(PRESET_COLORS[5])) return "Cyan";
        if (color.equals(PRESET_COLORS[6])) return "Orange";
        if (color.equals(PRESET_COLORS[7])) return "Purple";
        if (color.equals(PRESET_COLORS[8])) return "White";
        if (color.equals(PRESET_COLORS[9])) return "Black";
        return "Custom";
    }
    
    // ========== CLIENT ACCESS METHODS ==========
    
    private int getPlayerX() {
        try {
            if (clientInstance != null) {
                Player player = clientInstance.getMyPlayer();
                // Return WORLD tile position 
                int localTileX = player.x >> 7;
                int worldTileX = Client.baseX + localTileX;  // Add baseX back
                //System.out.println("DEBUG: getPlayerX - local=" + localTileX + " baseX=" + Client.baseX + " world=" + worldTileX);
                return worldTileX;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int getPlayerY() {
        try {
            if (clientInstance != null) {
                Player player = clientInstance.getMyPlayer();
                // Return WORLD tile position
                int localTileY = player.y >> 7;
                int worldTileY = Client.baseY + localTileY;  // Add baseY back  
                //System.out.println("DEBUG: getPlayerY - local=" + localTileY + " baseY=" + Client.baseY + " world=" + worldTileY);
                return worldTileY;
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private int getCurrentPlane() {
        try {
            if (currentPlane != 0) {
                return currentPlane;
            } else if (clientInstance != null) {
                return clientInstance.getPlane();
            }
            return 0; // Fallback
        } catch (Exception e) {
            return 0;
        }
    }
    
    private Point worldToScreen(int worldX, int worldY) {
        try {
            // Get current player position
            int playerX = getPlayerX();
            int playerY = getPlayerY();
            
            //System.out.println("DEBUG: worldToScreen - player at (" + playerX + "," + playerY + ") rendering marker at (" + worldX + "," + worldY + ")");
            
            // Calculate offset from player
            int deltaX = worldX - playerX;
            int deltaY = worldY - playerY;
            
            //System.out.println("DEBUG: worldToScreen - delta (" + deltaX + "," + deltaY + ")");
            
            // Convert to screen coordinates (32 pixels per tile)
            int screenX = (DrawingArea.width / 2) + (deltaX * 32);
            int screenY = (DrawingArea.height / 2) + (deltaY * 32);
            
            //System.out.println("DEBUG: worldToScreen - screen coords (" + screenX + "," + screenY + ") viewport size (" + DrawingArea.width + "x" + DrawingArea.height + ")");
            
            return new Point(screenX, screenY);
            
        } catch (Exception e) {
            System.err.println("DEBUG: worldToScreen error - " + e.getMessage());
            return null;
        }
    }


    private Point worldToScreenTest(int worldTileX, int worldTileY) {
        try {
            // Convert world tile to local tile (same as displayGroundItems)
            int localTileX = worldTileX - Client.baseX;
            int localTileY = worldTileY - Client.baseY;
            
            if (localTileX < 0 || localTileX >= 104 || localTileY < 0 || localTileY >= 104) {
                return null;
            }
            
            // Use the exact same call as displayGroundItems
            // calcEntityScreenPos((x * 128 + 64), 25, (y * 128 + 64))
            Client.instance.calcEntityScreenPos((localTileX * 128 + 64), 25, (localTileY * 128 + 64));
            
            // Return the screen coordinates
            return new Point(Client.instance.getSpriteDrawX(), Client.instance.getSpriteDrawY());
            
        } catch (Exception e) {
            System.err.println("worldToScreenTest error: " + e.getMessage());
            return null;
        }
    }

    private Point screenToWorld(Point screenPos) {
        try {
            int playerX = Client.baseX + (Client.myPlayer.x >> 7);
            int playerY = Client.baseY + (Client.myPlayer.y >> 7);
            
            int bestX = playerX;
            int bestY = playerY;
            double bestDistance = Double.MAX_VALUE;
            
            // Search a reasonable area around the player
            for (int testX = playerX - 8; testX <= playerX + 8; testX++) {
                for (int testY = playerY - 8; testY <= playerY + 8; testY++) {
                    Point projectedScreen = worldToScreenTest(testX, testY);
                    
                    if (projectedScreen != null) {
                        double distance = Math.sqrt(
                            Math.pow(projectedScreen.x - screenPos.x, 2) + 
                            Math.pow(projectedScreen.y - screenPos.y, 2)
                        );
                        
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestX = testX;
                            bestY = testY;
                        }
                    }
                }
            }
            
            return new Point(bestX, bestY);
            
        } catch (Exception e) {
            System.err.println("screenToWorld error: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if screen position is visible on screen
     */
    private boolean isOnScreen(Point screenPos) {
        return screenPos.x >= 0 && screenPos.x <= viewportWidth &&
               screenPos.y >= 0 && screenPos.y <= viewportHeight;
    }
    
    /**
     * Attach event listeners to game client
     */
    private void attachEventListeners() {
        try {
            if (gameCanvas != null) {
                gameCanvas.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleMouseClick(e);
                    }
                });
                
                gameCanvas.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        handleKeyPress(e);
                    }
                });
                
                // Make sure canvas can receive key events
                gameCanvas.setFocusable(true);
            }
        } catch (Exception e) {
            System.err.println("Failed to attach event listeners: " + e.getMessage());
        }
    }
    
    /**
     * Detach event listeners from game client
     */
    private void detachEventListeners() {
        // Remove the listeners you added in attachEventListeners()
        // Implementation depends on your event system
    }
    
    /**
     * Load tile markers from persistent storage
     */
    private void loadTileMarkers() {
        // Implement loading from file/database/preferences
        //System.out.println("Loading tile markers from storage...");
    }
    
    private JPanel createFallbackSettingsPanel() {
        JPanel fallback = new JPanel(new BorderLayout());
        fallback.setBackground(new Color(0x1e, 0x1e, 0x1e));
        fallback.add(new JLabel("Tile Markers settings could not load", SwingConstants.CENTER), BorderLayout.CENTER);
        return fallback;
    }
    
    /**
     * Save tile markers to persistent storage
     */
    private void saveTileMarkers() {
        // Implement saving to file/database/preferences
        //System.out.println("Saving tile markers to storage...");
    }
    
    // ========== GETTERS AND SETTERS ==========
    
    public Color getCurrentMarkerColor() {
        return currentMarkerColor;
    }
    
    public void setCurrentMarkerColor(Color color) {
        this.currentMarkerColor = color;
    }
    
    public String getCurrentMarkerLabel() {
        return currentMarkerLabel;
    }
    
    public void setCurrentMarkerLabel(String label) {
        this.currentMarkerLabel = label;
    }
    
    public boolean isMarkingMode() {
        return markingMode;
    }
    
    public Map<TileCoordinate, TileMarker> getTileMarkers() {
        return new HashMap<>(tileMarkers);
    }
    
    /**
     * Set viewport dimensions if they change
     */
    public void setViewportSize(int width, int height) {
        this.viewportWidth = width;
        this.viewportHeight = height;
    }
    
    /**
     * Set the game canvas reference
     */
    public void setGameCanvas(Component canvas) {
        this.gameCanvas = canvas;
    }
}