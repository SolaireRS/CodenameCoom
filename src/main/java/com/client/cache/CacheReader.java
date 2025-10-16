package com.client.cache;

import com.client.sign.Signlink;
import java.io.RandomAccessFile;

public class CacheReader {
    
    /**
     * Reads a file from the cache with lenient validation
     */
    public static byte[] readFile(int index, int file) {
        if (index < 0 || index >= 8 || file < 0) {
            return null;
        }
        
        try {
            if (Signlink.cache_idx == null || index >= Signlink.cache_idx.length) {
                return null;
            }
            
            RandomAccessFile indexFile = Signlink.cache_idx[index];
            RandomAccessFile dataFile = Signlink.cache_dat;
            
            if (indexFile == null || dataFile == null) {
                return null;
            }
            
            synchronized (indexFile) {
                synchronized (dataFile) {
                    long indexPosition = file * 6L;
                    if (indexFile.length() < indexPosition + 6) {
                        return null;
                    }
                    
                    indexFile.seek(indexPosition);
                    
                    int size = ((indexFile.read() & 0xFF) << 16) | 
                              ((indexFile.read() & 0xFF) << 8) | 
                              (indexFile.read() & 0xFF);
                    
                    int sector = ((indexFile.read() & 0xFF) << 16) | 
                                ((indexFile.read() & 0xFF) << 8) | 
                                (indexFile.read() & 0xFF);
                    
                    if (size <= 0 || size > 5000000 || sector <= 0) {
                        return null;
                    }
                    
                    byte[] data = new byte[size];
                    int bytesRead = 0;
                    int chunkNumber = 0;
                    
                    while (bytesRead < size) {
                        if (sector <= 0) {
                            return null;
                        }
                        
                        dataFile.seek(sector * 520L);
                        
                        int remainingBytes = size - bytesRead;
                        int headerSize = (chunkNumber == 0) ? 8 : 9;
                        int dataSize = Math.min(512 - headerSize, remainingBytes);
                        
                        // Read header
                        int fileId = ((dataFile.read() & 0xFF) << 8) | (dataFile.read() & 0xFF);
                        int chunkId = ((dataFile.read() & 0xFF) << 8) | (dataFile.read() & 0xFF);
                        int nextSector = ((dataFile.read() & 0xFF) << 16) | 
                                        ((dataFile.read() & 0xFF) << 8) | 
                                        (dataFile.read() & 0xFF);
                        dataFile.read(); // indexId - skip validation
                        
                        if (chunkNumber > 0) {
                            dataFile.read(); // extended header
                        }
                        
                        // Only validate file and chunk IDs
                        if (fileId != file || chunkId != chunkNumber) {
                            return null;
                        }
                        
                        dataFile.read(data, bytesRead, dataSize);
                        
                        bytesRead += dataSize;
                        sector = nextSector;
                        chunkNumber++;
                        
                        if (chunkNumber > 10000) {
                            return null;
                        }
                    }
                    
                    return data;
                }
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Decompresses GZIP data
     */
    public static byte[] decompress(byte[] compressed) {
        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(compressed);
            java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(bais);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            
            gzis.close();
            baos.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}