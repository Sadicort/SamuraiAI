package yadi.samuraiai.ai.knowledge.validation;

import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.Revision;
import yadi.samuraiai.ai.knowledge.model.ValidationEvidence;
import yadi.samuraiai.ai.knowledge.model.ValidationState;

/**
 * Turns evidence into a validation state. A rumour repeated by the same source counts once; independent sources raise
 * confidence and can make it likely, but only direct observation, a public event or a highly credible source can make it
 * verified. Contradicting evidence lowers confidence and can prove it false. A verified belief is only overturned by direct
 * contradiction.
 */
public final class ValidationEngine {
    public record Outcome(ValidationState before, ValidationState after, double oldConfidence, double newConfidence, boolean changed) { }

    public Outcome apply(KnowledgeRecord r, ValidationEvidence ev, KnowledgeSettings s) {
        ValidationState before = r.state();
        double oldConfidence = r.confidence();
        double confidence = oldConfidence;
        switch (ev.kind()) {
            case DIRECT_CONFIRMS -> {
                r.directEvidence(true);
                if (ev.source() != null) r.supporters().add(ev.source());
                confidence = 1.0D - (1.0D - confidence) * (1.0D - 0.8D);
            }
            case DIRECT_CONTRADICTS -> {
                if (ev.source() != null) r.contradictors().add(ev.source()); else r.contradictors().add(r.owner());
                confidence = confidence * (1.0D - Math.min(1.0D, s.contradictionPenalty() * 2.0D));
            }
            case INDEPENDENT_SOURCE, PLAYER_STATEMENT -> {
                boolean independent = ev.source() != null && r.supporters().add(ev.source());
                if (independent) confidence = 1.0D - (1.0D - confidence) * (1.0D - ev.credibility() * s.confirmationBoost());
            }
            case PUBLIC_EVENT -> {
                r.publicEvidence(true);
                confidence = 1.0D - (1.0D - confidence) * (1.0D - 0.7D);
            }
            case TRUSTED_SOURCE -> {
                boolean independent = ev.source() != null && r.supporters().add(ev.source());
                if (ev.credibility() >= s.trustedSource()) r.trustedEvidence(true);
                if (independent) confidence = 1.0D - (1.0D - confidence) * (1.0D - ev.credibility() * s.confirmationBoost() * 1.5D);
            }
            case CONTRADICTING_SOURCE -> {
                if (ev.source() != null) r.contradictors().add(ev.source());
                confidence = confidence * (1.0D - s.contradictionPenalty() * ev.credibility());
            }
        }
        r.confidence(confidence);
        ValidationState after = classify(r, before, ev.kind() == ValidationEvidence.Kind.DIRECT_CONTRADICTS, s);
        boolean changed = after != before || Math.abs(r.confidence() - oldConfidence) > 1e-6;
        if (changed) {
            r.state(after);
            r.updated(ev.at());
            r.revisions().add(new Revision(ev.at(), oldConfidence, r.confidence(), before, after, ev.kind() + (ev.note().isEmpty() ? "" : " " + ev.note())));
            while (r.revisions().size() > s.maxRevisions()) r.revisions().remove(0);
            r.bump();
        }
        return new Outcome(before, after, oldConfidence, r.confidence(), changed);
    }

    /** The state a belief deserves given its confidence and the kinds of evidence behind it. */
    public ValidationState classify(KnowledgeRecord r, ValidationState current, boolean directContradiction, KnowledgeSettings s) {
        if (!r.contradictors().isEmpty() && r.confidence() <= s.falseAt()) return ValidationState.FALSE;
        boolean evidenced = r.directEvidence() || r.publicEvidence() || r.trustedEvidence();
        if (current == ValidationState.VERIFIED && !directContradiction) return r.confidence() >= s.likelyAt() ? ValidationState.VERIFIED : ValidationState.LIKELY;
        if (evidenced && r.confidence() >= s.verifiedAt()) return ValidationState.VERIFIED;
        if (r.confidence() >= s.likelyAt() || r.supporters().size() >= s.independentForLikely()) return ValidationState.LIKELY;
        if (current == ValidationState.FALSE && r.contradictors().isEmpty()) return ValidationState.RUMOR;
        return r.origin() == yadi.samuraiai.ai.knowledge.model.LearnMethod.RUMOR || current == ValidationState.RUMOR || current == ValidationState.LIKELY ? ValidationState.RUMOR : ValidationState.UNKNOWN;
    }

    /** The state a belief starts in, from how it was learned and how sure the learner is. */
    public ValidationState initial(KnowledgeRecord r, KnowledgeSettings s) {
        boolean evidenced = r.origin().direct();
        if (evidenced) { r.directEvidence(r.origin() != yadi.samuraiai.ai.knowledge.model.LearnMethod.PUBLIC_EVENT); r.publicEvidence(r.origin() == yadi.samuraiai.ai.knowledge.model.LearnMethod.PUBLIC_EVENT); }
        if (evidenced && r.confidence() >= s.verifiedAt()) return ValidationState.VERIFIED;
        if (r.confidence() >= s.likelyAt()) return ValidationState.LIKELY;
        return r.origin() == yadi.samuraiai.ai.knowledge.model.LearnMethod.RUMOR || r.origin() == yadi.samuraiai.ai.knowledge.model.LearnMethod.CONVERSATION ? ValidationState.RUMOR : ValidationState.UNKNOWN;
    }
}
