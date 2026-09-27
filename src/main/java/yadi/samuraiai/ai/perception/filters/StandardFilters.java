package yadi.samuraiai.ai.perception.filters;

import java.util.List;
import java.util.Optional;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;

/** The default filter chain: distance, visibility, relation, priority and cooldown, in that order. */
public final class StandardFilters {
    private StandardFilters() { }

    public static List<StimulusFilter> chain() { return List.of(new DistanceFilter(), new VisibilityFilter(), new RelationFilter(), new PriorityFilter(), new CooldownFilter()); }

    /** Far things matter less: priority fades with distance instead of being cut off at an arbitrary line. */
    public static final class DistanceFilter implements StimulusFilter {
        @Override public String name() { return "distance"; }
        @Override public Optional<Stimulus> apply(Stimulus s, FilterContext c) {
            if (s.type() == StimulusType.ENVIRONMENT || s.type() == StimulusType.DAMAGE || s.type() == StimulusType.CONVERSATION) return Optional.of(s);
            double distance = s.distanceTo(c.perceiver().x(), c.perceiver().y(), c.perceiver().z());
            double reach = Math.max(8.0D, c.settings().entityScanRadius() * 2.0D);
            if (distance > reach * 2.0D) return Optional.empty();
            return Optional.of(s.withPriority(s.priority() * Math.max(0.3D, 1.0D - distance / reach)));
        }
    }

    /** A visual stimulus below the minimum confidence is not really seen. */
    public static final class VisibilityFilter implements StimulusFilter {
        @Override public String name() { return "visibility"; }
        @Override public Optional<Stimulus> apply(Stimulus s, FilterContext c) {
            return s.type() == StimulusType.VISUAL && s.intensity() < c.settings().minConfidence() ? Optional.empty() : Optional.of(s);
        }
    }

    /** Whom the NPC knows changes what deserves attention: friends fade into the background, enemies stand out. */
    public static final class RelationFilter implements StimulusFilter {
        @Override public String name() { return "relation"; }
        @Override public Optional<Stimulus> apply(Stimulus s, FilterContext c) {
            if (s.source() == null || !c.perceiver().knows(s.source())) return Optional.of(s);
            double relation = c.perceiver().relationTo(s.source());
            if (relation >= 50.0D && s.category().threatWeight() == 0) return Optional.of(s.withPriority(s.priority() * 0.6D));
            if (relation <= -30.0D) return Optional.of(s.withPriority(Math.min(100.0D, s.priority() * 1.4D)));
            return Optional.of(s);
        }
    }

    public static final class PriorityFilter implements StimulusFilter {
        @Override public String name() { return "priority"; }
        @Override public Optional<Stimulus> apply(Stimulus s, FilterContext c) {
            return s.priority() < c.settings().attentionMinScore() * 0.5D ? Optional.empty() : Optional.of(s);
        }
    }

    /**
     * The same stimulus is not reported again within its category cooldown, so a long footstep sequence or a steady rain
     * is one piece of evidence and not one per scan. Sight is never cooled down: tracking needs a fresh position each scan.
     */
    public static final class CooldownFilter implements StimulusFilter {
        @Override public String name() { return "cooldown"; }
        @Override public Optional<Stimulus> apply(Stimulus s, FilterContext c) {
            int cooldown = cooldownTicks(s);
            String key = s.key();
            Long last = c.cooldowns().get(key);
            if (cooldown > 0 && last != null && c.tick() - last < cooldown && s.category() != StimulusCategory.DAMAGE_TAKEN) return Optional.empty();
            c.cooldowns().put(key, c.tick());
            return Optional.of(s);
        }

        static int cooldownTicks(Stimulus s) {
            if (s.type() == StimulusType.VISUAL || s.type() == StimulusType.DAMAGE || s.type() == StimulusType.TOUCH) return 0;
            return switch (s.category()) {
                case FOOTSTEP -> 10;
                case WEATHER -> 200;
                case LIGHT, TERRAIN -> 100;
                case DOOR, BLOCK_CHANGE -> 40;
                case IMPACT -> 20;
                case SPEECH, VOICE -> 30;
                case SCENT, FIRE, LAVA -> 60;
                default -> 5;
            };
        }
    }
}
