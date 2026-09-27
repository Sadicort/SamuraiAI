package yadi.samuraiai.ai.cognition.model;

import java.util.Objects;
import java.util.UUID;

/** A stable reference to someone or something a cognitive record is about. Identity is the UUID; the name is only a label. */
public record EntityRef(UUID id, EntityKind kind, String name) {
    public EntityRef {
        Objects.requireNonNull(id, "id");
        kind = kind == null ? EntityKind.UNKNOWN : kind;
        name = name == null ? "" : name;
    }

    public static EntityRef player(UUID id, String name) { return new EntityRef(id, EntityKind.PLAYER, name); }
    public static EntityRef npc(UUID id, String name) { return new EntityRef(id, EntityKind.NPC, name); }
    public static EntityRef of(UUID id, EntityKind kind, String name) { return new EntityRef(id, kind, name); }

    /** A deterministic id for something that has none in the world (a named place, a concept). */
    public static UUID nameId(String namespace, String name) {
        return UUID.nameUUIDFromBytes((namespace + ":" + name.toLowerCase(java.util.Locale.ROOT)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String label() { return name.isEmpty() ? id.toString().substring(0, 8) : name; }

    @Override public boolean equals(Object o) { return o instanceof EntityRef r && r.id.equals(id); }
    @Override public int hashCode() { return id.hashCode(); }
}
