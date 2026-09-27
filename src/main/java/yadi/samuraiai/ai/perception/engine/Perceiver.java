package yadi.samuraiai.ai.perception.engine;

import java.util.UUID;

/**
 * Everything perception needs to know about the observing NPC, as a snapshot taken by the adapter each pass: where its
 * eyes are, where it faces, how it feels, whom it knows and what it is doing. No NPC internals leak into the engine.
 *
 * @param entityId   the physical entity (to exclude the NPC from its own senses); may equal id in tests
 * @param relation   score in -100..100 for a target, or NaN when the NPC does not know it
 * @param goal       name of the current goal ("PATROL", "REST", ...), used only to weigh what deserves attention
 */
public record Perceiver(UUID id, UUID entityId, String name, double x, double y, double z, float yaw, float pitch, double eyeHeight,
                        double fear, double anger, double calm, String goal, SenseProfile senses,
                        java.util.function.ToDoubleFunction<UUID> relation) {

    public Perceiver {
        senses = senses == null ? SenseProfile.NEUTRAL : senses;
        relation = relation == null ? target -> Double.NaN : relation;
        goal = goal == null ? "IDLE" : goal;
        name = name == null ? "npc" : name;
    }

    public double eyeY() { return y + eyeHeight; }
    public boolean knows(UUID target) { return !Double.isNaN(relation.applyAsDouble(target)); }
    public double relationTo(UUID target) { return relation.applyAsDouble(target); }

    public static Perceiver simple(UUID id, double x, double y, double z, float yaw) {
        return new Perceiver(id, id, "npc", x, y, z, yaw, 0.0F, 1.62D, 0, 0, 50, "IDLE", SenseProfile.NEUTRAL, null);
    }
    public Perceiver at(double nx, double ny, double nz, float nyaw) {
        return new Perceiver(id, entityId, name, nx, ny, nz, nyaw, pitch, eyeHeight, fear, anger, calm, goal, senses, relation);
    }
    public Perceiver feeling(double newFear, double newAnger, double newCalm) {
        return new Perceiver(id, entityId, name, x, y, z, yaw, pitch, eyeHeight, newFear, newAnger, newCalm, goal, senses, relation);
    }
    public Perceiver withGoal(String value) {
        return new Perceiver(id, entityId, name, x, y, z, yaw, pitch, eyeHeight, fear, anger, calm, value, senses, relation);
    }
    public Perceiver withSenses(SenseProfile value) {
        return new Perceiver(id, entityId, name, x, y, z, yaw, pitch, eyeHeight, fear, anger, calm, goal, value, relation);
    }
    public Perceiver withRelation(java.util.function.ToDoubleFunction<UUID> value) {
        return new Perceiver(id, entityId, name, x, y, z, yaw, pitch, eyeHeight, fear, anger, calm, goal, senses, value);
    }
}
