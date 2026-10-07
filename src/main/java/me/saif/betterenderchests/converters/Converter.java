package me.saif.betterenderchests.converters;

import me.saif.betterenderchests.OberonEnder;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public abstract class Converter {

    private String name;
    protected OberonEnder plugin;

    public Converter(OberonEnder plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract boolean convert(String... args);

}
