package yadi.samuraiai.living.village.runtime;

import java.util.ArrayList;
import java.util.List;
import yadi.samuraiai.living.core.Dice;
import yadi.samuraiai.living.village.buildings.BuildingKind;

/**
 * Village generation as a plan: where a new village's plaza, temple, market, houses, farms, wells, warehouse, workshops, dojo
 * and gates go. The plaza is the centre; the spiritual district lies north, the market east, the crafts west, the fields
 * south, the houses in a ring around the plaza and the gates on the edge. Deterministic from the village id. The plan
 * becomes buildings in state PLANNED (or BUILT for villages that only exist in the simulation); raising real blocks in the
 * world is not part of this engine.
 */
public final class VillagePlanner {
    public record Planned(BuildingKind kind, String name, double x, double z, double radius) { }

    private VillagePlanner() { }

    public static List<Planned> plan(String key, double cx, double cz, double radius, int houses, boolean temple, boolean market, Dice dice) {
        List<Planned> out = new ArrayList<>();
        double r = Math.max(24.0D, radius);
        out.add(new Planned(BuildingKind.PLAZA, "Plaza", cx, cz, 8));
        out.add(new Planned(BuildingKind.WELL, "Pozo de la plaza", cx + 4, cz + 4, 2));
        if (temple) out.add(new Planned(BuildingKind.TEMPLE, "Templo", cx, cz - r * 0.6, 10));
        if (market) {
            out.add(new Planned(BuildingKind.MARKET, "Mercado", cx + r * 0.45, cz, 9));
            out.add(new Planned(BuildingKind.WAREHOUSE, "Almacén", cx + r * 0.62, cz + 10, 6));
        } else out.add(new Planned(BuildingKind.WAREHOUSE, "Almacén", cx + r * 0.4, cz + 6, 6));
        out.add(new Planned(BuildingKind.KITCHEN, "Cocina común", cx - 10, cz + 8, 5));
        out.add(new Planned(BuildingKind.SMITHY, "Herrería", cx - r * 0.5, cz - 6, 5));
        out.add(new Planned(BuildingKind.CARPENTRY, "Carpintería", cx - r * 0.5, cz + 10, 5));
        out.add(new Planned(BuildingKind.DOJO, "Dojo", cx - r * 0.35, cz - r * 0.45, 7));
        out.add(new Planned(BuildingKind.GUARD_POST, "Puesto de guardia", cx + r * 0.3, cz - r * 0.55, 4));
        for (int i = 0; i < 3; i++) out.add(new Planned(BuildingKind.FARM, "Campo " + (i + 1), cx - r * 0.3 + i * r * 0.3, cz + r * 0.75, 9));
        double ring = r * 0.3;
        int n = Math.max(1, houses);
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n + dice.between("plan:" + key, i, -0.2, 0.2);
            double dist = ring + dice.between("plan-d:" + key, i, -4, 6);
            out.add(new Planned(BuildingKind.HOUSE, "Casa " + (i + 1), cx + Math.cos(angle) * dist, cz + Math.sin(angle) * dist, 4));
            if (i % 4 == 3) out.add(new Planned(BuildingKind.WELL, "Pozo " + (i / 4 + 1), cx + Math.cos(angle + 0.3) * (dist + 5), cz + Math.sin(angle + 0.3) * (dist + 5), 2));
        }
        out.add(new Planned(BuildingKind.GATE, "Puerta norte", cx, cz - r, 3));
        out.add(new Planned(BuildingKind.GATE, "Puerta sur", cx, cz + r, 3));
        return out;
    }
}
