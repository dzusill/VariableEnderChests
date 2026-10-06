package me.saif.betterenderchests.enderchest;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * One configurable sound, read from a section like:
 * <pre>
 * enabled: true
 * sound: 'BLOCK_ENDER_CHEST_OPEN'   # Sound name, or a resource key such as 'block.ender_chest.open'
 * volume: 1.0
 * pitch: 1.0
 * </pre>
 * A resource key (anything with a '.' or ':') is played as is, which also allows resource pack sounds.
 */
public final class SoundSetting {

    private final boolean enabled;
    private final Sound sound;
    private final String key;
    private final float volume;
    private final float pitch;

    private SoundSetting(boolean enabled, Sound sound, String key, float volume, float pitch) {
        this.enabled = enabled;
        this.sound = sound;
        this.key = key;
        this.volume = volume;
        this.pitch = pitch;
    }

    /**
     * @param section  the config section, may be null (then the fallback is used)
     * @param fallback the sound used when the section is missing or names an unknown sound
     */
    public static SoundSetting load(ConfigurationSection section, Sound fallback, String path, Logger logger) {
        if (section == null)
            return new SoundSetting(true, fallback, null, 1F, 1F);

        boolean enabled = section.getBoolean("enabled", true);
        float volume = (float) section.getDouble("volume", 1.0);
        float pitch = (float) section.getDouble("pitch", 1.0);
        String name = section.getString("sound", "").trim();

        if (name.isEmpty())
            return new SoundSetting(enabled, fallback, null, volume, pitch);

        if (name.contains(".") || name.contains(":"))
            return new SoundSetting(enabled, null, name.toLowerCase(Locale.ROOT), volume, pitch);

        try {
            return new SoundSetting(enabled, Sound.valueOf(name.toUpperCase(Locale.ROOT)), null, volume, pitch);
        } catch (IllegalArgumentException e) {
            logger.warning("Unknown sound '" + name + "' at " + path + ". Using the default sound instead.");
            return new SoundSetting(enabled, fallback, null, volume, pitch);
        }
    }

    public void play(Player player, Location location) {
        if (!enabled)
            return;

        if (key != null)
            player.playSound(location, key, volume, pitch);
        else if (sound != null)
            player.playSound(location, sound, volume, pitch);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Sound getSound() {
        return sound;
    }

    public String getKey() {
        return key;
    }

    public float getVolume() {
        return volume;
    }

    public float getPitch() {
        return pitch;
    }
}
