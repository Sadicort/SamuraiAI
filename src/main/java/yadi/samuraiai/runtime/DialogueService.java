package yadi.samuraiai.runtime;

import java.util.*;
import java.util.concurrent.Executor;
import yadi.samuraiai.action.TalkAction;
import yadi.samuraiai.ai.*;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.context.AIContext;
import yadi.samuraiai.emotion.*;
import yadi.samuraiai.event.*;
import yadi.samuraiai.event.npc.*;
import yadi.samuraiai.npc.*;
import yadi.samuraiai.npc.relationship.RelationshipService;
import yadi.samuraiai.perception.WorldPerceptionSystem;

/** Server-thread actor: advances a lane only AFTER delivery/memory commit of its preceding turn. */
public final class DialogueService {
    private static final DialogueService INSTANCE = new DialogueService();
    public static DialogueService getInstance() { return INSTANCE; }
    private final AIRequestQueue queue;
    private final yadi.samuraiai.perception.PerceptionSystem perception;
    public DialogueService() { this(AIRequestQueue.getInstance(), runtime -> yadi.samuraiai.perception.PerceptionSystems.current().perceive(runtime)); }
    public DialogueService(AIRequestQueue queue, yadi.samuraiai.perception.PerceptionSystem perception) {
        this.queue = java.util.Objects.requireNonNull(queue);
        this.perception = java.util.Objects.requireNonNull(perception);
    }
    private final Map<UUID, Deque<Turn>> lanes = new HashMap<>();
    private final Map<Conversation, UUID> conversations = new HashMap<>();
    private record Conversation(UUID npc, UUID player) {}
    private static final class Turn {
        final UUID id = UUID.randomUUID(), conversation;
        final NPCRuntime runtime;
        final TalkAction action;
        AIRequestQueue.Request request;
        Turn(UUID conversation, NPCRuntime runtime, TalkAction action) {
            this.conversation = conversation; this.runtime = runtime; this.action = action;
        }
    }
    public void submit(NPCRuntime runtime, TalkAction action) {
        ServerScheduler.getInstance().requireServerThread();
        if (!live(runtime)) return;
        Deque<Turn> lane = lanes.computeIfAbsent(runtime.getId(), key -> new ArrayDeque<>());
        if (lane.size() >= SamuraiSettings.dialogueQueueSize()) {
            runtime.getController().speak(runtime.getInstance(), action.targetPlayerName(), AIError.QUEUE_FULL.fallback());
            return;
        }
        UUID conversation = conversations.computeIfAbsent(new Conversation(runtime.getId(), action.targetPlayerId()), key -> UUID.randomUUID());
        Turn turn = new Turn(conversation, runtime, action);
        lane.addLast(turn);
        if (lane.size() == 1) start(turn);
    }
    private void start(Turn turn) {
        if (!live(turn.runtime)) { cancelNpc(turn.runtime.getId()); return; }
        NPCRuntime runtime = turn.runtime;
        TalkAction action = turn.action;
        String message = sanitize(action.playerMessage());
        if (message.isBlank()) { complete(turn, AIResponse.failure(AIError.CANCELLED)); return; }
        // Capture perception and immutable social/memory snapshots when this turn actually starts.
        var world = perception.perceive(runtime);
        var history = new yadi.samuraiai.memory.ConversationMemory(SamuraiSettings.memoryMessages());
        runtime.getConversationMemory().snapshot().forEach(history::add);
        AIContext context = AIContext.builder().npcName(runtime.getName())
                .npcType(runtime.getDefinition().getDisplayNamePrefix()).playerName(action.targetPlayerName())
                .playerMessage(message).personality(runtime.getPersonality()).memory(history)
                .emotions(runtime.getEmotionState().snapshot())
                .relationship(RelationshipService.getInstance().find(runtime.getId(), action.targetPlayerId()).orElse(null))
                .nearby(world.getPerceivedEntities().stream().filter(p -> !p.id().equals(action.targetPlayerId())).map(p -> p.name()).toList())
                .timeOfDay(yadi.samuraiai.living.server.LivingService.getInstance().timeOfDay(runtime.getId()).orElse("hora del mundo " + Math.floorMod(world.getWorldTime(), 24000)))
                .world(yadi.samuraiai.living.server.LivingService.getInstance().promptLines(runtime.getId(), action.targetPlayerId())).build();
        NPCEventBus.getInstance().post(new DialogueStartedEvent(runtime.getId(), turn.conversation, turn.id));
        Executor delivery = ServerScheduler.getInstance().executor();
        turn.request = queue.enqueue(runtime.getId(), context);
        turn.request.future().whenComplete((response, error) -> delivery.execute(() ->
                complete(turn, response == null ? AIResponse.failure(AIError.INTERNAL) : response)));
    }
    private void complete(Turn turn, AIResponse response) {
        ServerScheduler.getInstance().requireServerThread();
        Deque<Turn> lane = lanes.get(turn.runtime.getId());
        if (!live(turn.runtime) || lane == null || lane.peekFirst() != turn) return;
        try {
            if (response.success()) {
                turn.runtime.getConversationMemory().addExchange(turn.action.targetPlayerName() + ": " + sanitize(turn.action.playerMessage()), response.text());
                EmotionService.getInstance().conversation(turn.runtime);
                if (turn.action.targetPlayerId() != null) {
                    RelationshipService.getInstance().adjustTrust(turn.runtime.getId(), turn.action.targetPlayerId(), 2);
                    yadi.samuraiai.ai.cognition.world.CognitionService.getInstance().conversation(turn.runtime, turn.action.targetPlayerId(), turn.action.targetPlayerName());
                    yadi.samuraiai.living.server.LivingService.getInstance().conversation(turn.runtime, turn.action.targetPlayerId(), turn.action.targetPlayerName());
                }
                NPCEventBus.getInstance().post(new MemoryCreatedEvent(turn.runtime.getId(), "conversation exchange"));
                NPCEventBus.getInstance().post(new AIRequestCompletedEvent(turn.runtime.getId(), turn.id));
            } else {
                NPCEventBus.getInstance().post(new AIRequestFailedEvent(turn.runtime.getId(), turn.id, response.error()));
            }
            if (response.hasSpeakableText() && live(turn.runtime))
                turn.runtime.getController().speak(turn.runtime.getInstance(), turn.action.targetPlayerName(), response.text());
            NPCEventBus.getInstance().post(new DialogueEndedEvent(turn.runtime.getId(), turn.conversation, turn.id, response.success()));
        } finally {
            // A listener may have removed this NPC during event delivery.
            if (lanes.get(turn.runtime.getId()) == lane && lane.peekFirst() == turn) {
                lane.removeFirst();
                if (lane.isEmpty()) lanes.remove(turn.runtime.getId());
                else start(lane.peekFirst());
            }
        }
    }
    public void cancelNpc(UUID npcId) {
        ServerScheduler.getInstance().requireServerThread();
        Deque<Turn> lane = lanes.remove(npcId);
        conversations.keySet().removeIf(key -> key.npc().equals(npcId));
        if (lane != null) for (Turn turn : lane) if (turn.request != null) turn.request.cancel();
        queue.cancelNpc(npcId);
    }
    public void clear() {
        ServerScheduler.getInstance().requireServerThread();
        for (UUID id : List.copyOf(lanes.keySet())) cancelNpc(id);
        conversations.clear();
    }
    public int pending(UUID npcId) { return lanes.getOrDefault(npcId, new ArrayDeque<>()).size(); }
    private static boolean live(NPCRuntime runtime) {
        return runtime.isActive() && NPCManager.getInstance().find(runtime.getId()).orElse(null) == runtime;
    }
    public static String sanitize(String message) {
        String text = message == null ? "" : message.replaceAll("\\p{Cc}", " ").replaceAll("\\s{2,}", " ").trim();
        return text.substring(0, Math.min(text.length(), SamuraiSettings.maxMessageLength()));
    }
}
