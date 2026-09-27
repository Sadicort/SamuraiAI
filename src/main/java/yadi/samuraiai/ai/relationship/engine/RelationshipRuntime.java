package yadi.samuraiai.ai.relationship.engine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.relationship.model.PromiseRecord;
import yadi.samuraiai.ai.relationship.model.RelationshipRecord;
import yadi.samuraiai.ai.relationship.reputation.ReputationBook;

/** Everything one NPC feels about others: its relationships (directional, keyed by the other's UUID), its promises and the reputations it has heard. Never shared. */
public final class RelationshipRuntime {
    private final UUID npcId;
    private final Map<UUID, RelationshipRecord> byTarget = new LinkedHashMap<>();
    private final Map<UUID, PromiseRecord> promises = new LinkedHashMap<>();
    private final ReputationBook reputation = new ReputationBook();
    private boolean dirty, urgent;
    long lastDecay = Long.MIN_VALUE / 2;
    int cursor;
    private String storageState = "NEW";
    private long lastSaved;
    private long lastUpdate;
    private String lastUpdateNote = "";

    public RelationshipRuntime(UUID npcId) { this.npcId = npcId; }

    public UUID npcId() { return npcId; }
    public RelationshipRecord get(UUID target) { return byTarget.get(target); }
    public void put(RelationshipRecord r) { byTarget.put(r.target().id(), r); dirty = true; }
    public RelationshipRecord remove(UUID target) { dirty = true; return byTarget.remove(target); }
    public List<RelationshipRecord> all() { return new ArrayList<>(byTarget.values()); }
    public int size() { return byTarget.size(); }
    public Map<UUID, PromiseRecord> promises() { return promises; }
    public ReputationBook reputation() { return reputation; }

    public void markDirty() { dirty = true; }
    public void markUrgent() { dirty = true; urgent = true; }
    public boolean dirty() { return dirty; }
    public boolean urgent() { return urgent; }
    public void saved(long now) { dirty = false; urgent = false; lastSaved = now; storageState = "SAVED"; }
    public long lastSaved() { return lastSaved; }
    public String storageState() { return storageState; }
    public void storageState(String v) { storageState = v; }
    public long lastUpdate() { return lastUpdate; }
    public String lastUpdateNote() { return lastUpdateNote; }
    void updated(long at, String note) { lastUpdate = at; lastUpdateNote = note; }
}
