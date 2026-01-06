package me.penguinx13.wrtp.scanner;

import me.penguinx13.wapi.Managers.ConfigManager;
import me.penguinx13.wrtp.WRTP;
import me.penguinx13.wrtp.cache.CachedBlockData;
import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChunkScanner {

    private static final int TARGET_POINTS = 10_000;
    private static final int SCANS_PER_TICK = 1;

    private final WRTP plugin;
    private final World world;
    private final ConfigManager configManager;
    private final PointGenerator pointGenerator = new PointGenerator();

    private final AtomicInteger validPoints = new AtomicInteger(0);
    private BukkitRunnable task;

    private boolean running = false;
    private boolean paused = false;
    private long startTime;

    public ChunkScanner(WRTP plugin, World world, ConfigManager configManager) {
        this.plugin = plugin;
        this.world = world;
        this.configManager = configManager;
    }

    /* ===================== PUBLIC API ===================== */

    public void start() {
        if (running) return;

        running = true;
        paused = false;
        startTime = System.currentTimeMillis();

        Bukkit.getLogger().info("[WRTP] ▶ ChunkScanner запущен для мира " + world.getName());

        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        };

        task.runTaskTimer(plugin, 1L, 1L);
    }

    public void stop() {
        if (task != null) task.cancel();
        running = false;
        paused = false;
        Bukkit.getLogger().info("[WRTP] ■ ChunkScanner остановлен");
    }

    public void pause() {
        paused = true;
        Bukkit.getLogger().info("[WRTP] ⏸ Сканирование поставлено на паузу вручную");
    }

    public void resume() {
        paused = false;
        Bukkit.getLogger().info("[WRTP] ▶ Сканирование возобновлено вручную");
    }

    public boolean isRunning() {
        return running;
    }

    public ScanStatus getStatus() {
        double elapsed = (System.currentTimeMillis() - startTime) / 1000.0;
        double speed = elapsed > 0 ? validPoints.get() / elapsed : 0;

        return new ScanStatus(
                running,
                paused,
                validPoints.get(),
                TARGET_POINTS,
                speed
        );
    }

    /* ===================== CORE ===================== */

    private void tick() {
        if (!running) return;

        double tps = getTPS();
        double minTps = configManager.getConfig("config.yml").getDouble("cacheSett.minTPS");

        // Авто-пауза при низком TPS
        if (tps < minTps) {
            if (!paused) {
                paused = true;
                Bukkit.getLogger().warning(
                        "[WRTP] ⏸ Авто-пауза (TPS: " + String.format("%.2f", tps) + ")"
                );
            }
            return;
        }

        if (paused) {
            paused = false;
            Bukkit.getLogger().info(
                    "[WRTP] ▶ Авто-возобновление (TPS: " + String.format("%.2f", tps) + ")"
            );
        }

        for (int i = 0; i < SCANS_PER_TICK; i++) {
            if (pointGenerator.isFinished()) {
                finish();
                return;
            }
            scanNextPoint();
        }
    }

    /**
     * Сканируем точку из генератора
     */
    private void scanNextPoint() {
        PointGenerator.Point point = pointGenerator.generateNext();
        if (point == null) return; // не удалось сгенерировать подходящую точку

        int y = world.getHighestBlockYAt(point.x, point.z);
        Material ground = world.getBlockAt(point.x, y - 1, point.z).getType();

        if (isSafe(ground)) {
            PointGenerator.Point valid = new PointGenerator.Point(point.x, y, point.z);
            pointGenerator.addPoint(valid);

            int count = validPoints.incrementAndGet();
            onValidPoint(valid.x, valid.y, valid.z);

            if (count % 500 == 0 || count == TARGET_POINTS) {
                logProgress(count);
            }
        }
    }

    private void finish() {
        running = false;
        if (task != null) task.cancel();

        Bukkit.getLogger().info(
                "[WRTP] ✔ Сканирование завершено. Точек: " + validPoints.get()
        );
    }

    /* ===================== HOOK ===================== */

    private void onValidPoint(int x, int y, int z) {
        CachedBlockData data = new CachedBlockData(
                world.getEnvironment().getId(),
                x, z, y,
                world.getBiome(x, z).ordinal(),
                world.getBlockAt(x, y - 1, z).getType().getId()
        );

        plugin.getDatabase().insertBlocks(List.of(data));

        pointGenerator.addPoint(new PointGenerator.Point(x, y, z));
    }


    /* ===================== UTILS ===================== */

    private boolean isSafe(Material mat) {
        return mat.isSolid()
                && mat != Material.LAVA
                && mat != Material.WATER
                && mat != Material.MAGMA_BLOCK
                && mat != Material.CACTUS;
    }

    private void logProgress(int count) {
        double percent = (count / (double) TARGET_POINTS) * 100.0;
        Bukkit.getLogger().info(String.format(
                "[WRTP] ⏳ Прогресс: %d / %d (%.2f%%)",
                count, TARGET_POINTS, percent
        ));
    }

    private double getTPS() {
        try {
            return Bukkit.getTPS()[0];
        } catch (Throwable ignored) {
            return 20.0;
        }
    }
}
