package com.client.sound;

import com.client.Client;
import com.client.OnDemandData;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class SoundRequestQueue {

    private static final Queue<SoundRequest> queuedSounds = new LinkedList<>();

    public static void add(SoundRequest request) {
        if (request == null) return;

        if (SoundCache.isReady(request.id)) {
            //System.out.println("⚡ Instant play from cache: ID=" + request.id);
            new SoundPlayer(SoundCache.get(request.id), request.volume, request.delay);
            return;
        }

        queuedSounds.add(request);
        //System.out.println("🔁 Queued sound request: ID=" + request.id + " volume=" + request.volume + " delay=" + request.delay);
        Client.instance.onDemandFetcher.provide(6, request.id); // 
    }

    /**
     * This should be called ONLY when `OnDemandData.dataType == 5`
     */
    public static void process(OnDemandData data) {
    	if (data == null || data.dataType != 6) return;

        Iterator<SoundRequest> it = queuedSounds.iterator();
        while (it.hasNext()) {
            SoundRequest req = it.next();
            if (req.id == data.ID) {
                SoundCache.store(req.id, data.buffer); // store in RAM
                //System.out.println("🔊 Playing sound ID=" + req.id + " instantly from loaded buffer ");
                new SoundPlayer(data.buffer, req.volume, req.delay);
                it.remove();
                break;
            }
        }
    }
}
