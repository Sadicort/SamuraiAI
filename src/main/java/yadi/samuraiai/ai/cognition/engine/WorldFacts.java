package yadi.samuraiai.ai.cognition.engine;

import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.PlaceRef;

/** What the cognitive layer needs to know about the world without touching Minecraft: the weather, where an NPC is, what it is trying to do and who an id refers to. The world adapter implements it. */
public interface WorldFacts {
    String weather();
    PlaceRef placeOf(UUID npc);
    Set<String> goalTags(UUID npc);
    EntityRef refOf(UUID id);

    WorldFacts NONE = new WorldFacts() {
        @Override public String weather() { return ""; }
        @Override public PlaceRef placeOf(UUID npc) { return PlaceRef.unknown(); }
        @Override public Set<String> goalTags(UUID npc) { return Set.of(); }
        @Override public EntityRef refOf(UUID id) { return new EntityRef(id, EntityKind.UNKNOWN, ""); }
    };
}
