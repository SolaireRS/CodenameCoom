package com.client;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.client.definitions.ItemDefinition;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.lang.reflect.Method;
import java.awt.geom.RoundRectangle2D;

public class LootTrackerPanel extends JPanel {
    
    // Modern color scheme with depth
    private static final Color BACKGROUND_COLOR = new Color(0x2a2a2a);
    private static final Color PANEL_COLOR = new Color(0x1e1e1e);
    private static final Color PANEL_HOVER = new Color(0x252525);
    private static final Color TEXT_COLOR = new Color(0xe8e6e3);
    private static final Color ACCENT_COLOR = new Color(0x4a90e2);
    private static final Color ACCENT_HOVER = new Color(0x5ba3ff);
    private static final Color BORDER_COLOR = new Color(0x3a3a3a);
    private static final Color HOVER_COLOR = new Color(0x353535);
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 60);
    private static final Color GLOW_COLOR = new Color(0x4a, 0x90, 0xe2, 40);
    
    // Item slot colors with tiers
    private static final Color SLOT_BACKGROUND = new Color(0x2d2d2d);
    private static final Color SLOT_BORDER_DARK = new Color(0x1a1a1a);
    private static final Color SLOT_BORDER_LIGHT = new Color(0x404040);
    private static final Color SLOT_HOVER_GLOW = new Color(0x4a, 0x90, 0xe2, 80);
    
    // Value tier colors
    private static final Color TIER_BRONZE = new Color(0xCD7F32);
    private static final Color TIER_SILVER = new Color(0xC0C0C0);
    private static final Color TIER_GOLD = new Color(0xFFD700);
    private static final Color TIER_RAINBOW_START = new Color(0xFF00FF);
    private static final Color TIER_RAINBOW_END = new Color(0x00FFFF);
    
    // Visual constants
    private static final int PANEL_CORNER_RADIUS = 12;
    private static final int SLOT_CORNER_RADIUS = 6;
    private static final int SHADOW_OFFSET = 4;
    private static final int GLOW_SIZE = 8;
    private static final int SLOT_SIZE = 48;
    private static final int SLOT_PADDING = 2;
    
    // Logging constants
    private static final String LOG_DIRECTORY = "loot_logs";
    private static final DateTimeFormatter LOG_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Data storage - Current session only
    private List<LootDrop> allDrops = new ArrayList<>();
    private Map<String, LootSession> sessionData = new HashMap<>();
    private String currentCharacter = "Default";
    
    // UI Components
    private JPanel headerPanel;
    private JPanel contentPanel;
    private JScrollPane scrollPane;
    private JTextField searchField;
    private JLabel characterDisplay;
    private boolean isCollapsed = false;
    private boolean isTracking = true;
    
    // History window
    private JFrame historyWindow = null;
    
    // SPRITE SYSTEM - UNCHANGED (CRITICAL - DO NOT MODIFY)
    private static final Map<Integer, Object> spriteCache = new ConcurrentHashMap<>();
    private static final Map<Integer, List<ItemSlotComponent>> pendingComponents = new ConcurrentHashMap<>();
    private static Timer globalSpriteTimer = null;
    private static boolean isProcessingSprites = false;
    private static final Queue<Integer> spriteLoadQueue = new LinkedList<>();
    
    // Formatters
    private static final NumberFormat numberFormat = NumberFormat.getInstance();
    private static final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
    
    private static synchronized void requestSprite(int itemId, ItemSlotComponent component) {
        if (spriteCache.containsKey(itemId)) {
            SwingUtilities.invokeLater(() -> {
                component.setSprite(spriteCache.get(itemId));
            });
            return;
        }
        
        pendingComponents.computeIfAbsent(itemId, k -> new ArrayList<>()).add(component);
        
        if (!spriteLoadQueue.contains(itemId)) {
            spriteLoadQueue.offer(itemId);
        }
        
        if (!isProcessingSprites) {
           startSpriteProcessor();
        }
    }
    
    private static void startSpriteProcessor() {
        if (globalSpriteTimer != null) {
            globalSpriteTimer.stop();
        }
        
        isProcessingSprites = true;
        globalSpriteTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processNextSprite();
            }
        });
        globalSpriteTimer.start();
    }

    private static synchronized void processNextSprite() {
        if (spriteLoadQueue.isEmpty()) {
            isProcessingSprites = false;
            if (globalSpriteTimer != null) {
                globalSpriteTimer.stop();
            }
            return;
        }
        
        Integer itemId = spriteLoadQueue.poll();
        if (itemId != null) {
            loadSpriteForItemId(itemId);
        }
    }

    private static void loadSpriteForItemId(int itemId) {
        Client.queueSpriteLoad(() -> {
            try {
                Sprite sprite = ItemDefinition.getSprite(itemId, 1, 0, 0, false);
                
                if (sprite != null && sprite.myPixels != null && sprite.myPixels.length > 0) {
                    spriteCache.put(itemId, sprite);
                    
                    List<ItemSlotComponent> components = pendingComponents.remove(itemId);
                    if (components != null) {
                        final Object finalSprite = sprite;
                        SwingUtilities.invokeLater(() -> {
                            for (ItemSlotComponent component : components) {
                                component.setSprite(finalSprite);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Sprite load error: " + e.getMessage());
            }
        });
    }
    
    public LootTrackerPanel() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(new EmptyBorder(8, 8, 8, 8));
        
        currentCharacter = getCurrentCharacterName();
        
        initializeComponents();
        updateDisplay();
    }
    
    private void initializeComponents() {
        headerPanel = createHeader();
        add(headerPanel, BorderLayout.NORTH);
        
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BACKGROUND_COLOR);
        
        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setBackground(PANEL_COLOR);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createHeader() {
        ModernPanel header = new ModernPanel();
        header.setLayout(new BorderLayout());
        header.setBackground(PANEL_COLOR);
        header.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // First row - Title on LEFT, Buttons on RIGHT (no collapse button)
        JPanel firstRow = new JPanel(new BorderLayout(10, 0));
        firstRow.setOpaque(false);
        
        // Left side: Just Title
        JLabel titleLabel = new JLabel("Loot Tracker");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setBorder(new EmptyBorder(0, 4, 0, 0));
        
        // Right side: Buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightPanel.setOpaque(false);
        
        ModernButton historyBtn = new ModernButton("History");
        historyBtn.addActionListener(e -> openDropHistory());
        
        ModernButton pauseBtn = new ModernButton(isTracking ? "Pause" : "Resume");
        pauseBtn.addActionListener(e -> toggleTracking(pauseBtn));
        
        ModernButton clearBtn = new ModernButton("Clear");
        clearBtn.addActionListener(e -> clearAllData());
        
        rightPanel.add(historyBtn);
        rightPanel.add(pauseBtn);
        rightPanel.add(clearBtn);
        
        firstRow.add(titleLabel, BorderLayout.WEST);
        firstRow.add(rightPanel, BorderLayout.EAST);
        
        // Second row - Character display
        JPanel secondRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        secondRow.setOpaque(false);
        
        JLabel charLabel = new JLabel("Character:");
        charLabel.setForeground(TEXT_COLOR);
        charLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        characterDisplay = new JLabel(currentCharacter);
        characterDisplay.setForeground(Color.WHITE);
        characterDisplay.setFont(new Font("Segoe UI", Font.BOLD, 11));
        characterDisplay.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(new Color(0x4a4a4a), 1, 8),
            new EmptyBorder(3, 10, 3, 10)
        ));
        characterDisplay.setOpaque(true);
        characterDisplay.setBackground(new Color(0x353535));
        
        secondRow.add(charLabel);
        secondRow.add(characterDisplay);
        
        // Third row - Search
        JPanel searchPanel = createSearchPanel();
        
        // Stack rows vertically
        JPanel headerContent = new JPanel();
        headerContent.setLayout(new BoxLayout(headerContent, BoxLayout.Y_AXIS));
        headerContent.setOpaque(false);
        
        headerContent.add(firstRow);
        headerContent.add(Box.createVerticalStrut(6));
        headerContent.add(secondRow);
        headerContent.add(searchPanel);
        
        header.add(headerContent, BorderLayout.CENTER);
        
        return header;
    }
    
    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout(6, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        
        searchField = new JTextField();
        searchField.setBackground(SLOT_BACKGROUND);
        searchField.setForeground(TEXT_COLOR);
        searchField.setCaretColor(TEXT_COLOR);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            new EmptyBorder(6, 10, 6, 10)
        ));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { performSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { performSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { performSearch(); }
        });
        
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(TEXT_COLOR);
        searchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        
        return searchPanel;
    }
    
    private void toggleCollapse() {
        isCollapsed = !isCollapsed;
        scrollPane.setVisible(!isCollapsed);
        
        try {
            ModernPanel headerMod = (ModernPanel) headerPanel;
            JPanel headerContent = (JPanel) headerMod.getComponent(0);
            if (headerContent != null && headerContent.getComponentCount() > 0) {
                JPanel firstRow = (JPanel) headerContent.getComponent(0);
                if (firstRow != null) {
                    Component leftComponent = ((BorderLayout) firstRow.getLayout()).getLayoutComponent(firstRow, BorderLayout.WEST);
                    if (leftComponent instanceof JPanel) {
                        JPanel leftPanel = (JPanel) leftComponent;
                        if (leftPanel.getComponentCount() > 0) {
                            Component firstComponent = leftPanel.getComponent(0);
                            if (firstComponent instanceof ModernButton) {
                                ModernButton collapseBtn = (ModernButton) firstComponent;
                                collapseBtn.setText(isCollapsed ? "▶" : "▼");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        
        revalidate();
        repaint();
    }
    
    private void toggleTracking(JButton pauseBtn) {
        isTracking = !isTracking;
        pauseBtn.setText(isTracking ? "Pause" : "Resume");
    }
    
    private String getCurrentCharacterName() {
        try {
            if (Client.myPlayer != null && Client.myPlayer.displayName != null && !Client.myPlayer.displayName.trim().isEmpty()) {
                String playerName = Client.myPlayer.displayName.trim();
                return playerName;
            }
        } catch (Exception e) {
            System.err.println("Error accessing myPlayer.displayName: " + e.getMessage());
        }
        
        return "Unknown Character";
    }
    
    private void refreshCharacterName() {
        String detectedCharacter = getCurrentCharacterName();
        
        if (!detectedCharacter.equals(currentCharacter)) {
            boolean wasUnknown = currentCharacter.equals("Unknown Character");
            currentCharacter = detectedCharacter;
            
            if (characterDisplay != null) {
                characterDisplay.setText(currentCharacter);
            }
            
            if (!wasUnknown && !currentCharacter.equals("Unknown Character")) {
                allDrops.clear();
                sessionData.clear();
            }
            
            updateDisplay();
        }
    }
    
    private void openDropHistory() {
        if (historyWindow != null && historyWindow.isDisplayable()) {
            historyWindow.toFront();
            historyWindow.requestFocus();
            return;
        }
        
        historyWindow = new JFrame("Drop History");
        historyWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        historyWindow.setSize(1000, 700);
        historyWindow.setLocationRelativeTo(this);
        
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBackground(BACKGROUND_COLOR);
        historyPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        
        ModernPanel historyHeaderPanel = new ModernPanel();
        historyHeaderPanel.setLayout(new BorderLayout());
        historyHeaderPanel.setBackground(PANEL_COLOR);
        historyHeaderPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        
        JPanel topControlsPanel = new JPanel(new BorderLayout(10, 0));
        topControlsPanel.setOpaque(false);
        
        JPanel charSelectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        charSelectorPanel.setOpaque(false);
        
        JLabel historyCharLabel = new JLabel("Character:");
        historyCharLabel.setForeground(TEXT_COLOR);
        historyCharLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JComboBox<String> historyCharDropdown = new JComboBox<>();
        historyCharDropdown.setBackground(Color.WHITE);
        historyCharDropdown.setForeground(Color.BLACK);
        historyCharDropdown.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        historyCharDropdown.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historyCharDropdown.setPreferredSize(new Dimension(120, 26));
        
        populateHistoryCharacterDropdown(historyCharDropdown);
        
        charSelectorPanel.add(historyCharLabel);
        charSelectorPanel.add(Box.createHorizontalStrut(5));
        charSelectorPanel.add(historyCharDropdown);
        
        JPanel searchSection = new JPanel(new BorderLayout(8, 0));
        searchSection.setOpaque(false);
        
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setForeground(TEXT_COLOR);
        searchLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JTextField historySearchField = new JTextField();
        historySearchField.setBackground(SLOT_BACKGROUND);
        historySearchField.setForeground(TEXT_COLOR);
        historySearchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            new EmptyBorder(6, 10, 6, 10)
        ));
        historySearchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        searchSection.add(searchLabel, BorderLayout.WEST);
        searchSection.add(historySearchField, BorderLayout.CENTER);
        
        topControlsPanel.add(charSelectorPanel, BorderLayout.WEST);
        topControlsPanel.add(searchSection, BorderLayout.CENTER);
        
        historyHeaderPanel.add(topControlsPanel, BorderLayout.NORTH);
        
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        filterPanel.setOpaque(false);
        
        ModernButton todayBtn = new ModernButton("Today");
        ModernButton weekBtn = new ModernButton("This Week");
        ModernButton clearFilterBtn = new ModernButton("All Time");
        ModernButton deleteLogsBtn = new ModernButton("Delete All");
        deleteLogsBtn.setForeground(new Color(0xFF6B6B));
        
        filterPanel.add(todayBtn);
        filterPanel.add(weekBtn);
        filterPanel.add(clearFilterBtn);
        filterPanel.add(deleteLogsBtn);
        
        historyHeaderPanel.add(filterPanel, BorderLayout.CENTER);
        
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        statsPanel.setOpaque(false);
        
        JLabel statsLabel = new JLabel();
        statsLabel.setForeground(TEXT_COLOR.brighter());
        statsLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        
        statsPanel.add(statsLabel);
        historyHeaderPanel.add(statsPanel, BorderLayout.SOUTH);
        
        JTextArea historyResults = new JTextArea();
        historyResults.setBackground(BACKGROUND_COLOR);
        historyResults.setForeground(TEXT_COLOR);
        historyResults.setFont(new Font("Consolas", Font.PLAIN, 11));
        historyResults.setEditable(false);
        historyResults.setTabSize(4);
        historyResults.setLineWrap(false);
        
        JScrollPane historyScrollPane = new JScrollPane(historyResults);
        historyScrollPane.setBackground(BACKGROUND_COLOR);
        historyScrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        historyScrollPane.getVerticalScrollBar().setBackground(PANEL_COLOR);
        historyScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        historyScrollPane.getHorizontalScrollBar().setBackground(PANEL_COLOR);
        historyScrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        
        loadHistoricalDropsWithCharacter(historyResults, statsLabel, "", null, "All Characters");
        
        Timer searchTimer = new Timer(300, null);
        searchTimer.setRepeats(false);
        
        historySearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { scheduleSearch(); }
            
            private void scheduleSearch() {
                searchTimer.stop();
                searchTimer.addActionListener(evt -> {
                    String selectedChar = (String) historyCharDropdown.getSelectedItem();
                    loadHistoricalDropsWithCharacter(historyResults, statsLabel, historySearchField.getText().toLowerCase().trim(), null, selectedChar);
                });
                searchTimer.start();
            }
        });
        
        historyCharDropdown.addActionListener(e -> {
            String selectedChar = (String) historyCharDropdown.getSelectedItem();
            loadHistoricalDropsWithCharacter(historyResults, statsLabel, historySearchField.getText().toLowerCase().trim(), null, selectedChar);
        });
        
        todayBtn.addActionListener(e -> {
            String selectedChar = (String) historyCharDropdown.getSelectedItem();
            loadHistoricalDropsWithCharacter(historyResults, statsLabel, historySearchField.getText().toLowerCase().trim(), "today", selectedChar);
        });
        weekBtn.addActionListener(e -> {
            String selectedChar = (String) historyCharDropdown.getSelectedItem();
            loadHistoricalDropsWithCharacter(historyResults, statsLabel, historySearchField.getText().toLowerCase().trim(), "week", selectedChar);
        });
        clearFilterBtn.addActionListener(e -> {
            String selectedChar = (String) historyCharDropdown.getSelectedItem();
            loadHistoricalDropsWithCharacter(historyResults, statsLabel, historySearchField.getText().toLowerCase().trim(), null, selectedChar);
        });
        deleteLogsBtn.addActionListener(e -> deleteAllLogs(historyResults, statsLabel));
        
        historyPanel.add(historyHeaderPanel, BorderLayout.NORTH);
        historyPanel.add(historyScrollPane, BorderLayout.CENTER);
        
        historyWindow.add(historyPanel);
        historyWindow.setVisible(true);
    }
    
    private void loadHistoricalDropsWithCharacter(JTextArea resultsArea, JLabel statsLabel, String searchQuery, String timeFilter, String selectedCharacter) {
        SwingUtilities.invokeLater(() -> {
            resultsArea.setText("Loading historical drops...");
            statsLabel.setText("Loading...");
        });
        
        new Thread(() -> {
            try {
                List<HistoryEntry> allHistoricalEntries = new ArrayList<>();
                Path logDir = Paths.get(LOG_DIRECTORY);
                
                final LocalDate filterDate;
                if ("today".equals(timeFilter)) {
                    filterDate = LocalDate.now();
                } else if ("week".equals(timeFilter)) {
                    filterDate = LocalDate.now().minusDays(7);
                } else {
                    filterDate = null;
                }
                
                if (Files.exists(logDir)) {
                    Files.walk(logDir)
                         .filter(path -> path.toString().endsWith(".txt"))
                         .filter(path -> {
                             if ("All Characters".equals(selectedCharacter)) {
                                 return true;
                             }
                             String fileName = path.getFileName().toString();
                             String sanitizedChar = selectedCharacter.replaceAll("[^a-zA-Z0-9_-]", "_");
                             return fileName.contains("_" + sanitizedChar + "_");
                         })
                         .sorted((p1, p2) -> p2.getFileName().toString().compareTo(p1.getFileName().toString()))
                         .forEach(logFile -> {
                             try {
                                 List<String> lines = Files.readAllLines(logFile);
                                 for (String line : lines) {
                                     if (line.trim().isEmpty()) continue;
                                     
                                     HistoryEntry entry = parseHistoryEntry(line);
                                     if (entry == null) continue;
                                     
                                     if (!"All Characters".equals(selectedCharacter) && 
                                         !entry.character.equals(selectedCharacter)) {
                                         continue;
                                     }
                                     
                                     if (filterDate != null) {
                                         if ("today".equals(timeFilter) && !entry.date.equals(filterDate)) {
                                             continue;
                                         } else if ("week".equals(timeFilter) && entry.date.isBefore(filterDate)) {
                                             continue;
                                         }
                                     }
                                     
                                     if (!searchQuery.isEmpty()) {
                                         if (!entry.source.toLowerCase().contains(searchQuery) && 
                                             !entry.itemName.toLowerCase().contains(searchQuery) &&
                                             !entry.character.toLowerCase().contains(searchQuery)) {
                                             continue;
                                         }
                                     }
                                     
                                     allHistoricalEntries.add(entry);
                                 }
                             } catch (IOException e) {
                                 System.err.println("Failed to read log file: " + logFile);
                             }
                         });
                }
                
                StringBuilder formatted = new StringBuilder();
                
                if (allHistoricalEntries.isEmpty()) {
                    formatted.append("No drops found");
                    if (!searchQuery.isEmpty() || timeFilter != null || !"All Characters".equals(selectedCharacter)) {
                        formatted.append(" matching your criteria");
                    }
                    formatted.append(".\n");
                } else {
                    long totalValue = allHistoricalEntries.stream().mapToLong(e -> e.totalValue).sum();
                    int totalDrops = allHistoricalEntries.size();
                    Set<String> uniqueSources = allHistoricalEntries.stream().map(e -> e.source).collect(Collectors.toSet());
                    Set<String> uniqueCharacters = allHistoricalEntries.stream().map(e -> e.character).collect(Collectors.toSet());
                    
                    formatted.append(String.format("%-8s %-11s %-14s %-21s %15s %15s%n", 
                        "TIME", "CHARACTER", "SOURCE", "ITEM", "EACH", "TOTAL"));
                    formatted.append(repeatChar('─', 106)).append("\n");
                    
                    Map<LocalDate, List<HistoryEntry>> groupedByDate = allHistoricalEntries.stream()
                        .collect(Collectors.groupingBy(e -> e.date, LinkedHashMap::new, Collectors.toList()));
                    
                    for (Map.Entry<LocalDate, List<HistoryEntry>> dateGroup : groupedByDate.entrySet()) {
                        formatted.append("\n");
                        formatted.append(String.format("[%s]", dateGroup.getKey().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))));
                        formatted.append("\n");
                        formatted.append(repeatChar('═', 106)).append("\n");
                        
                        List<HistoryEntry> dayEntries = dateGroup.getValue();
                        dayEntries.sort((a, b) -> b.time.compareTo(a.time));
                        
                        for (HistoryEntry entry : dayEntries) {
                            formatted.append(formatHistoryEntryRowWithCharacter(entry)).append("\n");
                        }
                    }
                    
                    formatted.append("\n");
                    formatted.append(repeatChar('═', 106)).append("\n");
                    formatted.append(String.format("SUMMARY: %d drops from %d characters, %d sources • Total Value: %s", 
                        totalDrops, uniqueCharacters.size(), uniqueSources.size(), formatGold(totalValue)));
                }
                
                final String statsText = allHistoricalEntries.isEmpty() ? "No results" :
                    String.format("Found %d drops • Total: %s", 
                        allHistoricalEntries.size(), 
                        formatGold(allHistoricalEntries.stream().mapToLong(e -> e.totalValue).sum()));
                
                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText(formatted.toString());
                    resultsArea.setCaretPosition(0);
                    statsLabel.setText(statsText);
                });
                
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    resultsArea.setText("Error loading historical drops: " + e.getMessage());
                    statsLabel.setText("Error");
                });
            }
        }).start();
    }
    
    private void populateHistoryCharacterDropdown(JComboBox<String> dropdown) {
        try {
            Set<String> characters = new HashSet<>();
            characters.add("All Characters");
            
            Path logDir = Paths.get(LOG_DIRECTORY);
            if (Files.exists(logDir)) {
                Files.walk(logDir)
                     .filter(path -> path.toString().endsWith(".txt"))
                     .forEach(logFile -> {
                         try {
                             List<String> lines = Files.readAllLines(logFile);
                             for (String line : lines) {
                                 if (line.trim().isEmpty()) continue;
                                 HistoryEntry entry = parseHistoryEntry(line);
                                 if (entry != null && !entry.character.isEmpty()) {
                                     characters.add(entry.character);
                                 }
                             }
                         } catch (IOException e) {
                             System.err.println("Failed to read log file for characters: " + logFile);
                         }
                     });
            }
            
            for (String character : characters.stream().sorted().collect(Collectors.toList())) {
                dropdown.addItem(character);
            }
            
        } catch (Exception e) {
            dropdown.addItem("All Characters");
            System.err.println("Failed to populate character dropdown: " + e.getMessage());
        }
    }
    
    private static class HistoryEntry {
        final LocalDate date;
        final String time;
        final String character;
        final String source;
        final String itemName;
        final int quantity;
        final long valuePerItem;
        final long totalValue;
        
        HistoryEntry(LocalDate date, String time, String character, String source, String itemName, int quantity, long valuePerItem, long totalValue) {
            this.date = date;
            this.time = time;
            this.character = character;
            this.source = source;
            this.itemName = itemName;
            this.quantity = quantity;
            this.valuePerItem = valuePerItem;
            this.totalValue = totalValue;
        }
    }
    
    private HistoryEntry parseHistoryEntry(String line) {
        try {
            String[] parts = line.split(" \\| ");
            if (parts.length < 7) return null;
            
            String timestamp = parts[0];
            String character = parts[1];
            String source = parts[2];
            String itemPart = parts[4];
            String valuePart = parts[5];
            String totalPart = parts[6];
            
            LocalDate date = LocalDate.parse(timestamp.substring(0, 10));
            String time = timestamp.substring(11);
            
            int xIndex = itemPart.lastIndexOf(" x");
            if (xIndex <= 0) return null;
            
            String itemName = itemPart.substring(0, xIndex);
            int quantity = Integer.parseInt(itemPart.substring(xIndex + 2));
            
            long valuePerItem = 0;
            if (valuePart.endsWith(" gp each")) {
                valuePerItem = Long.parseLong(valuePart.substring(0, valuePart.length() - 8));
            }
            
            long totalValue = 0;
            if (totalPart.endsWith(" gp total")) {
                totalValue = Long.parseLong(totalPart.substring(0, totalPart.length() - 9));
            }
            
            return new HistoryEntry(date, time, character, source, itemName, quantity, valuePerItem, totalValue);
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private String formatHistoryEntryRowWithCharacter(HistoryEntry entry) {
        String truncatedChar = entry.character.length() > 10 ? entry.character.substring(0, 7) + "..." : entry.character;
        String truncatedSource = entry.source.length() > 13 ? entry.source.substring(0, 10) + "..." : entry.source;
        
        String itemDisplay;
        if (entry.quantity > 1) {
            String baseItem = entry.itemName.length() > 16 ? entry.itemName.substring(0, 13) + "..." : entry.itemName;
            itemDisplay = String.format("%s x%d", baseItem, entry.quantity);
            if (itemDisplay.length() > 20) {
                baseItem = entry.itemName.length() > 12 ? entry.itemName.substring(0, 9) + "..." : entry.itemName;
                itemDisplay = String.format("%s x%d", baseItem, entry.quantity);
            }
        } else {
            itemDisplay = entry.itemName.length() > 20 ? entry.itemName.substring(0, 17) + "..." : entry.itemName;
        }
        
        return String.format("%-8s %-10s %-13s %-20s %15s %15s", 
            entry.time,
            truncatedChar,
            truncatedSource,
            itemDisplay,
            formatGold(entry.valuePerItem),
            formatGold(entry.totalValue));
    }
    
    private void deleteAllLogs(JTextArea resultsArea, JLabel statsLabel) {
        int result = JOptionPane.showConfirmDialog(
            historyWindow,
            "Are you sure you want to DELETE ALL log files?\n\nThis will permanently remove ALL historical loot data!\n\nThis action cannot be undone.",
            "Delete All Logs - WARNING",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            int secondResult = JOptionPane.showConfirmDialog(
                historyWindow,
                "FINAL CONFIRMATION\n\nYou are about to permanently delete ALL loot logs.\nType YES if you are absolutely certain:",
                "Final Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE
            );
            
            if (secondResult == JOptionPane.YES_OPTION) {
                try {
                    Path logDir = Paths.get(LOG_DIRECTORY);
                    if (Files.exists(logDir)) {
                        Files.walk(logDir)
                             .filter(path -> path.toString().endsWith(".txt"))
                             .forEach(path -> {
                                 try {
                                     Files.delete(path);
                                 } catch (IOException e) {
                                     System.err.println("Failed to delete: " + path.getFileName());
                                 }
                             });
                    }
                    
                    SwingUtilities.invokeLater(() -> {
                        resultsArea.setText("All log files have been deleted successfully.\n\nThe history is now empty.");
                        statsLabel.setText("All logs deleted");
                    });
                    
                    JOptionPane.showMessageDialog(
                        historyWindow,
                        "All log files have been deleted successfully.",
                        "Deletion Complete",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                    
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                        historyWindow,
                        "Error deleting log files: " + e.getMessage(),
                        "Deletion Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }
    }
    
    public void recordDrop(String source, int droppedItemId, int quantity, long itemValue) {
        if (!isTracking) {
            return;
        }
        
        refreshCharacterName();
        
        String itemName = getItemNameFromLiveId(droppedItemId);
        
        LootDrop drop = new LootDrop(source, droppedItemId, itemName, quantity, itemValue, LocalTime.now());
        allDrops.add(drop);
        
        updateSession(drop);
        logDropToFile(drop);
        
        SwingUtilities.invokeLater(this::updateDisplay);
    }
    
    private String getItemNameFromLiveId(int itemId) {
        try {
            Class<?> itemDefClass = Class.forName("com.client.definitions.ItemDefinition");
            Method forIdMethod = itemDefClass.getMethod("forID", int.class);
            
            Object itemDef = forIdMethod.invoke(null, itemId);
            if (itemDef != null) {
                java.lang.reflect.Field nameField = itemDefClass.getField("name");
                String name = (String) nameField.get(itemDef);
                if (name != null && !name.trim().isEmpty()) {
                    return name;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to get name for ID " + itemId + ": " + e.getMessage());
        }
        
        return "Item " + itemId;
    }
    
    private void updateSession(LootDrop drop) {
        LootSession session = sessionData.get(drop.source);
        if (session == null) {
            session = new LootSession(drop.source);
            sessionData.put(drop.source, session);
        }
        
        session.addDrop(drop);
    }
    
    // ITEM SLOT WITH VISUAL POLISH
    private class ItemSlotComponent extends JPanel {
        private final int liveItemId;
        private final String itemName;
        private final int quantity;
        private final long totalValue;
        private Object itemSprite = null;
        private boolean hovering = false;
        private float pulseAlpha = 0.0f;
        private Timer pulseTimer;
        
        public ItemSlotComponent(int liveItemId, String itemName, int quantity, long totalValue) {
            this.liveItemId = liveItemId;
            this.itemName = itemName != null ? itemName : "Unknown Item";
            this.quantity = quantity;
            this.totalValue = totalValue;
            
            try {
                setLayout(null);
                setOpaque(false);
                
                setPreferredSize(new Dimension(SLOT_SIZE, SLOT_SIZE));
                setMaximumSize(new Dimension(SLOT_SIZE, SLOT_SIZE));
                setMinimumSize(new Dimension(SLOT_SIZE, SLOT_SIZE));
                
                setToolTipText(formatTooltip());
                
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovering = true;
                        repaint();
                    }
                    
                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovering = false;
                        repaint();
                    }
                });
                
                if (liveItemId > 0) {
                    requestSprite(liveItemId, this);
                }
                
                // Pulse animation on creation
                startPulseAnimation();
                
            } catch (Exception e) {
                System.err.println("Error initializing ItemSlotComponent for ID " + liveItemId + ": " + e.getMessage());
                setPreferredSize(new Dimension(SLOT_SIZE, SLOT_SIZE));
            }
        }

        private void startPulseAnimation() {
            if (pulseTimer != null && pulseTimer.isRunning()) {
                pulseTimer.stop();
            }
            
            pulseTimer = new Timer(30, null);
            final long startTime = System.currentTimeMillis();
            
            pulseTimer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float progress = elapsed / 400.0f;
                    
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

        public void setSprite(Object sprite) {
            this.itemSprite = sprite;
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            try {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int width = getWidth();
                int height = getHeight();
                
                // Draw shadow
                g2d.setColor(SHADOW_COLOR);
                g2d.fillRoundRect(SHADOW_OFFSET, SHADOW_OFFSET, width - SHADOW_OFFSET, 
                                height - SHADOW_OFFSET, SLOT_CORNER_RADIUS, SLOT_CORNER_RADIUS);
                
                // Draw hover glow
                if (hovering) {
                    for (int i = GLOW_SIZE; i > 0; i--) {
                        float alpha = (1.0f - (i / (float) GLOW_SIZE)) * 0.4f;
                        g2d.setColor(new Color(SLOT_HOVER_GLOW.getRed(), SLOT_HOVER_GLOW.getGreen(), 
                                             SLOT_HOVER_GLOW.getBlue(), (int) (alpha * 255)));
                        g2d.fillRoundRect(-i, -i, width + (i * 2), height + (i * 2), 
                                       SLOT_CORNER_RADIUS + i, SLOT_CORNER_RADIUS + i);
                    }
                }
                
                // Draw background with 3D bevel effect
                g2d.setColor(SLOT_BACKGROUND);
                g2d.fillRoundRect(0, 0, width, height, SLOT_CORNER_RADIUS, SLOT_CORNER_RADIUS);
                
                // Top-left highlight (bevel effect)
                g2d.setColor(SLOT_BORDER_LIGHT);
                g2d.drawLine(2, 2, width - 3, 2);
                g2d.drawLine(2, 2, 2, height - 3);
                
                // Bottom-right shadow (bevel effect)
                g2d.setColor(SLOT_BORDER_DARK);
                g2d.drawLine(2, height - 2, width - 2, height - 2);
                g2d.drawLine(width - 2, 2, width - 2, height - 2);
                
                // Draw value tier border
                Color tierColor = getValueTierColor(totalValue);
                if (tierColor != null) {
                    g2d.setColor(tierColor);
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawRoundRect(1, 1, width - 3, height - 3, SLOT_CORNER_RADIUS, SLOT_CORNER_RADIUS);
                }
                
                // Draw sprite or fallback
                if (itemSprite != null && drawItemSprite(g2d)) {
                    // Add shine overlay on sprite
                    GradientPaint shine = new GradientPaint(
                        0, 0, new Color(255, 255, 255, 60),
                        0, height / 2, new Color(255, 255, 255, 0)
                    );
                    g2d.setPaint(shine);
                    g2d.fillRoundRect(4, 4, width - 8, height / 2, SLOT_CORNER_RADIUS, SLOT_CORNER_RADIUS);
                } else {
                    drawTextFallback(g2d);
                }
                
                // Draw quantity overlay
                if (quantity > 1) {
                    drawQuantityOverlay(g2d);
                }
                
                // Draw pulse effect
                if (pulseAlpha > 0) {
                    g2d.setColor(new Color(ACCENT_COLOR.getRed(), ACCENT_COLOR.getGreen(), 
                                         ACCENT_COLOR.getBlue(), (int) (pulseAlpha * 100)));
                    g2d.fillRoundRect(0, 0, width, height, SLOT_CORNER_RADIUS, SLOT_CORNER_RADIUS);
                }
                
                g2d.dispose();
            } catch (Exception e) {
                System.err.println("Error painting ItemSlotComponent: " + e.getMessage());
            }
        }
        
        private Color getValueTierColor(long value) {
            if (value >= 1000000) {
                return TIER_GOLD;
            } else if (value >= 100000) {
                return TIER_SILVER;
            } else if (value >= 10000) {
                return TIER_BRONZE;
            }
            return null;
        }
        
        private boolean drawItemSprite(Graphics2D g2d) {
            if (itemSprite == null) {
                return false;
            }
            
            try {
                Sprite sprite = (Sprite) itemSprite;
                
                int width = sprite.myWidth;
                int height = sprite.myHeight;
                int[] pixels = sprite.myPixels;
                
                if (pixels == null || width <= 0 || height <= 0) {
                    return false;
                }
                
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                
                int[] convertedPixels = new int[pixels.length];
                for (int i = 0; i < pixels.length; i++) {
                    int pixel = pixels[i];
                    if (pixel == 0) {
                        convertedPixels[i] = 0;
                    } else {
                        convertedPixels[i] = 0xFF000000 | pixel;
                    }
                }
                
                image.setRGB(0, 0, width, height, convertedPixels, 0, width);
                
                int x = (SLOT_SIZE - width) / 2;
                int y = (SLOT_SIZE - height) / 2;
                g2d.drawImage(image, x, y, null);
                
                return true;
                
            } catch (Exception e) {
                return false;
            }
        }

        private void drawTextFallback(Graphics2D g2d) {
            try {
                String text = itemName.length() >= 2 ? 
                    itemName.substring(0, 2).toUpperCase() : itemName.toUpperCase();
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2d.setColor(new Color(0xFFFF00));
                
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(text)) / 2;
                int textY = (getHeight() + fm.getAscent()) / 2 - 2;
                
                // Text shadow
                g2d.setColor(new Color(0, 0, 0, 150));
                g2d.drawString(text, textX + 1, textY + 1);
                
                g2d.setColor(new Color(0xFFFF00));
                g2d.drawString(text, textX, textY);
            } catch (Exception e) {
                System.err.println("Error drawing text fallback: " + e.getMessage());
            }
        }
        
        private void drawQuantityOverlay(Graphics2D g2d) {
            try {
                if (quantity <= 1) return;
                
                String qtyText = formatQuantity(quantity);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 11));
                
                FontMetrics fm = g2d.getFontMetrics();
                if (fm != null) {
                    int qtyX = getWidth() - fm.stringWidth(qtyText) - 3;
                    int qtyY = getHeight() - 3;
                    
                    // Rounded background badge
                    int badgeWidth = fm.stringWidth(qtyText) + 6;
                    int badgeHeight = fm.getHeight() - 2;
                    int badgeX = qtyX - 3;
                    int badgeY = qtyY - badgeHeight + 2;
                    
                    g2d.setColor(new Color(0, 0, 0, 180));
                    g2d.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 6, 6);
                    
                    // Text shadow
                    g2d.setColor(new Color(0, 0, 0, 200));
                    g2d.drawString(qtyText, qtyX + 1, qtyY + 1);
                    
                    // Text
                    g2d.setColor(Color.YELLOW);
                    g2d.drawString(qtyText, qtyX, qtyY);
                }
            } catch (Exception e) {
                System.err.println("Error drawing quantity overlay: " + e.getMessage());
            }
        }
        
        private String formatTooltip() {
            try {
                String safeName = (itemName != null && !itemName.trim().isEmpty()) ? itemName : "Unknown Item";
                return String.format("<html><b>%s</b><br>Quantity: %,d<br>Total Value: %s<br>ID: %d</html>", 
                                   safeName, quantity, formatGold(totalValue), liveItemId);
            } catch (Exception e) {
                return "Item " + liveItemId;
            }
        }
    }
    
    private void updateDisplay() {
        contentPanel.removeAll();
        
        if (sessionData.isEmpty()) {
            JLabel emptyLabel = new JLabel("No loot recorded this session");
            emptyLabel.setForeground(TEXT_COLOR.darker());
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
            contentPanel.add(emptyLabel);
        } else {
            for (LootSession session : sessionData.values()) {
                contentPanel.add(createSessionPanel(session));
                contentPanel.add(Box.createVerticalStrut(6));
            }
        }
        
        contentPanel.revalidate();
        contentPanel.repaint();
        
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }
    
    private JPanel createSessionPanel(LootSession session) {
        ModernPanel panel = new ModernPanel();
        panel.setLayout(new BorderLayout());
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(new EmptyBorder(2, 2, 2, 2));
        
        ModernPanel header = new ModernPanel();
        header.setLayout(new BorderLayout());
        header.setBackground(PANEL_COLOR);
        header.setBorder(new EmptyBorder(8, 10, 8, 10));
        
        JLabel nameLabel = new JLabel(session.source + " × " + session.getKillCount());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(TEXT_COLOR);
        
        JLabel valueLabel = new JLabel(formatGold(session.getTotalValue()));
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valueLabel.setForeground(session.getTotalValue() > 100000 ? 
            new Color(0x4CAF50) : TEXT_COLOR);
        
        header.add(nameLabel, BorderLayout.WEST);
        header.add(valueLabel, BorderLayout.EAST);
        
        Map<Integer, StackedItem> stackedItems = session.getStackedItems();
        JPanel itemGrid = createItemGrid(stackedItems);
        
        panel.add(header, BorderLayout.NORTH);
        panel.add(itemGrid, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createItemGrid(Map<Integer, StackedItem> stackedItems) {
        JPanel gridContainer = new JPanel(new BorderLayout());
        gridContainer.setBackground(PANEL_COLOR);
        gridContainer.setBorder(new EmptyBorder(8, 8, 8, 8));
        
        if (stackedItems.isEmpty()) {
            return gridContainer;
        }
        
        List<StackedItem> sortedItems = new ArrayList<>(stackedItems.values());
        sortedItems.sort((a, b) -> Long.compare(b.getTotalValue(), a.getTotalValue()));
        
        JPanel itemGridPanel = new JPanel(new GridBagLayout());
        itemGridPanel.setBackground(PANEL_COLOR);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(SLOT_PADDING, SLOT_PADDING, SLOT_PADDING, SLOT_PADDING);
        gbc.anchor = GridBagConstraints.CENTER;
        
        int itemsPerRow = 5;
        int row = 0;
        int col = 0;
        
        for (int i = 0; i < sortedItems.size(); i++) {
            StackedItem item = sortedItems.get(i);
            
            try {
                ItemSlotComponent slot = new ItemSlotComponent(
                    item.itemId, item.itemName, item.totalQuantity, item.getTotalValue()
                );
                
                gbc.gridx = col;
                gbc.gridy = row;
                itemGridPanel.add(slot, gbc);
                
            } catch (Exception e) {
                System.err.println("Error creating item slot for ID " + item.itemId + ": " + e.getMessage());
                
                JPanel fallbackSlot = new JPanel();
                fallbackSlot.setBackground(SLOT_BACKGROUND);
                fallbackSlot.setPreferredSize(new Dimension(SLOT_SIZE, SLOT_SIZE));
                fallbackSlot.setMinimumSize(new Dimension(SLOT_SIZE, SLOT_SIZE));
                fallbackSlot.setMaximumSize(new Dimension(SLOT_SIZE, SLOT_SIZE));
                fallbackSlot.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
                
                gbc.gridx = col;
                gbc.gridy = row;
                itemGridPanel.add(fallbackSlot, gbc);
            }
            
            col++;
            if (col >= itemsPerRow) {
                col = 0;
                row++;
            }
        }
        
        JPanel centeringPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        centeringPanel.setBackground(PANEL_COLOR);
        centeringPanel.add(itemGridPanel);
        
        gridContainer.add(centeringPanel, BorderLayout.CENTER);
        return gridContainer;
    }
    
    private void performSearch() {
        String query = searchField.getText().toLowerCase().trim();
        
        if (query.isEmpty()) {
            updateDisplay();
            return;
        }
        
        Map<String, LootSession> filteredSessions = new HashMap<>();
        
        for (LootSession session : sessionData.values()) {
            if (session.source.toLowerCase().contains(query)) {
                filteredSessions.put(session.source, session);
            } else {
                boolean hasMatchingItem = session.getAllDrops().stream()
                    .anyMatch(drop -> drop.itemName.toLowerCase().contains(query));
                
                if (hasMatchingItem) {
                    filteredSessions.put(session.source, session);
                }
            }
        }
        
        displayFilteredSessions(filteredSessions);
    }
    
    private void displayFilteredSessions(Map<String, LootSession> filteredSessions) {
        contentPanel.removeAll();
        
        if (filteredSessions.isEmpty()) {
            JLabel emptyLabel = new JLabel("No matching loot found in current session");
            emptyLabel.setForeground(TEXT_COLOR.darker());
            emptyLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            emptyLabel.setBorder(new EmptyBorder(20, 10, 20, 10));
            contentPanel.add(emptyLabel);
        } else {
            for (LootSession session : filteredSessions.values()) {
                contentPanel.add(createSessionPanel(session));
                contentPanel.add(Box.createVerticalStrut(6));
            }
        }
        
        contentPanel.revalidate();
        contentPanel.repaint();
    }
    
    private void logDropToFile(LootDrop drop) {
        try {
            Path logDir = Paths.get(LOG_DIRECTORY);
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            
            String sanitizedCharName = currentCharacter.replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = "loot_" + sanitizedCharName + "_" + LocalDate.now().format(FILE_TIMESTAMP) + ".txt";
            Path logFile = logDir.resolve(fileName);
            
            String logEntry = String.format("%s | %s | %s | ID:%d | %s x%d | %d gp each | %d gp total%n",
                LocalDateTime.now().format(LOG_TIMESTAMP),
                currentCharacter,
                drop.source,
                drop.itemId,
                drop.itemName,
                drop.quantity,
                drop.valuePerItem,
                drop.getTotalValue()
            );
            
            Files.write(logFile, logEntry.getBytes(), 
                       StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                       
        } catch (IOException e) {
            System.err.println("Failed to log drop: " + e.getMessage());
        }
    }
    
    private String repeatChar(char ch, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }
    
    private String formatQuantity(int quantity) {
        if (quantity >= 1000000) {
            return (quantity / 1000000) + "M";
        } else if (quantity >= 1000) {
            return (quantity / 1000) + "K";
        }
        return String.valueOf(quantity);
    }
    
    private String formatGold(long value) {
        if (value >= 1000000) {
            return String.format("%.1fM gp", value / 1000000.0);
        } else if (value >= 1000) {
            return String.format("%.1fK gp", value / 1000.0);
        }
        return value + " gp";
    }
    
    private void clearAllData() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to clear current session data?\n(Historical data will remain saved)",
            "Clear Current Session",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            allDrops.clear();
            sessionData.clear();
            updateDisplay();
        }
    }
    
    // Modern panel with rounded corners and shadow
    private class ModernPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Drop shadow
            g2d.setColor(SHADOW_COLOR);
            g2d.fillRoundRect(SHADOW_OFFSET, SHADOW_OFFSET, 
                            getWidth() - SHADOW_OFFSET, getHeight() - SHADOW_OFFSET, 
                            PANEL_CORNER_RADIUS, PANEL_CORNER_RADIUS);
            
            // Background
            g2d.setColor(getBackground());
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 
                            PANEL_CORNER_RADIUS, PANEL_CORNER_RADIUS);
            
            // Subtle gradient overlay
            GradientPaint gradient = new GradientPaint(
                0, 0, new Color(255, 255, 255, 5),
                0, getHeight(), new Color(0, 0, 0, 10)
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 
                            PANEL_CORNER_RADIUS, PANEL_CORNER_RADIUS);
            
            // Border
            g2d.setColor(BORDER_COLOR);
            g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 
                            PANEL_CORNER_RADIUS, PANEL_CORNER_RADIUS);
            
            g2d.dispose();
        }
    }
    
    // Modern button with hover animation
    private class ModernButton extends JButton {
        private Color currentBg = ACCENT_COLOR;
        private Timer animationTimer;
        
        public ModernButton(String text) {
            super(text);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 10));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(4, 10, 4, 10));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    animateToColor(ACCENT_HOVER);
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    animateToColor(ACCENT_COLOR);
                }
            });
        }
        
        private void animateToColor(Color target) {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }
            
            final Color start = currentBg;
            animationTimer = new Timer(20, null);
            final long startTime = System.currentTimeMillis();
            
            animationTimer.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float progress = Math.min(1.0f, elapsed / 200.0f);
                    
                    int r = (int) (start.getRed() + (target.getRed() - start.getRed()) * progress);
                    int g = (int) (start.getGreen() + (target.getGreen() - start.getGreen()) * progress);
                    int b = (int) (start.getBlue() + (target.getBlue() - start.getBlue()) * progress);
                    
                    currentBg = new Color(r, g, b);
                    repaint();
                    
                    if (progress >= 1.0f) {
                        animationTimer.stop();
                    }
                }
            });
            animationTimer.start();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Button background
            g2d.setColor(currentBg);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            
            // Text
            g2d.setColor(getForeground());
            g2d.setFont(getFont());
            FontMetrics fm = g2d.getFontMetrics();
            int textX = (getWidth() - fm.stringWidth(getText())) / 2;
            int textY = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(getText(), textX, textY);
            
            g2d.dispose();
        }
    }
    
    // Modern scrollbar
    private class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(0x5a, 0x5a, 0x5a);
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
                               thumbBounds.width - 6, thumbBounds.height - 4, 8, 8);
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
    
    // DATA CLASSES (UNCHANGED)
    public static class LootDrop {
        public final String source;
        public final int itemId;
        public final String itemName;
        public final int quantity;
        public final long valuePerItem;
        public final LocalTime timestamp;
        
        public LootDrop(String source, int itemId, String itemName, int quantity, long valuePerItem, LocalTime timestamp) {
            this.source = source;
            this.itemId = itemId;
            this.itemName = itemName;
            this.quantity = quantity;
            this.valuePerItem = valuePerItem;
            this.timestamp = timestamp;
        }
        
        public long getTotalValue() {
            return valuePerItem * quantity;
        }
    }
    
    public static class StackedItem {
        public final int itemId;
        public final String itemName;
        public int totalQuantity;
        public final long valuePerItem;
        
        public StackedItem(int itemId, String itemName, int quantity, long valuePerItem) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.totalQuantity = quantity;
            this.valuePerItem = valuePerItem;
        }
        
        public void addQuantity(int quantity) {
            this.totalQuantity += quantity;
        }
        
        public long getTotalValue() {
            return valuePerItem * totalQuantity;
        }
    }
    
    public static class LootSession {
        public final String source;
        private final List<LootDrop> drops;
        private final Map<Integer, StackedItem> stackedItems;
        
        public LootSession(String source) {
            this.source = source;
            this.drops = Collections.synchronizedList(new ArrayList<>());
            this.stackedItems = new ConcurrentHashMap<>();
        }
        
        public void addDrop(LootDrop drop) {
            drops.add(drop);
            
            StackedItem existing = stackedItems.get(drop.itemId);
            if (existing != null) {
                existing.addQuantity(drop.quantity);
            } else {
                stackedItems.put(drop.itemId, new StackedItem(drop.itemId, drop.itemName, drop.quantity, drop.valuePerItem));
            }
        }
        
        public int getKillCount() {
            return drops.size();
        }
        
        public long getTotalValue() {
            List<LootDrop> dropsCopy;
            synchronized(drops) {
                dropsCopy = new ArrayList<>(drops);
            }
            return dropsCopy.stream().mapToLong(LootDrop::getTotalValue).sum();
        }
        
        public Map<Integer, StackedItem> getStackedItems() {
            return new LinkedHashMap<>(stackedItems);
        }
        
        public List<LootDrop> getAllDrops() {
            synchronized(drops) {
                return new ArrayList<>(drops);
            }
        }
    }
    
    // Custom rounded border for thin elegant borders
    private class RoundedBorder implements javax.swing.border.Border {
        private Color color;
        private int thickness;
        private int radius;
        
        public RoundedBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }
        
        @Override
        public boolean isBorderOpaque() {
            return false;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(thickness));
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2d.dispose();
        }
    }
}