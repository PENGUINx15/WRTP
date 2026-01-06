package me.penguinx13.wrtp.scanner;

public record ScanStatus(
        boolean running,
        boolean paused,
        int points,
        int target,
        double pointsPerSecond
) {}