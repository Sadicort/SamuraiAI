package yadi.samuraiai.event;

import java.util.UUID;

/** A player came within an NPC's perception radius. */
public record PlayerApproachEvent(UUID npcId, UUID playerId, String playerName, double distance)
        implements NpcEvent {

    @Override
    public String getName() {
        return "PLAYER_APPROACH";
    }

    public String getPlayerName() {
        return playerName;
    }
}
