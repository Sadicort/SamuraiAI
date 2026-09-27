package yadi.samuraiai.ai.perception.world;

import com.mojang.math.Vector3f;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.ai.perception.awareness.AwarenessLevel;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.PerceptionState;
import yadi.samuraiai.ai.perception.vision.VisibilityState;
import yadi.samuraiai.world.ServerWorlds;

/**
 * Server-side perception overlay drawn with dust particles only the requesting operator sees: the field of view edges,
 * targets (green = seen, orange = obstructed, grey = memory only), heard sounds with their uncertainty ring (cyan),
 * threats (red), the investigation target (yellow) and a coloured marker over each NPC for its awareness level.
 */
final class PerceptionDebugRenderer {
    private static final DustParticleOptions FOV = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 0.3F), 0.6F);
    private static final DustParticleOptions SEEN = new DustParticleOptions(new Vector3f(0.1F, 0.9F, 0.2F), 1.2F);
    private static final DustParticleOptions OBSTRUCTED = new DustParticleOptions(new Vector3f(1.0F, 0.55F, 0.0F), 1.0F);
    private static final DustParticleOptions MEMORY = new DustParticleOptions(new Vector3f(0.6F, 0.6F, 0.6F), 0.9F);
    private static final DustParticleOptions SOUND = new DustParticleOptions(new Vector3f(0.1F, 0.9F, 0.9F), 1.0F);
    private static final DustParticleOptions THREAT = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.4F);
    private static final DustParticleOptions INVESTIGATE = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.0F), 1.6F);
    private static final double RANGE_SQUARED = 48.0D * 48.0D;

    private PerceptionDebugRenderer() { }

    static void render(PerceptionService service, Set<UUID> viewers) {
        PerceptionSettings s = PerceptionSettings.current();
        for (UUID viewerId : viewers) {
            var maybe = ServerWorlds.playerById(viewerId);
            if (maybe.isEmpty() || !(maybe.get().level instanceof ServerLevel level)) continue;
            ServerPlayer viewer = maybe.get();
            String dimension = level.dimension().location().toString();
            for (var npcId : service.trackedIds()) {
                PerceptionState st = service.state(npcId).orElse(null);
                if (st == null || !st.dimension.equals(dimension) || st.snapshot == null) continue;
                if (viewer.distanceToSqr(st.lastX, st.lastY, st.lastZ) > RANGE_SQUARED) continue;
                drawNpc(level, viewer, st, s);
            }
        }
    }

    private static void drawNpc(ServerLevel level, ServerPlayer viewer, PerceptionState st, PerceptionSettings s) {
        double eyeY = st.lastY + 1.6D;
        double reach = Math.min(s.visionFar(), 14.0D);
        for (double side : new double[]{-1.0D, 1.0D}) {
            double yaw = Math.toRadians(st.lastYaw + side * s.visionHorizontalFov() / 2.0D);
            for (double d = 2.0D; d <= reach; d += 2.0D) dot(level, viewer, FOV, st.lastX - Math.sin(yaw) * d, eyeY, st.lastZ + Math.cos(yaw) * d);
        }
        dot(level, viewer, awarenessColour(st.snapshot.awareness()), st.lastX, st.lastY + 2.4D, st.lastZ);
        for (var t : st.snapshot.targets()) {
            DustParticleOptions colour = t.state() == VisibilityState.MEMORY_ONLY || t.state() == VisibilityState.LOST ? MEMORY : t.seen() ? SEEN : OBSTRUCTED;
            dot(level, viewer, colour, t.x(), t.y() + 2.1D, t.z());
        }
        for (var h : st.snapshot.recentSounds()) {
            dot(level, viewer, SOUND, h.estimatedX(), h.estimatedY() + 0.5D, h.estimatedZ());
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4.0D;
                dot(level, viewer, SOUND, h.estimatedX() + Math.cos(a) * h.uncertainty(), h.estimatedY() + 0.5D, h.estimatedZ() + Math.sin(a) * h.uncertainty());
            }
        }
        for (var threat : st.snapshot.threats()) dot(level, viewer, THREAT, threat.x(), threat.y() + 1.0D, threat.z());
        st.snapshot.investigationTarget().ifPresent(i -> dot(level, viewer, INVESTIGATE, i.x(), i.y() + 1.5D, i.z()));
    }

    private static DustParticleOptions awarenessColour(AwarenessLevel level) {
        return switch (level) {
            case UNAWARE -> new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.0F);
            case AWARE -> new DustParticleOptions(new Vector3f(0.3F, 1.0F, 0.3F), 1.0F);
            case ALERT -> new DustParticleOptions(new Vector3f(1.0F, 1.0F, 0.0F), 1.0F);
            case SEARCHING -> new DustParticleOptions(new Vector3f(1.0F, 0.55F, 0.0F), 1.2F);
            case TRACKING -> new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.2F), 1.2F);
            case FOCUSED -> new DustParticleOptions(new Vector3f(0.8F, 0.1F, 1.0F), 1.4F);
        };
    }

    private static void dot(ServerLevel level, ServerPlayer viewer, DustParticleOptions particle, double x, double y, double z) {
        level.sendParticles(viewer, particle, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
