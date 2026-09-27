package yadi.samuraiai.living.family.events;

/** A master took a disciple. */
public record MentorshipStartedEvent(long minute, java.util.UUID mentorship, java.util.UUID master, java.util.UUID disciple, String type) implements FamilyEngineEvent { }
