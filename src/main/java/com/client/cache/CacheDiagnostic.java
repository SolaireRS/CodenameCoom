package com.client.cache;

import com.client.sign.Signlink;

public class CacheDiagnostic {
    
    public static void scanCacheForMapData() {
        System.out.println("=== Cache Map Data Scanner ===");
        
        for (int index = 0; index < 8; index++) {
            if (Signlink.cache_idx[index] == null) {
                continue;
            }
            
            try {
                long idxSize = Signlink.cache_idx[index].length();
                int maxFiles = (int)(idxSize / 6);
                
                System.out.println("\nIndex " + index + ": " + maxFiles + " potential files");
                
                int validFiles = 0;
                int sampleSize = Math.min(100, maxFiles);
                
                // Sample first 100 files
                for (int fileId = 0; fileId < sampleSize; fileId++) {
                    byte[] data = CacheReader.readFile(index, fileId);
                    if (data != null && data.length > 0) {
                        validFiles++;
                        if (validFiles <= 3) {
                            System.out.println("  File " + fileId + ": " + data.length + " bytes");
                        }
                    }
                }
                
                System.out.println("  Found " + validFiles + " valid files in first " + sampleSize);
                
            } catch (Exception e) {
                System.err.println("Error scanning index " + index);
            }
        }
        
        // Now test specific region IDs that should exist for Lumbridge
        System.out.println("\n=== Testing Known Lumbridge Regions ===");
        int[] lumbridgeRegions = {12342, 12343, 12598, 12599};
        
        for (int index = 0; index < 8; index++) {
            if (Signlink.cache_idx[index] == null) continue;
            
            for (int regionId : lumbridgeRegions) {
                byte[] data = CacheReader.readFile(index, regionId);
                if (data != null) {
                    System.out.println("Region " + regionId + " found in index " + index + 
                                     " (size: " + data.length + " bytes)");
                }
            }
        }
    }
}