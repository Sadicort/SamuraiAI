package yadi.samuraiai.living.family.events;

/** An heirloom changed hands. */
public record HeirloomTransferredEvent(long minute, java.util.UUID item, String name, java.util.UUID from, java.util.UUID to, String reason) implements FamilyEngineEvent { }
