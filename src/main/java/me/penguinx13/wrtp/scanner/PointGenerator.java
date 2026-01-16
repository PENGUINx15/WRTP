package me.penguinx13.wrtp.scanner;

import me.penguinx13.wrtp.WRTP;
import me.penguinx13.wrtp.cache.CachedBlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PointGenerator {

    private final int minDistance;
    private final int range;

    private final Random random = new Random();
    private final List<Point> points = new ArrayList<>();

    public PointGenerator() {
        this.minDistance = WRTP.getInstance()
                .getConfigManager()
                .getConfig("config.yml")
                .getInt("cacheSett.pointBetween", 128);

        this.range = WRTP.getInstance()
                .getConfigManager()
                .getConfig("config.yml")
                .getInt("cacheSett.range", 30000);
    }
    public Point generateNext() {
        for (int i = 0; i < 1000; i++) {
            int x = random.nextInt(range * 2) - range;
            int z = random.nextInt(range * 2) - range;

            Point candidate = new Point(x, 0, z);
            if (isFarEnough(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public void loadExisting(List<CachedBlockData> blocks) {
        for (CachedBlockData b : blocks) {
            points.add(new Point(b.x, 0, b.z));
        }
    }

    private boolean isFarEnough(Point candidate) {
        for (Point p : points) {
            double dx = p.x - candidate.x;
            double dz = p.z - candidate.z;
            if ((dx * dx + dz * dz) < (minDistance * minDistance)) {
                return false;
            }
        }
        return true;
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public static class Point {
        public final int x, y, z;

        public Point(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
