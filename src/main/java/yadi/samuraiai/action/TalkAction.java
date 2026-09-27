package yadi.samuraiai.action;

import java.util.List;
import java.util.UUID;

/**
 * "Say something back to this player."
 *
 * <p>Carries the player's UUID alongside their name because relationships are
 * keyed by UUID: keying them by name would transfer a player's entire history
 * with an NPC to anyone who later took that name.
 *
 * @param nearbyNames who else the NPC can see, for the situation block of the
 *                    prompt; empty when perception found nobody
 * @param timeOfDay   human-readable time, or null if unknown
 */
public record TalkAction(String targetPlayerName,
                         UUID targetPlayerId,
                         String playerMessage,
                         List<String> nearbyNames,
                         String timeOfDay) implements Action {

    public TalkAction {
        nearbyNames = nearbyNames == null ? List.of() : List.copyOf(nearbyNames);
    }

    /** Minimal form for callers with no perception data to hand. */
    public TalkAction(String targetPlayerName, UUID targetPlayerId, String playerMessage) {
        this(targetPlayerName, targetPlayerId, playerMessage, List.of(), null);
    }

    @Override
    public ActionType getType() {
        return ActionType.TALK;
    }
}
