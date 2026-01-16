package me.penguinx13.wrtp;

import me.penguinx13.wapi.Managers.ConfigManager;
import me.penguinx13.wapi.Managers.MessageManager;
import me.penguinx13.wrtp.cache.CachedBlockData;
import me.penguinx13.wrtp.cache.SQLiteManager;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TeleportEvent implements Listener {

    private final ConfigManager config;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final int cooldown;
    private final SQLiteManager db;

    public TeleportEvent() {
        this.db = WRTP.getDatabase();
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

        int maxRange = config.getConfig("config.yml").getInt("channels." + channel + ".maxRange");
        int minRange = config.getConfig("config.yml").getInt("channels." + channel + ".minRange");

        Location safeLocation = findSafeLocation(world, maxRange, minRange, channel, false);
        if (safeLocation == null) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noSafeLocation"));
            return;
        }

        teleportAndNotify(player, safeLocation, channel, null);
    }

    // ------------------- NEARBY TYPE -------------------

    private void nearbyType(Player player, String channel) {
        World world = player.getWorld();
        int minRange = config.getConfig("config.yml").getInt("channels." + channel + ".minRange");
        int maxRange = config.getConfig("config.yml").getInt("channels." + channel + ".maxRange");
        int minOnline = config.getConfig("config.yml").getInt("channels." + channel + ".minOnline");

        if (world.getPlayers().size() < minOnline) {
            MessageManager.sendMessage(player,
                    config.getConfig("config.yml").getString("messages.noMinPlayers"));
            return;
        }
        Player target = findRandomNearbyPlayer(player);
        if (target == null) {
            MessageManager.sendMessage(player,
                    config.getConfig("config.yml").getString("messages.noPlayerInWorld"));
            return;
        }
        Location loc = findSafeLocationNearPlayer(
                target,
                minRange,
                maxRange
        );
        if (loc == null) {
            MessageManager.sendMessage(player,
                    config.getConfig("config.yml").getString("messages.noSafeLocation"));
            return;
        }

        teleportAndNotify(player, loc, channel, target.getName());
    }

    // ------------------- BIOME TYPE -------------------
    private void biomeType(Player player, String channel) {
        World world = getWorldFromConfig(channel);
        if (world == null) return;

        int maxRange = config.getConfig("config.yml").getInt("channels." + channel + ".maxRange");
        int minRange = config.getConfig("config.yml").getInt("channels." + channel + ".minRange");

        Location safeLocation = findSafeLocation(world, maxRange, minRange, channel, true);
        if (safeLocation == null) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noSafeLocation"));
            return;
        }

        Biome biome = safeLocation.getBlock().getBiome();
        teleportAndNotify(player, safeLocation, channel, biome.name());
    }

    private Location findSafeLocation(World world, int maxRange, int minRange, String channel, boolean checkBiome) {
        int worldId = CachedBlockData.getOrCreateWorldId(world.getName());
        List<String> biomes = null;
        if (checkBiome) {
            biomes = config.getConfig("config.yml")
                    .getStringList("channels." + channel + ".biomes");
        }
        for (int i = 0; i < 100; i++) {
            CachedBlockData data = db.getRandomPoint(
                    worldId,
                    minRange,
                    maxRange,
                    biomes
            );
            if (data == null) continue;
            return new Location(
                    world,
                    data.x + 0.5,
                    data.highestY + 1,
                    data.z + 0.5
            );
        }
        return null;
    }
    private Location findSafeLocationNearPlayer(
            Player center,
            int minRange,
            int maxRange
    ) {
        World world = center.getWorld();
        Location base = center.getLocation();

        for (int i = 0; i < 100; i++) {
            double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
            int radius = ThreadLocalRandom.current().nextInt(minRange, maxRange + 1);

            int x = base.getBlockX() + (int) (Math.cos(angle) * radius);
            int z = base.getBlockZ() + (int) (Math.sin(angle) * radius);

            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;

            int y = world.getHighestBlockYAt(x, z);
            Block ground = world.getBlockAt(x, y - 1, z);

            if (ground.isSolid()) continue;

            return new Location(world, x + 0.5, y, z + 0.5);
        }

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

    private Player findRandomNearbyPlayer(Player player) {
        List<Player> candidates = new ArrayList<>();

        for (Player p : player.getWorld().getPlayers()) {
            if (p.equals(player)) continue;
            candidates.add(p);
        }

        if (candidates.isEmpty()) return null;

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
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
