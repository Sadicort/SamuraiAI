package yadi.samuraiai.living.family.events;

import java.util.UUID;

/** An heirloom is lost (not merely misplaced and found again the same tick) — the hub turns this into a recovery quest. */
public record HeirloomLostEvent(long minute, UUID itemId, String name, UUID familyId) implements FamilyEngineEvent { }
