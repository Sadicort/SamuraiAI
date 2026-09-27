package yadi.samuraiai.living.quest.campaigns;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A campaign: a story told over several quests (a war, a trade route to reopen, a temple's rites, a clan feud, a family's
 * lost heirloom). It keeps its stages (template ids), which one is running, the quest of each stage, its state and the
 * condition key it came from, and it persists across restarts.
 */
public final class Campaign {
    public enum Type { WAR, TEMPLE, CLAN, TRADE, FAMILY }
    public enum State { ACTIVE, COMPLETED, FAILED }

    private final UUID id, settlement;
    private final Type type;
    private final String title, originKey;
    private final List<String> stages;
    private final List<UUID> quests = new ArrayList<>();
    private int stage;
    private State state = State.ACTIVE;
    private final long started;
    private long ended;
    private UUID player;

    public Campaign(UUID id, Type type, String title, List<String> stages, String originKey, UUID settlement, long started) {
        this.id = id; this.type = type; this.title = title; this.stages = List.copyOf(stages); this.originKey = originKey; this.settlement = settlement; this.started = started;
    }

    public UUID id() { return id; }
    public Type type() { return type; }
    public String title() { return title; }
    public List<String> stages() { return stages; }
    public String originKey() { return originKey; }
    public UUID settlement() { return settlement; }
    public List<UUID> quests() { return quests; }
    public int stage() { return stage; }
    public String currentTemplate() { return stage < stages.size() ? stages.get(stage) : ""; }
    public boolean last() { return stage >= stages.size() - 1; }
    public void advance() { stage++; }
    public State state() { return state; }
    public void state(State s, long at) { state = s; ended = at; }
    public long started() { return started; }
    public long ended() { return ended; }
    public UUID player() { return player; }
    public void player(UUID p) { player = p; }
    public void restore(int s, State st, long e, UUID p, List<UUID> q) { stage = s; state = st; ended = e; player = p; quests.clear(); quests.addAll(q); }
}
