package yadi.samuraiai.ai.memory.retrieval;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.memory.model.Category;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryType;

/**
 * What to look for. Any combination of criteria: person, place (cell or zone), emotion, day range, importance, category, kind,
 * type, tags, event. EXACT needs every criterion, FUZZY needs a configured fraction of them, SIMILAR ranks by structural
 * similarity to the tags/kind given.
 */
public final class RetrievalQuery {
    public enum Mode { EXACT, FUZZY, SIMILAR }

    UUID entity; PlaceRef place; String zone; EmotionKind emotion; Long fromDay, toDay; Importance minImportance; Category category; ExperienceKind kind;
    MemoryType type; String event; double minStrength; int limit; Mode mode = Mode.EXACT;
    final Set<String> tags = new LinkedHashSet<>();

    public static RetrievalQuery create() { return new RetrievalQuery(); }
    public RetrievalQuery entity(UUID v) { entity = v; return this; }
    public RetrievalQuery place(PlaceRef v) { place = v; return this; }
    public RetrievalQuery zone(String v) { zone = v; return this; }
    public RetrievalQuery emotion(EmotionKind v) { emotion = v; return this; }
    public RetrievalQuery days(long from, long to) { fromDay = from; toDay = to; return this; }
    public RetrievalQuery minImportance(Importance v) { minImportance = v; return this; }
    public RetrievalQuery category(Category v) { category = v; return this; }
    public RetrievalQuery kind(ExperienceKind v) { kind = v; return this; }
    public RetrievalQuery type(MemoryType v) { type = v; return this; }
    public RetrievalQuery tag(String v) { if (v != null && !v.isEmpty()) tags.add(v); return this; }
    public RetrievalQuery event(String v) { event = v; return this; }
    public RetrievalQuery minStrength(double v) { minStrength = v; return this; }
    public RetrievalQuery limit(int v) { limit = v; return this; }
    public RetrievalQuery mode(Mode v) { mode = v; return this; }
    public Mode mode() { return mode; }
}
