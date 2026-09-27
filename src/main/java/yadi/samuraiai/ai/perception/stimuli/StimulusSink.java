package yadi.samuraiai.ai.perception.stimuli;

/** Where sensors put what they detect. The pipeline behind it filters, classifies and prioritises. */
@FunctionalInterface
public interface StimulusSink {
    void accept(Stimulus stimulus);
}
