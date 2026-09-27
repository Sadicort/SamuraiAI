package yadi.samuraiai.living.quest.reputation;

import java.util.Locale;
import java.util.Optional;

/**
 * The reputation contexts quests reward and require. They are labels of the standing the Knowledge Engine's communities keep
 * for each person (no second reputation system): {@link #label()} is the standing label used.
 */
public enum ReputationContext {
    VILLAGE("village"), TEMPLE("temple"), CLAN("clan"), MERCHANTS("merchants"), GUARDS("guards"), MONKS("monks");

    private final String label;
    ReputationContext(String label) { this.label = label; }
    public String label() { return label; }

    public static Optional<ReputationContext> parse(String text) {
        if (text == null) return Optional.empty();
        try { return Optional.of(valueOf(text.trim().toUpperCase(Locale.ROOT))); } catch (IllegalArgumentException e) { return Optional.empty(); }
    }
}
