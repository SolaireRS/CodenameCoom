package com.client.sound;

import java.util.HashMap;
import java.util.Map;
import com.client.Client;

public class SoundCache {
    private static final Map<Integer, byte[]> cache = new HashMap<>();

    public static void preload(int soundId) {
        if (!cache.containsKey(soundId)) {
            Client.instance.onDemandFetcher.provide(6, soundId); // fetch in advance
        }
    }

    public static void store(int soundId, byte[] data) {
        cache.put(soundId, data);
    }

    public static byte[] get(int soundId) {
        return cache.get(soundId);
    }

    public static boolean isReady(int soundId) {
        return cache.containsKey(soundId);
    }
}
