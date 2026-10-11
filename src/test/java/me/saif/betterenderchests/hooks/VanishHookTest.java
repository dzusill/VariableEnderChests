package me.saif.betterenderchests.hooks;

import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VanishHookTest {

    private static Player withMetadata(List<MetadataValue> values) {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getMetadata("vanished")).thenReturn(values);
        return player;
    }

    private static MetadataValue flag(boolean value) {
        MetadataValue meta = Mockito.mock(MetadataValue.class);
        Mockito.when(meta.asBoolean()).thenReturn(value);
        return meta;
    }

    @Test
    void setFlagMeansVanished() {
        assertTrue(VanishHook.isVanished(withMetadata(List.of(flag(true)))));
    }

    @Test
    void noMetadataMeansVisible() {
        assertFalse(VanishHook.isVanished(withMetadata(List.of())));
    }

    @Test
    void flagSetToFalseMeansVisible() {
        assertFalse(VanishHook.isVanished(withMetadata(List.of(flag(false)))));
    }

    @Test
    void anyPluginReportingVanishedWins() {
        //PremiumVanish and EssentialsX can both set the flag, one of them saying false must not hide the other
        assertTrue(VanishHook.isVanished(withMetadata(List.of(flag(false), flag(true)))));
    }
}
