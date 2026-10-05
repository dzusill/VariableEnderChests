package me.saif.betterenderchests.data;

import me.saif.betterenderchests.data.database.SQLiteDatabase;
import me.saif.betterenderchests.enderchest.EnderChestSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the "chest silently comes back empty and is saved over the real data" bug.
 * Stored data that cannot be read must stay untouched in the database.
 */
class UnreadableChestSafetyTest {

    private static final UUID PLAYER = UUID.fromString("3997988f-c010-4743-8cdb-7b8a457f4108");

    @TempDir
    File folder;

    private SQLiteDatabase database;
    private SQLiteDataManager dataManager;

    @BeforeEach
    void setUp() {
        database = new SQLiteDatabase(folder, "data.db");
        dataManager = new SQLiteDataManager(database);
        dataManager.init();
    }

    @AfterEach
    void tearDown() {
        dataManager.finishUp();
    }

    private void storeRaw(String contents) throws Exception {
        try (Connection c = database.getConnection();
             PreparedStatement st = c.prepareStatement("REPLACE INTO enderchests (UUID,`ROWS`,CONTENTS) VALUES (?,?,?)")) {
            st.setString(1, PLAYER.toString());
            st.setInt(2, 6);
            st.setString(3, contents);
            st.executeUpdate();
        }
    }

    private String readRaw() throws Exception {
        try (Connection c = database.getConnection();
             PreparedStatement st = c.prepareStatement("SELECT CONTENTS FROM enderchests WHERE UUID=?")) {
            st.setString(1, PLAYER.toString());
            ResultSet rs = st.executeQuery();
            assertTrue(rs.next(), "row must still exist");
            return rs.getString(1);
        }
    }

    @Test
    void unreadableDataIsReportedAsLoadFailedInsteadOfAnEmptyChest() throws Exception {
        storeRaw("nbtbytes:this is not valid base64 !!!");

        EnderChestSnapshot snapshot = dataManager.loadEnderChest(PLAYER);

        assertNotNull(snapshot);
        assertTrue(snapshot.isLoadFailed());
        assertEquals(6, snapshot.getRows());
    }

    @Test
    void loadFailedSnapshotIsNeverWrittenBack() throws Exception {
        String stored = "nbtbytes:this is not valid base64 !!!";
        storeRaw(stored);

        EnderChestSnapshot snapshot = dataManager.loadEnderChest(PLAYER);
        dataManager.saveEnderChestMultiple(Map.of(PLAYER, snapshot));

        assertEquals(stored, readRaw(), "stored data of an unreadable chest must not be overwritten");
    }

    @Test
    void loadFailedFlagIsSetWhenLoadingMultipleChests() throws Exception {
        storeRaw("json:{not json");

        Map<UUID, EnderChestSnapshot> loaded = dataManager.loadEnderChestsByUUID(Set.of(PLAYER));

        assertTrue(loaded.get(PLAYER).isLoadFailed());
    }
}
