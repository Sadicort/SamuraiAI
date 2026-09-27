package yadi.samuraiai.ai.memory.pipeline;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import yadi.samuraiai.ai.cognition.model.Trait;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.Category;
import yadi.samuraiai.ai.memory.model.Experience;

/**
 * Decides whether an experience deserves to be kept and how much it matters. The score is the experience's own weight plus a
 * weighted sum of duration, rarity (how often the NPC has lived this before), emotional impact, relevance to its current goal,
 * who was involved, where, what followed, how much the actor matters to it, its personality and the danger. Every factor is
 * reported, so the decision can be audited.
 */
public final class MemoryEvaluator {
    private final ImportanceCalculator calculator = new ImportanceCalculator();

    public ImportanceCalculator calculator() { return calculator; }

    public Evaluation evaluate(Experience e, MemoryRuntime runtime, EvaluationContext context, MemorySettings s) {
        Map<String, Double> f = new LinkedHashMap<>();
        f.put("base", clamp(e.magnitude()));
        f.put("duration", clamp((double) e.duration() / s.durationCapTicks()));
        int similar = e.actor() != null ? intersect(runtime, e) : runtime.index().kind(e.kind()).size();
        f.put("rarity", clamp(1.0D - (double) similar / s.rarityCap()));
        f.put("emotion", clamp(e.emotion().intensity()));
        f.put("goal", e.tags().stream().anyMatch(context.goalTags()::contains) ? 1.0D : 0.0D);
        int people = e.participants().size() + (e.actor() != null ? 1 : 0) + (e.target() != null ? 1 : 0);
        f.put("participants", clamp(people / 4.0D));
        boolean novelPlace = e.place().known() && runtime.index().cell(e.place().cell(s.cellSize())).isEmpty();
        f.put("place", novelPlace ? 0.8D : e.place().zone().isEmpty() ? 0.0D : 0.3D);
        double consequences = 0;
        for (var c : e.consequences()) consequences += Math.abs(c.magnitude());
        f.put("consequence", clamp(consequences));
        f.put("relationship", context.relationshipWeight());
        f.put("personality", personalityFactor(e.category(), context, s));
        f.put("danger", clamp(e.danger()));
        double score = s.weightBase() * f.get("base") + s.weightDuration() * f.get("duration") + s.weightRarity() * f.get("rarity")
                + s.weightEmotion() * f.get("emotion") + s.weightGoal() * f.get("goal") + s.weightParticipants() * f.get("participants")
                + s.weightPlace() * f.get("place") + s.weightConsequence() * f.get("consequence") + s.weightRelationship() * f.get("relationship")
                + s.weightPersonality() * f.get("personality") + s.weightDanger() * f.get("danger");
        // A deeply felt or dangerous event is never trivial, whatever the rest of the sum says.
        if (e.emotion().traumatic()) score = Math.max(score, s.cutCritical());
        // A pivotal event (a betrayal, a death witnessed) is critical once it is really felt, whatever the sum says.
        if (e.tags().contains("pivotal") && e.emotion().intensity() >= 0.5D) score = Math.max(score, s.cutCritical() + 0.02D);
        score = clamp(score);
        boolean keep = score >= s.retainThreshold() || e.emotion().traumatic();
        return new Evaluation(score, calculator.level(score, s), keep, f);
    }

    private static int intersect(MemoryRuntime runtime, Experience e) {
        int count = 0;
        var byActor = runtime.index().entity(e.actor().id());
        for (var id : runtime.index().kind(e.kind())) if (byActor.contains(id)) count++;
        return count;
    }

    private static double personalityFactor(Category category, EvaluationContext context, MemorySettings s) {
        EnumMap<Category, Trait> map = new EnumMap<>(Category.class);
        for (String line : s.effectiveCategoryTraits()) {
            String[] parts = line.split(":");
            if (parts.length != 2) continue;
            try { Trait.parse(parts[1]).ifPresent(t -> map.put(Category.valueOf(parts[0].trim().toUpperCase(java.util.Locale.ROOT)), t)); } catch (IllegalArgumentException ignored) { /* bad line */ }
        }
        Trait trait = map.get(category);
        return trait == null ? 0.0D : context.personality().unit(trait);
    }

    private static double clamp(double v) { return Double.isFinite(v) ? Math.max(0.0D, Math.min(1.0D, v)) : 0.0D; }
}
