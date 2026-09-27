package yadi.samuraiai.ai.perception.engine;

import java.util.UUID;

/** Small event-driven inputs queued for an NPC's damage, conversation and voice sensors. */
public final class Reports {
    private Reports() { }

    /** The NPC was hurt. {@code source} may be null (fall, fire); the position is where the hit came from when known. */
    public record DamageReport(UUID source, String sourceName, double x, double y, double z, double amount, long tick) { }

    /** A player addressed the NPC in text. */
    public record ConversationReport(UUID speaker, String speakerName, double x, double y, double z, int length, long tick) { }

    /** A player spoke aloud near the NPC (server-side voice input, a future channel). */
    public record VoiceReport(UUID speaker, String speakerName, double x, double y, double z, double loudness, long tick) { }
}
