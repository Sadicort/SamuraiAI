package yadi.samuraiai.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/**
 * The ground every physical test stands on. The test world is natural terrain, so a bare floor laid on it can sit above caves
 * and lava, which perception scans within a few blocks and navigation treats as hazards: a test then passes or fails by where
 * it happens to be placed. Each arena therefore replaces a generous box around it (solid stone below the floor, clear air above)
 * and keeps its chunks loaded.
 */
final class TestTerrain {
    /** How far beyond the arena the box reaches: more than perception's block scan radius. */
    private static final int MARGIN = 10;
    private static final int DEPTH_BELOW = 10;
    private static final int HEIGHT_ABOVE = 8;

    private TestTerrain() { }

    /** Flat stone floor at {@code origin.y}, solid below it and air above it, over the arena plus a margin. */
    static void flatten(ServerLevel level, BlockPos origin, int width, int depth) {
        for (int cx = (origin.getX() - MARGIN) >> 4; cx <= (origin.getX() + width + MARGIN) >> 4; cx++)
            for (int cz = (origin.getZ() - MARGIN) >> 4; cz <= (origin.getZ() + depth + MARGIN) >> 4; cz++) level.setChunkForced(cx, cz, true);
        var stone = Blocks.STONE.defaultBlockState();
        var air = Blocks.AIR.defaultBlockState();
        for (int x = -MARGIN; x < width + MARGIN; x++) for (int z = -MARGIN; z < depth + MARGIN; z++) {
            for (int y = -DEPTH_BELOW; y <= 0; y++) level.setBlock(origin.offset(x, y, z), stone, 2);
            for (int y = 1; y <= HEIGHT_ABOVE; y++) level.setBlock(origin.offset(x, y, z), air, 2);
        }
    }
}
