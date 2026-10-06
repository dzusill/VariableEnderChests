package me.saif.betterenderchests.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class LegacyDataMigrationTest {

    private static final Logger LOGGER = Logger.getLogger("test");

    @TempDir
    Path plugins;

    private File newFolder() {
        return plugins.resolve("OberonEnder").toFile();
    }

    private Path legacy() throws Exception {
        Path legacy = plugins.resolve(LegacyDataMigration.LEGACY_FOLDER);
        Files.createDirectories(legacy.resolve("lang"));
        Files.writeString(legacy.resolve("data.db"), "chests");
        Files.writeString(legacy.resolve("config.yml"), "owner: true");
        Files.writeString(legacy.resolve("lang/english.yml"), "owner lang");
        return legacy;
    }

    @Test
    void copiesEverythingAndKeepsTheOldFolder() throws Exception {
        Path legacy = legacy();

        assertTrue(LegacyDataMigration.migrate(newFolder(), LOGGER));

        Path target = newFolder().toPath();
        assertEquals("chests", Files.readString(target.resolve("data.db")));
        assertEquals("owner: true", Files.readString(target.resolve("config.yml")));
        assertEquals("owner lang", Files.readString(target.resolve("lang/english.yml")));
        assertEquals("chests", Files.readString(legacy.resolve("data.db")), "old folder must stay as a backup");
    }

    @Test
    void ownerFilesReplaceFreshDefaults() throws Exception {
        legacy();
        Path target = newFolder().toPath();
        Files.createDirectories(target);
        Files.writeString(target.resolve("config.yml"), "defaults");

        LegacyDataMigration.migrate(newFolder(), LOGGER);

        assertEquals("owner: true", Files.readString(target.resolve("config.yml")));
    }

    @Test
    void runsOnlyOnce() throws Exception {
        legacy();
        LegacyDataMigration.migrate(newFolder(), LOGGER);
        Path target = newFolder().toPath();
        Files.writeString(target.resolve("data.db"), "newer chests");

        assertFalse(LegacyDataMigration.migrate(newFolder(), LOGGER));
        assertEquals("newer chests", Files.readString(target.resolve("data.db")), "later starts must never copy again");
    }

    @Test
    void freshInstallWithoutOldFolderNeverCopiesLater() throws Exception {
        assertFalse(LegacyDataMigration.migrate(newFolder(), LOGGER));
        legacy();

        assertFalse(LegacyDataMigration.migrate(newFolder(), LOGGER));
        assertFalse(Files.exists(newFolder().toPath().resolve("data.db")));
    }
}
