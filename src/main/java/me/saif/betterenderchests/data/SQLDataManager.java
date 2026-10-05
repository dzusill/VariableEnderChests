package me.saif.betterenderchests.data;

import me.saif.betterenderchests.data.database.SQLDatabase;
import me.saif.betterenderchests.enderchest.EnderChestSnapshot;
import me.saif.betterenderchests.utils.ItemStackSerializer;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SQLDataManager implements DataManager {

    private static final Logger LOGGER = Logger.getLogger("VariableEnderChests");

    protected final SQLDatabase database;
    private final String dataTableName = "enderchests";
    private final String playersTableName = "players";

    public SQLDataManager(SQLDatabase database) {
        this.database = database;
    }

    public String getPlayersTableName() {
        return playersTableName;
    }

    public String getDataTableName() {
        return dataTableName;
    }

    @Override
    public String getName(UUID uuid) {
        String sql = "SELECT * from " + getPlayersTableName() + " WHERE UUID=?";

        try (Connection connection = this.database.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next())
                return resultSet.getString("NAME");

            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public UUID getUUID(String name) {
        String sql = "SELECT * from " + getPlayersTableName() + " WHERE NAME=?";

        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return UUID.fromString(resultSet.getString("UUID"));
            }

            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void init() {
        String createDataTable = "CREATE TABLE IF NOT EXISTS " + getDataTableName() + " (`UUID` VARCHAR(36) NOT NULL PRIMARY KEY, `ROWS` INT, `CONTENTS` LONGTEXT);";
        String createPlayersTable = "CREATE TABLE IF NOT EXISTS " + getPlayersTableName() + " (`UUID` VARCHAR(36) NOT NULL UNIQUE, `NAME` VARCHAR(16) NOT NULL UNIQUE);";
        try (Connection connection = this.database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createDataTable);
            statement.executeUpdate(createPlayersTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finishUp() {
        this.database.close();
    }

    @Override
    public void saveNameAndUUIDs(Map<String, UUID> map) {
        String sql = "REPLACE INTO " + getPlayersTableName() + " (UUID,NAME) VALUES (?, ?)";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String s : map.keySet()) {
                statement.setString(1, map.get(s).toString());
                statement.setString(2, s);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveNameAndUUID(String name, UUID uuid) {
        String sql = "REPLACE INTO " + getPlayersTableName() + " (UUID,NAME) VALUES (?, ?)";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid.toString());
            statement.setString(2, name);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveEnderChestMultiple(Map<UUID, EnderChestSnapshot> snapshotMap) {
        String sql = "REPLACE INTO " + getDataTableName() + " (UUID,`ROWS`,CONTENTS) VALUES (?,?,?)";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (UUID uuid : snapshotMap.keySet()) {
                EnderChestSnapshot snapshot = snapshotMap.get(uuid);

                //the stored data of this chest could not be read, so it is still intact in the database. Never overwrite it.
                if (snapshot.isLoadFailed())
                    continue;

                String contents;
                try {
                    contents = ItemStackSerializer.serialize(snapshot.getContents());
                } catch (Exception | LinkageError e) {
                    //one chest failing to serialize must not stop everyone else's chest from being saved
                    LOGGER.log(Level.SEVERE, "Could not serialize the enderchest of " + snapshot.getName() + " (" + uuid + "). It was not saved.", e);
                    continue;
                }

                statement.setString(1, uuid.toString());
                statement.setInt(2, snapshot.getRows());
                statement.setString(3, contents);
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<UUID, EnderChestSnapshot> loadEnderChestsByUUID(Set<UUID> uuids) {
        if (uuids.isEmpty())
            return new HashMap<>();
        String sql = "SELECT " + getDataTableName() + ".UUID," + getPlayersTableName() + ".NAME,`ROWS`,CONTENTS FROM " + getDataTableName() + " LEFT JOIN " + getPlayersTableName()
                + " ON " + getDataTableName() + ".UUID=" + getPlayersTableName() + ".UUID WHERE " + getWhereConditionForUUID(uuids.size());
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Map<UUID, EnderChestSnapshot> resultMap = new HashMap<>();
            Iterator<UUID> uuidIterator = uuids.iterator();
            int i = 0;
            while (i < uuids.size()) {
                statement.setString(i + 1, uuidIterator.next().toString());
                i++;
            }

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int rows = resultSet.getInt("ROWS");
                UUID uuid = UUID.fromString(resultSet.getString("UUID"));
                String name = resultSet.getString("NAME");
                resultMap.put(uuid, readSnapshot(uuid, name, rows, resultSet.getString("CONTENTS")));
            }

            return resultMap;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    //Unreadable data becomes a "load failed" snapshot instead of an empty chest, so it can never be saved over the real data.
    private EnderChestSnapshot readSnapshot(UUID uuid, String name, int rows, String contents) {
        try {
            return new EnderChestSnapshot(uuid, name, ItemStackSerializer.deserialize(contents), rows);
        } catch (RuntimeException | LinkageError e) {
            LOGGER.log(Level.SEVERE, "Could not read the stored enderchest of " + name + " (" + uuid + "). It has been locked and will not be "
                    + "saved, the stored data was left untouched in the database.", e);
            return EnderChestSnapshot.loadFailed(uuid, name, rows);
        }
    }

    private String getWhereConditionForUUID(int num) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < num; i++) {
            stringBuilder.append(getDataTableName()).append(".UUID=?");
            if (i == num - 1)
                stringBuilder.append(";");
            else
                stringBuilder.append("OR ");
        }
        return stringBuilder.toString();
    }

    @Override
    public Set<UUID> getAllEnderChests() {
        String sql = "SELECT UUID FROM " + this.getDataTableName() + ";";
        Set<UUID> uuids = new HashSet<>();
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                uuids.add(UUID.fromString(resultSet.getString("UUID")));
            }

            return uuids;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<String, EnderChestSnapshot> loadEnderChestsByName(Set<String> names) {
        if (names.isEmpty())
            return new HashMap<>();
        String sql = "SELECT " + getDataTableName() + ".UUID," + getPlayersTableName() + ".NAME,`ROWS`,CONTENTS FROM " + getDataTableName() + " LEFT JOIN " + getPlayersTableName()
                + " ON " + getDataTableName() + ".UUID=" + getPlayersTableName() + ".UUID WHERE " + getWhereConditionForNames(names.size());
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Map<String, EnderChestSnapshot> resultMap = new HashMap<>();
            Iterator<String> nameIterator = names.iterator();
            int i = 0;
            while (i < names.size()) {
                statement.setString(i + 1, nameIterator.next().toLowerCase());
                i++;
            }

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int rows = resultSet.getInt("ROWS");
                UUID uuid = UUID.fromString(resultSet.getString("UUID"));
                String name = resultSet.getString("NAME");
                resultMap.put(name.toLowerCase(), readSnapshot(uuid, name, rows, resultSet.getString("CONTENTS")));
            }

            return resultMap;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getWhereConditionForNames(int num) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < num; i++) {
            stringBuilder.append(dataTableName).append(".UUID = (SELECT ").append("UUID FROM ")
                    .append(playersTableName).append(" WHERE ").append("LOWER(NAME)=? LIMIT 1)");
            if (i == num - 1)
                stringBuilder.append(";");
            else
                stringBuilder.append("OR ");
        }
        return stringBuilder.toString();
    }

    @Override
    public void purge(char... confirm) {
        if (!new String(confirm).equals("YES"))
            return;
        try (Connection connection = this.database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("delete from " + this.getDataTableName());
            statement.executeUpdate("delete from " + this.getPlayersTableName());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteEnderChest(UUID uuid) {
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement("delete from " + this.getDataTableName() + " WHERE UUID=?;")) {
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteEnderChest(String name) {
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement("delete from " + this.getDataTableName() + " WHERE UUID=(SELECT * FROM " + this.getPlayersTableName() + " WHERE LOWER(NAME)=? LIMIT 1);")) {
            statement.setString(1, name.toLowerCase(Locale.ROOT));
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
