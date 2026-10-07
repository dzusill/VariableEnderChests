package me.saif.betterenderchests.hooks;

import me.saif.betterenderchests.OberonEnder;
import me.saif.betterenderchests.hooks.showitem.ShowItemHookListener;
import org.bukkit.Bukkit;

public class ShowItemHook {

    private OberonEnder plugin;

    public ShowItemHook(OberonEnder plugin) {
        if (Bukkit.getPluginManager().getPlugin("ShowItem") == null) {
            return;
        }
        plugin.getLogger().info("Hooked into ShowItem");
        Bukkit.getPluginManager().registerEvents(new ShowItemHookListener(plugin), plugin);
        this.plugin = plugin;
    }

}
