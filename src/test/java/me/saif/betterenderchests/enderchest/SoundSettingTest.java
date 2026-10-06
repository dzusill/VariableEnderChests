package me.saif.betterenderchests.enderchest;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SoundSettingTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    private static YamlConfiguration yaml(String text) throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(text);
        return yaml;
    }

    @Test
    void resourceKeyIsKeptAsKey() throws Exception {
        YamlConfiguration yaml = yaml("s:\n  sound: 'Custom.Pack:ui.open'\n  volume: 0.5\n  pitch: 1.5\n");

        SoundSetting setting = SoundSetting.load(yaml.getConfigurationSection("s"), null, "s", LOGGER);

        assertTrue(setting.isEnabled());
        assertEquals("custom.pack:ui.open", setting.getKey());
        assertNull(setting.getSound());
        assertEquals(0.5F, setting.getVolume());
        assertEquals(1.5F, setting.getPitch());
    }

    @Test
    void disabledSoundIsDisabled() throws Exception {
        YamlConfiguration yaml = yaml("s:\n  enabled: false\n  sound: 'block.ender_chest.open'\n");

        assertFalse(SoundSetting.load(yaml.getConfigurationSection("s"), null, "s", LOGGER).isEnabled());
    }

    @Test
    void missingSectionOrEmptySoundFallsBackWithDefaults() throws Exception {
        SoundSetting missing = SoundSetting.load(null, null, "s", LOGGER);
        assertTrue(missing.isEnabled());
        assertNull(missing.getKey());
        assertEquals(1F, missing.getVolume());

        YamlConfiguration yaml = yaml("s:\n  sound: ''\n  pitch: 2\n");
        SoundSetting empty = SoundSetting.load(yaml.getConfigurationSection("s"), null, "s", LOGGER);
        assertNull(empty.getKey());
        assertEquals(2F, empty.getPitch());
    }
}
