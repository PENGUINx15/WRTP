package me.penguinx13.wrtp;

import me.penguinx13.wapi.Managers.ConfigManager;
import me.penguinx13.wapi.Managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.*;

public class TeleportEvent implements Listener {

    private final ConfigManager config;
    private final Map<UUID, Long> cooldowns;
    private final int cooldown;

    public TeleportEvent() {
        this.config = new ConfigManager(Bukkit.getPluginManager().getPlugin("WRTP"));
        this.cooldown = config.getConfig("config.yml").getInt("cooldown");
        this.cooldowns = new HashMap<>();
    }

    public void teleportPlayer(Player player, String channel) {
        teleportPlayer(player, channel, false);
    }

    public void teleportPlayer(Player player, String channel, Boolean ignoreCooldown) {
        String type = config.getConfig("config.yml").getString("channels." + channel + ".type");

        if (Boolean.TRUE.equals(ignoreCooldown) || isCooldownExpired(player)) {
            switch (Objects.requireNonNull(type).toUpperCase()) {
                case "DEFAULT":
                    defaultType(player, channel);
                    break;
                case "NEARBY_PLAYERS":
                    nearbyType(player, channel);
                    break;
                case "BIOME":
                    biomeType(player, channel);
                    break;
                default:
                    MessageManager.sendMessage(player, Objects.requireNonNull(config.getConfig("config.yml").getString("messages.unknownType")).replace("{type}", type));
            }
        } else {
            MessageManager.sendMessage(player, Objects.requireNonNull(config.getConfig("config.yml").getString("messages.cooldown")).replace("{time}", String.valueOf(getRemainingCooldown(player))));
        }
    }

    private void defaultType(Player player, String channel) {
        int minX = config.getConfig("config.yml").getInt("channels." + channel + ".range.minX");
        int maxX = config.getConfig("config.yml").getInt("channels." + channel + ".range.maxX");
        int minZ = config.getConfig("config.yml").getInt("channels." + channel + ".range.minZ");
        int maxZ = config.getConfig("config.yml").getInt("channels." + channel + ".range.maxZ");
        World world = Bukkit.getWorld(Objects.requireNonNull(config.getConfig("config.yml").getString("channels." + channel + ".world")));

        assert world != null;
        Location safeLocation = findSafeLocation(world, minX, maxX, minZ, maxZ, channel, false);
        if (safeLocation == null) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noSafeLocation"));
            return;
        }

        setCooldown(player);
        player.setFallDistance(0.0f);
        player.teleport(safeLocation);

        MessageManager.sendMessage(player,
                Objects.requireNonNull(config.getConfig("config.yml").getString("channels." + channel + ".message"))
                        .replace("{x}", String.valueOf(safeLocation.getBlockX()))
                        .replace("{y}", String.valueOf(safeLocation.getBlockY()))
                        .replace("{z}", String.valueOf(safeLocation.getBlockZ()))
                        .replace("{world}", world.getName()));
    }

    private void nearbyType(Player player, String channel) {
        int minRange = config.getConfig("config.yml").getInt("channels." + channel + ".nearbyRange.min");
        int maxRange = config.getConfig("config.yml").getInt("channels." + channel + ".nearbyRange.max");
        int minOnline = config.getConfig("config.yml").getInt("channels." + channel + ".minOnline");

        if (Bukkit.getOnlinePlayers().size() < minOnline) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noMinPlayers"));
            return;
        }

        Player targetPlayer = findRandomNearbyPlayer(player);
        if (targetPlayer == null) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noPlayerInWorld"));
            return;
        }

        double angle = Math.toRadians(new Random().nextDouble() * 360);
        int range = getRandom(minRange, maxRange);

        int x = (int) (targetPlayer.getLocation().getX() + range * Math.cos(angle));
        int z = (int) (targetPlayer.getLocation().getZ() + range * Math.sin(angle));

        Location safeLocation = findSafeLocation(player.getWorld(), x, x, z, z, channel, false);
        if (safeLocation == null) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noSafeLocation"));
            return;
        }

        setCooldown(player);
        player.setFallDistance(0.0f);
        player.teleport(safeLocation);

        MessageManager.sendMessage(player,
                Objects.requireNonNull(config.getConfig("config.yml").getString("channels." + channel + ".message"))
                        .replace("{x}", String.valueOf(safeLocation.getBlockX()))
                        .replace("{y}", String.valueOf(safeLocation.getBlockY()))
                        .replace("{z}", String.valueOf(safeLocation.getBlockZ()))
                        .replace("{targetPlayer}", targetPlayer.getName())
                        .replace("{world}", String.valueOf(player.getWorld())));
    }

    private void biomeType(Player player, String channel) {
        int minX = config.getConfig("config.yml").getInt("channels." + channel + ".range.minX");
        int maxX = config.getConfig("config.yml").getInt("channels." + channel + ".range.maxX");
        int minZ = config.getConfig("config.yml").getInt("channels." + channel + ".range.minZ");
        int maxZ = config.getConfig("config.yml").getInt("channels." + channel + ".range.maxZ");
        World world = Bukkit.getWorld(Objects.requireNonNull(config.getConfig("config.yml").getString("channels." + channel + ".world")));

        assert world != null;
        Location safeLocation = findSafeLocation(world, minX, maxX, minZ, maxZ, channel, true);
        if (safeLocation == null) {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noSafeLocation"));
            return;
        }

        Biome biome = safeLocation.getWorld().getBlockAt(safeLocation.getBlockX(), safeLocation.getBlockY() - 1, safeLocation.getBlockZ()).getBiome();
        setCooldown(player);
        player.setFallDistance(0.0f);
        player.teleport(safeLocation);

        MessageManager.sendMessage(player,
                Objects.requireNonNull(config.getConfig("config.yml").getString("channels." + channel + ".message"))
                        .replace("{biome}", biome.name()));
    }

    private Location findSafeLocation(World world, int minX, int maxX, int minZ, int maxZ, String channel, boolean checkBiome) {
        int x, z, y;
        int attempts = 0;

        do {
            x = getRandom(minX, maxX);
            z = getRandom(minZ, maxZ);
            y = world.getHighestBlockYAt(x, z);

            attempts++;
            if (attempts > 100) {
                return null;
            }

        } while ((checkBiome && !isBiome(world.getBlockAt(x, y - 1, z).getBiome(), channel))
                || !isSafeLocation(world, x, y, z));
        Bukkit.getLogger().info("Location find, location " + new Location(world, x + 0.5, y + 1, z + 0.5));
        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }



    private boolean isSafeLocation(World world, int x, int y, int z) {
        Material below = world.getBlockAt(x, y, z).getType();
        Material at = world.getBlockAt(x, y + 1, z).getType();
        Material above = world.getBlockAt(x, y + 2, z).getType();
        Bukkit.getLogger().info("Blocks" + below + " " + at + " " + above);
        return !isBlacklistedBlock(below) && (at == Material.AIR || at == Material.SNOW) && above == Material.AIR;
    }

    private boolean isBlacklistedBlock(Material material) {
        List<String> blacklistedBlocks = config.getConfig("config.yml").getStringList("blacklistedBlocks");
        blacklistedBlocks = blacklistedBlocks.stream().map(String::toUpperCase).toList();
        return blacklistedBlocks.contains(material.toString());
    }

    private boolean isBiome(Biome biome, String channel) {
        List<String> biomes = config.getConfig("config.yml").getStringList("channels." + channel + ".biomes");
        Bukkit.getLogger().info("Biome" + biome);
        return biomes.stream().map(String::toUpperCase).toList().contains(biome.name());
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() / 1000);
    }

    private boolean isCooldownExpired(Player player) {
        long startTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        return ((System.currentTimeMillis() / 1000) - startTime) >= cooldown;
    }

    private int getRemainingCooldown(Player player) {
        long startTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        return Math.max(0, cooldown - (int) ((System.currentTimeMillis() / 1000) - startTime));
    }

    private int getRandom(int min, int max) {
        return min + new Random().nextInt(max - min + 1);
    }

    private Player findRandomNearbyPlayer(Player player) {
        List<Player> nearbyPlayers = new ArrayList<>(player.getWorld().getPlayers());
        if (!nearbyPlayers.isEmpty()) {
            return nearbyPlayers.get(new Random().nextInt(nearbyPlayers.size()));
        }
        return null;
    }
}
