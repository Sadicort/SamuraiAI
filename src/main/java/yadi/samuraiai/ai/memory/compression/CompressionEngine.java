package yadi.samuraiai.ai.memory.compression;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.engine.MemorySettings;
import yadi.samuraiai.ai.memory.model.Chapter;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.model.MemoryState;
import yadi.samuraiai.ai.memory.model.MemoryType;

/**
 * Keeps thousands of memories affordable. Runs of repetitive, unimportant episodes (a hundred patrols of the same route, a
 * dozen chats with the same person) collapse into one gist memory that remembers how many and over what span; the detail of
 * old, unimportant memories (context, events, minor participants, consequences) is dropped. Important, protected and traumatic
 * memories are never touched.
 */
public final class CompressionEngine {
    public record Report(int before, int after, int groups, int detailReduced) {
        public boolean changed() { return groups > 0 || detailReduced > 0; }
    }

    public Report run(MemoryRuntime rt, long now, int groupBudget, MemorySettings s) {
        int before = rt.size();
        int groups = 0, reduced = 0;
        if (before >= s.compressionTrigger()) {
            Map<String, List<MemoryRecord>> buckets = new HashMap<>();
            for (MemoryRecord r : rt.all()) {
                if (!eligible(r)) continue;
                long window = r.stamp().gameTime() / s.compressWindowTicks();
                String key = r.kind() + "|" + (r.actor() == null ? "-" : r.actor().id()) + "|" + r.place().cell(rt.index().cellSize()) + "|" + window;
                buckets.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
            }
            for (List<MemoryRecord> group : buckets.values()) {
                if (groups >= groupBudget) break;
                if (group.size() < s.compressGroupMin()) continue;
                collapse(rt, group, s);
                groups++;
            }
        }
        int budget = groupBudget * 8;
        for (MemoryRecord r : rt.all()) {
            if (budget <= 0) break;
            if (!r.detailed() || r.isProtected() || r.importance().atLeast(Importance.HIGH) || r.state() == MemoryState.TEMPORARY) continue;
            if (now - r.stamp().gameTime() < s.detailAgeTicks()) continue;
            reduceDetail(rt, r);
            reduced++; budget--;
        }
        if (groups > 0 || reduced > 0) rt.markDirty();
        return new Report(before, rt.size(), groups, reduced);
    }

    private static boolean eligible(MemoryRecord r) {
        return !r.isProtected() && r.importance().ordinal() <= Importance.NORMAL.ordinal() && r.state() != MemoryState.TEMPORARY
                && r.state() != MemoryState.COMPRESSED && !r.emotion().traumatic();
    }

    private void collapse(MemoryRuntime rt, List<MemoryRecord> group, MemorySettings s) {
        group.sort(Comparator.comparingLong(r -> r.stamp().gameTime()));
        MemoryRecord survivor = group.get(0);
        int total = 0; long duration = 0, end = survivor.endTime(); double strength = 0, weight = 0; Importance top = survivor.importance();
        double valence = 0, intensity = 0;
        for (MemoryRecord r : group) {
            total += r.repeatCount(); duration += r.duration(); end = Math.max(end, r.endTime()); strength = Math.max(strength, r.strength());
            weight = Math.max(weight, r.emotionalWeight()); if (r.importance().ordinal() > top.ordinal()) top = r.importance();
            valence += r.emotion().valence() * r.repeatCount(); intensity += r.emotion().intensity() * r.repeatCount();
        }
        final int count = total; final long span = end, length = duration; final double s2 = strength, w2 = weight, v2 = valence / total, i2 = intensity / total; final Importance level = top;
        List<UUID> absorbed = new ArrayList<>();
        for (int i = 1; i < group.size(); i++) absorbed.add(group.get(i).id());
        rt.reindex(survivor, () -> {
            survivor.type(MemoryType.SEMANTIC);
            survivor.state(MemoryState.COMPRESSED);
            survivor.repeatCount(count); survivor.duration(length); survivor.endTime(span); survivor.strength(s2); survivor.emotionalWeight(w2);
            survivor.importance(level);
            survivor.emotion(survivor.emotion().withIntensity(i2).withValence(v2));
            survivor.chapters().clear();
            survivor.chapters().add(new Chapter(survivor.stamp().gameTime(), span, survivor.kind().name().toLowerCase() + " x" + count));
            survivor.events().clear(); survivor.consequences().clear(); survivor.context().clear();
            for (int i = 1; i < group.size(); i++) { survivor.socialLinks().addAll(group.get(i).socialLinks()); survivor.knowledgeLinks().addAll(group.get(i).knowledgeLinks()); survivor.tags().addAll(group.get(i).tags()); }
            survivor.detailed(false);
            survivor.bumpVersion();
        });
        for (UUID id : absorbed) rt.remove(id);
    }

    private static void reduceDetail(MemoryRuntime rt, MemoryRecord r) {
        rt.reindex(r, () -> {
            r.context().clear(); r.events().clear(); r.consequences().clear(); r.chapters().clear();
            r.entities().removeIf(e -> (r.actor() == null || !e.id().equals(r.actor().id())) && (r.target() == null || !e.id().equals(r.target().id())));
            r.detailed(false);
            r.bumpVersion();
        });
    }
}
