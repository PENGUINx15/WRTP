package me.penguinx13.wrtp;

import me.penguinx13.wapi.Managers.ConfigManager;
import me.penguinx13.wapi.Managers.MessageManager;
import me.penguinx13.wrtp.cache.CachedBlockData;
import me.penguinx13.wrtp.cache.SQLiteManager;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.*;

public class TeleportEvent implements Listener {

    private final ConfigManager config;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final int cooldown;
    private final SQLiteManager db;

    public TeleportEvent() {
        this.db = WRTP.getInstance().getDatabase();
        this.config = new ConfigManager(Bukkit.getPluginManager().getPlugin("WRTP"));
        this.cooldown = config.getConfig("config.yml").getInt("cooldown");
    }

    public void teleportPlayer(Player player, String channel) {
        teleportPlayer(player, channel, false);
    }

    public void teleportPlayer(Player player, String channel, boolean ignoreCooldown) {
        String type = config.getConfig("config.yml").getString("channels." + channel + ".type");

        if (Boolean.TRUE.equals(ignoreCooldown) || isCooldownExpired(player)) {
            switch (Objects.requireNonNull(type).toUpperCase()) {
                case "DEFAULT" -> defaultType(player, channel);
                case "NEARBY_PLAYERS" -> nearbyType(player, channel);
                case "BIOME" -> biomeType(player, channel);
                default -> MessageManager.sendMessage(player,
                        Objects.requireNonNull(config.getConfig("config.yml").getString("messages.unknownType"))
                                .replace("{type}", type));
            }
        } else {
            MessageManager.sendMessage(player,
                    Objects.requireNonNull(config.getConfig("config.yml").getString("messages.cooldown"))
                            .replace("{time}", String.valueOf(getRemainingCooldown(player))));
        }
    }

    // ------------------- DEFAULT TYPE -------------------

    private void defaultType(Player player, String channel) {
        World world = getWorldFromConfig(channel);
        if (world == null) return;

        int minX = config.getConfig("config.yml").getInt("channels." + channel + ".range.minX");
        int maxX = config.getConfig("config.yml").getInt("channels." + channel + ".range.maxX");
        int minZ = config.getConfig("config.yml").getInt("channels." + channel + ".range.minZ");
        int maxZ = config.getConfig("config.yml").getInt("channels." + channel + ".range.maxZ");

        Location safeLocation = findSafeLocation(world, minX, maxX, minZ, maxZ, channel, false);
        if (safeLocation == null) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noSafeLocation"));
            return;
        }

        teleportAndNotify(player, safeLocation, channel, null);
    }

    // ------------------- NEARBY TYPE -------------------

    private void nearbyType(Player player, String channel) {
        int minRange = config.getConfig("config.yml").getInt("channels." + channel + ".nearbyRange.min");
        int maxRange = config.getConfig("config.yml").getInt("channels." + channel + ".nearbyRange.max");
        int minOnline = config.getConfig("config.yml").getInt("channels." + channel + ".minOnline");

        if (Bukkit.getOnlinePlayers().size() < minOnline) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noMinPlayers"));
            return;
        }

        Player target = findRandomNearbyPlayer(player);
        if (target == null) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noPlayerInWorld"));
            return;
        }

        double angle = Math.toRadians(new Random().nextDouble() * 360);
        int range = getRandom(minRange, maxRange);
        int x = (int) (target.getLocation().getX() + range * Math.cos(angle));
        int z = (int) (target.getLocation().getZ() + range * Math.sin(angle));

        Location safeLocation = findSafeLocation(player.getWorld(), x, x, z, z, channel, false);
        if (safeLocation == null) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noSafeLocation"));
            return;
        }

        teleportAndNotify(player, safeLocation, channel, target.getName());
    }

    // ------------------- BIOME TYPE -------------------

    private void biomeType(Player player, String channel) {
        World world = getWorldFromConfig(channel);
        if (world == null) return;

        int minX = config.getConfig("config.yml").getInt("channels." + channel + ".range.minX");
        int maxX = config.getConfig("config.yml").getInt("channels." + channel + ".range.maxX");
        int minZ = config.getConfig("config.yml").getInt("channels." + channel + ".range.minZ");
        int maxZ = config.getConfig("config.yml").getInt("channels." + channel + ".range.maxZ");

        Location safeLocation = findSafeLocation(world, minX, maxX, minZ, maxZ, channel, true);
        if (safeLocation == null) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noSafeLocation"));
            return;
        }

        Biome biome = safeLocation.getBlock().getBiome();
        teleportAndNotify(player, safeLocation, channel, biome.name());
    }

    // ------------------- SAFE LOCATION SEARCH -------------------

    private Location findSafeLocation(World world, int minX, int maxX, int minZ, int maxZ, String channel, boolean checkBiome) {
        int worldId = CachedBlockData.getOrCreateWorldId(world.getName());

        for (int i = 0; i < 100; i++) {
            CachedBlockData data = db.getRandomPointInRange(worldId, minX, maxX, minZ, maxZ);
            if (data == null) continue;
            if (checkBiome && !isBiome(Biome.valueOf(data.biomeName), channel)) continue;
            if (!isSafeBlock(data.blockName)) continue;

            Location loc = new Location(world, data.x + 0.5, data.highestY + 1, data.z + 0.5);
            Bukkit.getLogger().info("✅ Найдено безопасное место: " + loc);
            return loc;
        }

        Bukkit.getLogger().warning("⚠ Не удалось найти безопасное место после 100 попыток!");
        return null;
    }


    // ------------------- HELPERS -------------------

    private void teleportAndNotify(Player player, Location loc, String channel, String extra) {
        setCooldown(player);
        player.setFallDistance(0.0f);
        player.teleport(loc);

        String msg = config.getConfig("config.yml").getString("channels." + channel + ".message");
        if (msg == null) return;

        msg = msg.replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ()))
                .replace("{world}", loc.getWorld().getName());

        if (extra != null)
            msg = msg.replace("{targetPlayer}", extra).replace("{biome}", extra);

        MessageManager.sendMessage(player, msg);
    }

    private boolean isSafeBlock(String blockName) {
        Material mat = Material.getMaterial(blockName);
        return mat != null && mat.isSolid() && !mat.isAir()
                && mat != Material.LAVA && mat != Material.WATER;
    }

    private boolean isBiome(Biome biome, String channel) {
        List<String> biomes = config.getConfig("config.yml").getStringList("channels." + channel + ".biomes");
        return biomes.stream().map(String::toUpperCase).toList().contains(biome.name());
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() / 1000);
    }

    private boolean isCooldownExpired(Player player) {
        long start = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        return (System.currentTimeMillis() / 1000 - start) >= cooldown;
    }

    private int getRemainingCooldown(Player player) {
        long start = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        return Math.max(0, cooldown - (int) (System.currentTimeMillis() / 1000 - start));
    }

    private int getRandom(int min, int max) {
        return min + new Random().nextInt(max - min + 1);
    }

    private Player findRandomNearbyPlayer(Player player) {
        List<Player> players = new ArrayList<>(player.getWorld().getPlayers());
        return players.isEmpty() ? null : players.get(new Random().nextInt(players.size()));
    }

    private World getWorldFromConfig(String channel) {
        String worldName = config.getConfig("config.yml").getString("channels." + channel + ".world");
        assert worldName != null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Bukkit.getLogger().warning("[WRTP] World not found in config: " + worldName);
        }
        return world;
    }
}
