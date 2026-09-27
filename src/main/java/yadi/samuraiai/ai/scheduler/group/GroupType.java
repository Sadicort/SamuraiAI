package yadi.samuraiai.ai.scheduler.group;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import yadi.samuraiai.ai.scheduler.formation.FormationType;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;

/**
 * The five kinds of group, with the routines whose leader's choice the members share, and the shape they take when the
 * leader is on the move or standing still.
 */
public enum GroupType {
    PATROL(EnumSet.of(RoutineType.PATROL, RoutineType.GUARD, RoutineType.TRAINING, RoutineType.EAT, RoutineType.REST), FormationType.COLUMN, FormationType.LINE),
    GUARD(EnumSet.of(RoutineType.GUARD, RoutineType.PATROL), FormationType.LINE, FormationType.CIRCLE),
    MERCHANT(EnumSet.of(RoutineType.MERCHANT, RoutineType.SOCIAL, RoutineType.EAT), FormationType.ESCORT, FormationType.CIRCLE),
    VILLAGE(EnumSet.of(RoutineType.SOCIAL, RoutineType.PRAYER, RoutineType.EAT), FormationType.TRIANGLE, FormationType.CIRCLE),
    SQUAD(EnumSet.of(RoutineType.PATROL, RoutineType.TRAINING, RoutineType.GUARD, RoutineType.MEDITATE, RoutineType.EAT, RoutineType.REST), FormationType.DIAMOND, FormationType.CIRCLE);

    private final Set<RoutineType> shared;
    private final FormationType moving, standing;

    GroupType(Set<RoutineType> shared, FormationType moving, FormationType standing) { this.shared = shared; this.moving = moving; this.standing = standing; }

    public boolean shares(RoutineType routine) { return shared.contains(routine); }
    public FormationType moving() { return moving; }
    public FormationType standing() { return standing; }

    public static Optional<GroupType> parse(String name) {
        if (name == null) return Optional.empty();
        try { return Optional.of(valueOf(name.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
