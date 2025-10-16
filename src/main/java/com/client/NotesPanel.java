package com.client;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class NotesPanel extends JPanel {
    private static final Color BACKGROUND_COLOR = new Color(0x1e, 0x1e, 0x1e);
    private static final Color TAB_COLOR = new Color(0x2a, 0x2a, 0x2a);
    private static final Color SELECTED_TAB_COLOR = new Color(0x00a2e8);
    private static final Color TEXT_COLOR = new Color(0x00a2e8);
    private static final Color BUTTON_COLOR = new Color(0x3a, 0x3a, 0x3a);
    private static final Color INFO_COLOR = new Color(0xaaaaaa);
    
    // Preload toolbar icons
    private static final ImageIcon[] TOOLBAR_ICONS = preloadToolbarIcons();
    
    private JTabbedPane tabbedPane;
    private List<NoteTab> noteTabs;
    private File notesDirectory;
    private Timer autoSaveTimer;
    
    public NotesPanel() {
        initializeNotesDirectory();
        initializeComponents();
        initializeUI();
        loadSavedNotesAndCreateDefault();
        startAutoSave();
    }
    
    private static ImageIcon[] preloadToolbarIcons() {
        String[] iconPaths = {
            "/icons/new_tab.png",
            "/icons/delete_tab.png", 
            "/icons/save.png",
            "/icons/search.png"
        };
        
        ImageIcon[] icons = new ImageIcon[iconPaths.length];
        for (int i = 0; i < iconPaths.length; i++) {
            try {
                java.net.URL iconUrl = NotesPanel.class.getResource(iconPaths[i]);
                if (iconUrl != null) {
                    Image img = new ImageIcon(iconUrl).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH);
                    icons[i] = new ImageIcon(img);
                } else {
                    icons[i] = createEmptyIcon(16);
                }
            } catch (Exception e) {
                icons[i] = createEmptyIcon(16);
            }
        }
        return icons;
    }
    
    private static ImageIcon createEmptyIcon(int size) {
        Image img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        return new ImageIcon(img);
    }
    
    private void initializeNotesDirectory() {
        try {
            notesDirectory = new File(System.getProperty("user.home"), ".junescapenotes");
            if (!notesDirectory.exists()) {
                notesDirectory.mkdirs();
            }
        } catch (Exception e) {
            notesDirectory = new File("notes");
            notesDirectory.mkdirs();
        }
    }
    
    private void initializeComponents() {
        noteTabs = new ArrayList<>();
        
        // Create and configure tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(BACKGROUND_COLOR);
        tabbedPane.setForeground(TEXT_COLOR);
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        
        // Style the tabbed pane
        UIManager.put("TabbedPane.selected", SELECTED_TAB_COLOR);
        UIManager.put("TabbedPane.background", TAB_COLOR);
        UIManager.put("TabbedPane.foreground", TEXT_COLOR);
        UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        
        JPanel toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private void loadSavedNotesAndCreateDefault() {
        loadSavedNotes();
        
        // Only create default tab if no saved notes exist
        if (noteTabs.isEmpty()) {
            addNewTab("General");
        }
    }
    
    private void startAutoSave() {
        autoSaveTimer = new Timer(30000, e -> saveAllNotes());
        autoSaveTimer.start();
    }
    
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        toolbar.setBackground(BACKGROUND_COLOR);
        toolbar.setBorder(new EmptyBorder(2, 2, 2, 2));
        
        JButton newTabBtn = createToolbarButton(TOOLBAR_ICONS[0], "New Tab");
        newTabBtn.addActionListener(e -> showNewTabDialog());
        
        JButton deleteTabBtn = createToolbarButton(TOOLBAR_ICONS[1], "Delete Tab");
        deleteTabBtn.addActionListener(e -> deleteCurrentTab());
        
        JButton saveBtn = createToolbarButton(TOOLBAR_ICONS[2], "Save All");
        saveBtn.addActionListener(e -> saveAllNotes());
        
        JButton searchBtn = createToolbarButton(TOOLBAR_ICONS[3], "Search");
        searchBtn.addActionListener(e -> showSearchDialog());
        
        toolbar.add(newTabBtn);
        toolbar.add(deleteTabBtn);
        toolbar.add(new JSeparator(SwingConstants.VERTICAL));
        toolbar.add(saveBtn);
        toolbar.add(searchBtn);
        
        return toolbar;
    }
    
    private JButton createToolbarButton(ImageIcon icon, String tooltip) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(24, 24));
        button.setBackground(BUTTON_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setContentAreaFilled(true);
                button.setOpaque(true);
                button.setBackground(SELECTED_TAB_COLOR);
            }
            public void mouseExited(MouseEvent e) {
                button.setContentAreaFilled(false);
                button.setOpaque(false);
            }
        });
        
        return button;
    }
    
    private void addNewTab(String name) {
        NoteTab noteTab = new NoteTab(name);
        noteTabs.add(noteTab);
        
        JPanel tabPanel = createTabPanel(noteTab);
        tabbedPane.addTab(name, tabPanel);
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        
        // Add close button to tab if we have more than one tab
        updateTabHeaders();
    }
    
    private void updateTabHeaders() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (noteTabs.size() > 1) {
                tabbedPane.setTabComponentAt(i, createTabHeader(noteTabs.get(i).getName(), noteTabs.get(i)));
            } else {
                tabbedPane.setTabComponentAt(i, null); // Remove close button from single tab
            }
        }
    }
    
    private JPanel createTabHeader(String name, NoteTab noteTab) {
        JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabHeader.setOpaque(false);
        
        JLabel nameLabel = new JLabel(name);
        nameLabel.setForeground(TEXT_COLOR);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        
        JButton closeBtn = new JButton("×");
        closeBtn.setPreferredSize(new Dimension(16, 16));
        closeBtn.setBackground(new Color(0, 0, 0, 0));
        closeBtn.setForeground(TEXT_COLOR);
        closeBtn.setBorder(BorderFactory.createEmptyBorder());
        closeBtn.setFocusPainted(false);
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        closeBtn.setContentAreaFilled(false);
        
        closeBtn.addActionListener(e -> deleteTab(noteTab));
        
        tabHeader.add(nameLabel);
        tabHeader.add(Box.createHorizontalStrut(4));
        tabHeader.add(closeBtn);
        
        return tabHeader;
    }
    
    private void deleteTab(NoteTab noteTab) {
        int index = noteTabs.indexOf(noteTab);
        if (index >= 0 && noteTabs.size() > 1) {
            noteTabs.remove(index);
            tabbedPane.removeTabAt(index);
            deleteNoteFile(noteTab.getName());
            updateTabHeaders(); // Update remaining tabs
        }
    }
    
    private JPanel createTabPanel(NoteTab noteTab) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BACKGROUND_COLOR);
        
        JTextArea textArea = createStyledTextArea(noteTab);
        noteTab.setTextArea(textArea);
        
        JScrollPane scrollPane = createScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel infoPanel = createInfoPanel(noteTab);
        panel.add(infoPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JTextArea createStyledTextArea(NoteTab noteTab) {
        JTextArea textArea = new JTextArea();
        textArea.setBackground(BACKGROUND_COLOR);
        textArea.setForeground(TEXT_COLOR);
        textArea.setCaretColor(TEXT_COLOR);
        textArea.setSelectionColor(SELECTED_TAB_COLOR);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        
        // Add document listener for modification tracking
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { noteTab.setModified(true); }
            public void removeUpdate(DocumentEvent e) { noteTab.setModified(true); }
            public void changedUpdate(DocumentEvent e) { noteTab.setModified(true); }
        });
        
        return textArea;
    }
    
    private JScrollPane createScrollPane(JTextArea textArea) {
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBackground(BACKGROUND_COLOR);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setBackground(TAB_COLOR);
        scrollPane.getHorizontalScrollBar().setBackground(TAB_COLOR);
        return scrollPane;
    }
    
    private JPanel createInfoPanel(NoteTab noteTab) {
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(TAB_COLOR);
        infoPanel.setBorder(new EmptyBorder(2, 8, 2, 8));
        
        JLabel infoLabel = new JLabel();
        infoLabel.setForeground(INFO_COLOR);
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        noteTab.setInfoLabel(infoLabel);
        
        infoPanel.add(infoLabel, BorderLayout.WEST);
        
        // Start word count timer
        Timer wordCountTimer = new Timer(1000, e -> updateInfoLabel(noteTab));
        wordCountTimer.start();
        noteTab.setWordCountTimer(wordCountTimer);
        
        return infoPanel;
    }
    
    private void updateInfoLabel(NoteTab noteTab) {
        if (noteTab.getTextArea() != null && noteTab.getInfoLabel() != null) {
            String text = noteTab.getTextArea().getText();
            int chars = text.length();
            int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
            int lines = noteTab.getTextArea().getLineCount();
            
            String info = String.format("Lines: %d | Words: %d | Chars: %d", lines, words, chars);
            if (noteTab.isModified()) {
                info += " | Modified";
            }
            
            noteTab.getInfoLabel().setText(info);
        }
    }
    
    private void showNewTabDialog() {
        String name = JOptionPane.showInputDialog(this, "Enter tab name:", "New Tab", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            name = name.trim();
            
            // Check if name already exists
            if (tabNameExists(name)) {
                JOptionPane.showMessageDialog(this, "Tab name already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            addNewTab(name);
        }
    }
    
    private boolean tabNameExists(String name) {
        return noteTabs.stream().anyMatch(tab -> tab.getName().equals(name));
    }
    
    private void deleteCurrentTab() {
        if (noteTabs.size() <= 1) {
            JOptionPane.showMessageDialog(this, "Cannot delete the last tab!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex >= 0) {
            NoteTab noteTab = noteTabs.get(selectedIndex);
            int result = JOptionPane.showConfirmDialog(this, 
                "Delete tab '" + noteTab.getName() + "'?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
            
            if (result == JOptionPane.YES_OPTION) {
                deleteTab(noteTab);
            }
        }
    }
    
    private void showSearchDialog() {
        String searchText = JOptionPane.showInputDialog(this, "Search for:", "Search Notes", JOptionPane.PLAIN_MESSAGE);
        if (searchText != null && !searchText.trim().isEmpty()) {
            searchInNotes(searchText.trim());
        }
    }
    
    private void searchInNotes(String searchText) {
        StringBuilder results = new StringBuilder();
        results.append("Search results for '").append(searchText).append("':\n\n");
        
        boolean found = false;
        for (NoteTab noteTab : noteTabs) {
            String content = noteTab.getTextArea().getText();
            if (content.toLowerCase().contains(searchText.toLowerCase())) {
                results.append("Found in tab: ").append(noteTab.getName()).append("\n");
                
                String[] lines = content.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].toLowerCase().contains(searchText.toLowerCase())) {
                        results.append("  Line ").append(i + 1).append(": ").append(lines[i]).append("\n");
                    }
                }
                results.append("\n");
                found = true;
            }
        }
        
        if (!found) {
            results.append("No matches found.");
        }
        
        showSearchResults(results.toString());
    }
    
    private void showSearchResults(String results) {
        JTextArea resultArea = new JTextArea(results);
        resultArea.setEditable(false);
        resultArea.setBackground(BACKGROUND_COLOR);
        resultArea.setForeground(TEXT_COLOR);
        resultArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        
        JOptionPane.showMessageDialog(this, scrollPane, "Search Results", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void saveAllNotes() {
        for (NoteTab noteTab : noteTabs) {
            saveNoteToFile(noteTab);
        }
        //System.out.println("All notes saved");
    }
    
    private void saveNoteToFile(NoteTab noteTab) {
        try {
            File noteFile = new File(notesDirectory, noteTab.getName() + ".txt");
            String content = noteTab.getTextArea().getText();
            
            StringBuilder fullContent = new StringBuilder();
            fullContent.append("# Junescape Note: ").append(noteTab.getName()).append("\n");
            fullContent.append("# Created: ").append(noteTab.getCreatedDate()).append("\n");
            fullContent.append("# Last Modified: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
            fullContent.append("# ");
            for (int i = 0; i < 50; i++) {
                fullContent.append("=");
            }
            fullContent.append("\n\n");
            fullContent.append(content);
            
            Files.write(noteFile.toPath(), fullContent.toString().getBytes());
            noteTab.setModified(false);
        } catch (IOException e) {
            //System.err.println("Failed to save note: " + noteTab.getName());
            e.printStackTrace();
        }
    }
    
    private void loadSavedNotes() {
        File[] noteFiles = notesDirectory.listFiles((dir, name) -> name.endsWith(".txt"));
        if (noteFiles != null) {
            for (File file : noteFiles) {
                loadNoteFromFile(file);
            }
        }
    }
    
    private void loadNoteFromFile(File file) {
        try {
            String fileName = file.getName().replace(".txt", "");
            String content = new String(Files.readAllBytes(file.toPath()));
            
            // Remove metadata header if present
            content = extractContentFromSavedFile(content);
            
            NoteTab noteTab = new NoteTab(fileName);
            noteTabs.add(noteTab);
            
            JPanel tabPanel = createTabPanel(noteTab);
            tabbedPane.addTab(fileName, tabPanel);
            
            noteTab.getTextArea().setText(content);
            noteTab.setModified(false);
            
        } catch (IOException e) {
            System.err.println("Failed to load note: " + file.getName());
        }
    }
    
    private String extractContentFromSavedFile(String content) {
        if (content.startsWith("# Junescape Note:")) {
            String[] lines = content.split("\n");
            StringBuilder actualContent = new StringBuilder();
            boolean foundSeparator = false;
            for (String line : lines) {
                if (foundSeparator) {
                    actualContent.append(line).append("\n");
                } else if (line.startsWith("# ====")) {
                    foundSeparator = true;
                }
            }
            return actualContent.toString().trim();
        }
        return content;
    }
    
    private void deleteNoteFile(String name) {
        try {
            File noteFile = new File(notesDirectory, name + ".txt");
            if (noteFile.exists()) {
                noteFile.delete();
            }
        } catch (Exception e) {
            System.err.println("Failed to delete note file: " + name);
        }
    }
    
    // Clean up resources when panel is destroyed
    public void cleanup() {
        if (autoSaveTimer != null) {
            autoSaveTimer.stop();
        }
        for (NoteTab tab : noteTabs) {
            if (tab.getWordCountTimer() != null) {
                tab.getWordCountTimer().stop();
            }
        }
    }
    
    // Inner class to represent a note tab
    private static class NoteTab {
        private final String name;
        private final String createdDate;
        private JTextArea textArea;
        private JLabel infoLabel;
        private Timer wordCountTimer;
        private boolean modified;
        
        public NoteTab(String name) {
            this.name = name;
            this.modified = false;
            this.createdDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        }
        
        // Getters and setters
        public String getName() { return name; }
        public String getCreatedDate() { return createdDate; }
        public JTextArea getTextArea() { return textArea; }
        public void setTextArea(JTextArea textArea) { this.textArea = textArea; }
        public JLabel getInfoLabel() { return infoLabel; }
        public void setInfoLabel(JLabel infoLabel) { this.infoLabel = infoLabel; }
        public Timer getWordCountTimer() { return wordCountTimer; }
        public void setWordCountTimer(Timer timer) { this.wordCountTimer = timer; }
        public boolean isModified() { return modified; }
        public void setModified(boolean modified) { this.modified = modified; }
    }
}