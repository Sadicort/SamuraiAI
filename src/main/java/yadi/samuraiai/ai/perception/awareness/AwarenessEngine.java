package yadi.samuraiai.ai.perception.awareness;

import yadi.samuraiai.ai.perception.attention.AttentionLevel;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;

/**
 * The awareness state machine. It reads the outputs of the other engines (attention, suspicion, threat, whether the
 * focus is in sight, whether a lost target or a sound is still remembered) and decides how alert the NPC is. Rising is
 * immediate; falling happens one step at a time and only after a minimum dwell, so awareness never flickers.
 */
public final class AwarenessEngine {
    /** Inputs to one evaluation. */
    public record Inputs(AttentionLevel attention, boolean focusSeen, boolean focusLost, double suspicion, ThreatLevel threat,
                         boolean lostTargetRemembered, boolean heardRecently, boolean anyStimulus) { }

    public record Update(AwarenessLevel level, AwarenessLevel previous, boolean changed, String reason) { }

    private AwarenessLevel level = AwarenessLevel.UNAWARE;
    private long changedTick;

    public AwarenessLevel level() { return level; }
    public void reset() { level = AwarenessLevel.UNAWARE; changedTick = 0; }

    public Update update(Inputs in, long tick, PerceptionSettings s) {
        AwarenessLevel target = target(in, s);
        AwarenessLevel previous = level;
        String reason = explain(in, target);
        if (target.compareTo(level) > 0) {
            level = target;
            changedTick = tick;
        } else if (target.compareTo(level) < 0 && tick - changedTick >= s.awarenessMinDwellTicks()) {
            level = AwarenessLevel.values()[level.ordinal() - 1];
            changedTick = tick;
            reason = "calming down (" + reason + ")";
        }
        return new Update(level, previous, level != previous, reason);
    }

    private static AwarenessLevel target(Inputs in, PerceptionSettings s) {
        boolean suspicious = in.suspicion() >= s.suspicionRaiseThreshold();
        boolean threatened = in.threat().atLeast(ThreatLevel.WARNING);
        boolean worried = suspicious || threatened || (in.focusLost() && in.suspicion() >= s.suspicionClearThreshold());
        if ((in.attention().atLeast(AttentionLevel.FOCUSED) && in.focusSeen()) || in.threat() == ThreatLevel.CRITICAL
                || (in.threat() == ThreatLevel.DANGER && in.focusSeen())) return AwarenessLevel.FOCUSED;
        if (in.focusSeen() && (suspicious || threatened || in.attention().atLeast(AttentionLevel.HIGH))) return AwarenessLevel.TRACKING;
        if (!in.focusSeen() && (in.lostTargetRemembered() || in.heardRecently()) && worried) return AwarenessLevel.SEARCHING;
        if (suspicious || threatened || in.attention().atLeast(AttentionLevel.HIGH)) return AwarenessLevel.ALERT;
        if (in.attention().atLeast(AttentionLevel.MEDIUM) || in.anyStimulus() || in.suspicion() >= s.suspicionClearThreshold()) return AwarenessLevel.AWARE;
        return AwarenessLevel.UNAWARE;
    }

    private static String explain(Inputs in, AwarenessLevel target) {
        return target + " (attention=" + in.attention() + " focusSeen=" + in.focusSeen() + " suspicion=" + (int) in.suspicion() + " threat=" + in.threat()
                + (in.lostTargetRemembered() ? " remembers-lost-target" : "") + (in.heardRecently() ? " heard-something" : "") + ")";
    }
}
