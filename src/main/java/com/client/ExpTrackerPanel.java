package com.client;

import javax.swing.*;
import javax.swing.Timer;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.awt.geom.RoundRectangle2D;

public class ExpTrackerPanel extends JPanel {
    // Modern color scheme with gradients
    private static final Color BACKGROUND_COLOR = new Color(0x1a, 0x1a, 0x1a);
    private static final Color TEXT_COLOR = new Color(0x00a2e8);
    private static final Color TEXT_SECONDARY = new Color(0x888888);
    private static final Color PANEL_BACKGROUND = new Color(35, 35, 40);
    private static final Color PANEL_HOVER = new Color(40, 40, 45);
    private static final Color PANEL_DRAG = new Color(45, 45, 50);
    private static final Color BUTTON_BACKGROUND = new Color(0x2d, 0x2d, 0x35);
    private static final Color BUTTON_HOVER = new Color(0x35, 0x35, 0x40);
    private static final Color PROGRESS_BG = new Color(45, 45, 50);
    private static final Color PROGRESS_START = new Color(76, 175, 80);
    private static final Color PROGRESS_END = new Color(56, 142, 60);
    private static final Color XP_GAINED_COLOR = new Color(129, 199, 132);
    private static final Color XP_LEFT_COLOR = new Color(0xb0b0b0);
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 40);
    private static final Color GLOW_COLOR = new Color(0x00, 0xa2, 0xe8, 30);
    
    // Visual constants
    private static final int PANEL_HEIGHT = 90;
    private static final int PANEL_MIN_WIDTH = 260;
    private static final int ICON_SIZE = 20;
    private static final int PROGRESS_BAR_HEIGHT = 20;
    private static final int PANEL_PADDING = 12;
    private static final int CORNER_RADIUS = 12;
    private static final int PROGRESS_CORNER_RADIUS = 10;
    private static final int SHADOW_SIZE = 8;
    private static final int GLOW_SIZE = 15;
    private static final int ANIMATION_DURATION = 200;
    
    // Spacing constants
    private static final int VERTICAL_STRUT_SMALL = 6;
    private static final int VERTICAL_STRUT_MEDIUM = 6;
    private static final int VERTICAL_STRUT_LARGE = 8;
    
    // Timing constants
    private static final int UPDATE_INTERVAL_MS = 1000;
    private static final long MILLIS_PER_HOUR = 3600000L;
    
    // Skills
    private static final String[] SKILL_NAMES = {
        "Attack", "Defence", "Strength", "Hitpoints", "Ranged", "Prayer", "Magic",
        "Cooking", "Woodcutting", "Fletching", "Fishing", "Firemaking", "Crafting",
        "Smithing", "Mining", "Herblore", "Agility", "Thieving", "Slayer", "Farming",
        "Runecrafting", "Hunter", "Construction", "Summoning"
    };
    
    private static final int TRANSPARENCY_KEY_RGB = 0xFFFF00FF;
    
    private JPanel skillsContainer;
    private JScrollPane scrollPane;
    private Map<Integer, SkillTracker> skillTrackers;
    private JButton resetButton;
    private ModernToggleButton pulseToggle;
    private JLabel totalGainLabel;
    private JLabel sessionTimeLabel;
    private long sessionStartTime;
    private Timer updateTimer;
    private boolean pulseEffectsEnabled = true;
    
    private static final Map<String, ImageIcon> iconCache = new ConcurrentHashMap<>();
    
    public ExpTrackerPanel() {
        skillTrackers = new ConcurrentHashMap<>();
        sessionStartTime = System.currentTimeMillis();
        initializePanel();
        createComponents();
        startUpdateTimer();
    }
    
    private void initializePanel() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }
    
    private void createComponents() {
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        skillsContainer = new JPanel();
        skillsContainer.setLayout(new BoxLayout(skillsContainer, BoxLayout.Y_AXIS));
        skillsContainer.setBackground(BACKGROUND_COLOR);
        skillsContainer.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        
        scrollPane = new JScrollPane(skillsContainer);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        scrollPane.getVerticalScrollBar().setBackground(BACKGROUND_COLOR);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        
        add(scrollPane, BorderLayout.CENTER);
        
        initializeSkillTrackers();
    }
    
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BACKGROUND_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JLabel titleLabel = new JLabel("Experience Tracker");
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        resetButton = createModernButton("Reset");
        resetButton.addActionListener(e -> resetSession());
        
        // First row: Title and Reset button aligned
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(BACKGROUND_COLOR);
        topRow.add(titleLabel, BorderLayout.WEST);
        topRow.add(resetButton, BorderLayout.EAST);
        
        // Second row: Info panel and Pulse toggle
        JPanel secondRow = new JPanel(new BorderLayout());
        secondRow.setBackground(BACKGROUND_COLOR);
        
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        infoPanel.setBackground(BACKGROUND_COLOR);
        
        totalGainLabel = new JLabel("Total: 0 XP");
        totalGainLabel.setForeground(Color.WHITE);
        totalGainLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        sessionTimeLabel = new JLabel("Session: 00:00:00");
        sessionTimeLabel.setForeground(TEXT_SECONDARY);
        sessionTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        infoPanel.add(totalGainLabel);
        infoPanel.add(sessionTimeLabel);
        
        pulseToggle = new ModernToggleButton("Pulse");
        pulseToggle.setSelected(true);
        pulseToggle.addActionListener(e -> {
            pulseEffectsEnabled = pulseToggle.isSelected();
        });
        
        secondRow.add(infoPanel, BorderLayout.WEST);
        secondRow.add(pulseToggle, BorderLayout.EAST);
        
        // Combine rows
        JPanel fullHeader = new JPanel();
        fullHeader.setLayout(new BoxLayout(fullHeader, BoxLayout.Y_AXIS));
        fullHeader.setBackground(BACKGROUND_COLOR);
        
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        secondRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        fullHeader.add(topRow);
        fullHeader.add(Box.createVerticalStrut(6));
        fullHeader.add(secondRow);
        
        return fullHeader;
    }
    
    private JButton createModernButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Paint rounded background
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                
                // Paint text
                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(getText(), textX, textY);
                
                g2.dispose();
            }
        };
        
        button.setBackground(BUTTON_BACKGROUND);
        button.setForeground(TEXT_COLOR);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setPreferredSize(new Dimension(70, 28));

        button.addMouseListener(new MouseAdapter() {
            private Timer fadeTimer;
            
            @Override
            public void mouseEntered(MouseEvent e) {
                animateColor(button, button.getBackground(), BUTTON_HOVER);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                animateColor(button, button.getBackground(), BUTTON_BACKGROUND);
            }
        });

        return button;
    }
    
    private void animateColor(JComponent component, Color from, Color to) {
        Timer timer = new Timer(20, null);
        final long startTime = System.currentTimeMillis();
        
        timer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long elapsed = System.currentTimeMillis() - startTime;
                float progress = Math.min(1.0f, elapsed / (float) ANIMATION_DURATION);
                
                int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * progress);
                int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * progress);
                int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * progress);
                
                component.setBackground(new Color(r, g, b));
                component.repaint();
                
                if (progress >= 1.0f) {
                    timer.stop();
                }
            }
        });
        timer.start();
    }
    
    private void initializeSkillTrackers() {
        for (int i = 0; i < SKILL_NAMES.length; i++) {
            SkillTracker tracker = new SkillTracker(i, SKILL_NAMES[i]);
            skillTrackers.put(i, tracker);
        }
    }
    
    public void updateExperience(int skillId, long newExperience) {
        SkillTracker tracker = skillTrackers.get(skillId);
        if (tracker != null) {
            boolean hadGains = tracker.hasGains();
            tracker.updateExperience(newExperience);
            
            if (!hadGains && tracker.hasGains()) {
                addSkillTrackerToUI(tracker);
            } else if (tracker.hasGains() && tracker.getPanel().getParent() == null) {
                addSkillTrackerToUI(tracker);
            }
            
            updateTotalGains();
        }
    }
    
    private void addSkillTrackerToUI(SkillTracker tracker) {
        JPanel panel = tracker.getPanel();
        
        // Fade in animation
        panel.setVisible(false);
        skillsContainer.add(panel);
        skillsContainer.add(Box.createVerticalStrut(VERTICAL_STRUT_SMALL));
        skillsContainer.revalidate();
        
        Timer fadeIn = new Timer(20, null);
        final long startTime = System.currentTimeMillis();
        final float[] alpha = {0.0f};
        
        fadeIn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long elapsed = System.currentTimeMillis() - startTime;
                alpha[0] = Math.min(1.0f, elapsed / 300.0f);
                
                panel.setVisible(true);
                panel.repaint();
                
                if (alpha[0] >= 1.0f) {
                    fadeIn.stop();
                }
            }
        });
        fadeIn.start();
    }
    
    private void updateTotalGains() {
        long totalGains = 0;
        for (SkillTracker tracker : skillTrackers.values()) {
            totalGains += tracker.getSessionGains();
        }
        
        totalGainLabel.setText("Total: " + formatNumber(totalGains) + " XP");
    }
    
    private void updateSessionTime() {
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        long seconds = sessionDuration / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        
        sessionTimeLabel.setText(String.format("Session: %02d:%02d:%02d", hours, minutes, seconds));
    }
    
    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return NumberFormat.getInstance().format(number);
    }
    
    private void resetSession() {
        for (SkillTracker tracker : skillTrackers.values()) {
            tracker.resetSession();
        }
        skillsContainer.removeAll();
        sessionStartTime = System.currentTimeMillis();
        updateTotalGains();
        updateSessionTime();
        skillsContainer.revalidate();
        skillsContainer.repaint();
        this.revalidate();  
        this.repaint();
    }
    
    public void resetTracker() {
        SwingUtilities.invokeLater(() -> {
            resetButton.doClick();
        });
    }
    
    public void refreshFromClient() {
        if (Client.experience == null) return;
        
        for (int skillId = 0; skillId < SKILL_NAMES.length && skillId < Client.experience.length; skillId++) {
            long currentExp = (long) Client.experience[skillId];
            updateExperience(skillId, currentExp);
        }
    }
    
    private void startUpdateTimer() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        
        updateTimer = new Timer(UPDATE_INTERVAL_MS, e -> {
            for (SkillTracker tracker : skillTrackers.values()) {
                tracker.updateRates();
            }
            updateTotalGains();
            updateSessionTime();
        });
        updateTimer.start();
    }
    
    public void stopTimer() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }
    
    @Override
    public void removeNotify() {
        super.removeNotify();
        stopTimer();
    }
    
    private static ImageIcon loadSkillIcon(String skillName) {
        return iconCache.computeIfAbsent(skillName.toLowerCase(), name -> {
            try {
                String iconPath = "/icons/skills/" + name + ".png";
                URL iconUrl = ExpTrackerPanel.class.getResource(iconPath);
                
                if (iconUrl == null) {
                    return null;
                }
                
                BufferedImage originalImage = ImageIO.read(iconUrl);
                if (originalImage == null) {
                    return null;
                }
                
                BufferedImage transparentImage = new BufferedImage(
                    originalImage.getWidth(), 
                    originalImage.getHeight(), 
                    BufferedImage.TYPE_INT_ARGB
                );
                
                int[] pixels = originalImage.getRGB(0, 0, 
                    originalImage.getWidth(), originalImage.getHeight(), 
                    null, 0, originalImage.getWidth());
                
                for (int i = 0; i < pixels.length; i++) {
                    int pixel = pixels[i];
                    int rgb = pixel & 0x00FFFFFF;
                    
                    if (rgb == (TRANSPARENCY_KEY_RGB & 0x00FFFFFF)) {
                        pixels[i] = 0x00000000;
                    } else {
                        pixels[i] = pixel | 0xFF000000;
                    }
                }
                
                transparentImage.setRGB(0, 0, 
                    transparentImage.getWidth(), transparentImage.getHeight(), 
                    pixels, 0, transparentImage.getWidth());
                
                Image scaled = transparentImage.getScaledInstance(
                    ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH);
                
                return new ImageIcon(scaled);
                
            } catch (Exception e) {
                System.err.println("Error loading icon for " + name + ": " + e.getMessage());
                return null;
            }
        });
    }
    
    private class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(80, 80, 90);
            this.trackColor = BACKGROUND_COLOR;
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
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x + 3, thumbBounds.y + 2, 
                               thumbBounds.width - 6, thumbBounds.height - 4, 10, 10);
            } finally {
                g2.dispose();
            }
        }
        
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            g.setColor(trackColor);
            g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        }
    }
    
    private class SkillTracker {
        private final int skillId;
        private final String skillName;
        
        private final AtomicLong baseExperience;
        private final AtomicLong currentExperience;
        private final AtomicLong sessionGains;
        
        private long lastUpdateTime;
        private double currentRate;
        
        private ModernSkillPanel panel;
        private JLabel nameLabel;
        private JLabel gainsLabel;
        private JLabel rateLabel;
        private JLabel xpLeftLabel;
        private ModernProgressBar progressBar;
        
        private boolean isDragging = false;
        private int cachedLevel = -1;
        private long cachedLevelExp = -1;
        
        public SkillTracker(int skillId, String skillName) {
            this.skillId = skillId;
            this.skillName = skillName;
            this.baseExperience = new AtomicLong(0);
            this.currentExperience = new AtomicLong(0);
            this.sessionGains = new AtomicLong(0);
            this.lastUpdateTime = System.currentTimeMillis();
            this.currentRate = 0.0;
            
            createPanel();
            updateDisplay();
        }
        
        private void createPanel() {
            panel = new ModernSkillPanel();
            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, PANEL_HEIGHT));
            panel.setPreferredSize(new Dimension(PANEL_MIN_WIDTH, PANEL_HEIGHT));
            
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isDragging) {
                        panel.setHovering(true);
                    }
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isDragging) {
                        panel.setHovering(false);
                    }
                }
            });
            
            setupDragAndDrop();
            
            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setOpaque(false);
            
            JPanel topRow = new JPanel(new BorderLayout());
            topRow.setOpaque(false);
            topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            
            JPanel leftInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            leftInfo.setOpaque(false);
            
            JLabel iconLabel = createIconLabel();
            nameLabel = new JLabel(skillName);
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            
            leftInfo.add(iconLabel);
            leftInfo.add(nameLabel);
            
            gainsLabel = new JLabel("Gained: 0", SwingConstants.RIGHT);
            gainsLabel.setForeground(XP_GAINED_COLOR);
            gainsLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            
            topRow.add(leftInfo, BorderLayout.WEST);
            topRow.add(gainsLabel, BorderLayout.EAST);
            
            progressBar = new ModernProgressBar();
            progressBar.setValue(0);
            progressBar.setText("Lvl 1");
            progressBar.setPreferredSize(new Dimension(0, PROGRESS_BAR_HEIGHT));
            progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, PROGRESS_BAR_HEIGHT));
            
            JPanel bottomRow = new JPanel(new BorderLayout());
            bottomRow.setOpaque(false);
            bottomRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            
            xpLeftLabel = new JLabel("XP Left: -", SwingConstants.LEFT);
            xpLeftLabel.setForeground(XP_LEFT_COLOR);
            xpLeftLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            
            rateLabel = new JLabel("XP/Hour: 0", SwingConstants.RIGHT);
            rateLabel.setForeground(TEXT_COLOR);
            rateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            
            bottomRow.add(xpLeftLabel, BorderLayout.WEST);
            bottomRow.add(rateLabel, BorderLayout.EAST);
            
            content.add(topRow);
            content.add(Box.createVerticalStrut(VERTICAL_STRUT_MEDIUM));
            content.add(progressBar);
            content.add(Box.createVerticalStrut(VERTICAL_STRUT_LARGE));
            content.add(bottomRow);
            
            panel.setLayout(new BorderLayout());
            panel.add(content, BorderLayout.CENTER);
            panel.setBorder(BorderFactory.createEmptyBorder(PANEL_PADDING, PANEL_PADDING, PANEL_PADDING, PANEL_PADDING));
        }
        
        private JLabel createIconLabel() {
            JLabel iconLabel = new JLabel();
            iconLabel.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
            
            ImageIcon icon = loadSkillIcon(skillName);
            if (icon != null) {
                iconLabel.setIcon(icon);
            } else {
                iconLabel.setText(skillName.substring(0, 1));
                iconLabel.setForeground(Color.WHITE);
                iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
                iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            }
            
            return iconLabel;
        }
        
        private int getCurrentLevel() {
            long exp = currentExperience.get();
            
            if (exp == cachedLevelExp && cachedLevel != -1) {
                return cachedLevel;
            }
            
            if (exp <= 0) {
                cachedLevel = 1;
                cachedLevelExp = exp;
                return 1;
            }
            
            try {
                int points = 0;
                int output = 0;
                for (int lvl = 1; lvl < 100; lvl++) {
                    points += (int) Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
                    output = (int) Math.floor(points / 4);
                    if ((output - 1) >= exp) {
                        cachedLevel = lvl;
                        cachedLevelExp = exp;
                        return lvl;
                    }
                }
                cachedLevel = 99;
                cachedLevelExp = exp;
                return 99;
            } catch (Exception e) {
                return 1;
            }
        }

        private String calculateXpToNextLevel() {
            try {
                int currentLevel = getCurrentLevel();
                
                // At max level, show MAX
                if (currentLevel >= 99) {
                    return "MAX";
                }
                
                long exp = currentExperience.get();
                if (exp <= 0) {
                    return "-";
                }
                
                int expForNextLevel = Client.getXPForLevel(currentLevel + 1);
                int expNeeded = expForNextLevel - (int) exp;
                
                return formatNumber(Math.max(0, expNeeded));
            } catch (Exception e) {
                return "-";
            }
        }

        private int calculateLevelProgress() {
            try {
                // Check level FIRST, before any XP calculations
                int currentLevel = getCurrentLevel();
                
                // Level 99 is the maximum - show 100% completion
                if (currentLevel >= 99) {
                    return 100;
                }
                
                long exp = currentExperience.get();
                if (exp <= 0) {
                    return 0;
                }
                
                int expForCurrentLevel = Client.getXPForLevel(currentLevel);
                int expForNextLevel = Client.getXPForLevel(currentLevel + 1);
                
                int expInCurrentLevel = (int) exp - expForCurrentLevel;
                int expNeededForLevel = expForNextLevel - expForCurrentLevel;
                
                if (expNeededForLevel <= 0) return 100;
                
                int percentage = (int) ((double) expInCurrentLevel / expNeededForLevel * 100);
                return Math.min(100, Math.max(0, percentage));
            } catch (Exception e) {
                return 0;
            }
        }
        
        private void setupDragAndDrop() {
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    isDragging = true;
                    panel.setDragging(true);
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    isDragging = false;
                    panel.setDragging(false);
                    
                    Component parent = panel.getParent();
                    if (parent != null) {
                        Point dropPoint = SwingUtilities.convertPoint(panel, e.getPoint(), parent);
                        reorderSkills(dropPoint);
                    }
                }
            });
        }
        
        private void reorderSkills(Point dropPoint) {
            Component parent = panel.getParent();
            if (!(parent instanceof JPanel)) {
                return;
            }
            
            JPanel container = (JPanel) parent;
            Component[] components = container.getComponents();
            
            int targetIndex = -1;
            for (int i = 0; i < components.length; i++) {
                if (dropPoint.y < components[i].getBounds().getCenterY()) {
                    targetIndex = i;
                    break;
                }
            }
            
            if (targetIndex == -1) targetIndex = components.length;
            
            container.remove(panel);
            if (targetIndex >= components.length) {
                container.add(panel);
            } else {
                container.add(panel, targetIndex);
            }
            
            container.revalidate();
            container.repaint();
        }
        
        public void updateExperience(long newExperience) {
            long currentExp = currentExperience.get();
            long baseExp = baseExperience.get();
            
            if (baseExp == 0 && currentExp == 0) {
                baseExperience.set(newExperience);
                currentExperience.set(newExperience);
                updateDisplay();
                return;
            }
            
            if (newExperience > currentExp) {
                long gain = newExperience - currentExp;
                sessionGains.addAndGet(gain);
                currentExperience.set(newExperience);
                
                cachedLevel = -1;
                cachedLevelExp = -1;
                
                // Pulse animation on XP gain (if enabled)
                if (pulseEffectsEnabled) {
                    panel.pulseEffect();
                }
                
                updateDisplay();
                updateRates();
            }
        }

        public void updateRates() {
            long currentTime = System.currentTimeMillis();
            long sessionDuration = currentTime - sessionStartTime;
            long gains = sessionGains.get();
            
            if (sessionDuration > 0 && gains > 0) {
                currentRate = (double) gains / sessionDuration * MILLIS_PER_HOUR;
                rateLabel.setText("XP/Hour: " + formatNumber(Math.round(currentRate)));
            } else {
                rateLabel.setText("XP/Hour: 0");
            }
        }
        
        private void updateDisplay() {
            gainsLabel.setText("Gained: " + formatNumber(sessionGains.get()));
            xpLeftLabel.setText("XP Left: " + calculateXpToNextLevel());
            
            int currentLevel = getCurrentLevel();
            progressBar.setText("Lvl " + currentLevel);
            
            int newProgress = calculateLevelProgress();
            progressBar.animateToValue(newProgress);
        }
        
        public void resetSession() {
            baseExperience.set(currentExperience.get());
            sessionGains.set(0);
            currentRate = 0.0;
            cachedLevel = -1;
            cachedLevelExp = -1;
            updateDisplay();
            updateRates();
        }
        
        public boolean hasGains() {
            return sessionGains.get() > 0;
        }
        
        public long getSessionGains() {
            return sessionGains.get();
        }
        
        public JPanel getPanel() {
            return panel;
        }
    }
    
    private class ModernSkillPanel extends JPanel {
        private boolean hovering = false;
        private boolean dragging = false;
        private float pulseAlpha = 0.0f;
        private Timer pulseTimer;
        
        public ModernSkillPanel() {
            setOpaque(false);
        }
        
        public void setHovering(boolean hovering) {
            this.hovering = hovering;
            repaint();
        }
        
        public void setDragging(boolean dragging) {
            this.dragging = dragging;
            repaint();
        }
        
        public void pulseEffect() {
            if (pulseTimer != null && pulseTimer.isRunning()) {
                pulseTimer.stop();
            }
            
            pulseTimer = new Timer(30, null);
            final long startTime = System.currentTimeMillis();
            
            pulseTimer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float progress = elapsed / 500.0f;
                    
                    if (progress <= 0.5f) {
                        pulseAlpha = progress * 2.0f;
                    } else {
                        pulseAlpha = (1.0f - progress) * 2.0f;
                    }
                    
                    repaint();
                    
                    if (progress >= 1.0f) {
                        pulseAlpha = 0.0f;
                        pulseTimer.stop();
                        repaint();
                    }
                }
            });
            pulseTimer.start();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            
            // Draw shadow
            if (!dragging) {
                g2.setColor(SHADOW_COLOR);
                g2.fillRoundRect(4, 4, width - 8, height - 8, CORNER_RADIUS, CORNER_RADIUS);
            }
            
            // Draw glow on hover
            if (hovering && !dragging) {
                for (int i = GLOW_SIZE; i > 0; i--) {
                    float alpha = (1.0f - (i / (float) GLOW_SIZE)) * 0.3f;
                    g2.setColor(new Color(GLOW_COLOR.getRed(), GLOW_COLOR.getGreen(), 
                                         GLOW_COLOR.getBlue(), (int) (alpha * 255)));
                    g2.fillRoundRect(-i, -i, width + (i * 2), height + (i * 2), 
                                   CORNER_RADIUS + i, CORNER_RADIUS + i);
                }
            }
            
            // Draw background
            Color bgColor = dragging ? PANEL_DRAG : (hovering ? PANEL_HOVER : PANEL_BACKGROUND);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, width, height, CORNER_RADIUS, CORNER_RADIUS);
            
            // Draw subtle gradient overlay
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(255, 255, 255, 5),
                0, height, new Color(0, 0, 0, 10)
            );
            g2.setPaint(gradient);
            g2.fillRoundRect(0, 0, width, height, CORNER_RADIUS, CORNER_RADIUS);
            
            // Draw pulse effect
            if (pulseAlpha > 0) {
                g2.setColor(new Color(XP_GAINED_COLOR.getRed(), XP_GAINED_COLOR.getGreen(), 
                                     XP_GAINED_COLOR.getBlue(), (int) (pulseAlpha * 60)));
                g2.fillRoundRect(0, 0, width, height, CORNER_RADIUS, CORNER_RADIUS);
            }
            
            // Draw border
            if (dragging) {
                g2.setColor(TEXT_COLOR);
                g2.setStroke(new BasicStroke(2));
            } else {
                g2.setColor(new Color(255, 255, 255, 15));
                g2.setStroke(new BasicStroke(1));
            }
            g2.drawRoundRect(0, 0, width - 1, height - 1, CORNER_RADIUS, CORNER_RADIUS);
            
            g2.dispose();
        }
    }
    
    private class ModernProgressBar extends JComponent {
        private int value = 0;
        private int targetValue = 0;
        private String text = "";
        private Timer animationTimer;
        
        public void setValue(int value) {
            this.value = Math.max(0, Math.min(100, value));
            repaint();
        }
        
        public void animateToValue(int target) {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }
            
            targetValue = Math.max(0, Math.min(100, target));
            
            if (Math.abs(targetValue - value) < 2) {
                value = targetValue;
                repaint();
                return;
            }
            
            animationTimer = new Timer(20, null);
            
            animationTimer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int diff = targetValue - value;
                    if (Math.abs(diff) < 1) {
                        value = targetValue;
                        animationTimer.stop();
                    } else {
                        // FIX: Use Math.max to ensure we always move at least 1
                        int step = Math.max(1, diff / 3);
                        if (diff < 0) {
                            value -= Math.max(1, Math.abs(diff) / 3);
                        } else {
                            value += step;
                        }
                    }
                    repaint();
                }
            });
            animationTimer.start();
        }
        
        public void setText(String text) {
            this.text = text;
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            
            // Draw background
            g2.setColor(PROGRESS_BG);
            g2.fillRoundRect(0, 0, width, height, PROGRESS_CORNER_RADIUS, PROGRESS_CORNER_RADIUS);
            
            // Draw progress fill with gradient
            if (value > 0) {
                int fillWidth = (int) ((value / 100.0) * width);
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, PROGRESS_START,
                    0, height, PROGRESS_END
                );
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, fillWidth, height, PROGRESS_CORNER_RADIUS, PROGRESS_CORNER_RADIUS);
                
                // Add shine effect
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(0, 0, fillWidth, height / 2, PROGRESS_CORNER_RADIUS, PROGRESS_CORNER_RADIUS);
            }
            
            // Draw border
            g2.setColor(new Color(0, 0, 0, 50));
            g2.drawRoundRect(0, 0, width - 1, height - 1, PROGRESS_CORNER_RADIUS, PROGRESS_CORNER_RADIUS);
            
            // Draw text with shadow
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            int textX = (width - fm.stringWidth(text)) / 2;
            int textY = ((height - fm.getHeight()) / 2) + fm.getAscent();
            
            // Text shadow
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(text, textX + 1, textY + 1);
            
            // Text
            g2.setColor(Color.WHITE);
            g2.drawString(text, textX, textY);
            
            g2.dispose();
        }
    }
    
    private class ModernToggleButton extends JComponent {
        private boolean selected = false;
        private String text;
        private Timer animationTimer;
        private float togglePosition = 0.0f; // 0.0 = off, 1.0 = on
        
        public ModernToggleButton(String text) {
            this.text = text;
            setPreferredSize(new Dimension(85, 28));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setSelected(!selected);
                    fireActionPerformed();
                }
                
                @Override
                public void mouseEntered(MouseEvent e) {
                    repaint();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    repaint();
                }
            });
        }
        
        public boolean isSelected() {
            return selected;
        }
        
        public void setSelected(boolean selected) {
            if (this.selected != selected) {
                this.selected = selected;
                animateToggle();
            }
        }
        
        private void animateToggle() {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }
            
            float targetPosition = selected ? 1.0f : 0.0f;
            
            animationTimer = new Timer(20, null);
            animationTimer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    float diff = targetPosition - togglePosition;
                    if (Math.abs(diff) < 0.05f) {
                        togglePosition = targetPosition;
                        animationTimer.stop();
                    } else {
                        togglePosition += diff * 0.2f;
                    }
                    repaint();
                }
            });
            animationTimer.start();
        }
        
        private void fireActionPerformed() {
            ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "toggle");
            for (ActionListener listener : getListeners(ActionListener.class)) {
                listener.actionPerformed(event);
            }
        }
        
        public void addActionListener(ActionListener listener) {
            listenerList.add(ActionListener.class, listener);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            
            // Draw text
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            g2.setColor(TEXT_SECONDARY);
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            g2.drawString(text, 4, ((height - fm.getHeight()) / 2) + fm.getAscent());
            
            // Toggle switch dimensions
            int switchWidth = 36;
            int switchHeight = 18;
            int switchX = width - switchWidth - 4;
            int switchY = (height - switchHeight) / 2;
            
            // Draw switch background
            Color bgColor = selected ? PROGRESS_START : new Color(60, 60, 65);
            g2.setColor(bgColor);
            g2.fillRoundRect(switchX, switchY, switchWidth, switchHeight, switchHeight, switchHeight);
            
            // Draw switch knob
            int knobSize = 14;
            int knobPadding = 2;
            int knobX = switchX + knobPadding + (int) ((switchWidth - knobSize - knobPadding * 2) * togglePosition);
            int knobY = switchY + knobPadding;
            
            // Knob shadow
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillOval(knobX + 1, knobY + 1, knobSize, knobSize);
            
            // Knob
            g2.setColor(Color.WHITE);
            g2.fillOval(knobX, knobY, knobSize, knobSize);
            
            g2.dispose();
        }
    }
}