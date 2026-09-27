package yadi.samuraiai.ai.scheduler.group;

import java.util.UUID;
import yadi.samuraiai.ai.scheduler.personality.PersonalityTraits;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.zone.Place;

/** What the group coordinator needs to know about one member at a sync: where it is, what it is doing and what it is like. */
public record MemberSnapshot(UUID id, String dimension, double x, double y, double z, PersonalityTraits traits, String groupType,
                             RoutineType routine, Place routinePlace, boolean travelling, boolean emergency) { }
