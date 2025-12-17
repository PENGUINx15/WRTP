package me.penguinx13.wrtp.cache;

import io.papermc.lib.PaperLib;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ChunkScanner {

    private final SQLiteManager db;
    private final World world;
    private final int minX, maxX, minZ, maxZ;
    private static final int MAX_CONCURRENT_TASKS = 6;
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private static final AtomicInteger processedPoints = new AtomicInteger(0);

    private boolean wasPaused = false;
    private boolean isFinished = false;

    private final Queue<CachedBlockData> pointQueue = new ConcurrentLinkedQueue<>();
    private final List<CachedBlockData> locationBuffer =
            Collections.synchronizedList(new ArrayList<>());

    public ChunkScanner(SQLiteManager db, World world, int minX, int maxX, int minZ, int maxZ) {
        this.db = db;
        this.world = world;
        int worldId = CachedBlockData.getOrCreateWorldId(world.getName());
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;

        List<CachedBlockData> points = db.loadUnscannedPoints(worldId);

        if (points.isEmpty()) {
            points = PointGenerator.generateDistributedPoints(world.getName(), minX, maxX, minZ, maxZ, 10000, 128);
            db.savePoints(points);
            Bukkit.getLogger().info("[WRTP] Generated and saved " + points.size() + " RTP points");
        } else {
            Bukkit.getLogger().info("[WRTP] Loaded " + points.size() + " RTP points from DB");
        }

        Collections.shuffle(points);
        pointQueue.addAll(points);
    }

    public void startScanning() {
        new BukkitRunnable() {
            @Override
            public void run() {

                if (isFinished && activeTasks.get() == 0) {
                    flushBuffer();
                    Bukkit.getLogger().info(
                            "[WRTP] Finished scanning " + processedPoints.get() + " points"
                    );
                    cancel();
                    return;
                }

                if (shouldPause()) return;

                while (activeTasks.get() < MAX_CONCURRENT_TASKS && !pointQueue.isEmpty()) {
                    CachedBlockData point = pointQueue.poll();
                    if (point == null) break;

                    int x = point.x;
                    int z = point.z;

                    activeTasks.incrementAndGet();
                    int chunkX = x >> 4;
                    int chunkZ = z >> 4;

                    PaperLib.getChunkAtAsync(world, chunkX, chunkZ, true)
                            .thenAccept(chunk -> {
                                ChunkSnapshot snapshot =
                                        chunk.getChunkSnapshot(true, true, false);

                                int y = snapshot.getHighestBlockYAt(x & 15, z & 15);
                                Material mat = snapshot.getBlockType(x & 15, y, z & 15);
                                if (mat.isAir()) {
                                    mat = snapshot.getBlockType(x & 15, y - 1, z & 15);
                                }

                                if (isSafeBlock(mat)) {
                                    Biome biome =
                                            snapshot.getBiome(x & 15, y, z & 15);

                                    locationBuffer.add(
                                            new CachedBlockData(
                                                    world.getName(),
                                                    x, z, y,
                                                    biome.name(),
                                                    mat.name()
                                            )
                                    );

                                    db.markPointScanned(point);
                                } else {
                                    pointQueue.add(
                                            PointGenerator.randomPoint(
                                                    world.getName(), minX, maxX, minZ, maxZ
                                            )
                                    );
                                }
                            })
                            .exceptionally(ex -> {
                                ex.printStackTrace();
                                return null;
                            })
                            .thenRun(() -> {
                                processedPoints.incrementAndGet();
                                activeTasks.decrementAndGet();

                                if (processedPoints.get() >= 10000) {
                                    isFinished = true;
                                }
                            });
                }
            }
        }.runTaskTimerAsynchronously(
                Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("WRTP")),
                20L, 10L
        );
    }

    private synchronized void flushBuffer() {
        if (locationBuffer.isEmpty()) return;

        List<CachedBlockData> batch = new ArrayList<>(locationBuffer);
        locationBuffer.clear();
        db.insertBlocks(batch);
    }

    private boolean shouldPause() {
        double tps = Bukkit.getServer().getTPS()[0];
        long freeRam = Runtime.getRuntime().freeMemory() / (1024 * 1024);
        int online = Bukkit.getOnlinePlayers().size();

        boolean pause = tps < 19.5 || freeRam < 500 || online > 10;

        if (pause && !wasPaused) {
            Bukkit.getLogger().info(
                    "[WRTP] Paused (TPS=" + tps + ", RAM=" + freeRam + "MB)"
            );
            wasPaused = true;
        } else if (!pause && wasPaused) {
            Bukkit.getLogger().info("[WRTP] Resumed");
            wasPaused = false;
        }

        return pause;
    }
    public static void logProgress() {
        double progress = (processedPoints.get() * 100.0) / 10000;

        Bukkit.getLogger().info(
                String.format(
                        "[WRTP] Progress: %.2f%% (%d/%d points)",
                        progress, processedPoints.get(), 10000
                )
        );
    }

    private boolean isSafeBlock(Material mat) {
        return mat != null
                && mat.isSolid()
                && mat != Material.LAVA
                && mat != Material.WATER
                && !mat.isAir();
    }
}
