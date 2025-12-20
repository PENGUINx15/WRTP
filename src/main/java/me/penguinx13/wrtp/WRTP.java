package me.penguinx13.wrtp;

import me.penguinx13.wrtp.cache.ChunkScanner;
import me.penguinx13.wrtp.cache.SQLiteManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import me.penguinx13.wapi.Managers.ConfigManager;

import java.io.File;
import java.util.Objects;

public class WRTP extends JavaPlugin{
    private static WRTP instance;
    public TeleportEvent teleportEvent;
    public SQLiteManager sqliteManager;
    @Override
    public void onEnable() {
        getLogger().info("Plugin Enabled");

        ConfigManager configManager = new ConfigManager(this);
        configManager.registerConfig("config.yml");
        instance =this;
        sqliteManager = new SQLiteManager(new File("plugins/WRTP/cache"));

        teleportEvent = new TeleportEvent();
        getServer().getPluginManager().registerEvents(teleportEvent, this);

        new ChunkScanner(sqliteManager,
                Objects.requireNonNull(Bukkit.getWorld(Objects.requireNonNull(configManager.getConfig("config.yml").getString("cacheSett.world")))),
                configManager.getConfig("config.yml").getInt("cacheSett.minX"),
                configManager.getConfig("config.yml").getInt("cacheSett.maxX"),
                configManager.getConfig("config.yml").getInt("cacheSett.minZ"),
                configManager.getConfig("config.yml").getInt("cacheSett.maxZ")).startScanning();

        Objects.requireNonNull(getCommand("rtp")).setExecutor(new Commands(this, configManager, teleportEvent));
        Objects.requireNonNull(getCommand("wrtp")).setExecutor(new Commands(this, configManager, teleportEvent));
    }
    public static WRTP getInstance() {
        return instance;
    }
    public SQLiteManager getDatabase() {
        return sqliteManager;
    }
}