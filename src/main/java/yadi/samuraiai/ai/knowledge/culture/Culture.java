package yadi.samuraiai.ai.knowledge.culture;

import java.util.List;
import java.util.Map;

/** A culture: its traditions and its norms (named numbers such as {@code HONOR_SCALE} and {@code OATH_WEIGHT} that other engines read). Data, not code. */
public record Culture(String id, String name, List<Tradition> traditions, Map<String, Double> norms) {
    public Culture {
        traditions = List.copyOf(traditions);
        norms = Map.copyOf(norms);
    }

    public double norm(String key, double fallback) { return norms.getOrDefault(key, fallback); }
}
