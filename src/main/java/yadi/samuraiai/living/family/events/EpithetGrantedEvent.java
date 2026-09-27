package yadi.samuraiai.living.family.events;

/** A person earned an epithet from a real cause (leadership, mastery, mentorship, honour, dishonour or founding a clan). */
public record EpithetGrantedEvent(long minute, java.util.UUID person, String epithet, String category, String cause) implements FamilyEngineEvent { }
