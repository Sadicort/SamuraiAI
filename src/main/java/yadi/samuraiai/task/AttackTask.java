package yadi.samuraiai.task;

import yadi.samuraiai.action.*;
import yadi.samuraiai.context.*;
import yadi.samuraiai.controller.*;
import yadi.samuraiai.npc.NPCRuntime;
import java.util.UUID;
import yadi.samuraiai.event.NPCEventBus;
import yadi.samuraiai.event.npc.CombatEndedEvent;
import yadi.samuraiai.event.npc.CombatStartedEvent;

public final class AttackTask implements Task {
    private final UUID target;
    public AttackTask(UUID target) { this.target = target; }
    @Override public TaskStatus tick(NPCContext c, WorldContext w, NPCRuntime r, NPCController controller, ActionExecutor e) {
        if (!controller.requiresPhysicalBody()) return TaskStatus.SUCCESS;
        NPCEventBus.getInstance().post(new CombatStartedEvent(r.getId(), target));
        boolean hit = controller.attack(r.getInstance(), target);
        NPCEventBus.getInstance().post(new CombatEndedEvent(r.getId(), hit ? "hit" : "out-of-range"));
        return hit ? TaskStatus.SUCCESS : TaskStatus.FAILURE;
    }
}
