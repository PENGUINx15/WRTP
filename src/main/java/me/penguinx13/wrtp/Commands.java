package me.penguinx13.wrtp;

import me.penguinx13.wapi.Managers.ConfigManager;
import me.penguinx13.wapi.Managers.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class Commands implements CommandExecutor {
    private final ConfigManager config;
    private final WRTP plugin;
    private final TeleportEvent teleportEvent;


    public Commands(WRTP plugin, ConfigManager config, TeleportEvent teleportEvent) {
        this.config = config;
        this.plugin = plugin;
        this.teleportEvent = teleportEvent;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("rtp")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (args.length < 1) {
                    MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.usage"));
                }

                String channel = args[0];
                Player targetPlayer;
                if (args.length >= 2) {
                    targetPlayer = Bukkit.getPlayer(args[1]);
                } else {
                    targetPlayer = player;
                }

                if (config.getConfig("config.yml").getConfigurationSection("channels").contains(channel)) {
                    if (player.hasPermission("wrtp."+channel)) {
                        teleportEvent.teleportPlayer(targetPlayer, channel);
                    } else {
                        MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noPermission"));
                    }
                } else {
                    MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.unknownChannel").replace("{channel}", channel));

                }
            }else if(sender instanceof ConsoleCommandSender) {
                if (args.length < 1) {
                    MessageManager.sendLog(plugin,"info", config.getConfig("config.yml").getString("messages.usage"));
                }

                String channel = args[0];
                Player targetPlayer = null;
                if (args.length >= 2) {
                    targetPlayer = Bukkit.getPlayer(args[1]);
                } else {
                    MessageManager.sendLog(plugin,"warn", "Enter player name");
                }

                if (config.getConfig("config.yml").getConfigurationSection("channels").contains(channel)) {
                    teleportEvent.teleportPlayer(targetPlayer, channel);
                } else {
                    MessageManager.sendLog(plugin,"info", config.getConfig("config.yml").getString("messages.unknownChannel").replace("{channel}", channel));

                }
            }
        }
        if (cmd.getName().equalsIgnoreCase("wrtp")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (sender.hasPermission("wrtp.reload")) {
                        plugin.getServer().getPluginManager().disablePlugin(plugin);
                        plugin.getServer().getPluginManager().enablePlugin(plugin);


                        MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.reloadMessage"));
                        return true;
                    } else {
                        MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noPermission"));
                        return true;
                    }
                }else if(sender instanceof ConsoleCommandSender) {
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    plugin.getServer().getPluginManager().enablePlugin(plugin);


                    MessageManager.sendLog(plugin, "info", config.getConfig("config.yml").getString("messages.reloadMessage"));
                    return true;
                }
            }
        }


        return true;
    }
}