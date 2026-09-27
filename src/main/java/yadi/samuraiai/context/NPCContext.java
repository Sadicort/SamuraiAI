package yadi.samuraiai.context;

import yadi.samuraiai.emotion.EmotionState;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.npc.NPCCapability;
import yadi.samuraiai.npc.NPCInstance;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.npc.NPCState;
import yadi.samuraiai.personality.NpcPersonality;

import java.util.Set;

/**
 * What the DecisionEngine is allowed to know about the NPC itself:
 * personality, emotions, current state and goal, relationships (via the
 * instance) and capabilities.
 *
 * <p>Kept separate from {@link WorldContext} so "who I am" and "what I
 * perceive" never get mixed together — that separation is what stops a
 * behaviour from quietly reading ground truth about the world.
 */
public class NPCContext {

    private final NPCInstance instance;
    private final NpcPersonality personality;
    private final EmotionState emotionState;
    private final NPCState state;
    private final Goal currentGoal;
    private final Set<NPCCapability> capabilities;

    public NPCContext(NPCInstance instance,
                      NpcPersonality personality,
                      EmotionState emotionState,
                      NPCState state,
                      Goal currentGoal,
                      Set<NPCCapability> capabilities) {
        this.instance = instance;
        this.personality = personality;
        this.emotionState = emotionState;
        this.state = state;
        this.currentGoal = currentGoal;
        this.capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }

    /**
     * Snapshots a runtime into the view the decision layer sees. Having one
     * factory keeps the six-argument constructor from being spelled out at
     * every call site, where it is easy to swap two arguments of the same type.
     */
    public static NPCContext of(NPCRuntime runtime) {
        return new NPCContext(
                runtime.getInstance(),
                runtime.getPersonality(),
                runtime.getEmotionState(),
                runtime.getState(),
                runtime.getCurrentGoal(),
                runtime.getDefinition().getCapabilities());
    }

    public NPCInstance getInstance() {
        return instance;
    }

    public NpcPersonality getPersonality() {
        return personality;
    }

    public EmotionState getEmotionState() {
        return emotionState;
    }

    public NPCState getState() {
        return state;
    }

    public Goal getCurrentGoal() {
        return currentGoal;
    }

    public Set<NPCCapability> getCapabilities() {
        return capabilities;
    }

    public boolean can(NPCCapability capability) {
        return capabilities.contains(capability);
    }
}
