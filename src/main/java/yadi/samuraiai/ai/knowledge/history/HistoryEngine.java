package yadi.samuraiai.ai.knowledge.history;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;

/** Keeps a bounded, time-ordered list of historical events: when full, the least significant (oldest among equals) is dropped, so the great events are the ones that last. */
public final class HistoryEngine {
    public boolean add(List<HistoricalEvent> history, HistoricalEvent event, int max) {
        for (HistoricalEvent e : history) if (e.id().equals(event.id())) return false;
        int at = history.size();
        while (at > 0 && history.get(at - 1).at() > event.at()) at--;
        history.add(at, event);
        while (history.size() > max) {
            int drop = 0;
            for (int i = 1; i < history.size(); i++) if (history.get(i).significance() < history.get(drop).significance()) drop = i;
            history.remove(drop);
        }
        return true;
    }

    public List<HistoricalEvent> between(List<HistoricalEvent> history, long from, long to) {
        List<HistoricalEvent> result = new ArrayList<>();
        for (HistoricalEvent e : history) if (e.at() >= from && e.at() <= to) result.add(e);
        return result;
    }

    public List<HistoricalEvent> involving(List<HistoricalEvent> history, UUID entity) {
        List<HistoricalEvent> result = new ArrayList<>();
        for (HistoricalEvent e : history) if (e.participants().stream().anyMatch(p -> p.id().equals(entity))) result.add(e);
        return result;
    }

    public List<HistoricalEvent> ofType(List<HistoricalEvent> history, HistoryType type) {
        List<HistoricalEvent> result = new ArrayList<>();
        for (HistoricalEvent e : history) if (e.type() == type) result.add(e);
        return result;
    }

    public static boolean significant(double significance, KnowledgeSettings s) { return significance >= s.publicSignificance(); }
}
