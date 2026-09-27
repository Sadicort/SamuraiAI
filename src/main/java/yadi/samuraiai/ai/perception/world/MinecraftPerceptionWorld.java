package yadi.samuraiai.ai.perception.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import yadi.samuraiai.ai.perception.engine.BlockInterest;
import yadi.samuraiai.ai.perception.engine.EntityClass;
import yadi.samuraiai.ai.perception.engine.PerceptionWorld;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.environment.EnvironmentSnapshot;
import yadi.samuraiai.ai.perception.environment.WeatherState;

/**
 * Reads a {@link ServerLevel} for the perception core. Chunks are read with {@code getChunkNow} (perception never loads a
 * chunk), block opacity and interest are classified once per {@link BlockState}, and entity queries are bucketed into
 * cells and cached for the current tick so many NPCs share one world query. Velocity is derived from position changes
 * between observations, because a server-side player's delta movement is not reliable. Server-thread only.
 */
public final class MinecraftPerceptionWorld implements PerceptionWorld {
    private static final Map<BlockState, Double> OPACITY = new ConcurrentHashMap<>();
    private static final Map<BlockState, BlockInterest> INTEREST = new ConcurrentHashMap<>();
    private static final BlockInterest NONE = BlockInterest.CUSTOM;
    private static final int CELL = 16;

    private final ServerLevel level;
    private final String dimension;
    private final Supplier<Set<UUID>> npcEntities;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
    private final Map<Long, List<SensedEntity>> buckets = new HashMap<>();
    private final Map<UUID, double[]> motion = new HashMap<>();
    private long cachedTick = Long.MIN_VALUE;

    public MinecraftPerceptionWorld(ServerLevel level, Supplier<Set<UUID>> npcEntities) {
        this.level = level;
        this.dimension = level.dimension().location().toString();
        this.npcEntities = npcEntities;
    }

    public ServerLevel level() { return level; }
    @Override public String dimension() { return dimension; }
    @Override public long gameTick() { return level.getGameTime(); }
    @Override public boolean isLoaded(int chunkX, int chunkZ) { return level.getChunkSource().getChunkNow(chunkX, chunkZ) != null; }

    // ------------------------------------------------------------------ entities

    @Override public List<SensedEntity> entitiesNear(double x, double y, double z, double radius, UUID exclude) {
        long tick = level.getGameTime();
        if (tick != cachedTick) { buckets.clear(); cachedTick = tick; if (tick % 200 == 0) motion.values().removeIf(m -> tick - m[3] > 200); }
        int radiusBucket = (int) Math.ceil(radius / 8.0D) * 8;
        int cellX = Math.floorDiv((int) Math.floor(x), CELL), cellZ = Math.floorDiv((int) Math.floor(z), CELL);
        long key = ((long) cellX << 40) ^ ((long) cellZ << 16) ^ radiusBucket;
        List<SensedEntity> bucket = buckets.computeIfAbsent(key, k -> query(cellX, cellZ, radiusBucket, y, tick));
        List<SensedEntity> result = new ArrayList<>();
        double radiusSquared = radius * radius;
        for (SensedEntity e : bucket) {
            if (e.id().equals(exclude)) continue;
            double dx = e.x() - x, dy = e.y() - y, dz = e.z() - z;
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) result.add(e);
        }
        return result;
    }

    private List<SensedEntity> query(int cellX, int cellZ, int radius, double y, long tick) {
        double minX = cellX * CELL - radius, minZ = cellZ * CELL - radius;
        AABB box = new AABB(minX, y - radius / 2.0D - 4.0D, minZ, minX + CELL + 2 * radius, y + radius / 2.0D + 6.0D, minZ + CELL + 2 * radius);
        Set<UUID> npcs = npcEntities.get();
        List<SensedEntity> found = new ArrayList<>();
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box, e -> e.isAlive() && !e.isSpectator())) {
            EntityClass kind = classify(entity, npcs);
            if (kind == EntityClass.UNKNOWN && !(entity instanceof LivingEntity)) continue;
            double[] v = velocity(entity, tick);
            found.add(new SensedEntity(entity.getUUID(), kind, entity.getName().getString(), entity.getX(), entity.getY(), entity.getZ(), v[0], v[1], v[2],
                    entity.getBbWidth(), entity.getBbHeight(), entity.isCrouching(), entity.isSprinting(), entity.isOnGround(), entity.isInvisible()));
        }
        return found;
    }

    private static EntityClass classify(Entity e, Set<UUID> npcs) {
        if (e instanceof ServerPlayer) return EntityClass.PLAYER;
        if (npcs.contains(e.getUUID())) return EntityClass.NPC;
        if (e instanceof Enemy) return EntityClass.HOSTILE;
        if (e instanceof AbstractVillager) return EntityClass.VILLAGER;
        if (e instanceof Animal || e instanceof AmbientCreature) return EntityClass.ANIMAL;
        if (e instanceof ItemEntity) return EntityClass.OBJECT;
        if (e instanceof Projectile) return EntityClass.PROJECTILE;
        if (e instanceof Boat || e instanceof AbstractMinecart) return EntityClass.VEHICLE;
        return EntityClass.UNKNOWN;
    }

    /** Per-tick velocity from the change of position since the last observation; stable within a tick. */
    private double[] velocity(Entity e, long tick) {
        double[] last = motion.get(e.getUUID());
        if (last != null && last[3] == tick) return new double[]{last[4], last[5], last[6]};
        double vx = 0, vy = 0, vz = 0;
        if (last != null) {
            double dt = tick - last[3];
            if (dt > 0 && dt <= 20) { vx = (e.getX() - last[0]) / dt; vy = (e.getY() - last[1]) / dt; vz = (e.getZ() - last[2]) / dt; }
        }
        motion.put(e.getUUID(), new double[]{e.getX(), e.getY(), e.getZ(), tick, vx, vy, vz});
        return new double[]{vx, vy, vz};
    }

    // ------------------------------------------------------------------ blocks

    private BlockState state(int x, int y, int z) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return Blocks.AIR.defaultBlockState();
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        return chunk == null ? Blocks.STONE.defaultBlockState() : chunk.getBlockState(cursor.set(x, y, z));
    }

    @Override public double opacity(int x, int y, int z) { return OPACITY.computeIfAbsent(state(x, y, z), MinecraftPerceptionWorld::computeOpacity); }

    /** How much a block blocks sight: solid blocks and closed doors fully, leaves and glass partly, plants and air not at all. */
    private static double computeOpacity(BlockState state) {
        if (state.isAir()) return 0.0D;
        Block block = state.getBlock();
        if (block instanceof DoorBlock) return state.getValue(DoorBlock.OPEN) ? 0.05D : 1.0D;
        if (block instanceof FenceGateBlock) return state.getValue(FenceGateBlock.OPEN) ? 0.0D : 0.4D;
        if (block instanceof TrapDoorBlock) return state.getValue(TrapDoorBlock.OPEN) ? 0.05D : 0.9D;
        if (block instanceof LiquidBlock) return state.getFluidState().is(FluidTags.LAVA) ? 0.6D : 0.3D;
        if (state.is(BlockTags.LEAVES)) return 0.55D;
        if (state.is(BlockTags.IMPERMEABLE)) return 0.12D;
        if (block == Blocks.COBWEB) return 0.5D;
        if (state.canOcclude()) return 1.0D;
        return state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty() ? 0.0D : 0.35D;
    }

    @Override public int lightLevel(int x, int y, int z) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight() || !isLoaded(x >> 4, z >> 4)) return 15;
        return level.getMaxLocalRawBrightness(cursor.set(x, y, z));
    }

    @Override public BlockInterest interest(int x, int y, int z) {
        BlockInterest known = INTEREST.computeIfAbsent(state(x, y, z), MinecraftPerceptionWorld::computeInterest);
        return known == NONE ? null : known;
    }

    private static BlockInterest computeInterest(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof DoorBlock) return state.getValue(DoorBlock.OPEN) ? BlockInterest.DOOR_OPEN : BlockInterest.DOOR_CLOSED;
        if (block instanceof FenceGateBlock) return state.getValue(FenceGateBlock.OPEN) ? BlockInterest.DOOR_OPEN : BlockInterest.DOOR_CLOSED;
        if (block instanceof BaseFireBlock) return BlockInterest.FIRE;
        if (block instanceof LiquidBlock) return state.getFluidState().is(FluidTags.LAVA) ? BlockInterest.LAVA : NONE;
        if (block instanceof ChestBlock || block instanceof BarrelBlock || block instanceof EnderChestBlock) return BlockInterest.CHEST;
        if (block instanceof BedBlock) return BlockInterest.BED;
        if (block instanceof BellBlock) return BlockInterest.BELL;
        return NONE;
    }

    /** Called when data packs or mods change block behavior at runtime; normally never needed. */
    public static void clearBlockCaches() { OPACITY.clear(); INTEREST.clear(); }

    // ------------------------------------------------------------------ environment

    @Override public EnvironmentSnapshot environment(double x, double y, double z) {
        BlockPos pos = new BlockPos(x, y, z);
        var biome = level.getBiome(pos);
        Biome value = biome.value();
        String name = biome.unwrapKey().map(key -> key.location().toString()).orElse("unknown");
        WeatherState weather = WeatherState.CLEAR;
        if (level.isRaining()) {
            Biome.Precipitation precipitation = value.getPrecipitation();
            if (level.isThundering() && precipitation == Biome.Precipitation.RAIN) weather = WeatherState.STORM;
            else if (precipitation == Biome.Precipitation.SNOW) weather = WeatherState.SNOW;
            else if (precipitation == Biome.Precipitation.RAIN) weather = WeatherState.RAIN;
        }
        boolean nearLava = false;
        for (int dx = -2; dx <= 2 && !nearLava; dx++) for (int dz = -2; dz <= 2 && !nearLava; dz++) for (int dy = -1; dy <= 1; dy++)
            if (level.getFluidState(pos.offset(dx, dy, dz)).is(FluidTags.LAVA)) { nearLava = true; break; }
        return new EnvironmentSnapshot(dimension, name, pos.getY(), value.getBaseTemperature(), weather, level.getDayTime(), 15,
                level.getFluidState(pos).is(FluidTags.WATER), nearLava, level.canSeeSky(pos));
    }
}
