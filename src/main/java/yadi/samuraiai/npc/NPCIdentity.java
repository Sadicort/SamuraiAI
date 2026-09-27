package yadi.samuraiai.npc;

import java.util.Objects;
import java.util.UUID;

/**
 * Technical identity of one specific NPC. The UUID — not the display name —
 * is what memory, relationships and persistence key off of, so two NPCs can
 * share a name without their histories getting confused.
 */
public record NPCIdentity(UUID id, String name, NPCTypeId type) {

    public NPCIdentity {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }

    public static NPCIdentity generate(String name, NPCTypeId type) {
        return new NPCIdentity(UUID.randomUUID(), name, type);
    }
}
