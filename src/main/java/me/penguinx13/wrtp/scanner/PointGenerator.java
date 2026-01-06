package me.penguinx13.wrtp.scanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PointGenerator {

    private static final int TARGET_POINTS = 10_000;
    private static final int MIN_DISTANCE = 128;
    private static final int RANGE = 30_000; // квадрат мира

    private final Random random = new Random();
    private final List<Point> points = new ArrayList<>();

    public Point generateNext() {
        for (int i = 0; i < 1000; i++) { // максимум попыток
            int x = random.nextInt(RANGE * 2) - RANGE;
            int z = random.nextInt(RANGE * 2) - RANGE;
            int y = 0; // y будет определяться на ChunkScanner

            Point candidate = new Point(x, y, z);
            if (isFarEnough(candidate)) {
                return candidate;
            }
        }
        return null; // не удалось найти подходящую точку
    }

    private boolean isFarEnough(Point candidate) {
        for (Point p : points) {
            double dx = p.x - candidate.x;
            double dz = p.z - candidate.z;
            if (Math.sqrt(dx * dx + dz * dz) < MIN_DISTANCE) {
                return false;
            }
        }
        return true;
    }

    public void addPoint(Point point) {
        points.add(point);
    }

    public List<Point> getPoints() {
        return points;
    }

    public int getCount() {
        return points.size();
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
