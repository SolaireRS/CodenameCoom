package com.client.plugins.tilemarkers;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Tile Markers Settings Panel - Clean version matching Ammo plugin
 */
public class TileMarkersSettingsPanel extends JPanel {
    
    private final Color BACKGROUND_COLOR = new Color(0x2b, 0x2b, 0x2b);
    private final Color TEXT_COLOR = new Color(0x00, 0xa2, 0xe8);
    private final Color MUTED_TEXT = new Color(0x9f, 0x9f, 0x9f);
    private final Color BORDER_COLOR = new Color(0x3c, 0x3c, 0x3c);
    
    private TileMarkersPlugin plugin;
    private JCheckBox markingModeToggle;
    private JLabel markerCountLabel;
    private JTextField labelField;
    
    public TileMarkersSettingsPanel() {
        setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
    }
    
    public void setPlugin(TileMarkersPlugin plugin) {
        this.plugin = plugin;
        createSlimUI();
    }
    
    private void createSlimUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BACKGROUND_COLOR);
        
        // Simple title - no extra header panel or duplicate back button
        JLabel titleLabel = new JLabel("Tile Markers Settings");
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);
        
        // Settings list - compact rows
        mainPanel.add(createSlimRow("Enable Plugin", createToggleButton(plugin.isEnabled(), e -> {
            if (((JCheckBox)e.getSource()).isSelected()) {
                plugin.enable();
            } else {
                plugin.disable();
            }
            updateMarkerCountDisplay();
        })));
        
        markingModeToggle = createToggleButton(plugin.isMarkingMode(), e -> {
            plugin.toggleMarkingMode();
        });
        mainPanel.add(createSlimRow("Marking Mode", markingModeToggle));
        
        mainPanel.add(createSlimRow("Marker Color", createColorDropdown(
            plugin.getCurrentMarkerColor(),
            color -> plugin.setCurrentMarkerColor(color)
        )));
        
        labelField = createLabelTextField();
        mainPanel.add(createSlimRow("Default Label", labelField));
        
        markerCountLabel = new JLabel("0 markers");
        markerCountLabel.setForeground(TEXT_COLOR);
        markerCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        updateMarkerCountDisplay();
        mainPanel.add(createSlimRow("Total Markers", markerCountLabel));
        
        mainPanel.add(createSlimRow("Clear All", createActionButton("Clear All", e -> {
            if (plugin != null) {
                int result = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to clear all tile markers?",
                    "Confirm Clear All",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );
                if (result == JOptionPane.YES_OPTION) {
                    plugin.clearAllMarkers();
                    updateMarkerCountDisplay();
                }
            }
        })));
        
        JPanel instructionsPanel = createInstructionsPanel();
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(instructionsPanel);
        
        add(mainPanel, BorderLayout.NORTH);
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
        toggle.setForeground(TEXT_COLOR);
        toggle.addActionListener(listener);
        
        toggle.setUI(new javax.swing.plaf.basic.BasicCheckBoxUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                JCheckBox cb = (JCheckBox) c;
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(cb.getBackground());
                g2.fillRect(0, 0, 16, 16);
                
                g2.setColor(cb.isSelected() ? TEXT_COLOR : MUTED_TEXT);
                g2.drawRect(0, 0, 15, 15);
                
                if (cb.isSelected()) {
                    g2.setColor(TEXT_COLOR);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawLine(3, 8, 6, 11);
                    g2.drawLine(6, 11, 12, 4);
                }
            }
        });
        
        return toggle;
    }
    
    private JComboBox<String> createColorDropdown(Color currentColor, java.util.function.Consumer<Color> onChange) {
        String[] colorNames = {"Red", "Green", "Blue", "Yellow", "Magenta", "Cyan", "Orange", "Purple", "White", "Black"};
        Color[] colorValues = TileMarkersPlugin.PRESET_COLORS;
        
        String selectedName = "Red";
        for (int i = 0; i < colorValues.length; i++) {
            if (colorValues[i].getRed() == currentColor.getRed() &&
                colorValues[i].getGreen() == currentColor.getGreen() &&
                colorValues[i].getBlue() == currentColor.getBlue()) {
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
    
    private JTextField createLabelTextField() {
        JTextField textField = new JTextField(plugin != null ? plugin.getCurrentMarkerLabel() : "");
        textField.setBackground(new Color(0x3c, 0x3c, 0x3c));
        textField.setForeground(TEXT_COLOR);
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        textField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        textField.setCaretColor(TEXT_COLOR);
        
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateLabel(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateLabel(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateLabel(); }
            
            private void updateLabel() {
                if (plugin != null) {
                    String text = textField.getText();
                    plugin.setCurrentMarkerLabel(text);
                }
            }
        });
        
        return textField;
    }
    
    private JButton createActionButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.setBackground(new Color(0x3c, 0x3c, 0x3c));
        button.setForeground(TEXT_COLOR);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.addActionListener(listener);
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(0x4c, 0x4c, 0x4c));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(new Color(0x3c, 0x3c, 0x3c));
            }
        });
        
        return button;
    }
    
    private JPanel createInstructionsPanel() {
        JPanel instructionsPanel = new JPanel();
        instructionsPanel.setLayout(new BoxLayout(instructionsPanel, BoxLayout.Y_AXIS));
        instructionsPanel.setBackground(BACKGROUND_COLOR);
        instructionsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            "Controls",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12),
            TEXT_COLOR
        ));
        instructionsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        instructionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        String[] instructions = {
            "When Marking Mode ON: Right click adds marker",
            "When Marking Mode OFF: Shift + Right click adds marker",
            "Right click on existing marker: Remove marker",
            "Ctrl + Shift + C: Clear all markers"
        };
        
        for (String instruction : instructions) {
            JLabel instructionLabel = new JLabel("• " + instruction);
            instructionLabel.setForeground(MUTED_TEXT);
            instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            instructionLabel.setBorder(new EmptyBorder(2, 10, 2, 10));
            instructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            instructionsPanel.add(instructionLabel);
        }
        
        return instructionsPanel;
    }
    
    public void updateMarkingModeDisplay() {
        if (markingModeToggle != null && plugin != null) {
            markingModeToggle.setSelected(plugin.isMarkingMode());
        }
    }
    
    public void updateMarkerCountDisplay() {
        if (markerCountLabel != null && plugin != null) {
            int count = plugin.getTileMarkers().size();
            markerCountLabel.setText(count + " marker" + (count != 1 ? "s" : ""));
        }
    }
}