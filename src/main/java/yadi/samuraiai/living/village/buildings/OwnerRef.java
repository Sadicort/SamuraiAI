package yadi.samuraiai.living.village.buildings;

import java.util.UUID;

/**
 * Who owns something in a village. The Village Engine records ownership of buildings; wealth and goods are the Economy
 * Engine's, and inheritance moves ownership through the Family Engine, which calls back here.
 */
public record OwnerRef(Kind kind, UUID id, String label) {
    public enum Kind { VILLAGE, CITIZEN, HOUSEHOLD, FAMILY, TEMPLE, MERCHANT, NONE }

    public static final OwnerRef NONE = new OwnerRef(Kind.NONE, null, "");

    public static OwnerRef village(UUID id) { return new OwnerRef(Kind.VILLAGE, id, "aldea"); }
    public static OwnerRef citizen(UUID id, String name) { return new OwnerRef(Kind.CITIZEN, id, name); }

    public String describe() { return kind == Kind.NONE ? "nadie" : kind.name().toLowerCase(java.util.Locale.ROOT) + (label.isEmpty() ? "" : " " + label); }
}
