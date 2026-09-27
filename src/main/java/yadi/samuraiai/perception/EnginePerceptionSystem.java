package yadi.samuraiai.perception;

import java.util.List;
import yadi.samuraiai.ai.perception.engine.EntityClass;
import yadi.samuraiai.ai.perception.engine.PerceptionSnapshot;
import yadi.samuraiai.ai.perception.world.PerceptionService;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Adapts the perception engine to the {@link PerceptionSystem} contract the brain already consumes: the world context an NPC
 * gets contains only the targets it currently <em>sees</em> (not everything nearby) plus the whole {@link PerceptionSnapshot}
 * for consumers that want awareness, threat, suspicion or an investigation target. Until the engine has produced a first
 * snapshot for an NPC it falls back to the legacy nearest-player perception, so a brand-new NPC is never blind.
 */
public final class EnginePerceptionSystem implements PerceptionSystem {
    private final PerceptionSystem fallback;

    public EnginePerceptionSystem(PerceptionSystem fallback) { this.fallback = fallback; }

    @Override public WorldContext perceive(NPCRuntime runtime) {
        PerceptionSnapshot snapshot = PerceptionService.getInstance().snapshot(runtime.getId()).orElse(null);
        if (snapshot == null) return fallback.perceive(runtime);
        String dimension = runtime.getInstance().getLocation() == null ? "minecraft:overworld" : runtime.getInstance().getLocation().dimensionKey();
        List<PerceivedEntity> seen = snapshot.seenTargets().stream()
                .map(t -> new PerceivedEntity(t.id(), t.name(), t.distance(), t.kind() == EntityClass.PLAYER, new SpawnLocation(dimension, t.x(), t.y(), t.z(), 0.0F)))
                .toList();
        return new WorldContext(seen, snapshot.environment().dayTime(), snapshot);
    }
}
