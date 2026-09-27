package yadi.samuraiai.memory;

import java.util.UUID;

/** Stable capability contract; non-conversation kinds are intentionally unavailable in phase 1. */
public interface MemoryAccess {
    ConversationMemory getMemory(UUID npcId);
    default boolean supports(MemoryKind kind) { return kind == MemoryKind.CONVERSATION; }
    void forget(UUID npcId);
}
