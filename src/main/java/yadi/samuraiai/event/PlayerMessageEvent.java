package yadi.samuraiai.event;

import java.util.UUID;

/** A player said something an NPC could hear. */
public record PlayerMessageEvent(UUID npcId, UUID playerId, String playerName, String message)
        implements NpcEvent {

    @Override
    public String getName() {
        return "PLAYER_MESSAGE";
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }
}
