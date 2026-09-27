package yadi.samuraiai.controller;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.npc.NPCInstance;
import yadi.samuraiai.world.ServerWorlds;
import yadi.samuraiai.world.SpawnLocation;

import java.util.Optional;

/**
 * Controller for NPCs that have a voice but not yet a body.
 *
 * <p>This is what closes the loop that was previously open: dialogue used to
 * be generated and then only written to the log, so from inside the game an
 * NPC never answered anything. Here the reply reaches the player who spoke,
 * and anyone standing close enough to overhear.
 *
 * <p>Spawning and removal are inherited as no-ops from
 * {@link NoopNPCController}; a CustomNPCs adapter would extend this and add
 * the physical entity while keeping the same speech behaviour.
 */
public class ChatNPCController extends NoopNPCController {

    @Override
    public void speak(NPCInstance instance, String targetPlayerName, String text) {
        yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();

        if (text == null || text.isBlank()) {
            return;
        }

        Component line = Component.literal("<" + instance.getIdentity().name() + "> ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE));

        SamuraiLogger.DIALOGUE.info("{} -> {}: {}",
                instance.getIdentity().name(),
                targetPlayerName == null ? "(nadie)" : targetPlayerName,
                text);

        if (broadcastNearby(instance, line)) {
            return;
        }

        // No position known, or nobody within earshot: fall back to telling
        // just the player who was addressed, so a reply is never lost.
        ServerWorlds.playerByName(targetPlayerName)
                .ifPresent(player -> player.sendSystemMessage(line));
    }

    /**
     * Sends the line to every player within the configured chat radius, so a
     * conversation reads naturally to bystanders instead of being invisible to
     * everyone but its addressee.
     *
     * @return true if at least one player heard it
     */
    private static boolean broadcastNearby(NPCInstance instance, Component line) {

        SpawnLocation location = instance.getLocation();

        if (location == null) {
            return false;
        }

        Optional<ServerLevel> maybeLevel = ServerWorlds.level(location.dimensionKey());

        if (maybeLevel.isEmpty()) {
            return false;
        }

        double radius = SamuraiSettings.chatRadius();
        double radiusSquared = radius * radius;

        boolean heard = false;

        for (ServerPlayer player : maybeLevel.get().players()) {

            if (player.distanceToSqr(location.x(), location.y(), location.z()) > radiusSquared) {
                continue;
            }

            player.sendSystemMessage(line);
            heard = true;
        }

        return heard;
    }
}
