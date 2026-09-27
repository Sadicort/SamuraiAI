package yadi.samuraiai.ai.scheduler.zone;

import java.util.UUID;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;

/**
 * Finds the place a routine happens in. It prefers a zone of the routine's kind (through the zone scheduler); with no such
 * zone it falls back to a spot on a ring around the NPC's home, at a direction unique to the routine, so that even an
 * unconfigured world gives every routine its own recognisable place. Without a home the NPC stays where it is.
 */
public final class PlaceResolver {
    private final SchedulerSettings settings;
    private final ZoneScheduler zones;

    public PlaceResolver(SchedulerSettings settings, ZoneScheduler zones) { this.settings = settings; this.zones = zones; }

    public Place resolve(RoutineType routine, String dimension, double x, double z, Place home, UUID npc, String group, DayPeriod period, int patrolIndex) {
        var zone = zones.choose(dimension, routine.zoneKind(), x, z, npc, group, period);
        if (zone.isPresent()) {
            if (routine == RoutineType.PATROL) return zone.get().patrolPoint(patrolIndex, settings.patrolPoints());
            return zone.get().place();
        }
        if (home == null) return null;
        if (routine == RoutineType.WAKE || routine == RoutineType.SLEEP) return home.withRadius(Math.max(2.0D, home.radius()));
        int points = settings.patrolPoints();
        if (routine == RoutineType.PATROL) {
            double angle = 2.0D * Math.PI * Math.floorMod(patrolIndex, points) / points;
            return new Place(home.dimension(), home.x() + Math.cos(angle) * settings.fallbackRing(), home.y(), home.z() + Math.sin(angle) * settings.fallbackRing(), 1.5D, null);
        }
        double angle = 2.0D * Math.PI * routine.ordinal() / RoutineType.values().length;
        double ring = settings.fallbackRing() * (routine == RoutineType.GUARD ? 1.0D : 0.6D);
        return new Place(home.dimension(), home.x() + Math.cos(angle) * ring, home.y(), home.z() + Math.sin(angle) * ring, 2.0D, null);
    }
}
