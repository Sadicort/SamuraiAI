package yadi.samuraiai.perception;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.world.ServerWorlds;
import yadi.samuraiai.world.SpawnLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Real sensing against the Minecraft world: the players an NPC could
 * plausibly notice from where it stands, nearest first.
 *
 * <p>This is the first implementation that actually looks at the world;
 * {@link NoopPerceptionSystem} remains for tests and headless runs. It stays
 * deliberately limited to the configured radius and the NPC's own dimension,
 * because the whole point of the perception layer is that an NPC acts on what
 * it noticed rather than on ground truth.
 *
 * <p>Only players are perceived for now. Mobs would be a one-line addition
 * here, but nothing in the decision layer yet does anything useful with them,
 * and scanning every entity in a 24-block cube on every brain tick is a cost
 * worth deferring until something needs it.
 */
public class WorldPerceptionSystem implements PerceptionSystem {

    @Override
    public WorldContext perceive(NPCRuntime runtime) {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();

        SpawnLocation location = runtime.getInstance().getLocation();

        if (location == null) {
            // An NPC with no known position cannot perceive from anywhere;
            // reporting an empty world beats guessing that it is at 0,0.
            return WorldContext.empty();
        }

        Optional<ServerLevel> maybeLevel = ServerWorlds.level(location.dimensionKey());

        if (maybeLevel.isEmpty()) {
            return WorldContext.empty();
        }

        ServerLevel level = maybeLevel.get();
        double radius = SamuraiSettings.perceptionRadius();

        AABB box = new AABB(
                location.x() - radius, location.y() - radius, location.z() - radius,
                location.x() + radius, location.y() + radius, location.z() + radius);

        double radiusSquared = radius * radius;

        List<PerceivedEntity> perceived = new ArrayList<>();

        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {

            double distanceSquared = player.distanceToSqr(location.x(), location.y(), location.z());

            // getEntitiesOfClass returns everything in the bounding box, which
            // is a cube; without this the NPC would see a player standing in a
            // corner 41 blocks away on a radius of 24.
            if (distanceSquared > radiusSquared) {
                continue;
            }
            Vec3 from = new Vec3(location.x(), location.y() + 1.6D, location.z());
            Vec3 to = player.getEyePosition();
            if (level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, null)).getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                continue;
            }

            perceived.add(new PerceivedEntity(
                    player.getUUID(),
                    player.getName().getString(),
                    Math.sqrt(distanceSquared),
                    true,
                    new SpawnLocation(level.dimension().location().toString(), player.getX(), player.getY(), player.getZ(), player.getYRot())));
        }

        perceived.sort(Comparator.comparingDouble(PerceivedEntity::distance));

        return new WorldContext(perceived, level.getDayTime());
    }
}
