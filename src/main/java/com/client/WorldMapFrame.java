package com.client;

import javax.swing.*;

import com.client.cache.CacheDiagnostic;
import com.client.cache.MapDataLoader;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class WorldMapFrame extends JFrame {
    private BufferedImage mapImage;
    private JPanel mapCanvas;
    private Point dragStart;
    private Point mapPosition = new Point(0, 0);
    private double zoomLevel = 1.0;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 4.0;
    private static final double ZOOM_STEP = 0.15;
    
    private static final Color BG_COLOR = new Color(0x2b2b2b);
    private static final Color PANEL_COLOR = new Color(0x3c3c3c);
    private static final Color TEXT_COLOR = new Color(0xe8e6e3);
    private static final Color ACCENT_COLOR = new Color(0x4a90e2);
    private static final Color BORDER_COLOR = new Color(0x555555);
    
    private Client client;
    private JLabel statusLabel;
    
    public WorldMapFrame(Client client) {
        super("World Map");
        this.client = client;
        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        initializeUI();
        loadMapAsync();
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG_COLOR);
        
        JPanel toolbar = createToolbar();
        add(toolbar, BorderLayout.NORTH);
        
        mapCanvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                	    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
                    RenderingHints.VALUE_RENDER_QUALITY);
                
                if (mapImage != null) {
                    int scaledWidth = (int) (mapImage.getWidth() * zoomLevel);
                    int scaledHeight = (int) (mapImage.getHeight() * zoomLevel);
                    
                    //System.out.println("RENDERING: Drawing " + mapImage.getWidth() + "x" + 
                    //                 mapImage.getHeight() + " scaled to " + scaledWidth + "x" + 
                    //                 scaledHeight + " at " + mapPosition.x + "," + mapPosition.y);
                    
                    g2d.drawImage(mapImage, mapPosition.x, mapPosition.y, 
                        scaledWidth, scaledHeight, this);
                        
                    g2d.setColor(BORDER_COLOR);
                    g2d.drawRect(mapPosition.x - 1, mapPosition.y - 1, 
                        scaledWidth + 1, scaledHeight + 1);
                } else {
                    System.out.println("RENDERING: mapImage is NULL");
                    drawCenteredText(g2d, "Loading map data...");
                }
                
                g2d.dispose();
            }
            
            private void drawCenteredText(Graphics2D g2d, String text) {
                g2d.setColor(TEXT_COLOR);
                g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = getHeight() / 2;
                g2d.drawString(text, x, y);
            }
        };
        mapCanvas.setBackground(new Color(0x1a1a1a));
        
        addMapInteraction();
        add(mapCanvas, BorderLayout.CENTER);
        
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(PANEL_COLOR);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));
        
        statusLabel = new JLabel(" Loading...");
        statusLabel.setForeground(TEXT_COLOR);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusBar.add(statusLabel, BorderLayout.WEST);
        
        add(statusBar, BorderLayout.SOUTH);
    }
    
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(PANEL_COLOR);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        JLabel titleLabel = new JLabel("World Map");
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        controlsPanel.setBackground(PANEL_COLOR);
        
        JButton zoomInBtn = createToolbarButton("Zoom In (+)", "+");
        zoomInBtn.addActionListener(e -> zoomIn());
        
        JButton zoomOutBtn = createToolbarButton("Zoom Out (-)", "-");
        zoomOutBtn.addActionListener(e -> zoomOut());
        
        JButton resetBtn = createToolbarButton("Reset View", "⟲");
        resetBtn.addActionListener(e -> resetView());
        
        controlsPanel.add(zoomInBtn);
        controlsPanel.add(zoomOutBtn);
        controlsPanel.add(resetBtn);
        
        toolbar.add(titleLabel, BorderLayout.WEST);
        toolbar.add(controlsPanel, BorderLayout.EAST);
        
        return toolbar;
    }
    
    private JButton createToolbarButton(String tooltip, String text) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setBackground(PANEL_COLOR);
        button.setForeground(TEXT_COLOR);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(ACCENT_COLOR);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(PANEL_COLOR);
            }
        });
        
        return button;
    }
    
    private void loadMapAsync() {
        SwingWorker<BufferedImage, Void> worker = new SwingWorker<BufferedImage, Void>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                System.out.println("Loading world map from cache...");
                BufferedImage map = MapDataLoader.loadWorldMap();
                
                if (map != null) {
                    System.out.println("Map loaded successfully: " + map.getWidth() + "x" + map.getHeight());
                } else {
                    System.out.println("Map loading returned NULL");
                }
                
                return map;
            }
            
            @Override
            protected void done() {
                try {
                    mapImage = get();
                    System.out.println("SwingWorker done - mapImage = " + 
                                     (mapImage != null ? mapImage.getWidth() + "x" + mapImage.getHeight() : "NULL"));
                    
                    if (mapImage != null) {
                        statusLabel.setText(" Map loaded: " + mapImage.getWidth() + "x" + mapImage.getHeight() + 
                                          " | Zoom: 100% | Drag to pan");
                    } else {
                        statusLabel.setText(" Failed to load map");
                    }
                    
                    resetView();
                    mapCanvas.repaint();
                    System.out.println("Called repaint on mapCanvas");
                    
                } catch (Exception e) {
                    System.err.println("Error in SwingWorker.done():");
                    e.printStackTrace();
                    statusLabel.setText(" Error loading map");
                }
            }
        };
        
        worker.execute();
    }
    
    private void addMapInteraction() {
        mapCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
                mapCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                dragStart = null;
                mapCanvas.setCursor(Cursor.getDefaultCursor());
            }
        });
        
        mapCanvas.addMouseMotionListener(new MouseMotionAdapter() {
        	@Override
        	public void mouseDragged(MouseEvent e) {
        	    if (dragStart != null && mapImage != null) {
        	        int dx = e.getX() - dragStart.x;
        	        int dy = e.getY() - dragStart.y;
        	        
        	        mapPosition.x += dx;
        	        mapPosition.y += dy;
        	        
        	        // Constrain to keep map visible
        	        int scaledWidth = (int)(mapImage.getWidth() * zoomLevel);
        	        int scaledHeight = (int)(mapImage.getHeight() * zoomLevel);
        	        
        	        mapPosition.x = Math.max(Math.min(mapPosition.x, 100), 
        	                                 mapCanvas.getWidth() - scaledWidth - 100);
        	        mapPosition.y = Math.max(Math.min(mapPosition.y, 100), 
        	                                 mapCanvas.getHeight() - scaledHeight - 100);
        	        
        	        dragStart = e.getPoint();
        	        mapCanvas.repaint();
        	        updateStatus();
        	    }
        	}
        });
        
        mapCanvas.addMouseWheelListener(e -> {
            if (mapImage == null) return;
            
            double oldZoom = zoomLevel;
            if (e.getWheelRotation() < 0) {
                zoomLevel = Math.min(zoomLevel + ZOOM_STEP, MAX_ZOOM);
            } else {
                zoomLevel = Math.max(zoomLevel - ZOOM_STEP, MIN_ZOOM);
            }
            
            // Zoom towards mouse position
            Point mouse = e.getPoint();
            double zoomFactor = zoomLevel / oldZoom;
            mapPosition.x = (int)(mouse.x - (mouse.x - mapPosition.x) * zoomFactor);
            mapPosition.y = (int)(mouse.y - (mouse.y - mapPosition.y) * zoomFactor);
            
            mapCanvas.repaint();
            updateStatus();
        });
    }
    
    private void zoomIn() {
        if (zoomLevel < MAX_ZOOM) {
            zoomLevel = Math.min(zoomLevel + ZOOM_STEP, MAX_ZOOM);
            mapCanvas.repaint();
            updateStatus();
        }
    }
    
    private void zoomOut() {
        if (zoomLevel > MIN_ZOOM) {
            zoomLevel = Math.max(zoomLevel - ZOOM_STEP, MIN_ZOOM);
            mapCanvas.repaint();
            updateStatus();
        }
    }
    
    private void resetView() {
        zoomLevel = 1.0;
        if (mapImage != null) {
            mapPosition.x = (mapCanvas.getWidth() - mapImage.getWidth()) / 2;
            mapPosition.y = (mapCanvas.getHeight() - mapImage.getHeight()) / 2;
            System.out.println("Reset view - centered at " + mapPosition.x + "," + mapPosition.y);
        } else {
            mapPosition = new Point(0, 0);
        }
        mapCanvas.repaint();
        updateStatus();
    }
    
    private void updateStatus() {
        if (mapImage != null) {
            statusLabel.setText(String.format(" Zoom: %.0f%% | Position: %d, %d", 
                zoomLevel * 100, mapPosition.x, mapPosition.y));
        }
    }
}