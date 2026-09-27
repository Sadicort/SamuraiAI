package yadi.samuraiai.event;

import java.util.UUID;

/**
 * An NPC took damage. Carries the victim's id, which the previous version
 * omitted — leaving listeners unable to tell *which* NPC was hurt.
 */
public record NpcDamagedEvent(UUID npcId, String attacker, float amount) implements NpcEvent {

    @Override
    public String getName() {
        return "NPC_DAMAGED";
    }

    /** Kept so existing callers reading the attacker still compile. */
    public String getAttacker() {
        return attacker;
    }
}
