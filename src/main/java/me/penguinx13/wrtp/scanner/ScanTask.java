package me.penguinx13.wrtp.scanner;

import io.papermc.lib.PaperLib;
import me.penguinx13.wrtp.WRTP;
import me.penguinx13.wrtp.cache.CachedBlockData;
import org.bukkit.*;

import java.util.function.Consumer;

public final class ScanTask {

    private final WRTP plugin;
    private final World world;

    public ScanTask(WRTP plugin, World world) {
        this.plugin = plugin;
        this.world = world;
    }

    public void scan(
            PointGenerator.Point point,
            Consumer<CachedBlockData> onValid,
            Runnable onFinish
    ) {
        int cx = point.x >> 4;
        int cz = point.z >> 4;

        PaperLib.getChunkAtAsync(world, cx, cz, false).thenAccept(chunk -> {
            try {
                if (chunk == null) return;

                ChunkSnapshot snap = chunk.getChunkSnapshot(true, true, false);
                int y = world.getHighestBlockYAt(point.x, point.z);
                Material block = snap.getBlockType(point.x & 15, y - 1, point.z & 15);

                if (!isSafe(block)) return;

                CachedBlockData data = new CachedBlockData(
                        world.getName(),
                        point.x,
                        point.z,
                        y,
                        snap.getBiome(point.x & 15, y - 1, point.z & 15).name(),
                        block.name()
                );

                onValid.accept(data);

            } finally {
                onFinish.run();
            }
        });
    }

    private boolean isSafe(Material mat) {
        return mat.isSolid()
                && mat != Material.LAVA
                && mat != Material.WATER
                && mat != Material.MAGMA_BLOCK
                && mat != Material.CACTUS;
    }
}
