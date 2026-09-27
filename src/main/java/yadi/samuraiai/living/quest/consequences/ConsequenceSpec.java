package yadi.samuraiai.living.quest.consequences;

import yadi.samuraiai.living.quest.branching.Path;

/**
 * What a quest's ending does to the world. {@code onSuccess} / {@code onFailure} say when it applies, {@code branch} limits it
 * to one path. Consequences reach the other engines through the hub:
 * <ul>
 *   <li>RENOWN / UNREST — the village's standing among villages and its discontent;</li>
 *   <li>REPUTATION — the player's standing with a community context;</li>
 *   <li>RELATIONSHIP — the quest giver's feelings (an experience: helped, betrayed);</li>
 *   <li>MEMORY — witnesses remember what happened; HISTORY — the world timeline records it;</li>
 *   <li>RESOLVE_EVENT / SPAWN_EVENT — a world event ends or begins (bandits cleared, looters come);</li>
 *   <li>LOOT — the settlement loses part of its stores; BUILD_HOUSE / BUILD_ROAD — the village or the road network grows;</li>
 *   <li>FAMILY_HONOR / FAMILY_MEMORY / MENTORSHIP — the Family Engine records it.</li>
 * </ul>
 */
public record ConsequenceSpec(Kind kind, String target, String amount, String text, boolean onSuccess, boolean onFailure, Path branch) {
    public enum Kind { RENOWN, UNREST, REPUTATION, RELATIONSHIP, MEMORY, HISTORY, RESOLVE_EVENT, SPAWN_EVENT, LOOT, BUILD_HOUSE, BUILD_ROAD, FAMILY_HONOR, FAMILY_MEMORY, MENTORSHIP }

    public ConsequenceSpec {
        target = target == null ? "" : target;
        amount = amount == null || amount.isBlank() ? "1" : amount;
        text = text == null ? "" : text;
    }

    public static ConsequenceSpec success(Kind kind, String target, String amount, String text) { return new ConsequenceSpec(kind, target, amount, text, true, false, null); }
    public static ConsequenceSpec failure(Kind kind, String target, String amount, String text) { return new ConsequenceSpec(kind, target, amount, text, false, true, null); }
    public ConsequenceSpec on(Path path) { return new ConsequenceSpec(kind, target, amount, text, onSuccess, onFailure, path); }
}
