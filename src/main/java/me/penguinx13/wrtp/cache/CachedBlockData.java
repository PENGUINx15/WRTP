package me.penguinx13.wrtp.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CachedBlockData {

    private static final AtomicInteger WORLD_SEQ = new AtomicInteger(0);
    private static final ConcurrentHashMap<String, Integer> WORLD_IDS = new ConcurrentHashMap<>();

    public static int getOrCreateWorldId(String worldName) {
        return WORLD_IDS.computeIfAbsent(worldName, k -> WORLD_SEQ.incrementAndGet());
    }

    public final int worldId;
    public final int x;
    public final int z;
    public final int highestY;
    public final String biomeName;
    public final String blockName;

    public CachedBlockData(String world, int x, int z, int highestY, String biomeName, String blockName) {
        this.worldId = getOrCreateWorldId(world);
        this.x = x;
        this.z = z;
        this.highestY = highestY;
        this.biomeName = biomeName;
        this.blockName = blockName;
    }
    public CachedBlockData(int worldId, int x, int z, int highestY, String biomeName, String blockName) {
        this.worldId = worldId;
        this.x = x;
        this.z = z;
        this.highestY = highestY;
        this.biomeName = biomeName;
        this.blockName = blockName;
    }
}
