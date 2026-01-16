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

    public CachedBlockData getRandomPoint(
            int worldId,
            int minRadius,
            int maxRadius,
            List<String> biomes
    ) {
        StringBuilder sql = new StringBuilder("""
        SELECT worldId, x, z, highestY, biomeId, blockId
        FROM rtp_blocks
        WHERE worldId = ?
          AND x BETWEEN ? AND ?
          AND z BETWEEN ? AND ?
          AND NOT (x BETWEEN ? AND ? AND z BETWEEN ? AND ?)
    """);

        if (biomes != null && !biomes.isEmpty()) {
            sql.append(" AND biomeId IN (");
            for (int i = 0; i < biomes.size(); i++) {
                sql.append("?");
                if (i < biomes.size() - 1) sql.append(",");
            }
            sql.append(")");
        }

        sql.append(" ORDER BY RANDOM() LIMIT 1");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int idx = 1;

            ps.setInt(idx++, worldId);

            // внешний квадрат
            ps.setInt(idx++, -maxRadius);
            ps.setInt(idx++,  maxRadius);
            ps.setInt(idx++, -maxRadius);
            ps.setInt(idx++,  maxRadius);

            // внутренняя дырка
            ps.setInt(idx++, -minRadius);
            ps.setInt(idx++,  minRadius);
            ps.setInt(idx++, -minRadius);
            ps.setInt(idx++,  minRadius);

            if (biomes != null && !biomes.isEmpty()) {
                for (String biome : biomes) {
                    ps.setString(idx++, biome);
                }
            }

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
}