package me.penguinx13.wrtp;

import me.penguinx13.wapi.Managers.ConfigManager;
import me.penguinx13.wapi.Managers.MessageManager;
import me.penguinx13.wrtp.scanner.ChunkScanner;
import me.penguinx13.wrtp.scanner.ScanStatus;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args) {

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
            if (sender instanceof Player player) {
                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    if (sender.hasPermission("wrtp.reload")) {
                        plugin.getServer().getPluginManager().disablePlugin(plugin);
                        plugin.getServer().getPluginManager().enablePlugin(plugin);


                        MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.reloadMessage"));
                    } else {
                        MessageManager.sendMessage(player, config.getConfig("config.yml").getString("messages.noPermission"));
                    }
                    return true;
                }
            }else if(sender instanceof ConsoleCommandSender) {
                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                    plugin.getServer().getPluginManager().enablePlugin(plugin);

                    plugin.getLogger().info(config.getConfig("config.yml").getString("messages.reloadMessage"));
                    return true;
                }else if (args.length == 2 && args[0].equalsIgnoreCase("scan")
                        && args[1].equalsIgnoreCase("status")) {

                    ScanStatus status = plugin.getScanner().getStatus();

                    sender.sendMessage("§6[WRTP] §fСтатус сканирования:");
                    sender.sendMessage("§7• Активен: §e" + status.running());
                    sender.sendMessage("§7• Пауза (TPS): §e" + status.paused());
                    sender.sendMessage("§7• Точки: §a" + status.points()
                            + " §7/ §a" + status.target());

                    double percent = (status.points() / (double) status.target()) * 100;
                    sender.sendMessage(String.format("§7• Прогресс: §a%.2f%%", percent));
                    sender.sendMessage(String.format("§7• Скорость: §a%.2f точек/сек",
                            status.pointsPerSecond()));

                    return true;
                }else if (args.length == 2 && args[0].equalsIgnoreCase("scan")
                        && args[1].equalsIgnoreCase("stop")) {
                    plugin.getScanner().stop();
                }else if (args.length == 2 && args[0].equalsIgnoreCase("scan")
                        && args[1].equalsIgnoreCase("resume")) {
                    plugin.getScanner().resume();
                }else if (args.length == 2 && args[0].equalsIgnoreCase("scan")
                        && args[1].equalsIgnoreCase("pause")) {
                    plugin.getScanner().pause();
                }

            }

        }


        return true;
    }
}