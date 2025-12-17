package me.penguinx13.wrtp;

import me.penguinx13.wrtp.cache.ChunkScanner;
import me.penguinx13.wrtp.cache.SQLiteManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import me.penguinx13.wapi.Managers.ConfigManager;

import java.io.File;
import java.util.Objects;

public class WRTP extends JavaPlugin{
    public TeleportEvent teleportEvent;

    @Override
    public void onEnable() {
        getLogger().info("Plugin Enabled");

        ConfigManager configManager = new ConfigManager(this);
        configManager.registerConfig("config.yml");

        SQLiteManager db = new SQLiteManager(new File("plugins/WRTP/cache"));

        teleportEvent = new TeleportEvent(db);
        getServer().getPluginManager().registerEvents(teleportEvent, this);

        new ChunkScanner(db,
                Objects.requireNonNull(Bukkit.getWorld(Objects.requireNonNull(configManager.getConfig("config.yml").getString("cacheSett.world")))),
                configManager.getConfig("config.yml").getInt("cacheSett.minX"),
                configManager.getConfig("config.yml").getInt("cacheSett.maxX"),
                configManager.getConfig("config.yml").getInt("cacheSett.minZ"),
                configManager.getConfig("config.yml").getInt("cacheSett.maxZ")).startScanning();

        Objects.requireNonNull(getCommand("rtp")).setExecutor(new Commands(this, configManager, teleportEvent));
        Objects.requireNonNull(getCommand("wrtp")).setExecutor(new Commands(this, configManager, teleportEvent));
    }

}