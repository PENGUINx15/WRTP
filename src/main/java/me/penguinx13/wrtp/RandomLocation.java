package me.penguinx13.wrtp;

import me.penguinx13.wapi.Managers.ConfigManager;
import org.bukkit.*;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Random;

public class RandomLocation {
    private static ConfigManager config;

    public RandomLocation(ConfigManager config) {
        this.config = config;
    }

    public static Location generateLocation(World world, int min, int max, String channel, boolean checkBiome) {
        return generateSquare(world, min, max);

    }

    private static Location generateSquare(World world, int min, int max) {
        max = max - min;
        int x, z;
        int quadrant = new Random().nextInt(4);
        try {
            z = switch (quadrant) {
                case 0 -> {
                    x = new Random().nextInt(max) + min;
                    yield new Random().nextInt(max) + min; // Positive X and Z
                }
                case 1 -> {
                    x = -new Random().nextInt(max) - min;
                    yield -(new Random().nextInt(max) + min); // Negative X and Z
                }
                case 2 -> {
                    x = -new Random().nextInt(max) - min;
                    yield new Random().nextInt(max) + min; // Negative X and Positive Z
                }
                default -> {
                    x = new Random().nextInt(max) + min;
                    yield -(new Random().nextInt(max) + min); // Positive X and Negative Z
                }
            };
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            Bukkit.getLogger().warning("A bounding location was negative! Please check your config only has positive x/z for max/min radius!");
            Bukkit.getLogger().warning("Max: " + max + " Min: " + min);
            return null;
        }
        return new Location(world, x, 69, z);
    }


    public static Location getSafeLocation( World world, Location loc, int minY, int maxY, List<String> biomes) {
        return getSafeLocation(loc.getBlockX(), loc.getBlockZ(), minY, maxY, world, biomes);
    }
    private static Location getSafeLocation(int x, int z, int minY, int maxY, World world, List<String> biomes) {
        Block b = getHighestBlock(x, z, world);
        if (!b.getType().isSolid()) {
            if (!badBlock(b.getType().name(), x, z, world, null)) {
                b = world.getBlockAt(x, b.getY() - 1, z);
            }
        }
        if (    b.getY() >= minY
                && b.getY() <= maxY
                && !badBlock(b.getType().name(), x, z, world, biomes)) {
            return new Location(world, x, b.getY() + 1, z);
        }
        return null;
    }

    public static Block getHighestBlock(int x, int z, World world) {
        Block b = world.getHighestBlockAt(x, z);
        if (b.getType().toString().endsWith("AIR")) //1.15.1 or less
            b = world.getBlockAt(x, b.getY() - 1, z);
        return b;
    }

    public static boolean badBlock(String block, int x, int z, World world, List<String> biomes) {
        for (String currentBlock : config.getConfig("config.yml").getStringList("blacklistedBlocks"))
            if (currentBlock.equalsIgnoreCase(block))
                return true;
        if (biomes == null || biomes.isEmpty())
            return false;
        String biomeCurrent = world.getBiome(x, z).name();
        for (String biome : biomes)
            if (biomeCurrent.toUpperCase().contains(biome.toUpperCase()))
                return false;
        return true;
    }
}