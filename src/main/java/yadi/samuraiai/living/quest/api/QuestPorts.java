package yadi.samuraiai.living.quest.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.Provenance;

/**
 * What the Quest Engine needs from the rest of the living world. Quests never live inside an NPC: the giver is an id and a
 * name; rewards are paid by the Economy from real treasuries and stores; reputation is the Knowledge communities' standing;
 * gratitude is an experience for the cognitive layer; consequences go to the engine that owns what they change. The hub
 * implements these ports.
 */
public final class QuestPorts {
    private QuestPorts() { }

    public record Giver(UUID npc, String name, String profession) { }
    public record Place(String dimension, double x, double y, double z) { }

    public interface World {
        String settlementName(UUID settlement);
        Optional<Place> settlementPlace(UUID settlement);
        String regionName(UUID region);
        Optional<Place> regionCenter(UUID region);
        Optional<UUID> neighbour(UUID settlement);
        boolean resolveEvent(UUID event, boolean success, String by, String outcome);
        Optional<UUID> spawnEvent(String type, UUID region, UUID settlement, double severity, String cause);
        boolean buildRoad(UUID fromSettlement, UUID toSettlement);
        String seasonName();
        String weatherName(UUID region);
        String moonName();
    }

    public interface Villages {
        Optional<Giver> giver(UUID settlement, List<String> professions);
        Optional<Place> building(UUID settlement, String kind);
        void renown(UUID settlement, double delta, String cause);
        void unrest(UUID settlement, double delta);
        boolean buildHouse(UUID settlement);
        String culture(UUID settlement);
    }

    public interface Economy {
        double reward(UUID settlement, UUID player, String playerName, double coins, String reason);
        /** Takes goods from the settlement's stores for the player (the adapter turns them into items). Returns the quantity given. */
        double giveItems(UUID settlement, UUID player, String resource, double quantity, String reason);
        double price(UUID settlement, String resource);
        boolean isFood(String resource);
        void loot(UUID settlement, double fraction, String cause);
    }

    public interface Social {
        double trust(UUID npc, UUID player);
        String mood(UUID npc);
        double standing(UUID player, UUID settlement, String context);
        void adjustStanding(UUID player, String playerName, UUID settlement, String context, double amount);
        void experience(UUID npc, UUID player, String playerName, String kind, String note);
        void witnesses(UUID settlement, UUID player, String playerName, String kind, String note);
    }

    public interface Families {
        void honor(String subject, double delta, String cause);
        void memory(String subject, String text);
        void mentorship(UUID master, UUID disciple, double progress);
    }

    public interface Chronicle { void record(long minute, String category, String title, String detail, Set<String> scopes, double significance, Provenance source); }

    /** Messages to players (chat), and items handed over. */
    public interface Notifier { void tell(UUID player, String text); }
}
