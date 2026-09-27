package yadi.samuraiai.living.server;

import com.mojang.math.Vector3f;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.living.economy.caravans.Caravan;
import yadi.samuraiai.living.sim.LivingWorld;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.homes.HomeRecord;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.village.security.SecurityState;
import yadi.samuraiai.living.world.settlements.Settlement;
import yadi.samuraiai.world.ServerWorlds;

/**
 * Server-side overlay of the living world drawn with dust particles only the requesting operator sees: each nearby village's
 * border (green at peace, yellow on alert, red in danger), its buildings as columns (white built, grey planned, orange damaged,
 * black destroyed or abandoned), the beds of its homes (blue), and caravans on the road between settlements (gold dot at the
 * share of the route's length already travelled). Everything shown is read from the engines; nothing is computed for the overlay.
 */
final class LivingDebugRenderer {
    private static final DustParticleOptions PEACE = dust(0.2F, 0.9F, 0.3F, 1.4F), ALERT = dust(1.0F, 0.85F, 0.1F, 1.4F), DANGER = dust(1.0F, 0.1F, 0.1F, 1.6F),
            BUILT = dust(1.0F, 1.0F, 1.0F, 1.2F), PLANNED = dust(0.55F, 0.55F, 0.55F, 1.0F), DAMAGED = dust(1.0F, 0.5F, 0.1F, 1.2F), RUINED = dust(0.1F, 0.1F, 0.1F, 1.2F),
            BED = dust(0.2F, 0.4F, 1.0F, 1.0F), CARAVAN = dust(1.0F, 0.8F, 0.2F, 2.0F);
    private static final double RANGE = 160.0D;

    private LivingDebugRenderer() { }

    private static DustParticleOptions dust(float r, float g, float b, float size) { return new DustParticleOptions(new Vector3f(r, g, b), size); }

    static void render(LivingService service, Set<UUID> viewers) {
        LivingWorld w = service.world().orElse(null);
        if (w == null) return;
        for (UUID viewerId : viewers) {
            var maybe = ServerWorlds.playerById(viewerId);
            if (maybe.isEmpty() || !(maybe.get().level instanceof ServerLevel level)) continue;
            ServerPlayer viewer = maybe.get();
            String dimension = level.dimension().location().toString();
            for (Village v : w.villages().villages()) {
                if (!v.dimension().equals(dimension) || Math.hypot(viewer.getX() - v.x(), viewer.getZ() - v.z()) > RANGE + v.radius()) continue;
                SecurityState s = v.security().state();
                DustParticleOptions border = s == SecurityState.PEACE ? PEACE : s.threatened() ? DANGER : ALERT;
                int points = (int) Math.min(64, Math.max(16, v.radius() / 2));
                for (int i = 0; i < points; i++) {
                    double a = 2 * Math.PI * i / points;
                    level.sendParticles(viewer, border, true, v.x() + Math.cos(a) * v.radius(), viewer.getY() + 0.5D, v.z() + Math.sin(a) * v.radius(), 1, 0, 0, 0, 0);
                }
                for (Building b : v.buildings().values()) {
                    DustParticleOptions c = switch (b.state()) { case BUILT -> BUILT; case PLANNED -> PLANNED; case DAMAGED -> DAMAGED; default -> RUINED; };
                    for (int h = 0; h < 3; h++) level.sendParticles(viewer, c, true, b.x(), b.y() + 1 + h * 0.7D, b.z(), 1, 0, 0, 0, 0);
                }
                for (HomeRecord home : v.homes().values()) level.sendParticles(viewer, BED, true, home.bedX(), home.bedY() + 0.6D, home.bedZ(), 1, 0, 0, 0, 0);
            }
            for (Caravan c : w.economy().caravans()) {
                if (c.state() != Caravan.State.TRAVELLING) continue;
                Settlement from = w.world().settlement(c.origin()).orElse(null), to = w.world().settlement(c.destination()).orElse(null);
                if (from == null || to == null || !from.dimension().equals(dimension)) continue;
                double length = w.economy().routes().stream().filter(r -> r.id().equals(c.route())).mapToDouble(r -> r.distance()).findFirst().orElse(0.0D);
                double t = length <= 0 ? 0.5D : Math.max(0, Math.min(1, c.progress() / length));
                double x = from.x() + (to.x() - from.x()) * t, z = from.z() + (to.z() - from.z()) * t;
                if (Math.hypot(viewer.getX() - x, viewer.getZ() - z) <= RANGE) level.sendParticles(viewer, CARAVAN, true, x, viewer.getY() + 1.5D, z, 3, 0.2, 0.2, 0.2, 0);
            }
        }
    }
}
