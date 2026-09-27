package yadi.samuraiai.ai.perception.engine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.perception.attention.AttentionManager;
import yadi.samuraiai.ai.perception.awareness.AwarenessEngine;
import yadi.samuraiai.ai.perception.awareness.InterestEngine;
import yadi.samuraiai.ai.perception.awareness.SuspicionMeter;
import yadi.samuraiai.ai.perception.awareness.ThreatEngine;
import yadi.samuraiai.ai.perception.environment.EnvironmentSnapshot;
import yadi.samuraiai.ai.perception.hearing.HeardSound;
import yadi.samuraiai.ai.perception.memory.PerceptionMemory;
import yadi.samuraiai.ai.perception.sensors.SensorRecord;
import yadi.samuraiai.ai.perception.sensors.SensorType;
import yadi.samuraiai.ai.perception.vision.VisualTrack;

/**
 * Everything one NPC knows and has in progress in its perception: tracks, memory, attention, suspicion, interest, threat,
 * awareness, sensor bookkeeping and queued inputs. Owned by the perception runtime, one per NPC, server-thread only.
 * Nothing here decides what to do; it is the NPC's belief about the world.
 */
public final class PerceptionState {
    public final UUID npcId;
    public final Map<UUID, VisualTrack> tracks = new HashMap<>();
    public final PerceptionMemory memory = new PerceptionMemory();
    public final AttentionManager attention = new AttentionManager();
    public final SuspicionMeter suspicion = new SuspicionMeter();
    public final InterestEngine interest = new InterestEngine();
    public final ThreatEngine threat = new ThreatEngine();
    public final AwarenessEngine awareness = new AwarenessEngine();
    public final Map<String, Long> cooldowns = new HashMap<>();
    public final Map<SensorType, SensorRecord> sensors = new EnumMap<>(SensorType.class);

    // per-sensor working data
    public List<SensedEntity> catalog = List.of();
    public long catalogTick = -1;
    public long lastSoundTick = 0;
    public EnvironmentSnapshot environment = EnvironmentSnapshot.UNKNOWN;
    public boolean environmentKnown;
    public int lightLevel = 15;
    public final Map<Long, BlockInterest> blockMemory = new HashMap<>();
    public final Deque<Long> dirtyBlocks = new ArrayDeque<>();
    public int blockCursor;
    public final Map<UUID, double[]> movement = new HashMap<>();
    public final Deque<Reports.DamageReport> pendingDamage = new ArrayDeque<>();
    public final Deque<Reports.ConversationReport> pendingConversation = new ArrayDeque<>();
    public final Deque<Reports.VoiceReport> pendingVoice = new ArrayDeque<>();

    // pass bookkeeping
    public long lastPassTick = -1, lastHeardTick = -1;
    /** Where and in which dimension this NPC last perceived from; lets world events find the NPCs they affect without asking the world. */
    public double lastX, lastY, lastZ;
    public float lastYaw;
    public String dimension = "";
    public List<HeardSound> recentSounds = List.of();
    public PerceptionSnapshot snapshot;
    public long passes, stimuliAccepted;

    public PerceptionState(UUID npcId) {
        this.npcId = npcId;
        for (SensorType type : SensorType.values()) sensors.put(type, new SensorRecord(type));
    }

    public SensorRecord record(SensorType type) { return sensors.get(type); }

    /** Queues a block position whose state may have changed, so the block sensor rechecks it at once. */
    public void markBlockDirty(int x, int y, int z) {
        if (dirtyBlocks.size() < 256) dirtyBlocks.add(pack(x, y, z));
    }

    public static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
    public static int unpackX(long p) { return (int) (p >> 38) << 6 >> 6; }
    public static int unpackZ(long p) { return (int) ((p >> 12) & 0x3FFFFFF) << 6 >> 6; }
    public static int unpackY(long p) { return (int) (p & 0xFFF) << 20 >> 20; }

    public void reset() {
        tracks.clear(); memory.clear(); attention.reset(); suspicion.reset(); interest.reset(); threat.reset(); awareness.reset();
        cooldowns.clear(); catalog = List.of(); catalogTick = -1; blockMemory.clear(); dirtyBlocks.clear(); movement.clear();
        pendingDamage.clear(); pendingConversation.clear(); pendingVoice.clear(); recentSounds = List.of(); snapshot = null;
    }
}
