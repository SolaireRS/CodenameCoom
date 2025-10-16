package com.client;

import com.client.plugins.gpu.GPUPlugin;
import com.client.plugins.tilemarkers.TileMarkersPlugin;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;
import com.client.plugins.ammo.AmmoPlugin;
import com.client.plugins.stretchedmode.StretchedModePlugin;
public class PluginPanel extends JPanel {
    private CardLayout cardLayout;
    private JTextField searchField;
    private JPanel pluginListPanel;
    private List<PluginItem> allPlugins;
    private List<PluginItemPanel> pluginPanels;
    
    // Main container and views
    private JPanel mainContainer;
    private JPanel pluginListContainer;
    private JPanel settingsContainer;
    private JLabel settingsTitle;
    private boolean showingSettings = false;
    
    // Plugin List
    private GPUPlugin gpuPlugin;
    private TileMarkersPlugin tileMarkersPlugin;
    private AmmoPlugin ammoPlugin;
    private CustomGameFrame gameFrame;
    private StretchedModePlugin stretchedModePlugin;
    // Star icon images - created programmatically
    private ImageIcon unfilledStarIcon;
    private ImageIcon filledStarIcon;

    public PluginPanel(CustomGameFrame gameFrame) {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
        this.gameFrame = gameFrame;
        
        setLayout(new BorderLayout());
        updateThemeColors(); // Set initial colors
        setBorder(new EmptyBorder(5, 5, 5, 5));
        
        //Initialize Plugins
        initializeGPUPlugin();
        initializeTileMarkersPlugin();
        initializeAmmoPlugin();
        initializeStretchedModePlugin();
        //End of Plugin Init
        createStarIcons();
        initializePlugins();
        createMainContainer();
    }
    
    // Method to get current theme colors - background should be darker (frame background)
    private Color getBackgroundColor() {
        if (gameFrame != null && gameFrame.getThemeManager() != null) {
            return gameFrame.getThemeManager().getFrameBackground(); // Use frame background (darker)
        }
        return new Color(0x1e, 0x1e, 0x1e); // Fallback
    }
    
    private Color getPanelColor() {
        if (gameFrame != null && gameFrame.getThemeManager() != null) {
            return gameFrame.getThemeManager().getSidebarBackground(); // Use sidebar background for panels
        }
        return new Color(0x2b, 0x2b, 0x2b); // Fallback
    }
    
    private Color getTextColor() {
        if (gameFrame != null && gameFrame.getThemeManager() != null) {
            return gameFrame.getThemeManager().getAccentColor();
        }
        return new Color(0x00a2e8); // Fallback
    }
    
    private Color getMutedTextColor() {
        if (gameFrame != null && gameFrame.getThemeManager() != null) {
            return gameFrame.getThemeManager().getTextColor();
        }
        return new Color(0x9f, 0x9f, 0x9f); // Fallback
    }
    
    private Color getSearchBackgroundColor() {
        if (gameFrame != null && gameFrame.getThemeManager() != null) {
            return gameFrame.getThemeManager().getSidebarBackground(); // Use sidebar background for search
        }
        return new Color(0x3c, 0x3c, 0x3c); // Fallback
    }
    
    private Color getHoverColor() {
        if (gameFrame != null && gameFrame.getThemeManager() != null) {
            return gameFrame.getThemeManager().getHoverColor();
        }
        return new Color(0x48, 0x48, 0x48); // Fallback
    }
    
    private Color getIconColor() {
        if (gameFrame != null && gameFrame.getThemeManager() != null) {
            return gameFrame.getThemeManager().getAccentColor();
        }
        return new Color(0x4a, 0x90, 0xe2); // Fallback
    }
    
    // Method to update all theme colors - called when theme changes
    public void updateThemeColors() {
        setBackground(getBackgroundColor()); // Use darker frame background
        
        // Update all child components
        updateComponentColorsRecursively(this);
        
        // Recreate icons with new colors
        createStarIcons();
        
        // Update all plugin panels
        if (pluginPanels != null) {
            for (PluginItemPanel panel : pluginPanels) {
                panel.updateThemeColors();
            }
        }
        
        revalidate();
        repaint();
    }
    
    private void updateComponentColorsRecursively(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel) {
                comp.setBackground(getBackgroundColor()); // All panels use frame background (darker)
            } else if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label == settingsTitle) {
                    label.setForeground(getTextColor());
                } else {
                    label.setForeground(getMutedTextColor());
                }
            } else if (comp instanceof JTextField) {
                JTextField field = (JTextField) comp;
                field.setBackground(getSearchBackgroundColor());
                field.setForeground(getMutedTextColor());
            } else if (comp instanceof JButton) {
                JButton button = (JButton) comp;
                button.setBackground(getPanelColor());
                button.setForeground(getTextColor());
            }
            
            if (comp instanceof Container) {
                updateComponentColorsRecursively((Container) comp);
            }
        }
    }
    
    private void createMainContainer() {
        mainContainer = new JPanel(new CardLayout());
        mainContainer.setBackground(getBackgroundColor());
        
        // Create plugin list container
        pluginListContainer = new JPanel(new BorderLayout());
        pluginListContainer.setBackground(getBackgroundColor());
        
        createSearchBar();
        createPluginList();
        
        pluginListContainer.add(createSearchPanel(), BorderLayout.NORTH);
        pluginListContainer.add(createScrollPane(), BorderLayout.CENTER);
        
        // Create settings container
        settingsContainer = new JPanel(new BorderLayout());
        settingsContainer.setBackground(getBackgroundColor());
        
        // Settings header with back button
        JPanel settingsHeader = new JPanel(new BorderLayout());
        settingsHeader.setBackground(getBackgroundColor());
        settingsHeader.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        JButton backButton = new JButton("← Back");
        backButton.setBackground(getPanelColor());
        backButton.setForeground(getTextColor());
        backButton.setFocusPainted(false);
        backButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        backButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backButton.addActionListener(e -> showPluginList());
        
        settingsTitle = new JLabel("Settings");
        settingsTitle.setForeground(getTextColor());
        settingsTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        settingsTitle.setHorizontalAlignment(SwingConstants.CENTER);
        
        settingsHeader.add(backButton, BorderLayout.WEST);
        settingsHeader.add(settingsTitle, BorderLayout.CENTER);
        
        settingsContainer.add(settingsHeader, BorderLayout.NORTH);
        
        // Add both containers to main container
        mainContainer.add(pluginListContainer, "LIST");
        mainContainer.add(settingsContainer, "SETTINGS");
        
        add(mainContainer, BorderLayout.CENTER);
        updatePluginDisplay();
    }
    
    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBackground(getBackgroundColor());
        searchPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        // Add search icon
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setForeground(getMutedTextColor());
        searchIcon.setBorder(new EmptyBorder(0, 0, 0, 8));
        
        JPanel searchContainer = new JPanel(new BorderLayout());
        searchContainer.setBackground(getSearchBackgroundColor());
        searchContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getIconColor(), 1),
            BorderFactory.createEmptyBorder(1, 1, 1, 1)
        ));
        
        searchContainer.add(searchField, BorderLayout.CENTER);
        searchContainer.add(searchIcon, BorderLayout.EAST);
        
        searchPanel.add(searchContainer, BorderLayout.CENTER);
        return searchPanel;
    }

    private JScrollPane createScrollPane() {
        JScrollPane scrollPane = new JScrollPane(pluginListPanel);
        scrollPane.setBackground(getBackgroundColor());
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // PREVENT HORIZONTAL SCROLLING
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Style the scrollbar
        scrollPane.getVerticalScrollBar().setBackground(getBackgroundColor());
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        
        return scrollPane;
    }
    
    public void showPluginList() {
        CardLayout layout = (CardLayout) mainContainer.getLayout();
        layout.show(mainContainer, "LIST");
        showingSettings = false;
    }
    
    public void showSettings(String pluginName, JPanel settingsPanel) {
        settingsTitle.setText(pluginName + " Settings");
        
        // Clear existing settings content
        if (settingsContainer.getComponentCount() > 1) {
            settingsContainer.remove(1);
        }
        
        // Wrap the settings panel in a scroll pane
        JScrollPane settingsScrollPane = new JScrollPane(settingsPanel);
        settingsScrollPane.setBackground(getBackgroundColor());
        settingsScrollPane.setBorder(null);
        settingsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        // PREVENT HORIZONTAL SCROLLING FOR SETTINGS
        settingsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        settingsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Style the scrollbar
        settingsScrollPane.getVerticalScrollBar().setBackground(getBackgroundColor());
        settingsScrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        
        // FIX MOUSE WHEEL SCROLLING - enable scrolling anywhere in the panel
        settingsScrollPane.addMouseWheelListener(e -> {
            JScrollBar verticalScrollBar = settingsScrollPane.getVerticalScrollBar();
            int unitsToScroll = e.getUnitsToScroll() * 16; // 16 pixels per unit
            int currentValue = verticalScrollBar.getValue();
            int newValue = currentValue + unitsToScroll;
            
            // Clamp to valid range
            newValue = Math.max(verticalScrollBar.getMinimum(), 
                       Math.min(newValue, verticalScrollBar.getMaximum() - verticalScrollBar.getVisibleAmount()));
            
            verticalScrollBar.setValue(newValue);
        });
        
        // Also enable mouse wheel scrolling on the settings panel itself
        settingsPanel.addMouseWheelListener(e -> {
            // Forward the event to the scroll pane
            settingsScrollPane.dispatchEvent(e);
        });
        
        // Add the scroll pane instead of the raw settings panel
        settingsContainer.add(settingsScrollPane, BorderLayout.CENTER);
        
        CardLayout layout = (CardLayout) mainContainer.getLayout();
        layout.show(mainContainer, "SETTINGS");
        showingSettings = true;
        
        // Refresh the container
        settingsContainer.revalidate();
        settingsContainer.repaint();
    }
    
    /**
     * Get the shared GPU plugin instance from CustomGameFrame
     */
    private void initializeGPUPlugin() {
        try {
            // GET THE SHARED INSTANCE instead of creating a new one
            this.gpuPlugin = gameFrame.getGPUPlugin();
            if (gpuPlugin != null) {
                System.out.println("PluginPanel: Using shared GPU Plugin instance: " + gpuPlugin.hashCode());
            } else {
                System.err.println("PluginPanel: No GPU Plugin available from CustomGameFrame!");
            }
        } catch (Exception e) {
            System.err.println("Failed to get shared GPU Plugin: " + e.getMessage());
            e.printStackTrace();
            // Show error to user
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Failed to get GPU Plugin: " + e.getMessage(),
                    "GPU Plugin Error", 
                    JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    
    /**
     * Initialize the Tile Markers plugin
     */
    private void initializeTileMarkersPlugin() {
        try {
            this.tileMarkersPlugin = new TileMarkersPlugin();
            Client.tileMarkersPlugin = this.tileMarkersPlugin; // ADD THIS LINE
            System.out.println("PluginPanel: Tile Markers Plugin created: " + tileMarkersPlugin.hashCode());
        } catch (Exception e) {
            System.err.println("Failed to create Tile Markers Plugin: " + e.getMessage());
            e.printStackTrace();
            // Show error to user
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Failed to create Tile Markers Plugin: " + e.getMessage(),
                    "Tile Markers Plugin Error", 
                    JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    
    private void initializeAmmoPlugin() {
        try {
            this.ammoPlugin = new AmmoPlugin(gameFrame);
            System.out.println("PluginPanel: Ammo Plugin created: " + ammoPlugin.hashCode());
        } catch (Exception e) {
            System.err.println("Failed to create Ammo Plugin: " + e.getMessage());
            e.printStackTrace();
            // Show error to user
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Failed to create Ammo Plugin: " + e.getMessage(),
                    "Ammo Plugin Error", 
                    JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    private void initializeStretchedModePlugin() {
        try {
            this.stretchedModePlugin = new StretchedModePlugin(gameFrame);
            System.out.println("PluginPanel: Stretched Mode Plugin created: " + stretchedModePlugin.hashCode());
        } catch (Exception e) {
            System.err.println("Failed to create Stretched Mode Plugin: " + e.getMessage());
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Failed to create Stretched Mode Plugin: " + e.getMessage(),
                    "Stretched Mode Plugin Error", 
                    JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    private void createStarIcons() {
        unfilledStarIcon = new ImageIcon(createStarIcon(18, false));
        filledStarIcon = new ImageIcon(createStarIcon(18, true));
    }
    
    private BufferedImage createStarIcon(int size, boolean filled) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        
        // Enable anti-aliasing for smooth edges
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Create star shape
        int[] xPoints = new int[10];
        int[] yPoints = new int[10];
        
        double centerX = size / 2.0;
        double centerY = size / 2.0;
        double outerRadius = size * 0.4;
        double innerRadius = size * 0.16;
        
        for (int i = 0; i < 10; i++) {
            double angle = Math.PI * i / 5.0 - Math.PI / 2.0;
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;
            xPoints[i] = (int) (centerX + radius * Math.cos(angle));
            yPoints[i] = (int) (centerY + radius * Math.sin(angle));
        }
        
        if (filled) {
            // Filled star - use current theme icon color
            g2.setColor(getIconColor());
            g2.fillPolygon(xPoints, yPoints, 10);
            
            // Add slight border for definition
            g2.setColor(getIconColor().darker());
            g2.setStroke(new BasicStroke(0.5f));
            g2.drawPolygon(xPoints, yPoints, 10);
        } else {
            // Unfilled star - just outline
            g2.setColor(new Color(0x5a, 0x5a, 0x5a));
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawPolygon(xPoints, yPoints, 10);
        }
        
        g2.dispose();
        return img;
    }
    
    
    private BufferedImage createCogIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        
        // Enable anti-aliasing for smooth edges
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        double centerX = size / 2.0;
        double centerY = size / 2.0;
        double outerRadius = size * 0.42;
        double innerRadius = size * 0.30;
        double holeRadius = size * 0.16;
        
        // Use current theme icon color
        g2.setColor(getIconColor());
        g2.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Create 8 smooth teeth
        int numTeeth = 8;
        for (int i = 0; i < numTeeth; i++) {
            double angle = (2.0 * Math.PI * i) / numTeeth;
            double toothWidth = Math.PI / 16; // Narrow teeth
            
            // Create rounded rectangular teeth
            double angle1 = angle - toothWidth / 2;
            double angle2 = angle + toothWidth / 2;
            
            // Outer edge of tooth
            int x1 = (int)(centerX + outerRadius * Math.cos(angle1));
            int y1 = (int)(centerY + outerRadius * Math.sin(angle1));
            int x2 = (int)(centerX + outerRadius * Math.cos(angle2));
            int y2 = (int)(centerY + outerRadius * Math.sin(angle2));
            
            // Inner edge of tooth
            int x3 = (int)(centerX + innerRadius * Math.cos(angle2));
            int y3 = (int)(centerY + innerRadius * Math.sin(angle2));
            int x4 = (int)(centerX + innerRadius * Math.cos(angle1));
            int y4 = (int)(centerY + innerRadius * Math.sin(angle1));
            
            // Draw the tooth outline
            g2.drawLine(x1, y1, x2, y2); // outer edge
            g2.drawLine(x2, y2, x3, y3); // right side
            g2.drawLine(x4, y4, x1, y1); // left side
        }
        
        // Draw the main body circle
        int bodySize = (int)(innerRadius * 2);
        g2.drawOval((int)(centerX - innerRadius), (int)(centerY - innerRadius), bodySize, bodySize);
        
        // Draw the center hole
        int holeSize = (int)(holeRadius * 2);
        g2.drawOval((int)(centerX - holeRadius), (int)(centerY - holeRadius), holeSize, holeSize);
        
        g2.dispose();
        return img;
    }
    
    private void initializePlugins() {
        allPlugins = new ArrayList<>();
        pluginPanels = new ArrayList<>();
        
        // Add only the actual functional plugins (removed all placeholder plugins)
        PluginItem ammoPluginItem = new PluginItem("Ammo", false, true, false);
        if (ammoPlugin != null) {
            ammoPluginItem.setAmmoPlugin(ammoPlugin);
        }
        allPlugins.add(ammoPluginItem);
        
        PluginItem gpuPluginItem = new PluginItem("GPU Renderer", false, true, false);
        if (gpuPlugin != null) {
            gpuPluginItem.setGPUPlugin(gpuPlugin);
        }
        allPlugins.add(gpuPluginItem);
        
        PluginItem stretchedModePluginItem = new PluginItem("Stretched Mode", false, true, false);
        if (stretchedModePlugin != null) {
            stretchedModePluginItem.setStretchedModePlugin(stretchedModePlugin);
        }
        allPlugins.add(stretchedModePluginItem);
        
        PluginItem tileMarkersPluginItem = new PluginItem("Tile Markers", false, true, false);
        if (tileMarkersPlugin != null) {
            tileMarkersPluginItem.setTileMarkersPlugin(tileMarkersPlugin);
        }
        allPlugins.add(tileMarkersPluginItem);
        
        // Sort plugins alphabetically initially
        sortPlugins();
    }
    
    /**
     * Sort plugins with favorites at the top, then alphabetically
     */
    private void sortPlugins() {
        allPlugins.sort(new Comparator<PluginItem>() {
            @Override
            public int compare(PluginItem p1, PluginItem p2) {
                // Favorites come first
                if (p1.isFavorited && !p2.isFavorited) {
                    return -1;
                }
                if (!p1.isFavorited && p2.isFavorited) {
                    return 1;
                }
                // Within same favorite status, sort alphabetically
                return p1.name.compareToIgnoreCase(p2.name);
            }
        });
    }
    
    private void createSearchBar() {
        searchField = new JTextField();
        searchField.setBackground(getSearchBackgroundColor());
        searchField.setForeground(getMutedTextColor());
        searchField.setBorder(new EmptyBorder(8, 10, 8, 30));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { filterPlugins(); }
            @Override
            public void removeUpdate(DocumentEvent e) { filterPlugins(); }
            @Override
            public void changedUpdate(DocumentEvent e) { filterPlugins(); }
        });
    }
    
    private void createPluginList() {
        pluginListPanel = new JPanel();
        pluginListPanel.setLayout(new BoxLayout(pluginListPanel, BoxLayout.Y_AXIS));
        pluginListPanel.setBackground(getBackgroundColor());
    }
    
    private void updatePluginDisplay() {
        pluginListPanel.removeAll();
        pluginPanels.clear();
        
        for (PluginItem plugin : allPlugins) {
            PluginItemPanel panel = new PluginItemPanel(plugin);
            pluginPanels.add(panel);
            pluginListPanel.add(panel);
        }
        
        pluginListPanel.revalidate();
        pluginListPanel.repaint();
    }
    
    private void filterPlugins() {
        String searchText = searchField.getText().toLowerCase().trim();
        
        pluginListPanel.removeAll();
        
        // Create filtered list while maintaining sort order
        List<PluginItem> filteredPlugins = allPlugins.stream()
            .filter(plugin -> plugin.name.toLowerCase().contains(searchText))
            .collect(Collectors.toList());
        
        // Find corresponding panels and add them
        for (PluginItem plugin : filteredPlugins) {
            PluginItemPanel panel = pluginPanels.stream()
                .filter(p -> p.plugin == plugin)
                .findFirst()
                .orElse(null);
            if (panel != null) {
                pluginListPanel.add(panel);
            }
        }
        
        pluginListPanel.revalidate();
        pluginListPanel.repaint();
    }
    
    /**
     * Called when a plugin's favorite status changes to re-sort the list
     */
    private void onFavoriteChanged() {
        sortPlugins();
        updatePluginDisplay();
    }
    
    /**
     * Get the GPU plugin instance
     */
    public GPUPlugin getGPUPlugin() {
        return gpuPlugin;
    }
    
    public StretchedModePlugin getStretchedModePlugin() {
        return stretchedModePlugin;
    }
    /**
     * Get the Tile Markers plugin instance
     */
    public TileMarkersPlugin getTileMarkersPlugin() {
        return tileMarkersPlugin;
    }
    
    public AmmoPlugin getAmmoPlugin() {
        return ammoPlugin;
    }
    
    /**
     * Cleanup method for when panel is destroyed
     */
    public void cleanup() {
        // Don't shutdown the shared GPU plugin - let CustomGameFrame handle it
        if (gpuPlugin != null) {
            System.out.println("PluginPanel cleanup - GPU plugin will be managed by CustomGameFrame");
        }
        
        // Cleanup Tile Markers plugin
        if (tileMarkersPlugin != null) {
            tileMarkersPlugin.cleanup();
        }
        
        // Cleanup Ammo plugin
        if (ammoPlugin != null) {
            ammoPlugin.cleanup();
        }
    }
    
    private class PluginItemPanel extends JPanel {
        private PluginItem plugin;
        private JButton starButton;
        private JButton settingsButton;
        private CustomToggle toggleButton;
        private CustomGameFrame gameFrame;
        
        public PluginItemPanel(PluginItem plugin) {
            this.plugin = plugin;
            this.gameFrame = PluginPanel.this.gameFrame;
            setLayout(new BorderLayout());
            setBackground(getBackgroundColor()); // Use frame background (darker)
            setOpaque(true);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
            setPreferredSize(new Dimension(0, 35));
            setBorder(new EmptyBorder(2, 5, 2, 5));
            
            createComponents();
            addHoverEffect();
        }
        
        // Method to update colors when theme changes
        public void updateThemeColors() {
            setBackground(getBackgroundColor());
            
            // Update child components
            for (Component comp : getComponents()) {
                if (comp instanceof JLabel) {
                    ((JLabel) comp).setForeground(getMutedTextColor());
                }
            }
            
            // Recreate icons with new colors
            updateStarIcon();
            if (settingsButton != null) {
                settingsButton.setIcon(new ImageIcon(createCogIcon(16)));
            }
            
            repaint();
        }
        
        private void createComponents() {
            // Star button (favorite) - uses drawn icons
            starButton = new JButton();
            updateStarIcon();
            starButton.setBackground(new Color(0, 0, 0, 0));
            starButton.setBorder(null);
            starButton.setFocusPainted(false);
            starButton.setContentAreaFilled(false);
            starButton.setOpaque(false);
            starButton.setPreferredSize(new Dimension(25, 25));
            starButton.addActionListener(e -> toggleFavorite());
            
            // Add hover effect for star button
            starButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    starButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    starButton.setCursor(Cursor.getDefaultCursor());
                }
            });
            
            // Plugin name
            JLabel nameLabel = new JLabel(plugin.name);
            nameLabel.setForeground(getMutedTextColor());
            nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            nameLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
            nameLabel.setOpaque(false);
            
            // Right panel for settings and toggle
            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
            rightPanel.setBackground(new Color(0, 0, 0, 0));
            rightPanel.setOpaque(false);
            
            // Settings button (only if plugin has settings)
            if (plugin.hasSettings) {
                settingsButton = new JButton();
                settingsButton.setIcon(new ImageIcon(createCogIcon(16)));
                settingsButton.setBackground(new Color(0, 0, 0, 0));
                settingsButton.setBorder(null);
                settingsButton.setFocusPainted(false);
                settingsButton.setContentAreaFilled(false);
                settingsButton.setOpaque(false);
                settingsButton.setPreferredSize(new Dimension(25, 25));
                settingsButton.addActionListener(e -> openSettings());
                
                // Add hover effect for settings button
                settingsButton.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                    
                    @Override
                    public void mouseExited(MouseEvent e) {
                        settingsButton.setCursor(Cursor.getDefaultCursor());
                    }
                });
                
                rightPanel.add(settingsButton);
            }
            
            // Toggle switch
            toggleButton = new CustomToggle(plugin.isEnabled);
            toggleButton.addActionListener(e -> togglePlugin());
            rightPanel.add(toggleButton);
            
            add(starButton, BorderLayout.WEST);
            add(nameLabel, BorderLayout.CENTER);
            add(rightPanel, BorderLayout.EAST);
        }
        
        public void updateStarIcon() {
            if (plugin.isFavorited) {
                starButton.setIcon(filledStarIcon);
            } else {
                starButton.setIcon(unfilledStarIcon);
            }
            starButton.setText("");
        }
        
        private void addHoverEffect() {
            MouseAdapter hoverListener = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(getHoverColor());
                    repaint(); // Force immediate repaint
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(getBackgroundColor());
                    repaint(); // Force immediate repaint
                }
            };
            
            addMouseListener(hoverListener);
        }

        // Helper method to update child component backgrounds
        private void updateChildBackgrounds(Color parentColor) {
            // Update right panel background but keep buttons transparent
            Component[] components = getComponents();
            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    if (panel.getLayout() instanceof FlowLayout) { // This is the right panel
                        panel.setBackground(new Color(0, 0, 0, 0)); // Keep transparent
                        // Ensure toggle and settings buttons stay transparent
                        for (Component child : panel.getComponents()) {
                            if (child instanceof JButton || child instanceof CustomToggle) {
                                child.setBackground(new Color(0, 0, 0, 0));
                            }
                        }
                    }
                }
            }
        }
        
        private void toggleFavorite() {
            plugin.isFavorited = !plugin.isFavorited;
            updateStarIcon();
            // Trigger re-sort and display update
            onFavoriteChanged();
        }
        
        private void togglePlugin() {
            plugin.isEnabled = !plugin.isEnabled;
            toggleButton.setEnabled(plugin.isEnabled);
            
            // Special handling for GPU plugin
            if (plugin.gpuPlugin != null) {
                handleGPUPluginToggle();
            } 
            // Special handling for Tile Markers plugin
            else if (plugin.tileMarkersPlugin != null) {
                handleTileMarkersPluginToggle();
            } 
            // Special handling for Ammo plugin
            else if (plugin.ammoPlugin != null) {
                handleAmmoPluginToggle();
            }
            else if (plugin.stretchedModePlugin != null) {
                handleStretchedModePluginToggle();
            }
            else {
                System.out.println("Plugin " + plugin.name + " " + (plugin.isEnabled ? "enabled" : "disabled"));
            }
        }
        
        private void handleGPUPluginToggle() {
            try {
                if (plugin.isEnabled) {
                    // ENABLE GPU RENDERING
                    if (!plugin.gpuPlugin.isInitialized()) {
                        // Initialize with game dimensions
                        int width = 765;  // Default OSRS width
                        int height = 503; // Default OSRS height
                        
                        // Try to get actual game dimensions if available
                        try {
                            Client clientInstance = gameFrame.getClientInstance();
                            if (clientInstance != null) {
                                width = Client.currentGameWidth;
                                height = Client.currentGameHeight;
                            }
                        } catch (Exception e) {
                            System.err.println("Could not get game dimensions, using defaults");
                        }
                        
                        if (!plugin.gpuPlugin.initialize(width, height)) {
                            // Failed to initialize, revert toggle
                            plugin.isEnabled = false;
                            toggleButton.setEnabled(false);
                            showPluginError("Failed to initialize GPU plugin. Check GPU compatibility.\nMake sure you have OpenGL 3.3+ support.");
                            return;
                        }
                    }
                    
                    // Enable GPU rendering
                    plugin.gpuPlugin.enable();
                    
                    // CRITICAL: Tell the client to use GPU rendering
                    Client clientInstance = gameFrame.getClientInstance();
                    if (clientInstance != null) {
                        clientInstance.useGPURendering = true;
                        Rasterizer.useGPU = true;
                        System.out.println("✓ GPU 3D Rendering enabled - Software rasterizer disabled");
                    } else {
                        System.err.println("Warning: Could not access client to enable GPU rendering");
                    }
                    
                } else {
                    // DISABLE GPU RENDERING
                    plugin.gpuPlugin.disable();
                    
                    // CRITICAL: Tell the client to use software rendering
                    Client clientInstance = gameFrame.getClientInstance();
                    if (clientInstance != null) {
                        clientInstance.useGPURendering = false;
                        Rasterizer.useGPU = false;
                        System.out.println("✓ Software rendering enabled - GPU disabled");
                    }
                }
            } catch (Exception e) {
                // Handle errors gracefully
                plugin.isEnabled = false;
                toggleButton.setEnabled(false);
                
                String errorMsg = "GPU Plugin error: " + e.getMessage();
                if (e.getMessage() != null && e.getMessage().contains("OpenGL")) {
                    errorMsg += "\n\nYour GPU may not support OpenGL 3.3+";
                }
                
                showPluginError(errorMsg);
                e.printStackTrace();
                
                // Make sure to disable GPU in client if error occurs
                try {
                    Client clientInstance = gameFrame.getClientInstance();
                    if (clientInstance != null) {
                        clientInstance.useGPURendering = false;
                        Rasterizer.useGPU = false;
                    }
                } catch (Exception ex) {
                    // Ignore
                }
            }
        }
        
        private void handleTileMarkersPluginToggle() {
            try {
                if (plugin.isEnabled) {
                    if (!plugin.tileMarkersPlugin.isInitialized()) {
                        if (!plugin.tileMarkersPlugin.initialize()) {
                            // Failed to initialize, revert toggle
                            plugin.isEnabled = false;
                            toggleButton.setEnabled(false);
                            showPluginError("Failed to initialize Tile Markers plugin.");
                            return;
                        }
                    }
                    plugin.tileMarkersPlugin.enable();
                    System.out.println("Tile Markers Plugin enabled successfully");
                } else {
                    plugin.tileMarkersPlugin.disable();
                    System.out.println("Tile Markers Plugin disabled");
                }
            } catch (Exception e) {
                // Handle errors gracefully
                plugin.isEnabled = false;
                toggleButton.setEnabled(false);
                showPluginError("Tile Markers Plugin error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        private void handleAmmoPluginToggle() {
            try {
                if (plugin.isEnabled) {
                    if (!plugin.ammoPlugin.isInitialized()) {
                        if (!plugin.ammoPlugin.initialize()) {
                            // Failed to initialize, revert toggle
                            plugin.isEnabled = false;
                            toggleButton.setEnabled(false);
                            showPluginError("Failed to initialize Ammo plugin.");
                            return;
                        }
                    }
                    plugin.ammoPlugin.enable();
                    System.out.println("Ammo Plugin enabled successfully");
                } else {
                    plugin.ammoPlugin.disable();
                    System.out.println("Ammo Plugin disabled");
                }
            } catch (Exception e) {
                // Handle errors gracefully
                plugin.isEnabled = false;
                toggleButton.setEnabled(false);
                showPluginError("Ammo Plugin error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        private void handleStretchedModePluginToggle() {
            try {
                if (plugin.isEnabled) {
                    if (!plugin.stretchedModePlugin.isInitialized()) {
                        if (!plugin.stretchedModePlugin.initialize()) {
                            plugin.isEnabled = false;
                            toggleButton.setEnabled(false);
                            showPluginError("Failed to initialize Stretched Mode plugin.");
                            return;
                        }
                    }
                    plugin.stretchedModePlugin.enable();
                    System.out.println("Stretched Mode Plugin enabled successfully");
                } else {
                    plugin.stretchedModePlugin.disable();
                    System.out.println("Stretched Mode Plugin disabled");
                }
                
                // ADD THIS LINE - notify the frame to refresh scaling
                plugin.stretchedModePlugin.notifyFrameOfChange();
                
            } catch (Exception e) {
                plugin.isEnabled = false;
                toggleButton.setEnabled(false);
                showPluginError("Stretched Mode Plugin error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        private void showPluginError(String message) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, message, "Plugin Error", JOptionPane.ERROR_MESSAGE);
            });
        }
        
        private void openSettings() {
            if (plugin.gpuPlugin != null) {
                // Show GPU plugin settings inline
                showSettings("GPU Renderer", plugin.gpuPlugin.getSettingsPanel());
            } else if (plugin.tileMarkersPlugin != null) {
                // Show Tile Markers plugin settings inline
                JPanel settingsPanel = plugin.tileMarkersPlugin.getSettingsPanel();
                if (settingsPanel instanceof com.client.plugins.tilemarkers.TileMarkersSettingsPanel) {
                    ((com.client.plugins.tilemarkers.TileMarkersSettingsPanel) settingsPanel).setPlugin(plugin.tileMarkersPlugin);
                }
                showSettings("Tile Markers", settingsPanel);
            } else if (plugin.ammoPlugin != null) {
                // Show Ammo plugin settings inline
                showSettings("Ammo", plugin.ammoPlugin.getSettingsPanel());
               
            } else if (plugin.stretchedModePlugin != null) {
                    showSettings("Stretched Mode", plugin.stretchedModePlugin.getSettingsPanel());
            } else {
                // Show placeholder settings for other plugins
                showSettings(plugin.name, createPlaceholderSettings());
            }
        }
        
        private JPanel createPlaceholderSettings() {
            JPanel placeholder = new JPanel(new BorderLayout());
            placeholder.setBackground(getBackgroundColor());
            placeholder.setBorder(new EmptyBorder(20, 20, 20, 20));
            
            JLabel label = new JLabel("Settings for " + plugin.name, SwingConstants.CENTER);
            label.setForeground(getTextColor());
            label.setFont(new Font("Segoe UI", Font.BOLD, 14));
            
            JLabel subLabel = new JLabel("This plugin doesn't have configurable settings yet.", SwingConstants.CENTER);
            subLabel.setForeground(getMutedTextColor());
            subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBackground(getBackgroundColor());
            content.add(label);
            content.add(Box.createVerticalStrut(10));
            content.add(subLabel);
            
            placeholder.add(content, BorderLayout.CENTER);
            return placeholder;
        }
    }
    
    private class CustomToggle extends JButton {
        private boolean enabled;
        
        public CustomToggle(boolean enabled) {
            this.enabled = enabled;
            setPreferredSize(new Dimension(35, 18));
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            // Use SwingUtilities.invokeLater to ensure repaint happens after other UI updates
            SwingUtilities.invokeLater(() -> {
                repaint();
                // Also repaint parent to ensure proper rendering
                if (getParent() != null) {
                    getParent().repaint();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Don't clear the background - let the parent handle it
            // Just draw the toggle directly
            
            // Use theme colors for toggle
            Color toggleOnColor = getIconColor();
            Color toggleOffColor = new Color(0x5a, 0x5a, 0x5a);
            
            // Draw track
            g2.setColor(enabled ? toggleOnColor : toggleOffColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            
            // Draw thumb
            g2.setColor(Color.WHITE);
            int thumbSize = getHeight() - 4;
            int thumbX = enabled ? getWidth() - thumbSize - 2 : 2;
            g2.fillOval(thumbX, 2, thumbSize, thumbSize);
            
            g2.dispose();
        }
    }
    
    private class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(0x5a, 0x5a, 0x5a);
            this.trackColor = getBackgroundColor();
        }
        
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }
        
        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }
        
        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }
        
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y, 
                           thumbBounds.width - 4, thumbBounds.height, 6, 6);
            g2.dispose();
        }
        
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(trackColor);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }
    }
    
    /**
     * Enhanced PluginItem class with GPU plugin and Tile Markers plugin support
     */
    private class PluginItem {
        String name;
        boolean isEnabled;
        boolean hasSettings;
        boolean isFavorited;
        GPUPlugin gpuPlugin; // Reference to GPU plugin if this is the GPU renderer
        TileMarkersPlugin tileMarkersPlugin; // Reference to Tile Markers plugin if this is the tile markers
        AmmoPlugin ammoPlugin;
        StretchedModePlugin stretchedModePlugin;
        public PluginItem(String name, boolean isFavorited, boolean hasSettings, boolean isEnabled) {
            this.name = name;
            this.isFavorited = isFavorited;
            this.hasSettings = hasSettings;
            this.isEnabled = isEnabled;
        }
        
        public void setGPUPlugin(GPUPlugin gpuPlugin) {
            this.gpuPlugin = gpuPlugin;
        }
        
        public void setTileMarkersPlugin(TileMarkersPlugin tileMarkersPlugin) {
            this.tileMarkersPlugin = tileMarkersPlugin;
        }
        
        public void setAmmoPlugin(AmmoPlugin ammoPlugin) {
            this.ammoPlugin = ammoPlugin;
        }
        public void setStretchedModePlugin(StretchedModePlugin stretchedModePlugin) {
            this.stretchedModePlugin = stretchedModePlugin;
        }
        
    }
}