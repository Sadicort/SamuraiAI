package yadi.samuraiai.ai.perception.awareness;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.ai.perception.memory.MemoryEntry;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.memory.PerceptionMemory;

/**
 * The NPC's mental map of what it believes is around it: players, NPCs, dangers, objects and recent events, each with the
 * place it was last known and how strongly it is still remembered. Built from memory, so it contains exactly what the NPC
 * could know and nothing it did not perceive.
 */
public record AwarenessMap(List<Entry> players, List<Entry> npcs, List<Entry> dangers, List<Entry> objects, List<Entry> events, long tick) {

    public record Entry(String kind, java.util.UUID subject, String label, double x, double y, double z, long ageTicks, double strength) { }

    public static AwarenessMap from(PerceptionMemory memory, long tick) {
        List<Entry> players = new ArrayList<>(), npcs = new ArrayList<>(), dangers = new ArrayList<>(), objects = new ArrayList<>(), events = new ArrayList<>();
        for (MemoryEntry e : memory.all()) {
            Entry entry = new Entry(e.detail, e.subject, e.label, e.x, e.y, e.z, e.ageTicks(tick), e.strength);
            switch (e.kind) {
                case DANGER -> dangers.add(entry);
                case AUDITORY, ENVIRONMENTAL -> events.add(entry);
                case INTEREST -> objects.add(entry);
                case VISUAL, SOCIAL -> {
                    if (e.detail.startsWith("PLAYER")) players.add(entry);
                    else if (e.detail.startsWith("NPC")) npcs.add(entry);
                    else objects.add(entry);
                }
            }
        }
        for (List<Entry> list : List.of(players, npcs, dangers, objects, events)) list.sort(java.util.Comparator.comparingDouble(Entry::strength).reversed());
        return new AwarenessMap(List.copyOf(players), List.copyOf(npcs), List.copyOf(dangers), List.copyOf(objects), List.copyOf(events), tick);
    }

    public int total() { return players.size() + npcs.size() + dangers.size() + objects.size() + events.size(); }
    public boolean isEmpty() { return total() == 0; }
}
