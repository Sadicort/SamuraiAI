package yadi.samuraiai.ai.navigation.world;

import com.mojang.math.Vector3f;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.ai.navigation.engine.NavigationSession;
import yadi.samuraiai.ai.navigation.graph.NavPos;

/**
 * Server-side debug overlay: draws each nearby path with dust particles only the requesting operator sees
 * (green = route, yellow = node being approached, red = destination, magenta = blocked cell, blue = danger zone).
 * No client mod or network packet is needed, so it works on a dedicated server.
 */
final class NavigationDebugRenderer {
    private static final DustParticleOptions ROUTE = new DustParticleOptions(new Vector3f(0.1F, 0.9F, 0.2F), 0.8F);
    private static final DustParticleOptions TARGET = new DustParticleOptions(new Vector3f(1.0F, 0.9F, 0.1F), 1.2F);
    private static final DustParticleOptions GOAL = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.5F);
    private static final DustParticleOptions BLOCKED = new DustParticleOptions(new Vector3f(0.9F, 0.1F, 0.9F), 1.0F);
    private static final DustParticleOptions DANGER = new DustParticleOptions(new Vector3f(0.2F, 0.4F, 1.0F), 1.0F);
    private static final double RANGE_SQUARED = 64.0D * 64.0D;
    private static final int MAX_POINTS_PER_PATH = 96;

    private NavigationDebugRenderer() { }

    static void render(NavigationService service, Set<UUID> viewers) {
        for (UUID viewerId : viewers) {
            var maybe = NavigationService.player(viewerId);
            if (maybe.isEmpty()) continue;
            ServerPlayer viewer = maybe.get();
            if (!(viewer.level instanceof ServerLevel level)) continue;
            String dimension = level.dimension().location().toString();
            for (NavigationService.Dimension d : service.dimensions()) {
                if (!d.key().equals(dimension)) continue;
                for (NavigationSession session : d.runtime().activeSessions()) drawSession(level, viewer, session);
                for (var zone : d.dangers().activeZones(dimension, service.currentTick()))
                    dot(level, viewer, DANGER, zone.center().centerX(), zone.center().y() + 0.3D, zone.center().centerZ());
            }
        }
    }

    private static void drawSession(ServerLevel level, ServerPlayer viewer, NavigationSession session) {
        var path = session.path();
        if (path == null) return;
        int stride = Math.max(1, path.size() / MAX_POINTS_PER_PATH);
        for (int i = 0; i < path.size(); i += stride) {
            NavPos p = path.get(i).pos();
            if (viewer.distanceToSqr(p.centerX(), p.y(), p.centerZ()) > RANGE_SQUARED) continue;
            dot(level, viewer, ROUTE, p.centerX(), p.y() + 0.2D, p.centerZ());
        }
        int index = Math.min(session.index(), path.size() - 1);
        NavPos target = path.get(index).pos();
        dot(level, viewer, TARGET, target.centerX(), target.y() + 0.9D, target.centerZ());
        NavPos goal = session.destination();
        dot(level, viewer, GOAL, goal.centerX(), goal.y() + 1.2D, goal.centerZ());
        for (NavPos blocked : session.blockedNodes()) dot(level, viewer, BLOCKED, blocked.centerX(), blocked.y() + 0.6D, blocked.centerZ());
    }

    private static void dot(ServerLevel level, ServerPlayer viewer, DustParticleOptions particle, double x, double y, double z) {
        level.sendParticles(viewer, particle, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}
