package yadi.samuraiai.living.village;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.calendar.engine.CalendarSettings;
import yadi.samuraiai.living.core.CalendarDate;
import yadi.samuraiai.living.core.WeatherKind;
import yadi.samuraiai.living.core.persistence.LivingStorage;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.buildings.BuildingKind;
import yadi.samuraiai.living.village.citizens.Citizen;
import yadi.samuraiai.living.village.engine.VillageEngine;
import yadi.samuraiai.living.village.engine.VillagePorts;
import yadi.samuraiai.living.village.engine.VillageSettings;
import yadi.samuraiai.living.village.events.HousingShortageEvent;
import yadi.samuraiai.living.village.events.MarketClosedEvent;
import yadi.samuraiai.living.village.events.MarketOpenedEvent;
import yadi.samuraiai.living.village.events.VillageSecurityChangedEvent;
import yadi.samuraiai.living.village.events.VisitorArrivedEvent;
import yadi.samuraiai.living.village.life.VillageEventKind;
import yadi.samuraiai.living.village.persistence.VillageStorage;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.village.security.SecurityState;
import yadi.samuraiai.living.world.professions.ProfessionCatalog;

class VillageEngineTest {
    private static final String OW = "minecraft:overworld";
    private final List<NpcEvent> events = new ArrayList<>();
    private final CalendarEngine calendar = new CalendarEngine(CalendarSettings::defaults, e -> { }, 3L);
    private WeatherKind weather = WeatherKind.SUNNY;
    private final List<String> festivals = new ArrayList<>();

    private VillageEngine engine(VillageSettings settings) {
        VillageEngine v = new VillageEngine(() -> settings, events::add, calendar, 3L);
        ProfessionCatalog catalog = ProfessionCatalog.defaults();
        v.useProfessions(new VillagePorts.Professions() {
            @Override public Optional<VillagePorts.ProfessionInfo> get(String id) {
                return catalog.get(id).map(p -> new VillagePorts.ProfessionInfo(p.id(), p.name(), p.workRoutine(), p.bias(), p.locations(), p.future()));
            }
            @Override public Optional<String> forNpcType(String type) { return catalog.forNpcType(type).map(p -> p.id()); }
            @Override public List<String> ids() { return catalog.all().stream().map(p -> p.id()).toList(); }
        });
        v.useCalendar(new VillagePorts.Calendar() {
            @Override public CalendarDate today() { return calendar.today(); }
            @Override public int sunriseShift() { return calendar.sun().sunriseShift(calendar.today().dayOfYear()); }
            @Override public double seasonSocial() { return calendar.seasonProfile().social(); }
            @Override public WeatherKind weather(String regionScope) { return weather; }
            @Override public double temperature(String regionScope) { return 18; }
            @Override public List<VillagePorts.FestivalInfo> festivals(String culture) {
                return festivals.stream().map(f -> new VillagePorts.FestivalInfo(f, f, Map.of("SOCIAL", 40.0, "WORK", -15.0), 1.5)).toList();
            }
            @Override public boolean holyDay() { return !festivals.isEmpty(); }
        });
        return v;
    }
    private VillageEngine engine() { return engine(VillageSettings.defaults()); }
    private <T> List<T> of(Class<T> type) { return events.stream().filter(type::isInstance).map(type::cast).toList(); }
    private void at(int hour, int minute) { calendar.advanceMinutes(Math.floorMod(hour * 60 + minute - calendar.today().minuteOfDay(), 1440), "test"); }

    private Village village(VillageEngine e) { return e.createVillage(UUID.randomUUID(), UUID.randomUUID(), "Sakura no Sato", "village", OW, 0, 64, 0, 64, true, true); }

    @Test void aPlannedVillageHasItsFunctionalBuildingsDistrictsAndStreets() {
        VillageEngine e = engine();
        Village v = village(e);
        assertNotNull(v.mainTemple());
        assertNotNull(v.market());
        assertEquals(8, v.buildingsOf(BuildingKind.HOUSE).size());
        assertFalse(v.buildingsOf(BuildingKind.FARM).isEmpty());
        assertFalse(v.buildingsOf(BuildingKind.WELL).isEmpty());
        assertTrue(v.districts().size() >= 6);
        Building smithy = v.buildingsOf(BuildingKind.SMITHY).get(0);
        assertFalse(v.layout().path(smithy.id(), v.market()).isEmpty(), "streets connect the smithy to the market");
    }

    @Test void citizensGetProfessionsAndHomesAndAVillageRunsOutOfBeds() {
        VillageEngine e = engine(VillageSettings.builder().set("plannedHouses", 1).build());
        Village v = village(e);
        Citizen guard = e.admit(v.id(), UUID.randomUUID(), "Kenji", "guard", true, 0);
        assertEquals("guard", guard.profession());
        Citizen villager = e.admit(v.id(), UUID.randomUUID(), "Aiko", "villager", true, 0);
        assertEquals("farmer", villager.profession(), "the village needs farmers first");
        assertNotNull(e.home(guard.id()).orElseThrow().house());
        assertFalse(e.home(villager.id()).orElseThrow().personalObjects().isEmpty());
        assertTrue(e.neighbours(guard.id()).contains(villager.id()));
        for (int i = 0; i < 4; i++) e.admit(v.id(), UUID.randomUUID(), "N" + i, "villager", true, 0);
        e.update(v);
        assertFalse(of(HousingShortageEvent.class).isEmpty(), "one house of four beds cannot hold six people");
        assertTrue(e.depart(guard.id(), Citizen.Status.MIGRATED, "se va"));
        assertTrue(e.home(guard.id()).isEmpty());
        assertEquals(5, v.citizens().size());
    }

    @Test void schedulesDifferByProfessionSeasonFestivalAndPerson() {
        VillageEngine e = engine();
        Village v = village(e);
        Citizen fisher = e.admit(v.id(), UUID.randomUUID(), "Taro", "fisherman", true, 0);
        Citizen smith = e.admit(v.id(), UUID.randomUUID(), "Jun", "blacksmith", true, 0);
        Citizen monk = e.admit(v.id(), UUID.randomUUID(), "Ren", "monk", true, 0);
        var fisherPlan = e.plan(fisher.id()).orElseThrow();
        var smithPlan = e.plan(smith.id()).orElseThrow();
        assertNotEquals(fisherPlan.blocks(), smithPlan.blocks());
        assertTrue(e.plan(monk.id()).orElseThrow().blocks().stream().anyMatch(b -> b.routine().equals("PRAYER")));
        assertTrue(smithPlan.workHours() > 6, "a working day: " + smithPlan.workHours());
        assertEquals(smithPlan.workHours() * 2, e.plannedWorkHours(smith.id(), calendar.now(), calendar.now() + 2 * 1440), 1e-6);
        festivals.add("hanami");
        e.update(v);
        assertTrue(v.activeEvents(calendar.now()).stream().anyMatch(x -> x.kind() == VillageEventKind.FESTIVAL));
        var festive = e.plan(smith.id()).orElseThrow();
        assertTrue(festive.label().contains("festividad"), festive.label());
    }

    @Test void theVillageBiasesTheSchedulerAndRespondsToAttacksAndRain() {
        VillageEngine e = engine();
        Village v = village(e);
        Citizen farmer = e.admit(v.id(), UUID.randomUUID(), "Hana", "farmer", true, 0);
        Citizen guard = e.admit(v.id(), UUID.randomUUID(), "Shin", "guard", true, 0);
        at(9, 0);
        VillageEngine.Bias calm = e.routineBias(farmer.id());
        assertEquals("WORK", calm.planned());
        assertTrue(calm.bias().getOrDefault("WORK", 0.0) > 30, calm.toString());
        weather = WeatherKind.STORM;
        assertTrue(e.routineBias(farmer.id()).bias().getOrDefault("WORK", 0.0) < calm.bias().get("WORK"), "storms keep farmers from the fields");
        weather = WeatherKind.SUNNY;
        UUID attack = UUID.randomUUID();
        e.attackStarted(v.id(), attack, "Ataque de bandidos");
        assertEquals(SecurityState.ATTACK, v.security().state());
        assertTrue(e.routineBias(farmer.id()).bias().getOrDefault("REST", 0.0) > 50);
        assertTrue(e.routineBias(guard.id()).bias().getOrDefault("GUARD", 0.0) > 100);
        e.attackEnded(v.id(), attack, true, "la guardia");
        assertEquals(SecurityState.RECOVERY, v.security().state());
        assertEquals(1, v.counter(Village.Counter.ATTACKS_REPELLED));
        assertTrue(v.renown() > 0);
        calendar.advanceMinutes(400, "test");
        e.update(v);
        assertEquals(SecurityState.PEACE, v.security().state());
    }

    @Test void threatRaisesAlertAndDangerAndDecays() {
        VillageEngine e = engine();
        Village v = village(e);
        e.reportThreat(v.id(), 30, "lobos vistos");
        assertEquals(SecurityState.ALERT, v.security().state());
        e.reportThreat(v.id(), 40, "más lobos");
        assertEquals(SecurityState.DANGER, v.security().state());
        calendar.advanceMinutes(8 * 60, "test");
        e.update(v);
        assertEquals(SecurityState.RECOVERY, v.security().state());
        assertTrue(of(VillageSecurityChangedEvent.class).size() >= 3);
    }

    @Test void theMarketOpensWithMerchantsAndClosesUnderAttack() {
        VillageEngine e = engine();
        Village v = village(e);
        e.admit(v.id(), UUID.randomUUID(), "Mei", "merchant", true, 0);
        at(9, 0);
        e.update(v);
        assertTrue(v.marketOpen());
        assertEquals(1, v.stalls().size());
        assertFalse(of(MarketOpenedEvent.class).isEmpty());
        e.attackStarted(v.id(), UUID.randomUUID(), "Asalto");
        e.update(v);
        assertFalse(v.marketOpen());
        assertFalse(of(MarketClosedEvent.class).isEmpty());
    }

    @Test void visitorsComeAndGoWhileTheVillageIsSimulated() {
        VillageEngine e = engine(VillageSettings.builder().set("visitorsPerDay", 3.0).build());
        Village v = village(e);
        long from = calendar.now();
        calendar.advanceMinutes(3 * 1440, "test");
        e.simulate(v.id(), from, calendar.now(), false);
        assertFalse(of(VisitorArrivedEvent.class).isEmpty());
        calendar.advanceMinutes(10 * 1440, "test");
        e.update(v);
        assertTrue(v.visitors().stream().allMatch(r -> r.leaves() > calendar.now() - 1), "visitors left after their stay");
    }

    @Test void abstractSimulationGivesWorkExperience() {
        VillageEngine e = engine();
        Village v = village(e);
        Citizen smith = e.admit(v.id(), UUID.randomUUID(), "Jun", "blacksmith", false, 0);
        long from = calendar.now();
        calendar.advanceMinutes(10 * 1440, "test");
        e.simulate(v.id(), from, calendar.now(), false);
        assertTrue(smith.professionHours() > 50, "ten planned working days: " + smith.professionHours());
    }

    @Test void villagesSurviveARestart() throws Exception {
        Path dir = Files.createTempDirectory("living-village");
        VillageEngine a = engine();
        Village v = village(a);
        Citizen c = a.admit(v.id(), UUID.randomUUID(), "Yuki", "guard", true, 12);
        a.reportThreat(v.id(), 30, "ruido");
        a.addVisitor(v.id(), yadi.samuraiai.living.village.visitors.VisitorRecord.Kind.PILGRIM, "Sora", null, null, 24, "peregrinación");
        LivingStorage store = new LivingStorage(dir, false);
        VillageStorage.install(store, a);
        assertTrue(store.saveAll() >= 2);

        VillageEngine b = engine();
        LivingStorage load = new LivingStorage(dir, false);
        VillageStorage.install(load, b);
        load.loadAll().values().forEach(r -> assertTrue(r.usable(), r.detail()));
        Village w = b.village(v.id()).orElseThrow();
        assertEquals(v.buildings().size(), w.buildings().size());
        assertEquals(v.mainTemple(), w.mainTemple());
        assertEquals(SecurityState.ALERT, w.security().state());
        assertEquals("guard", b.citizen(c.id()).orElseThrow().profession());
        assertEquals(c.scheduleOffset(), b.citizen(c.id()).orElseThrow().scheduleOffset());
        assertEquals(a.home(c.id()).orElseThrow().house(), b.home(c.id()).orElseThrow().house());
        assertEquals(1, w.visitors().size());
        assertEquals(a.plan(c.id()).orElseThrow().blocks(), b.plan(c.id()).orElseThrow().blocks());
    }
}
