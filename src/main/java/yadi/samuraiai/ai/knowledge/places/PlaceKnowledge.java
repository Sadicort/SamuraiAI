package yadi.samuraiai.ai.knowledge.places;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.model.KnowledgeCategory;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.Predicate;
import yadi.samuraiai.ai.knowledge.model.ValidationState;

/**
 * Reading what an NPC believes about places, for the Brain and the scheduler: where the temple is, which places are dangerous,
 * the nearest known place of a kind. It gives locations, never routes: computing a physical path stays with the navigation engine.
 */
public final class PlaceKnowledge {
    private PlaceKnowledge() { }

    public static List<KnowledgeRecord> known(KnowledgeRuntime rt) {
        List<KnowledgeRecord> result = new ArrayList<>();
        for (UUID id : rt.ofType(KnowledgeType.PLACE)) { KnowledgeRecord r = rt.get(id); if (r != null && r.alive() && r.predicate() == Predicate.LOCATED_AT && r.place().known()) result.add(r); }
        return result;
    }

    public static List<KnowledgeRecord> ofCategory(KnowledgeRuntime rt, KnowledgeCategory category) {
        List<KnowledgeRecord> result = new ArrayList<>();
        for (KnowledgeRecord r : known(rt)) if (r.category() == category) result.add(r);
        return result;
    }

    /** The nearest believed place of the category (or any), skipping beliefs known to be false. */
    public static KnowledgeRecord nearest(KnowledgeRuntime rt, PlaceRef from, KnowledgeCategory category) {
        KnowledgeRecord best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (KnowledgeRecord r : known(rt)) {
            if (r.state() == ValidationState.FALSE || (category != null && r.category() != category)) continue;
            double d = r.place().distance(from);
            if (d < bestDistance) { bestDistance = d; best = r; }
        }
        return best;
    }

    /** Places the NPC believes to be dangerous, most dangerous first. */
    public static List<KnowledgeRecord> dangerous(KnowledgeRuntime rt, double minimum) {
        List<KnowledgeRecord> result = new ArrayList<>();
        for (UUID id : rt.all().stream().map(KnowledgeRecord::id).toList()) {
            KnowledgeRecord r = rt.get(id);
            if (r == null || !r.alive() || r.state() == ValidationState.FALSE) continue;
            if (danger(r) >= minimum) result.add(r);
        }
        result.sort(Comparator.comparingDouble((KnowledgeRecord r) -> danger(r)).reversed());
        return result;
    }

    public static double danger(KnowledgeRecord r) {
        String value = r.attributes().get("danger");
        if (value != null) try { return Double.parseDouble(value); } catch (NumberFormatException ignored) { /* fall through */ }
        return r.type() == KnowledgeType.DANGER || r.predicate() == Predicate.IS_DANGEROUS ? r.confidence() : 0.0D;
    }
}
