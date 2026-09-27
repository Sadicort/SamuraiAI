package yadi.samuraiai.task;

import yadi.samuraiai.action.ActionExecutor;
import yadi.samuraiai.action.TalkAction;
import yadi.samuraiai.context.NPCContext;
import yadi.samuraiai.context.WorldContext;
import yadi.samuraiai.controller.NPCController;
import yadi.samuraiai.npc.NPCRuntime;
import yadi.samuraiai.perception.PerceivedEntity;

import java.util.List;
import java.util.UUID;

/**
 * The seam between the decision layer and the dialogue pipeline: turns "I
 * need to reply to this player" into a TalkAction for the ActionExecutor.
 *
 * <p>Built by whatever detects a player message (the chat listener or the
 * talk command) and handed to {@link yadi.samuraiai.brain.Brain#submit}, so
 * the request is issued from the brain tick rather than from an event handler
 * on some other thread.
 */
public class RequestDialogueTask implements Task {

    private final String targetPlayerName;
    private final UUID targetPlayerId;
    private final String playerMessage;

    public RequestDialogueTask(String targetPlayerName, UUID targetPlayerId, String playerMessage) {
        this.targetPlayerName = targetPlayerName;
        this.targetPlayerId = targetPlayerId;
        this.playerMessage = playerMessage;
    }

    @Override
    public TaskStatus tick(NPCContext context, WorldContext world, NPCRuntime runtime,
                           NPCController controller, ActionExecutor executor) {

        executor.execute(new TalkAction(
                targetPlayerName,
                targetPlayerId,
                playerMessage,
                nearbyNames(world),
                describeTime(world)), runtime, controller);

        // The request is fire-and-forget: the model answers asynchronously and
        // the executor delivers it, so keeping this task RUNNING would only
        // block the NPC from doing anything else while it waits.
        return TaskStatus.SUCCESS;
    }

    /** Everyone the NPC can see except the person it is answering. */
    private List<String> nearbyNames(WorldContext world) {

        if (world == null) {
            return List.of();
        }

        return world.getPerceivedEntities().stream()
                .map(PerceivedEntity::name)
                .filter(name -> name != null && !name.equalsIgnoreCase(targetPlayerName))
                .toList();
    }

    /**
     * Minecraft day time runs 0-23999 with 0 at dawn, so the quarters below
     * map onto morning, afternoon, evening and night.
     */
    private static String describeTime(WorldContext world) {

        if (world == null) {
            return null;
        }

        long timeOfDay = Math.floorMod(world.getWorldTime(), 24000L);

        if (timeOfDay < 6000L) {
            return "por la manana";
        }

        if (timeOfDay < 12000L) {
            return "por la tarde";
        }

        if (timeOfDay < 18000L) {
            return "al anochecer";
        }

        return "de noche";
    }
}
