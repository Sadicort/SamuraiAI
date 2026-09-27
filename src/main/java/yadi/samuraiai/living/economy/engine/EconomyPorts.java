package yadi.samuraiai.living.economy.engine;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.Season;
import yadi.samuraiai.living.core.WeatherKind;

/**
 * What the Economy Engine needs from outside: the world (deposits, animals, roads, danger), the villages (who works at what,
 * how many mouths, the market's life), the calendar (season, agriculture, weather, festivals), trust between people for
 * discounts, and the chronicle. The hub implements them; the Economy imports no other engine. Villages are not duplicated
 * inside the economy: it only asks for these numbers.
 */
public final class EconomyPorts {
    private EconomyPorts() { }

    public record Worker(UUID npc, String name, String profession, double totalHours) { }
    public record Leg(UUID edge, double length, double danger) { }
    public record RouteInfo(List<Leg> legs, int bridges, long version) { }

    public interface World {
        String regionScope(UUID settlement);
        double extract(UUID region, String resource, double amount);
        double harvestAnimals(UUID region, String resource, double amount, double maxShare);
        Optional<RouteInfo> route(UUID from, UUID to);
        long roadVersion();
        boolean blocked(UUID edge);
        double edgeDanger(UUID edge);
        double regionDanger(UUID region);
        double distance(UUID settlementA, UUID settlementB);
        List<UUID> settlements();
        void travelled(UUID edge, long minute);
    }

    public interface Villages {
        List<Worker> workers(UUID settlement);
        int population(UUID settlement);
        int children(UUID settlement);
        int visitors(UUID settlement);
        boolean marketOpen(UUID settlement);
        int footfall(UUID settlement);
        int buildings(UUID settlement, String kind);
        String culture(UUID settlement);
    }

    public interface Calendar {
        double foodFactor();
        double fuelFactor();
        double tradeFactor();
        double travelFactor();
        /** Share of a full day's output of a resource in a region this month (the agricultural calendar); 1 for non-crops. */
        double agricultureFactor(String resource, String regionScope);
        boolean harvestSeason(String resource);
        boolean offSeason(String resource);
        WeatherKind weather(String regionScope);
        /** Resource demand multipliers of the festivals running for a culture. */
        Map<String, Double> festivalDemand(String culture);
    }

    public interface Relations { double trust01(UUID a, UUID b); }

    public interface Chronicle { void record(long minute, String category, String title, String detail, Set<String> scopes, double significance, Provenance source); }

    public static final Relations NEUTRAL_TRUST = (a, b) -> 0.5D;
    public static final Chronicle SILENT = (m, c, t, d, s, x, p) -> { };
}
