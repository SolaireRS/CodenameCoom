package com.client;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;

public class ThemeManagerPanel extends JPanel {
    // Spacing constants
    private static final int SPACING_XS = 4;
    private static final int SPACING_SM = 8;
    private static final int SPACING_MD = 12;
    private static final int SPACING_LG = 16;
    private static final int SPACING_XL = 24;
    
    // Size constants
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 650;
    private static final int COLOR_PREVIEW_SIZE = 22;
    private static final int BUTTON_HEIGHT = 22;
    private static final int MAX_RECENT_COLORS = 10;
    
    // Component type identifiers
    private static final String PROP_COMPONENT_TYPE = "componentType";
    private static final String PROP_COLOR_PREVIEW = "isColorPreview";
    private static final String TYPE_SIDEBAR = "sidebar";
    private static final String TYPE_TITLEBAR = "titlebar";
    private static final String TYPE_MAINFRAME = "mainframe";
    
    // Parent frame reference
    private CustomGameFrame parentFrame;
    
    // Store references to color buttons, previews, and hex labels for updating
    private Map<String, JButton> colorButtons = new HashMap<String, JButton>();
    private Map<String, JPanel> colorPreviews = new HashMap<String, JPanel>();
    private Map<String, JLabel> hexLabels = new HashMap<String, JLabel>();
    
    // Theme colors - now using immutable theme objects
    private ThemeColors currentColors;
    
    // Icon pack and theme selection
    private String selectedIconPack = "default";
    private String currentTheme = "Default (Dark)";
    private JComboBox<String> iconPackDropdown;
    private JComboBox<String> presetDropdown;
    
    // Preferences for persistence
    private Preferences prefs = Preferences.userNodeForPackage(ThemeManagerPanel.class);
    
    // Theme application state
    private AtomicBoolean isApplyingTheme = new AtomicBoolean(false);
    
    // Available options
    private static final String[] ICON_PACKS = {"Default", "Blue", "Red", "White", "Black"};
    private static final String[] PRESET_THEMES = {"Default (Dark)", "RuneLite Blue", "Dark Purple", "Forest Green", "Light Mode"};
    
    // Preset theme definitions
    private static final Map<String, ThemeColors> PRESET_THEME_COLORS = new HashMap<String, ThemeColors>();
    static {
        PRESET_THEME_COLORS.put("Default (Dark)", new ThemeColors(
            new Color(0x2b2b2b), new Color(0x3c3c3c), new Color(0x2b2b2b),
            new Color(0xe8e6e3), new Color(0x4a90e2), new Color(0x484848),
            new Color(0x555555), "default"
        ));
        PRESET_THEME_COLORS.put("RuneLite Blue", new ThemeColors(
            new Color(0x2e2e2e), new Color(0x393939), new Color(0x2e2e2e),
            new Color(0xffffff), new Color(0x00a2e8), new Color(0x4a4a4a),
            new Color(0x00a2e8), "blue"
        ));
        PRESET_THEME_COLORS.put("Dark Purple", new ThemeColors(
            new Color(0x2d1b3d), new Color(0x3d2548), new Color(0x2d1b3d),
            new Color(0xe6d7ff), new Color(0x9966cc), new Color(0x4a3356),
            new Color(0x9966cc), "white"
        ));
        PRESET_THEME_COLORS.put("Forest Green", new ThemeColors(
            new Color(0x1a2e1a), new Color(0x233823), new Color(0x1a2e1a),
            new Color(0xd4ffd4), new Color(0x4caf50), new Color(0x2e4a2e),
            new Color(0x4caf50), "white"
        ));
        PRESET_THEME_COLORS.put("Light Mode", new ThemeColors(
            new Color(0xf5f5f5), new Color(0xe8e8e8), new Color(0xf5f5f5),
            new Color(0x333333), new Color(0x1976d2), new Color(0xdcdcdc),
            new Color(0xcccccc), "black"
        ));
    }
    
    public ThemeManagerPanel(CustomGameFrame parentFrame) {
        this.parentFrame = parentFrame;
        loadStoredTheme();
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(currentColors.sidebarBackground);
        setBorder(BorderFactory.createEmptyBorder(SPACING_MD, SPACING_MD, SPACING_MD, SPACING_MD));
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setMinimumSize(new Dimension(PANEL_WIDTH, 450));
        setMaximumSize(new Dimension(PANEL_WIDTH, 850));
        putClientProperty(PROP_COMPONENT_TYPE, TYPE_SIDEBAR);
        
        // Title
        JLabel titleLabel = createTitleLabel();
        
        // Main content
        JPanel mainPanel = createMainPanel();
        JScrollPane scrollPane = createScrollPane(mainPanel);
        
        add(titleLabel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        
        // Initialize previews after UI is built
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                updateAllColorPreviews();
                revalidate();
                repaint();
            }
        });
    }
    
    private JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel("Theme Manager");
        titleLabel.setForeground(currentColors.textColor);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setPreferredSize(new Dimension(250, 35));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(SPACING_SM, 0, SPACING_MD, 0));
        return titleLabel;
    }
    
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(currentColors.sidebarBackground);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(SPACING_MD, SPACING_SM, 0, SPACING_SM));
        mainPanel.setPreferredSize(new Dimension(260, 580));
        mainPanel.setMaximumSize(new Dimension(260, 580));
        mainPanel.putClientProperty(PROP_COMPONENT_TYPE, TYPE_SIDEBAR);
        
        // Colors section
        mainPanel.add(createSectionHeader("Colors", "🎨"));
        mainPanel.add(Box.createVerticalStrut(SPACING_SM));
        mainPanel.add(createColorsPanel());
        mainPanel.add(Box.createVerticalStrut(SPACING_LG));
        mainPanel.add(createSectionDivider());
        mainPanel.add(Box.createVerticalStrut(SPACING_LG));
        
        // Icon Pack section
        mainPanel.add(createSectionHeader("Icon Pack", "📦"));
        mainPanel.add(Box.createVerticalStrut(SPACING_SM));
        JPanel iconRow = createIconPackRow();
        iconRow.setMaximumSize(new Dimension(250, 28));
        mainPanel.add(iconRow);
        mainPanel.add(Box.createVerticalStrut(SPACING_LG));
        mainPanel.add(createSectionDivider());
        mainPanel.add(Box.createVerticalStrut(SPACING_LG));
        
        // Preset Themes section
        mainPanel.add(createSectionHeader("Preset Themes", "⚙"));
        mainPanel.add(Box.createVerticalStrut(SPACING_SM));
        JPanel presetRow = createPresetRow();
        presetRow.setMaximumSize(new Dimension(250, 28));
        mainPanel.add(presetRow);
        mainPanel.add(Box.createVerticalStrut(SPACING_LG));
        mainPanel.add(createSectionDivider());
        mainPanel.add(Box.createVerticalStrut(SPACING_LG));
        
        // Actions section
        mainPanel.add(createSectionHeader("Actions", "⚡"));
        mainPanel.add(Box.createVerticalStrut(SPACING_SM));
        JPanel actionButtons = createActionButtons();
        actionButtons.setMaximumSize(new Dimension(250, 50));
        mainPanel.add(actionButtons);
        
        mainPanel.add(Box.createVerticalGlue());
        
        return mainPanel;
    }
    
    private JScrollPane createScrollPane(JPanel content) {
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setBackground(currentColors.sidebarBackground);
        containerPanel.add(content, BorderLayout.NORTH);
        containerPanel.putClientProperty(PROP_COMPONENT_TYPE, TYPE_SIDEBAR);
        
        JScrollPane scrollPane = new JScrollPane(containerPanel);
        scrollPane.setBackground(currentColors.sidebarBackground);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(270, 620));
        scrollPane.setMaximumSize(new Dimension(PANEL_WIDTH, Integer.MAX_VALUE));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        scrollPane.getVerticalScrollBar().setBackground(currentColors.sidebarBackground);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        
        return scrollPane;
    }
    
    private JPanel createSectionHeader(String text, String icon) {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerPanel.setBackground(currentColors.sidebarBackground);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        headerPanel.putClientProperty(PROP_COMPONENT_TYPE, TYPE_SIDEBAR);
        
        JLabel iconLabel = new JLabel(icon + " ");
        iconLabel.setForeground(currentColors.accentColor);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        
        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(currentColors.accentColor);
        textLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        headerPanel.add(iconLabel);
        headerPanel.add(textLabel);
        
        return headerPanel;
    }
    
    private JPanel createSectionDivider() {
        JPanel divider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient divider
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(currentColors.borderColor.getRGB() & 0x00FFFFFF | 0x00000000),
                    getWidth() / 2, 0, new Color(currentColors.borderColor.getRGB() & 0x00FFFFFF | 0x60000000),
                    true
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        divider.setPreferredSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setOpaque(false);
        return divider;
    }
    
    private JPanel createColorsPanel() {
        JPanel colorsPanel = new JPanel();
        colorsPanel.setLayout(new GridLayout(7, 1, 0, SPACING_XS));
        colorsPanel.setBackground(currentColors.sidebarBackground);
        colorsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentColors.borderColor, 1),
            BorderFactory.createEmptyBorder(SPACING_SM, SPACING_SM, SPACING_SM, SPACING_SM)
        ));
        colorsPanel.setPreferredSize(new Dimension(250, 200));
        colorsPanel.setMinimumSize(new Dimension(250, 200));
        colorsPanel.setMaximumSize(new Dimension(250, 200));
        colorsPanel.putClientProperty(PROP_COMPONENT_TYPE, TYPE_SIDEBAR);
        
        colorsPanel.add(createColorRow("Frame Color", "frameBackground"));
        colorsPanel.add(createColorRow("Sidebar Color", "sidebarBackground"));
        colorsPanel.add(createColorRow("Title Bar", "titleBarColor"));
        colorsPanel.add(createColorRow("Text Color", "textColor"));
        colorsPanel.add(createColorRow("Accent Color", "accentColor"));
        colorsPanel.add(createColorRow("Hover Color", "hoverColor"));
        colorsPanel.add(createColorRow("Border Color", "borderColor"));
        
        return colorsPanel;
    }
    
    private JPanel createColorRow(String name, String key) {
        JPanel row = new JPanel(new BorderLayout(SPACING_SM, 0));
        row.setBackground(currentColors.sidebarBackground);
        row.setPreferredSize(new Dimension(230, 26));
        row.putClientProperty(PROP_COMPONENT_TYPE, TYPE_SIDEBAR);
        
        JLabel label = new JLabel(name);
        label.setForeground(currentColors.textColor);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        // Color preview and controls panel
        JPanel colorControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, SPACING_XS, 0));
        colorControlPanel.setBackground(currentColors.sidebarBackground);
        colorControlPanel.putClientProperty(PROP_COMPONENT_TYPE, TYPE_SIDEBAR);
        
        // Hex label
        Color currentColor = getColorByKey(key);
        JLabel hexLabel = new JLabel(colorToHex(currentColor));
        hexLabel.setFont(new Font("Consolas", Font.PLAIN, 9));
        hexLabel.setForeground(new Color(0xaaaaaa));
        hexLabels.put(key, hexLabel);
        
        // Color preview with checkered background
        JPanel colorPreview = createColorPreview(currentColor);
        colorPreviews.put(key, colorPreview);
        
        // Select button
        JButton button = createColorButton();
        colorButtons.put(key, button);
        
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectColor(name, key, colorPreview, hexLabel);
            }
        });
        
        colorControlPanel.add(hexLabel);
        colorControlPanel.add(colorPreview);
        colorControlPanel.add(button);
        
        row.add(label, BorderLayout.WEST);
        row.add(colorControlPanel, BorderLayout.EAST);
        
        return row;
    }
    
    private JPanel createColorPreview(Color color) {
        JPanel colorPreview = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Checkered background
                int checkerSize = 4;
                for (int y = 0; y < getHeight(); y += checkerSize) {
                    for (int x = 0; x < getWidth(); x += checkerSize) {
                        g2.setColor(((x / checkerSize + y / checkerSize) % 2 == 0) 
                            ? new Color(0xcccccc) : new Color(0x999999));
                        g2.fillRect(x, y, checkerSize, checkerSize);
                    }
                }
                
                // Color on top
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                
                g2.dispose();
            }
        };
        
        colorPreview.setPreferredSize(new Dimension(COLOR_PREVIEW_SIZE, COLOR_PREVIEW_SIZE));
        colorPreview.setBackground(color);
        colorPreview.setBorder(BorderFactory.createLineBorder(new Color(0x555555), 1));
        colorPreview.setOpaque(false);
        colorPreview.putClientProperty(PROP_COLOR_PREVIEW, Boolean.TRUE);
        
        return colorPreview;
    }
    
    private JButton createColorButton() {
        final JButton button = new JButton("Select");
        button.setPreferredSize(new Dimension(55, BUTTON_HEIGHT));
        button.setBackground(new Color(0xf0f0f0));
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xcccccc), 1),
            BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(new Color(0xe0e0e0));
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(new Color(0xf0f0f0));
            }
        });
        
        return button;
    }
    
    private void selectColor(String name, String key, JPanel preview, JLabel hexLabel) {
        Color originalColor = getColorByKey(key);
        Color newColor = showColorChooser("Choose " + name, originalColor, preview, hexLabel);
        
        if (newColor != null && !newColor.equals(originalColor)) {
            setColorByKey(key, newColor);
            currentTheme = "Custom";
            updatePresetDropdown();
            applyTheme();
            saveTheme();
        }
    }
    
    private JPanel createIconPackRow() {
        JPanel row = new JPanel(new BorderLayout(SPACING_MD, 0));
        row.setBackground(currentColors.sidebarBackground);
        row.setPreferredSize(new Dimension(230, 28));
        row.setMaximumSize(new Dimension(230, 28));
        row.putClientProperty(PROP_COMPONENT_TYPE, TYPE_SIDEBAR);
        
        JLabel label = new JLabel("Icon Pack");
        label.setForeground(currentColors.textColor);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        iconPackDropdown = createStyledDropdown(ICON_PACKS);
        iconPackDropdown.setSelectedItem(getIconPackDisplay(selectedIconPack));
        
        iconPackDropdown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selected = (String) iconPackDropdown.getSelectedItem();
                if (selected != null) {
                    String internalName = getIconPackInternal(selected);
                    if (!internalName.equals(selectedIconPack)) {
                        selectedIconPack = internalName;
                        refreshIconPack();
                        saveTheme();
                    }
                }
            }
        });
        
        row.add(label, BorderLayout.WEST);
        row.add(iconPackDropdown, BorderLayout.EAST);
        
        return row;
    }
    
    private JPanel createPresetRow() {
        JPanel row = new JPanel(new BorderLayout(SPACING_MD, 0));
        row.setBackground(currentColors.sidebarBackground);
        row.setPreferredSize(new Dimension(230, 28));
        row.setMaximumSize(new Dimension(230, 28));
        row.putClientProperty(PROP_COMPONENT_TYPE, TYPE_SIDEBAR);
        
        JLabel label = new JLabel("Presets");
        label.setForeground(currentColors.textColor);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        presetDropdown = createStyledDropdown(PRESET_THEMES);
        presetDropdown.setSelectedItem(currentTheme);
        
        presetDropdown.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selected = (String) presetDropdown.getSelectedItem();
                if (selected != null && !selected.equals(currentTheme)) {
                    applyPresetTheme(selected);
                }
            }
        });
        
        row.add(label, BorderLayout.WEST);
        row.add(presetDropdown, BorderLayout.EAST);
        
        return row;
    }
    
    private JComboBox<String> createStyledDropdown(String[] items) {
        JComboBox<String> dropdown = new JComboBox<String>(items);
        dropdown.setPreferredSize(new Dimension(110, BUTTON_HEIGHT));
        dropdown.setMaximumSize(new Dimension(110, BUTTON_HEIGHT));
        dropdown.setBackground(new Color(0xf8f8f8));
        dropdown.setForeground(Color.BLACK);
        dropdown.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        dropdown.setBorder(BorderFactory.createLineBorder(new Color(0xcccccc), 1));
        dropdown.setFocusable(true);
        
        dropdown.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setFont(new Font("Segoe UI", Font.PLAIN, 10));
                setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                if (isSelected) {
                    setBackground(currentColors.accentColor);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(new Color(0xf8f8f8));
                    setForeground(Color.BLACK);
                }
                return this;
            }
        });
        
        return dropdown;
    }
    
    private JPanel createActionButtons() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, SPACING_SM));
        panel.setBackground(currentColors.sidebarBackground);
        panel.setPreferredSize(new Dimension(230, 50));
        panel.putClientProperty(PROP_COMPONENT_TYPE, TYPE_SIDEBAR);
        
        JButton resetButton = createStyledButton("Reset to Default");
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                applyPresetTheme("Default (Dark)");
                showFeedback("Theme reset to default", true);
            }
        });
        
        JButton exportButton = createStyledButton("Export Theme");
        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (exportTheme()) {
                    showFeedback("Theme exported successfully", true);
                } else {
                    showFeedback("Export cancelled", false);
                }
            }
        });
        
        panel.add(resetButton);
        panel.add(exportButton);
        
        return panel;
    }
    
    private JButton createStyledButton(final String text) {
        final JButton button = new JButton(text) {
            private float hoverAlpha = 0f;
            private javax.swing.Timer animTimer;
            
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Base color
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                
                // Hover overlay
                if (hoverAlpha > 0) {
                    g2.setColor(new Color(
                        currentColors.accentColor.getRed(),
                        currentColors.accentColor.getGreen(),
                        currentColors.accentColor.getBlue(),
                        (int)(hoverAlpha * 255)
                    ));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                }
                
                g2.dispose();
                super.paintComponent(g);
            }
            
            private void startFade(final boolean fadeIn) {
                if (animTimer != null && animTimer.isRunning()) {
                    animTimer.stop();
                }
                
                animTimer = new javax.swing.Timer(16, new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        hoverAlpha += fadeIn ? 0.15f : -0.15f;
                        hoverAlpha = Math.max(0f, Math.min(1f, hoverAlpha));
                        repaint();
                        
                        if ((fadeIn && hoverAlpha >= 1f) || (!fadeIn && hoverAlpha <= 0f)) {
                            animTimer.stop();
                        }
                    }
                });
                animTimer.start();
            }
            
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent evt) {
                        startFade(true);
                        setForeground(Color.WHITE);
                    }
                    public void mouseExited(MouseEvent evt) {
                        startFade(false);
                        setForeground(Color.BLACK);
                    }
                });
            }
        };
        
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBackground(new Color(0xf0f0f0));
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Segoe UI", Font.BOLD, 10));
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(Integer.MAX_VALUE, BUTTON_HEIGHT));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xcccccc), 1),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        return button;
    }
    
    private Color showColorChooser(String title, Color initialColor, final JPanel livePreview, final JLabel liveHexLabel) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setLayout(new BorderLayout());
        
        final JColorChooser chooser = new JColorChooser(initialColor);
        AbstractColorChooserPanel[] panels = chooser.getChooserPanels();
        for (AbstractColorChooserPanel panel : panels) {
            if (!panel.getDisplayName().equals("HSB") && !panel.getDisplayName().equals("RGB")) {
                chooser.removeChooserPanel(panel);
            }
        }
        
        // Live preview update
        chooser.getSelectionModel().addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                Color tempColor = chooser.getColor();
                livePreview.setBackground(tempColor);
                livePreview.repaint();
                liveHexLabel.setText(colorToHex(tempColor));
            }
        });
        
        // Recent colors panel
        JPanel recentColorsPanel = createRecentColorsPanel(chooser);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        
        final Color[] selectedColor = new Color[1];
        
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedColor[0] = chooser.getColor();
                saveRecentColor(selectedColor[0]);
                dialog.dispose();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Revert preview on cancel
                livePreview.setBackground(initialColor);
                livePreview.repaint();
                liveHexLabel.setText(colorToHex(initialColor));
                dialog.dispose();
            }
        });
        
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        
        // Assemble dialog
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(chooser, BorderLayout.CENTER);
        topPanel.add(recentColorsPanel, BorderLayout.SOUTH);
        
        dialog.add(topPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        
        return selectedColor[0];
    }
    
    private JPanel createRecentColorsPanel(final JColorChooser chooser) {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 2));
        panel.setBorder(BorderFactory.createTitledBorder("Recent Colors"));
        panel.setPreferredSize(new Dimension(400, 60));
        
        String recentColorsStr = prefs.get("recentColors", "");
        if (!recentColorsStr.isEmpty()) {
            String[] colorStrs = recentColorsStr.split(",");
            for (String colorStr : colorStrs) {
                try {
                    int rgb = Integer.parseInt(colorStr);
                    final Color color = new Color(rgb);
                    
                    JButton colorButton = new JButton() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setColor(color);
                            g2.fillRect(0, 0, getWidth(), getHeight());
                            g2.dispose();
                        }
                    };
                    colorButton.setPreferredSize(new Dimension(20, 20));
                    colorButton.setContentAreaFilled(false);
                    colorButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
                    colorButton.setToolTipText(String.format("RGB(%d, %d, %d)", 
                        color.getRed(), color.getGreen(), color.getBlue()));
                    
                    colorButton.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            chooser.setColor(color);
                        }
                    });
                    
                    panel.add(colorButton);
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }
        }
        
        return panel;
    }
    
    private void saveRecentColor(Color color) {
        try {
            String recentColorsStr = prefs.get("recentColors", "");
            List<String> recentColors = new ArrayList<String>();
            
            if (!recentColorsStr.isEmpty()) {
                String[] colors = recentColorsStr.split(",");
                for (String colorStr : colors) {
                    recentColors.add(colorStr);
                }
            }
            
            String newColorStr = String.valueOf(color.getRGB());
            recentColors.remove(newColorStr);
            recentColors.add(0, newColorStr);
            
            if (recentColors.size() > MAX_RECENT_COLORS) {
                recentColors = recentColors.subList(0, MAX_RECENT_COLORS);
            }
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < recentColors.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(recentColors.get(i));
            }
            prefs.put("recentColors", sb.toString());
        } catch (Exception e) {
            // Fail silently
        }
    }
    
    private void showFeedback(String message, boolean success) {
        final JLabel feedback = new JLabel(message);
        feedback.setForeground(success ? new Color(0x4caf50) : new Color(0xf44336));
        feedback.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        feedback.setHorizontalAlignment(SwingConstants.CENTER);
        feedback.setBorder(BorderFactory.createEmptyBorder(SPACING_SM, 0, SPACING_SM, 0));
        
        add(feedback, BorderLayout.SOUTH);
        revalidate();
        repaint();
        
        javax.swing.Timer timer = new javax.swing.Timer(2000, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                remove(feedback);
                revalidate();
                repaint();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    
    private void applyPresetTheme(String themeName) {
        ThemeColors preset = PRESET_THEME_COLORS.get(themeName);
        if (preset != null) {
            currentColors = preset;
            selectedIconPack = preset.iconPack;
            currentTheme = themeName;
            
            updatePresetDropdown();
            updateIconPackDropdown();
            updateAllColorPreviews();
            refreshIconPack();
            applyTheme();
            saveTheme();
        }
    }
    
    private void refreshIconPack() {
        if (parentFrame != null) {
            parentFrame.refreshIconPack(selectedIconPack.equals("default") ? null : selectedIconPack);
        }
    }
    
    public void applyTheme() {
        if (!isApplyingTheme.compareAndSet(false, true)) {
            return; // Already applying, prevent race condition
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    updateThemeManagerPanel();
                    
                    if (parentFrame != null) {
                        updateComponentTree(parentFrame);
                        
                        PluginPanel pluginPanel = parentFrame.getPluginPanel();
                        if (pluginPanel != null) {
                            pluginPanel.updateThemeColors();
                        }
                        
                        parentFrame.revalidate();
                        parentFrame.repaint();
                    }
                    
                    // Protect color previews
                    updateAllColorPreviews();
                } finally {
                    isApplyingTheme.set(false);
                }
            }
        });
    }
    
    private void updateThemeManagerPanel() {
        setBackground(currentColors.sidebarBackground);
        revalidate();
        repaint();
    }
    
    private void updateComponentTree(Container container) {
        if (container == null) return;
        
        updateComponentColors(container);
        
        Component[] components = container.getComponents();
        if (components != null) {
            for (Component component : components) {
                if (component != null) {
                    updateComponentColors(component);
                    if (component instanceof Container) {
                        updateComponentTree((Container) component);
                    }
                }
            }
        }
    }
    
    private void updateComponentColors(Component component) {
        if (component == null) return;
        
        // Skip color preview panels - check if it's a JComponent first
        if (component instanceof JComponent) {
            JComponent jcomp = (JComponent) component;
            if (Boolean.TRUE.equals(jcomp.getClientProperty(PROP_COLOR_PREVIEW))) {
                return;
            }
            
            String componentType = (String) jcomp.getClientProperty(PROP_COMPONENT_TYPE);
            
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                if (TYPE_SIDEBAR.equals(componentType)) {
                    panel.setBackground(currentColors.sidebarBackground);
                } else if (TYPE_TITLEBAR.equals(componentType)) {
                    panel.setBackground(currentColors.titleBarColor);
                } else if (TYPE_MAINFRAME.equals(componentType)) {
                    panel.setBackground(currentColors.frameBackground);
                }
            }
        }
        
        if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            label.setForeground(currentColors.textColor);
        }
        
        if (component instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) component;
            scroll.setBackground(currentColors.sidebarBackground);
            scroll.getViewport().setBackground(currentColors.sidebarBackground);
        }
    }
    
    private void updateAllColorPreviews() {
        for (Map.Entry<String, JPanel> entry : colorPreviews.entrySet()) {
            String key = entry.getKey();
            JPanel preview = entry.getValue();
            Color color = getColorByKey(key);
            preview.setBackground(color);
            preview.repaint();
            
            // Update hex label
            JLabel hexLabel = hexLabels.get(key);
            if (hexLabel != null) {
                hexLabel.setText(colorToHex(color));
            }
        }
    }
    
    private void updatePresetDropdown() {
        if (presetDropdown != null) {
            presetDropdown.setSelectedItem(currentTheme);
        }
    }
    
    private void updateIconPackDropdown() {
        if (iconPackDropdown != null) {
            iconPackDropdown.setSelectedItem(getIconPackDisplay(selectedIconPack));
        }
    }
    
    private Color getColorByKey(String key) {
        if ("frameBackground".equals(key)) return currentColors.frameBackground;
        if ("sidebarBackground".equals(key)) return currentColors.sidebarBackground;
        if ("titleBarColor".equals(key)) return currentColors.titleBarColor;
        if ("textColor".equals(key)) return currentColors.textColor;
        if ("accentColor".equals(key)) return currentColors.accentColor;
        if ("hoverColor".equals(key)) return currentColors.hoverColor;
        if ("borderColor".equals(key)) return currentColors.borderColor;
        return Color.BLACK;
    }
    
    private void setColorByKey(String key, Color color) {
        if ("frameBackground".equals(key)) {
            currentColors = new ThemeColors(color, currentColors.sidebarBackground, currentColors.titleBarColor,
                currentColors.textColor, currentColors.accentColor, currentColors.hoverColor, currentColors.borderColor, currentColors.iconPack);
        } else if ("sidebarBackground".equals(key)) {
            currentColors = new ThemeColors(currentColors.frameBackground, color, currentColors.titleBarColor,
                currentColors.textColor, currentColors.accentColor, currentColors.hoverColor, currentColors.borderColor, currentColors.iconPack);
        } else if ("titleBarColor".equals(key)) {
            currentColors = new ThemeColors(currentColors.frameBackground, currentColors.sidebarBackground, color,
                currentColors.textColor, currentColors.accentColor, currentColors.hoverColor, currentColors.borderColor, currentColors.iconPack);
        } else if ("textColor".equals(key)) {
            currentColors = new ThemeColors(currentColors.frameBackground, currentColors.sidebarBackground, currentColors.titleBarColor,
                color, currentColors.accentColor, currentColors.hoverColor, currentColors.borderColor, currentColors.iconPack);
        } else if ("accentColor".equals(key)) {
            currentColors = new ThemeColors(currentColors.frameBackground, currentColors.sidebarBackground, currentColors.titleBarColor,
                currentColors.textColor, color, currentColors.hoverColor, currentColors.borderColor, currentColors.iconPack);
        } else if ("hoverColor".equals(key)) {
            currentColors = new ThemeColors(currentColors.frameBackground, currentColors.sidebarBackground, currentColors.titleBarColor,
                currentColors.textColor, currentColors.accentColor, color, currentColors.borderColor, currentColors.iconPack);
        } else if ("borderColor".equals(key)) {
            currentColors = new ThemeColors(currentColors.frameBackground, currentColors.sidebarBackground, currentColors.titleBarColor,
                currentColors.textColor, currentColors.accentColor, currentColors.hoverColor, color, currentColors.iconPack);
        }
    }
    
    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
    
    private String getIconPackInternal(String displayName) {
        if ("Default".equals(displayName)) return "default";
        if ("Blue".equals(displayName)) return "blue";
        if ("Red".equals(displayName)) return "red";
        if ("White".equals(displayName)) return "white";
        if ("Black".equals(displayName)) return "black";
        return "default";
    }
    
    private String getIconPackDisplay(String internalName) {
        if ("default".equals(internalName)) return "Default";
        if ("blue".equals(internalName)) return "Blue";
        if ("red".equals(internalName)) return "Red";
        if ("white".equals(internalName)) return "White";
        if ("black".equals(internalName)) return "Black";
        return "Default";
    }
    
    private void saveTheme() {
        prefs.putInt("frameBackground", currentColors.frameBackground.getRGB());
        prefs.putInt("sidebarBackground", currentColors.sidebarBackground.getRGB());
        prefs.putInt("titleBarColor", currentColors.titleBarColor.getRGB());
        prefs.putInt("textColor", currentColors.textColor.getRGB());
        prefs.putInt("accentColor", currentColors.accentColor.getRGB());
        prefs.putInt("hoverColor", currentColors.hoverColor.getRGB());
        prefs.putInt("borderColor", currentColors.borderColor.getRGB());
        prefs.put("selectedIconPack", selectedIconPack);
        prefs.put("currentTheme", currentTheme);
    }
    
    private void loadStoredTheme() {
        Color frameBackground = new Color(prefs.getInt("frameBackground", 0x2b2b2b));
        Color sidebarBackground = new Color(prefs.getInt("sidebarBackground", 0x3c3c3c));
        Color titleBarColor = new Color(prefs.getInt("titleBarColor", 0x2b2b2b));
        Color textColor = new Color(prefs.getInt("textColor", 0xe8e6e3));
        Color accentColor = new Color(prefs.getInt("accentColor", 0x4a90e2));
        Color hoverColor = new Color(prefs.getInt("hoverColor", 0x484848));
        Color borderColor = new Color(prefs.getInt("borderColor", 0x555555));
        selectedIconPack = prefs.get("selectedIconPack", "default");
        currentTheme = prefs.get("currentTheme", "Default (Dark)");
        
        currentColors = new ThemeColors(frameBackground, sidebarBackground, titleBarColor,
            textColor, accentColor, hoverColor, borderColor, selectedIconPack);
    }
    
    private boolean exportTheme() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Theme files (*.theme)", "theme"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".theme")) {
                    file = new File(file.getPath() + ".theme");
                }
                
                Properties props = new Properties();
                props.setProperty("frameBackground", String.valueOf(currentColors.frameBackground.getRGB()));
                props.setProperty("sidebarBackground", String.valueOf(currentColors.sidebarBackground.getRGB()));
                props.setProperty("titleBarColor", String.valueOf(currentColors.titleBarColor.getRGB()));
                props.setProperty("textColor", String.valueOf(currentColors.textColor.getRGB()));
                props.setProperty("accentColor", String.valueOf(currentColors.accentColor.getRGB()));
                props.setProperty("hoverColor", String.valueOf(currentColors.hoverColor.getRGB()));
                props.setProperty("borderColor", String.valueOf(currentColors.borderColor.getRGB()));
                props.setProperty("iconPack", selectedIconPack);
                
                FileWriter writer = new FileWriter(file);
                try {
                    props.store(writer, "Custom Theme");
                    return true;
                } finally {
                    writer.close();
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to export theme: " + e.getMessage(), 
                    "Export Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        return false;
    }
    
    // Custom scroll bar UI
    private class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(0x5a, 0x5a, 0x5a);
            this.trackColor = currentColors.sidebarBackground;
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
    
    // Immutable theme data object
    private static class ThemeColors {
        final Color frameBackground;
        final Color sidebarBackground;
        final Color titleBarColor;
        final Color textColor;
        final Color accentColor;
        final Color hoverColor;
        final Color borderColor;
        final String iconPack;
        
        ThemeColors(Color frameBackground, Color sidebarBackground, Color titleBarColor,
                   Color textColor, Color accentColor, Color hoverColor, 
                   Color borderColor, String iconPack) {
            this.frameBackground = frameBackground;
            this.sidebarBackground = sidebarBackground;
            this.titleBarColor = titleBarColor;
            this.textColor = textColor;
            this.accentColor = accentColor;
            this.hoverColor = hoverColor;
            this.borderColor = borderColor;
            this.iconPack = iconPack;
        }
    }
    
    // Public getter methods
    public Color getFrameBackground() { return currentColors.frameBackground; }
    public Color getSidebarBackground() { return currentColors.sidebarBackground; }
    public Color getTitleBarColor() { return currentColors.titleBarColor; }
    public Color getTextColor() { return currentColors.textColor; }
    public Color getAccentColor() { return currentColors.accentColor; }
    public Color getHoverColor() { return currentColors.hoverColor; }
    public Color getBorderColor() { return currentColors.borderColor; }
    public String getSelectedIconPack() { return selectedIconPack; }
    public String getCurrentTheme() { return currentTheme; }
}