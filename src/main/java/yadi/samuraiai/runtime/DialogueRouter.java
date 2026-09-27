package yadi.samuraiai.runtime;

import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.event.*;
import yadi.samuraiai.npc.*;
import yadi.samuraiai.task.RequestDialogueTask;
import yadi.samuraiai.world.ServerWorlds;

/** All accepted inputs enter the Brain, using separate monotonic cooldowns. */
public final class DialogueRouter {
    private static final Map<UUID, Long> players = new HashMap<>(), npcs = new HashMap<>();
    private record Conversation(UUID npc, UUID player) {}
    private static final Map<Conversation, Long> conversations = new HashMap<>();
    public static Optional<NPCRuntime> speakToNearest(ServerPlayer player, String message) {
        ServerScheduler.getInstance().requireServerThread();
        if (player == null) return Optional.empty();
        Optional<NPCRuntime> nearest = NPCManager.getInstance().findNearest(ServerWorlds.dimensionKeyOf(player.getLevel()),
                player.getX(),player.getY(),player.getZ(),SamuraiSettings.chatRadius());
        nearest.ifPresent(npc -> speakTo(npc,player,message));
        return nearest;
    }
    public static void speakTo(NPCRuntime npc, ServerPlayer player, String message) {
        ServerScheduler.getInstance().requireServerThread();
        if (npc == null || !npc.isActive() || player == null || message == null || message.isBlank()
                || !npc.hasCapability(NPCCapability.CAN_TALK)) return;
        long now = System.nanoTime();
        Conversation conversation = new Conversation(npc.getId(), player.getUUID());
        if (cooling(players.get(player.getUUID()), now, SamuraiSettings.playerCooldownMillis())
                || cooling(npcs.get(npc.getId()), now, SamuraiSettings.npcCooldownMillis())
                || cooling(conversations.get(conversation), now, SamuraiSettings.conversationCooldownMillis())) return;
        players.put(player.getUUID(), now); npcs.put(npc.getId(), now); conversations.put(conversation, now);
        String clean = DialogueService.sanitize(message);
        NPCEventBus.getInstance().post(new PlayerMessageEvent(npc.getId(),player.getUUID(),player.getName().getString(),clean));
        if (npc.isActive()) npc.getBrain().submit(new RequestDialogueTask(player.getName().getString(),player.getUUID(),clean));
    }
    private static boolean cooling(Long last, long now, int millis) { return last != null && now-last < millis*1_000_000L; }
    public static void forgetNpc(UUID id) { npcs.remove(id); conversations.keySet().removeIf(c -> c.npc().equals(id)); }
    public static void forgetPlayer(UUID id) { players.remove(id); conversations.keySet().removeIf(c -> c.player().equals(id)); }
    public static void reset() { players.clear(); npcs.clear(); conversations.clear(); }
    private DialogueRouter() {}
}
