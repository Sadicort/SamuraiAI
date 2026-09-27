package yadi.samuraiai.ai.perception.sensors;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.perception.awareness.SuspicionSource;
import yadi.samuraiai.ai.perception.hearing.HeardSound;
import yadi.samuraiai.ai.perception.hearing.HearingEngine;
import yadi.samuraiai.ai.perception.hearing.SoundCategory;
import yadi.samuraiai.ai.perception.hearing.SoundEvent;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;
import yadi.samuraiai.ai.perception.vision.VisualTrack;

/**
 * Hears sounds recorded in the dimension's log plus footsteps derived from the movement of nearby entities. Each heard
 * sound becomes an audio stimulus at the <em>estimated</em> position with its uncertainty. A sound whose source is not in
 * sight is unexplained, which makes the NPC a little suspicious.
 */
public final class HearingSensor implements Sensor {
    private final HearingEngine engine = new HearingEngine();

    @Override public SensorType type() { return SensorType.HEARING; }

    @Override public void scan(SensorContext ctx) {
        var state = ctx.state();
        List<SoundEvent> sounds = new ArrayList<>(ctx.sounds().since(state.lastSoundTick));
        sounds.addAll(engine.footsteps(ctx.perceiver(), ctx.catalog(), ctx.tick()));
        state.lastSoundTick = ctx.tick();
        if (sounds.isEmpty()) return;
        List<HeardSound> heard = engine.hear(ctx.perceiver(), ctx.world(), ctx.settings(), sounds, ctx.rays());
        for (HeardSound h : heard) {
            SoundEvent sound = h.sound();
            double priority = sound.category().stimulusCategory().basePriority() * Math.max(0.25D, h.intensity());
            ctx.out().accept(new Stimulus(StimulusType.AUDIO, sound.category().stimulusCategory(), sound.source(), sound.label(), h.estimatedX(), h.estimatedY(),
                    h.estimatedZ(), h.uncertainty(), h.intensity(), priority, ctx.tick(), Math.max(20, sound.durationTicks()), "sound " + h.direction()));
            ctx.out().heard.add(h);
            state.lastHeardTick = ctx.tick();
            VisualTrack track = sound.source() == null ? null : state.tracks.get(sound.source());
            boolean explained = track != null && track.state.seen();
            if (!explained) {
                double amount = switch (sound.category()) {
                    case EXPLOSION -> 25.0D;
                    case BLOCK_BREAK, IMPACT, DOOR, PROJECTILE, DAMAGE -> 14.0D;
                    case FOOTSTEP_WALK, FOOTSTEP_SPRINT, ANIMAL -> sound.source() != null && ctx.perceiver().knows(sound.source()) ? 2.0D : 6.0D;
                    default -> 5.0D;
                };
                ctx.out().suspect(SuspicionSource.SOUND, amount * h.intensity());
            }
        }
    }
}
