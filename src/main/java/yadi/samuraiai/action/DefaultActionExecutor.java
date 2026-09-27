package yadi.samuraiai.action;

import yadi.samuraiai.controller.NPCController;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.runtime.DialogueService;
import yadi.samuraiai.runtime.ServerScheduler;

public final class DefaultActionExecutor implements ActionExecutor {
    @Override public void execute(Action action, NPCRuntime runtime, NPCController controller) {
        ServerScheduler.getInstance().requireServerThread();
        if (runtime == null || !runtime.isActive()) return;
        if (action instanceof TalkAction talk) DialogueService.getInstance().submit(runtime, talk);
    }
    static String sanitizeMessage(String message) { return DialogueService.sanitize(message); }
}
