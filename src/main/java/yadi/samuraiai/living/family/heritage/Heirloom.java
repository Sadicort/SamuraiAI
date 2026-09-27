package yadi.samuraiai.living.family.heritage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * An heirloom (the Heirloom Record): a thing whose history matters — a katana older than its owner, a founder's hammer. It
 * keeps who made or first owned it, who has it now, the family it belongs to, when it was made, every transfer and every
 * event it was part of. Its symbolic value grows with the generations it passes through and the history it gathers.
 * {@code itemId} is the id stamped on the physical item when there is one.
 */
public final class Heirloom {
    public record TransferRecord(UUID from, UUID to, long minute, String reason) { }
    public record HistoricEvent(long minute, String text) { }

    private final UUID itemId;
    private final String name, kind;
    private final UUID originalOwner, family;
    private final long created;
    private UUID currentOwner;
    private boolean lost;
    private double reputation;
    private String epithet = "";
    private final List<TransferRecord> transfers = new ArrayList<>();
    private final List<HistoricEvent> events = new ArrayList<>();

    public Heirloom(UUID itemId, String name, String kind, UUID originalOwner, UUID family, long created) {
        this.itemId = itemId; this.name = name; this.kind = kind == null ? "objeto" : kind; this.originalOwner = originalOwner; this.family = family; this.created = created;
        this.currentOwner = originalOwner;
    }

    public UUID itemId() { return itemId; }
    public String name() { return name; }
    public String kind() { return kind; }
    public UUID originalOwner() { return originalOwner; }
    public UUID family() { return family; }
    public long created() { return created; }
    public UUID currentOwner() { return currentOwner; }
    public boolean lost() { return lost; }
    public void lost(boolean v) { lost = v; }
    public double reputation() { return reputation; }
    public void reputation(double v) { reputation = v; }
    /** A name history gave it beyond its plain kind ("Griefsteel"), once it has passed through enough hands and events. */
    public String epithet() { return epithet; }
    public void epithet(String v) { epithet = v == null ? "" : v; }
    /** How the item is known: its epithet if it earned one, otherwise its registered name. */
    public String displayName() { return epithet.isEmpty() ? name : epithet; }
    public List<TransferRecord> transfers() { return transfers; }
    public List<HistoricEvent> events() { return events; }

    public void transfer(UUID to, long minute, String reason) { transfers.add(new TransferRecord(currentOwner, to, minute, reason)); currentOwner = to; }
    public void event(long minute, String text) { events.add(new HistoricEvent(minute, text)); while (events.size() > 50) events.remove(0); }

    /** How much it means: grows with each generation it passed through and each historic event it was part of. */
    public double symbolicValue() { return 1.0 + transfers.size() * 1.5 + events.size() * 0.75 + Math.max(0, reputation); }

    public void restore(UUID owner, boolean wasLost, double rep, List<TransferRecord> t, List<HistoricEvent> e, String savedEpithet) {
        currentOwner = owner; lost = wasLost; reputation = rep; transfers.clear(); transfers.addAll(t); events.clear(); events.addAll(e); epithet = savedEpithet == null ? "" : savedEpithet;
    }
}
