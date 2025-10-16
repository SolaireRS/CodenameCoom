package com.client;

import com.client.model.rt7_anims.SkeletalAnimBase;

public final class AnimBase {
    SkeletalAnimBase skeletal_animbase;
    public int count;
    public AnimBase(Buffer packet, boolean highrev, int buffer_size) {
        int before_read = packet.currentOffset;
        count = highrev ? packet.readUShort() : packet.readUnsignedByte();
        transform_types = new int[count];
        labels = new int[count][];
        for(int j = 0; j < count; j++)
            transform_types[j] = highrev ? packet.readUShort() : packet.readUnsignedByte();
        for(int j = 0; j < count; j++)
            labels[j] = new int[highrev ? packet.readUShort() : packet.readUnsignedByte()];
        for(int j = 0; j < count; j++)
            for(int l = 0; l < labels[j].length; l++)
                labels[j][l] = highrev ? packet.readUShort() : packet.readUnsignedByte();

        int read1_size = packet.currentOffset - before_read;

        if(!highrev) {
            if (read1_size != buffer_size) {
                try {
                    int size = packet.readUShort();
                    if (size > 0) {
                        this.skeletal_animbase = new SkeletalAnimBase(packet, size);
                    }
                } catch (Throwable t) {
                    System.err.println("Tried to load base because there was extra base data but skeletal failed to load.");
                    t.printStackTrace();
                }
            }
            int read2_size = packet.currentOffset - before_read;

            if(read2_size != buffer_size) {
                throw new RuntimeException("base data size mismatch: " + read2_size + ", expected " + buffer_size);
            }
        }
    }
    public int transforms_count() {
        return this.count;
    }

    public SkeletalAnimBase get_skeletal_animbase() {
        return this.skeletal_animbase;
    }
    public final int[] transform_types;//anIntArray342
    public final int[][] labels;//anIntArray343
}
