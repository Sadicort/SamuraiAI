package yadi.samuraiai.living.village.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * The Security Engine's state for one village: the current state and since when, the accumulated threat (0..100, decays by
 * the hour), the attacks under way (open world events) and the last reasons it changed.
 *
 * <p>Transitions: an attack under way means ATTACK; when it ends the village goes into RECOVERY; threat above the danger
 * threshold means DANGER, above the alert threshold ALERT; RECOVERY returns to PEACE once the threat is low for
 * {@code recoveryMinutes}; ALERT and DANGER fall back when the threat decays.
 */
public final class SecurityRuntime {
    public record Change(SecurityState from, SecurityState to, String reason) { }

    private SecurityState state = SecurityState.PEACE;
    private double threat;
    private long since, updatedAt;
    private final Set<UUID> attacks = new LinkedHashSet<>();
    private final Deque<String> reasons = new ArrayDeque<>();

    public SecurityState state() { return state; }
    public double threat() { return threat; }
    public long since() { return since; }
    public Set<UUID> attacks() { return attacks; }
    public Deque<String> reasons() { return reasons; }

    public void report(double amount, String reason, long now, double decayPerHour) {
        decay(now, decayPerHour);
        threat = Math.max(0.0D, Math.min(100.0D, threat + amount));
        note(String.format("+%.0f %s", amount, reason));
    }

    private void note(String r) { reasons.addLast(r); while (reasons.size() > 12) reasons.removeFirst(); }

    private void decay(long now, double decayPerHour) {
        if (now > updatedAt) { threat = Math.max(0.0D, threat - decayPerHour * (now - updatedAt) / 60.0D); updatedAt = now; }
    }

    /** Re-evaluates the state; returns the change, or null. */
    public Change update(long now, double decayPerHour, double alert, double danger, long recoveryMinutes) {
        decay(now, decayPerHour);
        SecurityState next = state;
        String why = "";
        if (!attacks.isEmpty()) { next = SecurityState.ATTACK; why = "ataque en curso"; }
        else if (state == SecurityState.ATTACK) { next = SecurityState.RECOVERY; why = "el ataque terminó"; }
        else if (threat >= danger) { next = SecurityState.DANGER; why = String.format("amenaza %.0f", threat); }
        else if (threat >= alert) { next = state == SecurityState.DANGER || state == SecurityState.RECOVERY ? state : SecurityState.ALERT; why = String.format("amenaza %.0f", threat); }
        else if (state == SecurityState.DANGER) { next = SecurityState.RECOVERY; why = "la amenaza bajó"; }
        else if (state == SecurityState.ALERT) { next = SecurityState.PEACE; why = "calma"; }
        else if (state == SecurityState.RECOVERY && now - since >= recoveryMinutes) { next = SecurityState.PEACE; why = "recuperada"; }
        if (next == state) return null;
        Change c = new Change(state, next, why);
        state = next;
        since = now;
        note(c.from() + "→" + c.to() + ": " + why);
        return c;
    }

    public void restore(SecurityState s, double t, long sinceMinute, long updated, Set<UUID> ongoing) {
        state = s; threat = t; since = sinceMinute; updatedAt = updated; attacks.clear(); attacks.addAll(ongoing);
    }

    public long updatedAt() { return updatedAt; }
}
