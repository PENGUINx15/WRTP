package me.penguinx13.wrtp;

import org.bukkit.plugin.java.JavaPlugin;
import me.penguinx13.wapi.Managers.ConfigManager;

public class WRTP extends JavaPlugin{
    public TeleportEvent teleportEvent;
    private ConfigManager config;

    @Override
    public void onEnable() {
        getLogger().info("Plugin Enabled");

        ConfigManager configManager = new ConfigManager(this);
        configManager.registerConfig("config.yml");

        teleportEvent = new TeleportEvent(config);
        getServer().getPluginManager().registerEvents(teleportEvent, this);

        getCommand("rtp").setExecutor(new Commands(this, config, teleportEvent));
        getCommand("wrtp").setExecutor(new Commands(this, config, teleportEvent));
    }

}