package yadi.samuraiai.npc;

import yadi.samuraiai.brain.Brain;
import yadi.samuraiai.controller.NPCController;
import yadi.samuraiai.emotion.EmotionState;
import yadi.samuraiai.goal.Goal;
import yadi.samuraiai.memory.ConversationMemory;
import yadi.samuraiai.personality.NpcPersonality;

import java.util.Objects;
import java.util.UUID;

/**
 * Execution-time state of an NPC while it is active in the world: its Brain,
 * active emotions, current goal and conversation memory. Rebuilt (not
 * persisted directly) whenever an {@link NPCInstance} is activated — only the
 * durable parts of an NPC live on the instance itself.
 */
public class NPCRuntime {

    private final NPCInstance instance;
    private final NPCDefinition definition;
    private final Brain brain;
    private final NPCController controller;
    private final ConversationMemory conversationMemory;
    private final NpcPersonality personality;
    private final EmotionState emotionState = new EmotionState();

    private volatile NPCState state = NPCState.IDLE;
    private volatile Goal currentGoal;
    private volatile boolean active;

    public boolean isActive() { return active; }
    public void setActive(boolean value) {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();
        active = value;
    }

    /**
     * Server tick on which this NPC's brain last ran. The tick service uses
     * it to spread brains across ticks instead of thinking for every NPC in
     * the same tick, which is what turns 50 NPCs into a lag spike.
     */
    private volatile long lastBrainTick = Long.MIN_VALUE;

    public NPCRuntime(NPCInstance instance,
                      NPCDefinition definition,
                      Brain brain,
                      NPCController controller,
                      ConversationMemory conversationMemory,
                      NpcPersonality personality) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.brain = Objects.requireNonNull(brain, "brain");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.conversationMemory = Objects.requireNonNull(conversationMemory, "conversationMemory");
        this.personality = personality;
    }

    public NPCInstance getInstance() {
        return instance;
    }

    /** Shorthand for the identity UUID, which nearly every caller wanted. */
    public UUID getId() {
        return instance.getIdentity().id();
    }

    public String getName() {
        return instance.getIdentity().name();
    }

    public NPCDefinition getDefinition() {
        return definition;
    }

    public Brain getBrain() {
        return brain;
    }

    public NPCController getController() {
        return controller;
    }

    public ConversationMemory getConversationMemory() {
        return conversationMemory;
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

    public void setState(NPCState state) {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();
        this.state = state;
        NPCManager.getInstance().refresh(this);
    }

    public Goal getCurrentGoal() {
        return currentGoal;
    }

    public void setCurrentGoal(Goal currentGoal) {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();
        this.currentGoal = currentGoal;
    }

    public long getLastBrainTick() {
        return lastBrainTick;
    }

    public void setLastBrainTick(long tick) {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();
        this.lastBrainTick = tick;
    }

    public boolean hasCapability(NPCCapability capability) {
        return definition.getCapabilities().contains(capability);
    }
}
