package yadi.samuraiai.ai.knowledge.rumors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.Ids;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;

/**
 * The life of a rumour: created from a witness's memory, passed on hop by hop (weakening and sometimes distorting, never
 * inventing a subject), confirmed or rejected by evidence, and forgotten when nobody repeats it. A rumour without an origin
 * cannot exist. Distortion is deterministic (seeded by the rumour and the hop), so a given telling always changes the same way.
 */
public final class RumorEngine {
    public record Transfer(int hop, double magnitude, double strength, boolean transformed, boolean accepted) { }

    public RumorRecord create(EntityRef origin, UUID originMemory, UUID traceId, RumorClaim claim, String community, long now, double strength) {
        if (origin == null || (originMemory == null && traceId == null)) throw new IllegalArgumentException("A rumour must know where it came from");
        if (claim == null || claim.subject() == null) throw new IllegalArgumentException("A rumour must be about someone or something");
        return new RumorRecord(UUID.randomUUID(), origin, originMemory, traceId, claim, community, now, strength);
    }

    /** One more telling: the rumour weakens, may distort, and the listener becomes a holder. Refused when the chain is too long or the listener already knows it. */
    public Transfer transfer(RumorRecord rumor, UUID from, UUID to, double credibility, long now, KnowledgeSettings s) {
        int hop = rumor.hops().size() + 1;
        if (!rumor.open() || hop > s.maxHops() || rumor.holders().contains(to) || credibility < s.rumorMinCredibility()) return new Transfer(hop, rumor.claim().magnitude(), rumor.strength(), false, false);
        double before = rumor.claim().magnitude();
        double after = before;
        boolean transformed = false;
        double noise = Ids.noise(rumor.id(), hop);
        if (noise < Math.min(0.9D, s.distortionPerHop() * hop * 2.0D)) {
            double swing = (Ids.noise(rumor.id(), hop * 7L + 3L) - 0.4D) * 2.0D * s.distortionPerHop() * 2.0D;
            double ratio = 1.0D + swing;
            double floor = 1.0D - s.distortionMax(), ceiling = 1.0D + s.distortionMax();
            double total = (rumor.initialMagnitude() == 0 ? 1.0D : before / rumor.initialMagnitude()) * ratio;
            after = Math.max(0.0D, Math.min(1.0D, rumor.initialMagnitude() * Math.max(floor, Math.min(ceiling, total))));
            transformed = Math.abs(after - before) > 1e-6;
            if (transformed) {
                rumor.claim(rumor.claim().withMagnitude(after));
                rumor.transformations().add(new Transformation(now, hop, after > before ? "exaggerated" : "toned down", before, after));
            }
        }
        rumor.strength(rumor.strength() * s.rumorHopDecay());
        rumor.hops().add(new RumorHop(from, to, now, credibility, transformed));
        rumor.holders().add(to);
        rumor.lastSpread(now);
        return new Transfer(hop, after, rumor.strength(), transformed, true);
    }

    public void resolve(RumorRecord rumor, boolean confirmed, String by, long now) {
        rumor.state(confirmed ? RumorState.CONFIRMED : RumorState.FALSE);
        rumor.resolved(now, by);
    }

    /** Rumours nobody has repeated for a while (and that were never resolved) are forgotten. */
    public List<RumorRecord> forgetStale(Iterable<RumorRecord> rumors, long now, KnowledgeSettings s) {
        List<RumorRecord> forgotten = new ArrayList<>();
        for (RumorRecord r : rumors) {
            if (!r.open()) continue;
            if (now - r.lastSpread() > s.rumorHalfLifeTicks() * 2.0D) { r.state(RumorState.FORGOTTEN); r.resolved(now, "forgotten"); forgotten.add(r); }
        }
        return forgotten;
    }
}
