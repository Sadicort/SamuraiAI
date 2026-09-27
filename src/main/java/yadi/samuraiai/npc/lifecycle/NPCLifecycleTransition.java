package yadi.samuraiai.npc.lifecycle;

import java.time.Instant;
import java.util.UUID;

public record NPCLifecycleTransition(UUID npcId, NPCLifecycleState previous,
                                     NPCLifecycleState next, Instant timestamp) { }
