package yadi.samuraiai.ai.cognition.world;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;

/**
 * A cheap look at the surroundings that tells an NPC what kind of place it has found: a cave (under a roof, below sea level),
 * a river (water at hand), a mountain (high ground) or a forest (many logs and leaves). Plain ground returns nothing: not every
 * step is a discovery. Reads a few dozen blocks, so it is only called from the exploration scan.
 */
final class PlaceClassifier {
    record Found(String category, String label) { }

    private PlaceClassifier() { }

    static Optional<Found> classify(ServerLevel level, BlockPos pos) {
        try {
            for (int dy = -2; dy <= 0; dy++)
                for (BlockPos p : new BlockPos[] {pos.offset(0, dy, 0), pos.offset(2, dy, 0), pos.offset(-2, dy, 0), pos.offset(0, dy, 2), pos.offset(0, dy, -2)})
                    if (level.getFluidState(p).is(FluidTags.WATER)) return Optional.of(new Found("RIVER", "Agua"));
            boolean sky = level.canSeeSky(pos.above());
            if (!sky && pos.getY() < level.getSeaLevel()) return Optional.of(new Found("CAVE", "Cueva"));
            if (pos.getY() >= 110) return Optional.of(new Found("MOUNTAIN", "Montaña"));
            int trees = 0;
            for (int dx = -4; dx <= 4; dx += 2)
                for (int dz = -4; dz <= 4; dz += 2)
                    for (int dy = 0; dy <= 4; dy += 2) {
                        var state = level.getBlockState(pos.offset(dx, dy, dz));
                        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) trees++;
                    }
            if (trees >= 8) return Optional.of(new Found("FOREST", "Bosque"));
        } catch (RuntimeException ignored) { /* an unloaded chunk or a bad position: nothing found */ }
        return Optional.empty();
    }
}
