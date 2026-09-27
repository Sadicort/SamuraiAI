package yadi.samuraiai.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SamuraiLogger {

    public static final Logger CORE = LoggerFactory.getLogger("SamuraiAI/CORE");
    public static final Logger BRAIN = LoggerFactory.getLogger("SamuraiAI/BRAIN");
    public static final Logger MEMORY = LoggerFactory.getLogger("SamuraiAI/MEMORY");
    public static final Logger PERCEPTION = LoggerFactory.getLogger("SamuraiAI/PERCEPTION");
    public static final Logger DECISION = LoggerFactory.getLogger("SamuraiAI/DECISION");
    public static final Logger BEHAVIOR = LoggerFactory.getLogger("SamuraiAI/BEHAVIOR");
    public static final Logger COMBAT = LoggerFactory.getLogger("SamuraiAI/COMBAT");
    public static final Logger DIALOGUE = LoggerFactory.getLogger("SamuraiAI/DIALOGUE");
    public static final Logger AI = LoggerFactory.getLogger("SamuraiAI/AI");
    public static final Logger NPC = LoggerFactory.getLogger("SamuraiAI/NPC");
    public static final Logger NETWORK = LoggerFactory.getLogger("SamuraiAI/NETWORK");
    public static final Logger CONFIG = LoggerFactory.getLogger("SamuraiAI/CONFIG");
    public static final Logger RUNTIME = LoggerFactory.getLogger("SamuraiAI/RUNTIME");
    public static final Logger LIFECYCLE = LoggerFactory.getLogger("SamuraiAI/LIFECYCLE");
    public static final Logger RELATIONSHIP = LoggerFactory.getLogger("SamuraiAI/RELATIONSHIP");
    public static final Logger EMOTION = LoggerFactory.getLogger("SamuraiAI/EMOTION");
    public static final Logger EVENTS = LoggerFactory.getLogger("SamuraiAI/EVENTS");
    public static final Logger QUEUE = LoggerFactory.getLogger("SamuraiAI/QUEUE");
    public static final Logger CONTROLLER = LoggerFactory.getLogger("SamuraiAI/CONTROLLER");
    public static final Logger SPAWN = LoggerFactory.getLogger("SamuraiAI/SPAWN");
    public static final Logger PERSISTENCE = LoggerFactory.getLogger("SamuraiAI/PERSISTENCE");
    public static final Logger CUSTOM_NPCS = LoggerFactory.getLogger("SamuraiAI/CustomNPCs");
    public static final Logger SCHEDULER = LoggerFactory.getLogger("SamuraiAI/SCHEDULER");
    public static final Logger NAVIGATION = LoggerFactory.getLogger("SamuraiAI/Navigation");
    public static final Logger PERFORMANCE = LoggerFactory.getLogger("SamuraiAI/Performance");

    private SamuraiLogger() {
    }

    public static String context(yadi.samuraiai.npc.NPCRuntime npc) {
        var location = npc.getInstance().getLocation();
        return "npc=" + npc.getId() + " name=" + npc.getName() + " type=" + npc.getInstance().getIdentity().type()
                + " world=" + (location == null ? "unknown" : location.dimensionKey()) + " state=" + npc.getState()
                + " timestamp=" + java.time.Instant.now() + " thread=" + Thread.currentThread().getName();
    }
}
