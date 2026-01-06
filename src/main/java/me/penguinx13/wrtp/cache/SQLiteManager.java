package me.penguinx13.wrtp.cache;

import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.List;

public class SQLiteManager {

    private Connection connection;

    public SQLiteManager(File dataFolder) {
        try {
            if (!dataFolder.exists()) dataFolder.mkdirs();
            File dbFile = new File(dataFolder, "wrtp.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            createTables();
            Bukkit.getLogger().info("[WRTP] SQLite initialized.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS rtp_blocks (
                    worldId INTEGER NOT NULL,
                    x INTEGER NOT NULL,
                    z INTEGER NOT NULL,
                    highestY INTEGER NOT NULL,
                    biomeId INTEGER NOT NULL,
                    blockId INTEGER NOT NULL,
                    PRIMARY KEY(worldId, x, z)
                );
            """);
        }
    }

    public void insertBlocks(List<CachedBlockData> blocks) {
        String sql = """
            INSERT OR REPLACE INTO rtp_blocks(worldId, x, z, highestY, biomeId, blockId)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            for (CachedBlockData b : blocks) {
                ps.setInt(1, b.worldId);
                ps.setInt(2, b.x);
                ps.setInt(3, b.z);
                ps.setInt(4, b.highestY);
                ps.setInt(5, b.biomeId);
                ps.setInt(6, b.blockId);
                ps.addBatch();
            }
            ps.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            e.printStackTrace();
        } finally {
            try { connection.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public CachedBlockData getCachedBlock(int worldId, int x, int z) {
        String sql = "SELECT highestY, biomeId, blockId FROM rtp_blocks WHERE worldId=? AND x=? AND z=? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, worldId);
            ps.setInt(2, x);
            ps.setInt(3, z);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new CachedBlockData(
                        worldId,
                        x,
                        z,
                        rs.getInt("highestY"),
                        rs.getInt("biomeId"),
                        rs.getInt("blockId")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException ignored) {}
    }
}
