package yadi.samuraiai.living.world.debug;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import yadi.samuraiai.living.world.engine.WorldEngine;
import yadi.samuraiai.living.world.events.WorldEventRecord;
import yadi.samuraiai.living.world.metrics.WorldMetrics;
import yadi.samuraiai.living.world.regions.Region;
import yadi.samuraiai.living.world.regions.ResourceDeposit;
import yadi.samuraiai.living.world.roads.RoadEdge;
import yadi.samuraiai.living.world.settlements.Settlement;
import yadi.samuraiai.living.world.wildlife.WildlifePopulation;

/**
 * The World Overlay in text form: regions (type, level of detail, danger, population), settlements, roads, events and the
 * simulation state; the adapter draws the same data with particles for players.
 */
public final class WorldInspector {
    private WorldInspector() { }

    public static List<String> overview(WorldEngine w) {
        List<String> out = new ArrayList<>();
        var rt = w.runtime(0, "", "", "", 0);
        out.add(String.format("Regiones %d %s; asentamientos %d; caminos %d (%d bloqueados); población %d", w.regionCount(), rt.loadedRegions(), rt.settlements(), rt.roads(),
                w.roads().blockedCount(), rt.population()));
        out.add(String.format("Eventos abiertos %d (creados %d, cerrados %d, rechazados %d); simulación: pasos %d, catch-ups %d (%d pasos), fallos %d, %.1f µs/paso",
                w.events().open().size(), w.events().created(), w.events().closed(), w.events().refused(), w.simulation().steps(), w.simulation().catchUps(),
                w.simulation().catchUpSteps(), w.simulation().failures(), w.simulation().averageStepMicros()));
        if (!w.simulation().lastError().isEmpty()) out.add("Último error de simulación: " + w.simulation().lastError());
        List<String> problems = w.problems();
        if (!problems.isEmpty()) out.add("Problemas de configuración: " + String.join("; ", problems));
        return out;
    }

    public static List<String> region(WorldEngine w, Region r) {
        List<String> out = new ArrayList<>();
        out.add(String.format("%s [%s] %s, bioma %s, cultura %s, altitud %.0f, nivel %s", r.name(), r.key(), r.type(), r.biome().isEmpty() ? "-" : r.biome(), r.cultureId(), r.altitude(), r.level()));
        out.add(String.format("Peligro %.2f (tierra %.2f + eventos %.2f), población %d, asentamientos %d, simulada por última vez %s", r.danger(), r.baseDanger(), r.eventDanger(),
                r.population(), r.settlements().size(), w.clock().date(r.lastSimulated()).describe()));
        StringBuilder dep = new StringBuilder("Recursos: ");
        for (ResourceDeposit d : r.deposits().values()) dep.append(String.format("%s %.0f/%.0f (+%.0f/día) ", d.resource(), d.stock(w.clock().now(), w.clock().minutesPerDay()), d.capacity(), d.regenPerDay()));
        out.add(dep.toString().trim());
        StringBuilder fauna = new StringBuilder("Fauna: ");
        for (WildlifePopulation p : r.wildlife().values()) fauna.append(String.format("%s %.0f/%.0f ", p.species(), p.count(), p.capacity()));
        out.add(fauna.toString().trim());
        out.add("Memoria: " + r.counters());
        return out;
    }

    public static List<String> settlements(WorldEngine w) {
        List<String> out = new ArrayList<>();
        for (Settlement s : w.settlements())
            out.add(String.format("%s (%s, %s) en %s — %.0f,%.0f r=%.0f, fundado %s, población %d", s.name(), s.type(), s.status(), w.region(s.regionId()).map(Region::name).orElse("?"),
                    s.x(), s.z(), s.radius(), w.clock().date(s.founded()).shortDate(), w.population().of(s.id())));
        if (out.isEmpty()) out.add("No hay asentamientos.");
        return out;
    }

    public static List<String> roads(WorldEngine w) {
        List<String> out = new ArrayList<>();
        for (RoadEdge e : w.roads().edges()) {
            String a = w.roads().node(e.a()).map(n -> n.name()).orElse("?"), b = w.roads().node(e.b()).map(n -> n.name()).orElse("?");
            out.add(String.format("%s ↔ %s: %s %.0f bloques, puentes %d, peligro %.2f, estado %.2f, tráfico %d%s", a, b, e.kind(), e.length(), e.bridges(), e.danger(), e.condition(), e.traffic(),
                    e.blocked() ? " [BLOQUEADO: " + e.blockedReason() + "]" : ""));
        }
        if (out.isEmpty()) out.add("No hay caminos.");
        return out;
    }

    public static List<String> events(WorldEngine w, boolean archive) {
        List<String> out = new ArrayList<>();
        for (WorldEventRecord e : archive ? w.events().archive() : w.events().open())
            out.add(String.format("%s %s [%s] sev %.2f, %s → %s%s — causa: %s%s", e.id().toString().substring(0, 8), e.title(), e.phase(), e.severity(), w.clock().date(e.startAt()).shortDate(),
                    w.clock().date(e.endAt()).shortDate(), e.resolution() == WorldEventRecord.Resolution.NONE ? "" : " (" + e.resolution() + ")", e.cause().label(),
                    e.consequences().isEmpty() ? "" : "; consecuencias: " + String.join(", ", e.consequences())));
        if (out.isEmpty()) out.add(archive ? "Archivo vacío." : "No hay eventos abiertos.");
        return out;
    }

    public static Optional<WorldEventRecord> event(WorldEngine w, String idPrefix) {
        for (WorldEventRecord e : w.events().open()) if (e.id().toString().startsWith(idPrefix)) return Optional.of(e);
        for (WorldEventRecord e : w.events().archive()) if (e.id().toString().startsWith(idPrefix)) return Optional.of(e);
        return Optional.empty();
    }

    public static List<String> metrics(WorldEngine w) {
        WorldMetrics.Snapshot m = w.metrics().snapshot();
        return List.of(
                String.format("Ticks %d (%.2f µs/tick, máx %.1f µs); streaming %d (activaciones %d, dormidas %d, catch-ups %d)", m.ticks(), m.tickMicros(), m.maxTickMicros(), m.streamingUpdates(),
                        m.activations(), m.sleeps(), m.catchUps()),
                String.format("Regiones creadas %d, asentamientos %d, caminos %d (bloqueos %d), rutas planificadas %d, extracciones %d, cacerías %d, eventos %d (transiciones %d), trabajo diario %.2f ms",
                        m.regionsCreated(), m.settlementsFounded(), m.roadsBuilt(), m.roadsBlocked(), m.routesPlanned(), m.extractions(), m.hunts(), m.eventsScheduled(), m.eventTransitions(), m.dailyMillis()));
    }

    public static String shortId(UUID id) { return id == null ? "-" : id.toString().substring(0, 8); }
}
