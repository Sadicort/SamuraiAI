package yadi.samuraiai.living.quest.objectives;

import yadi.samuraiai.living.quest.branching.Path;

/**
 * An objective in a template, before its variables are known: {@code target} and {@code quantity} may name variables
 * ({@code {resource}}, {@code {quantity}*1.5}), {@code place} names where it happens ({@code settlement}, {@code region},
 * {@code event}, {@code route}, {@code temple}...), {@code branch} limits it to one path (null = every path).
 */
public record ObjectiveSpec(ObjectiveType type, String description, String target, String quantity, String place, double radius, boolean optional, Path branch) {
    public ObjectiveSpec {
        target = target == null ? "" : target;
        quantity = quantity == null || quantity.isBlank() ? "1" : quantity;
        place = place == null ? "" : place;
        radius = radius <= 0 ? 24 : radius;
    }

    public static ObjectiveSpec of(ObjectiveType type, String description, String target, String quantity, String place) {
        return new ObjectiveSpec(type, description, target, quantity, place, 24, false, null);
    }

    public ObjectiveSpec on(Path path) { return new ObjectiveSpec(type, description, target, quantity, place, radius, optional, path); }
    public ObjectiveSpec asOptional() { return new ObjectiveSpec(type, description, target, quantity, place, radius, true, branch); }
    public ObjectiveSpec within(double r) { return new ObjectiveSpec(type, description, target, quantity, place, r, optional, branch); }
}
