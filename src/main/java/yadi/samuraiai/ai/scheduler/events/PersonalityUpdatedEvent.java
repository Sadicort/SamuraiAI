package yadi.samuraiai.ai.scheduler.events;

import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;

/** Experience moved one of an NPC's traits. */
public record PersonalityUpdatedEvent(UUID npcId, String trait, double before, double after, String cause) implements NpcEvent {
    @Override public String getName() { return "PersonalityUpdatedEvent"; }
}
