package yadi.samuraiai.ai.knowledge.worldmemory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.knowledge.history.HistoricalEvent;
import yadi.samuraiai.ai.knowledge.history.HistoryType;

/** The collective memory of public events: it turns the events communities recorded into heroes and traitors (who did what the world remembers), and keeps the world timeline. */
public final class WorldMemoryEngine {
    private final Map<UUID, Legend> legends = new HashMap<>();

    /** Updates the legends of the people named in a public event. */
    public void recorded(HistoricalEvent e) {
        double hero = 0, traitor = 0;
        switch (e.type()) {
            case HERO_ACT -> hero = e.significance();
            case BETRAYAL -> traitor = e.significance();
            case BATTLE -> hero = e.significance() * 0.4D;
            default -> { return; }
        }
        for (EntityRef p : e.participants()) {
            if (p.kind() != yadi.samuraiai.ai.cognition.model.EntityKind.PLAYER && p.kind() != yadi.samuraiai.ai.cognition.model.EntityKind.NPC) continue;
            // The first named participant is the actor of the deed; the others only shared in it.
            boolean actor = p.equals(e.participants().get(0));
            legends.computeIfAbsent(p.id(), id -> new Legend(p, 0, 0, 0)).add(actor ? hero : hero * 0.3D, actor ? traitor : 0.0D);
        }
    }

    public Optional<Legend> legend(UUID subject) { return Optional.ofNullable(legends.get(subject)); }
    public Collection<Legend> all() { return List.copyOf(legends.values()); }
    public void put(Legend legend) { legends.put(legend.id(), legend); }

    public List<Legend> heroes(int limit) { return rank(true, limit); }
    public List<Legend> traitors(int limit) { return rank(false, limit); }

    private List<Legend> rank(boolean hero, int limit) {
        List<Legend> result = new ArrayList<>();
        for (Legend l : legends.values()) if (hero ? l.hero() : l.traitor()) result.add(l);
        result.sort((a, b) -> Double.compare(hero ? b.heroism() : b.treachery(), hero ? a.heroism() : a.treachery()));
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    public void clear() { legends.clear(); }
    public static boolean counts(HistoryType type) { return type == HistoryType.HERO_ACT || type == HistoryType.BETRAYAL || type == HistoryType.BATTLE; }
}
