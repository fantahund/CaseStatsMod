package de.fanta.casestats.data;

import de.fanta.casestats.CachedPlayer;
import de.fanta.casestats.CaseStats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.logging.log4j.Level;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Database {
    private SQLConnection connection = null;
    private final String insertEntryQuery;
    private final String selectNextEntryQuery;
    private final String selectEntryQuery;
    private final String deleteEntryQuery;
    private final String insertUUIDPlayerQuery;
    private final String selectUUIDPlayerQuery;
    private final String incrementCreatedStatsQuery;
    private final String incrementFinishedStatsQuery;
    private final String updateCurrentEditQuery;
    private final String selectCurrentEditsQuery;
    private final String selectCurrentEditQuery;
    private final String selectStatsQuary;
    private final String countEntriesQuary;
    private final String tablename;

    public Database() {
        String url = "";
        String username = CaseStats.getConfig().get("user");
        String database = CaseStats.getConfig().get("database");
        String password = CaseStats.getConfig().get("password");
        tablename = CaseStats.getConfig().get("tableprefix");

        CaseStats.LOGGER.log(Level.INFO, "Loading driver...");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            url = "jdbc:mysql://" + CaseStats.getConfig().get("host") + ":3306/" + database + "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
            CaseStats.LOGGER.log(Level.INFO, "Driver loaded!");
        } catch (ClassNotFoundException e) {
            CaseStats.LOGGER.log(Level.ERROR, "Cannot find the driver in the classpath!", e);
        }

        CaseStats.LOGGER.log(Level.INFO, "Connecting database...");
        try {
            this.connection = new SQLConnection(url, database, username, password);
            CaseStats.LOGGER.log(Level.INFO, "Database connected.");
        } catch (SQLException e) {
            CaseStats.LOGGER.log(Level.ERROR, "Could not initialize database", e);
        }

        try {
            createTablesIfNotExist();
        } catch (SQLException e) {
            CaseStats.LOGGER.log(Level.WARN, "Keine Rechte neue Tabellen anzulegen!");
        }

        insertEntryQuery = "INSERT INTO " + tablename + "_entries (timestamp, x, y, z, player, info) VALUES (?, ?, ?, ?, ?, ?)";
        selectNextEntryQuery = "SELECT * FROM " + tablename + "_entries ORDER BY `id`";
        selectEntryQuery = "SELECT * FROM " + tablename + "_entries WHERE `id` = ?";
        deleteEntryQuery = "DELETE FROM " + tablename + "_entries WHERE `id` = ?";
        countEntriesQuary = "SELECT COUNT(*) FROM " + tablename + "_entries";

        insertUUIDPlayerQuery = "INSERT INTO " + tablename + "_UUIDCache (uuid, name) VALUES (?, ?) ON DUPLICATE KEY UPDATE uuid = ?, name = ?";
        selectUUIDPlayerQuery = "SELECT * FROM " + tablename + "_UUIDCache WHERE uuid = ?";
        updateCurrentEditQuery = "UPDATE " + tablename + "_UUIDCache SET `current` = ? WHERE `uuid` = ?";
        selectCurrentEditsQuery = "SELECT `current` FROM " + tablename + "_UUIDCache";
        selectCurrentEditQuery = "SELECT `current` FROM " + tablename + "_UUIDCache WHERE `uuid` = ?";

        incrementCreatedStatsQuery = "INSERT INTO " + tablename + "_Stats (`uuid`, `created`, `finished`) VALUES (?, 1, 0) ON DUPLICATE KEY UPDATE `created` = `created` + 1";
        incrementFinishedStatsQuery = "INSERT INTO " + tablename + "_Stats (`uuid`, `created`, `finished`) VALUES (?, 0, 1) ON DUPLICATE KEY UPDATE `finished` = `finished` + 1";
        selectStatsQuary = "SELECT * FROM " + tablename + "_Stats";
    }

    private void createTablesIfNotExist() throws SQLException {
        if (connection == null) {
            return;
        }
        this.connection.runCommands((connection, sqlConnection) -> {
            Statement smt = connection.createStatement();
            smt.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablename + "_entries (" +
                    "`id` int(11) NOT NULL AUTO_INCREMENT," +
                    "`timestamp` BIGINT," +
                    "`x` INT," +
                    "`y` INT," +
                    "`z` INT," +
                    "`player` varchar(36)," +
                    "`info` TEXT," +
                    "PRIMARY KEY (`id`)) ENGINE=innodb");
            smt.close();
            return null;
        });

        this.connection.runCommands((connection, sqlConnection) -> {
            Statement smt = connection.createStatement();
            smt.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablename + "_UUIDCache (" +
                    "`uuid` varchar(36)," +
                    "`name` VARCHAR(16)," +
                    "`current` BIGINT," +
                    "PRIMARY KEY (`uuid`)) ENGINE=innodb");
            smt.close();
            return null;
        });

        this.connection.runCommands((connection, sqlConnection) -> {
            Statement smt = connection.createStatement();
            smt.executeUpdate("CREATE TABLE IF NOT EXISTS " + tablename + "_Stats (" +
                    "`uuid` varchar(36)," +
                    "`created` BIGINT," +
                    "`finished` BIGINT," +
                    "PRIMARY KEY (`uuid`)) ENGINE=innodb");
            smt.close();
            return null;
        });
    }

    /*public void addEntry(WischmobEntry entry) throws SQLException {
        if (connection == null) {
            return;
        }
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(insertEntryQuery);
            statement.setLong(1, entry.getTimestamp());
            statement.setInt(2, entry.getEntryLocation().getX());
            statement.setInt(3, entry.getEntryLocation().getY());
            statement.setInt(4, entry.getEntryLocation().getZ());
            statement.setString(5, entry.getPlayer().toString());
            statement.setString(6, entry.getInfo());
            statement.executeUpdate();
            return null;
        });
    }

    public WischmobEntry getNextWischmob() throws SQLException {
        if (connection == null) {
            return null;
        }
        return this.connection.runCommands((connection, sqlConnection) -> {
            PlayerEntity player = MinecraftClient.getInstance().player;
            PreparedStatement statement = sqlConnection.getOrCreateStatement(selectNextEntryQuery);
            ResultSet rs = statement.executeQuery();
            int currentEdit = getCurrentEdit(player.getUuid());
            if (currentEdit > 0) {
                WischmobEntry currentEntry = getWischmob(currentEdit);
                if (currentEntry != null) {
                    return currentEntry;
                }
            }

            List<Integer> editList = getCurrentEdits();
            while (rs.next()) {
                int id = rs.getInt("id");
                if (!editList.contains(id)) {
                    long timestamp = rs.getLong("timestamp");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    UUID playerUUID = UUID.fromString(rs.getString("player"));
                    String info = rs.getString("info");
                    return new WischmobEntry(id, timestamp, x, y, z, playerUUID, info);
                }
            }
            return null;
        });
    }

    public WischmobEntry getWischmob(int id) throws SQLException {
        if (connection == null) {
            return null;
        }
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(selectEntryQuery);
            statement.setInt(1, id);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                    int entryID = rs.getInt("id");
                    long timestamp = rs.getLong("timestamp");
                    int x = rs.getInt("x");
                    int y = rs.getInt("y");
                    int z = rs.getInt("z");
                    UUID playerUUID = UUID.fromString(rs.getString("player"));
                    String info = rs.getString("info");
                    return new WischmobEntry(entryID, timestamp, x, y, z, playerUUID, info);
            }
            return null;
        });
    }*/

    public void deleteEntry(int id) throws SQLException {
        if (connection == null) {
            return;
        }
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(deleteEntryQuery);
            statement.setInt(1, id);
            statement.executeUpdate();
            return null;
        });
    }

    public void insertUUIDPlayer(CachedPlayer cp) throws SQLException {
        if (connection == null) {
            return;
        }
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(insertUUIDPlayerQuery);
            statement.setString(1, cp.getUuid().toString());
            statement.setString(2, cp.getName());
            statement.setString(3, cp.getUuid().toString());
            statement.setString(4, cp.getName());
            statement.executeUpdate();
            return null;
        });
    }

    public CachedPlayer getCachedPlayerFromUUID(UUID uuid) throws SQLException {
        if (connection == null) {
            return null;
        }
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(selectUUIDPlayerQuery);
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                UUID cachedUUID = UUID.fromString(rs.getString("uuid"));
                String cachedName = rs.getString("name");
                return new CachedPlayer(cachedUUID, cachedName);
            }
            return null;
        });
    }

    public void incrementCreatedStats(UUID uuid) throws SQLException {
        if (connection == null) {
            return;
        }
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(incrementCreatedStatsQuery);
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
            return null;
        });
    }

    public void incrementFinishedStats(UUID uuid) throws SQLException {
        if (connection == null) {
            return;
        }
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(incrementFinishedStatsQuery);
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
            return null;
        });
    }

    public void updateCurrentEdit(UUID uuid, int id) throws SQLException {
        if (connection == null) {
            return;
        }
        this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement smt = sqlConnection.getOrCreateStatement(updateCurrentEditQuery);
            smt.setInt(1, id);
            smt.setString(2, uuid.toString());
            smt.executeUpdate();
            return null;
        });
    }

    public List<Integer> getCurrentEdits() throws SQLException {
        List<Integer> editsList = new ArrayList<>();
        if (connection == null) {
            return editsList;
        }
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(selectCurrentEditsQuery);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("current");
                if (id != 0) {
                    editsList.add(id);
                }
            }
            return editsList;
        });
    }

    public Integer getCurrentEdit(UUID uuid) throws SQLException {
        if (connection == null) {
            return -1;
        }
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(selectCurrentEditQuery);
            statement.setString(1, uuid.toString());
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("current");
                if (id != 0) {
                    return id;
                }
            }
            return -1;
        });
    }

    /*public ArrayList<StatsEntry> getStats() throws SQLException {
        ArrayList<StatsEntry> statsEntries = new ArrayList<>();
        if (connection == null) {
            return statsEntries;
        }
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(selectStatsQuary);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString(1));
                int created = rs.getInt(2);
                int finished = rs.getInt(3);
                statsEntries.add(new StatsEntry(uuid, created, finished));
            }
            return statsEntries;
        });
    }*/

    public Integer countEntries() throws SQLException {
        if (connection == null) {
            return 0;
        }
        return this.connection.runCommands((connection, sqlConnection) -> {
            PreparedStatement statement = sqlConnection.getOrCreateStatement(countEntriesQuary);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        });
    }

}
