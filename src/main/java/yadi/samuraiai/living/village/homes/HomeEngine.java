package yadi.samuraiai.living.village.homes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.buildings.BuildingKind;
import yadi.samuraiai.living.village.runtime.Village;

/**
 * The Home Engine: gives each citizen a bed in a house with room left (the least crowded first, so households spread), a room,
 * a private zone and a few personal objects that fit their trade; answers who the neighbours are; frees the bed when a
 * citizen leaves. A village without a free bed reports homeless citizens (a need, and so a possible quest).
 */
public final class HomeEngine {
    private static final Map<String, List<String>> OBJECTS = Map.ofEntries(
            Map.entry("farmer", List.of("hoz gastada", "sombrero de paja", "semillas guardadas")),
            Map.entry("fisherman", List.of("red remendada", "caña de bambú")),
            Map.entry("blacksmith", List.of("martillo propio", "delantal de cuero")),
            Map.entry("carpenter", List.of("cepillo de carpintero", "caja de clavos")),
            Map.entry("merchant", List.of("ábaco", "libro de cuentas")),
            Map.entry("monk", List.of("rosario", "sutra copiado a mano")),
            Map.entry("guard", List.of("lanza de guardia", "farol")),
            Map.entry("samurai", List.of("soporte para katana", "caligrafía del maestro")),
            Map.entry("cook", List.of("cuchillo de cocina", "cuenco favorito")),
            Map.entry("hunter", List.of("arco", "pieles curtidas")));

    public int occupants(Village village, UUID house) {
        int n = 0;
        for (HomeRecord h : village.homes().values()) if (house.equals(h.house())) n++;
        return n;
    }

    /** Finds a bed for a citizen; empty when every house is full. */
    public Optional<HomeRecord> assign(Village village, UUID citizen, String profession, long now, Dice dice) {
        HomeRecord existing = village.homes().get(citizen);
        if (existing != null) return Optional.of(existing);
        Building best = null;
        int bestLoad = Integer.MAX_VALUE;
        for (Building b : village.buildingsOf(BuildingKind.HOUSE)) {
            if (!b.usable() && b.state() != Building.State.PLANNED) continue;
            int load = occupants(village, b.id());
            if (load >= b.capacity()) continue;
            if (load < bestLoad) { bestLoad = load; best = b; }
        }
        if (best == null) return Optional.empty();
        int slot = occupants(village, best.id());
        double angle = slot * Math.PI / 2;
        HomeRecord home = new HomeRecord(citizen, best.id(), "habitación " + (slot + 1), best.x() + Math.cos(angle) * Math.min(2.0D, best.radius() * 0.5D), best.y(),
                best.z() + Math.sin(angle) * Math.min(2.0D, best.radius() * 0.5D), Math.max(2.0D, best.radius()), now);
        List<String> pool = OBJECTS.getOrDefault(profession == null ? "" : profession, List.of("manta de lana", "cuenco de arroz", "amuleto del templo"));
        home.personalObjects().add(pool.get(dice.below("objects:" + citizen, 0, pool.size())));
        home.personalObjects().add("amuleto del templo");
        village.homes().put(citizen, home);
        village.markDirty();
        return Optional.of(home);
    }

    public void release(Village village, UUID citizen) { if (village.homes().remove(citizen) != null) village.markDirty(); }

    /** Citizens living within {@code radius} blocks (same house first). */
    public List<UUID> neighbours(Village village, UUID citizen, double radius) {
        HomeRecord mine = village.homes().get(citizen);
        List<UUID> out = new ArrayList<>();
        if (mine == null) return out;
        Building house = village.buildings().get(mine.house());
        for (HomeRecord other : village.homes().values()) {
            if (other.citizen().equals(citizen)) continue;
            if (other.house().equals(mine.house())) { out.add(0, other.citizen()); continue; }
            Building b = village.buildings().get(other.house());
            if (house != null && b != null && Math.hypot(house.x() - b.x(), house.z() - b.z()) <= radius) out.add(other.citizen());
        }
        return out;
    }

    public int homeless(Village village) {
        int n = 0;
        for (UUID c : village.citizens()) if (!village.homes().containsKey(c)) n++;
        return n;
    }

    public Map<UUID, Integer> occupancy(Village village) {
        Map<UUID, Integer> out = new HashMap<>();
        for (HomeRecord h : village.homes().values()) out.merge(h.house(), 1, Integer::sum);
        return out;
    }
}
