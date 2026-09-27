package yadi.samuraiai.ai.knowledge.entities;

import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.Predicate;

/** Reading what an NPC knows about a person: role, faction, home and where they were last seen, each with its own confidence. */
public final class PersonKnowledge {
    private PersonKnowledge() { }

    private static Optional<KnowledgeRecord> first(KnowledgeRuntime rt, UUID person, Predicate predicate) {
        KnowledgeRecord best = null;
        for (UUID id : rt.aboutSubject(person)) {
            KnowledgeRecord r = rt.get(id);
            if (r == null || r.predicate() != predicate || !r.alive()) continue;
            if (best == null || r.confidence() > best.confidence()) best = r;
        }
        return Optional.ofNullable(best);
    }

    public static Optional<KnowledgeRecord> role(KnowledgeRuntime rt, UUID person) { return first(rt, person, Predicate.HAS_ROLE); }
    public static Optional<KnowledgeRecord> faction(KnowledgeRuntime rt, UUID person) { return first(rt, person, Predicate.BELONGS_TO); }
    public static Optional<KnowledgeRecord> home(KnowledgeRuntime rt, UUID person) { return first(rt, person, Predicate.LIVES_AT); }
    public static Optional<KnowledgeRecord> honorable(KnowledgeRuntime rt, UUID person) { return first(rt, person, Predicate.IS_HONORABLE); }
    public static boolean knowsOf(KnowledgeRuntime rt, UUID person) { return !rt.aboutSubject(person).isEmpty(); }
}
