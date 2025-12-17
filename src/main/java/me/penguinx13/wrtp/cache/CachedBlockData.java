package me.penguinx13.wrtp.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CachedBlockData {
    public final int worldId;
    public final int x, z;
    public final int highestY;
    public final int biomeId;
    public final int blockId;

    private static final Map<String, Integer> BIOME_TO_ID = new ConcurrentHashMap<>();
    private static final Map<Integer, String> ID_TO_BIOME = new ConcurrentHashMap<>();

    private static final Map<String, Integer> BLOCK_TO_ID = new ConcurrentHashMap<>();
    private static final Map<Integer, String> ID_TO_BLOCK = new ConcurrentHashMap<>();

    private static final Map<String, Integer> WORLD_TO_ID = new ConcurrentHashMap<>();
    private static final Map<Integer, String> ID_TO_WORLD = new ConcurrentHashMap<>();

    private static int nextBiomeId = 0;
    private static int nextBlockId = 0;
    private static int nextWorldId = 0;

    public CachedBlockData(int worldId, int x, int z, int highestY, int biomeId, int blockId) {
        this.worldId = worldId;
        this.x = x;
        this.z = z;
        this.highestY = highestY;
        this.biomeId = biomeId;
        this.blockId = blockId;
    }

    public CachedBlockData(String world, int x, int z, int highestY, String biomeName, String blockName) {
        this.worldId = getOrCreateWorldId(world);
        this.x = x;
        this.z = z;
        this.highestY = highestY;
        this.biomeId = getOrCreateBiomeId(biomeName);
        this.blockId = getOrCreateBlockId(blockName);
    }

    public static int getOrCreateWorldId(String name) {
        return WORLD_TO_ID.computeIfAbsent(name, n -> {
            int id = nextWorldId++;
            ID_TO_WORLD.put(id, n);
            return id;
        });
    }

    public static int getOrCreateBiomeId(String name) {
        return BIOME_TO_ID.computeIfAbsent(name, n -> {
            int id = nextBiomeId++;
            ID_TO_BIOME.put(id, n);
            return id;
        });
    }

    public static int getOrCreateBlockId(String name) {
        return BLOCK_TO_ID.computeIfAbsent(name, n -> {
            int id = nextBlockId++;
            ID_TO_BLOCK.put(id, n);
            return id;
        });
    }

    public static String getBiomeName(int id) {
        return ID_TO_BIOME.getOrDefault(id, "UNKNOWN");
    }

    public static String getBlockName(int id) {
        return ID_TO_BLOCK.getOrDefault(id, "UNKNOWN");
    }

    public static String getWorldName(int id) {
        return ID_TO_WORLD.getOrDefault(id, "UNKNOWN");
    }

    @Override
    public String toString() {
        return "CachedBlockData{" +
                "world=" + getWorldName(worldId) +
                ", x=" + x +
                ", z=" + z +
                ", Y=" + highestY +
                ", biome=" + getBiomeName(biomeId) +
                ", block=" + getBlockName(blockId) +
                '}';
    }
}
