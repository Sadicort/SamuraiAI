package yadi.samuraiai.brain;

import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.controller.NPCController;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.task.Task;

/**
 * The NPC's cognitive core: given what it perceives and knows about itself,
 * decides what to do and drives it to completion.
 *
 * <p>This replaces the old NpcBrain/OllamaBrain pairing, which was really
 * just a dialogue generator. That capability now lives under
 * {@code yadi.samuraiai.ai} and is something a Behavior or Task can
 * *request* from the Brain, not something the Brain *is*.
 */
public interface Brain {

    /** One full cognition cycle: decide, plan, act. */
    void tick(NPCContext context, WorldContext world, NPCRuntime runtime, NPCController controller);

    /**
     * Injects work from outside the goal loop, such as a player speaking. The
     * task runs ahead of whatever the NPC had planned, on the next tick, so
     * external callers never execute Tasks on their own thread.
     */
    void submit(Task task);

    /**
     * True when injected work is waiting. The tick service uses this to run a
     * brain immediately instead of making a player wait out the rest of the
     * tick interval for a reply.
     */
    boolean hasPendingWork();

    void cancelAll();

}
