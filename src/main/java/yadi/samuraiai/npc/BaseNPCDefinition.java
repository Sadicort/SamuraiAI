package yadi.samuraiai.npc;

import yadi.samuraiai.goal.GoalType;
import yadi.samuraiai.personality.NpcPersonality;

import java.util.List;
import java.util.Set;

/**
 * Plain data implementation of {@link NPCDefinition}. New NPC types are
 * created by instantiating this (or a subclass) with different data, not by
 * writing a new Brain.
 */
public class BaseNPCDefinition implements NPCDefinition {

    private final NPCTypeId type;
    private final String displayNamePrefix;
    private final NpcPersonality basePersonality;
    private final Set<NPCCapability> capabilities;
    private final List<GoalType> defaultGoals;

    public BaseNPCDefinition(NPCTypeId type,
                              String displayNamePrefix,
                              NpcPersonality basePersonality,
                              Set<NPCCapability> capabilities,
                              List<GoalType> defaultGoals) {
        this.type = type;
        this.displayNamePrefix = displayNamePrefix;
        this.basePersonality = basePersonality;
        this.capabilities = Set.copyOf(capabilities);
        this.defaultGoals = List.copyOf(defaultGoals);
    }

    @Override
    public NPCTypeId getType() {
        return type;
    }

    @Override
    public String getDisplayNamePrefix() {
        return displayNamePrefix;
    }

    @Override
    public NpcPersonality getBasePersonality() {
        return basePersonality;
    }

    @Override
    public Set<NPCCapability> getCapabilities() {
        return capabilities;
    }

    @Override
    public List<GoalType> getDefaultGoals() {
        return defaultGoals;
    }
}
