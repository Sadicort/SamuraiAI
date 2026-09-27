package yadi.samuraiai.living.village.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.citizens.Citizen;
import yadi.samuraiai.living.village.districts.District;
import yadi.samuraiai.living.village.engine.VillageEngine;
import yadi.samuraiai.living.village.homes.HomeRecord;
import yadi.samuraiai.living.village.life.VillageEventRecord;
import yadi.samuraiai.living.village.metrics.VillageMetrics;
import yadi.samuraiai.living.village.population.VillageCensus;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.village.visitors.VisitorRecord;
import yadi.samuraiai.living.core.Skill;

/** The Village Overlay in text form: buildings, districts, homes, professions, schedules, citizens, security, events, visitors. */
public final class VillageInspector {
    private VillageInspector() { }

    public static List<String> overview(VillageEngine e, Village v) {
        List<String> out = new ArrayList<>();
        VillageCensus c = e.census(v.id());
        long now = e.clock().now();
        out.add(String.format("%s (cultura %s, comunidad %s) — fundada %s, renombre %.1f, prosperidad %.2f, descontento %.2f", v.name(), v.culture(), v.communityKey(),
                e.clock().date(v.founded()).shortDate(), v.renown(), v.prosperity(), v.unrest()));
        out.add(String.format("Población %d (%d sin cuerpo, %d niños, %d ancianos), guardias %d, monjes %d, visitantes %d, sin hogar %d; oficios %s", c.residents(), c.abstractCitizens(),
                c.children(), c.elders(), c.guards(), c.monks(), c.visitors(), c.homeless(), c.professions()));
        out.add(String.format("Seguridad %s (amenaza %.0f) desde %s; mercado %s (afluencia %d, puestos %d); templo %s; actividad social %.1f", v.security().state(), v.security().threat(),
                e.clock().date(v.security().since()).shortDate(), v.marketOpen() ? "abierto" : "cerrado", v.marketFootfall(), v.stalls().size(), v.ritualActive() ? "en ritual" : "tranquilo", v.socialActivity()));
        List<String> active = new ArrayList<>();
        for (VillageEventRecord ev : v.activeEvents(now)) active.add(ev.title() + " [" + ev.kind() + "]");
        out.add("Eventos: " + (active.isEmpty() ? "ninguno" : String.join(", ", active)));
        StringBuilder d = new StringBuilder("Distritos: ");
        for (District x : v.districts().values()) d.append(String.format("%s(%d edif., actividad %.0f%%) ", x.kind(), x.buildings().size(), x.activity() * 100));
        out.add(d.toString().trim());
        out.add("Memoria: " + v.counters() + (v.renownCauses().isEmpty() ? "" : "; renombre: " + String.join(" | ", v.renownCauses())));
        return out;
    }

    public static List<String> buildings(Village v) {
        List<String> out = new ArrayList<>();
        for (Building b : v.buildings().values())
            out.add(String.format("%s %s [%s] %.0f,%.0f r=%.0f, estado %.0f%%, dueño %s, capacidad %d%s", b.kind(), b.name(), b.state(), b.x(), b.z(), b.radius(), b.condition() * 100,
                    b.owner().describe(), b.capacity(), b.zoneId().isEmpty() ? "" : ", zona " + b.zoneId()));
        return out;
    }

    public static List<String> citizens(VillageEngine e, Village v) {
        List<String> out = new ArrayList<>();
        for (Citizen c : e.citizensOf(v.id())) {
            HomeRecord h = v.homes().get(c.id());
            out.add(String.format("%s — %s (%s, %.0f h), %s, hogar %s, ahora: %s", c.name(), c.profession().isEmpty() ? "sin oficio" : c.profession(), Skill.rank(c.professionHours()),
                    c.professionHours(), c.embodied() ? "con cuerpo" : "simulado", h == null ? "ninguno" : v.buildings().containsKey(h.house()) ? v.buildings().get(h.house()).name() + " " + h.room() : "?",
                    e.plannedRoutine(c.id())));
        }
        if (out.isEmpty()) out.add("Sin ciudadanos.");
        return out;
    }

    public static List<String> citizen(VillageEngine e, UUID npc) {
        List<String> out = new ArrayList<>();
        e.citizen(npc).ifPresentOrElse(c -> {
            out.add(String.format("%s (%s) ciudadano de %s desde %s, estado %s, oficio %s %s", c.name(), c.npcType(), e.village(c.village()).map(Village::name).orElse("?"),
                    e.clock().date(c.joined()).shortDate(), c.status(), c.profession(), Skill.rank(c.professionHours())));
            e.plan(npc).ifPresent(p -> {
                StringBuilder sb = new StringBuilder("Día: ");
                for (var b : p.blocks()) sb.append(String.format("%02d:%02d %s  ", b.minute() / 60, b.minute() % 60, b.routine()));
                out.add(sb.toString().trim() + " (" + p.label() + ")");
            });
            VillageEngine.Bias bias = e.routineBias(npc);
            out.add("Sesgo al scheduler: " + bias.bias() + " ← " + String.join("; ", bias.reasons()));
            e.home(npc).ifPresent(h -> out.add("Hogar: " + h.room() + ", cama " + (int) h.bedX() + "," + (int) h.bedY() + "," + (int) h.bedZ() + ", objetos " + h.personalObjects()
                    + ", vecinos " + e.neighbours(npc).size()));
        }, () -> out.add("No es ciudadano de ninguna aldea."));
        return out;
    }

    public static List<String> visitors(Village v) {
        List<String> out = new ArrayList<>();
        for (VisitorRecord r : v.visitors()) out.add(String.format("%s %s (%s): %s, %s", r.kind(), r.name(), r.purpose(), r.phase(), r.npc() == null ? "simulado" : "NPC"));
        if (out.isEmpty()) out.add("Sin visitantes.");
        return out;
    }

    public static List<String> metrics(VillageEngine e) {
        VillageMetrics.Snapshot m = e.metrics().snapshot();
        return List.of(
                String.format("Aldeas %d, edificios %d, ciudadanos +%d/-%d, oficios asignados %d, hogares %d", m.villagesCreated(), m.buildingsRegistered(), m.citizensJoined(), m.citizensLeft(),
                        m.professionsAssigned(), m.homesAssigned()),
                String.format("Cambios de seguridad %d, eventos %d, visitantes %d, aperturas de mercado %d, rituales %d, relevos %d", m.securityChanges(), m.eventsStarted(), m.visitorsArrived(),
                        m.marketOpenings(), m.rituals(), m.shiftChanges()),
                String.format("Actualizaciones %d (%.1f µs), simulaciones %d, consultas de sesgo %d (%.1f µs), planes construidos %d", m.villageUpdates(), m.updateMicros(), m.simulations(),
                        m.biasQueries(), m.biasMicros(), e.schedules().plansBuilt()));
    }
}
