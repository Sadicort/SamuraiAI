package yadi.samuraiai.memory;

import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.ollama.model.ChatMessage;
import yadi.samuraiai.ollama.model.ChatRole;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Rolling window of one NPC's recent dialogue, oldest entries dropped first.
 *
 * <p>Every method is synchronised and {@link #snapshot()} hands back an
 * immutable copy. That matters: replies are appended from the HTTP
 * completion thread while the prompt for the *next* turn is built on the
 * server thread, and the previous unsynchronised {@code ArrayList} could
 * throw {@link java.util.ConcurrentModificationException} mid-conversation
 * or silently lose a turn.
 *
 * <p>An {@link ArrayDeque} replaces the old list because trimming used
 * {@code remove(0)}, which is O(n) on an ArrayList; here it is O(1).
 */
public class ConversationMemory {

    private final Deque<ChatMessage> messages = new ArrayDeque<>();

    /**
     * Per-instance override of the configured window. Null means "follow
     * whatever the config currently says", which is the normal case and lets
     * a config reload take effect on already-spawned NPCs.
     */
    private final Integer maxMessagesOverride;
    private final boolean serverOwned;

    public ConversationMemory() {
        this(null);
    }

    public ConversationMemory(Integer maxMessagesOverride) {
        this(maxMessagesOverride, false);
    }

    ConversationMemory(Integer maxMessagesOverride, boolean serverOwned) {
        this.maxMessagesOverride = maxMessagesOverride;
        this.serverOwned = serverOwned;
    }

    private void requireOwner() {
        if (serverOwned) yadi.samuraiai.runtime.ServerScheduler.getInstance().requireServerThread();
    }

    public synchronized void add(ChatMessage message) {
        requireOwner();
        messages.addLast(Objects.requireNonNull(message, "message"));
        trim();
    }

    public void add(ChatRole role, String content) {
        add(new ChatMessage(role, content));
    }

    /**
     * Appends a full player/NPC exchange atomically, so a concurrent reader
     * can never observe a question without its answer.
     */
    public synchronized void addExchange(String playerMessage, String npcReply) {
        requireOwner();
        messages.addLast(ChatMessage.user(playerMessage));
        messages.addLast(ChatMessage.assistant(npcReply));
        trim();
    }

    /** Immutable point-in-time copy, safe to iterate off-thread. */
    public synchronized List<ChatMessage> snapshot() {
        return List.copyOf(messages);
    }

    /**
     * @deprecated retained for source compatibility; prefer {@link #snapshot()},
     *             whose name makes the copy explicit.
     */
    @Deprecated
    public List<ChatMessage> getMessages() {
        return snapshot();
    }

    public synchronized Optional<ChatMessage> last() {
        return Optional.ofNullable(messages.peekLast());
    }

    public synchronized void clear() {
        requireOwner();
        messages.clear();
    }

    public synchronized int size() {
        return messages.size();
    }

    public synchronized boolean isEmpty() {
        return messages.isEmpty();
    }

    public int maxMessages() {
        return maxMessagesOverride != null ? maxMessagesOverride : SamuraiSettings.memoryMessages();
    }

    /** Caller must hold the monitor. */
    private void trim() {

        int limit = Math.max(2, maxMessages());

        while (messages.size() > limit) {
            messages.removeFirst();
        }
    }
}
