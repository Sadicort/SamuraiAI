package yadi.samuraiai.ai.cognition.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** Experience slowly moved a personality trait (never a base value, only the long-term evolution). */
public record PersonalityEvolvedEvent(UUID npcId, UUID traceId, String trait, double moved, double total, String cause) implements NpcEvent {
    @Override public String getName() { return "PersonalityEvolvedEvent"; }
}
