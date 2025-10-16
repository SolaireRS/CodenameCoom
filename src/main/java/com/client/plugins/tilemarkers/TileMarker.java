package com.client.plugins.tilemarkers;

import java.awt.Color;
import java.util.Objects;

/**
 * Represents a single tile marker
 */
public class TileMarker {
    private final TileCoordinate coordinate;
    private final Color color;
    private final String label;
    private final long timestamp;
    
    public TileMarker(TileCoordinate coordinate, Color color, String label) {
        this.coordinate = coordinate;
        this.color = color;
        this.label = label;
        this.timestamp = System.currentTimeMillis();
    }
    
    public TileCoordinate getCoordinate() {
        return coordinate;
    }
    
    public Color getColor() {
        return color;
    }
    
    public String getLabel() {
        return label;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getWorldX() {
        return coordinate.getX();
    }
    
    public int getWorldY() {
        return coordinate.getY();
    }
    
    public int getPlane() {
        return coordinate.getPlane();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TileMarker that = (TileMarker) obj;
        return Objects.equals(coordinate, that.coordinate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(coordinate);
    }
    
    @Override
    public String toString() {
        return "TileMarker{coordinate=" + coordinate + ", color=" + color + ", label='" + label + "'}";
    }
}