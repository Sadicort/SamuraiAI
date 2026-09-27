package yadi.samuraiai.ai.scheduler.stack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import yadi.samuraiai.ai.scheduler.emotion.Mood;
import yadi.samuraiai.ai.scheduler.engine.Intent;
import yadi.samuraiai.ai.scheduler.routine.RoutineInstance;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;

/**
 * The multi-behaviour stack: the background activities that accompany the main one. The main routine takes the channels it
 * needs; each background behaviour is offered in priority order and kept only if its channel is still free, so an NPC on
 * patrol scans the horizon while walking, a guard on duty stays alert, a merchant greets passers-by, and nobody tries to
 * scan and sleep at once.
 */
public final class BehaviorStack {
    /** What the stack needs to know about the moment. */
    public record Situation(RoutineInstance main, Mood mood, boolean suspicious, boolean peopleNearby, boolean sociable, boolean inFormation) { }

    private BehaviorStack() { }

    public static Set<Channel> occupied(RoutineInstance main) {
        Set<Channel> used = EnumSet.noneOf(Channel.class);
        if (main == null) return used;
        Intent intent = main.intent();
        if (main.state() == RoutineInstance.State.TRAVELLING) { used.add(Channel.MOVE); return used; }
        if (intent.isResponse()) { used.add(Channel.MOVE); used.add(Channel.LOOK); return used; }
        switch (intent.routine()) {
            case SLEEP, MEDITATE, PRAYER, REST -> { used.add(Channel.POSTURE); used.add(Channel.LOOK); }
            case SOCIAL, MERCHANT, EAT -> { used.add(Channel.TALK); used.add(Channel.POSTURE); }
            case TRAINING -> { used.add(Channel.MOVE); used.add(Channel.POSTURE); used.add(Channel.LOOK); }
            case GUARD -> used.add(Channel.LOOK);
            case WORK -> used.add(Channel.POSTURE);
            default -> { }
        }
        return used;
    }

    public static List<BackgroundBehavior> compose(Situation s) {
        RoutineInstance main = s.main();
        List<BackgroundBehavior> wanted = new ArrayList<>();
        if (main == null) return wanted;
        RoutineType routine = main.intent().routine();
        boolean asleep = routine == RoutineType.SLEEP && main.state() == RoutineInstance.State.ACTIVE;
        if (asleep) return wanted;
        if (s.suspicious() || s.mood() == Mood.ANXIOUS || s.mood() == Mood.PANICKED || routine == RoutineType.GUARD) wanted.add(BackgroundBehavior.STAY_ALERT);
        if (routine == RoutineType.PATROL || main.state() == RoutineInstance.State.TRAVELLING) wanted.add(BackgroundBehavior.SCAN_SURROUNDINGS);
        if (s.inFormation()) wanted.add(BackgroundBehavior.KEEP_FORMATION);
        if (s.peopleNearby() && s.sociable()) wanted.add(BackgroundBehavior.GREET_NEARBY);
        if (main.state() == RoutineInstance.State.ACTIVE && (routine == RoutineType.WORK || routine == RoutineType.REST)) wanted.add(BackgroundBehavior.IDLE_FIDGET);
        wanted.sort(Comparator.comparingInt(BackgroundBehavior::priority).reversed());
        Set<Channel> taken = occupied(main);
        List<BackgroundBehavior> kept = new ArrayList<>();
        for (BackgroundBehavior b : wanted) if (taken.add(b.channel())) kept.add(b);
        return kept;
    }
}
