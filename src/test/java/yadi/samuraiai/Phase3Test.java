package yadi.samuraiai;

import org.junit.jupiter.api.Test;
import yadi.samuraiai.behavior.*;
import yadi.samuraiai.goal.GoalType;
import yadi.samuraiai.npc.NPCInstance;
import yadi.samuraiai.task.MoveToTask;
import yadi.samuraiai.task.TaskStatus;
import yadi.samuraiai.world.SpawnLocation;

import static org.junit.jupiter.api.Assertions.*;

class Phase3Test extends ServerTestBase {
    @Test void defaultRegistryContainsPhysicalBehaviors() {
        var registry = new BehaviorRegistry();
        assertTrue(registry.hasDedicatedBehavior(GoalType.PATROL));
        assertTrue(registry.hasDedicatedBehavior(GoalType.FLEE));
        assertTrue(registry.hasDedicatedBehavior(GoalType.COMBAT));
        assertTrue(registry.hasDedicatedBehavior(GoalType.INVESTIGATE));
        assertTrue(registry.hasDedicatedBehavior(GoalType.PROTECT));
    }

    @Test void chatBackendCompletesPhysicalTaskWithoutBlocking() {
        var definition = yadi.samuraiai.npc.definition.SamuraiDefinition.create();
        var identity = yadi.samuraiai.npc.NPCIdentity.generate("phase3", definition.getType());
        var instance = new NPCInstance(identity, new SpawnLocation("minecraft:overworld", 0, 64, 0, 0));
        var runtime = new yadi.samuraiai.npc.NPCRuntime(instance, definition, new yadi.samuraiai.brain.DefaultBrain(),
                new yadi.samuraiai.controller.ChatNPCController(), new yadi.samuraiai.memory.ConversationMemory(4), definition.getBasePersonality());
        runtime.setActive(true);
        var task = new MoveToTask(instance.getLocation().withPosition(4, 64, 0), 1, 1, 2);
        assertEquals(TaskStatus.SUCCESS, task.tick(yadi.samuraiai.context.NPCContext.of(runtime), yadi.samuraiai.context.WorldContext.empty(), runtime, runtime.getController(), new yadi.samuraiai.action.DefaultActionExecutor()));
    }
}
