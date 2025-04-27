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
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

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
        String type = config.getConfig("config.yml").getString("channels."+channel+".type");
        if (Boolean.TRUE.equals(ignoreCooldown) || isCooldownExpired(player)) {
            switch (type.toUpperCase()) {
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
                    MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.unknownType").replace("{type}", type));
            }
        } else{
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.cooldown").replace("{time}", String.valueOf(getRemainingCooldown(player))));
        }
    }
    private void defaultType(Player player, String channel) {
        int minX = config.getConfig("config.yml").getInt("channels."+channel+".range.minX");
        int maxX = config.getConfig("config.yml").getInt("channels."+channel+".range.maxX");
        int minZ = config.getConfig("config.yml").getInt("channels."+channel+".range.minZ");
        int maxZ = config.getConfig("config.yml").getInt("channels."+channel+".range.maxZ");
        World world = Bukkit.getWorld(config.getConfig("config.yml").getString("channels."+channel+".world"));
        int x, z, y;
        final Player playerFinal = player;
        do {
            x = getRandom(minX, maxX);
            z = getRandom(minZ, maxZ);
            y = player.getWorld().getHighestBlockYAt(x, z);

        } while (isBlacklistedBlock(player.getWorld().getBlockAt(x, y-1, z).getType(), channel) &&
                player.getWorld().getBlockAt(x, y + 1, z).getType() == Material.AIR &&
                player.getWorld().getBlockAt(x, y + 2, z).getType() == Material.AIR);

        setCooldown(player);

        player.setInvulnerable(true);
        int finalX = x;
        int finalY = y;
        int finalZ = z;
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("WRTP"), () -> {
            player.teleport(new Location(world, finalX + 0.5, finalY + 1, finalZ + 0.5));
            player.setInvulnerable(false);
        }, 5L);

        MessageManager.sendMessage(player,
                config.getConfig("config.yml").getString("channels."+channel+".message")
                        .replace("{x}", String.valueOf(x))
                        .replace("{z}", String.valueOf(z))
                        .replace("{y}", String.valueOf(y))
                        .replace("{world}", world.getName())
        );

    }
    private void nearbyType(Player player, String channel) {
        int min = config.getConfig("config.yml").getInt("channels."+channel+".nearbyRange.min");
        int max = config.getConfig("config.yml").getInt("channels."+channel+".nearbyRange.max");
        int x, z, y;
        int minOnline = config.getConfig("config.yml").getInt("channels."+channel+".minOnline");
        if (Bukkit.getOnlinePlayers().size() >= minOnline) {
            int range = getRandom(min, max);
            Player targetPlayer = findRandomNearbyPlayer(player);
            if(targetPlayer != null) {
                double angle = Math.toRadians(new Random().nextDouble() * 360);
                do {
                    x = (int) (targetPlayer.getLocation().getX() + range * Math.cos(angle));
                    z = (int) (targetPlayer.getLocation().getZ() + range * Math.sin(angle));

                    y = targetPlayer.getWorld().getHighestBlockYAt(x, z);

                } while (isBlacklistedBlock(player.getWorld().getBlockAt(x, y-1, z).getType(), channel) &&
                        player.getWorld().getBlockAt(x, y + 1, z).getType() == Material.AIR &&
                        player.getWorld().getBlockAt(x, y + 2, z).getType() == Material.AIR);
                setCooldown(player);

                player.setInvulnerable(true);
                int finalX = x;
                int finalY = y;
                int finalZ = z;
                Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("WRTP"), () -> {
                    player.teleport(new Location(targetPlayer.getWorld(), finalX + 0.5, finalY + 1, finalZ + 0.5));
                    player.setInvulnerable(false);
                }, 5L);
                MessageManager.sendMessage(player,
                        (config.getConfig("config.yml").getString("channels."+channel+".message"))
                                .replace("{x}", String.valueOf(x))
                                .replace("{z}", String.valueOf(z))
                                .replace("{y}", String.valueOf(y))
                                .replace("{targetPlayer}", String.valueOf(targetPlayer.getName()))
                                .replace("{world}", String.valueOf(targetPlayer.getWorld()))
                );
            } else {
                MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noPlayerInWorld"));
            }
        } else {
            MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noMinPlayers"));
        }
    }
    private void biomeType(Player player, String channel) {
        int minX = config.getConfig("config.yml").getInt("channels."+channel+".range.minX");
        int maxX= config.getConfig("config.yml").getInt("channels."+channel+".range.maxX");
        int minZ = config.getConfig("config.yml").getInt("channels."+channel+".range.minZ");
        int maxZ = config.getConfig("config.yml").getInt("channels."+channel+".range.maxZ");
        World world = Bukkit.getWorld(config.getConfig("config.yml").getString("channels."+channel+".world"));
        int x, z, y;

        do {
            x = getRandom(minX, maxX);
            z = getRandom(minZ, maxZ);
            y = player.getWorld().getHighestBlockYAt(x, z);

        } while (!isBiome(world.getBlockAt(x, y - 1, z).getBiome(), channel) &&
                world.getBlockAt(x, y + 1, z).getType() == Material.AIR &&
                world.getBlockAt(x, y + 2, z).getType() == Material.AIR);
        Biome biome = world.getBlockAt(x, y-1, z).getBiome();
        setCooldown(player);

        player.setInvulnerable(true);
        int finalX = x;
        int finalY = y;
        int finalZ = z;
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("WRTP"), () -> {
            player.teleport(new Location(world, finalX + 0.5, finalY + 1, finalZ + 0.5));
            player.setInvulnerable(false);
        }, 5L);
        MessageManager.sendMessage(player,
                config.getConfig("config.yml").getString("channels."+channel+".message")
                        .replace("{biome}", biome.name())
        );
    }

    private void setCooldown(Player player) {
        long currentTime =  (System.currentTimeMillis() / 1000);
        cooldowns.put(player.getUniqueId(), currentTime);
    }
    private boolean isCooldownExpired(Player player) {
        long startTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        int elapsedTime = (int) ((System.currentTimeMillis() / 1000) - startTime);
        return elapsedTime >= cooldown;
    }
    private int getRemainingCooldown(Player player) {
        long startTime = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        int elapsedTime = (int) ((System.currentTimeMillis() / 1000) - startTime);
        return Math.max(0, cooldown - elapsedTime);
    }

    private int getRandom(int min, int max) {
        return min + new Random().nextInt(max - min + 1);
    }

    private boolean isBlacklistedBlock(Material material, String channel) {
        List<String> blacklistedBlocks = config.getConfig("config.yml").getStringList("channels." + channel + ".blacklistedBlocks");
        blacklistedBlocks = blacklistedBlocks.stream().map(String::toUpperCase).collect(Collectors.toList());
        return blacklistedBlocks.contains(material.toString());
    }
    private boolean isBiome(Biome biome, String channel) {
        List<String> biomes = config.getConfig("config.yml").getStringList("channels." + channel + ".biomes");
        biomes = biomes.stream().map(String::toUpperCase).collect(Collectors.toList());
        return biomes.contains(biome.name());
    }

    private Player findRandomNearbyPlayer(Player player) {
        World world = player.getWorld();

        List<Player> nearbyPlayers = new ArrayList<>(world.getPlayers());

        if (!nearbyPlayers.isEmpty()) {
            return nearbyPlayers.get(new Random().nextInt(nearbyPlayers.size()));
        } else {
            return null;
        }
    }

}