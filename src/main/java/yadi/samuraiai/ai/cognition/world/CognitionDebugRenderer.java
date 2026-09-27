package yadi.samuraiai.ai.cognition.world;

import com.mojang.math.Vector3f;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.ai.emotion.model.MoodKind;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.ValidationState;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.world.ServerWorlds;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Server-side overlay drawn with dust particles that only the requesting operator sees: over each nearby NPC a coloured mark for
 * its mood (red angry, dark red fearful, blue melancholic, gold happy, white calm), and at each place it knows about a column
 * (green verified, yellow likely, violet rumour, red dangerous), so what an NPC believes about the world can be seen in the world.
 */
final class CognitionDebugRenderer {
    private static final DustParticleOptions VERIFIED = new DustParticleOptions(new Vector3f(0.2F, 0.9F, 0.3F), 1.2F), LIKELY = new DustParticleOptions(new Vector3f(0.95F, 0.85F, 0.2F), 1.2F),
            RUMOR = new DustParticleOptions(new Vector3f(0.7F, 0.3F, 0.9F), 1.2F), DANGER = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.4F),
            ANGRY = new DustParticleOptions(new Vector3f(1.0F, 0.2F, 0.1F), 1.6F), FEARFUL = new DustParticleOptions(new Vector3f(0.5F, 0.0F, 0.1F), 1.6F), SAD = new DustParticleOptions(new Vector3f(0.2F, 0.3F, 1.0F), 1.6F),
            HAPPY = new DustParticleOptions(new Vector3f(1.0F, 0.85F, 0.1F), 1.6F), CALM = new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.2F);
    private static final double RANGE_SQUARED = 48.0D * 48.0D;

    private CognitionDebugRenderer() { }

    static void render(CognitionService service, Set<UUID> viewers) {
        for (UUID viewerId : viewers) {
            var maybe = ServerWorlds.playerById(viewerId);
            if (maybe.isEmpty() || !(maybe.get().level instanceof ServerLevel level)) continue;
            ServerPlayer viewer = maybe.get();
            String dimension = level.dimension().location().toString();
            for (NPCRuntime npc : service.living()) {
                SpawnLocation l = npc.getInstance().getLocation();
                if (l == null || !l.dimensionKey().equals(dimension) || viewer.distanceToSqr(l.x(), l.y(), l.z()) > RANGE_SQUARED) continue;
                MoodKind mood = service.engine().emotions().mood(npc.getId());
                var colour = switch (mood) { case ANGRY -> ANGRY; case FEARFUL, ALERT -> FEARFUL; case MELANCHOLIC, EXHAUSTED -> SAD; case HAPPY, INSPIRED, HOPEFUL -> HAPPY; default -> CALM; };
                level.sendParticles(viewer, colour, true, l.x(), l.y() + 2.6D, l.z(), 2, 0.1D, 0.1D, 0.1D, 0.0D);
                var knowledge = service.engine().knowledge().peek(npc.getId());
                if (knowledge.isEmpty()) continue;
                int shown = 0;
                for (KnowledgeRecord r : knowledge.get().all()) {
                    if (shown >= 12 || r.type() != KnowledgeType.PLACE || !r.place().known() || !r.place().dimension().equals(dimension)) continue;
                    if (viewer.distanceToSqr(r.place().x(), r.place().y(), r.place().z()) > RANGE_SQUARED) continue;
                    var c = r.state() == ValidationState.VERIFIED ? VERIFIED : r.state() == ValidationState.LIKELY ? LIKELY : otherColour(r);
                    for (int h = 0; h < 4; h++) level.sendParticles(viewer, c, true, r.place().x(), r.place().y() + 1 + h * 0.6D, r.place().z(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    shown++;
                }
            }
        }
    }

    private static DustParticleOptions otherColour(KnowledgeRecord r) { return r.type() == KnowledgeType.DANGER ? DANGER : RUMOR; }
}
