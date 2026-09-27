package yadi.samuraiai.ai.perception.filters;

import java.util.Optional;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;

/** One step of noise removal. Returns the stimulus (possibly with adjusted priority) or empty to reject it. */
public interface StimulusFilter {
    String name();
    Optional<Stimulus> apply(Stimulus stimulus, FilterContext context);
}
