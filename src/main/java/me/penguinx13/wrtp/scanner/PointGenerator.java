package me.penguinx13.wrtp.scanner;

import me.penguinx13.wrtp.cache.CachedBlockData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PointGenerator {

    private static final int TARGET_POINTS = 10_000;
    private static final int MIN_DISTANCE = 128;
    private static final int RANGE = 30_000;

    private final Random random = new Random();
    private final List<Point> points = new ArrayList<>();

    public Point generateNext() {
        for (int i = 0; i < 1000; i++) {
            int x = random.nextInt(RANGE * 2) - RANGE;
            int z = random.nextInt(RANGE * 2) - RANGE;

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
            if ((dx * dx + dz * dz) < (MIN_DISTANCE * MIN_DISTANCE)) {
                return false;
            }
        }
        return true;
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public boolean isFinished() {
        return points.size() >= TARGET_POINTS;
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
