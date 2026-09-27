package yadi.samuraiai.npc;

import java.util.Locale;
import java.util.Objects;

/**
 * Identifies a *type* of NPC (samurai, guard, merchant, ...). New types are
 * added by registering an {@link NPCDefinition} under a new id, never by
 * touching this class.
 *
 * <p>Normalisation uses {@link Locale#ROOT} rather than the default locale:
 * with a Turkish system locale, {@code "SAMURAI".toLowerCase()} yields
 * "samurai" with a dotless i, which would silently fail to match the
 * registered type on that machine and nowhere else.
 */
public record NPCTypeId(String value) {

    public NPCTypeId {
        Objects.requireNonNull(value, "value");
        value = value.trim().toLowerCase(Locale.ROOT);

        if (value.isEmpty()) {
            throw new IllegalArgumentException("El identificador de tipo no puede estar vacio.");
        }
    }

    public static NPCTypeId of(String value) {
        return new NPCTypeId(value);
    }

    public static final NPCTypeId SAMURAI = new NPCTypeId("samurai");

    @Override
    public String toString() {
        return value;
    }
}
