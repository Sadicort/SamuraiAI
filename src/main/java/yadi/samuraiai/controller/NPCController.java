package yadi.samuraiai.controller;

import yadi.samuraiai.npc.NPCInstance;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Boundary between SamuraiAI's intelligence and whatever represents the NPC
 * physically in Minecraft.
 *
 * <p>The Brain never touches Entity/Level/PathNavigation directly. It goes
 * through this interface (and, for individual actions, through
 * {@link yadi.samuraiai.action.ActionExecutor}), so the physical
 * representation can be swapped — vanilla entity, CustomNPCs, chat-only —
 * without touching the decision-making code.
 */
public interface NPCController {

    default void close() {}
    default boolean requiresPhysicalBody() { return false; }
    default void synchronize(NPCInstance instance) {}
    default boolean ownsEntity(NPCInstance instance, java.util.UUID entityId) { return false; }
    /** Starts navigation; returns false when this backend has no physical body. */
    default boolean moveTo(NPCInstance instance, SpawnLocation target, double speed) { return false; }
    /** Requests a look-control update toward a player/entity. */
    default boolean lookAt(NPCInstance instance, java.util.UUID targetId) { return false; }
    /** Performs one server-side attack attempt against a target. */
    default boolean attack(NPCInstance instance, java.util.UUID targetId) { return false; }

    /**
     * The physical actuator the navigation engine drives, or empty for a backend without a body. Navigation
     * asks for it instead of calling {@link #moveTo}, so routing stays in SamuraiAI and the backend only executes.
     */
    default java.util.Optional<yadi.samuraiai.ai.navigation.movement.MovementBody> movementBody(NPCInstance instance) {
        return java.util.Optional.empty();
    }
    /** Entity UUIDs this backend owns, so entity scans can tell NPCs apart from players and mobs. */
    default java.util.Set<java.util.UUID> ownedEntityIds() { return java.util.Set.of(); }

    boolean spawnPhysical(NPCInstance instance, SpawnLocation location);

    void removePhysical(NPCInstance instance);

    boolean isPhysicalPresent(NPCInstance instance);

    /**
     * Makes the NPC say something to a player.
     *
     * <p>Speech lives here rather than in the executor because saying a line
     * is a physical act in the world, and the mechanism differs per backend:
     * a chat-only NPC posts to chat, a CustomNPCs one would show a dialogue
     * window. Keeping it behind this interface is what lets
     * {@code DefaultActionExecutor} deliver dialogue without importing a
     * single Minecraft class.
     *
     * @param targetPlayerName who the line is addressed to; may be null for
     *                         something said out loud to nobody in particular
     */
    void speak(NPCInstance instance, String targetPlayerName, String text);

}
