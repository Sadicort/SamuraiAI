package yadi.samuraiai.living.quest.rewards;

/**
 * A reward in a template: money (paid from the settlement's treasury, never created), items (taken from the settlement's
 * stores, with their provenance), reputation in a context (village, temple, clan, merchants, guards, monks), knowledge (what
 * the quest giver tells: a place, a story), relationship (the quest giver's trust and gratitude), and titles (prepared).
 * {@code amount} may use variables; {@code target} is a resource, a reputation context or a text.
 */
public record RewardSpec(Kind kind, String amount, String target, String text) {
    public enum Kind { COINS, ITEMS, REPUTATION, KNOWLEDGE, RELATIONSHIP, TITLE }

    public RewardSpec {
        amount = amount == null || amount.isBlank() ? "1" : amount;
        target = target == null ? "" : target;
        text = text == null ? "" : text;
    }

    public static RewardSpec coins(String amount) { return new RewardSpec(Kind.COINS, amount, "", ""); }
    public static RewardSpec items(String resource, String amount) { return new RewardSpec(Kind.ITEMS, amount, resource, ""); }
    public static RewardSpec reputation(String context, String amount) { return new RewardSpec(Kind.REPUTATION, amount, context, ""); }
    public static RewardSpec knowledge(String text) { return new RewardSpec(Kind.KNOWLEDGE, "1", "", text); }
    public static RewardSpec relationship(String amount) { return new RewardSpec(Kind.RELATIONSHIP, amount, "", ""); }
}
