package yadi.samuraiai.ai.navigation.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.shapes.VoxelShape;
import yadi.samuraiai.ai.navigation.graph.NavEnvironment;
import yadi.samuraiai.ai.navigation.graph.NavPos;
import yadi.samuraiai.ai.navigation.graph.NavWorldView;
import yadi.samuraiai.ai.navigation.terrain.BlockProfile;
import yadi.samuraiai.ai.navigation.terrain.Material;

/**
 * Reads a {@link ServerLevel} for the navigation core. Chunks are read with {@code getChunkNow}, so
 * navigation never forces a chunk to load and an unloaded chunk reads as {@link BlockProfile#UNLOADED}.
 * Block classification is cached per {@link BlockState}. Server-thread only.
 */
public final class MinecraftNavWorldView implements NavWorldView {
    private static final Map<BlockState, BlockProfile> PROFILES = new ConcurrentHashMap<>();
    private final ServerLevel level;
    private final String dimension;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public MinecraftNavWorldView(ServerLevel level) {
        this.level = level;
        this.dimension = level.dimension().location().toString();
    }

    public ServerLevel level() { return level; }
    @Override public String dimension() { return dimension; }
    @Override public int minY() { return level.getMinBuildHeight(); }
    @Override public int maxY() { return level.getMaxBuildHeight(); }
    @Override public boolean isLoaded(int chunkX, int chunkZ) { return level.getChunkSource().getChunkNow(chunkX, chunkZ) != null; }
    @Override public long gameTick() { return level.getGameTime(); }

    @Override public BlockProfile profile(int x, int y, int z) {
        if (y < level.getMinBuildHeight()) return BlockProfile.VOID;
        if (y >= level.getMaxBuildHeight()) return BlockProfile.AIR;
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) return BlockProfile.UNLOADED;
        return classify(chunk.getBlockState(cursor.set(x, y, z)));
    }

    @Override public NavEnvironment environment(NavPos at) {
        String biome = level.getBiome(new BlockPos(at.x(), at.y(), at.z())).unwrapKey().map(key -> key.location().toString()).orElse("unknown");
        return new NavEnvironment(level.getDayTime(), level.isRaining(), level.isThundering(), biome);
    }

    /** Maps a block state to what navigation cares about. Public so tests and the debug tools can reuse it. */
    public static BlockProfile classify(BlockState state) {
        BlockProfile known = PROFILES.get(state);
        if (known != null) return known;
        BlockProfile computed;
        try { computed = compute(state); }
        catch (RuntimeException error) { computed = new BlockProfile(Material.OTHER, true, 1.0D, false, false, false, true); }
        PROFILES.put(state, computed);
        return computed;
    }

    /** Called when data packs or mods change block behavior at runtime; normally never needed. */
    public static void clearProfileCache() { PROFILES.clear(); }

    private static BlockProfile compute(BlockState state) {
        Block block = state.getBlock();
        if (state.isAir()) return BlockProfile.AIR;
        if (block instanceof DoorBlock) return block == Blocks.IRON_DOOR ? BlockProfile.DOOR_IRON : BlockProfile.DOOR_WOOD;
        if (block instanceof FenceGateBlock) return BlockProfile.DOOR_WOOD;
        if (state.is(BlockTags.CLIMBABLE)) return BlockProfile.LADDER;
        VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        boolean collides = !shape.isEmpty();
        double top = collides ? shape.max(Direction.Axis.Y) : 0.0D;
        if (state.getFluidState().is(FluidTags.LAVA) && !collides) return BlockProfile.LAVA;
        if (!collides && state.getFluidState().is(FluidTags.WATER)) return BlockProfile.WATER;
        if (block instanceof BaseFireBlock) return BlockProfile.FIRE;
        if (block == Blocks.COBWEB || block == Blocks.POWDER_SNOW || block == Blocks.SWEET_BERRY_BUSH || block == Blocks.WITHER_ROSE)
            return new BlockProfile(Material.DAMAGING, collides, top, false, false, false, true);
        if (block instanceof CactusBlock || block == Blocks.MAGMA_BLOCK || block instanceof CampfireBlock)
            return new BlockProfile(Material.DAMAGING, true, Math.max(top, 1.0D), false, false, false, true);
        if (!collides) return BlockProfile.AIR;
        Material material;
        if (block instanceof StairBlock) material = Material.STAIRS;
        else if (block instanceof SlabBlock) material = Material.SLAB;
        else if (block instanceof FenceBlock || block instanceof WallBlock) material = Material.FENCE;
        else if (block instanceof LeavesBlock) material = Material.LEAVES;
        else if (state.is(BlockTags.ICE)) material = Material.ICE;
        else if (block == Blocks.MUD || block == Blocks.SOUL_SAND) material = Material.MUD;
        else if (state.is(BlockTags.SAND)) material = Material.SAND;
        else if (block instanceof SnowLayerBlock || block == Blocks.SNOW_BLOCK) material = Material.SNOW;
        else if (block == Blocks.DIRT_PATH || state.is(BlockTags.PLANKS) || state.is(BlockTags.STONE_BRICKS) || block == Blocks.COBBLESTONE)
            material = Material.ROAD;
        else if (top >= 0.99D) material = Material.GROUND;
        else material = Material.OTHER;
        return new BlockProfile(material, true, top, false, false, false, true);
    }
}
