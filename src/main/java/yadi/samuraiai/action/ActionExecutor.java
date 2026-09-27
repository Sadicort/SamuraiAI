package yadi.samuraiai.action;

import yadi.samuraiai.controller.NPCController;
import yadi.samuraiai.npc.NPCRuntime;

public interface ActionExecutor {

    void execute(Action action, NPCRuntime runtime, NPCController controller);

}
