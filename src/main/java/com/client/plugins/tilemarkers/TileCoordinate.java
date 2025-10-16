package com.client.plugins.tilemarkers;

import java.util.Objects;

/**
 * Represents a tile coordinate in the game world
 */
public class TileCoordinate {
    private final int x;
    private final int y;
    private final int plane;
    
    public TileCoordinate(int x, int y, int plane) {
        this.x = x;
        this.y = y;
        this.plane = plane;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getPlane() {
        return plane;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TileCoordinate that = (TileCoordinate) obj;
        return x == that.x && y == that.y && plane == that.plane;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, y, plane);
    }
    
    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + plane + ")";
    }
}