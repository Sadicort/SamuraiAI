package yadi.samuraiai.living.family.events;

/** Knowledge passed from one person to another. */
public record TechniqueTaughtEvent(long minute, String technique, java.util.UUID from, java.util.UUID to, String via) implements FamilyEngineEvent { }
