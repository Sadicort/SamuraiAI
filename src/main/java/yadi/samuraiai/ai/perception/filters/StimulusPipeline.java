package yadi.samuraiai.ai.perception.filters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;

/**
 * Detect, filter, classify, prioritise. Sensors hand it raw stimuli; it merges duplicates within the pass, runs the filter
 * chain (recording what each filter rejected, for the metrics) and returns the survivors ordered by priority, ready for the
 * attention engine.
 */
public final class StimulusPipeline {
    public record Result(List<Stimulus> accepted, int raw, int duplicatesMerged, Map<String, Integer> rejectedByFilter) {
        public int rejected() { return rejectedByFilter.values().stream().mapToInt(Integer::intValue).sum(); }
    }

    private final List<StimulusFilter> filters;

    public StimulusPipeline() { this(StandardFilters.chain()); }
    public StimulusPipeline(List<StimulusFilter> filters) { this.filters = List.copyOf(filters); }

    public Result process(List<Stimulus> raw, FilterContext context) {
        Map<String, Stimulus> unique = new LinkedHashMap<>();
        for (Stimulus stimulus : raw) {
            Stimulus known = unique.get(stimulus.key());
            if (known == null || stimulus.priority() > known.priority()) unique.put(stimulus.key(), stimulus);
        }
        int merged = raw.size() - unique.size();
        Map<String, Integer> rejected = new LinkedHashMap<>();
        List<Stimulus> accepted = new ArrayList<>();
        outer:
        for (Stimulus stimulus : unique.values()) {
            Stimulus current = stimulus;
            for (StimulusFilter filter : filters) {
                Optional<Stimulus> next = filter.apply(current, context);
                if (next.isEmpty()) { rejected.merge(filter.name(), 1, Integer::sum); continue outer; }
                current = next.get();
            }
            accepted.add(current);
        }
        accepted.sort(Comparator.comparingDouble(Stimulus::priority).reversed());
        return new Result(accepted, raw.size(), merged, rejected);
    }
}
