package yadi.samuraiai.spawn;

import yadi.samuraiai.brain.Brain;
import yadi.samuraiai.brain.DefaultBrain;
import yadi.samuraiai.controller.NPCController;
import yadi.samuraiai.memory.ConversationMemory;
import yadi.samuraiai.memory.MemoryManager;
import yadi.samuraiai.npc.NPCDefinition;
import yadi.samuraiai.npc.NPCIdentity;
import yadi.samuraiai.npc.NPCInstance;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.personality.NpcPersonality;
import yadi.samuraiai.personality.PersonalityGenerator;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Builds the internal composition of an NPC (instance plus runtime, with its
 * Brain, memory and personality wired together) from a definition and an
 * identity. Called only by {@link NPCSpawnService} — commands and other
 * callers never construct these pieces by hand.
 */
public class NPCFactory {

    public NPCRuntime build(NPCDefinition definition,
                            NPCIdentity identity,
                            NPCController controller,
                            SpawnLocation location) {

        NPCInstance instance = new NPCInstance(identity, location);

        Brain brain = new DefaultBrain();

        // Pulled from the shared manager rather than created fresh, so an NPC
        // that is unloaded and reactivated later still remembers its past
        // conversations instead of greeting an old friend as a stranger.
        ConversationMemory memory = MemoryManager.getInstance().getMemory(identity.id());

        // Individualised per NPC, not shared per type: two samurai spawned
        // side by side should not think and talk identically just because
        // they share a definition.
        NpcPersonality personality = PersonalityGenerator.individualize(definition.getBasePersonality());

        return new NPCRuntime(instance, definition, brain, controller, memory, personality);
    }

    public NPCRuntime build(NPCDefinition definition, NPCIdentity identity, NPCController controller,
                            SpawnLocation location, NpcPersonality restoredPersonality) {
        NPCInstance instance = new NPCInstance(identity, location);
        Brain brain = new DefaultBrain();
        ConversationMemory memory = MemoryManager.getInstance().getMemory(identity.id());
        return new NPCRuntime(instance, definition, brain, controller, memory,
                restoredPersonality == null ? PersonalityGenerator.individualize(definition.getBasePersonality()) : restoredPersonality);
    }
}
