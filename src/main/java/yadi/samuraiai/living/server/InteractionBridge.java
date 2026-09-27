package yadi.samuraiai.living.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CartographyTableBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.sim.LivingWorld;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.buildings.BuildingKind;
import yadi.samuraiai.living.village.citizens.Citizen;
import yadi.samuraiai.living.village.engine.VillageSettings;
import yadi.samuraiai.living.village.homes.HomeRecord;
import yadi.samuraiai.living.village.life.VillageEventKind;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.world.environment.EnvironmentInteractionEngine;
import yadi.samuraiai.living.world.environment.InteractionPoint;
import yadi.samuraiai.living.world.streaming.SimulationLevel;
import yadi.samuraiai.world.ServerWorlds;

/**
 * The physical side of the Environment Interaction Engine, at full detail only. It scans each usable building of a village
 * (one building per call, only inside loaded chunks, a bounded box) for the things people use — beds, doors, campfires, crops,
 * workstations, storage, bells — and registers them as interaction points; the beds found in a house become its residents'
 * real beds. Then, for villages a player is standing in, it carries out the engine's orders: campfires are lit at dusk (and
 * kept lit on cold or festive nights) and put out late at night, and a farmer at work makes one crop of the farm grow one
 * stage. Points are not persisted: the world is their source of truth and they are scanned again after a restart.
 */
final class InteractionBridge {
    private static final int MAX_RADIUS = 8, BELOW = 3, ABOVE = 4, MAX_POINTS = 64;
    private static final long RESCAN_TICKS = 12000;

    private final Map<UUID, Long> scannedAt = new HashMap<>();
    private long step, scans, pointsFound, bedsPlaced, firesChanged, cropsTended;

    long scans() { return scans; }
    long pointsFound() { return pointsFound; }
    long bedsPlaced() { return bedsPlaced; }
    long firesChanged() { return firesChanged; }
    long cropsTended() { return cropsTended; }

    void reset() { scannedAt.clear(); step = scans = pointsFound = bedsPlaced = firesChanged = cropsTended = 0; }

    // ------------------------------------------------------------------ scanning

    /** Scans the building most in need of it (never scanned, or longest ago), if its area is loaded. */
    void scanNext(LivingWorld w, long tick) {
        Building next = null;
        long oldest = Long.MAX_VALUE;
        for (Village v : w.villages().villages())
            for (Building b : v.buildings().values()) {
                if (!b.usable()) continue;
                long at = scannedAt.getOrDefault(b.id(), Long.MIN_VALUE);
                if (at != Long.MIN_VALUE && tick - at < RESCAN_TICKS) continue;
                if (at < oldest) { oldest = at; next = b; }
            }
        if (next == null) return;
        ServerLevel level = ServerWorlds.level(next.dimension()).orElse(null);
        BlockPos centre = new BlockPos(next.x(), next.y(), next.z());
        int r = (int) Math.max(2, Math.min(MAX_RADIUS, Math.ceil(next.radius())));
        if (level == null || !level.isLoaded(centre.offset(-r, 0, -r)) || !level.isLoaded(centre.offset(r, 0, r))) return;
        scannedAt.put(next.id(), tick);
        scan(w, level, next, centre, r);
    }

    private void scan(LivingWorld w, ServerLevel level, Building b, BlockPos centre, int r) {
        scans++;
        EnvironmentInteractionEngine engine = w.world().interactions();
        engine.forgetBuilding(b.village(), b.id());
        String dimension = b.dimension();
        List<BlockPos> beds = new ArrayList<>();
        int found = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = -BELOW; dy <= ABOVE && found < MAX_POINTS; dy++)
            for (int dx = -r; dx <= r && found < MAX_POINTS; dx++)
                for (int dz = -r; dz <= r && found < MAX_POINTS; dz++) {
                    pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    InteractionPoint.Kind kind = kindOf(level.getBlockState(pos));
                    if (kind == null) continue;
                    engine.register(new InteractionPoint(kind, dimension, pos.getX(), pos.getY(), pos.getZ(), b.id(), b.village()));
                    if (kind == InteractionPoint.Kind.BED) beds.add(pos.immutable());
                    found++;
                }
        pointsFound += found;
        if (b.kind() == BuildingKind.HOUSE && !beds.isEmpty()) placeBeds(w, b, beds);
    }

    static InteractionPoint.Kind kindOf(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof BedBlock) return state.getValue(BedBlock.PART) == BedPart.HEAD ? InteractionPoint.Kind.BED : null;
        if (block instanceof DoorBlock) return state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? InteractionPoint.Kind.DOOR : null;
        if (block instanceof CampfireBlock) return InteractionPoint.Kind.FIRE;
        if (block instanceof CropBlock) return InteractionPoint.Kind.CROP;
        if (block instanceof CraftingTableBlock || block instanceof AnvilBlock || block instanceof AbstractFurnaceBlock || block instanceof SmithingTableBlock
                || block instanceof GrindstoneBlock || block instanceof StonecutterBlock || block instanceof LoomBlock || block instanceof CartographyTableBlock)
            return InteractionPoint.Kind.WORKSTATION;
        if (block instanceof ChestBlock || block instanceof BarrelBlock) return InteractionPoint.Kind.STORAGE;
        if (block instanceof BellBlock) return InteractionPoint.Kind.SHRINE;
        return null;
    }

    /** The residents of a house sleep in its real beds, in order; a resident without a bed keeps the planned spot. */
    private void placeBeds(LivingWorld w, Building house, List<BlockPos> beds) {
        Village v = w.villages().village(house.village()).orElse(null);
        if (v == null) return;
        int i = 0;
        for (HomeRecord home : List.copyOf(v.homes().values())) {
            if (!house.id().equals(home.house())) continue;
            if (i >= beds.size()) break;
            BlockPos bed = beds.get(i++);
            double x = bed.getX() + 0.5D, y = bed.getY(), z = bed.getZ() + 0.5D;
            if (Math.abs(home.bedX() - x) < 1e-6 && Math.abs(home.bedY() - y) < 1e-6 && Math.abs(home.bedZ() - z) < 1e-6) continue;
            home.bed(x, y, z);
            v.markDirty();
            bedsPlaced++;
            LivingService.getInstance().bedMoved(home.citizen());
        }
    }

    // ------------------------------------------------------------------ acting

    /** Carries out the interaction orders of every village in a region a player is standing in. */
    void act(LivingWorld w, Function<Village, String> cellOf) {
        CalendarEngine cal = w.calendar();
        VillageSettings vs = VillageSettings.current();
        long now = cal.now();
        for (Village v : w.villages().villages()) {
            var region = w.world().region(v.region()).orElse(null);
            if (region == null || region.level() != SimulationLevel.FULL) continue;
            ServerLevel level = ServerWorlds.level(v.dimension()).orElse(null);
            if (level == null) continue;
            boolean cold = cal.temperature(cellOf.apply(v), v.y()) < vs.coldThreshold();
            boolean festive = v.events().stream().anyMatch(e -> e.kind() == VillageEventKind.FESTIVAL && e.end() > now);
            List<UUID> farmers = new ArrayList<>();
            for (Citizen c : w.villages().citizensOf(v.id()))
                if (c.present() && c.embodied() && "farmer".equals(c.profession()) && "WORK".equals(w.villages().plannedRoutine(c.id()))) farmers.add(c.id());
            for (EnvironmentInteractionEngine.Order order : w.world().interactions().plan(v.id(), cal.today().phase(), cold, festive, farmers, step++))
                carryOut(level, order);
        }
    }

    private void carryOut(ServerLevel level, EnvironmentInteractionEngine.Order order) {
        InteractionPoint p = order.point();
        BlockPos pos = new BlockPos(p.x(), p.y(), p.z());
        if (!level.isLoaded(pos)) return;
        BlockState state = level.getBlockState(pos);
        switch (order.kind()) {
            case LIGHT_FIRE, DOUSE_FIRE -> {
                if (!(state.getBlock() instanceof CampfireBlock)) return;
                boolean lit = order.kind() == EnvironmentInteractionEngine.OrderKind.LIGHT_FIRE;
                if (lit && state.getValue(CampfireBlock.WATERLOGGED)) return;
                if (state.getValue(CampfireBlock.LIT) == lit) return;
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, lit), Block.UPDATE_ALL);
                firesChanged++;
            }
            case TEND_CROP -> {
                if (!(state.getBlock() instanceof CropBlock)) return;
                for (var property : state.getProperties()) {
                    if (!(property instanceof IntegerProperty age) || !"age".equals(age.getName())) continue;
                    int current = state.getValue(age), max = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(current);
                    if (current >= max) return;
                    level.setBlock(pos, state.setValue(age, current + 1), Block.UPDATE_CLIENTS);
                    cropsTended++;
                    return;
                }
            }
            default -> { }
        }
    }
}
