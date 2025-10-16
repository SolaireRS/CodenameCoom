package com.client.plugins.ammo;

import com.client.Client;
import com.client.CustomGameFrame;
import com.client.definitions.ItemDefinition;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Simple Ammo Plugin - Shows equipped ammo sprite with count
 */
public class AmmoPlugin {
    private CustomGameFrame gameFrame;
    private Client client;
    private boolean initialized = false;
    private boolean enabled = false;
    
    // Settings
    private boolean showAmmoOverlay = true;
    private int overlayXPosition = 15;
    private int overlayYPosition = 50;
    private int frameColor = 0x555555; // Default gray frame
    
    // Current equipped ammo
    private int equippedAmmoId = -1;
    private int equippedAmmoCount = 0;
    
    // UI Components
    private AmmoSettingsPanel settingsPanel;
    
    // Equipment slot constants
    private static final int AMMO_SLOT = 13;
    private static final int WEAPON_SLOT = 3;
    
    public AmmoPlugin(CustomGameFrame gameFrame) {
        this.gameFrame = gameFrame;
        this.settingsPanel = new AmmoSettingsPanel(this);
        System.out.println("AmmoPlugin: Created");
    }
    
    public boolean initialize() {
        if (initialized) return true;
        
        try {
            this.client = findClientInFrame();
            if (client == null) {
                //System.err.println("AmmoPlugin: Could not find Client component");
                return false;
            }
            
            this.settingsPanel = new AmmoSettingsPanel(this);
            initialized = true;
            System.out.println("AmmoPlugin: Initialized successfully");
            return true;
            
        } catch (Exception e) {
            //System.err.println("AmmoPlugin: Initialization failed: " + e.getMessage());
            return false;
        }
    }
    
    public void enable() {
        if (!initialized) return;
        enabled = true;
        System.out.println("AmmoPlugin: Enabled");
    }
    
    public void disable() {
        enabled = false;
        System.out.println("AmmoPlugin: Disabled");
    }
    
    public void cleanup() {
        disable();
    }
    
    public boolean isInitialized() { return initialized; }
    public boolean isEnabled() { return enabled; }
    
    public JPanel getSettingsPanel() {
        if (settingsPanel == null) {
            settingsPanel = new AmmoSettingsPanel(this);
        }
        return settingsPanel;
    }
    
    /**
     * Main render method - call this from client rendering loop
     */
    public void renderAmmoOverlay() {
        if (!enabled || !showAmmoOverlay || client == null) return;
        
        try {
            // Update equipped ammo info
            updateEquippedAmmo();
            
            // Render if we have equipped ammo
            if (equippedAmmoId != -1 && equippedAmmoCount > 0) {
                renderAmmoDisplay();
            }
            
        } catch (Exception e) {
            //System.err.println("AmmoPlugin: Render error: " + e.getMessage());
        }
    }
    
    /**
     * Find equipped ammo in equipment slots only
     */
    private void updateEquippedAmmo() {
        // Reset
        equippedAmmoId = -1;
        equippedAmmoCount = 0;
        
        try {
            // Try to find equipped items via equipment container
            int[] ammoData = findEquippedInContainer();
            if (ammoData[0] != -1) {
                equippedAmmoId = ammoData[0];
                equippedAmmoCount = ammoData[1];
                return;
            }
            
        } catch (Exception e) {
            //System.err.println("DEBUG: Error finding equipped ammo: " + e.getMessage());
        }
    }
    
    /**
     * Try to find equipped ammo via equipment container
     */
    private int[] findEquippedInContainer() {
        try {
            // Check equipment interfaces
            int[] interfaceResult = checkEquipmentInterfaces();
            if (interfaceResult[0] != -1) {
                return interfaceResult;
            }
            
        } catch (Exception e) {
            //System.err.println("DEBUG: Container search failed: " + e.getMessage());
        }
        
        return new int[]{-1, 0};
    }
    
    /**
     * Check equipment through RSInterface system
     */
    private int[] checkEquipmentInterfaces() {
        try {
            // Access RSInterface from the correct package
            Class<?> rsInterfaceClass = Class.forName("com.client.graphics.interfaces.RSInterface");
            
            // Access the static interfaceCache field
            java.lang.reflect.Field interfaceCacheField = rsInterfaceClass.getDeclaredField("interfaceCache");
            interfaceCacheField.setAccessible(true);
            Object[] interfaceCache = (Object[]) interfaceCacheField.get(null);
            
            // Equipment interface IDs (we know 1688 works)
            int[] equipmentInterfaceIds = {1688};
            
            for (int interfaceId : equipmentInterfaceIds) {
                if (interfaceId < interfaceCache.length && interfaceCache[interfaceId] != null) {
                    Object equipmentInterface = interfaceCache[interfaceId];
                    
                    try {
                        // Access the inv array
                        java.lang.reflect.Field invField = equipmentInterface.getClass().getDeclaredField("inv");
                        invField.setAccessible(true);
                        int[] inv = (int[]) invField.get(equipmentInterface);
                        
                        if (inv != null && inv.length > AMMO_SLOT && inv[AMMO_SLOT] > 0) {
                            int ammoValue = inv[AMMO_SLOT];
                            if (ammoValue > 0) {
                                int count = getInterfaceItemCount(equipmentInterface, AMMO_SLOT);
                                return new int[]{ammoValue, Math.max(1, count)};
                            }
                        }
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        continue;
                    }
                }
            }
        } catch (Exception e) {
            //System.err.println("DEBUG: Equipment interface check failed: " + e.getMessage());
        }
        
        return new int[]{-1, 0};
    }
    
    /**
     * Get item count from interface
     */
    private int getInterfaceItemCount(Object equipmentInterface, int slot) {
        try {
            java.lang.reflect.Field stackSizesField = equipmentInterface.getClass().getDeclaredField("invStackSizes");
            stackSizesField.setAccessible(true);
            int[] stackSizes = (int[]) stackSizesField.get(equipmentInterface);
            
            if (stackSizes != null && stackSizes.length > slot && stackSizes[slot] > 0) {
                return stackSizes[slot];
            }
        } catch (Exception e) {
            // No count found
        }
        
        return 1; // Default count
    }
    
    /**
     * Render the ammo display (sprite + count)
     */
    private void renderAmmoDisplay() {
        try {
            // Draw background
            drawBackground();
            
            // Try to draw item sprite, if it fails draw the polished indicator
            if (!drawItemSprite(equippedAmmoId)) {
                drawSimpleAmmoIndicator();
            }
            
            // Draw count
            drawCount();
            
        } catch (Exception e) {
            //System.err.println("DEBUG: Render error: " + e.getMessage());
        }
    }
    
    /**
     * Draw background box with frame
     */
    private void drawBackground() {
        try {
            String[] classes = {"com.client.DrawingArea", "DrawingArea"};
            
            for (String className : classes) {
                try {
                    Class<?> drawingClass = Class.forName(className);
                    java.lang.reflect.Method method = drawingClass.getMethod("drawAlphaBox", 
                        int.class, int.class, int.class, int.class, int.class, int.class);
                    
                    // Draw larger background box to fit arrow properly
                    int boxSize = 40;
                    method.invoke(null, overlayXPosition, overlayYPosition, boxSize, boxSize, 0x000000, 120);
                    
                    // Draw colored frame around the box
                    drawFrame(drawingClass, method, boxSize);
                    
                    return;
                } catch (Exception e) {
                    continue;
                }
            }
        } catch (Exception e) {
            //System.err.println("DEBUG: Could not draw background");
        }
    }
    
    /**
     * Draw colored frame around the ammo box
     */
    private void drawFrame(Class<?> drawingClass, java.lang.reflect.Method method, int boxSize) {
        try {
            int frameThickness = 2;
            
            // Top border
            method.invoke(null, overlayXPosition, overlayYPosition, boxSize, frameThickness, frameColor, 255);
            // Bottom border  
            method.invoke(null, overlayXPosition, overlayYPosition + boxSize - frameThickness, boxSize, frameThickness, frameColor, 255);
            // Left border
            method.invoke(null, overlayXPosition, overlayYPosition, frameThickness, boxSize, frameColor, 255);
            // Right border
            method.invoke(null, overlayXPosition + boxSize - frameThickness, overlayYPosition, frameThickness, boxSize, frameColor, 255);
            
        } catch (Exception e) {
            //System.err.println("DEBUG: Could not draw frame");
        }
    }
    
    /**
     * Draw polished ammo indicator
     */
    private void drawSimpleAmmoIndicator() {
        try {
            String[] classes = {"com.client.DrawingArea", "DrawingArea"};
            
            for (String className : classes) {
                try {
                    Class<?> drawingClass = Class.forName(className);
                    java.lang.reflect.Method method = drawingClass.getMethod("drawAlphaBox", 
                        int.class, int.class, int.class, int.class, int.class, int.class);
                    method.invoke(null, overlayXPosition + 6, overlayYPosition + 6, 
                                 20, 20, 0x4682B4, 230); // Steel blue, better centered
                    return;
                } catch (Exception e) {
                    continue;
                }
            }
        } catch (Exception e) {
            //System.err.println("DEBUG: Error drawing ammo indicator: " + e.getMessage());
        }
    }
    
    /**
     * Try to draw item sprite
     */
    private boolean drawItemSprite(int itemId) {
        try {
            // Your client code uses itemId - 1 when calling getSprite
            int adjustedId = itemId - 1;
            
            // Try with adjusted ID first (like your client does)
            java.lang.reflect.Method getSpriteMethod = ItemDefinition.class.getMethod("getSprite", 
                int.class, int.class, int.class);
            Object sprite = getSpriteMethod.invoke(null, adjustedId, equippedAmmoCount, 0);
            
            if (sprite != null) {
                return drawSpriteUsingClientMethods(sprite);
            }
            
            // Try with original ID as fallback
            sprite = getSpriteMethod.invoke(null, itemId, equippedAmmoCount, 0);
            if (sprite != null) {
                return drawSpriteUsingClientMethods(sprite);
            }
            
        } catch (Exception e) {
            // Sprite failed
        }
        
        return false;
    }
    
    /**
     * Draw sprite using client methods
     */
    private boolean drawSpriteUsingClientMethods(Object sprite) {
        try {
            // Center sprite in the larger 40x40 box, moved another pixel left
            int x = overlayXPosition + 6;  // Moved left by another pixel
            int y = overlayYPosition + 6;  // Slightly higher to leave room for text below
            
            // Try drawTransparentSprite method
            java.lang.reflect.Method drawTransparentMethod = sprite.getClass().getMethod("drawTransparentSprite", 
                int.class, int.class, int.class);
            drawTransparentMethod.invoke(sprite, x, y, 256);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Draw ammo count - centered white text
     */
    private void drawCount() {
        try {
            Object font = getClientFont();
            if (font != null) {
                String countText = String.valueOf(equippedAmmoCount);
                
                // Center the text horizontally in the 40x40 box
                int textX = overlayXPosition + 20 - (countText.length() * 3); // Rough centering
                int textY = overlayYPosition + 35; // Near bottom of box
                
                java.lang.reflect.Method method = font.getClass().getMethod("drawBasicString", 
                    String.class, int.class, int.class, int.class, int.class);
                method.invoke(font, countText, textX, textY, 0xFFFFFF, -1); // White text
            }
        } catch (Exception e) {
            //System.err.println("DEBUG: Could not draw count");
        }
    }
    
    /**
     * Get client font
     */
    private Object getClientFont() {
        String[] fontFields = {"newRegularFont", "regularFont", "textDrawingArea"};
        
        for (String fieldName : fontFields) {
            try {
                java.lang.reflect.Field field = client.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object font = field.get(client);
                if (font != null) return font;
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }
    
    /**
     * Find client component in frame
     */
    private Client findClientInFrame() {
        return findClientComponent(gameFrame.getContentPane());
    }
    
    private Client findClientComponent(Container container) {
        try {
            Component[] components = container.getComponents();
            if (components == null) return null;
            
            for (Component component : components) {
                if (component instanceof Client) {
                    return (Client) component;
                }
                if (component instanceof Container && component != container) {
                    Client found = findClientComponent((Container) component);
                    if (found != null) return found;
                }
            }
        } catch (Exception e) {
            //System.err.println("DEBUG: Error finding client component: " + e.getMessage());
        }
        return null;
    }
    
    // Settings getters/setters
    public boolean isShowAmmoOverlay() { return showAmmoOverlay; }
    public void setShowAmmoOverlay(boolean show) { this.showAmmoOverlay = show; }
    public int getOverlayXPosition() { return overlayXPosition; }
    public void setOverlayXPosition(int x) { this.overlayXPosition = x; }
    public int getOverlayYPosition() { return overlayYPosition; }
    public void setOverlayYPosition(int y) { this.overlayYPosition = y; }
    public int getFrameColor() { return frameColor; }
    public void setFrameColor(int color) { this.frameColor = color; }
    
    /**
     * Slim Ammo Settings Panel - Matches GPU Plugin style
     */
    private class AmmoSettingsPanel extends JPanel {
        
        private final Color BACKGROUND_COLOR = new Color(0x2b, 0x2b, 0x2b); // Slightly lighter than frame
        private final Color TEXT_COLOR = new Color(0x00, 0xa2, 0xe8);
        private final Color MUTED_TEXT = new Color(0x9f, 0x9f, 0x9f);
        private final Color BORDER_COLOR = new Color(0x3c, 0x3c, 0x3c);
        
        private AmmoPlugin plugin;
        
        public AmmoSettingsPanel(AmmoPlugin plugin) {
            this.plugin = plugin;
            
            setBackground(BACKGROUND_COLOR);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(10, 10, 10, 10));
            
            createSlimUI();
        }
        
        private void createSlimUI() {
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBackground(BACKGROUND_COLOR);
            
            // Header - minimal
            JLabel titleLabel = new JLabel("Ammo Plugin Settings");
            titleLabel.setForeground(TEXT_COLOR);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            mainPanel.add(titleLabel);
            
            // Settings list - compact rows
            mainPanel.add(createSlimRow("Show Ammo Overlay", createToggleButton(plugin.isShowAmmoOverlay(), e -> {
                plugin.setShowAmmoOverlay(((JCheckBox)e.getSource()).isSelected());
            })));
            
            mainPanel.add(createSlimRow("X Position", createNumericSlider(
                plugin.getOverlayXPosition(), 0, 800,
                value -> plugin.setOverlayXPosition(value)
            )));
            
            mainPanel.add(createSlimRow("Y Position", createNumericSlider(
                plugin.getOverlayYPosition(), 0, 600,
                value -> plugin.setOverlayYPosition(value)
            )));
            
            mainPanel.add(createSlimRow("Frame Color", createColorDropdown(
                plugin.getFrameColor(),
                color -> plugin.setFrameColor(color)
            )));
            
            add(mainPanel, BorderLayout.NORTH); // Align to top
        }
        
        private JPanel createSlimRow(String labelText, JComponent control) {
            JPanel row = new JPanel(new BorderLayout());
            row.setBackground(BACKGROUND_COLOR);
            row.setBorder(new EmptyBorder(3, 5, 3, 5));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            JLabel label = new JLabel(labelText);
            label.setForeground(MUTED_TEXT);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            control.setPreferredSize(new Dimension(120, 22));
            
            row.add(label, BorderLayout.WEST);
            row.add(control, BorderLayout.EAST);
            
            return row;
        }
        
        private JCheckBox createToggleButton(boolean selected, ActionListener listener) {
            JCheckBox toggle = new JCheckBox();
            toggle.setSelected(selected);
            toggle.setBackground(BACKGROUND_COLOR);
            toggle.setFocusPainted(false);
            
            // Style the checkbox to show blue when selected
            toggle.setForeground(TEXT_COLOR); // Blue color when selected
            toggle.addActionListener(listener);
            
            // Custom UI to make selected checkboxes blue
            toggle.setUI(new javax.swing.plaf.basic.BasicCheckBoxUI() {
                @Override
                public void paint(Graphics g, JComponent c) {
                    JCheckBox cb = (JCheckBox) c;
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Draw checkbox background
                    g2.setColor(cb.getBackground());
                    g2.fillRect(0, 0, 16, 16);
                    
                    // Draw border
                    g2.setColor(cb.isSelected() ? TEXT_COLOR : MUTED_TEXT);
                    g2.drawRect(0, 0, 15, 15);
                    
                    // Draw checkmark if selected
                    if (cb.isSelected()) {
                        g2.setColor(TEXT_COLOR);
                        g2.setStroke(new BasicStroke(2));
                        // Draw checkmark
                        g2.drawLine(3, 8, 6, 11);
                        g2.drawLine(6, 11, 12, 4);
                    }
                }
            });
            
            return toggle;
        }
        
        private JPanel createNumericSlider(int value, int min, int max, java.util.function.Consumer<Integer> onChange) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            panel.setBackground(BACKGROUND_COLOR);
            
            JSlider slider = new JSlider(min, max, value);
            slider.setBackground(BACKGROUND_COLOR);
            slider.setPreferredSize(new Dimension(80, 22));
            
            JLabel valueLabel = new JLabel(String.valueOf(value));
            valueLabel.setForeground(TEXT_COLOR);
            valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            valueLabel.setPreferredSize(new Dimension(35, 22));
            
            slider.addChangeListener(e -> {
                int val = slider.getValue();
                valueLabel.setText(String.valueOf(val));
                onChange.accept(val);
            });
            
            panel.add(slider);
            panel.add(valueLabel);
            return panel;
        }
        
        private JComboBox<String> createColorDropdown(int currentColor, java.util.function.Consumer<Integer> onChange) {
            // Color options with names and hex values
            String[] colorNames = {"Gray", "Blue", "Gold", "Red", "Green", "Purple", "Orange"};
            int[] colorValues = {0x555555, 0x4682B4, 0xFFD700, 0xFF4444, 0x44FF44, 0x8A2BE2, 0xFF8C00};
            
            // Find current selection
            String selectedName = "Gray"; // default
            for (int i = 0; i < colorValues.length; i++) {
                if (colorValues[i] == currentColor) {
                    selectedName = colorNames[i];
                    break;
                }
            }
            
            JComboBox<String> combo = new JComboBox<>(colorNames);
            combo.setSelectedItem(selectedName);
            combo.setBackground(BACKGROUND_COLOR);
            combo.setForeground(TEXT_COLOR);
            combo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            
            combo.addActionListener(e -> {
                String selected = (String) combo.getSelectedItem();
                for (int i = 0; i < colorNames.length; i++) {
                    if (colorNames[i].equals(selected)) {
                        onChange.accept(colorValues[i]);
                        break;
                    }
                }
            });
            
            return combo;
        }
    }
}