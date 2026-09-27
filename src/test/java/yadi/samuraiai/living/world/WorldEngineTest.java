package yadi.samuraiai.living.world;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.calendar.engine.CalendarSettings;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.persistence.LivingStorage;
import yadi.samuraiai.living.world.engine.WorldEngine;
import yadi.samuraiai.living.world.engine.WorldPorts;
import yadi.samuraiai.living.world.engine.WorldSettings;
import yadi.samuraiai.living.world.events.WorldEventPhase;
import yadi.samuraiai.living.world.events.WorldEventRecord;
import yadi.samuraiai.living.world.events.WorldEventType;
import yadi.samuraiai.living.world.events_api.CatchUpCompletedEvent;
import yadi.samuraiai.living.world.events_api.RegionActivatedEvent;
import yadi.samuraiai.living.world.events_api.RoadBlockedEvent;
import yadi.samuraiai.living.world.events_api.RoadReopenedEvent;
import yadi.samuraiai.living.world.events_api.SettlementFoundedEvent;
import yadi.samuraiai.living.world.events_api.WorldEventPhaseEvent;
import yadi.samuraiai.living.world.persistence.WorldStorage;
import yadi.samuraiai.living.world.regions.Region;
import yadi.samuraiai.living.world.regions.RegionType;
import yadi.samuraiai.living.world.roads.RoadNetwork;
import yadi.samuraiai.living.world.settlements.Settlement;
import yadi.samuraiai.living.world.settlements.SettlementType;
import yadi.samuraiai.living.world.streaming.SimulationLevel;
import yadi.samuraiai.living.world.streaming.StreamingEngine;

class WorldEngineTest {
    private static final String OW = "minecraft:overworld";
    private final List<NpcEvent> events = new ArrayList<>();
    private final CalendarEngine calendar = new CalendarEngine(CalendarSettings::defaults, e -> { }, 7L);

    private WorldEngine world(WorldSettings settings) {
        WorldEngine w = new WorldEngine(() -> settings, events::add, calendar, 7L);
        // x < 0 is forest, a river runs along 1024 <= x < 1536, everything else is fields
        w.useClassifier((d, x, z) -> new WorldPorts.RegionClassifier.Classification(x < 0 ? RegionType.FOREST : x >= 1024 && x < 1536 ? RegionType.RIVER : RegionType.FIELDS, "plains", 64));
        return w;
    }
    private WorldEngine world() { return world(WorldSettings.defaults()); }
    private <T> List<T> of(Class<T> type) { return events.stream().filter(type::isInstance).map(type::cast).toList(); }

    @Test void regionsAreCreatedOnceWithResourcesWildlifeAndNeighbours() {
        WorldEngine w = world();
        Region a = w.ensureRegion(OW, -100, 10);
        assertSame(a, w.ensureRegion(OW, -300, 400));
        assertEquals(RegionType.FOREST, a.type());
        assertTrue(a.deposits().containsKey("wood"));
        assertTrue(a.wildlife().containsKey("deer") && a.wildlife().get("deer").count() > 0);
        Region b = w.ensureRegion(OW, 100, 10);
        assertEquals(Region.Relation.ADJACENT, a.relations().get(b.id()));
        assertEquals(Region.Relation.ADJACENT, b.relations().get(a.id()));
        assertFalse(a.name().isBlank());
    }

    @Test void resourcesComeOutOfDepositsAndNeverFromNothing() {
        WorldEngine w = world();
        Region forest = w.ensureRegion(OW, -100, 10);
        double stock = w.available(forest.id(), "wood");
        assertEquals(stock, w.extract(forest.id(), "wood", stock + 5000), 1e-6);
        assertEquals(0, w.extract(forest.id(), "wood", 10), 1e-6);
        assertEquals(0, w.extract(forest.id(), "iron", 10), 1e-6, "a forest has no iron");
        calendar.advanceMinutes(10L * 1440, "test");
        assertEquals(450, w.available(forest.id(), "wood"), 1e-6, "45 wood/day regenerate");
        double meat = w.harvestAnimals(forest.id(), "meat", 30, 0.2);
        assertTrue(meat > 0 && meat <= 30);
    }

    @Test void settlementsAreFoundedWithRoadsAndRoutesAvoidDanger() {
        WorldEngine w = world();
        Settlement a = w.foundSettlement("Aldea A", SettlementType.VILLAGE, OW, 100, 64, 100, 48, Provenance.of("test", "", "", 0));
        Settlement b = w.foundSettlement("Aldea B", SettlementType.VILLAGE, OW, 1900, 64, 100, 48, null);
        Settlement c = w.foundSettlement("Templo C", SettlementType.TEMPLE, OW, 900, 64, 900, 48, null);
        assertSame(a, w.foundSettlement("otra", SettlementType.VILLAGE, OW, 110, 64, 110, 10, null), "same type inside the area is the same settlement");
        assertEquals(3, of(SettlementFoundedEvent.class).size());
        assertTrue(w.roads().edgeCount() >= 2);
        var route = w.route(a.id(), b.id(), RoadNetwork.Preferences.CARAVAN).orElseThrow();
        assertTrue(route.length() >= 1800);
        assertTrue(w.roads().edges().stream().anyMatch(e -> e.bridges() > 0), "the road to B crosses the river");
        assertTrue(w.route(a.id(), c.id(), RoadNetwork.Preferences.TRAVELLER).isPresent());
    }

    @Test void warBlocksRoadsWhileItRunsAndTheyReopenAfterwards() {
        WorldEngine w = world();
        Settlement a = w.foundSettlement("A", SettlementType.VILLAGE, OW, 100, 64, 100, 48, null);
        Settlement b = w.foundSettlement("B", SettlementType.VILLAGE, OW, 700, 64, 100, 48, null);
        Region region = w.region(b.regionId()).orElseThrow();
        WorldEventRecord war = w.scheduleEvent(WorldEventType.WAR, "Guerra", region.id(), null, 0.9, 60, 3 * 1440, Provenance.of("test", "", "facciones", 0), Set.of()).orElseThrow();
        assertTrue(w.scheduleEvent(WorldEventType.WAR, "otra", region.id(), null, 0.9, Provenance.of("t", "", "", 0)).isEmpty(), "one war per region at a time");
        calendar.advanceMinutes(90, "test");
        w.tick(List.of());
        assertEquals(WorldEventPhase.DEVELOPMENT, war.phase());
        assertTrue(region.danger() > 0.4);
        assertFalse(of(RoadBlockedEvent.class).isEmpty());
        assertTrue(w.route(a.id(), b.id(), RoadNetwork.Preferences.CARAVAN).isEmpty(), "every road into the war is blocked");
        calendar.advanceMinutes(3 * 1440, "test");
        w.tick(List.of());
        assertEquals(WorldEventPhase.CONSEQUENCES, war.phase());
        assertFalse(of(RoadReopenedEvent.class).isEmpty());
        assertTrue(w.route(a.id(), b.id(), RoadNetwork.Preferences.CARAVAN).isPresent());
        calendar.advanceMinutes(120, "test");
        w.tick(List.of());
        assertEquals(WorldEventPhase.CLOSED, war.phase());
        assertEquals(1, region.counter(Region.Counter.WARS));
        List<String> phases = of(WorldEventPhaseEvent.class).stream().filter(e -> e.eventId().equals(war.id())).map(WorldEventPhaseEvent::phase).toList();
        assertEquals(List.of("PREPARATION", "START", "DEVELOPMENT", "END", "CONSEQUENCES", "CLOSED"), phases);
    }

    @Test void resolvingAnEventEndsItEarly() {
        WorldEngine w = world();
        Settlement a = w.foundSettlement("A", SettlementType.VILLAGE, OW, 100, 64, 100, 48, null);
        WorldEventRecord attack = w.scheduleEvent(WorldEventType.ATTACK, "Ataque", a.regionId(), a.id(), 0.8, 0, 600, Provenance.of("t", "", "", 0), Set.of()).orElseThrow();
        w.tick(List.of());
        assertTrue(attack.phase().running());
        assertTrue(w.resolveEvent(attack.id(), true, "jugador", "defendida"));
        assertEquals(WorldEventRecord.Resolution.RESOLVED, attack.resolution());
        assertEquals(WorldEventPhase.CONSEQUENCES, attack.phase());
    }

    @Test void streamingWakesRegionsAndCatchesThemUpInBoundedSteps() {
        WorldEngine w = world(WorldSettings.builder().set("maxCatchUpSteps", 10).set("streamingIntervalTicks", 1).build());
        Region far = w.ensureRegion(OW, 5000, 5000);
        w.tick(List.of());
        assertEquals(SimulationLevel.ABSTRACT, far.level());
        calendar.advanceMinutes(40L * 1440, "test");
        w.tick(List.of(new StreamingEngine.Viewer(OW, 5100, 5100)));
        assertEquals(SimulationLevel.FULL, far.level());
        CatchUpCompletedEvent c = of(CatchUpCompletedEvent.class).get(0);
        assertEquals(far.id(), c.regionId());
        assertTrue(c.steps() <= 10, "bounded catch-up: " + c.steps());
        assertEquals(calendar.now(), far.lastSimulated());
        assertEquals(1, of(RegionActivatedEvent.class).size());
        calendar.advanceMinutes(400L * 1440, "test");
        w.tick(List.of());
        assertEquals(SimulationLevel.HISTORICAL, far.level());
    }

    @Test void wildlifeGrowsLogisticallyAndPredatorsEat() {
        WorldEngine w = world();
        Region forest = w.ensureRegion(OW, -100, 10);
        double deer = forest.wildlife().get("deer").count();
        forest.wildlife().get("wolves").restore(0, 18, 0, 0, calendar.now());
        calendar.advanceMinutes(200L * 1440, "test");
        w.simulation().catchUp(forest, calendar.now());
        double grown = forest.wildlife().get("deer").count();
        assertTrue(grown > deer && grown <= 120.0001, grown + " vs " + deer);
    }

    @Test void theWorldGeneratesEventsFromItsStateDeterministically() {
        WorldSettings busy = WorldSettings.builder().set("fireChancePerDay", 1.0).set("attackChancePerDay", 0.0).set("banditChancePerDay", 0.0).set("duelChancePerDay", 0.0).set("specialMarketEveryDays", 0).build();
        WorldEngine w = world(busy);
        w.foundSettlement("A", SettlementType.VILLAGE, OW, 100, 64, 100, 48, null);
        w.daily(calendar.currentDay());
        assertEquals(1, w.events().open().size());
        assertEquals(WorldEventType.FIRE, w.events().open().iterator().next().type());
        assertTrue(w.events().open().iterator().next().cause().cause().length() > 0, "every event has a cause");
    }

    @Test void theWorldSurvivesARestart() throws Exception {
        Path dir = Files.createTempDirectory("living-world");
        WorldEngine a = world();
        Settlement s = a.foundSettlement("A", SettlementType.VILLAGE, OW, 100, 64, 100, 48, null);
        a.foundSettlement("B", SettlementType.MARKET, OW, 800, 64, 300, 48, null);
        Region forest = a.ensureRegion(OW, -100, 10);
        a.extract(forest.id(), "wood", 1000);
        a.scheduleEvent(WorldEventType.BANDITS, "Bandidos", forest.id(), null, 0.5, 30, 600, Provenance.of("t", "", "", 0), Set.of());
        a.reportPopulation(s.id(), 12, Map.of("farmer", 5));
        LivingStorage store = new LivingStorage(dir, true);
        WorldStorage.sections(a).forEach(store::register);
        assertEquals(5, store.saveAll());

        WorldEngine b = world();
        LivingStorage load = new LivingStorage(dir, true);
        WorldStorage.sections(b).forEach(load::register);
        load.loadAll().values().forEach(r -> assertTrue(r.usable(), r.detail()));
        assertEquals(a.regionCount(), b.regionCount());
        assertEquals(2, b.settlements().size());
        assertEquals(a.roads().edgeCount(), b.roads().edgeCount());
        assertEquals(a.available(forest.id(), "wood"), b.available(forest.id(), "wood"), 1e-6);
        assertEquals(1, b.events().open().size());
        assertEquals(12, b.population().of(s.id()));
        assertTrue(b.route(s.id(), b.findSettlement("B").orElseThrow().id(), RoadNetwork.Preferences.TRAVELLER).isPresent());
    }
}
