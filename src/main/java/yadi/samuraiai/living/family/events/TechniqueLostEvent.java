package yadi.samuraiai.living.family.events;

/** The last holder of a technique is gone without having taught it. */
public record TechniqueLostEvent(long minute, String technique, String name, java.util.UUID lastHolder) implements FamilyEngineEvent { }
