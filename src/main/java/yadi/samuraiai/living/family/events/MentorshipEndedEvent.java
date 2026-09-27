package yadi.samuraiai.living.family.events;

/** An apprenticeship ended (completed, failed, broken, the master died, the disciple left). */
public record MentorshipEndedEvent(long minute, java.util.UUID mentorship, String state, double progress) implements FamilyEngineEvent { }
