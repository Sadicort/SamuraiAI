package yadi.samuraiai.ai.memory.diagnostics;

import com.google.gson.JsonObject;
import java.util.EnumMap;
import java.util.Map;
import yadi.samuraiai.ai.memory.engine.MemoryRuntime;
import yadi.samuraiai.ai.memory.model.Importance;
import yadi.samuraiai.ai.memory.model.MemoryRecord;
import yadi.samuraiai.ai.memory.model.MemoryState;
import yadi.samuraiai.ai.memory.model.MemoryType;
import yadi.samuraiai.ai.memory.storage.MemoryCodec;

/** A picture of one NPC's memory: how many, of what kind, how strong, how compressed, how it is indexed and cached. Can be exported whole. */
public final class MemoryDiagnostics {
    public record Snapshot(int count, Map<MemoryType, Integer> byType, Map<Importance, Integer> byImportance, Map<MemoryState, Integer> byState,
                           int temporary, int protectedCount, int compressed, double averageStrength, int spatialNodes, int skills, int beliefs,
                           int indexedEntities, int indexedTags, int indexedCells, int hot, int warm, double cacheHitRate, long estimatedBytes) { }

    private MemoryDiagnostics() { }

    public static Snapshot snapshot(MemoryRuntime rt) {
        Map<MemoryType, Integer> types = new EnumMap<>(MemoryType.class);
        Map<Importance, Integer> importance = new EnumMap<>(Importance.class);
        Map<MemoryState, Integer> states = new EnumMap<>(MemoryState.class);
        int prot = 0, compressed = 0;
        double strength = 0;
        long bytes = 0;
        for (MemoryRecord r : rt.all()) {
            types.merge(r.type(), 1, Integer::sum); importance.merge(r.importance(), 1, Integer::sum); states.merge(r.state(), 1, Integer::sum);
            if (r.isProtected()) prot++;
            if (r.state() == MemoryState.COMPRESSED || !r.detailed()) compressed++;
            strength += r.strength();
            bytes += 220 + 40L * r.tags().size() + 60L * r.events().size() + 50L * r.consequences().size() + 60L * r.entities().size() + (r.detailed() ? 120L * r.context().size() : 0);
        }
        int n = rt.size();
        return new Snapshot(n, types, importance, states, rt.temporaryCount(), prot, compressed, n == 0 ? 0 : strength / n, rt.spatial().size(), rt.procedural().size(),
                rt.semantic().size(), rt.index().entityCount(), rt.index().tagCount(), rt.index().cellCount(), rt.cache().hot().size(), rt.cache().warmSize(), rt.cache().hitRate(), bytes);
    }

    /** A complete, self-describing export of the NPC's memory (the persistent form plus the counters above). */
    public static JsonObject export(MemoryRuntime rt) {
        JsonObject o = MemoryCodec.toJson(rt);
        Snapshot s = snapshot(rt);
        JsonObject d = new JsonObject();
        d.addProperty("count", s.count()); d.addProperty("temporary", s.temporary()); d.addProperty("protected", s.protectedCount()); d.addProperty("compressed", s.compressed());
        d.addProperty("averageStrength", s.averageStrength()); d.addProperty("estimatedBytes", s.estimatedBytes());
        o.add("diagnostics", d);
        return o;
    }
}
