package yadi.samuraiai.living.server;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import yadi.samuraiai.ai.scheduler.world.SchedulerService;
import yadi.samuraiai.ai.scheduler.zone.Zone;
import yadi.samuraiai.ai.scheduler.zone.ZoneKind;
import yadi.samuraiai.living.sim.LivingWorld;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.buildings.BuildingKind;
import yadi.samuraiai.living.village.runtime.Village;

/**
 * Scheduler zones are real places someone marked in the world (a house, a temple, a market, a dojo). Inside a village they
 * become that village's buildings, linked by zone id, so professions find their workplace, homes their beds and the census
 * its capacity from what actually exists. The zone stays the source of truth for position; a building whose zone was removed
 * is marked abandoned, never deleted (its history stays), and comes back when a zone with its id is marked again. Zones
 * outside every village are retried later, since a village may be founded around them.
 */
final class ZoneBridge {
    private ZoneBridge() { }

    static Optional<BuildingKind> kindOf(ZoneKind zone) {
        return switch (zone) {
            case HOME -> Optional.of(BuildingKind.HOUSE);
            case WORK -> Optional.of(BuildingKind.WORKSHOP);
            case MARKET -> Optional.of(BuildingKind.MARKET);
            case TEMPLE -> Optional.of(BuildingKind.TEMPLE);
            case TRAINING -> Optional.of(BuildingKind.DOJO);
            case GUARD_POST -> Optional.of(BuildingKind.GUARD_POST);
            case DINING -> Optional.of(BuildingKind.KITCHEN);
            case PLAZA -> Optional.of(BuildingKind.PLAZA);
            case PATROL_ROUTE, REST_AREA -> Optional.empty();
        };
    }

    /** Links new zones to buildings and marks buildings of vanished zones abandoned. Returns how many buildings changed. */
    static int importZones(LivingWorld world, Map<String, Long> seen) {
        var registry = SchedulerService.getInstance().scheduler().zoneRegistry();
        Map<String, Building> linked = new java.util.HashMap<>();
        for (Village v : world.villages().villages())
            for (Building b : v.buildings().values()) if (!b.zoneId().isEmpty()) linked.put(b.zoneId(), b);
        int changed = 0;
        Set<String> live = new HashSet<>();
        for (Zone z : registry.all()) {
            live.add(z.id());
            Building existing = linked.get(z.id());
            if (existing != null) {
                // the zone is the source of truth for position; a zone marked again brings its abandoned building back
                if (Math.abs(existing.x() - z.x()) > 0.5 || Math.abs(existing.z() - z.z()) > 0.5 || Math.abs(existing.radius() - z.radius()) > 0.5) { existing.move(z.x(), z.y(), z.z(), z.radius()); world.villages().village(existing.village()).ifPresent(Village::markDirty); changed++; }
                if (existing.state() == Building.State.ABANDONED && world.villages().setBuildingState(existing.id(), Building.State.BUILT, "zona " + z.id() + " restaurada")) changed++;
                continue;
            }
            if (seen.containsKey(z.id())) continue;
            Optional<BuildingKind> kind = kindOf(z.kind());
            if (kind.isEmpty()) { seen.put(z.id(), world.tickCount()); continue; }   // routes and rest areas are not buildings
            Optional<Village> village = world.villages().villageAt(z.dimension(), z.x(), z.z());
            if (village.isEmpty()) continue;   // retried later: a village may be founded around it
            world.villages().registerBuilding(village.get().id(), kind.get(), z.id(), z.dimension(), z.x(), z.y(), z.z(), z.radius(), Building.State.BUILT, z.id());
            changed++;
        }
        for (Village v : world.villages().villages())
            for (Building b : java.util.List.copyOf(v.buildings().values()))
                if (!b.zoneId().isEmpty() && !live.contains(b.zoneId()) && b.state() != Building.State.ABANDONED && b.state() != Building.State.DESTROYED) {
                    world.villages().setBuildingState(b.id(), Building.State.ABANDONED, "zona " + b.zoneId() + " eliminada");
                    changed++;
                }
        seen.keySet().retainAll(live);
        return changed;
    }
}
