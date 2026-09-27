package yadi.samuraiai.living.family.events;

/** An heirloom earned a name of its own beyond its plain kind, after enough generations and events passed through it. */
public record ArtifactNamedEvent(long minute, java.util.UUID item, String epithet, double symbolicValue) implements FamilyEngineEvent { }
