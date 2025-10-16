package com.client;

public class NpcOverrides {
    public long field2093;
    int[] modelIds;
    public short[] recolorTo;
    public short[] retextureTo;
    public boolean useLocalPlayer;

    public NpcOverrides(long var1, int[] var3, short[] var4, short[] var5, boolean var6) {
        this.useLocalPlayer = false;
        this.field2093 = var1;
        this.modelIds = var3;
        this.recolorTo = var4;
        this.retextureTo = var5;
        this.useLocalPlayer = var6;
    }
    static boolean method4322(int var0, int var1) {
        return var0 != 4 || var1 < 8;
    }

    static final int method4321(int var0, int var1, int var2, int var3) {
        int var4 = 65536 - Rasterizer.Rasterizer3D_cosine[var2 * 1024 / var3] >> 1;
        return ((65536 - var4) * var0 >> 16) + (var4 * var1 >> 16);
    }
}
