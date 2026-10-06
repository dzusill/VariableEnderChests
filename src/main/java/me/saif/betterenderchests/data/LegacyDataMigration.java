package me.saif.betterenderchests.data;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Logger;

/**
 * The plugin used to be called VariableEnderChests, so its data lived in plugins/VariableEnderChests/.
 * On the first start under the new name everything from that folder (data.db, config.yml, lang/) is copied
 * into the new data folder. The old folder is left untouched as a backup.
 * <p>
 * Files from the old folder replace same-named files in the new one, because before the migration the new
 * folder can only hold freshly generated defaults. A marker file makes sure this runs exactly once.
 */
public final class LegacyDataMigration {

    public static final String LEGACY_FOLDER = "VariableEnderChests";
    static final String MARKER = ".migrated-from-" + LEGACY_FOLDER;

    private LegacyDataMigration() {
    }

    /**
     * @return true if data was copied from the legacy folder
     */
    public static boolean migrate(File dataFolder, Logger logger) throws IOException {
        Path target = dataFolder.toPath();
        Path marker = target.resolve(MARKER);
        if (Files.exists(marker))
            return false;

        Path legacy = target.resolveSibling(LEGACY_FOLDER);
        Files.createDirectories(target);

        if (!Files.isDirectory(legacy) || legacy.equals(target)) {
            Files.createFile(marker);
            return false;
        }

        logger.info("Copying data from plugins/" + LEGACY_FOLDER + "/ (the plugin's old name). The old folder is kept as a backup.");
        Files.walkFileTree(legacy, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(legacy.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(legacy.relativize(file).toString()),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });

        Files.createFile(marker);
        return true;
    }
}
