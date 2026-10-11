package me.saif.betterenderchests.hooks;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

/**
 * Tells whether a player is hidden by a vanish plugin. PremiumVanish, SuperVanish, EssentialsX and CMI all set the
 * {@code vanished} metadata flag, so this needs no dependency on any of them.
 */
public final class VanishHook {

    private VanishHook() {
    }

    public static boolean isVanished(Player player) {
        for (MetadataValue value : player.getMetadata("vanished")) {
            if (value.asBoolean())
                return true;
        }
        return false;
    }
}
