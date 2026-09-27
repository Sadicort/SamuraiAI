package yadi.samuraiai.ai.scheduler.personality;

import java.util.Map;

/** Experiences that slowly change a personality, and which traits they move (per unit of drift). */
public enum DriftCause {
    FRIGHTENED(Map.of(Trait.CAUTION, 1.0D, Trait.COURAGE, -0.6D)),
    TRIUMPHED(Map.of(Trait.COURAGE, 1.0D, Trait.AGGRESSION, 0.3D)),
    SOCIALISED(Map.of(Trait.SOCIABILITY, 1.0D)),
    TOILED(Map.of(Trait.DILIGENCE, 1.0D, Trait.DISCIPLINE, 0.3D)),
    CONTEMPLATED(Map.of(Trait.SPIRITUALITY, 1.0D, Trait.PATIENCE, 0.5D)),
    DISCOVERED(Map.of(Trait.CURIOSITY, 1.0D)),
    BETRAYED(Map.of(Trait.LOYALTY, -1.0D, Trait.CAUTION, 0.5D));

    private final Map<Trait, Double> effect;
    DriftCause(Map<Trait, Double> effect) { this.effect = effect; }
    public Map<Trait, Double> effect() { return effect; }
}
