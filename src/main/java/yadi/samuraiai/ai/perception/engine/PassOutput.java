package yadi.samuraiai.ai.perception.engine;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.perception.awareness.SuspicionSource;
import yadi.samuraiai.ai.perception.hearing.HeardSound;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusSink;
import yadi.samuraiai.ai.perception.vision.VisionEngine;

/**
 * What the sensors produced during one perception pass: raw stimuli, vision transitions, heard sounds and requests to
 * raise suspicion. The engine reads it after all due sensors have run; sensors never touch the NPC's engines directly.
 */
public final class PassOutput implements StimulusSink {
    public record SuspicionRequest(SuspicionSource source, double amount) { }

    public final List<Stimulus> stimuli = new ArrayList<>();
    public final List<VisionEngine.Transition> transitions = new ArrayList<>();
    public final List<HeardSound> heard = new ArrayList<>();
    public final List<SuspicionRequest> suspicion = new ArrayList<>();
    public int targetsEvaluated, raysUsed, sensorsRun;

    @Override public void accept(Stimulus stimulus) { stimuli.add(stimulus); }
    public void suspect(SuspicionSource source, double amount) { suspicion.add(new SuspicionRequest(source, amount)); }
}
