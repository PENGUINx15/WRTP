package me.penguinx13.wrtp.scanner;

import me.penguinx13.wapi.Managers.ConfigManager;
import me.penguinx13.wrtp.WRTP;
import me.penguinx13.wrtp.cache.CachedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChunkScanner {

    private static final int TARGET_POINTS = 10_000;

    private final WRTP plugin;
    private final World world;
    private final ConfigManager configManager;
    private final PointGenerator pointGenerator = new PointGenerator();
    private final ScanTask scanTask;

    private final AtomicInteger validPoints = new AtomicInteger(0);

    private boolean running = false;
    private boolean paused = false;
    private boolean scanning = false;
    private long startTime;

    public ChunkScanner(WRTP plugin, World world, ConfigManager configManager) {
        this.plugin = plugin;
        this.world = world;
        this.configManager = configManager;
        this.scanTask = new ScanTask(plugin, world);
    }

    /* ===================== PUBLIC API ===================== */
    public void start() {
        int worldId = CachedBlockData.getOrCreateWorldId(world.getName());
        
        List<CachedBlockData> existing = plugin.getDatabase().loadAllPoints(worldId);
        pointGenerator.loadExisting(existing);
        validPoints.set(existing.size());

        if (existing.size() >= TARGET_POINTS) {
            Bukkit.getLogger().info("[WRTP] ✔ Точки уже сгенерированы (" + existing.size() + ")");
            running = false;
            return;
        }

        running = true;
        paused = false;
        startTime = System.currentTimeMillis();

        Bukkit.getLogger().info("[WRTP] ▶ Продолжение сканирования. Уже есть: " + existing.size());

        scanPoint();
    }

    public void stop() {
        running = false;
        paused = false;
        Bukkit.getLogger().info("[WRTP] ■ ChunkScanner остановлен");
    }

    public void pause() {
        paused = true;
        Bukkit.getLogger().info("[WRTP] ⏸ Сканирование поставлено на паузу");
    }

    public void resume() {
        paused = false;
        Bukkit.getLogger().info("[WRTP] ▶ Сканирование возобновлено");
        scanPoint();
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

    private void scanPoint() {
        if (!running || paused || scanning) return;

        double minTps = configManager.getConfig("config.yml")
                .getDouble("cacheSett.minTPS", 18.0);

        if (getTPS() < minTps) {
            paused = true;
            Bukkit.getScheduler().runTaskLater(plugin, this::scanPoint, 20L);
            return;
        }

        paused = false;

        if (validPoints.get() >= TARGET_POINTS) {
            finish();
            return;
        }

        PointGenerator.Point point = pointGenerator.generateNext();
        if (point == null) {
            Bukkit.getScheduler().runTaskLater(plugin, this::scanPoint, 1L);
            return;
        }

        scanning = true;

        scanTask.scan(
                point,
                data -> {
                    plugin.getDatabase().insertBlock(data);
                    pointGenerator.addPoint(point);

                    int count = validPoints.incrementAndGet();
                    if (count % 500 == 0 || count == TARGET_POINTS) {
                        logProgress(count);
                    }
                },
                () -> {
                    scanning = false;
                    Bukkit.getScheduler().runTask(plugin, this::scanPoint);
                }
        );
    }

    private void finish() {
        running = false;
        Bukkit.getLogger().info("[WRTP] ✔ Сканирование завершено. Точек: " + validPoints.get());
    }

    /* ===================== UTILS ===================== */

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