package yadi.samuraiai.ai.scheduler.world;

import com.mojang.math.Vector3f;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.ai.scheduler.engine.NpcSchedule;
import yadi.samuraiai.ai.scheduler.zone.Zone;
import yadi.samuraiai.world.ServerWorlds;

/**
 * Server-side scheduler overlay drawn with dust particles only the requesting operator sees: zones as rings (colour by kind,
 * red when on alert), each NPC's current routine target (gold, with a marker over the NPC), group leaders (white) and their
 * followers' slots (cyan).
 */
final class SchedulerDebugRenderer {
    private static final DustParticleOptions TARGET = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.1F), 1.4F);
    private static final DustParticleOptions LEADER = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.6F);
    private static final DustParticleOptions SLOT = new DustParticleOptions(new Vector3f(0.2F, 0.9F, 0.9F), 1.2F);
    private static final DustParticleOptions ALERT = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.2F);
    private static final DustParticleOptions[] KINDS = {
            new DustParticleOptions(new Vector3f(0.3F, 0.6F, 1.0F), 0.9F), new DustParticleOptions(new Vector3f(0.6F, 0.9F, 0.3F), 0.9F),
            new DustParticleOptions(new Vector3f(0.9F, 0.6F, 0.2F), 0.9F), new DustParticleOptions(new Vector3f(0.8F, 0.3F, 0.9F), 0.9F),
            new DustParticleOptions(new Vector3f(0.3F, 0.9F, 0.6F), 0.9F)};
    private static final double RANGE_SQUARED = 64.0D * 64.0D;

    private SchedulerDebugRenderer() { }

    static void render(SchedulerService service, Set<UUID> viewers) {
        long now = service.currentTick();
        for (UUID viewerId : viewers) {
            var maybe = ServerWorlds.playerById(viewerId);
            if (maybe.isEmpty() || !(maybe.get().level instanceof ServerLevel level)) continue;
            ServerPlayer viewer = maybe.get();
            String dimension = level.dimension().location().toString();
            for (Zone zone : service.scheduler().zoneRegistry().all()) {
                if (!zone.dimension().equals(dimension) || viewer.distanceToSqr(zone.x(), zone.y(), zone.z()) > RANGE_SQUARED * 4) continue;
                var colour = service.scheduler().zoneAlerted(zone.id(), now) ? ALERT : KINDS[zone.kind().ordinal() % KINDS.length];
                int points = (int) Math.max(12, Math.min(48, zone.radius() * 4));
                for (int i = 0; i < points; i++) {
                    double angle = 2.0D * Math.PI * i / points;
                    dot(level, viewer, colour, zone.x() + Math.cos(angle) * zone.radius(), zone.y() + 0.3D, zone.z() + Math.sin(angle) * zone.radius());
                }
            }
            for (UUID id : service.scheduler().tracked()) {
                NpcSchedule st = service.scheduler().schedule(id).orElse(null);
                if (st == null || st.light() == null || !st.light().dimension().equals(dimension) || st.current() == null) continue;
                if (viewer.distanceToSqr(st.light().x(), st.light().y(), st.light().z()) > RANGE_SQUARED) continue;
                dot(level, viewer, TARGET, st.light().x(), st.light().y() + 2.4D, st.light().z());
                var place = st.current().place();
                if (place != null && place.dimension().equals(dimension)) {
                    for (int k = 0; k < 3; k++) dot(level, viewer, TARGET, place.x(), place.y() + 0.3D + k * 0.5D, place.z());
                    boolean follower = st.advice() != null && st.advice().formation() != null;
                    if (follower) dot(level, viewer, SLOT, place.x(), place.y() + 1.8D, place.z());
                }
                if (st.advice() != null && st.advice().role() == yadi.samuraiai.ai.scheduler.group.GroupRole.LEADER)
                    dot(level, viewer, LEADER, st.light().x(), st.light().y() + 2.8D, st.light().z());
            }
        }
    }

    private static void dot(ServerLevel level, ServerPlayer viewer, DustParticleOptions colour, double x, double y, double z) {
        level.sendParticles(viewer, colour, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
