package me.penguinx13.wrtp;

import me.penguinx13.wrtp.scanner.ChunkScanner;
import me.penguinx13.wrtp.cache.SQLiteManager;
import me.penguinx13.wapi.Managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public class WRTP extends JavaPlugin {

    private static WRTP instance;
    private SQLiteManager sqliteManager;
    private ChunkScanner scanner;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Plugin Enabled");

        /* ---------- CONFIG ---------- */
        ConfigManager configManager = new ConfigManager(this);
        configManager.registerConfig("config.yml");

        /* ---------- DATABASE ---------- */
        sqliteManager = new SQLiteManager(new File(getDataFolder(), "cache"));

        /* ---------- EVENTS ---------- */
        TeleportEvent teleportEvent = new TeleportEvent();
        getServer().getPluginManager().registerEvents(teleportEvent, this);

        /* ---------- SCANNER ---------- */
        String worldName = configManager
                .getConfig("config.yml")
                .getString("cacheSett.world");

        World world = Bukkit.getWorld(Objects.requireNonNull(worldName));
        if (world == null) {
            getLogger().severe("[WRTP] World not found: " + worldName);
        } else {
            scanner = new ChunkScanner(this, world, configManager);
        }
        scanner.start();

        /* ---------- COMMANDS ---------- */
        Commands commands = new Commands(this, configManager, teleportEvent);
        Objects.requireNonNull(getCommand("rtp")).setExecutor(commands);
        Objects.requireNonNull(getCommand("wrtp")).setExecutor(commands);
    }

    @Override
    public void onDisable() {
        if (scanner != null && scanner.isRunning()) {
            scanner.stop();
        }
        getLogger().info("Plugin Disabled");
    }

    /* ---------- GETTERS ---------- */

    public static WRTP getInstance() {
        return instance;
    }

    public SQLiteManager getDatabase() {
        return sqliteManager;
    }

    public ChunkScanner getScanner() {
        return scanner;
    }
}
