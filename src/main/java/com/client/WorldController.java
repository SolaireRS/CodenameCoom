package com.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.awt.Color;

import com.client.plugins.tilemarkers.TileCoordinate;
import com.client.plugins.tilemarkers.TileMarker;
import com.client.plugins.tilemarkers.TileMarkersPlugin;

public final class WorldController {
    private final CustomGameFrame parentFrame;
    private static final int VISIBILITY_BATCH_SIZE = 64; // Tiles per batch
    private List<Ground> visibleTiles = new ArrayList<>(4096);
    private ExecutorService renderExecutor; // Initialize in constructor
    public static final int MAX_FAR_Z = 70;
    public static final int MIN_FAR_Z = 30;
    public static int farZ = MIN_FAR_Z;
    public static int renderDistanceClamp = farZ * 120;
    private static final int SCENE_THREADS = Runtime.getRuntime().availableProcessors() - 1;
    private static final ExecutorService scenePool = Executors.newFixedThreadPool(SCENE_THREADS);
    
    public WorldController(int ai[][][], CustomGameFrame parentFrame) {
        int i = 104;// was parameter
        int j = 104;// was parameter
        int k = 4;// was parameter
        aBoolean434 = true;
        // Initialize the final field with the parameter
        this.parentFrame = parentFrame;
        obj5Cache = new StaticObject[5000];
        anIntArray486 = new int[10000];
        anIntArray487 = new int[10000];
        anInt437 = k;
        anInt438 = j;
        anInt439 = i;
        groundArray = new Ground[k][j][i];
        anIntArrayArrayArray445 = new int[k][j + 1][i + 1];
        visibilityMap = new boolean[8][32][MAX_FAR_Z * 2 + 1][MAX_FAR_Z * 2 + 1];
        tileHeights = ai;
        initToNull();
    }
    private static class TileRenderData {
        Ground tile;
        int x, y, plane;
        boolean shouldRender;
        int distanceSquared;
    }
    
    public void calculateVisibilityParallel() {
        List<Future<?>> tasks = new ArrayList<>();
        
        // Split planes across threads
        int planesPerThread = Math.max(1, (anInt437 - anInt442 + SCENE_THREADS - 1) / SCENE_THREADS);
        
        for (int t = 0; t < SCENE_THREADS; t++) {
            final int startPlane = anInt442 + (t * planesPerThread);
            final int endPlane = Math.min(anInt437, startPlane + planesPerThread);
            
            if (startPlane >= endPlane) continue;
            
            tasks.add(scenePool.submit(() -> {
                for (int plane = startPlane; plane < endPlane; plane++) {
                    Ground[][] tiles = groundArray[plane];
                    
                    for (int x = anInt449; x < anInt450; x++) {
                        for (int y = anInt451; y < anInt452; y++) {
                            Ground tile = tiles[x][y];
                            if (tile == null) continue;
                            
                            // Calculate visibility
                            int visX = (x - Scene_cameraXTile) + MAX_FAR_Z;
                            int visY = (y - Scene_cameraYTile) + MAX_FAR_Z;
                            
                            if (visX < 0 || visX >= aBooleanArrayArray492.length || 
                                visY < 0 || visY >= aBooleanArrayArray492[0].length) {
                                tile.aBoolean1322 = false;
                                tile.aBoolean1323 = false;
                                tile.anInt1325 = 0;
                                continue;
                            }
                            
                            boolean isVisible = aBooleanArrayArray492[visX][visY];
                            
                            if (tile.anInt1321 > anInt447 || 
                                (!isVisible && tileHeights[plane][x][y] - zCameraPos < 1)) {
                                tile.aBoolean1322 = false;
                                tile.aBoolean1323 = false;
                                tile.anInt1325 = 0;
                            } else {
                                tile.aBoolean1322 = true;
                                tile.aBoolean1323 = true;
                                tile.aBoolean1324 = tile.anInt1317 > 0;
                            }
                        }
                    }
                }
            }));
        }
        
        // Wait for all threads
        for (Future<?> task : tasks) {
            try {
                task.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void nullLoader() {
        aClass28Array462 = null;
        anIntArray473 = null;
        aClass47ArrayArray474 = null;
        aClass19_477 = null;
        visibilityMap = null;
        aBooleanArrayArray492 = null;
    }
    public int[][][] getTileHeights() {
        return tileHeights;
    }
    public void initToNull() {
        for (int j = 0; j < anInt437; j++) {
            for (int k = 0; k < anInt438; k++) {
                for (int i1 = 0; i1 < anInt439; i1++)
                    groundArray[j][k][i1] = null;

            }

        }
        for (int l = 0; l < anInt472; l++) {
            for (int j1 = 0; j1 < anIntArray473[l]; j1++)
                aClass47ArrayArray474[l][j1] = null;

            anIntArray473[l] = 0;
        }

        for (int k1 = 0; k1 < obj5CacheCurrPos; k1++)
            obj5Cache[k1] = null;

        obj5CacheCurrPos = 0;
        for (int l1 = 0; l1 < aClass28Array462.length; l1++)
            aClass28Array462[l1] = null;

    }

    public void method275(int i) {
        anInt442 = i;
        for (int k = 0; k < anInt438; k++) {
            for (int l = 0; l < anInt439; l++)
                if (groundArray[i][k][l] == null)
                    groundArray[i][k][l] = new Ground(i, k, l);

        }

    }

    public void method276(int i, int j) {
        Ground class30_sub3 = groundArray[0][j][i];
        for (int l = 0; l < 3; l++) {
            Ground class30_sub3_1 = groundArray[l][j][i] = groundArray[l + 1][j][i];
            if (class30_sub3_1 != null) {
                class30_sub3_1.anInt1307--;
                for (int j1 = 0; j1 < class30_sub3_1.anInt1317; j1++) {
                    StaticObject class28 = class30_sub3_1.obj5Array[j1];
                    if ((class28.uid >> 29 & 3) == 2 && class28.anInt523 == j
                            && class28.anInt525 == i)
                        class28.anInt517--;
                }

            }
        }
        if (groundArray[0][j][i] == null)
            groundArray[0][j][i] = new Ground(0, j, i);
        groundArray[0][j][i].aClass30_Sub3_1329 = class30_sub3;
        groundArray[3][j][i] = null;
    }

    public static void createNewSceneCluster(int i, int j, int k, int l, int i1, int j1,
                                             int l1, int i2) {
        Class47 class47 = new Class47();
        class47.minTileX = j / 128;
        class47.anInt788 = l / 128;
        class47.anInt789 = l1 / 128;
        class47.anInt790 = i1 / 128;
        class47.type = i2;
        class47.anInt792 = j;
        class47.anInt793 = l;
        class47.anInt794 = l1;
        class47.anInt795 = i1;
        class47.anInt796 = j1;
        class47.anInt797 = k;
        aClass47ArrayArray474[i][anIntArray473[i]++] = class47;
    }

    public void method278(int i, int j, int k, int l) {
        Ground class30_sub3 = groundArray[i][j][k];
        if (class30_sub3 != null) {
            groundArray[i][j][k].anInt1321 = l;
        }
    }

    public void addTile(int i, int j, int k, int l, int i1, int j1, int k1,
                        int l1, int i2, int j2, int k2, int l2, int i3, int j3, int k3,
                        int l3, int i4, int j4, int k4, int l4) {
        if (l == 0) {
            Class43 class43 = new Class43(k2, l2, i3, j3, -1, k4, false);
            for (int i5 = i; i5 >= 0; i5--)
                if (groundArray[i5][j][k] == null)
                    groundArray[i5][j][k] = new Ground(i5, j, k);

            groundArray[i][j][k].aClass43_1311 = class43;
            return;
        }
        if (l == 1) {
            Class43 class43_1 = new Class43(k3, l3, i4, j4, j1, l4, k1 == l1
                    && k1 == i2 && k1 == j2);
            for (int j5 = i; j5 >= 0; j5--)
                if (groundArray[j5][j][k] == null)
                    groundArray[j5][j][k] = new Ground(j5, j, k);

            groundArray[i][j][k].aClass43_1311 = class43_1;
            return;
        }
        ShapedTile class40 = new ShapedTile(k, k3, j3, i2, j1, i4, i1, k2, k4, i3,
                j2, l1, k1, l, j4, l3, l2, j, l4);
        for (int k5 = i; k5 >= 0; k5--)
            if (groundArray[k5][j][k] == null)
                groundArray[k5][j][k] = new Ground(k5, j, k);

        groundArray[i][j][k].aClass40_1312 = class40;
    }

    public void method280(int i, int j, int k, Renderable class30_sub2_sub4,
                          byte byte0, long key, int j1) {
        if (class30_sub2_sub4 == null)
            return;
        Object3 class49 = new Object3();
        class49.aClass30_Sub2_Sub4_814 = class30_sub2_sub4;
        class49.anInt812 = j1 * 128 + 64;
        class49.anInt813 = k * 128 + 64;
        class49.anInt811 = j;
        class49.uid = key;
        class49.aByte816 = byte0;
        if (groundArray[i][j1][k] == null)
            groundArray[i][j1][k] = new Ground(i, j1, k);
        groundArray[i][j1][k].obj3 = class49;
    }

    public void method281(int i, int j, Renderable class30_sub2_sub4, int k,
                          Renderable class30_sub2_sub4_1, Renderable class30_sub2_sub4_2, int l,
                          int i1) {
        Object4 object4 = new Object4();
        object4.aClass30_Sub2_Sub4_48 = class30_sub2_sub4_2;
        object4.anInt46 = i * 128 + 64;
        object4.anInt47 = i1 * 128 + 64;
        object4.anInt45 = k;
        object4.uid = j;
        object4.aClass30_Sub2_Sub4_49 = class30_sub2_sub4;
        object4.aClass30_Sub2_Sub4_50 = class30_sub2_sub4_1;
        int j1 = 0;
        Ground class30_sub3 = groundArray[l][i][i1];
        if (class30_sub3 != null) {
            for (int k1 = 0; k1 < class30_sub3.anInt1317; k1++)
                if (class30_sub3.obj5Array[k1].aClass30_Sub2_Sub4_521 instanceof Model) {
                    int l1 = ((Model) class30_sub3.obj5Array[k1].aClass30_Sub2_Sub4_521).itemDropHeight;
                    if (l1 > j1)
                        j1 = l1;
                }

        }
        object4.anInt52 = j1;
        if (groundArray[l][i][i1] == null)
            groundArray[l][i][i1] = new Ground(l, i, i1);
        groundArray[l][i][i1].obj4 = object4;
    }

    public void method282(int i, Renderable class30_sub2_sub4, long key, int k,
                          byte byte0, int l, Renderable class30_sub2_sub4_1, int i1, int j1,
                          int k1) {
        if (class30_sub2_sub4 == null && class30_sub2_sub4_1 == null)
            return;
        Object1 object1 = new Object1();
        object1.uid = key;
        object1.aByte281 = byte0;
        object1.anInt274 = l * 128 + 64;
        object1.anInt275 = k * 128 + 64;
        object1.anInt273 = i1;
        object1.aClass30_Sub2_Sub4_278 = class30_sub2_sub4;
        object1.aClass30_Sub2_Sub4_279 = class30_sub2_sub4_1;
        object1.orientation = i;
        object1.orientation1 = j1;
        for (int l1 = k1; l1 >= 0; l1--)
            if (groundArray[l1][l][k] == null)
                groundArray[l1][l][k] = new Ground(l1, l, k);

        groundArray[k1][l][k].obj1 = object1;
    }

    public void method283(long key, int j, int k, int i1, int j1, int k1,
                          Renderable class30_sub2_sub4, int l1, byte byte0, int i2, int j2) {
        if (class30_sub2_sub4 == null)
            return;
        Object2 class26 = new Object2();
        class26.uid = key;
        class26.aByte506 = byte0;
        class26.anInt500 = l1 * 128 + 64 + j1;
        class26.anInt501 = j * 128 + 64 + i2;
        class26.anInt499 = k1;
        class26.aClass30_Sub2_Sub4_504 = class30_sub2_sub4;
        class26.anInt502 = j2;
        class26.anInt503 = k;
        for (int k2 = i1; k2 >= 0; k2--)
            if (groundArray[k2][l1][j] == null)
                groundArray[k2][l1][j] = new Ground(k2, l1, j);

        groundArray[i1][l1][j].obj2 = class26;
    }

    public boolean method284(long key, byte byte0, int j, int k,
                             Renderable class30_sub2_sub4, int l, int i1, int j1, int k1, int l1) {
        if (class30_sub2_sub4 == null) {
            return true;
        } else {
            int i2 = l1 * 128 + 64 * l;
            int j2 = k1 * 128 + 64 * k;
            return method287(i1, l1, k1, l, k, i2, j2, j, class30_sub2_sub4,
                    j1, false, key, byte0);
        }
    }

    public boolean method285(int i, int j, int k, long k3, int i1, int j1,
                             int k1, Renderable class30_sub2_sub4, boolean flag) {
        if (class30_sub2_sub4 == null)
            return true;
        int l1 = k1 - j1;
        int i2 = i1 - j1;
        int j2 = k1 + j1;
        int k2 = i1 + j1;
        if (flag) {
            if (j > 640 && j < 1408)
                k2 += 128;
            if (j > 1152 && j < 1920)
                j2 += 128;
            if (j > 1664 || j < 384)
                i2 -= 128;
            if (j > 128 && j < 896)
                l1 -= 128;
        }
        l1 /= 128;
        i2 /= 128;
        j2 /= 128;
        k2 /= 128;
        return method287(i, l1, i2, (j2 - l1) + 1, (k2 - i2) + 1, k1, i1, k,
                class30_sub2_sub4, j, true, k3, (byte) 0);
    }

    public boolean method286(int j, int k, Renderable class30_sub2_sub4, int l,
                             int i1, int j1, int k1, int l1, int i2, long i12, int k2) {
        return class30_sub2_sub4 == null
                || method287(j, l1, k2, (i2 - l1) + 1, (i1 - k2) + 1, j1, k,
                k1, class30_sub2_sub4, l, true, i12, (byte) 0);
    }

    private boolean method287(int i, int j, int k, int l, int i1, int j1,
                              int k1, int l1, Renderable class30_sub2_sub4, int i2, boolean flag,
                              long key, byte byte0) {
        for (int k2 = j; k2 < j + l; k2++) {
            for (int l2 = k; l2 < k + i1; l2++) {
                if (k2 < 0 || l2 < 0 || k2 >= anInt438 || l2 >= anInt439)
                    return false;
                Ground class30_sub3 = groundArray[i][k2][l2];
                if (class30_sub3 != null && class30_sub3.anInt1317 >= 5)
                    return false;
            }

        }

        StaticObject class28 = new StaticObject();
        class28.uid = key;
        class28.aByte530 = byte0;
        class28.anInt517 = i;
        class28.anInt519 = j1;
        class28.anInt520 = k1;
        class28.anInt518 = l1;
        class28.aClass30_Sub2_Sub4_521 = class30_sub2_sub4;
        class28.anInt522 = i2;
        class28.anInt523 = j;
        class28.anInt525 = k;
        class28.anInt524 = (j + l) - 1;
        class28.anInt526 = (k + i1) - 1;
        for (int i3 = j; i3 < j + l; i3++) {
            for (int j3 = k; j3 < k + i1; j3++) {
                int k3 = 0;
                if (i3 > j)
                    k3++;
                if (i3 < (j + l) - 1)
                    k3 += 4;
                if (j3 > k)
                    k3 += 8;
                if (j3 < (k + i1) - 1)
                    k3 += 2;
                for (int l3 = i; l3 >= 0; l3--)
                    if (groundArray[l3][i3][j3] == null)
                        groundArray[l3][i3][j3] = new Ground(l3, i3, j3);

                Ground class30_sub3_1 = groundArray[i][i3][j3];
                class30_sub3_1.obj5Array[class30_sub3_1.anInt1317] = class28;
                class30_sub3_1.anIntArray1319[class30_sub3_1.anInt1317] = k3;
                class30_sub3_1.anInt1320 |= k3;
                class30_sub3_1.anInt1317++;
            }

        }

        if (flag)
            obj5Cache[obj5CacheCurrPos++] = class28;
        return true;
    }

    public void clearObj5Cache() {
        for (int i = 0; i < obj5CacheCurrPos; i++) {
            StaticObject object5 = obj5Cache[i];
            method289(object5);
            obj5Cache[i] = null;
        }

        obj5CacheCurrPos = 0;
    }

    private void method289(StaticObject class28) {
        for (int j = class28.anInt523; j <= class28.anInt524; j++) {
            for (int k = class28.anInt525; k <= class28.anInt526; k++) {
                Ground class30_sub3 = groundArray[class28.anInt517][j][k];
                if (class30_sub3 != null) {
                    for (int l = 0; l < class30_sub3.anInt1317; l++) {
                        if (class30_sub3.obj5Array[l] != class28)
                            continue;
                        class30_sub3.anInt1317--;
                        for (int i1 = l; i1 < class30_sub3.anInt1317; i1++) {
                            class30_sub3.obj5Array[i1] = class30_sub3.obj5Array[i1 + 1];
                            class30_sub3.anIntArray1319[i1] = class30_sub3.anIntArray1319[i1 + 1];
                        }

                        class30_sub3.obj5Array[class30_sub3.anInt1317] = null;
                        break;
                    }

                    class30_sub3.anInt1320 = 0;
                    for (int j1 = 0; j1 < class30_sub3.anInt1317; j1++)
                        class30_sub3.anInt1320 |= class30_sub3.anIntArray1319[j1];

                }
            }

        }

    }

    public void method290(int i, int k, int l, int i1) {
        Ground class30_sub3 = groundArray[i1][l][i];
        if (class30_sub3 == null)
            return;
        Object2 class26 = class30_sub3.obj2;
        if (class26 != null) {
            int j1 = l * 128 + 64;
            int k1 = i * 128 + 64;
            class26.anInt500 = j1 + ((class26.anInt500 - j1) * k) / 16;
            class26.anInt501 = k1 + ((class26.anInt501 - k1) * k) / 16;
        }
    }

    public void method291(int i, int j, int k, byte byte0) {
        Ground class30_sub3 = groundArray[j][i][k];
        if (byte0 != -119)
            aBoolean434 = !aBoolean434;
        if (class30_sub3 != null) {
            class30_sub3.obj1 = null;
        }
    }

    public void method292(int j, int k, int l) {
        Ground class30_sub3 = groundArray[k][l][j];
        if (class30_sub3 != null) {
            class30_sub3.obj2 = null;
        }
    }

    public void method293(int i, int k, int l) {
        Ground class30_sub3 = groundArray[i][k][l];
        if (class30_sub3 == null)
            return;
        for (int j1 = 0; j1 < class30_sub3.anInt1317; j1++) {
            StaticObject class28 = class30_sub3.obj5Array[j1];
            if ((class28.uid >> 29 & 3) == 2 && class28.anInt523 == k
                    && class28.anInt525 == l) {
                method289(class28);
                return;
            }
        }

    }

    public void method294(int i, int j, int k) {
        Ground class30_sub3 = groundArray[i][k][j];
        if (class30_sub3 == null)
            return;
        class30_sub3.obj3 = null;
    }

    public void method295(int i, int j, int k) {
        Ground class30_sub3 = groundArray[i][j][k];
        if (class30_sub3 != null) {
            class30_sub3.obj4 = null;
        }
    }

    public Object1 method296(int i, int j, int k) {
        Ground class30_sub3 = groundArray[i][j][k];
        if (class30_sub3 == null)
            return null;
        else
            return class30_sub3.obj1;
    }

    public Object2 method297(int i, int k, int l) {
        Ground class30_sub3 = groundArray[l][i][k];
        if (class30_sub3 == null)
            return null;
        else
            return class30_sub3.obj2;
    }

    public StaticObject method298(int i, int j, int k) {
        Ground class30_sub3 = groundArray[k][i][j];
        if (class30_sub3 == null)
            return null;
        for (int l = 0; l < class30_sub3.anInt1317; l++) {
            StaticObject class28 = class30_sub3.obj5Array[l];
            if ((class28.uid >> 29 & 3) == 2 && class28.anInt523 == i
                    && class28.anInt525 == j)
                return class28;
        }
        return null;
    }

    public Object3 method299(int i, int j, int k) {
        Ground class30_sub3 = groundArray[k][j][i];
        if (class30_sub3 == null || class30_sub3.obj3 == null)
            return null;
        else
            return class30_sub3.obj3;
    }

    public long method300(int i, int j, int k) {
        Ground class30_sub3 = groundArray[i][j][k];
        if (class30_sub3 == null || class30_sub3.obj1 == null)
            return 0;
        else
            return class30_sub3.obj1.uid;
    }

    public long method301(int i, int j, int l) {
        Ground class30_sub3 = groundArray[i][j][l];
        if (class30_sub3 == null || class30_sub3.obj2 == null)
            return 0;
        else
            return class30_sub3.obj2.uid;
    }

    public long method302(int i, int j, int k) {
        Ground class30_sub3 = groundArray[i][j][k];
        if (class30_sub3 == null)
            return 0;
        for (int l = 0; l < class30_sub3.anInt1317; l++) {
            StaticObject class28 = class30_sub3.obj5Array[l];
            if ((class28.uid >> 29 & 3) == 2 && class28.anInt523 == j
                    && class28.anInt525 == k)
                return class28.uid;
        }

        return 0;
    }

    public long method303(int i, int j, int k) {
        Ground class30_sub3 = groundArray[i][j][k];
        if (class30_sub3 == null || class30_sub3.obj3 == null)
            return 0;
        else
            return class30_sub3.obj3.uid;
    }

    public int method304(int i, int j, int k, long l) {
        Ground class30_sub3 = groundArray[i][j][k];
        if (class30_sub3 == null)
            return -1;
        if (class30_sub3.obj1 != null && class30_sub3.obj1.uid == l)
            return class30_sub3.obj1.aByte281 & 0xff;
        if (class30_sub3.obj2 != null && class30_sub3.obj2.uid == l)
            return class30_sub3.obj2.aByte506 & 0xff;
        if (class30_sub3.obj3 != null && class30_sub3.obj3.uid == l)
            return class30_sub3.obj3.aByte816 & 0xff;
        for (int i1 = 0; i1 < class30_sub3.anInt1317; i1++)
            if (class30_sub3.obj5Array[i1].uid == l)
                return class30_sub3.obj5Array[i1].aByte530 & 0xff;

        return -1;
    }

    public void method305(int i, int k, int i1) {
        int j = 100;
        int l = 768;
        int j1 = (int)Math.sqrt(k * k + i * i + i1 * i1);
        int k1 = l * j1 >> 8;
        for (int l1 = 0; l1 < anInt437; l1++) {
            for (int i2 = 0; i2 < anInt438; i2++) {
                for (int j2 = 0; j2 < anInt439; j2++) {
                    Ground class30_sub3 = groundArray[l1][i2][j2];
                    if (class30_sub3 != null) {
                        Object1 class10 = class30_sub3.obj1;
                        if (class10 != null
                                && class10.aClass30_Sub2_Sub4_278 != null
                                && class10.aClass30_Sub2_Sub4_278.normals != null) {
                            method307(l1, 1, 1, i2, j2,
                                    (Model) class10.aClass30_Sub2_Sub4_278);
                            if (class10.aClass30_Sub2_Sub4_279 != null
                                    && class10.aClass30_Sub2_Sub4_279.normals != null) {
                                method307(l1, 1, 1, i2, j2,
                                        (Model) class10.aClass30_Sub2_Sub4_279);
                                method308(
                                        (Model) class10.aClass30_Sub2_Sub4_278,
                                        (Model) class10.aClass30_Sub2_Sub4_279,
                                        0, 0, 0, false);
                                ((Model) class10.aClass30_Sub2_Sub4_279)
                                        .setLighting(j, k1, k, i, i1);
                            }
                            ((Model) class10.aClass30_Sub2_Sub4_278).setLighting(
                                    j, k1, k, i, i1);
                        }
                        for (int k2 = 0; k2 < class30_sub3.anInt1317; k2++) {
                            StaticObject class28 = class30_sub3.obj5Array[k2];
                            if (class28 != null
                                    && class28.aClass30_Sub2_Sub4_521 != null
                                    && class28.aClass30_Sub2_Sub4_521.normals != null) {
                                method307(
                                        l1,
                                        (class28.anInt524 - class28.anInt523) + 1,
                                        (class28.anInt526 - class28.anInt525) + 1,
                                        i2, j2,
                                        (Model) class28.aClass30_Sub2_Sub4_521);
                                ((Model) class28.aClass30_Sub2_Sub4_521)
                                        .setLighting(j, k1, k, i, i1);
                            }
                        }

                        Object3 class49 = class30_sub3.obj3;
                        if (class49 != null
                                && class49.aClass30_Sub2_Sub4_814.normals != null) {
                            method306(i2, l1,
                                    (Model) class49.aClass30_Sub2_Sub4_814, j2);
                            ((Model) class49.aClass30_Sub2_Sub4_814).setLighting(
                                    j, k1, k, i, i1);
                        }
                    }
                }

            }

        }

    }

    private void method306(int i, int j, Model model, int k) {
        if (i < anInt438) {
            Ground class30_sub3 = groundArray[j][i + 1][k];
            if (class30_sub3 != null
                    && class30_sub3.obj3 != null
                    && class30_sub3.obj3.aClass30_Sub2_Sub4_814.normals != null)
                method308(model,
                        (Model) class30_sub3.obj3.aClass30_Sub2_Sub4_814, 128,
                        0, 0, true);
        }
        if (k < anInt438) {
            Ground class30_sub3_1 = groundArray[j][i][k + 1];
            if (class30_sub3_1 != null
                    && class30_sub3_1.obj3 != null
                    && class30_sub3_1.obj3.aClass30_Sub2_Sub4_814.normals != null)
                method308(model,
                        (Model) class30_sub3_1.obj3.aClass30_Sub2_Sub4_814, 0,
                        0, 128, true);
        }
        if (i < anInt438 && k < anInt439) {
            Ground class30_sub3_2 = groundArray[j][i + 1][k + 1];
            if (class30_sub3_2 != null
                    && class30_sub3_2.obj3 != null
                    && class30_sub3_2.obj3.aClass30_Sub2_Sub4_814.normals != null)
                method308(model,
                        (Model) class30_sub3_2.obj3.aClass30_Sub2_Sub4_814,
                        128, 0, 128, true);
        }
        if (i < anInt438 && k > 0) {
            Ground class30_sub3_3 = groundArray[j][i + 1][k - 1];
            if (class30_sub3_3 != null
                    && class30_sub3_3.obj3 != null
                    && class30_sub3_3.obj3.aClass30_Sub2_Sub4_814.normals != null)
                method308(model,
                        (Model) class30_sub3_3.obj3.aClass30_Sub2_Sub4_814,
                        128, 0, -128, true);
        }
    }

    private void method307(int i, int j, int k, int l, int i1, Model model) {
        boolean flag = true;
        int j1 = l;
        int k1 = l + j;
        int l1 = i1 - 1;
        int i2 = i1 + k;
        for (int j2 = i; j2 <= i + 1; j2++)
            if (j2 != anInt437) {
                for (int k2 = j1; k2 <= k1; k2++)
                    if (k2 >= 0 && k2 < anInt438) {
                        for (int l2 = l1; l2 <= i2; l2++)
                            if (l2 >= 0
                                    && l2 < anInt439
                                    && (!flag || k2 >= k1 || l2 >= i2 || l2 < i1
                                    && k2 != l)) {
                                Ground class30_sub3 = groundArray[j2][k2][l2];
                                if (class30_sub3 != null) {
                                    int i3 = (tileHeights[j2][k2][l2]
                                            + tileHeights[j2][k2 + 1][l2]
                                            + tileHeights[j2][k2][l2 + 1] + tileHeights[j2][k2 + 1][l2 + 1])
                                            / 4
                                            - (tileHeights[i][l][i1]
                                            + tileHeights[i][l + 1][i1]
                                            + tileHeights[i][l][i1 + 1] + tileHeights[i][l + 1][i1 + 1])
                                            / 4;
                                    Object1 class10 = class30_sub3.obj1;
                                    if (class10 != null
                                            && class10.aClass30_Sub2_Sub4_278 != null
                                            && class10.aClass30_Sub2_Sub4_278.normals != null)
                                        method308(
                                                model,
                                                (Model) class10.aClass30_Sub2_Sub4_278,
                                                (k2 - l) * 128 + (1 - j) * 64,
                                                i3, (l2 - i1) * 128 + (1 - k)
                                                        * 64, flag);
                                    if (class10 != null
                                            && class10.aClass30_Sub2_Sub4_279 != null
                                            && class10.aClass30_Sub2_Sub4_279.normals != null)
                                        method308(
                                                model,
                                                (Model) class10.aClass30_Sub2_Sub4_279,
                                                (k2 - l) * 128 + (1 - j) * 64,
                                                i3, (l2 - i1) * 128 + (1 - k)
                                                        * 64, flag);
                                    for (int j3 = 0; j3 < class30_sub3.anInt1317; j3++) {
                                        StaticObject class28 = class30_sub3.obj5Array[j3];
                                        if (class28 != null
                                                && class28.aClass30_Sub2_Sub4_521 != null
                                                && class28.aClass30_Sub2_Sub4_521.normals != null) {
                                            int k3 = (class28.anInt524 - class28.anInt523) + 1;
                                            int l3 = (class28.anInt526 - class28.anInt525) + 1;
                                            method308(
                                                    model,
                                                    (Model) class28.aClass30_Sub2_Sub4_521,
                                                    (class28.anInt523 - l)
                                                            * 128 + (k3 - j)
                                                            * 64, i3,
                                                    (class28.anInt525 - i1)
                                                            * 128 + (l3 - k)
                                                            * 64, flag);
                                        }
                                    }

                                }
                            }

                    }

                j1--;
                flag = false;
            }

    }

    private void method308(Model model, Model model_1, int i, int j, int k,
                           boolean flag) {
        anInt488++;
        int l = 0;
        int ai[] = model_1.verticesX;
        int i1 = model_1.vertex_count;
        for (int j1 = 0; j1 < model.vertex_count; j1++) {
            VertexNormal class33 = model.normals[j1];
            VertexNormal class33_1 = model.shared_normals[j1];
            if (class33_1.magnitude != 0) {
                int i2 = model.verticesY[j1] - j;
                if (i2 <= model_1.maxY) {
                    int j2 = model.verticesX[j1] - i;
                    if (j2 >= model_1.minX && j2 <= model_1.maxX) {
                        int k2 = model.verticesZ[j1] - k;
                        if (k2 >= model_1.minZ && k2 <= model_1.maxZ) {
                            for (int l2 = 0; l2 < i1; l2++) {
                                VertexNormal class33_2 = model_1.normals[l2];
                                VertexNormal class33_3 = model_1.shared_normals[l2];
                                if (j2 == ai[l2]
                                        && k2 == model_1.verticesZ[l2]
                                        && i2 == model_1.verticesY[l2]
                                        && class33_3.magnitude != 0) {
                                    class33.x += class33_3.x;
                                    class33.y += class33_3.y;
                                    class33.z += class33_3.z;
                                    class33.magnitude += class33_3.magnitude;
                                    class33_2.x += class33_1.x;
                                    class33_2.y += class33_1.y;
                                    class33_2.z += class33_1.z;
                                    class33_2.magnitude += class33_1.magnitude;
                                    l++;
                                    anIntArray486[j1] = anInt488;
                                    anIntArray487[l2] = anInt488;
                                }
                            }

                        }
                    }
                }
            }
        }

        if (l < 3 || !flag)
            return;
        for (int k1 = 0; k1 < model.face_count; k1++)
            if (anIntArray486[model.trianglesX[k1]] == anInt488
                    && anIntArray486[model.trianglesY[k1]] == anInt488
                    && anIntArray486[model.trianglesZ[k1]] == anInt488)
                model.drawType[k1] = -1;

        for (int l1 = 0; l1 < model_1.face_count; l1++)
            if (anIntArray487[model_1.trianglesX[l1]] == anInt488
                    && anIntArray487[model_1.trianglesY[l1]] == anInt488
                    && anIntArray487[model_1.trianglesZ[l1]] == anInt488)
                model_1.drawType[l1] = -1;

    }

    public void method309(int ai[], int i, int k, int l, int i1) {
        int j = 512;// was parameter
        Ground class30_sub3 = groundArray[k][l][i1];
        if (class30_sub3 == null)
            return;
        Class43 class43 = class30_sub3.aClass43_1311;
        if (class43 != null) {
            int j1 = class43.anInt722;
            if (j1 == 0)
                return;
            for (int k1 = 0; k1 < 4; k1++) {
                ai[i] = j1;
                ai[i + 1] = j1;
                ai[i + 2] = j1;
                ai[i + 3] = j1;
                i += j;
            }

            return;
        }
        ShapedTile class40 = class30_sub3.aClass40_1312;
        if (class40 == null)
            return;
        int l1 = class40.shape;
        int i2 = class40.rotation;
        int j2 = class40.colourRGB;
        int k2 = class40.colourRGBA;
        int ai1[] = anIntArrayArray489[l1];
        int ai2[] = anIntArrayArray490[i2];
        int l2 = 0;
        if (j2 != 0) {
            for (int i3 = 0; i3 < 4; i3++) {
                ai[i] = ai1[ai2[l2++]] != 0 ? k2 : j2;
                ai[i + 1] = ai1[ai2[l2++]] != 0 ? k2 : j2;
                ai[i + 2] = ai1[ai2[l2++]] != 0 ? k2 : j2;
                ai[i + 3] = ai1[ai2[l2++]] != 0 ? k2 : j2;
                i += j;
            }

            return;
        }
        for (int j3 = 0; j3 < 4; j3++) {
            if (ai1[ai2[l2++]] != 0)
                ai[i] = k2;
            if (ai1[ai2[l2++]] != 0)
                ai[i + 1] = k2;
            if (ai1[ai2[l2++]] != 0)
                ai[i + 2] = k2;
            if (ai1[ai2[l2++]] != 0)
                ai[i + 3] = k2;
            i += j;
        }

    }

    	public static void method310(int i, int j, int k, int l, int ai[]) {
    	    anInt495 = 0;
    	    anInt496 = 0;
    	    anInt497 = k;
    	    anInt498 = l;
    	    anInt493 = k / 2;
    	    anInt494 = l / 2;
    	    
    	    // Always allocate for MAX_FAR_Z to ensure consistency across all modes
    	    boolean aflag[][][][] = new boolean[9][32][MAX_FAR_Z * 2 + 3][MAX_FAR_Z * 2 + 3];

    	    for (int i1 = 128; i1 <= 384; i1 += 32) {
    	        for (int j1 = 0; j1 < 2048; j1 += 64) {
    	            camUpDownY = Model.SINE[i1];
    	            camUpDownX = Model.COSINE[i1];
    	            camLeftRightY = Model.SINE[j1];
    	            camLeftRightX = Model.COSINE[j1];
    	            int l1 = (i1 - 128) / 32;
    	            int j2 = j1 / 64;
    	            
    	            // Use current farZ for iteration but MAX_FAR_Z for array indexing
    	            for (int l2 = -farZ; l2 <= farZ; l2++) {
    	                for (int j3 = -farZ; j3 <= farZ; j3++) {
    	                    int k3 = l2 * 128;
    	                    int i4 = j3 * 128;
    	                    boolean flag2 = false;
    	                    
    	                    for (int k4 = -i; k4 <= j; k4 += 128) {
    	                        if (method311(ai[l1] + k4, i4, k3)) {
    	                            flag2 = true;
    	                            break;
    	                        }
    	                    }
    	                    
    	                    // Always use MAX_FAR_Z offset for array indexing
    	                    int xIndex = l2 + MAX_FAR_Z + 1;
    	                    int yIndex = j3 + MAX_FAR_Z + 1;
    	                    
    	                    if (xIndex >= 0 && xIndex < aflag[l1][j2].length &&
    	                        yIndex >= 0 && yIndex < aflag[l1][j2][0].length) {
    	                        aflag[l1][j2][xIndex][yIndex] = flag2;
    	                    }
    	                }
    	            }
    	        }
    	    }

    	    // Smooth visibility map edges
    	    for (int k1 = 0; k1 < 8; k1++) {
    	        for (int i2 = 0; i2 < 32; i2++) {
    	            for (int k2 = -farZ; k2 < farZ; k2++) {
    	                for (int i3 = -farZ; i3 < farZ; i3++) {
    	                    boolean flag1 = false;
    	                    
    	                    label0:
    	                    for (int l3 = -1; l3 <= 1; l3++) {
    	                        for (int j4 = -1; j4 <= 1; j4++) {
    	                            // Use MAX_FAR_Z for all array index calculations
    	                            int xIdx = k2 + l3 + MAX_FAR_Z + 1;
    	                            int yIdx = i3 + j4 + MAX_FAR_Z + 1;
    	                            
    	                            if (xIdx >= 0 && xIdx < aflag[k1][i2].length &&
    	                                yIdx >= 0 && yIdx < aflag[k1][i2][0].length) {
    	                                if (aflag[k1][i2][xIdx][yIdx]) {
    	                                    flag1 = true;
    	                                    break label0;
    	                                }
    	                            }
    	                            
    	                            int nextAngle = (i2 + 1) % 31;
    	                            if (xIdx >= 0 && xIdx < aflag[k1][nextAngle].length &&
    	                                yIdx >= 0 && yIdx < aflag[k1][nextAngle][0].length) {
    	                                if (aflag[k1][nextAngle][xIdx][yIdx]) {
    	                                    flag1 = true;
    	                                    break label0;
    	                                }
    	                            }
    	                            
    	                            if (k1 + 1 < 8) {
    	                                if (xIdx >= 0 && xIdx < aflag[k1 + 1][i2].length &&
    	                                    yIdx >= 0 && yIdx < aflag[k1 + 1][i2][0].length) {
    	                                    if (aflag[k1 + 1][i2][xIdx][yIdx]) {
    	                                        flag1 = true;
    	                                        break label0;
    	                                    }
    	                                }
    	                                
    	                                if (xIdx >= 0 && xIdx < aflag[k1 + 1][nextAngle].length &&
    	                                    yIdx >= 0 && yIdx < aflag[k1 + 1][nextAngle][0].length) {
    	                                    if (aflag[k1 + 1][nextAngle][xIdx][yIdx]) {
    	                                        flag1 = true;
    	                                        break label0;
    	                                    }
    	                                }
    	                            }
    	                        }
    	                    }
    	                    
    	                    // Store in global visibility map using MAX_FAR_Z offset
    	                    int visX = k2 + MAX_FAR_Z;
    	                    int visY = i3 + MAX_FAR_Z;
    	                    
    	                    if (visX >= 0 && visX < visibilityMap[k1][i2].length &&
    	                        visY >= 0 && visY < visibilityMap[k1][i2][0].length) {
    	                        visibilityMap[k1][i2][visX][visY] = flag1;
    	                    }
    	                }
    	            }
    	        }
    	    }
    	}
    private static boolean method311(int i, int j, int k) {
        int l = j * camLeftRightY + k * camLeftRightX >> 16;
        int i1 = j * camLeftRightX - k * camLeftRightY >> 16;
        int j1 = i * camUpDownY + i1 * camUpDownX >> 16;
        int k1 = i * camUpDownX - i1 * camUpDownY >> 16;
        
        // Scale render distance clamp with farZ
        int maxDistance = farZ * 100;
        
        // Check if point is in valid viewing frustum
        if (j1 < 50 || j1 > maxDistance) {
            return false;
        }
        
        int l1 = anInt493 + (l * focalLength) / j1;
        int i2 = anInt494 + (k1 * focalLength) / j1;
        
        return l1 >= anInt495 && l1 <= anInt497 && i2 >= anInt496 && i2 <= anInt498;
    }

    public void method312(int i, int j) {
        aBoolean467 = true;
        anInt468 = j;
        anInt469 = i;
        anInt470 = -1;
        anInt471 = -1;
    }

    public void draw(int i, int j, int k, int l, int i1, int j1) {
        long drawStart = System.nanoTime();
        
        // Depth buffer setup (existing code)
        if (Rasterizer.saveDepth = Client.getUserSettings().isFog()) {
            if (Rasterizer.depthBuffer == null
                || Rasterizer.depthBuffer.length != DrawingArea.pixels.length) {
                Rasterizer.depthBuffer = new float[DrawingArea.pixels.length];
                float depthValue = farZ * 100f;
                for (int index = 0; index < Rasterizer.depthBuffer.length; index++) {
                    Rasterizer.depthBuffer[index] = depthValue;
                }
            }
        } else if (Rasterizer.depthBuffer != null) {
            Rasterizer.depthBuffer = null;
        }

        // Camera clamping (existing code)
        if (i < 0) i = 0;
        else if (i >= anInt438 * 128) i = anInt438 * 128 - 1;
        if (j < 0) j = 0;
        else if (j >= anInt439 * 128) j = anInt439 * 128 - 1;
        if (j1 < 128) j1 = 128;
        else if (j1 > 383) j1 = 383;
        
        // Camera setup (existing code)
        anInt448++;
        camUpDownY = Model.SINE[j1];
        camUpDownX = Model.COSINE[j1];
        camLeftRightY = Model.SINE[k];
        camLeftRightX = Model.COSINE[k];
        aBooleanArrayArray492 = visibilityMap[(j1 - 128) / 32][k / 64];
        
        xCameraPos = i;
        zCameraPos = l;
        yCameraPos = j;
        Scene_cameraXTile = i / 128;
        Scene_cameraYTile = j / 128;
        anInt447 = i1;
        anInt449 = Scene_cameraXTile - farZ;
        if (anInt449 < 0) anInt449 = 0;
        anInt451 = Scene_cameraYTile - farZ;
        if (anInt451 < 0) anInt451 = 0;
        anInt450 = Scene_cameraXTile + farZ;
        if (anInt450 > anInt438) anInt450 = anInt438;
        anInt452 = Scene_cameraYTile + farZ;
        if (anInt452 > anInt439) anInt452 = anInt439;
        
        long afterSetup = System.nanoTime();
        
        method319(); // Occluders
        
        long afterOccluders = System.nanoTime();
        
        // ===== PARALLEL VISIBILITY CALCULATION =====
        calculateVisibilityParallel();
        
        long afterVisibility = System.nanoTime();
        
        // Count visible tiles
        anInt446 = 0;
        for (int plane = anInt442; plane < anInt437; plane++) {
            for (int x = anInt449; x < anInt450; x++) {
                for (int y = anInt451; y < anInt452; y++) {
                    Ground tile = groundArray[plane][x][y];
                    if (tile != null && tile.aBoolean1322) {
                        anInt446++;
                    }
                }
            }
        }
        
        if (anInt446 == 0) {
            aBoolean467 = false;
            return;
        }
        
        long afterCounting = System.nanoTime();
        
        // ===== RENDERING PASSES (Keep single-threaded - must write to same pixel buffer) =====
        // First render pass
        for (int l1 = anInt442; l1 < anInt437; l1++) {
            Ground aclass30_sub3_1[][] = groundArray[l1];
            for (int l2 = -farZ; l2 <= 0; l2++) {
                int i3 = Scene_cameraXTile + l2;
                int k3 = Scene_cameraXTile - l2;
                if (i3 >= anInt449 || k3 < anInt450) {
                    for (int i4 = -farZ; i4 <= 0; i4++) {
                        int k4 = Scene_cameraYTile + i4;
                        int i5 = Scene_cameraYTile - i4;
                        if (i3 >= anInt449) {
                            if (k4 >= anInt451) {
                                Ground tile = aclass30_sub3_1[i3][k4];
                                if (tile != null && tile.aBoolean1322)
                                    method314(tile, true);
                            }
                            if (i5 < anInt452) {
                                Ground tile = aclass30_sub3_1[i3][i5];
                                if (tile != null && tile.aBoolean1322)
                                    method314(tile, true);
                            }
                        }
                        if (k3 < anInt450) {
                            if (k4 >= anInt451) {
                                Ground tile = aclass30_sub3_1[k3][k4];
                                if (tile != null && tile.aBoolean1322)
                                    method314(tile, true);
                            }
                            if (i5 < anInt452) {
                                Ground tile = aclass30_sub3_1[k3][i5];
                                if (tile != null && tile.aBoolean1322)
                                    method314(tile, true);
                            }
                        }
                        if (anInt446 == 0) {
                            aBoolean467 = false;
                            return;
                        }
                    }
                }
            }
        }
        
        long afterFirstPass = System.nanoTime();
        
        // Second render pass
        for (int j2 = anInt442; j2 < anInt437; j2++) {
            Ground aclass30_sub3_2[][] = groundArray[j2];
            for (int j3 = -farZ; j3 <= 0; j3++) {
                int l3 = Scene_cameraXTile + j3;
                int j4 = Scene_cameraXTile - j3;
                if (l3 >= anInt449 || j4 < anInt450) {
                    for (int l4 = -farZ; l4 <= 0; l4++) {
                        int j5 = Scene_cameraYTile + l4;
                        int k5 = Scene_cameraYTile - l4;
                        if (l3 >= anInt449) {
                            if (j5 >= anInt451) {
                                Ground tile = aclass30_sub3_2[l3][j5];
                                if (tile != null && tile.aBoolean1322)
                                    method314(tile, false);
                            }
                            if (k5 < anInt452) {
                                Ground tile = aclass30_sub3_2[l3][k5];
                                if (tile != null && tile.aBoolean1322)
                                    method314(tile, false);
                            }
                        }
                        if (j4 < anInt450) {
                            if (j5 >= anInt451) {
                                Ground tile = aclass30_sub3_2[j4][j5];
                                if (tile != null && tile.aBoolean1322)
                                    method314(tile, false);
                            }
                            if (k5 < anInt452) {
                                Ground tile = aclass30_sub3_2[j4][k5];
                                if (tile != null && tile.aBoolean1322)
                                    method314(tile, false);
                            }
                        }
                        if (anInt446 == 0) {
                            aBoolean467 = false;
                            return;
                        }
                    }
                }
            }
        }
        
        long afterSecondPass = System.nanoTime();
        
        aBoolean467 = false;
        
        // Print timing
        if (anInt448 % 50 == 0) {
            System.out.println("===== SCENE DRAW (MULTITHREADED) =====");
            System.out.println(String.format("Setup:           %.2f ms", (afterSetup - drawStart) / 1_000_000.0));
            System.out.println(String.format("Occluders:       %.2f ms", (afterOccluders - afterSetup) / 1_000_000.0));
            System.out.println(String.format("Visibility (MT): %.2f ms  <-- PARALLEL", (afterVisibility - afterOccluders) / 1_000_000.0));
            System.out.println(String.format("Tile counting:   %.2f ms", (afterCounting - afterVisibility) / 1_000_000.0));
            System.out.println(String.format("First pass:      %.2f ms", (afterFirstPass - afterCounting) / 1_000_000.0));
            System.out.println(String.format("Second pass:     %.2f ms", (afterSecondPass - afterFirstPass) / 1_000_000.0));
            System.out.println(String.format("Visible tiles:   %d", anInt446));
            System.out.println(String.format("Threads used:    %d", SCENE_THREADS));
            System.out.println(String.format("---------------------------------------"));
            System.out.println(String.format("TOTAL:           %.2f ms", (afterSecondPass - drawStart) / 1_000_000.0));
            System.out.println("=======================================\n");
        }
    }


    void renderTileMarkers() {
        try {
            if (parentFrame != null && parentFrame.getPluginPanel() != null) {
                TileMarkersPlugin tileMarkers = parentFrame.getPluginPanel().getTileMarkersPlugin();
                if (tileMarkers != null && tileMarkers.isEnabled()) {
                    Map<TileCoordinate, TileMarker> markers = tileMarkers.getTileMarkers();
                    
                    // Calculate world bounds (base coordinates + local bounds)
                    int worldMinX = Client.baseX + anInt449;
                    int worldMaxX = Client.baseX + anInt450;
                    int worldMinY = Client.baseY + anInt451;
                    int worldMaxY = Client.baseY + anInt452;
                    
                    for (TileMarker marker : markers.values()) {
                        int worldX = marker.getWorldX();
                        int worldY = marker.getWorldY();
                        int plane = marker.getPlane();
                        
                        if (marker.getPlane() == anInt447 &&
                            worldX >= worldMinX && worldX < worldMaxX &&
                            worldY >= worldMinY && worldY < worldMaxY) {
                            
                            int localX = worldX - Client.baseX;
                            int localY = worldY - Client.baseY;
                            
                            // Pass the label through to the rendering method
                            renderTileMarkerOverlay(localX, localY, marker.getColor(), marker.getLabel());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error rendering tile markers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void renderTileMarkerOverlay(int tileX, int tileY, Color color, String label) {
        try {
            //System.out.println("DEBUG: renderTileMarkerOverlay() called for tile (" + tileX + ", " + tileY + ")");
            
            // Calculate world coordinates for tile center
            int centerX = tileX * 128 + 64;
            int centerY = tileY * 128 + 64;
            int height = tileHeights[anInt447][tileX][tileY];
            
            // Draw a single tile marker with label
            renderTileMarkerSquare(centerX, height, centerY, color, label);
            
            //System.out.println("DEBUG: renderTileMarkerOverlay() completed");
            
        } catch (Exception e) {
            System.err.println("DEBUG: Error in renderTileMarkerOverlay: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void renderTileMarkerSquare(int worldX, int height, int worldY, Color color, String label) {
        try {
            // Calculate the four corners of a 128x128 tile
            int[] cornerWorldX = {worldX - 64, worldX + 64, worldX + 64, worldX - 64};
            int[] cornerWorldY = {worldY - 64, worldY - 64, worldY + 64, worldY + 64};
            
            // Project all corners to screen coordinates
            int[] screenX = new int[4];
            int[] screenY = new int[4];
            boolean allValid = true;
            
            for (int i = 0; i < 4; i++) {
                int relX = cornerWorldX[i] - xCameraPos;
                int relY = height - zCameraPos;
                int relZ = cornerWorldY[i] - yCameraPos;
                
                int rotX = relZ * camLeftRightY + relX * camLeftRightX >> 16;
                int rotZ = relZ * camLeftRightX - relX * camLeftRightY >> 16;
                int finalY = relY * camUpDownX - rotZ * camUpDownY >> 16;
                int finalZ = relY * camUpDownY + rotZ * camUpDownX >> 16;
                
                if (finalZ < 50) {
                    allValid = false;
                    break;
                }
                
                screenX[i] = Rasterizer.textureInt1 + (rotX * WorldController.focalLength) / finalZ;
                screenY[i] = Rasterizer.textureInt2 + (finalY * WorldController.focalLength) / finalZ;
            }
            
            if (!allValid) return;
            
            // Fill the interior with transparent color first
            int fillColor = 0x40000000; // Semi-transparent grey
            fillQuadrilateral(screenX, screenY, fillColor);
            
            // Draw the border outline
            int borderColor = color.getRGB() | 0xFF000000;
            drawLine(screenX[0], screenY[0], screenX[1], screenY[1], borderColor);
            drawLine(screenX[1], screenY[1], screenX[2], screenY[2], borderColor);
            drawLine(screenX[2], screenY[2], screenX[3], screenY[3], borderColor);
            drawLine(screenX[3], screenY[3], screenX[0], screenY[0], borderColor);
            
         // Draw label if present
            if (label != null && !label.trim().isEmpty()) {
                // Calculate center of tile on screen for label placement
                int centerScreenX = (screenX[0] + screenX[1] + screenX[2] + screenX[3]) / 4;
                int centerScreenY = (screenY[0] + screenY[1] + screenY[2] + screenY[3]) / 4;
                
                // Draw label text centered over the tile
                drawLabelText(centerScreenX, centerScreenY, label);
            }
            
        } catch (Exception e) {
            System.err.println("DEBUG: Exception in renderTileMarkerSquare: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void drawLabelText(int x, int y, String text) {
        try {
            if (text == null || text.trim().isEmpty()) return;
            
            String trimmedText = text.trim().toUpperCase(); // Convert to uppercase for consistency
            int charSpacing = 6;
            int textWidth = trimmedText.length() * charSpacing;
            
            // Center the text horizontally around x position
            int startX = x - (textWidth / 2);
            
            // Position text ABOVE the tile center (not below)
            int textY = y - 10; // 10 pixels above center
            
            // Draw each character - NO BACKGROUND BOX
            for (int i = 0; i < trimmedText.length(); i++) {
                char c = trimmedText.charAt(i);
                drawChar(startX + (i * charSpacing), textY, c, 0xFFFFFFFF); // Pure white
            }
            
        } catch (Exception e) {
            System.err.println("Error drawing label text: " + e.getMessage());
        }
    }

    private void drawChar(int x, int y, char c, int color) {
        // Simple 5x7 bitmap font patterns
        // 1 = pixel on, 0 = pixel off
        byte[][] font = getCharPattern(c);
        
        if (font == null) return;
        
        for (int row = 0; row < font.length; row++) {
            for (int col = 0; col < font[row].length; col++) {
                if (font[row][col] == 1) {
                    int pixelX = x + col;
                    int pixelY = y + row;
                    
                    if (pixelX >= 0 && pixelX < DrawingArea.width && pixelY >= 0 && pixelY < DrawingArea.height) {
                        int index = pixelY * DrawingArea.width + pixelX;
                        if (index >= 0 && index < DrawingArea.pixels.length) {
                            DrawingArea.pixels[index] = color;
                        }
                    }
                }
            }
        }
    }

    private byte[][] getCharPattern(char c) {
        // 5x7 bitmap patterns for common characters
        // Add more as needed
        switch (Character.toUpperCase(c)) {
            case 'A': return new byte[][]{
                {0,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,1,1,1,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1}
            };
            case 'B': return new byte[][]{
                {1,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,1,1,1,0}
            };
            case 'C': return new byte[][]{
                {0,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,0,0,0,1},
                {0,1,1,1,0}
            };
            case 'D': return new byte[][]{
                {1,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,1,1,1,0}
            };
            case 'E': return new byte[][]{
                {1,1,1,1,1},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,1,1,1,0},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,1,1,1,1}
            };
            case 'F': return new byte[][]{
                {1,1,1,1,1},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,1,1,1,0},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,0,0,0,0}
            };
            case 'G': return new byte[][]{
                {0,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,0},
                {1,0,1,1,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {0,1,1,1,0}
            };
            case 'H': return new byte[][]{
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,1,1,1,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1}
            };
            case 'I': return new byte[][]{
                {1,1,1,1,1},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {1,1,1,1,1}
            };
            case 'J': return new byte[][]{
                {0,0,1,1,1},
                {0,0,0,1,0},
                {0,0,0,1,0},
                {0,0,0,1,0},
                {1,0,0,1,0},
                {1,0,0,1,0},
                {0,1,1,0,0}
            };
            case 'K': return new byte[][]{
                {1,0,0,0,1},
                {1,0,0,1,0},
                {1,0,1,0,0},
                {1,1,0,0,0},
                {1,0,1,0,0},
                {1,0,0,1,0},
                {1,0,0,0,1}
            };
            case 'L': return new byte[][]{
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,1,1,1,1}
            };
            case 'M': return new byte[][]{
                {1,0,0,0,1},
                {1,1,0,1,1},
                {1,0,1,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1}
            };
            case 'N': return new byte[][]{
                {1,0,0,0,1},
                {1,1,0,0,1},
                {1,0,1,0,1},
                {1,0,0,1,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1}
            };
            case 'O': return new byte[][]{
                {0,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {0,1,1,1,0}
            };
            case 'P': return new byte[][]{
                {1,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,1,1,1,0},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,0,0,0,0}
            };
            case 'Q': return new byte[][]{
                {0,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,1,0,1},
                {1,0,0,1,0},
                {0,1,1,0,1}
            };
            case 'R': return new byte[][]{
                {1,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,1,1,1,0},
                {1,0,1,0,0},
                {1,0,0,1,0},
                {1,0,0,0,1}
            };
            case 'S': return new byte[][]{
                {0,1,1,1,1},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {0,1,1,1,0},
                {0,0,0,0,1},
                {0,0,0,0,1},
                {1,1,1,1,0}
            };
            case 'T': return new byte[][]{
                {1,1,1,1,1},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0}
            };
            case 'U': return new byte[][]{
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {0,1,1,1,0}
            };
            case 'V': return new byte[][]{
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {0,1,0,1,0},
                {0,0,1,0,0}
            };
            case 'W': return new byte[][]{
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {1,0,1,0,1},
                {1,0,1,0,1},
                {1,1,0,1,1},
                {1,0,0,0,1}
            };
            case 'X': return new byte[][]{
                {1,0,0,0,1},
                {1,0,0,0,1},
                {0,1,0,1,0},
                {0,0,1,0,0},
                {0,1,0,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1}
            };
            case 'Y': return new byte[][]{
                {1,0,0,0,1},
                {1,0,0,0,1},
                {0,1,0,1,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0}
            };
            case 'Z': return new byte[][]{
                {1,1,1,1,1},
                {0,0,0,0,1},
                {0,0,0,1,0},
                {0,0,1,0,0},
                {0,1,0,0,0},
                {1,0,0,0,0},
                {1,1,1,1,1}
            };
            case '0': return new byte[][]{
                {0,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,1,1},
                {1,0,1,0,1},
                {1,1,0,0,1},
                {1,0,0,0,1},
                {0,1,1,1,0}
            };
            case '1': return new byte[][]{
                {0,0,1,0,0},
                {0,1,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,1,1,1,0}
            };
            case '2': return new byte[][]{
                {0,1,1,1,0},
                {1,0,0,0,1},
                {0,0,0,0,1},
                {0,0,0,1,0},
                {0,0,1,0,0},
                {0,1,0,0,0},
                {1,1,1,1,1}
            };
            case '3': return new byte[][]{
                {1,1,1,1,0},
                {0,0,0,0,1},
                {0,0,0,0,1},
                {0,1,1,1,0},
                {0,0,0,0,1},
                {0,0,0,0,1},
                {1,1,1,1,0}
            };
            case '4': return new byte[][]{
                {0,0,0,1,0},
                {0,0,1,1,0},
                {0,1,0,1,0},
                {1,0,0,1,0},
                {1,1,1,1,1},
                {0,0,0,1,0},
                {0,0,0,1,0}
            };
            case '5': return new byte[][]{
                {1,1,1,1,1},
                {1,0,0,0,0},
                {1,1,1,1,0},
                {0,0,0,0,1},
                {0,0,0,0,1},
                {1,0,0,0,1},
                {0,1,1,1,0}
            };
            case '6': return new byte[][]{
                {0,1,1,1,0},
                {1,0,0,0,0},
                {1,0,0,0,0},
                {1,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {0,1,1,1,0}
            };
            case '7': return new byte[][]{
                {1,1,1,1,1},
                {0,0,0,0,1},
                {0,0,0,1,0},
                {0,0,1,0,0},
                {0,1,0,0,0},
                {0,1,0,0,0},
                {0,1,0,0,0}
            };
            case '8': return new byte[][]{
                {0,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {0,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {0,1,1,1,0}
            };
            case '9': return new byte[][]{
                {0,1,1,1,0},
                {1,0,0,0,1},
                {1,0,0,0,1},
                {0,1,1,1,1},
                {0,0,0,0,1},
                {0,0,0,0,1},
                {0,1,1,1,0}
            };
            case ' ': return new byte[][]{
                {0,0,0,0,0},
                {0,0,0,0,0},
                {0,0,0,0,0},
                {0,0,0,0,0},
                {0,0,0,0,0},
                {0,0,0,0,0},
                {0,0,0,0,0}
            };
            default: return new byte[][]{ // Default to '?'
                {0,1,1,1,0},
                {1,0,0,0,1},
                {0,0,0,1,0},
                {0,0,1,0,0},
                {0,0,1,0,0},
                {0,0,0,0,0},
                {0,0,1,0,0}
            };
        }
    }
    private void fillQuadrilateral(int[] x, int[] y, int fillColor) {
        // Find bounding box
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        
        for (int i = 0; i < 4; i++) {
            minX = Math.min(minX, x[i]);
            maxX = Math.max(maxX, x[i]);
            minY = Math.min(minY, y[i]);
            maxY = Math.max(maxY, y[i]);
        }
        
        // Clamp to screen bounds
        minX = Math.max(0, minX);
        maxX = Math.min(DrawingArea.width - 1, maxX);
        minY = Math.max(0, minY);
        maxY = Math.min(DrawingArea.height - 1, maxY);
        
        // Fill pixels inside the quadrilateral
        for (int py = minY; py <= maxY; py++) {
            for (int px = minX; px <= maxX; px++) {
                if (isPointInQuad(px, py, x, y)) {
                    int index = py * DrawingArea.width + px;
                    if (index >= 0 && index < DrawingArea.pixels.length) {
                        // Blend with existing pixel for transparency
                        int existingColor = DrawingArea.pixels[index];
                        DrawingArea.pixels[index] = blendColors(existingColor, fillColor);
                    }
                }
            }
        }
    }

    private boolean isPointInQuad(int px, int py, int[] x, int[] y) {
        int intersections = 0;
        
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            
            if (((y[i] > py) != (y[j] > py)) &&
                (px < (x[j] - x[i]) * (py - y[i]) / (y[j] - y[i]) + x[i])) {
                intersections++;
            }
        }
        
        return (intersections % 2) == 1;
    }

    private int blendColors(int background, int foreground) {
        int alpha = (foreground >> 24) & 0xFF;
        if (alpha == 0) return background;
        if (alpha == 255) return foreground;
        
        int invAlpha = 255 - alpha;
        int r = ((background >> 16) & 0xFF) * invAlpha + ((foreground >> 16) & 0xFF) * alpha >> 8;
        int g = ((background >> 8) & 0xFF) * invAlpha + ((foreground >> 8) & 0xFF) * alpha >> 8;
        int b = (background & 0xFF) * invAlpha + (foreground & 0xFF) * alpha >> 8;
        
        return (r << 16) | (g << 8) | b;
    }
    private void drawLine(int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        
        while (true) {
            if (x0 >= 0 && x0 < DrawingArea.width && y0 >= 0 && y0 < DrawingArea.height) {
                int index = y0 * DrawingArea.width + x0;
                if (index >= 0 && index < DrawingArea.pixels.length) {
                    DrawingArea.pixels[index] = color;
                }
            }
            
            if (x0 == x1 && y0 == y1) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }
    private void fillQuad(Point2D[] corners, int color) {
        // Find bounding box of the quad
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        
        for (Point2D corner : corners) {
            minX = Math.min(minX, (int)corner.x);
            maxX = Math.max(maxX, (int)corner.x);
            minY = Math.min(minY, (int)corner.y);
            maxY = Math.max(maxY, (int)corner.y);
        }
        
        // Clamp to screen bounds
        minX = Math.max(0, minX);
        maxX = Math.min(DrawingArea.width - 1, maxX);
        minY = Math.max(0, minY);
        maxY = Math.min(DrawingArea.height - 1, maxY);
        
        // For each pixel in bounding box, check if it's inside the quad
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (pointInQuad(x, y, corners)) {
                    int index = y * DrawingArea.width + x;
                    if (index >= 0 && index < DrawingArea.pixels.length) {
                        // Blend with existing pixel for transparency
                        int existingColor = DrawingArea.pixels[index];
                        DrawingArea.pixels[index] = blendColors(existingColor, color);
                    }
                }
            }
        }
    }

    private boolean pointInQuad(int px, int py, Point2D[] corners) {
        // Use ray casting algorithm to determine if point is inside quad
        int intersections = 0;
        
        for (int i = 0; i < 4; i++) {
            Point2D p1 = corners[i];
            Point2D p2 = corners[(i + 1) % 4];
            
            if (((p1.y > py) != (p2.y > py)) &&
                (px < (p2.x - p1.x) * (py - p1.y) / (p2.y - p1.y) + p1.x)) {
                intersections++;
            }
        }
        
        return (intersections % 2) == 1;
    }
    private Point2D projectToScreen(int worldX, int height, int worldY) {
        try {
            int relX = worldX - xCameraPos;
            int relY = height - zCameraPos;
            int relZ = worldY - yCameraPos;
            
            int rotX = relZ * camLeftRightY + relX * camLeftRightX >> 16;
            int rotZ = relZ * camLeftRightX - relX * camLeftRightY >> 16;
            int finalY = relY * camUpDownX - rotZ * camUpDownY >> 16;
            int finalZ = relY * camUpDownY + rotZ * camUpDownX >> 16;
            
            if (finalZ < 50) return null;
            
            int screenX = Rasterizer.textureInt1 + (rotX * WorldController.focalLength) / finalZ;
            int screenY = Rasterizer.textureInt2 + (finalY * WorldController.focalLength) / finalZ;
            
            return new Point2D(screenX, screenY);
        } catch (Exception e) {
            return null;
        }
    }

    private void drawLine(Point2D p1, Point2D p2, int color) {
        // Bresenham's line algorithm to draw line between two points
        int x0 = (int)p1.x, y0 = (int)p1.y;
        int x1 = (int)p2.x, y1 = (int)p2.y;
        
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        
        while (true) {
            if (x0 >= 0 && x0 < DrawingArea.width && y0 >= 0 && y0 < DrawingArea.height) {
                int index = y0 * DrawingArea.width + x0;
                if (index >= 0 && index < DrawingArea.pixels.length) {
                    DrawingArea.pixels[index] = color;
                }
            }
            
            if (x0 == x1 && y0 == y1) break;
            
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }
    public int[] getClickedTile(int screenX, int screenY) {
        try {
            // Store the original values
            boolean originalBoolean467 = aBoolean467;
            int originalInt468 = anInt468;
            int originalInt469 = anInt469;
            int originalInt470 = anInt470;
            int originalInt471 = anInt471;
            
            // Set up the click processing without the walking flag
            aBoolean467 = false; // Don't trigger walking
            anInt468 = screenX;
            anInt469 = screenY;
            anInt470 = -1;
            anInt471 = -1;
            
            // Force the coordinate calculation (this normally happens in the render loop)
            // You'll need to find where anInt470 and anInt471 get calculated
            // It's likely in the draw() method somewhere
            
            // For now, let's try calling the same logic as method312 but manually
            // This is a simplified version - you may need to adapt it
            int tileX = (screenX - Rasterizer.textureInt1) * 128 / WorldController.focalLength + xCameraPos / 128;
            int tileY = (screenY - Rasterizer.textureInt2) * 128 / WorldController.focalLength + yCameraPos / 128;
            
            // Restore original values
            aBoolean467 = originalBoolean467;
            anInt468 = originalInt468;
            anInt469 = originalInt469;
            anInt470 = originalInt470;
            anInt471 = originalInt471;
            
            // Convert to world tile coordinates
            int worldTileX = Client.baseX + tileX;
            int worldTileY = Client.baseY + tileY;
            
            return new int[]{worldTileX, worldTileY};
            
        } catch (Exception e) {
            return null;
        }
    }
    // Simple Point2D class
    private class Point2D {
        public double x, y;
        public Point2D(double x, double y) { this.x = x; this.y = y; }
    }

    private void renderGroundTriangle(int x1, int h1, int y1, int x2, int h2, int y2, int x3, int h3, int y3, int color) {
        try {
            ////System.out.println("DEBUG: renderGroundTriangle called with points: (" + x1 + "," + h1 + "," + y1 + ") (" + x2 + "," + h2 + "," + y2 + ") (" + x3 + "," + h3 + "," + y3 + ")");
            
            // Calculate the tile center from the three triangle points
            int tileWorldX = (x1 + x2 + x3) / 3;
            int tileWorldY = (y1 + y2 + y3) / 3;
            int tileHeight = (h1 + h2 + h3) / 3;
            
            //System.out.println("DEBUG: Tile center world coords: (" + tileWorldX + "," + tileHeight + "," + tileWorldY + ")");
            //System.out.println("DEBUG: Camera position: (" + xCameraPos + "," + zCameraPos + "," + yCameraPos + ")");

            // Transform to camera space using the same method as WorldController
            int relX = tileWorldX - xCameraPos;
            int relY = tileHeight - zCameraPos;
            int relZ = tileWorldY - yCameraPos;
            
            //System.out.println("DEBUG: Relative coords: (" + relX + "," + relY + "," + relZ + ")");

            // Apply camera rotation using exact same variables as method315
            int rotX = relZ * camLeftRightY + relX * camLeftRightX >> 16;
            int rotZ = relZ * camLeftRightX - relX * camLeftRightY >> 16;
            int finalY = relY * camUpDownX - rotZ * camUpDownY >> 16;
            int finalZ = relY * camUpDownY + rotZ * camUpDownX >> 16;
            
            //System.out.println("DEBUG: After rotation: rotX=" + rotX + " finalY=" + finalY + " finalZ=" + finalZ);

            // Check if behind camera (same as method315)
            if (finalZ < 50) {
                //System.out.println("DEBUG: Behind camera - finalZ=" + finalZ);
                return;
            }

            // Project to screen coordinates (same as method315)
            int screenX = Rasterizer.textureInt1 + (rotX * WorldController.focalLength) / finalZ;
            int screenY = Rasterizer.textureInt2 + (finalY * WorldController.focalLength) / finalZ;

            //System.out.println("DEBUG: Screen projection: (" + screenX + "," + screenY + ")");
            //System.out.println("DEBUG: Screen bounds check: X in 0-" + (DrawingArea.width - 20) + " Y in 0-" + (DrawingArea.height - 20));

            // Draw a visible marker if on screen
            if (screenX >= 0 && screenX < DrawingArea.width - 20 && 
                screenY >= 0 && screenY < DrawingArea.height - 20) {
                
                int markerColor = color | 0xFF000000; // Ensure full opacity
                //System.out.println("DEBUG: Drawing 20x20 marker at (" + screenX + "," + screenY + ") color=" + Integer.toHexString(markerColor));

                // Draw a 20x20 colored square for visibility
                for (int dy = 0; dy < 20; dy++) {
                    for (int dx = 0; dx < 20; dx++) {
                        int pixelX = screenX + dx;
                        int pixelY = screenY + dy;
                        
                        if (pixelX < DrawingArea.width && pixelY < DrawingArea.height) {
                            int pixelIndex = pixelY * DrawingArea.width + pixelX;
                            if (pixelIndex >= 0 && pixelIndex < DrawingArea.pixels.length) {
                                DrawingArea.pixels[pixelIndex] = markerColor;
                            }
                        }
                    }
                }
                //System.out.println("DEBUG: Successfully drew marker square");
            } else {
                //System.out.println("DEBUG: Marker off-screen - not drawing");
            }

        } catch (Exception e) {
            System.err.println("DEBUG: Exception in renderGroundTriangle: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void method314(Ground class30_sub3, boolean flag) {
        aClass19_477.insertHead(class30_sub3);
        
        do {
            Ground class30_sub3_1;
            do {
                class30_sub3_1 = (Ground) aClass19_477.popHead();
                if (class30_sub3_1 == null)
                    return;
            } while (!class30_sub3_1.aBoolean1323);
            
            int i = class30_sub3_1.anInt1308;
            int j = class30_sub3_1.anInt1309;
            int k = class30_sub3_1.anInt1307;
            int l = class30_sub3_1.anInt1310;
            
            // Validate tile coordinates
            if (i < 0 || i >= anInt438 || j < 0 || j >= anInt439 || k < 0 || k >= anInt437) {
                continue;
            }
            
            Ground aclass30_sub3[][] = groundArray[k];
            
            if (class30_sub3_1.aBoolean1322) {
                if (flag) {
                    // Check tiles above
                    if (k > 0 && k - 1 < anInt437) {
                        Ground class30_sub3_2 = groundArray[k - 1][i][j];
                        if (class30_sub3_2 != null && class30_sub3_2.aBoolean1323)
                            continue;
                    }
                    
                    // Check west tile
                    if (i <= Scene_cameraXTile && i > anInt449) {
                        if (i - 1 >= 0 && i - 1 < anInt438) {
                            Ground class30_sub3_3 = aclass30_sub3[i - 1][j];
                            if (class30_sub3_3 != null && class30_sub3_3.aBoolean1323
                                    && (class30_sub3_3.aBoolean1322 || (class30_sub3_1.anInt1320 & 1) == 0))
                                continue;
                        }
                    }
                    
                    // Check east tile
                    if (i >= Scene_cameraXTile && i < anInt450 - 1) {
                        if (i + 1 < anInt438) {
                            Ground class30_sub3_4 = aclass30_sub3[i + 1][j];
                            if (class30_sub3_4 != null && class30_sub3_4.aBoolean1323
                                    && (class30_sub3_4.aBoolean1322 || (class30_sub3_1.anInt1320 & 4) == 0))
                                continue;
                        }
                    }
                    
                    // Check north tile
                    if (j <= Scene_cameraYTile && j > anInt451) {
                        if (j - 1 >= 0 && j - 1 < anInt439) {
                            Ground class30_sub3_5 = aclass30_sub3[i][j - 1];
                            if (class30_sub3_5 != null && class30_sub3_5.aBoolean1323
                                    && (class30_sub3_5.aBoolean1322 || (class30_sub3_1.anInt1320 & 8) == 0))
                                continue;
                        }
                    }
                    
                    // Check south tile  
                    if (j >= Scene_cameraYTile && j < anInt452 - 1) {
                        if (j + 1 < anInt439) {
                            Ground class30_sub3_6 = aclass30_sub3[i][j + 1];
                            if (class30_sub3_6 != null && class30_sub3_6.aBoolean1323
                                    && (class30_sub3_6.aBoolean1322 || (class30_sub3_1.anInt1320 & 2) == 0))
                                continue;
                        }
                    }
                } else {
                    flag = true;
                }
                
                class30_sub3_1.aBoolean1322 = false;
                
                // Render underground tile if exists
                if (class30_sub3_1.aClass30_Sub3_1329 != null) {
                    Ground class30_sub3_7 = class30_sub3_1.aClass30_Sub3_1329;
                    if (class30_sub3_7.aClass43_1311 != null) {
                        if (!method320(0, i, j))
                            method315(class30_sub3_7.aClass43_1311, 0,
                                    camUpDownY, camUpDownX, camLeftRightY, camLeftRightX, i, j);
                    } else if (class30_sub3_7.aClass40_1312 != null && !method320(0, i, j))
                        method316(i, camUpDownY, camLeftRightY,
                                class30_sub3_7.aClass40_1312, camUpDownX, j, camLeftRightX);
                    
                    Object1 class10 = class30_sub3_7.obj1;
                    if (class10 != null)
                        class10.aClass30_Sub2_Sub4_278.renderAtPoint(0, camUpDownY,
                                camUpDownX, camLeftRightY, camLeftRightX,
                                class10.anInt274 - xCameraPos,
                                class10.anInt273 - zCameraPos,
                                class10.anInt275 - yCameraPos, class10.uid);
                    
                    for (int i2 = 0; i2 < class30_sub3_7.anInt1317; i2++) {
                        StaticObject class28 = class30_sub3_7.obj5Array[i2];
                        if (class28 != null)
                            class28.aClass30_Sub2_Sub4_521.renderAtPoint(
                                    class28.anInt522, camUpDownY, camUpDownX,
                                    camLeftRightY, camLeftRightX,
                                    class28.anInt519 - xCameraPos,
                                    class28.anInt518 - zCameraPos,
                                    class28.anInt520 - yCameraPos, class28.uid);
                    }
                }
                
                // Render current tile
                boolean flag1 = false;
                if (class30_sub3_1.aClass43_1311 != null) {
                    if (!method320(l, i, j)) {
                        flag1 = true;
                        method315(class30_sub3_1.aClass43_1311, l, camUpDownY,
                                camUpDownX, camLeftRightY, camLeftRightX, i, j);
                    }
                } else if (class30_sub3_1.aClass40_1312 != null && !method320(l, i, j)) {
                    flag1 = true;
                    method316(i, camUpDownY, camLeftRightY,
                            class30_sub3_1.aClass40_1312, camUpDownX, j, camLeftRightX);
                }
                
                // Wall and decoration rendering
                int j1 = 0;
                int j2 = 0;
                Object1 class10_3 = class30_sub3_1.obj1;
                Object2 class26_1 = class30_sub3_1.obj2;
                
                if (class10_3 != null || class26_1 != null) {
                    if (Scene_cameraXTile == i)
                        j1++;
                    else if (Scene_cameraXTile < i)
                        j1 += 2;
                    if (Scene_cameraYTile == j)
                        j1 += 3;
                    else if (Scene_cameraYTile > j)
                        j1 += 6;
                    j2 = anIntArray478[j1];
                    class30_sub3_1.anInt1328 = anIntArray480[j1];
                }
                
                if (class10_3 != null) {
                    if ((class10_3.orientation & anIntArray479[j1]) != 0) {
                        if (class10_3.orientation == 16) {
                            class30_sub3_1.anInt1325 = 3;
                            class30_sub3_1.anInt1326 = anIntArray481[j1];
                            class30_sub3_1.anInt1327 = 3 - class30_sub3_1.anInt1326;
                        } else if (class10_3.orientation == 32) {
                            class30_sub3_1.anInt1325 = 6;
                            class30_sub3_1.anInt1326 = anIntArray482[j1];
                            class30_sub3_1.anInt1327 = 6 - class30_sub3_1.anInt1326;
                        } else if (class10_3.orientation == 64) {
                            class30_sub3_1.anInt1325 = 12;
                            class30_sub3_1.anInt1326 = anIntArray483[j1];
                            class30_sub3_1.anInt1327 = 12 - class30_sub3_1.anInt1326;
                        } else {
                            class30_sub3_1.anInt1325 = 9;
                            class30_sub3_1.anInt1326 = anIntArray484[j1];
                            class30_sub3_1.anInt1327 = 9 - class30_sub3_1.anInt1326;
                        }
                    } else {
                        class30_sub3_1.anInt1325 = 0;
                    }
                    
                    if ((class10_3.orientation & j2) != 0
                            && !method321(l, i, j, class10_3.orientation))
                        class10_3.aClass30_Sub2_Sub4_278.renderAtPoint(0, camUpDownY,
                                camUpDownX, camLeftRightY, camLeftRightX,
                                class10_3.anInt274 - xCameraPos,
                                class10_3.anInt273 - zCameraPos,
                                class10_3.anInt275 - yCameraPos, class10_3.uid);
                    
                    if ((class10_3.orientation1 & j2) != 0
                            && !method321(l, i, j, class10_3.orientation1))
                        class10_3.aClass30_Sub2_Sub4_279.renderAtPoint(0, camUpDownY,
                                camUpDownX, camLeftRightY, camLeftRightX,
                                class10_3.anInt274 - xCameraPos,
                                class10_3.anInt273 - zCameraPos,
                                class10_3.anInt275 - yCameraPos, class10_3.uid);
                }
                
                if (class26_1 != null
                        && !method322(l, i, j, class26_1.aClass30_Sub2_Sub4_504.modelHeight))
                    if ((class26_1.anInt502 & j2) != 0)
                        class26_1.aClass30_Sub2_Sub4_504.renderAtPoint(
                                class26_1.anInt503, camUpDownY, camUpDownX,
                                camLeftRightY, camLeftRightX,
                                class26_1.anInt500 - xCameraPos,
                                class26_1.anInt499 - zCameraPos,
                                class26_1.anInt501 - yCameraPos, class26_1.uid);
                    else if ((class26_1.anInt502 & 0x300) != 0) {
                        int j4 = class26_1.anInt500 - xCameraPos;
                        int l5 = class26_1.anInt499 - zCameraPos;
                        int k6 = class26_1.anInt501 - yCameraPos;
                        int i8 = class26_1.anInt503;
                        int k9 = (i8 == 1 || i8 == 2) ? -j4 : j4;
                        int k10 = (i8 == 2 || i8 == 3) ? -k6 : k6;
                        
                        if ((class26_1.anInt502 & 0x100) != 0 && k10 < k9) {
                            int i11 = j4 + anIntArray463[i8];
                            int k11 = k6 + anIntArray464[i8];
                            class26_1.aClass30_Sub2_Sub4_504.renderAtPoint(
                                    i8 * 512 + 256, camUpDownY, camUpDownX,
                                    camLeftRightY, camLeftRightX, i11, l5, k11, class26_1.uid);
                        }
                        if ((class26_1.anInt502 & 0x200) != 0 && k10 > k9) {
                            int j11 = j4 + anIntArray465[i8];
                            int l11 = k6 + anIntArray466[i8];
                            class26_1.aClass30_Sub2_Sub4_504.renderAtPoint(
                                    i8 * 512 + 1280 & 0x7ff, camUpDownY,
                                    camUpDownX, camLeftRightY, camLeftRightX, j11, l5, l11, class26_1.uid);
                        }
                    }
                
                if (flag1) {
                    Object3 class49 = class30_sub3_1.obj3;
                    if (class49 != null)
                        class49.aClass30_Sub2_Sub4_814.renderAtPoint(0, camUpDownY,
                                camUpDownX, camLeftRightY, camLeftRightX,
                                class49.anInt812 - xCameraPos,
                                class49.anInt811 - zCameraPos,
                                class49.anInt813 - yCameraPos, class49.uid);
                    
                    Object4 object4_1 = class30_sub3_1.obj4;
                    if (object4_1 != null && object4_1.anInt52 == 0) {
                        if (object4_1.aClass30_Sub2_Sub4_49 != null)
                            object4_1.aClass30_Sub2_Sub4_49.renderAtPoint(0, camUpDownY, camUpDownX,
                                    camLeftRightY, camLeftRightX,
                                    object4_1.anInt46 - xCameraPos,
                                    object4_1.anInt45 - zCameraPos,
                                    object4_1.anInt47 - yCameraPos, object4_1.uid);
                        if (object4_1.aClass30_Sub2_Sub4_50 != null)
                            object4_1.aClass30_Sub2_Sub4_50.renderAtPoint(0, camUpDownY, camUpDownX,
                                    camLeftRightY, camLeftRightX,
                                    object4_1.anInt46 - xCameraPos,
                                    object4_1.anInt45 - zCameraPos,
                                    object4_1.anInt47 - yCameraPos, object4_1.uid);
                        if (object4_1.aClass30_Sub2_Sub4_48 != null)
                            object4_1.aClass30_Sub2_Sub4_48.renderAtPoint(0, camUpDownY, camUpDownX,
                                    camLeftRightY, camLeftRightX,
                                    object4_1.anInt46 - xCameraPos,
                                    object4_1.anInt45 - zCameraPos,
                                    object4_1.anInt47 - yCameraPos, object4_1.uid);
                    }
                }
                
                // Add adjacent tiles to render queue with bounds checking
                int k4 = class30_sub3_1.anInt1320;
                if (k4 != 0) {
                    if (i < Scene_cameraXTile && (k4 & 4) != 0) {
                        if (i + 1 < anInt438) {
                            Ground class30_sub3_17 = aclass30_sub3[i + 1][j];
                            if (class30_sub3_17 != null && class30_sub3_17.aBoolean1323)
                                aClass19_477.insertHead(class30_sub3_17);
                        }
                    }
                    if (j < Scene_cameraYTile && (k4 & 2) != 0) {
                        if (j + 1 < anInt439) {
                            Ground class30_sub3_18 = aclass30_sub3[i][j + 1];
                            if (class30_sub3_18 != null && class30_sub3_18.aBoolean1323)
                                aClass19_477.insertHead(class30_sub3_18);
                        }
                    }
                    if (i > Scene_cameraXTile && (k4 & 1) != 0) {
                        if (i - 1 >= 0) {
                            Ground class30_sub3_19 = aclass30_sub3[i - 1][j];
                            if (class30_sub3_19 != null && class30_sub3_19.aBoolean1323)
                                aClass19_477.insertHead(class30_sub3_19);
                        }
                    }
                    if (j > Scene_cameraYTile && (k4 & 8) != 0) {
                        if (j - 1 >= 0) {
                            Ground class30_sub3_20 = aclass30_sub3[i][j - 1];
                            if (class30_sub3_20 != null && class30_sub3_20.aBoolean1323)
                                aClass19_477.insertHead(class30_sub3_20);
                        }
                    }
                }
            }
            
            // Object occlusion culling
            if (class30_sub3_1.anInt1325 != 0) {
                boolean flag2 = true;
                for (int k1 = 0; k1 < class30_sub3_1.anInt1317; k1++) {
                    if (class30_sub3_1.obj5Array[k1].anInt528 == anInt448
                            || (class30_sub3_1.anIntArray1319[k1] & class30_sub3_1.anInt1325) != class30_sub3_1.anInt1326)
                        continue;
                    flag2 = false;
                    break;
                }
                
                if (flag2) {
                    Object1 class10_1 = class30_sub3_1.obj1;
                    if (!method321(l, i, j, class10_1.orientation))
                        class10_1.aClass30_Sub2_Sub4_278.renderAtPoint(0, camUpDownY,
                                camUpDownX, camLeftRightY, camLeftRightX,
                                class10_1.anInt274 - xCameraPos,
                                class10_1.anInt273 - zCameraPos,
                                class10_1.anInt275 - yCameraPos, class10_1.uid);
                    class30_sub3_1.anInt1325 = 0;
                }
            }
            
            // Static object rendering
            if (class30_sub3_1.aBoolean1324)
                try {
                    int i1 = class30_sub3_1.anInt1317;
                    class30_sub3_1.aBoolean1324 = false;
                    int l1 = 0;
                    
                    label0:
                    for (int k2 = 0; k2 < i1; k2++) {
                        StaticObject class28_1 = class30_sub3_1.obj5Array[k2];
                        if (class28_1.anInt528 == anInt448)
                            continue;
                        
                        for (int k3 = class28_1.anInt523; k3 <= class28_1.anInt524; k3++) {
                            for (int l4 = class28_1.anInt525; l4 <= class28_1.anInt526; l4++) {
                                // Bounds check for static objects
                                if (k3 >= 0 && k3 < anInt438 && l4 >= 0 && l4 < anInt439) {
                                    Ground class30_sub3_21 = aclass30_sub3[k3][l4];
                                    if (class30_sub3_21.aBoolean1322) {
                                        class30_sub3_1.aBoolean1324 = true;
                                    } else {
                                        if (class30_sub3_21.anInt1325 == 0)
                                            continue;
                                        int l6 = 0;
                                        if (k3 > class28_1.anInt523)
                                            l6++;
                                        if (k3 < class28_1.anInt524)
                                            l6 += 4;
                                        if (l4 > class28_1.anInt525)
                                            l6 += 8;
                                        if (l4 < class28_1.anInt526)
                                            l6 += 2;
                                        if ((l6 & class30_sub3_21.anInt1325) != class30_sub3_1.anInt1327)
                                            continue;
                                        class30_sub3_1.aBoolean1324 = true;
                                    }
                                    continue label0;
                                }
                            }
                        }
                        
                        aClass28Array462[l1++] = class28_1;
                        int i5 = Scene_cameraXTile - class28_1.anInt523;
                        int i6 = class28_1.anInt524 - Scene_cameraXTile;
                        if (i6 > i5)
                            i5 = i6;
                        int i7 = Scene_cameraYTile - class28_1.anInt525;
                        int j8 = class28_1.anInt526 - Scene_cameraYTile;
                        if (j8 > i7)
                            class28_1.anInt527 = i5 + j8;
                        else
                            class28_1.anInt527 = i5 + i7;
                    }
                    
                    while (l1 > 0) {
                        int i3 = -50;
                        int l3 = -1;
                        for (int j5 = 0; j5 < l1; j5++) {
                            StaticObject class28_2 = aClass28Array462[j5];
                            if (class28_2.anInt528 != anInt448)
                                if (class28_2.anInt527 > i3) {
                                    i3 = class28_2.anInt527;
                                    l3 = j5;
                                } else if (class28_2.anInt527 == i3) {
                                    int j7 = class28_2.anInt519 - xCameraPos;
                                    int k8 = class28_2.anInt520 - yCameraPos;
                                    int l9 = aClass28Array462[l3].anInt519 - xCameraPos;
                                    int l10 = aClass28Array462[l3].anInt520 - yCameraPos;
                                    if (j7 * j7 + k8 * k8 > l9 * l9 + l10 * l10)
                                        l3 = j5;
                                }
                        }
                        
                        if (l3 == -1)
                            break;
                        
                        StaticObject class28_3 = aClass28Array462[l3];
                        class28_3.anInt528 = anInt448;
                        if (!method323(l, class28_3.anInt523, class28_3.anInt524,
                                class28_3.anInt525, class28_3.anInt526,
                                class28_3.aClass30_Sub2_Sub4_521.modelHeight))
                            class28_3.aClass30_Sub2_Sub4_521.renderAtPoint(
                                    class28_3.anInt522, camUpDownY, camUpDownX,
                                    camLeftRightY, camLeftRightX,
                                    class28_3.anInt519 - xCameraPos,
                                    class28_3.anInt518 - zCameraPos,
                                    class28_3.anInt520 - yCameraPos, class28_3.uid);
                        
                        for (int k7 = class28_3.anInt523; k7 <= class28_3.anInt524; k7++) {
                            for (int l8 = class28_3.anInt525; l8 <= class28_3.anInt526; l8++) {
                                if (k7 >= 0 && k7 < anInt438 && l8 >= 0 && l8 < anInt439) {
                                    Ground class30_sub3_22 = aclass30_sub3[k7][l8];
                                    if (class30_sub3_22.anInt1325 != 0)
                                        aClass19_477.insertHead(class30_sub3_22);
                                    else if ((k7 != i || l8 != j) && class30_sub3_22.aBoolean1323)
                                        aClass19_477.insertHead(class30_sub3_22);
                                }
                            }
                        }
                    }
                    
                    if (class30_sub3_1.aBoolean1324)
                        continue;
                } catch (Exception _ex) {
                    class30_sub3_1.aBoolean1324 = false;
                }
            
            if (!class30_sub3_1.aBoolean1323 || class30_sub3_1.anInt1325 != 0)
                continue;
            
            // Check if we should stop rendering this tile
            if (i <= Scene_cameraXTile && i > anInt449 && i - 1 >= 0) {
                Ground class30_sub3_8 = aclass30_sub3[i - 1][j];
                if (class30_sub3_8 != null && class30_sub3_8.aBoolean1323)
                    continue;
            }
            if (i >= Scene_cameraXTile && i < anInt450 - 1 && i + 1 < anInt438) {
                Ground class30_sub3_9 = aclass30_sub3[i + 1][j];
                if (class30_sub3_9 != null && class30_sub3_9.aBoolean1323)
                    continue;
            }
            if (j <= Scene_cameraYTile && j > anInt451 && j - 1 >= 0) {
                Ground class30_sub3_10 = aclass30_sub3[i][j - 1];
                if (class30_sub3_10 != null && class30_sub3_10.aBoolean1323)
                    continue;
            }
            if (j >= Scene_cameraYTile && j < anInt452 - 1 && j + 1 < anInt439) {
                Ground class30_sub3_11 = aclass30_sub3[i][j + 1];
                if (class30_sub3_11 != null && class30_sub3_11.aBoolean1323)
                    continue;
            }
            
            class30_sub3_1.aBoolean1323 = false;
            anInt446--;
            
            // Render floor decorations
            Object4 object4 = class30_sub3_1.obj4;
            if (object4 != null && object4.anInt52 != 0) {
                if (object4.aClass30_Sub2_Sub4_49 != null)
                    object4.aClass30_Sub2_Sub4_49.renderAtPoint(0, camUpDownY,
                            camUpDownX, camLeftRightY, camLeftRightX,
                            object4.anInt46 - xCameraPos,
                            object4.anInt45 - zCameraPos - object4.anInt52,
                            object4.anInt47 - yCameraPos, object4.uid);
                if (object4.aClass30_Sub2_Sub4_50 != null)
                    object4.aClass30_Sub2_Sub4_50.renderAtPoint(0, camUpDownY,
                            camUpDownX, camLeftRightY, camLeftRightX,
                            object4.anInt46 - xCameraPos,
                            object4.anInt45 - zCameraPos - object4.anInt52,
                            object4.anInt47 - yCameraPos, object4.uid);
                if (object4.aClass30_Sub2_Sub4_48 != null)
                    object4.aClass30_Sub2_Sub4_48.renderAtPoint(0, camUpDownY,
                            camUpDownX, camLeftRightY, camLeftRightX,
                            object4.anInt46 - xCameraPos,
                            object4.anInt45 - zCameraPos - object4.anInt52,
                            object4.anInt47 - yCameraPos, object4.uid);
            }
            
            // Render wall decorations
            if (class30_sub3_1.anInt1328 != 0) {
                Object2 class26 = class30_sub3_1.obj2;
                if (class26 != null && !method322(l, i, j, class26.aClass30_Sub2_Sub4_504.modelHeight))
                    if ((class26.anInt502 & class30_sub3_1.anInt1328) != 0)
                        class26.aClass30_Sub2_Sub4_504.renderAtPoint(
                                class26.anInt503, camUpDownY, camUpDownX,
                                camLeftRightY, camLeftRightX,
                                class26.anInt500 - xCameraPos,
                                class26.anInt499 - zCameraPos,
                                class26.anInt501 - yCameraPos, class26.uid);
                    else if ((class26.anInt502 & 0x300) != 0) {
                        int l2 = class26.anInt500 - xCameraPos;
                        int j3 = class26.anInt499 - zCameraPos;
                        int i4 = class26.anInt501 - yCameraPos;
                        int k5 = class26.anInt503;
                        int j6 = (k5 == 1 || k5 == 2) ? -l2 : l2;
                        int l7 = (k5 == 2 || k5 == 3) ? -i4 : i4;
                        
                        if ((class26.anInt502 & 0x100) != 0 && l7 >= j6) {
                            int i9 = l2 + anIntArray463[k5];
                            int i10 = i4 + anIntArray464[k5];
                            class26.aClass30_Sub2_Sub4_504.renderAtPoint(
                                    k5 * 512 + 256, camUpDownY, camUpDownX,
                                    camLeftRightY, camLeftRightX, i9, j3, i10, class26.uid);
                        }
                        if ((class26.anInt502 & 0x200) != 0 && l7 <= j6) {
                            int j9 = l2 + anIntArray465[k5];
                            int j10 = i4 + anIntArray466[k5];
                            class26.aClass30_Sub2_Sub4_504.renderAtPoint(
                                    k5 * 512 + 1280 & 0x7ff, camUpDownY,
                                    camUpDownX, camLeftRightY, camLeftRightX, j9, j3, j10, class26.uid);
                        }
                    }
                
                Object1 class10_2 = class30_sub3_1.obj1;
                if (class10_2 != null) {
                    if ((class10_2.orientation1 & class30_sub3_1.anInt1328) != 0
                            && !method321(l, i, j, class10_2.orientation1))
                        class10_2.aClass30_Sub2_Sub4_279.renderAtPoint(0, camUpDownY,
                                camUpDownX, camLeftRightY, camLeftRightX,
                                class10_2.anInt274 - xCameraPos,
                                class10_2.anInt273 - zCameraPos,
                                class10_2.anInt275 - yCameraPos, class10_2.uid);
                    if ((class10_2.orientation & class30_sub3_1.anInt1328) != 0
                            && !method321(l, i, j, class10_2.orientation))
                        class10_2.aClass30_Sub2_Sub4_278.renderAtPoint(0, camUpDownY,
                                camUpDownX, camLeftRightY, camLeftRightX,
                                class10_2.anInt274 - xCameraPos,
                                class10_2.anInt273 - zCameraPos,
                                class10_2.anInt275 - yCameraPos, class10_2.uid);
                }
            }
            
            // Queue tiles below current height
            if (k < anInt437 - 1 && k + 1 < anInt437) {
                Ground class30_sub3_12 = groundArray[k + 1][i][j];
                if (class30_sub3_12 != null && class30_sub3_12.aBoolean1323)
                    aClass19_477.insertHead(class30_sub3_12);
            }
            
            // Queue adjacent tiles with bounds checking
            if (i < Scene_cameraXTile && i + 1 < anInt438) {
                Ground class30_sub3_13 = aclass30_sub3[i + 1][j];
                if (class30_sub3_13 != null && class30_sub3_13.aBoolean1323)
                    aClass19_477.insertHead(class30_sub3_13);
            }
            if (j < Scene_cameraYTile && j + 1 < anInt439) {
                Ground class30_sub3_14 = aclass30_sub3[i][j + 1];
                if (class30_sub3_14 != null && class30_sub3_14.aBoolean1323)
                    aClass19_477.insertHead(class30_sub3_14);
            }
            if (i > Scene_cameraXTile && i - 1 >= 0) {
                Ground class30_sub3_15 = aclass30_sub3[i - 1][j];
                if (class30_sub3_15 != null && class30_sub3_15.aBoolean1323)
                    aClass19_477.insertHead(class30_sub3_15);
            }
            if (j > Scene_cameraYTile && j - 1 >= 0) {
                Ground class30_sub3_16 = aclass30_sub3[i][j - 1];
                if (class30_sub3_16 != null && class30_sub3_16.aBoolean1323)
                    aClass19_477.insertHead(class30_sub3_16);
            }
        } while (true);
    }

    public static boolean boolean666 = false; //hdtextures

    private void method315(Class43 class43, int i, int j, int k, int l, int i1,
                           int j1, int k1) {
        if (j1 > 100 || k1 > 100) {
            //System.out.println("Large tile coords: j1=" + j1 + " k1=" + k1);
        }
        int l1;
        int i2 = l1 = (j1 << 7) - xCameraPos;
        int j2;
        int k2 = j2 = (k1 << 7) - yCameraPos;
        int l2;
        int i3 = l2 = i2 + 128;
        int j3;
        int k3 = j3 = k2 + 128;
        int l3 = tileHeights[i][j1][k1] - zCameraPos;
        int i4 = tileHeights[i][j1 + 1][k1] - zCameraPos;
        int j4 = tileHeights[i][j1 + 1][k1 + 1] - zCameraPos;
        int k4 = tileHeights[i][j1][k1 + 1] - zCameraPos;
        int l4 = k2 * l + i2 * i1 >> 16;
        k2 = k2 * i1 - i2 * l >> 16;
        i2 = l4;
        l4 = l3 * k - k2 * j >> 16;
        k2 = l3 * j + k2 * k >> 16;
        l3 = l4;
        if (k2 < 50)
            return;
        l4 = j2 * l + i3 * i1 >> 16;
        j2 = j2 * i1 - i3 * l >> 16;
        i3 = l4;
        l4 = i4 * k - j2 * j >> 16;
        j2 = i4 * j + j2 * k >> 16;
        i4 = l4;
        if (j2 < 50)
            return;
        l4 = k3 * l + l2 * i1 >> 16;
        k3 = k3 * i1 - l2 * l >> 16;
        l2 = l4;
        l4 = j4 * k - k3 * j >> 16;
        k3 = j4 * j + k3 * k >> 16;
        j4 = l4;
        if (k3 < 50)
            return;
        l4 = j3 * l + l1 * i1 >> 16;
        j3 = j3 * i1 - l1 * l >> 16;
        l1 = l4;
        l4 = k4 * k - j3 * j >> 16;
        j3 = k4 * j + j3 * k >> 16;
        k4 = l4;
        if (j3 < 50)
            return;
        int i5 = Rasterizer.textureInt1 + (i2 * WorldController.focalLength)
                / k2;
        int j5 = Rasterizer.textureInt2 + (l3 * WorldController.focalLength)
                / k2;
        int k5 = Rasterizer.textureInt1 + (i3 * WorldController.focalLength)
                / j2;
        int l5 = Rasterizer.textureInt2 + (i4 * WorldController.focalLength)
                / j2;
        int i6 = Rasterizer.textureInt1 + (l2 * WorldController.focalLength)
                / k3;
        int j6 = Rasterizer.textureInt2 + (j4 * WorldController.focalLength)
                / k3;
        int k6 = Rasterizer.textureInt1 + (l1 * WorldController.focalLength)
                / j3;
        int l6 = Rasterizer.textureInt2 + (k4 * WorldController.focalLength)
                / j3;

        Rasterizer.alpha = 0;
        if ((i6 - k6) * (l5 - l6) - (j6 - l6) * (k5 - k6) > 0) {
            Rasterizer.textureOutOfDrawingBounds = i6 < 0 || k6 < 0 || k5 < 0
                    || i6 > DrawingArea.lastX || k6 > DrawingArea.lastX
                    || k5 > DrawingArea.lastX;
            if (aBoolean467
                    && method318(anInt468, anInt469, j6, l6, l5, i6, k6, k5)) {
                anInt470 = j1;
                anInt471 = k1;
            }
            if (class43.anInt720 == -1 || class43.anInt720 > 50) {
                if (class43.anInt718 != 0xbc614e) {
                    if (boolean666
                            && class43.anInt720 != -1) {
                        if (class43.aBoolean721) {
                            Rasterizer.drawMaterializedTriangle(j6, l6, l5, i6,
                                    k6, k5, class43.anInt718, class43.anInt719,
                                    class43.anInt717, i2, i3, l1, l3, i4, k4,
                                    k2, j2, j3, class43.anInt720, k3, j3, j2);
                        } else {
                            Rasterizer.drawMaterializedTriangle(j6, l6, l5, i6,
                                    k6, k5, class43.anInt718, class43.anInt719,
                                    class43.anInt717, l2, l1, i3, j4, k4, i4,
                                    k3, j3, j2, class43.anInt720, k3, j3, j2);
                        }
                    } else {
                        Rasterizer.drawGouraudTriangle(j6, l6, l5, i6, k6, k5,
                                class43.anInt718, class43.anInt719,
                                class43.anInt717, k3, j3, j2);
                    }
                }
            } else if (!lowMem) {
                if (class43.aBoolean721) {
                    Rasterizer.drawTexturedTriangleold(j6, l6, l5, i6, k6, k5,
                            class43.anInt718, class43.anInt719,
                            class43.anInt717, i2, i3, l1, l3, i4, k4, k2, j2,
                            j3, class43.anInt720, k3, j3, j2);
                } else {
                    Rasterizer.drawTexturedTriangleold(j6, l6, l5, i6, k6, k5,
                            class43.anInt718, class43.anInt719,
                            class43.anInt717, l2, l1, i3, j4, k4, i4, k3, j3,
                            j2, class43.anInt720, k3, j3, j2);
                }
            } else {
                int i7 = anIntArray485[class43.anInt720];
                Rasterizer.drawGouraudTriangle(j6, l6, l5, i6, k6, k5,
                        method317(i7, class43.anInt718),
                        method317(i7, class43.anInt719),
                        method317(i7, class43.anInt717), k3, j3, j2);
            }
        }
        if ((i5 - k5) * (l6 - l5) - (j5 - l5) * (k6 - k5) > 0) {
            Rasterizer.textureOutOfDrawingBounds = i5 < 0 || k5 < 0 || k6 < 0
                    || i5 > DrawingArea.lastX || k5 > DrawingArea.lastX
                    || k6 > DrawingArea.lastX;
            if (aBoolean467
                    && method318(anInt468, anInt469, j5, l5, l6, i5, k5, k6)) {
                anInt470 = j1;
                anInt471 = k1;
            }
            if (class43.anInt720 == -1 || class43.anInt720 > 50) {
                if (class43.anInt716 != 0xbc614e) {
                    if (boolean666
                            && class43.anInt720 != -1) {
                        Rasterizer.drawMaterializedTriangle(j5, l5, l6, i5, k5,
                                k6, class43.anInt716, class43.anInt717,
                                class43.anInt719, i2, i3, l1, l3, i4, k4, k2,
                                j2, j3, class43.anInt720, k2, j2, j3);
                    } else {
                        Rasterizer.drawGouraudTriangle(j5, l5, l6, i5, k5, k6,
                                class43.anInt716, class43.anInt717,
                                class43.anInt719, k2, j2, j3);
                    }
                }
            } else {
                if (!lowMem) {
                    Rasterizer.drawTexturedTriangleold(j5, l5, l6, i5, k5, k6,
                            class43.anInt716, class43.anInt717,
                            class43.anInt719, i2, i3, l1, l3, i4, k4, k2, j2,
                            j3, class43.anInt720, k2, j2, j3);
                    return;
                }
                int j7 = anIntArray485[class43.anInt720];
                Rasterizer.drawGouraudTriangle(j5, l5, l6, i5, k5, k6,
                        method317(j7, class43.anInt716),
                        method317(j7, class43.anInt717),
                        method317(j7, class43.anInt719), k2, j2, j3);
            }
        }
    }

    private void method316(int i, int j, int k, ShapedTile shapedTile, int l,
                           int i1, int j1) {
        int k1 = shapedTile.origVertexX.length;
        for (int l1 = 0; l1 < k1; l1++) {
            int i2 = shapedTile.origVertexX[l1] - xCameraPos;
            int k2 = shapedTile.origVertexY[l1] - zCameraPos;
            int i3 = shapedTile.origVertexZ[l1] - yCameraPos;
            int k3 = i3 * k + i2 * j1 >> 16;
            i3 = i3 * j1 - i2 * k >> 16;
            i2 = k3;
            k3 = k2 * l - i3 * j >> 16;
            i3 = k2 * j + i3 * l >> 16;
            k2 = k3;
            if (i3 < 50)
                return;
            if (shapedTile.triangleTexture != null) {
                shapedTile.viewSpaceX[l1] = i2;
                shapedTile.viewSpaceY[l1] = k2;
                shapedTile.viewSpaceZ[l1] = i3;
            }
            shapedTile.screenX[l1] = Rasterizer.textureInt1
                    + (i2 * WorldController.focalLength) / i3;
            shapedTile.screenY[l1] = Rasterizer.textureInt2
                    + (k2 * WorldController.focalLength) / i3;
            shapedTile.viewSpaceZ[l1] = i3;

            shapedTile.viewSpaceZ[l1] = i3;
        }

        Rasterizer.alpha = 0;
        k1 = shapedTile.triangleA.length;
        for (int j2 = 0; j2 < k1; j2++) {
            int l2 = shapedTile.triangleA[j2];
            int j3 = shapedTile.triangleB[j2];
            int l3 = shapedTile.triangleC[j2];
            int i4 = shapedTile.screenX[l2];
            int j4 = shapedTile.screenX[j3];
            int k4 = shapedTile.screenX[l3];
            int l4 = shapedTile.screenY[l2];
            int i5 = shapedTile.screenY[j3];
            int j5 = shapedTile.screenY[l3];
            if ((i4 - j4) * (j5 - i5) - (l4 - i5) * (k4 - j4) > 0) {
                Rasterizer.textureOutOfDrawingBounds = i4 < 0 || j4 < 0 || k4 < 0
                        || i4 > DrawingArea.lastX || j4 > DrawingArea.lastX
                        || k4 > DrawingArea.lastX;
                if (aBoolean467
                        && method318(anInt468, anInt469, l4, i5, j5, i4, j4, k4)) {
                    anInt470 = i;
                    anInt471 = i1;
                }
                if (shapedTile.triangleTexture == null
                        || shapedTile.triangleTexture[j2] == -1
                        || shapedTile.triangleTexture[j2] > 50) {
                    if (shapedTile.triangleHslA[j2] != 0xbc614e) {
                        if (shapedTile.triangleTexture != null
                                && shapedTile.triangleTexture[j2] != -1) {
                            if (shapedTile.flat
                                    || shapedTile.triangleTexture[j2] == 505) {
                                Rasterizer.drawMaterializedTriangle(l4, i5, j5,
                                        i4, j4, k4,
                                        shapedTile.triangleHslA[j2],
                                        shapedTile.triangleHslB[j2],
                                        shapedTile.triangleHslC[j2],
                                        shapedTile.viewSpaceX[0],
                                        shapedTile.viewSpaceX[1],
                                        shapedTile.viewSpaceX[3],
                                        shapedTile.viewSpaceY[0],
                                        shapedTile.viewSpaceY[1],
                                        shapedTile.viewSpaceY[3],
                                        shapedTile.viewSpaceZ[0],
                                        shapedTile.viewSpaceZ[1],
                                        shapedTile.viewSpaceZ[3],
                                        shapedTile.triangleTexture[j2],
                                        shapedTile.viewSpaceZ[l2],
                                        shapedTile.viewSpaceZ[j3],
                                        shapedTile.viewSpaceZ[l3]);
                            } else {
                                Rasterizer.drawMaterializedTriangle(l4, i5, j5,
                                        i4, j4, k4,
                                        shapedTile.triangleHslA[j2],
                                        shapedTile.triangleHslB[j2],
                                        shapedTile.triangleHslC[j2],
                                        shapedTile.viewSpaceX[l2],
                                        shapedTile.viewSpaceX[j3],
                                        shapedTile.viewSpaceX[l3],
                                        shapedTile.viewSpaceY[l2],
                                        shapedTile.viewSpaceY[j3],
                                        shapedTile.viewSpaceY[l3],
                                        shapedTile.viewSpaceZ[l2],
                                        shapedTile.viewSpaceZ[j3],
                                        shapedTile.viewSpaceZ[l3],
                                        shapedTile.triangleTexture[j2],
                                        shapedTile.viewSpaceZ[l2],
                                        shapedTile.viewSpaceZ[j3],
                                        shapedTile.viewSpaceZ[l3]);
                            }
                        } else {
                            Rasterizer.drawGouraudTriangle(l4, i5, j5, i4, j4,
                                    k4, shapedTile.triangleHslA[j2],
                                    shapedTile.triangleHslB[j2],
                                    shapedTile.triangleHslC[j2],
                                    shapedTile.viewSpaceZ[l2],
                                    shapedTile.viewSpaceZ[j3],
                                    shapedTile.viewSpaceZ[l3]);
                        }
                    }
                } else if (!lowMem) {
                    if (shapedTile.flat) {
                        Rasterizer.drawTexturedTriangleold(l4, i5, j5, i4, j4, k4,
                                shapedTile.triangleHslA[j2],
                                shapedTile.triangleHslB[j2],
                                shapedTile.triangleHslC[j2],
                                shapedTile.viewSpaceX[0],
                                shapedTile.viewSpaceX[1],
                                shapedTile.viewSpaceX[3],
                                shapedTile.viewSpaceY[0],
                                shapedTile.viewSpaceY[1],
                                shapedTile.viewSpaceY[3],
                                shapedTile.viewSpaceZ[0],
                                shapedTile.viewSpaceZ[1],
                                shapedTile.viewSpaceZ[3],
                                shapedTile.triangleTexture[j2],
                                shapedTile.viewSpaceZ[l2],
                                shapedTile.viewSpaceZ[j3],
                                shapedTile.viewSpaceZ[l3]);
                    } else {
                        Rasterizer.drawTexturedTriangleold(l4, i5, j5, i4, j4, k4,
                                shapedTile.triangleHslA[j2],
                                shapedTile.triangleHslB[j2],
                                shapedTile.triangleHslC[j2],
                                shapedTile.viewSpaceX[l2],
                                shapedTile.viewSpaceX[j3],
                                shapedTile.viewSpaceX[l3],
                                shapedTile.viewSpaceY[l2],
                                shapedTile.viewSpaceY[j3],
                                shapedTile.viewSpaceY[l3],
                                shapedTile.viewSpaceZ[l2],
                                shapedTile.viewSpaceZ[j3],
                                shapedTile.viewSpaceZ[l3],
                                shapedTile.triangleTexture[j2],
                                shapedTile.viewSpaceZ[l2],
                                shapedTile.viewSpaceZ[j3],
                                shapedTile.viewSpaceZ[l3]);
                    }
                } else {
                    int k5 = anIntArray485[shapedTile.triangleTexture[j2]];
                    Rasterizer.drawGouraudTriangle(l4, i5, j5, i4, j4, k4,
                            method317(k5, shapedTile.triangleHslA[j2]),
                            method317(k5, shapedTile.triangleHslB[j2]),
                            method317(k5, shapedTile.triangleHslC[j2]),
                            shapedTile.viewSpaceZ[l2],
                            shapedTile.viewSpaceZ[j3],
                            shapedTile.viewSpaceZ[l3]);
                }
            }
        }
    }

    private int method317(int j, int k) {
        k = 127 - k;
        k = (k * (j & 0x7f)) / 160;
        if (k < 2)
            k = 2;
        else if (k > 126)
            k = 126;
        return (j & 0xff80) + k;
    }

    private boolean method318(int i, int j, int k, int l, int i1, int j1,
                              int k1, int l1) {
        if (j < k && j < l && j < i1)
            return false;
        if (j > k && j > l && j > i1)
            return false;
        if (i < j1 && i < k1 && i < l1)
            return false;
        if (i > j1 && i > k1 && i > l1)
            return false;
        int i2 = (j - k) * (k1 - j1) - (i - j1) * (l - k);
        int j2 = (j - i1) * (j1 - l1) - (i - l1) * (k - i1);
        int k2 = (j - l) * (l1 - k1) - (i - k1) * (i1 - l);
        return i2 * k2 > 0 && k2 * j2 > 0;
    }

    private void method319() {
        int j = anIntArray473[anInt447];
        Class47 aclass47[] = aClass47ArrayArray474[anInt447];
        Scene_currentOccludersCount = 0;

        for (int k = 0; k < j; k++) {
            Class47 var4 = aclass47[k];
            if (var4.type == 1) {
                int l = (var4.minTileX - Scene_cameraXTile) + farZ;
                if (l < 0 || l > 50)
                    continue;

                int k1 = (var4.anInt789 - Scene_cameraYTile) + farZ;
                if (k1 < 0)
                    k1 = 0;

                int j2 = (var4.anInt790 - Scene_cameraYTile) + farZ;
                if (j2 > 50)
                    j2 = 50;

                boolean flag = false;

                while (k1 <= j2) {
                    if (aBooleanArrayArray492[l][k1++]) {
                        flag = true;
                        break;
                    }
                }

                if (!flag)
                    continue;

                int j3 = xCameraPos - var4.anInt792;
                if (j3 > 32) {
                    var4.anInt798 = 1;
                } else {
                    if (j3 >= -32)
                        continue;
                    var4.anInt798 = 2;
                    j3 = -j3;
                }
                var4.anInt801 = (var4.anInt794 - yCameraPos << 8) / j3;
                var4.anInt802 = (var4.anInt795 - yCameraPos << 8) / j3;
                var4.anInt803 = (var4.anInt796 - zCameraPos << 8) / j3;
                var4.anInt804 = (var4.anInt797 - zCameraPos << 8) / j3;
                aClass47Array476[Scene_currentOccludersCount++] = var4;
                continue;
            }
            if (var4.type == 2) {
                int i1 = (var4.anInt789 - Scene_cameraYTile) + farZ;
                if (i1 < 0 || i1 > 50)
                    continue;
                int l1 = (var4.minTileX - Scene_cameraXTile) + farZ;
                if (l1 < 0)
                    l1 = 0;
                int k2 = (var4.anInt788 - Scene_cameraXTile) + farZ;
                if (k2 > 50)
                    k2 = 50;
                boolean flag1 = false;
                while (l1 <= k2)
                    if (aBooleanArrayArray492[l1++][i1]) {
                        flag1 = true;
                        break;
                    }
                if (!flag1)
                    continue;
                int k3 = yCameraPos - var4.anInt794;
                if (k3 > 32) {
                    var4.anInt798 = 3;
                } else {
                    if (k3 >= -32)
                        continue;
                    var4.anInt798 = 4;
                    k3 = -k3;
                }
                var4.anInt799 = (var4.anInt792 - xCameraPos << 8) / k3;
                var4.anInt800 = (var4.anInt793 - xCameraPos << 8) / k3;
                var4.anInt803 = (var4.anInt796 - zCameraPos << 8) / k3;
                var4.anInt804 = (var4.anInt797 - zCameraPos << 8) / k3;
                aClass47Array476[Scene_currentOccludersCount++] = var4;
            } else if (var4.type == 4) {
                int j1 = var4.anInt796 - zCameraPos;
                if (j1 > 128) {
                    int i2 = (var4.anInt789 - Scene_cameraYTile) + farZ;
                    if (i2 < 0)
                        i2 = 0;
                    int l2 = (var4.anInt790 - Scene_cameraYTile) + farZ;
                    if (l2 > 50)
                        l2 = 50;
                    if (i2 <= l2) {
                        int i3 = (var4.minTileX - Scene_cameraXTile) + farZ;
                        if (i3 < 0)
                            i3 = 0;
                        int l3 = (var4.anInt788 - Scene_cameraXTile) + farZ;
                        if (l3 > 50)
                            l3 = 50;
                        boolean flag2 = false;
                        label0:
                        for (int i4 = i3; i4 <= l3; i4++) {
                            for (int j4 = i2; j4 <= l2; j4++) {
                                if (!aBooleanArrayArray492[i4][j4])
                                    continue;
                                flag2 = true;
                                break label0;
                            }

                        }

                        if (flag2) {
                            var4.anInt798 = 5;
                            var4.anInt799 = (var4.anInt792 - xCameraPos << 8)
                                    / j1;
                            var4.anInt800 = (var4.anInt793 - xCameraPos << 8)
                                    / j1;
                            var4.anInt801 = (var4.anInt794 - yCameraPos << 8)
                                    / j1;
                            var4.anInt802 = (var4.anInt795 - yCameraPos << 8)
                                    / j1;
                            aClass47Array476[Scene_currentOccludersCount++] = var4;
                        }
                    }
                }
            }
        }

    }

    private boolean method320(int i, int j, int k) {
        int l = anIntArrayArrayArray445[i][j][k];
        if (l == -anInt448)
            return false;
        if (l == anInt448)
            return true;
        int i1 = j << 7;
        int j1 = k << 7;
        if (method324(i1 + 1, tileHeights[i][j][k], j1 + 1)
                && method324((i1 + 128) - 1,
                tileHeights[i][j + 1][k], j1 + 1)
                && method324((i1 + 128) - 1,
                tileHeights[i][j + 1][k + 1],
                (j1 + 128) - 1)
                && method324(i1 + 1, tileHeights[i][j][k + 1],
                (j1 + 128) - 1)) {
            anIntArrayArrayArray445[i][j][k] = anInt448;
            return true;
        } else {
            anIntArrayArrayArray445[i][j][k] = -anInt448;
            return false;
        }
    }

    private boolean method321(int i, int j, int k, int l) {
        if (!method320(i, j, k))
            return false;
        int i1 = j << 7;
        int j1 = k << 7;
        int k1 = tileHeights[i][j][k] - 1;
        int l1 = k1 - 120;
        int i2 = k1 - 230;
        int j2 = k1 - 238;
        if (l < 16) {
            if (l == 1) {
                if (i1 > xCameraPos) {
                    if (!method324(i1, k1, j1))
                        return false;
                    if (!method324(i1, k1, j1 + 128))
                        return false;
                }
                if (i > 0) {
                    if (!method324(i1, l1, j1))
                        return false;
                    if (!method324(i1, l1, j1 + 128))
                        return false;
                }
                return method324(i1, i2, j1) && method324(i1, i2, j1 + 128);
            }
            if (l == 2) {
                if (j1 < yCameraPos) {
                    if (!method324(i1, k1, j1 + 128))
                        return false;
                    if (!method324(i1 + 128, k1, j1 + 128))
                        return false;
                }
                if (i > 0) {
                    if (!method324(i1, l1, j1 + 128))
                        return false;
                    if (!method324(i1 + 128, l1, j1 + 128))
                        return false;
                }
                return method324(i1, i2, j1 + 128)
                        && method324(i1 + 128, i2, j1 + 128);
            }
            if (l == 4) {
                if (i1 < xCameraPos) {
                    if (!method324(i1 + 128, k1, j1))
                        return false;
                    if (!method324(i1 + 128, k1, j1 + 128))
                        return false;
                }
                if (i > 0) {
                    if (!method324(i1 + 128, l1, j1))
                        return false;
                    if (!method324(i1 + 128, l1, j1 + 128))
                        return false;
                }
                return method324(i1 + 128, i2, j1)
                        && method324(i1 + 128, i2, j1 + 128);
            }
            if (l == 8) {
                if (j1 > yCameraPos) {
                    if (!method324(i1, k1, j1))
                        return false;
                    if (!method324(i1 + 128, k1, j1))
                        return false;
                }
                if (i > 0) {
                    if (!method324(i1, l1, j1))
                        return false;
                    if (!method324(i1 + 128, l1, j1))
                        return false;
                }
                return method324(i1, i2, j1) && method324(i1 + 128, i2, j1);
            }
        }
        if (!method324(i1 + 64, j2, j1 + 64))
            return false;
        if (l == 16)
            return method324(i1, i2, j1 + 128);
        if (l == 32)
            return method324(i1 + 128, i2, j1 + 128);
        if (l == 64)
            return method324(i1 + 128, i2, j1);
        if (l == 128) {
            return method324(i1, i2, j1);
        } else {
            //System.out.println("Warning unsupported wall type");
            return true;
        }
    }

    private boolean method322(int i, int j, int k, int l) {
        if (!method320(i, j, k))
            return false;
        int i1 = j << 7;
        int j1 = k << 7;
        return method324(i1 + 1, tileHeights[i][j][k] - l, j1 + 1)
                && method324((i1 + 128) - 1,
                tileHeights[i][j + 1][k] - l, j1 + 1)
                && method324((i1 + 128) - 1,
                tileHeights[i][j + 1][k + 1] - l,
                (j1 + 128) - 1)
                && method324(i1 + 1, tileHeights[i][j][k + 1] - l,
                (j1 + 128) - 1);
    }

    private boolean method323(int i, int j, int k, int l, int i1, int j1) {
        if (j == k && l == i1) {
            if (!method320(i, j, l))
                return false;
            int k1 = j << 7;
            int i2 = l << 7;
            return method324(k1 + 1, tileHeights[i][j][l] - j1,
                    i2 + 1)
                    && method324((k1 + 128) - 1,
                    tileHeights[i][j + 1][l] - j1, i2 + 1)
                    && method324((k1 + 128) - 1,
                    tileHeights[i][j + 1][l + 1] - j1,
                    (i2 + 128) - 1)
                    && method324(k1 + 1, tileHeights[i][j][l + 1]
                    - j1, (i2 + 128) - 1);
        }
        for (int l1 = j; l1 <= k; l1++) {
            for (int j2 = l; j2 <= i1; j2++)
                if (anIntArrayArrayArray445[i][l1][j2] == -anInt448)
                    return false;

        }

        int k2 = (j << 7) + 1;
        int l2 = (l << 7) + 2;
        int i3 = tileHeights[i][j][l] - j1;
        if (!method324(k2, i3, l2))
            return false;
        int j3 = (k << 7) - 1;
        if (!method324(j3, i3, l2))
            return false;
        int k3 = (i1 << 7) - 1;
        return method324(k2, i3, k3) && method324(j3, i3, k3);
    }

    private boolean method324(int i, int j, int k) {
        for (int l = 0; l < Scene_currentOccludersCount; l++) {
            Class47 class47 = aClass47Array476[l];
            if (class47.anInt798 == 1) {
                int i1 = class47.anInt792 - i;
                if (i1 > 0) {
                    int j2 = class47.anInt794 + (class47.anInt801 * i1 >> 8);
                    int k3 = class47.anInt795 + (class47.anInt802 * i1 >> 8);
                    int l4 = class47.anInt796 + (class47.anInt803 * i1 >> 8);
                    int i6 = class47.anInt797 + (class47.anInt804 * i1 >> 8);
                    if (k >= j2 && k <= k3 && j >= l4 && j <= i6)
                        return true;
                }
            } else if (class47.anInt798 == 2) {
                int j1 = i - class47.anInt792;
                if (j1 > 0) {
                    int k2 = class47.anInt794 + (class47.anInt801 * j1 >> 8);
                    int l3 = class47.anInt795 + (class47.anInt802 * j1 >> 8);
                    int i5 = class47.anInt796 + (class47.anInt803 * j1 >> 8);
                    int j6 = class47.anInt797 + (class47.anInt804 * j1 >> 8);
                    if (k >= k2 && k <= l3 && j >= i5 && j <= j6)
                        return true;
                }
            } else if (class47.anInt798 == 3) {
                int k1 = class47.anInt794 - k;
                if (k1 > 0) {
                    int l2 = class47.anInt792 + (class47.anInt799 * k1 >> 8);
                    int i4 = class47.anInt793 + (class47.anInt800 * k1 >> 8);
                    int j5 = class47.anInt796 + (class47.anInt803 * k1 >> 8);
                    int k6 = class47.anInt797 + (class47.anInt804 * k1 >> 8);
                    if (i >= l2 && i <= i4 && j >= j5 && j <= k6)
                        return true;
                }
            } else if (class47.anInt798 == 4) {
                int l1 = k - class47.anInt794;
                if (l1 > 0) {
                    int i3 = class47.anInt792 + (class47.anInt799 * l1 >> 8);
                    int j4 = class47.anInt793 + (class47.anInt800 * l1 >> 8);
                    int k5 = class47.anInt796 + (class47.anInt803 * l1 >> 8);
                    int l6 = class47.anInt797 + (class47.anInt804 * l1 >> 8);
                    if (i >= i3 && i <= j4 && j >= k5 && j <= l6)
                        return true;
                }
            } else if (class47.anInt798 == 5) {
                int i2 = j - class47.anInt796;
                if (i2 > 0) {
                    int j3 = class47.anInt792 + (class47.anInt799 * i2 >> 8);
                    int k4 = class47.anInt793 + (class47.anInt800 * i2 >> 8);
                    int l5 = class47.anInt794 + (class47.anInt801 * i2 >> 8);
                    int i7 = class47.anInt795 + (class47.anInt802 * i2 >> 8);
                    if (i >= j3 && i <= k4 && k >= l5 && k <= i7)
                        return true;
                }
            }
        }

        return false;
    }
    public static void shutdownSceneThreads() {
        scenePool.shutdown();
        try {
            if (!scenePool.awaitTermination(1, TimeUnit.SECONDS)) {
                scenePool.shutdownNow();
            }
        } catch (InterruptedException e) {
            scenePool.shutdownNow();
        }
    }
    private boolean aBoolean434;
    public static boolean lowMem = true;
    private final int anInt437;
    private final int anInt438;
    private final int anInt439;
    private final int[][][] tileHeights;
    private final Ground[][][] groundArray;
    private int anInt442;
    private int obj5CacheCurrPos;
    private final StaticObject[] obj5Cache;
    private final int[][][] anIntArrayArrayArray445;
    private static int anInt446;
    private static int anInt447;
    private static int anInt448;
    private static int anInt449;
    private static int anInt450;
    private static int anInt451;
    private static int anInt452;
    private static int Scene_cameraXTile;
    private static int Scene_cameraYTile;
    public static int xCameraPos;
    public static int zCameraPos;
    public static int yCameraPos;
    public static int camUpDownY;
    public static int camUpDownX;
    public static int camLeftRightY;
    public static int camLeftRightX;
    private static StaticObject[] aClass28Array462 = new StaticObject[100];
    private static final int[] anIntArray463 = {53, -53, -53, 53};
    private static final int[] anIntArray464 = {-53, -53, 53, 53};
    private static final int[] anIntArray465 = {-45, 45, 45, -45};
    private static final int[] anIntArray466 = {45, 45, -45, -45};
    private static boolean aBoolean467;
    private static int anInt468;
    private static int anInt469;
    public static int anInt470 = -1;
    public static int anInt471 = -1;
    private static final int anInt472;
    private static int[] anIntArray473;
    private static Class47[][] aClass47ArrayArray474;
    private static int Scene_currentOccludersCount;
    private static final Class47[] aClass47Array476 = new Class47[500];
    private static NodeList aClass19_477 = new NodeList();
    private static final int[] anIntArray478 = {19, 55, 38, 155, 255, 110,
            137, 205, 76};
    private static final int[] anIntArray479 = {160, 192, 80, 96, 0, 144, 80,
            48, 160};
    private static final int[] anIntArray480 = {76, 8, 137, 4, 0, 1, 38, 2, 19};
    private static final int[] anIntArray481 = {0, 0, 2, 0, 0, 2, 1, 1, 0};
    private static final int[] anIntArray482 = {2, 0, 0, 2, 0, 0, 0, 4, 4};
    private static final int[] anIntArray483 = {0, 4, 4, 8, 0, 0, 8, 0, 0};
    private static final int[] anIntArray484 = {1, 1, 0, 0, 0, 8, 0, 0, 8};
    private static final int[] anIntArray485 = {41, 39248, 41, 4643, 41, 41,
            41, 41, 41, 41, 41, 41, 41, 41, 41, 43086, 41, 41, 41, 41, 41, 41,
            41, 8602, 41, 28992, 41, 41, 41, 41, 41, 5056, 41, 41, 41, 7079,
            41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 3131, 41, 41, 41};
    private final int[] anIntArray486;
    private final int[] anIntArray487;
    private int anInt488;
    private final int[][] anIntArrayArray489 = {new int[16],
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 1, 1},
            {1, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
            {0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1},
            {0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0},
            {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0},
            {1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, 1},
            {1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1}};
    private final int[][] anIntArrayArray490 = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15},
            {12, 8, 4, 0, 13, 9, 5, 1, 14, 10, 6, 2, 15, 11, 7, 3},
            {15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0},
            {3, 7, 11, 15, 2, 6, 10, 14, 1, 5, 9, 13, 0, 4, 8, 12}};
    private static boolean[][][][] visibilityMap;
    private static boolean[][] aBooleanArrayArray492;
    private static int anInt493;
    private static int anInt494;
    private static int anInt495;
    private static int anInt496;
    private static int anInt497;
    private static int anInt498;
    public static int focalLength = 9;
    public static int viewDistance = 9;

    static {
        focalLength = 512;
        anInt472 = 4;
        anIntArray473 = new int[anInt472];
        aClass47ArrayArray474 = new Class47[anInt472][500];
    }
}