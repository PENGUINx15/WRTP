package me.penguinx13.wrtp.scanner;

import me.penguinx13.wapi.Managers.ConfigManager;
import me.penguinx13.wrtp.WRTP;
import me.penguinx13.wrtp.cache.CachedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChunkScanner {

    private final WRTP plugin;
    private final World world;
    private final ConfigManager configManager;
    private final PointGenerator pointGenerator = new PointGenerator();
    private final ScanTask scanTask;

    private final int TARGET_POINTS;

    private final AtomicInteger validPoints = new AtomicInteger(0);

    private boolean running = false;
    private boolean paused = false;
    private static final int MAX_IN_FLIGHT = 8;
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private long startTime;

    public ChunkScanner(WRTP plugin, World world, ConfigManager configManager) {
        this.plugin = plugin;
        this.world = world;
        this.configManager = configManager;
        this.scanTask = new ScanTask(world);
        this.TARGET_POINTS = configManager.getConfig("config.yml").getInt("cacheSett.pointCount", 10_000);
    }

    /* ===================== PUBLIC API ===================== */
    public void start() {
        int worldId = CachedBlockData.getOrCreateWorldId(world.getName());
        
        List<CachedBlockData> existing = WRTP.getDatabase().loadAllPoints(worldId);
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
        if (!running || paused) return;

        double minTps = configManager.getConfig("config.yml")
                .getDouble("cacheSett.minTPS", 18.0);

        if (getTPS() < minTps) {
            Bukkit.getScheduler().runTaskLater(plugin, this::scanPoint, 5L);
            return;
        }

        while (inFlight.get() < MAX_IN_FLIGHT && validPoints.get() < TARGET_POINTS) {
            PointGenerator.Point point = pointGenerator.generateNext();
            if (point == null) break;

            inFlight.incrementAndGet();

            scanTask.scan(
                    point,
                    data -> {
                        WRTP.getDatabase().insertBlock(data);
                        pointGenerator.addPoint(point);

                        int count = validPoints.incrementAndGet();
                        if (count % 500 == 0 || count == TARGET_POINTS) {
                            logProgress(count);
                        }
                    },
                    inFlight::decrementAndGet
            );
        }

        Bukkit.getScheduler().runTaskLater(plugin, this::scanPoint, 1L);
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