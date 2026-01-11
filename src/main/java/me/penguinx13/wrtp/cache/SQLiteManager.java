package me.penguinx13.wrtp.cache;

import org.bukkit.Bukkit;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
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
                    biomeId TEXT NOT NULL,
                    blockId TEXT NOT NULL,
                    PRIMARY KEY(worldId, x, z)
                );
            """);
        }
    }

    public void insertBlock(CachedBlockData b) {
        String sql = """
            INSERT OR IGNORE INTO rtp_blocks
            (worldId, x, z, highestY, biomeId, blockId)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, b.worldId);
            ps.setInt(2, b.x);
            ps.setInt(3, b.z);
            ps.setInt(4, b.highestY);
            ps.setString(5, b.biomeName);
            ps.setString(6, b.blockName);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getPointCount(int worldId) {
        String sql = "SELECT COUNT(*) FROM rtp_blocks WHERE worldId=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, worldId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    public List<CachedBlockData> loadAllPoints(int worldId) {
        List<CachedBlockData> list = new ArrayList<>();
        String sql = "SELECT x, z, highestY, biomeId, blockId FROM rtp_blocks WHERE worldId=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, worldId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new CachedBlockData(
                        worldId,
                        rs.getInt("x"),
                        rs.getInt("z"),
                        rs.getInt("highestY"),
                        rs.getString("biomeId"),
                        rs.getString("blockId")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
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
                        rs.getString("biomeId"),
                        rs.getString("blockId")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public CachedBlockData getRandomPointInRange(int worldId, int minX, int maxX, int minZ, int maxZ) {
        String sql = """
        SELECT worldId, x, z, highestY, biomeId, blockId
        FROM rtp_blocks
        WHERE worldId=? AND x BETWEEN ? AND ? AND z BETWEEN ? AND ?
        ORDER BY RANDOM()
        LIMIT 1
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, worldId);
            ps.setInt(2, minX);
            ps.setInt(3, maxX);
            ps.setInt(4, minZ);
            ps.setInt(5, maxZ);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new CachedBlockData(
                        rs.getInt("worldId"),
                        rs.getInt("x"),
                        rs.getInt("z"),
                        rs.getInt("highestY"),
                        rs.getString("biomeId"),
                        rs.getString("blockId")
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
