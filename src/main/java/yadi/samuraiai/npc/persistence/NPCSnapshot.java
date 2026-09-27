package yadi.samuraiai.npc.persistence;

import java.util.UUID;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.world.SpawnLocation;

/** Version 1 bootstrap DTO. No runtime, tasks, controller or full relationships are serialized. */
public record NPCSnapshot(int schemaVersion, UUID id, String type, String name,
                          String personality, SpawnLocation location) {
    public NPCSnapshot {
        if (schemaVersion != 1) throw new IllegalArgumentException("Unsupported snapshot version " + schemaVersion);
        java.util.Objects.requireNonNull(id);
        if (type == null || type.isBlank() || name == null || name.isBlank())
            throw new IllegalArgumentException("Identity required");
        java.util.Objects.requireNonNull(location);
    }
    public static NPCSnapshot capture(NPCRuntime npc) {
        return new NPCSnapshot(1, npc.getId(), npc.getInstance().getIdentity().type().value(), npc.getName(),
                npc.getPersonality() == null ? "" : npc.getPersonality().getDescription(), npc.getInstance().getLocation());
    }
}
