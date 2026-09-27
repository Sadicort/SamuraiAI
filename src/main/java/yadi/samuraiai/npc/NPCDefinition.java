package yadi.samuraiai.npc;

import yadi.samuraiai.goal.GoalType;
import yadi.samuraiai.personality.NpcPersonality;

import java.util.List;
import java.util.Set;

/**
 * Describes a *type* of NPC: what it tends to be like, what it is allowed
 * to do, and what it pursues by default. Shared by every instance of that
 * type (e.g. every samurai shares one SamuraiDefinition). Never holds
 * per-individual state such as memory or relationships — that lives on
 * {@link NPCInstance} and {@link NPCRuntime}.
 */
public interface NPCDefinition {

    NPCTypeId getType();

    String getDisplayNamePrefix();

    NpcPersonality getBasePersonality();

    Set<NPCCapability> getCapabilities();

    List<GoalType> getDefaultGoals();

}
