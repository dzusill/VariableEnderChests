package me.saif.betterenderchests.command.commands;

import me.saif.betterenderchests.OberonEnder;
import me.saif.betterenderchests.command.PluginCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /oberonender reload - applies config.yml and the lang files without a restart or a plugin reload.
 * Stored ender chests and the database are not touched.
 */
public class ReloadCommand extends PluginCommand {

    private static final String PERMISSION = "enderchest.reload";

    private final OberonEnder plugin;

    public ReloadCommand(OberonEnder plugin) {
        super("oberonender");
        this.plugin = plugin;
    }

    @Override
    public void onCommand(CommandSender sender, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + alias + " reload");
            return;
        }

        String problem;
        try {
            problem = plugin.reloadSettings();
        } catch (RuntimeException e) {
            plugin.getLogger().severe("Reload failed: " + e);
            problem = "see the console (" + e.getClass().getSimpleName() + ")";
        }

        if (problem != null) {
            sender.sendMessage(ChatColor.RED + "OberonEnder was NOT reloaded: " + problem);
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "OberonEnder reloaded: config.yml and lang files. Stored ender chests were not touched.");
        sender.sendMessage(ChatColor.GRAY + "Database settings, command names and papi-identifier need a restart.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String alias, String[] args) {
        if (!sender.hasPermission(PERMISSION))
            return Collections.emptyList();

        List<String> options = new ArrayList<>();
        if (args.length == 1 && "reload".startsWith(args[0].toLowerCase(Locale.ROOT)))
            options.add("reload");
        return options;
    }
}
