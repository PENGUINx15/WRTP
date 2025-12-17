package me.penguinx13.wrtp.cache;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class PointGenerator {

    public static List<CachedBlockData> generateDistributedPoints(String worldName,
                                                                  int minX, int maxX,
                                                                  int minZ, int maxZ,
                                                                  int count,
                                                                  int minDistance) {

        Random random = new Random();
        Map<Long, List<int[]>> grid = new HashMap<>();
        List<CachedBlockData> result = new ArrayList<>(count);

        int maxAttempts = count * 40;
        int attempts = 0;

        while (result.size() < count && attempts < maxAttempts) {
            attempts++;

            int x = random.nextInt(maxX - minX + 1) + minX;
            int z = random.nextInt(maxZ - minZ + 1) + minZ;

            int cellX = x / minDistance;
            int cellZ = z / minDistance;

            boolean ok = true;

            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    long key = (((long) (cellX + dx)) << 32) ^ (cellZ + dz);
                    List<int[]> list = grid.get(key);
                    if (list == null) continue;

                    for (int[] p : list) {
                        int diffX = p[0] - x;
                        int diffZ = p[1] - z;
                        if (diffX * diffX + diffZ * diffZ < minDistance * minDistance) {
                            ok = false;
                            break;
                        }
                    }

                    if (!ok) break;
                }
            }

            if (!ok) continue;

            long key = (((long) cellX) << 32) ^ cellZ;
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(new int[]{x, z});
            result.add(new CachedBlockData(worldName, x, z, 0, "UNKNOWN", "UNKNOWN"));
        }

        return result;
    }
    public static CachedBlockData randomPoint(
            String worldName,
            int minX, int maxX,
            int minZ, int maxZ
    ) {
        ThreadLocalRandom r = ThreadLocalRandom.current();

        return new CachedBlockData(
                worldName,
                r.nextInt(minX, maxX + 1),
                r.nextInt(minZ, maxZ + 1),
                0,
                "UNKNOWN",
                "UNKNOWN"
        );
    }


}
