package com.client;

import com.client.features.gameframe.ScreenMode;
import com.client.plugins.gpu.GPUPlugin;
import com.client.plugins.stretchedmode.StretchedModePlugin;
import com.client.sign.Signlink;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class CustomGameFrame extends JFrame {
    
    // Platform detection
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_MAC = OS_NAME.contains("mac");
    private static final boolean IS_LINUX = OS_NAME.contains("nix") || OS_NAME.contains("nux") || OS_NAME.contains("aix");
    
    // Layout constants
    private static final int SPACING_TINY = 2;
    private static final int SPACING_SMALL = 4;
    private static final int SPACING_MEDIUM = 8;
    private static final int SPACING_LARGE = 12;
    private static final int SPACING_XLARGE = 16;
    
    private static final int GAME_WIDTH = 765;
    private static final int GAME_HEIGHT = 503;
    private static final int SIDEBAR_COLLAPSED_WIDTH = 25;
    private static final int SIDEBAR_EXPANDED_WIDTH = 350;
    private static final int TITLE_BAR_HEIGHT = 25;
    private static final int CORNER_RADIUS = IS_MAC ? 0 : 12;
    
    // Typography
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 10);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.PLAIN, 10);
    
    // Colors
    private Color FRAME_BACKGROUND = new Color(0x2b2b2b);
    private Color SIDEBAR_BACKGROUND = new Color(0x3c3c3c);
    private Color TITLE_BAR_COLOR = new Color(0x2b2b2b);
    private Color TEXT_COLOR = new Color(0xe8e6e3);
    private Color ACCENT_COLOR = new Color(0x4a90e2);
    private Color HOVER_COLOR = new Color(0x484848);
    private Color BORDER_COLOR = new Color(0x555555);
    
    // Core components
    private JPanel gamePanel;
    private Component gameClient;
    private JPanel sidebar;
    private JPanel sidebarContainer;
    private JPanel buttonPanel;
    private JPanel mainContentPanel;
    private JPanel transitionOverlay;
    
    // Sidebar state
    public boolean sidebarVisible = false;
    private CardLayout sidebarCards;
    private String currentSidebarKey = "empty";
    private Map<String, JButton> buttonMap = new HashMap<>();
    
    // Panel references
    private PluginPanel pluginPanel;
    private ExpTrackerPanel expTrackerPanel;
    private LootTrackerPanel lootTrackerPanel;
    private ThemeManagerPanel themeManagerPanel;
    
    // Mode management
    private volatile ScreenMode currentScreenMode = ScreenMode.FIXED;
    private final AtomicBoolean isModeChanging = new AtomicBoolean(false);
    private final ReentrantLock modeChangeLock = new ReentrantLock();
    private volatile String lastKnownMode = "";
    
    // Resize handling
    private final AtomicBoolean isResizing = new AtomicBoolean(false);
    private final AtomicBoolean isDragging = new AtomicBoolean(false);
    private ResizeHandler resizeHandler;
    
    // Visual state
    private boolean undecoratedMode = false;
    private final AtomicBoolean isTransitioning = new AtomicBoolean(false);
    private Timer transitionTimer;
    
    // Performance cache - THREAD SAFE
    private volatile BufferedImage backBuffer;
    private volatile BufferedImage stableBackBuffer;
    public final Object paintLock = new Object();
    private Shape cachedWindowShape;
    private int cachedWidth = -1;
    private int cachedHeight = -1;
    private static final Map<String, ImageIcon> iconCache = new HashMap<>();
    
    // Lifecycle
    private final AtomicBoolean isDisposed = new AtomicBoolean(false);
    private GPUPlugin gpuPlugin;
    
    // Window drag handling
    private Point dragStartPoint;
    private Point dragStartLocation;
    
    public CustomGameFrame(String title) {
        super(title);
        
        System.out.println("Initializing CustomGameFrame on " + OS_NAME);
        
        initializeGPUPlugin();
        setupMinimalLookAndFeel();
        setupTooltipStyling();
        initializeMinimalFrame();
        createMinimalLayout();
        createTransitionOverlay();
        setupKeyboardShortcuts();
        
        if (!IS_MAC) {
            initializeResizeHandler();
        }
        
        addWindowStateListener(e -> handleWindowStateChange());
        addComponentListener(createResizeListener());
        addWindowListener(createWindowListener());
        
        SwingUtilities.invokeLater(() -> {
            SwingUtilities.invokeLater(() -> {
                if (themeManagerPanel != null) {
                    themeManagerPanel.applyTheme();
                }
                initializeCustomCursors();
            });
        });
    }
    
    private void initializeGPUPlugin() {
        try {
            System.out.println("=== Initializing GPU Plugin ===");
            this.gpuPlugin = new GPUPlugin(this);
            
            if (gpuPlugin.initialize(GAME_WIDTH, GAME_HEIGHT)) {
                System.out.println("GPU Plugin initialized successfully");
                gpuPlugin.enable();
                System.out.println("GPU Plugin enabled");
            } else {
                System.err.println("GPU Plugin initialization failed");
                this.gpuPlugin = null;
            }
        } catch (Exception e) {
            System.err.println("Failed to create GPU Plugin: " + e.getMessage());
            e.printStackTrace();
            this.gpuPlugin = null;
        }
    }
    
    private void setupMinimalLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setupTooltipStyling() {
        UIManager.put("ToolTip.background", SIDEBAR_BACKGROUND);
        UIManager.put("ToolTip.foreground", TEXT_COLOR);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR, 1),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        UIManager.put("ToolTip.font", FONT_BODY);
        
        ToolTipManager manager = ToolTipManager.sharedInstance();
        manager.setInitialDelay(300);
        manager.setDismissDelay(8000);
        manager.setReshowDelay(100);
    }
    
    private void initializeMinimalFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        int sidebarWidth = sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : SIDEBAR_COLLAPSED_WIDTH;
        int initialWidth = GAME_WIDTH + sidebarWidth;
        int initialHeight = TITLE_BAR_HEIGHT + GAME_HEIGHT;
        
        setSize(initialWidth, initialHeight);
        setLocationRelativeTo(null);
        setResizable(true);
        
        getContentPane().setBackground(Color.BLACK);
        getRootPane().setBackground(Color.BLACK);
        
        getRootPane().setBorder(null);

        try {
            setIconImage(createMinimalIcon());
        } catch (Exception e) {
            // Ignore
        }
        
        SwingUtilities.invokeLater(() -> {
            if (isDisplayable()) {
                setWindowShape();
            }
        });
    }
    
    private void initializeCustomCursors() {
        applyHandCursorToButtons(this);
    }
    
    private void applyHandCursorToButtons(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof AbstractButton) {
                comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            if (comp instanceof Container) {
                applyHandCursorToButtons((Container) comp);
            }
        }
    }
    
    private void initializeResizeHandler() {
        if (IS_MAC) {
            System.out.println("Skipping custom resize handler on macOS");
            return;
        }
        
        resizeHandler = new ResizeHandler(this);
        addMouseListener(resizeHandler);
        addMouseMotionListener(resizeHandler);
    }
    
    private void setupKeyboardShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK), "close");
        actionMap.put("close", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(CustomGameFrame.this, WindowEvent.WINDOW_CLOSING));
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "fullscreen");
        actionMap.put("fullscreen", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                toggleFullscreen();
            }
        });
        
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "minimize");
        actionMap.put("minimize", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                setState(JFrame.ICONIFIED);
            }
        });
    }
    
    private void createMinimalLayout() {
        setLayout(new BorderLayout());
        createMinimalTopBar();

        mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new CustomLayout());
        mainContentPanel.setBackground(Color.BLACK);

        gamePanel = new JPanel(new CenteringBorderLayout());
        gamePanel.setBackground(Color.BLACK);
        mainContentPanel.add(gamePanel, "game");

        add(mainContentPanel, BorderLayout.CENTER);

        createSidebar();
    }

    private void createMinimalTopBar() {
        Color topColor = TITLE_BAR_COLOR;
        Color bottomColor = new Color(
            Math.max(0, TITLE_BAR_COLOR.getRed() - 8),
            Math.max(0, TITLE_BAR_COLOR.getGreen() - 8),
            Math.max(0, TITLE_BAR_COLOR.getBlue() - 8)
        );
        
        JPanel topBar = createGradientPanel(topColor, bottomColor);
        topBar.setName("titleBar");
        topBar.setLayout(new BorderLayout());
        topBar.setPreferredSize(new Dimension(0, TITLE_BAR_HEIGHT));
        topBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 0, 0, 80))
        ));

        JLabel titleLabel = new JLabel("  " + getTitle());
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(FONT_TITLE);

        addDragListeners(topBar);
        addDragListeners(titleLabel);

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, SPACING_TINY, SPACING_TINY));
        controlsPanel.setOpaque(false);

        JButton pluginsBtn = createMinimalButton("⚙");
        pluginsBtn.setToolTipText("Settings");
        pluginsBtn.addActionListener(e -> openQuickSettings());

        JButton screenshotBtn = createMinimalButton("📷");
        screenshotBtn.setToolTipText("Screenshot");  
        screenshotBtn.addActionListener(e -> takeScreenshot());

        JButton minimizeBtn = createMinimalButton("−");
        minimizeBtn.setToolTipText("Minimize (ESC)");
        minimizeBtn.addActionListener(e -> setState(JFrame.ICONIFIED));

        JButton maximizeBtn = createMinimalButton("□");
        maximizeBtn.setToolTipText("Maximize (F11)");
        maximizeBtn.addActionListener(e -> {
            if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                setExtendedState(JFrame.NORMAL);
            } else {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });

        JButton closeBtn = createMinimalButton("✕");
        closeBtn.setToolTipText("Close (Alt+F4)");
        closeBtn.setForeground(TEXT_COLOR);
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                closeBtn.setBackground(new Color(232, 17, 35));
                closeBtn.setContentAreaFilled(true);
            }
            public void mouseExited(MouseEvent e) {
                closeBtn.setBackground(TITLE_BAR_COLOR);
                closeBtn.setContentAreaFilled(false);
            }
        });
        closeBtn.addActionListener(e -> {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        });

        controlsPanel.add(pluginsBtn);
        controlsPanel.add(screenshotBtn);
        controlsPanel.add(minimizeBtn);
        controlsPanel.add(maximizeBtn);
        controlsPanel.add(closeBtn);

        topBar.add(titleLabel, BorderLayout.WEST);
        topBar.add(controlsPanel, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);
    }
    
    private JButton createMinimalButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    Color pressColor = HOVER_COLOR.darker();
                    g2.setColor(pressColor);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else if (getModel().isRollover()) {
                    Color hoverColor = (themeManagerPanel != null) ? 
                        themeManagerPanel.getHoverColor() : HOVER_COLOR;
                    g2.setColor(hoverColor);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                
                super.paintComponent(g);
                g2.dispose();
            }
        };
        
        button.setName("titleBarButton");
        button.setBackground(TITLE_BAR_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setFont(FONT_BUTTON);
        button.setPreferredSize(new Dimension(28, 21));

        return button;
    }
    
    private void createSidebar() {
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setName("sidebarButtonPanel");
        buttonPanel.setBackground(SIDEBAR_BACKGROUND);
        buttonPanel.setPreferredSize(new Dimension(SIDEBAR_COLLAPSED_WIDTH, 0));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR),
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0, 0, 0, 80))
        ));
        
        buttonPanel.add(Box.createVerticalStrut(SPACING_MEDIUM));
        
        sidebarCards = new CardLayout();
        sidebar = new JPanel(sidebarCards);
        sidebar.setName("sidebarPanel");
        sidebar.setBackground(SIDEBAR_BACKGROUND);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 1, 0, 0),
            BorderFactory.createEmptyBorder(SPACING_LARGE, SPACING_LARGE, SPACING_LARGE, SPACING_LARGE)
        ));

        pluginPanel = new PluginPanel(this);
        expTrackerPanel = new ExpTrackerPanel();
        NotesPanel notesPanel = new NotesPanel();
        lootTrackerPanel = new LootTrackerPanel();
        themeManagerPanel = new ThemeManagerPanel(this);

        sidebar.add(new JPanel(), "empty");
        sidebar.add(pluginPanel, "plugins");
        sidebar.add(expTrackerPanel, "exp");
        sidebar.add(notesPanel, "notes");
        sidebar.add(lootTrackerPanel, "loot");
        sidebar.add(themeManagerPanel, "themes");
        
        sidebarContainer = new JPanel(new BorderLayout());
        sidebarContainer.setName("sidebarContainer");
        sidebarContainer.setBackground(SIDEBAR_BACKGROUND);
        sidebarContainer.add(buttonPanel, BorderLayout.WEST);

        addSidebarButton("plugins.png", "Plugins", "plugins");
        addSidebarButton("stats.png", "EXP Tracker", "exp");
        addSidebarButton("notes.png", "Notes", "notes");
        addSidebarButton("loottracker.png", "Loot Tracker", "loot");
        addSidebarButton("themes.png", "Themes", "themes");
        addSidebarButton("worldmap.png", "World Map", "worldmap");
        
        mainContentPanel.add(sidebarContainer, "sidebar");
        
        sidebarCards.show(sidebar, "empty");
        sidebarVisible = false;
    }
    
    private void addSidebarButton(String iconName, String tooltip, String key) {
        JButton button = createSidebarIconButton(iconName, tooltip);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(e -> toggleSidebarPanel(key));
        
        buttonPanel.add(button);
        buttonPanel.add(Box.createVerticalStrut(SPACING_SMALL));
        
        buttonMap.put(key, button);
    }
    
    private JButton createSidebarIconButton(String iconName, String tooltip) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (Boolean.TRUE.equals(getClientProperty("selected"))) {
                    g2.setColor(new Color(HOVER_COLOR.getRed(), 
                                         HOVER_COLOR.getGreen(), 
                                         HOVER_COLOR.getBlue(), 180));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(HOVER_COLOR.getRed(), 
                                         HOVER_COLOR.getGreen(), 
                                         HOVER_COLOR.getBlue(), 80));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else if (getModel().isPressed()) {
                    g2.setColor(new Color(HOVER_COLOR.getRed(), 
                                         HOVER_COLOR.getGreen(), 
                                         HOVER_COLOR.getBlue(), 200));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                
                super.paintComponent(g);
                
                if (Boolean.TRUE.equals(getClientProperty("selected"))) {
                    Color accent = (themeManagerPanel != null) ? 
                        themeManagerPanel.getAccentColor() : ACCENT_COLOR;
                    
                    GradientPaint gradient = new GradientPaint(
                        0, 0, accent,
                        0, getHeight(), new Color(accent.getRed(), accent.getGreen(), 
                                                 accent.getBlue(), 100)
                    );
                    g2.setPaint(gradient);
                    g2.fillRoundRect(0, 2, 3, getHeight() - 4, 3, 3);
                }
                
                g2.dispose();
            }
        };
        
        button.setName("sidebarIconButton");
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(SIDEBAR_COLLAPSED_WIDTH, SIDEBAR_COLLAPSED_WIDTH + 2));
        button.setMaximumSize(new Dimension(SIDEBAR_COLLAPSED_WIDTH, SIDEBAR_COLLAPSED_WIDTH + 2));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setMargin(new Insets(SPACING_TINY, SPACING_TINY, SPACING_TINY, SPACING_TINY));

        String iconPath = "/icons/" + iconName;
        ImageIcon icon = loadCachedIcon(iconPath);
        
        if (icon != null) {
            Image scaled = getHighQualityScaledImage(icon.getImage(), 12, 12);
            button.setIcon(new ImageIcon(scaled));
        } else {
            BufferedImage fallbackIcon = createFallbackIcon(12, 12);
            button.setIcon(new ImageIcon(fallbackIcon));
        }

        return button;
    }
    
    private void createTransitionOverlay() {
        transitionOverlay = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                g2.setColor(new Color(0, 0, 0, 230));
                g2.fillRect(0, 0, getWidth(), getHeight());
                
                int boxWidth = 200;
                int boxHeight = 60;
                int x = (getWidth() - boxWidth) / 2;
                int y = (getHeight() - boxHeight) / 2;
                
                GradientPaint bgGradient = new GradientPaint(
                    x, y, new Color(50, 50, 50),
                    x, y + boxHeight, new Color(40, 40, 40)
                );
                g2.setPaint(bgGradient);
                g2.fillRoundRect(x, y, boxWidth, boxHeight, 8, 8);
                
                Color accentColor = (themeManagerPanel != null) ? 
                    themeManagerPanel.getAccentColor() : ACCENT_COLOR;
                g2.setColor(accentColor);
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(x, y, boxWidth, boxHeight, 8, 8);
                
                g2.setColor(TEXT_COLOR);
                g2.setFont(FONT_BODY);
                String text = "Switching Mode...";
                FontMetrics fm = g2.getFontMetrics();
                int textX = x + (boxWidth - fm.stringWidth(text)) / 2;
                int textY = y + (boxHeight + fm.getAscent()) / 2 - 2;
                g2.drawString(text, textX, textY);
                
                g2.dispose();
            }
        };
        
        transitionOverlay.setOpaque(false);
        transitionOverlay.setVisible(false);
    }
    
    private ComponentAdapter createResizeListener() {
        return new ComponentAdapter() {
            private Dimension lastSize = new Dimension(-1, -1);
            private Timer debounceTimer;

            private ComponentAdapter createResizeListener() {
                return new ComponentAdapter() {
                    private Dimension lastSize = new Dimension(-1, -1);
                    private Timer debounceTimer;

                    @Override
                    public void componentResized(ComponentEvent e) {
                        if (currentScreenMode != ScreenMode.RESIZABLE || 
                            getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                            return;
                        }
                        
                        Dimension newSize = getSize();
                        if (newSize.equals(lastSize)) {
                            return;
                        }
                        lastSize = new Dimension(newSize);
                        
                        if (debounceTimer != null && debounceTimer.isRunning()) {
                            debounceTimer.stop();
                        }
                        
                        // Force immediate layout update
                        mainContentPanel.invalidate();
                        mainContentPanel.validate();
                        mainContentPanel.repaint();
                        
                        debounceTimer = new Timer(50, evt -> {
                            if (!isResizing.get() && !isDragging.get()) {
                                Client clientInstance = getClientInstance();
                                if (clientInstance != null && clientInstance.loggedIn) {
                                    // Update client dimensions based on actual gamePanel size
                                    int gamePanelWidth = gamePanel.getWidth();
                                    int gamePanelHeight = gamePanel.getHeight();
                                    
                                    Client.currentGameWidth = gamePanelWidth;
                                    Client.currentGameHeight = gamePanelHeight;
                                    
                                    System.out.println("DEBUG: Final resize - " + gamePanelWidth + "x" + gamePanelHeight);
                                }
                                
                                SwingUtilities.invokeLater(() -> {
                                    gamePanel.repaint();
                                    mainContentPanel.repaint();
                                    repaint();
                                });
                            }
                        });
                        debounceTimer.setRepeats(false);
                        debounceTimer.start();
                    }
                };
            }

            private void updateGameClientSize() {
                if (gameClient == null || currentScreenMode != ScreenMode.RESIZABLE) return;
                
                Client clientInstance = getClientInstance();
                if (clientInstance == null || !clientInstance.loggedIn) {
                    System.out.println("DEBUG: Skipping resize - not logged in");
                    return;
                }
                
                // Get the gamePanel's actual size - this is already correctly sized by CustomLayout
                int gamePanelWidth = gamePanel.getWidth();
                int gamePanelHeight = gamePanel.getHeight();
                
                Client.currentGameWidth = gamePanelWidth;
                Client.currentGameHeight = gamePanelHeight;
                
                if (gamePanel.getLayout() instanceof CenteringBorderLayout) {
                    CenteringBorderLayout layout = (CenteringBorderLayout) gamePanel.getLayout();
                    layout.resetCache();
                }
                
                gameClient.setSize(gamePanelWidth, gamePanelHeight);
                gameClient.setPreferredSize(new Dimension(gamePanelWidth, gamePanelHeight));
                gameClient.setMinimumSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
                gameClient.setBounds(0, 0, gamePanelWidth, gamePanelHeight);
                
                gameClient.invalidate();
                gamePanel.invalidate();
                gamePanel.revalidate();
                
                System.out.println("DEBUG: Resize - Game client updated to: " + gamePanelWidth + "x" + gamePanelHeight);
            }
        };
    }

    private WindowAdapter createWindowListener() {
        return new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
            
            @Override
            public void windowStateChanged(WindowEvent e) {
                if (currentScreenMode == ScreenMode.RESIZABLE) {
                    SwingUtilities.invokeLater(() -> {
                        if (gamePanel.getLayout() instanceof CenteringBorderLayout) {
                            CenteringBorderLayout layout = (CenteringBorderLayout) gamePanel.getLayout();
                            layout.resetCache();
                        }
                        
                        int gamePanelWidth = gamePanel.getWidth();
                        int gamePanelHeight = gamePanel.getHeight();
                        
                        Client.currentGameWidth = gamePanelWidth;
                        Client.currentGameHeight = gamePanelHeight;
                        
                        if (gameClient != null) {
                            gameClient.setSize(gamePanelWidth, gamePanelHeight);
                            gameClient.setPreferredSize(new Dimension(gamePanelWidth, gamePanelHeight));
                            gameClient.setBounds(0, 0, gamePanelWidth, gamePanelHeight);
                        }
                        
                        gamePanel.revalidate();
                        repaint();
                    });
                }
            }
        };
    }
    
    private void addDragListeners(Component component) {
        component.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                    dragStartPoint = e.getPoint();
                    dragStartLocation = getLocation();
                    isDragging.set(true);
                    
                    if (!IS_MAC) {
                        try {
                            com.sun.awt.AWTUtilities.setWindowShape(CustomGameFrame.this, null);
                        } catch (Exception ex) {
                            // Ignore
                        }
                    }
                    
                    ClientWindow clientWindow = findClientWindowComponent(gameClient);
                    if (clientWindow != null) {
                        try {
                            if (hasMethod(clientWindow.getClass(), "setDragging", boolean.class)) {
                                clientWindow.getClass().getMethod("setDragging", boolean.class)
                                    .invoke(clientWindow, true);
                            }
                        } catch (Exception ex) {
                            // Ignore
                        }
                    }
                }
            }
            
            public void mouseReleased(MouseEvent e) {
                isDragging.set(false);
                dragStartPoint = null;
                dragStartLocation = null;
                
                if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                    snapToScreenEdge();
                    
                    if (!IS_MAC) {
                        SwingUtilities.invokeLater(() -> {
                            setWindowShape();
                            repaint();
                        });
                    }
                }
                
                ClientWindow clientWindow = findClientWindowComponent(gameClient);
                if (clientWindow != null) {
                    try {
                        if (hasMethod(clientWindow.getClass(), "setDragging", boolean.class)) {
                            clientWindow.getClass().getMethod("setDragging", boolean.class)
                                .invoke(clientWindow, false);
                        }
                    } catch (Exception ex) {
                        // Ignore
                    }
                }
            }
        });
        
        component.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (isDragging.get() && dragStartPoint != null && dragStartLocation != null &&
                    getExtendedState() != JFrame.MAXIMIZED_BOTH && !isResizing.get()) {
                    
                    Point currentScreenPoint = e.getLocationOnScreen();
                    int newX = currentScreenPoint.x - dragStartPoint.x;
                    int newY = currentScreenPoint.y - dragStartPoint.y;
                    
                    setLocation(newX, newY);
                }
            }
        });
    }
    
    private void snapToScreenEdge() {
        Rectangle screenBounds = getGraphicsConfiguration().getBounds();
        Point location = getLocation();
        int snapDistance = 20;
        
        if (Math.abs(location.x - screenBounds.x) < snapDistance) {
            location.x = screenBounds.x;
        }
        if (Math.abs(location.y - screenBounds.y) < snapDistance) {
            location.y = screenBounds.y;
        }
        if (Math.abs(screenBounds.width - (location.x - screenBounds.x + getWidth())) < snapDistance) {
            location.x = screenBounds.x + screenBounds.width - getWidth();
        }
        
        setLocation(location);
    }
    
    @Override
    public void paint(Graphics g) {
        if (isDisposed.get() || g == null) return;
        
        if (isDragging.get()) {
            super.paint(g);
            return;
        }
        
        if (isResizing.get()) {
            super.paint(g);
            return;
        }
        
        if (IS_MAC || getExtendedState() == JFrame.MAXIMIZED_BOTH) {
            super.paint(g);
            return;
        }
        
        int width = getWidth();
        int height = getHeight();
        
        if (width <= 0 || height <= 0) {
            super.paint(g);
            return;
        }
        
        synchronized (paintLock) {
            if (backBuffer == null || backBuffer.getWidth() != width || backBuffer.getHeight() != height) {
                if (backBuffer != null) {
                    backBuffer.flush();
                }
                try {
                    backBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                } catch (OutOfMemoryError e) {
                    super.paint(g);
                    return;
                }
            }
            
            Graphics2D g2 = backBuffer.createGraphics();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
                
                g2.setColor(FRAME_BACKGROUND);
                g2.fillRect(0, 0, width, height);
                
                Shape windowShape = getWindowShape();
                g2.setClip(windowShape);
                super.paint(g2);
                g2.setClip(null);
            } finally {
                g2.dispose();
            }
            
            g.drawImage(backBuffer, 0, 0, null);
        }
    }

    @Override
    public void update(Graphics g) {
        paint(g);
    }
    
    private Shape getWindowShape() {
        int width = getWidth();
        int height = getHeight();
        
        if (cachedWindowShape == null || cachedWidth != width || cachedHeight != height) {
            if (CORNER_RADIUS > 0) {
                cachedWindowShape = new RoundRectangle2D.Float(0, 0, width, height, 
                    CORNER_RADIUS, CORNER_RADIUS);
            } else {
                cachedWindowShape = new Rectangle(0, 0, width, height);
            }
            cachedWidth = width;
            cachedHeight = height;
        }
        
        return cachedWindowShape;
    }
    
    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean locationChanged = (getX() != x || getY() != y);
        boolean sizeChanged = (getWidth() != width || getHeight() != height);
        
        if (!locationChanged && !sizeChanged) {
            return;
        }
        
        if (isDragging.get() && locationChanged && !sizeChanged) {
            super.setBounds(x, y, width, height);
            return;
        }
        
        if (isResizing.get()) {
            super.setBounds(x, y, width, height);
            return;
        }
        
        super.setBounds(x, y, width, height);
        
        if (sizeChanged) {
            synchronized (paintLock) {
                cachedWindowShape = null;
                if (backBuffer != null && (backBuffer.getWidth() != width || backBuffer.getHeight() != height)) {
                    backBuffer.flush();
                    backBuffer = null;
                }
            }
            
            SwingUtilities.invokeLater(this::setWindowShape);
        }
    }
    
    private void setWindowShape() {
        if (IS_MAC) {
            return;
        }
        
        setWindowShapePlatformSafe(getWindowShape());
    }
    
    private void setWindowShapePlatformSafe(Shape shape) {
        if (IS_MAC) {
            return;
        }
        
        try {
            if (isUndecorated() && getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                com.sun.awt.AWTUtilities.setWindowShape(this, shape);
            } else if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                com.sun.awt.AWTUtilities.setWindowShape(this, null);
            }
        } catch (Exception e) {
            // Silently fall back
        }
    }
    
    private void handleWindowStateChange() {
        SwingUtilities.invokeLater(() -> {
            resetCursor();
            if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                setWindowShapePlatformSafe(null);
            } else {
                setWindowShape();
            }
        });
    }
    
    private JPanel createGradientPanel(Color top, Color bottom) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, top,
                    0, getHeight(), bottom
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
    }
    
    private Image createMinimalIcon() {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = icon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.setColor(new Color(76, 117, 163));
        g2d.fillRect(0, 0, 16, 16);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(2, 2, 12, 12);
        g2d.setColor(new Color(76, 117, 163));
        g2d.fillRect(4, 4, 8, 8);
        g2d.dispose();
        return icon;
    }
    
    private ImageIcon loadCachedIcon(String path) {
        return iconCache.computeIfAbsent(path, p -> {
            try {
                URL url = getClass().getResource(p);
                if (url != null) {
                    return new ImageIcon(ImageIO.read(url));
                }
            } catch (Exception e) {
                System.err.println("Failed to load icon: " + p);
            }
            return null;
        });
    }
    
    private BufferedImage createFallbackIcon(int width, int height) {
        BufferedImage icon = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        Color iconColor = (themeManagerPanel != null) ? 
            themeManagerPanel.getAccentColor() : ACCENT_COLOR;
        g2.setColor(iconColor);
        g2.fillRoundRect(2, 2, width-4, height-4, 2, 2);
        
        g2.dispose();
        return icon;
    }
    
    private Image getHighQualityScaledImage(Image src, int width, int height) {
        BufferedImage dest = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dest.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(src, 0, 0, width, height, null);
        g2.dispose();
        return dest;
    }
    
    private boolean hasMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            clazz.getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
    
    public void resetCursor() {
        SwingUtilities.invokeLater(() -> setCursor(Cursor.getDefaultCursor()));
    }
    
    public void setGameClient(Component client) {
        System.out.println("DEBUG: setGameClient called with new ClientWindow architecture");
        
        if (gameClient != null) {
            gamePanel.remove(gameClient);
        }
        this.gameClient = client;
        
        ClientWindow clientWindow = findClientWindowComponent(client);
        if (clientWindow != null) {
            try {
                ClientWindow.frame = this;
                System.out.println("Set CustomGameFrame as ClientWindow.frame");
            } catch (Exception e) {
                System.out.println("Could not set frame reference: " + e.getMessage());
            }
            
            try {
                if (hasMethod(clientWindow.getClass(), "setGameFrame", CustomGameFrame.class)) {
                    clientWindow.getClass().getMethod("setGameFrame", CustomGameFrame.class)
                        .invoke(clientWindow, this);
                    System.out.println("GameFrame reference set on ClientWindow");
                }
            } catch (Exception e) {
                System.out.println("setGameFrame method not available: " + e.getMessage());
            }
        }
        
        gamePanel.add(client, BorderLayout.CENTER);
        gamePanel.revalidate();
        gamePanel.repaint();
        
        SwingUtilities.invokeLater(() -> {
            try {
                Thread.sleep(1000);
                Rasterizer.initializeGPUPlugin(this);
                System.out.println("GPU Plugin initialized after client setup");
            } catch (Exception e) {
                System.err.println("Failed to initialize GPU Plugin after client setup: " + e.getMessage());
            }
        });
        
        System.out.println("DEBUG: ClientWindow added to custom frame");
    }
    
    private ClientWindow findClientWindowComponent(Component component) {
        if (component instanceof ClientWindow) {
            return (ClientWindow) component;
        }
        
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                ClientWindow found = findClientWindowComponent(child);
                if (found != null) {
                    return found;
                }
            }
        }
        
        return null;
    }
    
    public Client getClientInstance() {
        return findClientComponent(gameClient);
    }
    
    private Client findClientComponent(Component component) {
        if (component instanceof Client) {
            return (Client) component;
        }
        
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                Client found = findClientComponent(child);
                if (found != null) {
                    return found;
                }
            }
        }
        
        return null;
    }
    
    public void refreshStretchedMode() {
        System.out.println("DEBUG: refreshStretchedMode called for new client");
        mainContentPanel.revalidate();
    }
    
    public Object getUserSettings() {
        try {
            Client client = getClientInstance();
            if (client != null) {
                return Client.getUserSettings();
            }
        } catch (Exception e) {
            System.err.println("Could not access user settings: " + e.getMessage());
        }
        return null;
    }
    
    public void setSetting(String settingName, boolean value) {
        try {
            Object settings = getUserSettings();
            if (settings != null) {
                String methodName = "set" + settingName.substring(0, 1).toUpperCase() + 
                    settingName.substring(1);
                java.lang.reflect.Method method = settings.getClass().getMethod(methodName, boolean.class);
                method.invoke(settings, value);
                System.out.println("Setting " + settingName + " set to: " + value);
            } else {
                System.err.println("Could not access settings to set " + settingName);
            }
        } catch (Exception e) {
            System.err.println("Error setting " + settingName + ": " + e.getMessage());
        }
    }

    public boolean getSetting(String settingName) {
        try {
            Object settings = getUserSettings();
            if (settings != null) {
                String methodName = "is" + settingName.substring(0, 1).toUpperCase() + 
                    settingName.substring(1);
                java.lang.reflect.Method method = settings.getClass().getMethod(methodName);
                return (Boolean) method.invoke(settings);
            }
        } catch (Exception e) {
            System.err.println("Error getting " + settingName + ": " + e.getMessage());
        }
        return false;
    }
    
    private void toggleSidebarPanel(String key) {
        if ("worldmap".equals(key)) {
            openWorldMapPopout();
            return;
        }
        
        if (!sidebarVisible) {
            sidebarContainer.add(sidebar, BorderLayout.CENTER);
            sidebarCards.show(sidebar, key);
            sidebarVisible = true;
            currentSidebarKey = key;
            
            // Resize frame to accommodate expanded sidebar
            if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                Dimension currentSize = getSize();
                int widthIncrease = SIDEBAR_EXPANDED_WIDTH - SIDEBAR_COLLAPSED_WIDTH;
                setSize(currentSize.width + widthIncrease, currentSize.height);
            }
            
        } else if (currentSidebarKey.equals(key)) {
            sidebarContainer.remove(sidebar);
            sidebarVisible = false;
            currentSidebarKey = "empty";
            
            // Resize frame back when closing sidebar
            if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                Dimension currentSize = getSize();
                int widthDecrease = SIDEBAR_EXPANDED_WIDTH - SIDEBAR_COLLAPSED_WIDTH;
                setSize(currentSize.width - widthDecrease, currentSize.height);
            }
            
        } else {
            // Just switch panels, no resize needed
            sidebarCards.show(sidebar, key);
            currentSidebarKey = key;
        }

        // Force immediate layout refresh
        mainContentPanel.invalidate();
        mainContentPanel.validate();
        mainContentPanel.repaint();
        updateButtonSelection();
        
        System.out.println("DEBUG: Sidebar toggled - visible=" + sidebarVisible + ", key=" + currentSidebarKey);
    }
    
    private void updateButtonSelection() {
        for (JButton btn : buttonMap.values()) {
            btn.putClientProperty("selected", false);
            btn.repaint();
        }
        
        if (sidebarVisible && !currentSidebarKey.equals("empty")) {
            JButton selected = buttonMap.get(currentSidebarKey);
            if (selected != null) {
                selected.putClientProperty("selected", true);
                selected.repaint();
            }
        }
    }
    
    private void openWorldMapPopout() {
        Client client = getClientInstance();
        if (client == null) {
            showStatusBar("Client is still loading. Please wait a moment.", 3000);
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            try {
                WorldMapFrame mapFrame = new WorldMapFrame(client);
                mapFrame.setVisible(true);
            } catch (Exception e) {
                System.err.println("Failed to open world map: " + e.getMessage());
                e.printStackTrace();
                showStatusBar("Failed to open world map", 3000);
            }
        });
    }
    
    public void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        if (sidebarVisible) {
            sidebarContainer.add(sidebar, BorderLayout.CENTER);
            Dimension currentSize = getSize();
            setSize(currentSize.width + (SIDEBAR_EXPANDED_WIDTH - SIDEBAR_COLLAPSED_WIDTH), 
                currentSize.height);
        } else {
            sidebarContainer.remove(sidebar);
            Dimension currentSize = getSize();
            setSize(currentSize.width - (SIDEBAR_EXPANDED_WIDTH - SIDEBAR_COLLAPSED_WIDTH), 
                currentSize.height);
        }
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }
    
    public void refreshTheme() {
        if (themeManagerPanel != null) {
            themeManagerPanel.applyTheme();
        }
    }
    
    public void refreshIconPack(String iconPack) {
        System.out.println("Refreshing all icons to pack: " + iconPack);
        SwingUtilities.invokeLater(() -> {
            refreshSidebarIcons(iconPack);
            CustomClientLauncher.refreshTitleBarIcons(iconPack);
        });
    }

    private void refreshSidebarIcons(String iconPack) {
        refreshIconsRecursively(this, iconPack);
        repaint();
    }

    private void refreshIconsRecursively(Container container, String iconPack) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                
                if ("sidebarIconButton".equals(button.getName())) {
                    String tooltip = button.getToolTipText();
                    refreshButtonIcon(button, tooltip, iconPack);
                }
            }
            
            if (component instanceof Container) {
                refreshIconsRecursively((Container) component, iconPack);
            }
        }
    }

    private void refreshButtonIcon(JButton button, String tooltip, String iconPack) {
        String iconName = getIconNameFromTooltip(tooltip);
        if (iconName != null) {
            String iconPath = (iconPack == null) ? 
                "/icons/" + iconName : 
                "/icons/" + iconPack + "/" + iconName;
            
            ImageIcon icon = loadCachedIcon(iconPath);
            if (icon == null && iconPack != null) {
                String fallbackPath = "/icons/" + iconName;
                icon = loadCachedIcon(fallbackPath);
            }
            
            if (icon != null) {
                Image scaled = getHighQualityScaledImage(icon.getImage(), 12, 12);
                button.setIcon(new ImageIcon(scaled));
            } else {
                BufferedImage fallback = createFallbackIcon(12, 12);
                button.setIcon(new ImageIcon(fallback));
            }
        }
    }

    private String getIconNameFromTooltip(String tooltip) {
        if (tooltip == null) return null;
        switch (tooltip) {
            case "Plugins": return "plugins.png";
            case "EXP Tracker": return "stats.png";
            case "Notes": return "notes.png";
            case "Loot Tracker": return "loottracker.png";
            case "Themes": return "themes.png";
            default: return null;
        }
    }
    
    public void handleGameModeChange(Object mode, int totalClientWidth, int totalClientHeight) {
        if (!modeChangeLock.tryLock()) {
            System.out.println("Mode change already in progress, skipping duplicate request");
            return;
        }
        
        try {
            if (isModeChanging.get()) {
                return;
            }
            
            String modeStr = mode.toString();
            
            if (modeStr.equals(lastKnownMode)) {
                return;
            }
            
            isModeChanging.set(true);
            
            try {
                showTransitionOverlay();
                pauseClientRendering();
                
                lastKnownMode = modeStr;
                updateScreenMode(mode, modeStr);
                
                SwingUtilities.invokeLater(() -> {
                    Timer setupTimer = new Timer(100, e -> {
                        try {
                            handleModeChangeInternal(modeStr, totalClientWidth, totalClientHeight);
                        } finally {
                            resumeClientRendering();
                            isModeChanging.set(false);
                        }
                    });
                    setupTimer.setRepeats(false);
                    setupTimer.start();
                });
                
            } catch (Exception e) {
                System.err.println("Error in mode change setup: " + e.getMessage());
                resumeClientRendering();
                isModeChanging.set(false);
                hideTransitionOverlay(500);
            }
        } finally {
            modeChangeLock.unlock();
        }
    }

    private void pauseClientRendering() {
        try {
            Client client = getClientInstance();
            if (client != null) {
                if (hasMethod(client.getClass(), "setRenderingPaused", boolean.class)) {
                    client.getClass().getMethod("setRenderingPaused", boolean.class)
                        .invoke(client, true);
                } else {
                    if (gameClient != null) {
                        gameClient.setVisible(false);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not pause client rendering: " + e.getMessage());
            if (gameClient != null) {
                gameClient.setVisible(false);
            }
        }
    }

    private void resumeClientRendering() {
        try {
            Client client = getClientInstance();
            if (client != null) {
                if (hasMethod(client.getClass(), "setRenderingPaused", boolean.class)) {
                    client.getClass().getMethod("setRenderingPaused", boolean.class)
                        .invoke(client, false);
                }
            }
            
            if (gameClient != null) {
                gameClient.setVisible(true);
            }
            
        } catch (Exception e) {
            System.err.println("Could not resume client rendering: " + e.getMessage());
            if (gameClient != null) {
                gameClient.setVisible(true);
            }
        }
    }

    private void updateScreenMode(Object mode, String modeStr) {
        try {
            if (mode instanceof ScreenMode) {
                currentScreenMode = (ScreenMode) mode;
            } else {
                if (modeStr.contains("RESIZABLE")) {
                    currentScreenMode = ScreenMode.RESIZABLE;
                } else if (modeStr.contains("FULLSCREEN")) {
                    currentScreenMode = ScreenMode.FULLSCREEN;
                } else {
                    currentScreenMode = ScreenMode.FIXED;
                }
            }
            System.out.println("DEBUG: Screen mode updated to: " + currentScreenMode);
        } catch (Exception e) {
            System.err.println("Error parsing screen mode: " + e.getMessage());
            currentScreenMode = ScreenMode.FIXED;
        }
    }

    private void handleModeChangeInternal(String modeStr, int totalClientWidth, int totalClientHeight) {
        System.out.println("DEBUG: handleModeChangeInternal - Mode: " + modeStr + 
                          ", Size: " + totalClientWidth + "x" + totalClientHeight);
        
        try {
            Client clientInstance = getClientInstance();
            boolean loggedIn = (clientInstance != null && clientInstance.loggedIn);
            
            if (!loggedIn) {
                System.out.println("DEBUG: Not logged in yet, keeping fixed layout");
                configureFrameForMode(modeStr);
                hideTransitionOverlay(300);
                return;
            }
            
            final int targetWidth = totalClientWidth;
            final int targetHeight = totalClientHeight;
            
            Client.currentGameWidth = targetWidth;
            Client.currentGameHeight = targetHeight;
            System.out.println("DEBUG: Set client dimensions to: " + targetWidth + "x" + targetHeight);
            
            setAllBackgroundsBlack();
            
            if (gamePanel.getLayout() instanceof CenteringBorderLayout) {
                CenteringBorderLayout layout = (CenteringBorderLayout) gamePanel.getLayout();
                layout.resetCache();
            }
            
            configureFrameForMode(modeStr);
            
            SwingUtilities.invokeLater(() -> {
                if (gameClient != null && currentScreenMode == ScreenMode.RESIZABLE) {
                    int gamePanelWidth = gamePanel.getWidth();
                    int gamePanelHeight = gamePanel.getHeight();
                    
                    gameClient.setSize(gamePanelWidth, gamePanelHeight);
                    gameClient.setPreferredSize(new Dimension(gamePanelWidth, gamePanelHeight));
                    gameClient.setBounds(0, 0, gamePanelWidth, gamePanelHeight);
                    gameClient.invalidate();
                    
                    System.out.println("DEBUG: Final game client size: " + gamePanelWidth + "x" + gamePanelHeight);
                }
                
                // CRITICAL: Force complete layout refresh including sidebar
                mainContentPanel.invalidate();
                sidebarContainer.invalidate();
                gamePanel.invalidate();
                
                mainContentPanel.validate();
                sidebarContainer.validate();
                gamePanel.validate();
                
                mainContentPanel.repaint();
                sidebarContainer.repaint();
                gamePanel.repaint();
                
                SwingUtilities.invokeLater(() -> {
                    hideTransitionOverlay(300);
                    
                    // Force one more layout pass to ensure everything is positioned correctly
                    SwingUtilities.invokeLater(() -> {
                        mainContentPanel.revalidate();
                        sidebarContainer.revalidate();
                    });
                });
            });
            
        } catch (Exception ex) {
            System.err.println("Error in mode change internal: " + ex.getMessage());
            ex.printStackTrace();
            hideTransitionOverlay(500);
        }
    }
    
    private void configureFrameForMode(String modeStr) {
        boolean stretchedActive = (Client.stretched && Client.instance != null && Client.instance.isStretchedEnabled());
        
        if (modeStr.contains("FULLSCREEN")) {
            setResizable(true);
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else if (currentScreenMode == ScreenMode.FIXED) {
            if (stretchedActive) {
                setResizable(true);
                int sidebarWidth = sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : SIDEBAR_COLLAPSED_WIDTH;
                setMinimumSize(new Dimension(GAME_WIDTH + sidebarWidth, GAME_HEIGHT + TITLE_BAR_HEIGHT));
            } else {
                if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                    setExtendedState(JFrame.NORMAL);
                }
                setResizable(false);
                setFixedModeSize();
            }
        } else if (currentScreenMode == ScreenMode.RESIZABLE) {
            if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                setExtendedState(JFrame.NORMAL);
            }
            setResizable(true);
            setResizableModeSize();
        }
        
        resetCursor();
    }
    private void showTransitionOverlay() {
        if (isTransitioning.getAndSet(true)) {
            return;
        }
        
        try {
            if (gamePanel != null) {
                if (transitionOverlay.getParent() != null) {
                    transitionOverlay.getParent().remove(transitionOverlay);
                }
                
                if (gameClient != null) {
                    gameClient.setVisible(false);
                }
                
                gamePanel.setLayout(new OverlayLayout(gamePanel));
                gamePanel.add(transitionOverlay, 0);
                
                transitionOverlay.setBounds(0, 0, gamePanel.getWidth(), gamePanel.getHeight());
                transitionOverlay.setVisible(true);
                
                gamePanel.revalidate();
                gamePanel.repaint();
                
                System.out.println("DEBUG: Overlay shown instantly, game client hidden");
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to show transition overlay: " + e.getMessage());
        }
    }

    private void hideTransitionOverlay(int delayMs) {
        if (transitionTimer != null && transitionTimer.isRunning()) {
            transitionTimer.stop();
        }
        
        transitionTimer = new Timer(Math.min(delayMs, 300), e -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    if (gamePanel != null && transitionOverlay.getParent() != null) {
                        gamePanel.remove(transitionOverlay);
                        gamePanel.setLayout(new CenteringBorderLayout());
                        
                        if (gameClient != null) {
                            gameClient.setVisible(true);
                        }
                        
                        gamePanel.revalidate();
                        gamePanel.repaint();
                    }
                    transitionOverlay.setVisible(false);
                    isTransitioning.set(false);
                    
                    System.out.println("DEBUG: Overlay removed, game client shown");
                } catch (Exception ex) {
                    System.err.println("ERROR: Failed to hide overlay: " + ex.getMessage());
                }
            });
            
            if (transitionTimer != null) {
                transitionTimer.stop();
                transitionTimer = null;
            }
        });
        transitionTimer.setRepeats(false);
        transitionTimer.start();
    }
    
    private void setAllBackgroundsBlack() {
        getContentPane().setBackground(Color.BLACK);
        getRootPane().setBackground(Color.BLACK);
        if (mainContentPanel != null) mainContentPanel.setBackground(Color.BLACK);
        if (gamePanel != null) gamePanel.setBackground(Color.BLACK);
        if (sidebarContainer != null) sidebarContainer.setBackground(Color.BLACK);
    }
    
    private void setFixedModeSize() {
        setResizable(false);
        int sidebarWidth = sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : SIDEBAR_COLLAPSED_WIDTH;
        int width = GAME_WIDTH + sidebarWidth;
        int height = GAME_HEIGHT + TITLE_BAR_HEIGHT;
        //System.out.println("DEBUG setFixedModeSize: width=" + width + " (sidebar=" + sidebarWidth + "), height=" + height);
        setSize(width, height);
        setLocationRelativeTo(null);
    }

    private void setResizableModeSize() {
        setResizable(true);
        int sidebarWidth = sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : SIDEBAR_COLLAPSED_WIDTH;
        int width = Math.max(GAME_WIDTH + sidebarWidth, 900);
        int height = Math.max(GAME_HEIGHT + TITLE_BAR_HEIGHT, 600);
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        width = Math.min(width, screenSize.width - 100);
        height = Math.min(height, screenSize.height - 100);
        
        setSize(width, height);
        setMinimumSize(new Dimension(GAME_WIDTH + SIDEBAR_COLLAPSED_WIDTH, 
            GAME_HEIGHT + TITLE_BAR_HEIGHT));
        setLocationRelativeTo(null);
    }

    private void forceLayoutRefresh() {
        try {
            setAllBackgroundsBlack();
            
            if (mainContentPanel != null) {
                LayoutManager layout = mainContentPanel.getLayout();
                if (layout instanceof LayoutManager2) {
                    ((LayoutManager2) layout).invalidateLayout(mainContentPanel);
                }
                mainContentPanel.invalidate();
            }
            
            if (gamePanel != null) {
                gamePanel.invalidate();
            }
            
            if (mainContentPanel != null) {
                mainContentPanel.revalidate();
            }
            if (gamePanel != null) {
                gamePanel.revalidate();
            }
            
            if (mainContentPanel != null) {
                mainContentPanel.repaint();
            }
            if (gamePanel != null) {
                gamePanel.repaint();
            }
            
            System.out.println("DEBUG: Layout refresh completed");
            
        } catch (Exception e) {
            System.err.println("Error in layout refresh: " + e.getMessage());
        }
    }
    
    private void toggleFullscreen() {
        if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
            setExtendedState(JFrame.NORMAL);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }
    
    private void openQuickSettings() {
        JPopupMenu settingsMenu = new JPopupMenu();
        settingsMenu.setBackground(new Color(40, 40, 40));

        JMenuItem screenshotItem = new JMenuItem("Take Screenshot");
        screenshotItem.setBackground(new Color(40, 40, 40));
        screenshotItem.setForeground(TEXT_COLOR);
        screenshotItem.setFont(FONT_BODY);
        screenshotItem.addActionListener(e -> takeScreenshot());

        JMenuItem fullscreenItem = new JMenuItem("Toggle Fullscreen (F11)");
        fullscreenItem.setBackground(new Color(40, 40, 40));
        fullscreenItem.setForeground(TEXT_COLOR);
        fullscreenItem.setFont(FONT_BODY);
        fullscreenItem.addActionListener(e -> toggleFullscreen());

        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setBackground(new Color(40, 40, 40));
        aboutItem.setForeground(TEXT_COLOR);
        aboutItem.setFont(FONT_BODY);
        aboutItem.addActionListener(e -> showAbout());

        settingsMenu.add(screenshotItem);
        settingsMenu.add(fullscreenItem);
        settingsMenu.addSeparator();
        settingsMenu.add(aboutItem);

        Component settingsBtn = ((JPanel) ((BorderLayout) getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.NORTH)).getComponent(1);
        settingsMenu.show(settingsBtn, 0, settingsBtn.getHeight());
    }

    private void takeScreenshot() {
        try {
            Robot robot = new Robot();
            Rectangle captureRect;
            
            Component glassPane = getRootPane().getGlassPane();
            glassPane.setVisible(true);
            glassPane.setBackground(Color.WHITE);
            
            if (gameClient != null) {
                captureRect = gameClient.getBounds();
                Point panelLocation = gameClient.getLocationOnScreen();
                captureRect.setLocation(panelLocation);
            } else {
                captureRect = gamePanel.getBounds();
                Point panelLocation = gamePanel.getLocationOnScreen();
                captureRect.setLocation(panelLocation);
            }

            BufferedImage screenshot = robot.createScreenCapture(captureRect);
            
            glassPane.setVisible(false);
            
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "screenshot_" + timestamp + ".png";
            
            File screenshotFile;
            File clientDir = new File("screenshots");
            if (!clientDir.exists()) {
                clientDir.mkdirs();
            }
            screenshotFile = new File(clientDir, filename);
            
            ImageIO.write(screenshot, "png", screenshotFile);
            
            showStatusBar("Screenshot saved: " + filename, 3000);
            
        } catch (Exception e) {
            showStatusBar("Screenshot failed: " + e.getMessage(), 3000);
        }
    }

    private void showAbout() {
        String about = "Custom RSPS Client\n" +
                      "RuneLite-style interface with enhanced features\n\n" +
                      "Features:\n" +
                      "• Cross-platform support (Windows, macOS, Linux)\n" +
                      "• Rounded corners with smooth rendering (Win/Linux)\n" +
                      "• Native window controls (macOS)\n" +
                      "• Custom resize cursors (when resizable)\n" +
                      "• Keyboard shortcuts (F11, Alt+F4, ESC)\n" +
                      "• Double-buffered painting\n" +
                      "• Performance optimizations\n" +
                      "• Dynamic theme management\n" +
                      "• Professional visual polish\n" +
                      "• FIXED: Flicker-free dragging & resizing\n\n" +
                      "Version 2.6 (Resizable Fixed)\n" +
                      "Running on: " + OS_NAME;
        
        JOptionPane pane = new JOptionPane(about, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = pane.createDialog(this, "About");
        dialog.setModal(false);
        dialog.setVisible(true);
    }
    
    private void showStatusBar(String message, int duration) {
        JLabel statusLabel = new JLabel(message);
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setBackground(new Color(40, 40, 40, 230));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_COLOR, 1),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        
        JLayeredPane layeredPane = getLayeredPane();
        statusLabel.setSize(statusLabel.getPreferredSize());
        statusLabel.setLocation(10, getHeight() - statusLabel.getHeight() - 40);
        
        layeredPane.add(statusLabel, JLayeredPane.POPUP_LAYER);
        layeredPane.revalidate();
        layeredPane.repaint();
        
        Timer removeTimer = new Timer(duration, e -> {
            layeredPane.remove(statusLabel);
            layeredPane.revalidate();
            layeredPane.repaint();
        });
        removeTimer.setRepeats(false);
        removeTimer.start();
    }
    
    private void cleanup() {
        if (isDisposed.getAndSet(true)) {
            return;
        }
        
        System.out.println("Cleaning up CustomGameFrame resources...");
        
        if (transitionTimer != null) {
            transitionTimer.stop();
            transitionTimer = null;
        }
        
        synchronized (paintLock) {
            if (backBuffer != null) {
                backBuffer.flush();
                backBuffer = null;
            }
            if (stableBackBuffer != null) {
                stableBackBuffer.flush();
                stableBackBuffer = null;
            }
        }
        
        cachedWindowShape = null;
        
        if (resizeHandler != null && !IS_MAC) {
            removeMouseListener(resizeHandler);
            removeMouseMotionListener(resizeHandler);
            resizeHandler = null;
        }
        
        buttonMap.clear();
        iconCache.clear();
        
        gpuPlugin = null;
        
        System.out.println("CustomGameFrame cleanup complete");
    }
    
    // Getters
    public GPUPlugin getGPUPlugin() {
        return gpuPlugin;
    }
    
    public Component getGameClient() {
        return gameClient;
    }
    
    public boolean isSidebarVisible() {
        return sidebarVisible;
    }
    
    public PluginPanel getPluginPanel() {
        return pluginPanel;
    }
    
    public ExpTrackerPanel getExpTracker() {
        return expTrackerPanel;
    }
    
    public LootTrackerPanel getLootTracker() {
        return lootTrackerPanel;
    }
    
    public ThemeManagerPanel getThemeManager() {
        return themeManagerPanel;
    }
    
    @Override
    public void setUndecorated(boolean undecorated) {
        if (isDisplayable()) {
            undecoratedMode = undecorated;
            System.out.println("Frame already displayable - cannot change undecorated state to: " + undecorated);
            return;
        } else {
            super.setUndecorated(undecorated);
            undecoratedMode = undecorated;
        }
    }
    
    public boolean isUndecoratedMode() {
        return undecoratedMode;
    }
    
 // Inner class: CustomLayout
    private class CustomLayout implements LayoutManager2 {
        @Override
        public void addLayoutComponent(Component comp, Object constraints) {}

        @Override
        public void addLayoutComponent(String name, Component comp) {}

        @Override
        public void removeLayoutComponent(Component comp) {}

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            int sidebarWidth = sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : SIDEBAR_COLLAPSED_WIDTH;
            return new Dimension(GAME_WIDTH + sidebarWidth, GAME_HEIGHT);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            int sidebarWidth = sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : SIDEBAR_COLLAPSED_WIDTH;
            return new Dimension(GAME_WIDTH + sidebarWidth, GAME_HEIGHT);
        }

        @Override
        public void layoutContainer(Container parent) {
            parent.setBackground(Color.BLACK);
            
            int parentWidth = parent.getWidth();
            int parentHeight = parent.getHeight();
            int sidebarWidth = sidebarVisible ? SIDEBAR_EXPANDED_WIDTH : SIDEBAR_COLLAPSED_WIDTH;
            
            // CRITICAL FIX: Use subtraction, not Math.max
            int gameWidth = parentWidth - sidebarWidth;
            int gameHeight = parentHeight;
            
            // Ensure gameWidth is never negative or too small
            if (gameWidth < GAME_WIDTH) {
                gameWidth = GAME_WIDTH;
            }
            
            gamePanel.setBounds(0, 0, gameWidth, gameHeight);
            gamePanel.setBackground(Color.BLACK);
            
            if (sidebarContainer != null) {
                sidebarContainer.setBounds(gameWidth, 0, sidebarWidth, gameHeight);
                sidebarContainer.setBackground(Color.BLACK);
            }
            
            //System.out.println("DEBUG CustomLayout: parent=" + parentWidth + "x" + parentHeight + 
            //                 ", gamePanel=" + gameWidth + "x" + gameHeight + 
            //                 ", sidebar=" + sidebarWidth);
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public float getLayoutAlignmentX(Container target) { return 0; }

        @Override
        public float getLayoutAlignmentY(Container target) { return 0; }

        @Override
        public void invalidateLayout(Container target) {
            target.setBackground(Color.BLACK);
        }
    }
    
    // Inner class: CenteringBorderLayout
    private class CenteringBorderLayout extends BorderLayout {
        private int lastWidth = -1;
        private int lastHeight = -1;
        private boolean lastStretchedState = false;
        
        public void resetCache() {
            lastWidth = -1;
            lastHeight = -1;
            lastStretchedState = false;
        }
        
        @Override
        public void layoutContainer(Container target) {
            try {
                target.setBackground(Color.BLACK);
                
                if (gameClient == null) {
                    return;
                }
                
                Dimension targetSize = target.getSize();
                
                if (targetSize.width <= 0 || targetSize.height <= 0) {
                    return;
                }
                
                boolean stretchedActive = Client.stretched && Client.instance != null && Client.instance.isStretchedEnabled();
                
                boolean loggedIn = false;
                try {
                    Client clientInstance = getClientInstance();
                    if (clientInstance != null) {
                        loggedIn = clientInstance.loggedIn;
                    }
                } catch (Exception e) {
                    // Assume not logged in
                }
                
                // ALWAYS update in resizable mode when logged in - no caching
                if ((currentScreenMode == ScreenMode.RESIZABLE || stretchedActive) && loggedIn) {
                    // ALWAYS fill the container in resizable mode
                    lastWidth = targetSize.width;
                    lastHeight = targetSize.height;
                    lastStretchedState = stretchedActive;
                    
                    gameClient.setBounds(0, 0, targetSize.width, targetSize.height);
                    gameClient.setSize(targetSize.width, targetSize.height);
                    gameClient.setPreferredSize(targetSize);
                    gameClient.setMinimumSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
                    
                    System.out.println("DEBUG CenteringBorderLayout: Filling with " + 
                        targetSize.width + "x" + targetSize.height);
                } else {
                    // Only update when dimensions actually change in fixed mode
                    boolean shouldUpdate = (lastWidth != targetSize.width) ||
                                           (lastHeight != targetSize.height) ||
                                           (stretchedActive != lastStretchedState) ||
                                           (lastWidth == -1);
                    
                    if (shouldUpdate) {
                        lastWidth = targetSize.width;
                        lastHeight = targetSize.height;
                        lastStretchedState = stretchedActive;
                        
                        // Fixed mode - center at fixed size
                        int x = Math.max(0, (targetSize.width - GAME_WIDTH) / 2);
                        int y = 0;
                        
                        gameClient.setBounds(x, y, GAME_WIDTH, GAME_HEIGHT);
                        gameClient.setSize(GAME_WIDTH, GAME_HEIGHT);
                        gameClient.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
                        
                        System.out.println("DEBUG CenteringBorderLayout: Fixed mode centered");
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error in CenteringBorderLayout: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        @Override
        public Dimension preferredLayoutSize(Container target) {
            if (currentScreenMode == ScreenMode.RESIZABLE) {
                Dimension size = target.getSize();
                if (size.width > 0 && size.height > 0) {
                    return size;
                }
            }
            return new Dimension(GAME_WIDTH, GAME_HEIGHT);
        }
        
        @Override
        public Dimension minimumLayoutSize(Container target) {
            return new Dimension(GAME_WIDTH, GAME_HEIGHT);
        }
    }
    
    // Inner class: ResizeHandler
    private class ResizeHandler extends MouseAdapter {
        private final JFrame frame;
        private final int BORDER_WIDTH = 8;
        private final int CORNER_SIZE = 16;
        
        private Point startPoint;
        private Rectangle startBounds;
        private int resizeDirection = 0;
        
        private static final int NONE = 0;
        private static final int NORTH = 1;
        private static final int SOUTH = 2;
        private static final int WEST = 3;
        private static final int EAST = 4;
        private static final int NORTH_WEST = 5;
        private static final int NORTH_EAST = 6;
        private static final int SOUTH_WEST = 7;
        private static final int SOUTH_EAST = 8;
        
        public ResizeHandler(JFrame frame) {
            this.frame = frame;
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH || !frame.isResizable()) {
                return;
            }
            
            startPoint = e.getLocationOnScreen();
            startBounds = frame.getBounds();
            resizeDirection = getResizeDirection(e.getPoint());
            
            if (resizeDirection != NONE) {
                isResizing.set(true);
                setCursorForDirection(resizeDirection);
            }
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            if (isResizing.get()) {
                isResizing.set(false);
                resizeDirection = NONE;
                
                SwingUtilities.invokeLater(() -> {
                    if (!IS_MAC) {
                        CustomGameFrame customFrame = (CustomGameFrame) frame;
                        
                        synchronized (customFrame.paintLock) {
                            customFrame.cachedWindowShape = null;
                            if (customFrame.backBuffer != null) {
                                customFrame.backBuffer.flush();
                                customFrame.backBuffer = null;
                            }
                        }
                        
                        customFrame.setWindowShape();
                    }
                    
                    frame.setCursor(Cursor.getDefaultCursor());
                    frame.repaint();
                });
            }
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            if (!isResizing.get()) {
                frame.setCursor(Cursor.getDefaultCursor());
            }
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {
            if (!isResizing.get()) {
                updateCursorForPosition(e.getPoint());
            }
        }
        
        @Override
        public void mouseMoved(MouseEvent e) {
            if (!isResizing.get()) {
                updateCursorForPosition(e.getPoint());
            }
        }
        
        @Override
        public void mouseDragged(MouseEvent e) {
            if (!isResizing.get() || resizeDirection == NONE || !frame.isResizable()) {
                return;
            }
            
            Point currentPoint = e.getLocationOnScreen();
            int deltaX = currentPoint.x - startPoint.x;
            int deltaY = currentPoint.y - startPoint.y;
            
            Rectangle newBounds = new Rectangle(startBounds);
            Dimension minSize = frame.getMinimumSize();
            
            switch (resizeDirection) {
                case NORTH:
                    newBounds.y += deltaY;
                    newBounds.height -= deltaY;
                    if (newBounds.height < minSize.height) {
                        newBounds.y = startBounds.y + startBounds.height - minSize.height;
                        newBounds.height = minSize.height;
                    }
                    break;
                case SOUTH:
                    newBounds.height += deltaY;
                    if (newBounds.height < minSize.height) {
                        newBounds.height = minSize.height;
                    }
                    break;
                case WEST:
                    newBounds.x += deltaX;
                    newBounds.width -= deltaX;
                    if (newBounds.width < minSize.width) {
                        newBounds.x = startBounds.x + startBounds.width - minSize.width;
                        newBounds.width = minSize.width;
                    }
                    break;
                case EAST:
                    newBounds.width += deltaX;
                    if (newBounds.width < minSize.width) {
                        newBounds.width = minSize.width;
                    }
                    break;
                case NORTH_WEST:
                    newBounds.x += deltaX;
                    newBounds.y += deltaY;
                    newBounds.width -= deltaX;
                    newBounds.height -= deltaY;
                    if (newBounds.width < minSize.width) {
                        newBounds.x = startBounds.x + startBounds.width - minSize.width;
                        newBounds.width = minSize.width;
                    }
                    if (newBounds.height < minSize.height) {
                        newBounds.y = startBounds.y + startBounds.height - minSize.height;
                        newBounds.height = minSize.height;
                    }
                    break;
                case NORTH_EAST:
                    newBounds.y += deltaY;
                    newBounds.width += deltaX;
                    newBounds.height -= deltaY;
                    if (newBounds.width < minSize.width) {
                        newBounds.width = minSize.width;
                    }
                    if (newBounds.height < minSize.height) {
                        newBounds.y = startBounds.y + startBounds.height - minSize.height;
                        newBounds.height = minSize.height;
                    }
                    break;
                case SOUTH_WEST:
                    newBounds.x += deltaX;
                    newBounds.width -= deltaX;
                    newBounds.height += deltaY;
                    if (newBounds.width < minSize.width) {
                        newBounds.x = startBounds.x + startBounds.width - minSize.width;
                        newBounds.width = minSize.width;
                    }
                    if (newBounds.height < minSize.height) {
                        newBounds.height = minSize.height;
                    }
                    break;
                case SOUTH_EAST:
                    newBounds.width += deltaX;
                    newBounds.height += deltaY;
                    if (newBounds.width < minSize.width) {
                        newBounds.width = minSize.width;
                    }
                    if (newBounds.height < minSize.height) {
                        newBounds.height = minSize.height;
                    }
                    break;
            }
            
            frame.setBounds(newBounds);
        }
        
        private void updateCursorForPosition(Point point) {
            if (frame.getExtendedState() == JFrame.MAXIMIZED_BOTH || !frame.isResizable()) {
                resetCursor();
                return;
            }
            
            int direction = getResizeDirection(point);
            setCursorForDirection(direction);
        }
        
        private int getResizeDirection(Point point) {
            int x = point.x;
            int y = point.y;
            int width = frame.getWidth();
            int height = frame.getHeight();
            
            if (x <= CORNER_SIZE && y <= CORNER_SIZE) return NORTH_WEST;
            if (x >= width - CORNER_SIZE && y <= CORNER_SIZE) return NORTH_EAST;
            if (x <= CORNER_SIZE && y >= height - CORNER_SIZE) return SOUTH_WEST;
            if (x >= width - CORNER_SIZE && y >= height - CORNER_SIZE) return SOUTH_EAST;
            
            if (y <= BORDER_WIDTH) return NORTH;
            if (y >= height - BORDER_WIDTH) return SOUTH;
            if (x <= BORDER_WIDTH) return WEST;
            if (x >= width - BORDER_WIDTH) return EAST;
            
            return NONE;
        }
        
        private void setCursorForDirection(int direction) {
            final Cursor cursor;
            
            switch (direction) {
                case NORTH:
                case SOUTH:
                    cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
                    break;
                case WEST:
                case EAST:
                    cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
                    break;
                case NORTH_WEST:
                case SOUTH_EAST:
                    cursor = Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                    break;
                case NORTH_EAST:
                case SOUTH_WEST:
                    cursor = Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                    break;
                case NONE:
                default:
                    cursor = Cursor.getDefaultCursor();
                    break;
            }
            
            SwingUtilities.invokeLater(() -> frame.setCursor(cursor));
        }
    }
}