package yadi.samuraiai.living.sim;

import java.util.Set;
import java.util.UUID;

/**
 * What the living world needs from the rest of SamuraiAI, gathered in one interface the Forge adapter implements over the
 * cognitive layer (knowledge communities, relationships, emotions, memory, knowledge) and the players. Tests use
 * {@link #NEUTRAL}. The hub turns these into the ports of each engine; no engine sees this interface.
 */
public interface Outside {
    // ---- knowledge communities (collective knowledge, culture, standing stay in the Knowledge Engine)
    String ensureCommunity(String key, String name, String culture, String dimension, double x, double y, double z, double radius);
    void joinCommunity(UUID npc, String key, boolean leader);
    void leaveCommunity(UUID npc, String key);
    void rememberInCommunity(String key, String historyType, String title, double significance, long minute, UUID actor);
    double standing(UUID subject, String communityKey, String context);
    void adjustStanding(UUID subject, String subjectName, String communityKey, String context, double amount);

    // ---- relationships, emotions, memory, knowledge
    double trust(UUID npc, UUID other);
    double respect(UUID npc, UUID other);
    String mood(UUID npc);
    /** An experience for the cognitive layer ({@code kind} is an ExperienceKind name: HELPED_ME, BETRAYED, HONOR_OBSERVED, ATTENDED_RITUAL...; an unknown name is logged and ignored). */
    void experience(UUID npc, UUID other, String otherName, String kind, String note);
    void learn(UUID npc, String key, String text, UUID teacher);
    double teachingQuality(UUID master, UUID disciple);
    double affinity(UUID npc, String profession);
    /** NPCs of a community that feel strong gratitude towards a player (for memory quests), npc → player. */
    java.util.Map<UUID, UUID> grateful(String communityKey);
    /** Pairs of members of a community who hold a strong grudge against each other (rivalry in their relationships), each pair once. */
    java.util.List<UUID[]> rivals(String communityKey);

    // ---- players
    void tell(UUID player, String text);
    /** Hands goods to a player as items; returns how many units could be given. */
    double giveItems(UUID player, String resource, double quantity);
    Set<UUID> playersNear(String dimension, double x, double z, double radius);

    Outside NEUTRAL = new Outside() {
        @Override public String ensureCommunity(String key, String name, String culture, String dimension, double x, double y, double z, double radius) { return key; }
        @Override public void joinCommunity(UUID npc, String key, boolean leader) { }
        @Override public void leaveCommunity(UUID npc, String key) { }
        @Override public void rememberInCommunity(String key, String historyType, String title, double significance, long minute, UUID actor) { }
        @Override public double standing(UUID subject, String communityKey, String context) { return 0; }
        @Override public void adjustStanding(UUID subject, String subjectName, String communityKey, String context, double amount) { }
        @Override public double trust(UUID npc, UUID other) { return 50; }
        @Override public double respect(UUID npc, UUID other) { return 50; }
        @Override public String mood(UUID npc) { return "CALM"; }
        @Override public void experience(UUID npc, UUID other, String otherName, String kind, String note) { }
        @Override public void learn(UUID npc, String key, String text, UUID teacher) { }
        @Override public double teachingQuality(UUID master, UUID disciple) { return 0.6; }
        @Override public double affinity(UUID npc, String profession) { return 0.5; }
        @Override public java.util.Map<UUID, UUID> grateful(String communityKey) { return java.util.Map.of(); }
        @Override public java.util.List<UUID[]> rivals(String communityKey) { return java.util.List.of(); }
        @Override public void tell(UUID player, String text) { }
        @Override public double giveItems(UUID player, String resource, double quantity) { return quantity; }
        @Override public Set<UUID> playersNear(String dimension, double x, double z, double radius) { return Set.of(); }
    };
}
