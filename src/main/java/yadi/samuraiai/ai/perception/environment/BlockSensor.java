package yadi.samuraiai.ai.perception.environment;

import yadi.samuraiai.ai.perception.awareness.SuspicionSource;
import yadi.samuraiai.ai.perception.engine.BlockInterest;
import yadi.samuraiai.ai.perception.engine.PerceptionState;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.sensors.Sensor;
import yadi.samuraiai.ai.perception.sensors.SensorContext;
import yadi.samuraiai.ai.perception.sensors.SensorType;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/**
 * Notices relevant blocks (doors, chests, torches, fire, lava, water, beds, bells) and, above all, what changes about
 * them: a door that opens, a block that disappears, a fire that starts. Scanning is incremental: each pass checks a slice
 * of the surrounding cube (a cursor walks it), plus any position the world reported as changed, so cost is bounded and the
 * whole neighbourhood is still covered after a few scans.
 */
public final class BlockSensor implements Sensor {
    @Override public SensorType type() { return SensorType.BLOCK; }

    @Override public void scan(SensorContext ctx) {
        var eye = ctx.perceiver();
        var state = ctx.state();
        var world = ctx.world();
        int radius = (int) Math.floor(ctx.settings().blockScanRadius());
        int side = radius * 2 + 1, total = side * side * side;
        int cx = (int) Math.floor(eye.x()), cy = (int) Math.floor(eye.y()) + 1, cz = (int) Math.floor(eye.z());
        Long dirty;
        int checked = 0;
        while ((dirty = state.dirtyBlocks.poll()) != null && checked < 64) {
            int x = PerceptionState.unpackX(dirty), y = PerceptionState.unpackY(dirty), z = PerceptionState.unpackZ(dirty);
            if (Math.abs(x - cx) <= radius && Math.abs(y - cy) <= radius && Math.abs(z - cz) <= radius) { observe(ctx, x, y, z); checked++; }
        }
        int budget = Math.min(ctx.settings().blockScanCellsPerScan(), total);
        for (int i = 0; i < budget; i++) {
            int index = Math.floorMod(state.blockCursor++, total);
            int x = cx + index % side - radius, y = cy + (index / side) % side - radius, z = cz + index / (side * side) - radius;
            if (world.isLoaded(x >> 4, z >> 4)) observe(ctx, x, y, z);
        }
        // Forget block memory that has drifted far outside the current neighbourhood.
        if (state.blockMemory.size() > 4096) state.blockMemory.clear();
    }

    private void observe(SensorContext ctx, int x, int y, int z) {
        var state = ctx.state();
        long key = PerceptionState.pack(x, y, z);
        BlockInterest now = ctx.world().interest(x, y, z), before = state.blockMemory.get(key);
        if (now == before) return;
        if (now == null) state.blockMemory.remove(key); else state.blockMemory.put(key, now);
        double cx = x + 0.5D, cz = z + 0.5D;
        long tick = ctx.tick();
        if (before == null) {
            // First sight of a block: a hazard is news, anything else is just scenery worth remembering.
            if (now.hazard()) hazard(ctx, now, cx, y + 0.5D, cz);
            else if (now == BlockInterest.CHEST || now == BlockInterest.BELL)
                state.memory.remember(MemoryKind.INTEREST, "block:" + key, null, now.name().toLowerCase(), cx, y + 0.5D, cz, 0, 0, 0.35D, tick, "OBJECT");
            return;
        }
        if (before == BlockInterest.DOOR_CLOSED && now == BlockInterest.DOOR_OPEN) {
            ctx.out().accept(Stimulus.at(StimulusType.ENVIRONMENT, StimulusCategory.DOOR, null, "a door opened", cx, y + 0.5D, cz, 0.7D, tick, 100).withDetail("door opened"));
            ctx.out().suspect(SuspicionSource.DOOR_OPENED, 12.0D);
            state.memory.remember(MemoryKind.ENVIRONMENTAL, "door:" + key, null, "door opened", cx, y + 0.5D, cz, 0, 0, 0.6D, tick, "EVENT");
        } else if (before == BlockInterest.DOOR_OPEN && now == BlockInterest.DOOR_CLOSED) {
            state.memory.remember(MemoryKind.ENVIRONMENTAL, "door:" + key, null, "door closed", cx, y + 0.5D, cz, 0, 0, 0.3D, tick, "EVENT");
        } else if (now == null) {
            ctx.out().accept(Stimulus.at(StimulusType.ENVIRONMENT, StimulusCategory.BLOCK_CHANGE, null, before.name().toLowerCase() + " gone", cx, y + 0.5D, cz, 0.6D, tick, 100)
                    .withDetail("block removed"));
            ctx.out().suspect(SuspicionSource.BLOCK_BROKEN, 10.0D);
            state.memory.remember(MemoryKind.ENVIRONMENTAL, "block:" + key, null, before.name().toLowerCase() + " gone", cx, y + 0.5D, cz, 0, 0, 0.5D, tick, "EVENT");
        } else if (now.hazard()) hazard(ctx, now, cx, y + 0.5D, cz);
    }

    private void hazard(SensorContext ctx, BlockInterest kind, double x, double y, double z) {
        ctx.out().accept(Stimulus.at(StimulusType.ENVIRONMENT, kind.category(), null, kind.name().toLowerCase(), x, y, z, 0.7D, ctx.tick(), 200).withDetail("hazard " + kind));
        ctx.state().memory.remember(MemoryKind.DANGER, "hazard:" + PerceptionState.pack((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)), null,
                kind.name().toLowerCase(), x, y, z, 0, 0, 0.7D, ctx.tick(), "HAZARD");
    }
}
