package com.client.plugins.gpu;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.client.Client;

/**
 * GPU Settings Panel - Fixed to properly update effects when settings change
 */
public class GPUSettingsPanel extends JPanel {
    
    private static final Color BACKGROUND_COLOR = new Color(0x2b, 0x2b, 0x2b);
    private static final Color TEXT_COLOR = new Color(0x00, 0xa2, 0xe8);
    private static final Color MUTED_TEXT = new Color(0x9f, 0x9f, 0x9f);
    private static final Color BORDER_COLOR = new Color(0x3c, 0x3c, 0x3c);
    
    private GPUPlugin plugin;
    private GPUConfig config;
    
    public GPUSettingsPanel(GPUPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        
        setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        createSlimUI();
    }
    
    private void createSlimUI() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BACKGROUND_COLOR);
        
        JLabel titleLabel = new JLabel("GPU Renderer Settings");
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);
        
        mainPanel.add(createSlimRow("Enable GPU Acceleration", createToggleButton(plugin.isEnabled(), e -> {
            if (((JCheckBox)e.getSource()).isSelected()) {
                plugin.enable();
            } else {
                plugin.disable();
            }
        })));
        
        mainPanel.add(createSlimRow("Client Anti-Aliasing", createToggleButton(config.clientAntiAliasing, e -> {
            config.clientAntiAliasing = ((JCheckBox)e.getSource()).isSelected();
        })));
        
        mainPanel.add(createSlimRow("GPU Anti-Aliasing", createToggleButton(config.antiAliasing, e -> {
            config.antiAliasing = ((JCheckBox)e.getSource()).isSelected();
            // Force immediate reprocessing
            plugin.applyEffects();
        })));
        
        mainPanel.add(createSlimRow("Anti-Aliasing Type", createDropdown(
            new String[]{"SIMPLE", "FXAA", "MSAA", "TAA", "COMBINED"}, 
            config.antiAliasingType.toString(),
            value -> {
                config.antiAliasingType = GPUConfig.AntiAliasingType.valueOf(value);
                // Force reprocessing when AA type changes
                plugin.applyEffects();
            }
        )));
        
        mainPanel.add(createSlimRow("AA Strength", createPercentageSlider(
            (int)(config.antiAliasingStrength * 100),
            value -> {
                config.antiAliasingStrength = value / 100.0f;
                // Force reprocessing when strength changes
                plugin.applyEffects();
            }
        )));
        
        mainPanel.add(createSlimRow("MSAA Samples", createDropdown(
            new String[]{"2", "4", "8", "16"},
            String.valueOf(config.antiAliasingMode),
            value -> {
                config.antiAliasingMode = Integer.parseInt(value);
                // Force reprocessing when sample count changes
                plugin.applyEffects();
            }
        )));
        
        mainPanel.add(createSlimRow("Brightness Control", createToggleButton(config.brightnessTint, e -> {
            config.brightnessTint = ((JCheckBox)e.getSource()).isSelected();
            plugin.applyEffects();
        })));
        
        mainPanel.add(createSlimRow("Brightness Level", createPercentageSlider(
            config.brightnessLevel,
            value -> {
                config.brightnessLevel = value;
                plugin.applyEffects();
            }
        )));
        
        mainPanel.add(createSlimRow("Dynamic Shadows", createToggleButton(config.shadows, e -> {
            config.shadows = ((JCheckBox)e.getSource()).isSelected();
            plugin.applyEffects();
        })));

        mainPanel.add(createSlimRow("Shadow Quality", createDropdown(
            new String[]{"Low", "Medium", "High", "Ultra"},
            config.shadowQuality,
            value -> {
                config.shadowQuality = value;
                
                // Apply quality presets with aggressive boosts
                switch (value) {
                    case "Low":
                        config.shadowSensitivity = 85;   // 30% more aggressive
                        config.shadowStrength = 0.42f;   // 30% boost: 0.12 * 1.3 = ~0.16, but boosted more
                        config.shadowDistance = 2;       // Increased from 1
                        config.shadowSoftness = 1;
                        break;
                    case "Medium":
                        config.shadowSensitivity = 65;   // 15% more aggressive
                        config.shadowStrength = 0.48f;   // 15% boost: 0.28 * 1.15 = ~0.32, boosted more
                        config.shadowDistance = 3;       // Increased from 2
                        config.shadowSoftness = 2;
                        break;
                    case "High":
                        config.shadowSensitivity = 45;   // 10% more aggressive
                        config.shadowStrength = 0.60f;   // 10% boost: 0.45 * 1.10 = ~0.50, boosted more
                        config.shadowDistance = 5;       // Increased from 4
                        config.shadowSoftness = 2;
                        break;
                    case "Ultra":
                        config.shadowSensitivity = 33;   // 5% more aggressive
                        config.shadowStrength = 0.70f;   // 5% boost: 0.65 * 1.05 = ~0.68
                        config.shadowDistance = 6;
                        config.shadowSoftness = 3;
                        break;
                }
                
                plugin.applyEffects();
            }
        )));

        mainPanel.add(createSlimRow("Color Tinted Shadows", createToggleButton(config.shadowColorTint, e -> {
            config.shadowColorTint = ((JCheckBox)e.getSource()).isSelected();
            plugin.applyEffects();
        })));
        
        mainPanel.add(createSlimRow("Atmospheric Fog", createToggleButton(config.fog, e -> {
            config.fog = ((JCheckBox)e.getSource()).isSelected();
            if (!config.fog) {
                config.fogType = GPUConfig.FogType.OFF;
            }
            plugin.applyEffects();
        })));
        
        mainPanel.add(createSlimRow("Fog Type", createDropdown(
        	    new String[]{"OFF", "GREY", "SISLE", "DARK", "MAROON", "RAINBOW"},
        	    config.fogType.toString(),
        	    value -> {
        	        config.fogType = GPUConfig.FogType.valueOf(value);
        	        if (config.fogType == GPUConfig.FogType.OFF) {
        	            config.fog = false;
        	            // Also update the Settings object
        	            if (Client.instance != null && Client.instance.getUserSettings() != null) {
        	                Client.instance.getUserSettings().setFog(false);
        	            }
        	        } else {
        	            config.fog = true;
        	            // Also update the Settings object
        	            if (Client.instance != null && Client.instance.getUserSettings() != null) {
        	                Client.instance.getUserSettings().setFog(true);
        	            }
        	        }
        	        plugin.applyEffects();
        	    }
        	)));
        
        mainPanel.add(createSlimRow("Detail Enhancement", createToggleButton(config.detailEnhancement, e -> {
            config.detailEnhancement = ((JCheckBox)e.getSource()).isSelected();
            plugin.applyEffects();
        })));
        
        mainPanel.add(createSlimRow("Sharpening Strength", createPercentageSlider(
            (int)(config.sharpeningStrength * 100),
            value -> {
                config.sharpeningStrength = value / 100.0f;
                plugin.applyEffects();
            }
        )));
        
        mainPanel.add(createSlimRow("Show FPS Counter", createToggleButton(config.showFPSCounter, e -> {
            config.showFPSCounter = ((JCheckBox)e.getSource()).isSelected();
        })));
        
        mainPanel.add(createSlimRow("FPS Position", createDropdown(
            new String[]{"Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right"},
            getFPSPositionString(config.fpsDisplayPosition),
            value -> {
                String[] positions = {"Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right"};
                for (int i = 0; i < positions.length; i++) {
                    if (positions[i].equals(value)) {
                        config.fpsDisplayPosition = i;
                        break;
                    }
                }
            }
        )));
        
        mainPanel.add(createSlimRow("Target FPS", createNumericSlider(
            config.targetFPS, 60, 240,
            value -> config.targetFPS = value
        )));
        
        add(mainPanel, BorderLayout.NORTH);
    }
    
    private String getFPSPositionString(int position) {
        String[] positions = {"Top-Left", "Top-Right", "Bottom-Left", "Bottom-Right"};
        if (position >= 0 && position < positions.length) {
            return positions[position];
        }
        return "Top-Left";
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
    
    private JCheckBox createToggleButton(boolean selected, java.awt.event.ActionListener listener) {
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
    
    private JComboBox<String> createDropdown(String[] options, String selected, java.util.function.Consumer<String> onChange) {
        JComboBox<String> combo = new JComboBox<>(options);
        combo.setSelectedItem(selected);
        combo.setBackground(BACKGROUND_COLOR);
        combo.setForeground(TEXT_COLOR);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        combo.addActionListener(e -> {
            if (!e.getActionCommand().equals("comboBoxEdited")) {  // Prevent duplicate calls
                onChange.accept((String)combo.getSelectedItem());
            }
        });
        return combo;
    }
    
    private JPanel createPercentageSlider(int value, java.util.function.Consumer<Integer> onChange) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.setBackground(BACKGROUND_COLOR);
        
        JSlider slider = new JSlider(0, 100, value);
        slider.setBackground(BACKGROUND_COLOR);
        slider.setPreferredSize(new Dimension(80, 22));
        
        JLabel valueLabel = new JLabel(value + "%");
        valueLabel.setForeground(TEXT_COLOR);
        valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        valueLabel.setPreferredSize(new Dimension(35, 22));
        
        slider.addChangeListener(e -> {
            if (!slider.getValueIsAdjusting()) {  // Only trigger when user releases slider
                int val = slider.getValue();
                valueLabel.setText(val + "%");
                onChange.accept(val);
            } else {
                // Update label while dragging, but don't trigger onChange
                valueLabel.setText(slider.getValue() + "%");
            }
        });
        
        panel.add(slider);
        panel.add(valueLabel);
        return panel;
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
            if (!slider.getValueIsAdjusting()) {  // Only trigger when user releases slider
                int val = slider.getValue();
                valueLabel.setText(String.valueOf(val));
                onChange.accept(val);
            } else {
                // Update label while dragging
                valueLabel.setText(String.valueOf(slider.getValue()));
            }
        });
        
        panel.add(slider);
        panel.add(valueLabel);
        return panel;
    }
    
    private JPanel createRangedSlider(int value, int min, int max, java.util.function.Consumer<Integer> onChange) {
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
            if (!slider.getValueIsAdjusting()) {  // Only trigger when user releases slider
                int val = slider.getValue();
                valueLabel.setText(String.valueOf(val));
                onChange.accept(val);
            } else {
                // Update label while dragging
                valueLabel.setText(String.valueOf(slider.getValue()));
            }
        });
        
        panel.add(slider);
        panel.add(valueLabel);
        return panel;
    }
}