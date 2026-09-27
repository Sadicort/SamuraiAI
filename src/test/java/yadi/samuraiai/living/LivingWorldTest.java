package yadi.samuraiai.living;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.calendar.engine.CalendarSettings;
import yadi.samuraiai.living.calendar.events.DayChangedEvent;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.economy.engine.EconomySettings;
import yadi.samuraiai.living.economy.events.ScarcityStartedEvent;
import yadi.samuraiai.living.economy.ledger.MovementRecord;
import yadi.samuraiai.living.family.engine.FamilySettings;
import yadi.samuraiai.living.quest.engine.QuestSettings;
import yadi.samuraiai.living.quest.events.QuestCreatedEvent;
import yadi.samuraiai.living.quest.runtime.Quest;
import yadi.samuraiai.living.sim.LivingSettings;
import yadi.samuraiai.living.sim.LivingWorld;
import yadi.samuraiai.living.sim.Outside;
import yadi.samuraiai.living.village.citizens.Citizen;
import yadi.samuraiai.living.village.engine.VillageSettings;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.world.engine.WorldSettings;
import yadi.samuraiai.living.world.events.WorldEventType;
import yadi.samuraiai.living.world.events_api.CatchUpCompletedEvent;
import yadi.samuraiai.living.world.settlements.Settlement;
import yadi.samuraiai.living.world.settlements.SettlementType;
import yadi.samuraiai.living.world.streaming.StreamingEngine;

class LivingWorldTest {
    private static final String OW = "minecraft:overworld";
    private final List<NpcEvent> events = new ArrayList<>();
    private final List<String> told = new ArrayList<>();
    private long gameTime = 1000;
    private final List<UUID[]> rivalPairs = new ArrayList<>();

    private LivingWorld.Settings settings() {
        LivingSettings l = LivingSettings.defaults();
        CalendarSettings c = CalendarSettings.builder().set("maxDaysPerAdvance", 400).build();
        WorldSettings w = WorldSettings.builder().set("streamingIntervalTicks", 1).set("simulationIntervalTicks", 1).set("maxRegionsPerTick", 50)
                .set("fireChancePerDay", 0.0).set("attackChancePerDay", 0.0).set("banditChancePerDay", 0.0).set("duelChancePerDay", 0.0).set("specialMarketEveryDays", 0).build();
        VillageSettings v = VillageSettings.defaults();
        EconomySettings e = EconomySettings.defaults();
        QuestSettings q = QuestSettings.defaults();
        FamilySettings f = FamilySettings.defaults();
        return new LivingWorld.Settings(() -> l, () -> c, () -> w, () -> v, () -> e, () -> q, () -> f);
    }

    private LivingWorld world() {
        LivingWorld lw = new LivingWorld(settings(), events::add, 77L);
        lw.useOutside(new Outside() {
            @Override public String ensureCommunity(String key, String name, String culture, String dimension, double x, double y, double z, double radius) { return key; }
            @Override public void joinCommunity(UUID npc, String key, boolean leader) { }
            @Override public void leaveCommunity(UUID npc, String key) { }
            @Override public void rememberInCommunity(String key, String historyType, String title, double significance, long minute, UUID actor) { }
            @Override public double standing(UUID subject, String communityKey, String context) { return 0; }
            @Override public void adjustStanding(UUID subject, String name, String communityKey, String context, double amount) { told.add("standing " + context + " " + amount); }
            @Override public double trust(UUID npc, UUID other) { return 60; }
            @Override public double respect(UUID npc, UUID other) { return 60; }
            @Override public String mood(UUID npc) { return "CALM"; }
            @Override public void experience(UUID npc, UUID other, String otherName, String kind, String note) { told.add("experience " + kind); }
            @Override public void learn(UUID npc, String key, String text, UUID teacher) { }
            @Override public double teachingQuality(UUID master, UUID disciple) { return 0.7; }
            @Override public double affinity(UUID npc, String profession) { return 0.5; }
            @Override public Map<UUID, UUID> grateful(String communityKey) { return Map.of(); }
            @Override public List<UUID[]> rivals(String communityKey) { return List.copyOf(rivalPairs); }
            @Override public void tell(UUID player, String text) { told.add(text); }
            @Override public double giveItems(UUID player, String resource, double quantity) { return quantity; }
            @Override public Set<UUID> playersNear(String dimension, double x, double z, double radius) { return Set.of(); }
        });
        return lw;
    }

    private void hours(LivingWorld lw, int n, List<StreamingEngine.Viewer> viewers) {
        for (int i = 0; i < n * 20; i++) { gameTime += 50; lw.tick(gameTime, gameTime, false, viewers); }   // 20 ticks per Deiliora hour
    }

    private <T> List<T> of(Class<T> type) { return events.stream().filter(type::isInstance).map(type::cast).toList(); }

    private Settlement village(LivingWorld lw, String name, double x) {
        return lw.foundSettlement(name, SettlementType.VILLAGE, OW, x, 64, 0, 64, true, true, Provenance.of("test", "", "fundación", lw.calendar().now()));
    }

    @Test void endToEndTheWorldLivesPersistsAndComesBack() throws Exception {
        LivingWorld lw = world();
        Path dir = Files.createTempDirectory("living-e2e");
        lw.useStorage(dir);
        // 1-2: a region and a village (with its planned buildings, market and temple)
        Settlement sakura = village(lw, "Sakura", 0);
        Village v = lw.villages().village(sakura.id()).orElseThrow();
        assertNotNull(v.market());
        assertTrue(lw.economy().settlement(sakura.id()).isPresent(), "the settlement got an economy");
        assertEquals(1, lw.world().regionCount());
        // 3-5: citizens with homes, professions and families
        List<UUID> npcs = new ArrayList<>();
        String[] types = {"villager", "villager", "villager", "blacksmith", "merchant", "guard", "villager", "monk"};
        for (int i = 0; i < types.length; i++) {
            UUID id = UUID.randomUUID();
            npcs.add(id);
            Citizen c = lw.npcArrived(id, "Aldeano" + i, types[i], OW, 2 + i, 64, 2, 0).orElseThrow();
            assertEquals(sakura.id(), c.village());
        }
        for (UUID id : npcs) {
            assertTrue(lw.villages().home(id).isPresent(), "everyone has a bed");
            assertTrue(lw.families().familyOf(id).isPresent(), "everyone has a family");
            assertFalse(lw.villages().citizen(id).orElseThrow().profession().isEmpty(), "everyone has a trade");
        }
        assertEquals("blacksmith", lw.villages().citizen(npcs.get(3)).orElseThrow().profession());
        assertTrue(lw.economy().merchants().stream().anyMatch(m -> npcs.get(4).equals(m.npc())), "the merchant got a stall (6: market)");
        lw.economy().donate(sakura.id(), "rice", 2000, 0.8, Provenance.of("farm", "sakura-fields", "cosecha del año pasado", 0));
        // 7-8: days pass without a player: the village produces and consumes
        hours(lw, 3 * 24, List.of());
        assertTrue(of(DayChangedEvent.class).size() >= 3);
        assertTrue(lw.economy().ledger().total(MovementRecord.Kind.PRODUCED, "rice") + lw.economy().ledger().total(MovementRecord.Kind.PRODUCED, "wheat") > 0, "farmers grew food");
        assertTrue(lw.economy().ledger().total(MovementRecord.Kind.CONSUMED, "water") > 0, "people drank");
        // 9-10: the food runs out → scarcity → a quest with a real cause
        for (String food : List.of("rice", "wheat", "fish", "meat", "meal")) lw.economy().withdraw(sakura.id(), food, 100000, "prueba: se pudre el granero");
        hours(lw, 26, List.of());
        assertTrue(of(ScarcityStartedEvent.class).stream().anyMatch(e -> e.settlementId().equals(sakura.id())), "scarcity");
        Quest quest = lw.quests().open().stream().filter(q -> sakura.id().equals(q.settlement()) && q.variables().getOrDefault("resource", "").equals("food") && q.offeredUntil() > lw.calendar().now()).findFirst().orElseThrow();
        assertFalse(of(QuestCreatedEvent.class).isEmpty());
        assertTrue(npcs.contains(quest.giver()), "a citizen asks for help");
        // 11-13: time passes, a player resolves it, the economy changes
        UUID player = UUID.randomUUID();
        var accepted = lw.quests().accept(quest.id(), player, "Jugador");
        assertTrue(accepted.ok(), accepted.message() + " state=" + quest.state() + " log=" + quest.log());
        lw.quests().choose(quest.id(), player, yadi.samuraiai.living.quest.branching.Path.PEACEFUL);
        double stockBefore = lw.economy().stock(sakura.id(), "rice");
        double needed = quest.relevant().stream().filter(o -> o.type() == yadi.samuraiai.living.quest.objectives.ObjectiveType.DELIVER).findFirst().orElseThrow().required();
        lw.deliver(player, "Jugador", sakura.id(), "rice", needed, 0.8);
        assertEquals(Quest.State.COMPLETED, quest.state());
        assertTrue(lw.economy().stock(sakura.id(), "rice") > stockBefore, "the rice is in the storehouse, with the player as origin");
        assertTrue(lw.economy().wealth().coins(player) > 0, "paid from the village treasury");
        // 14: family history
        UUID someone = npcs.get(0);
        var fam = lw.families().familyOf(someone).orElseThrow();
        assertFalse(lw.families().history(fam.id()).isEmpty());
        // 15-17: save, restart, verify
        long minute = lw.calendar().now();
        assertTrue(lw.save(true) > 5);
        LivingWorld back = world();
        back.useStorage(dir);
        back.load().forEach((file, r) -> assertTrue(r.usable() || r.status() == yadi.samuraiai.ai.cognition.storage.LoadResult.Status.MISSING, file + ": " + r.detail()));
        assertEquals(minute, back.calendar().now(), "the date survives");
        assertEquals(lw.world().settlements().size(), back.world().settlements().size());
        assertEquals(v.citizens().size(), back.villages().village(sakura.id()).orElseThrow().citizens().size());
        assertEquals(lw.economy().stock(sakura.id(), "rice"), back.economy().stock(sakura.id(), "rice"), 1e-6);
        assertEquals(lw.economy().wealth().coins(player), back.economy().wealth().coins(player), 1e-6);
        assertEquals(Quest.State.COMPLETED, back.quests().quest(quest.id()).orElseThrow().state());
        assertEquals(lw.families().people().size(), back.families().people().size());
        assertEquals(lw.families().parents(someone), back.families().parents(someone));
        assertTrue(back.families().audit().isEmpty(), back.families().audit().toString());
        assertEquals(lw.calendar().timeline().size(), back.calendar().timeline().size());
        hours(back, 24, List.of());   // and it keeps living
        assertTrue(back.metrics().days.get() >= 1);
    }

    @Test void birthsAndDeathsReachThePopulationLedger() {
        LivingWorld lw = world();
        village(lw, "Umare", 0);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        lw.npcArrived(a, "Haru", "villager", OW, 2, 64, 2, 0);
        lw.npcArrived(b, "Aki", "villager", OW, 4, 64, 4, 0);
        var child = lw.families().birth(a, b, "Natsu", lw.villages().villageOf(a).orElseThrow().id());
        var ledger = lw.world().population();
        assertEquals(1L, ledger.total(yadi.samuraiai.living.world.population.PopulationLedger.ChangeKind.BIRTH));
        assertTrue(ledger.log().stream().anyMatch(c -> c.person().equals(child.id())), "the child is in the log");
        lw.npcLeft(a, true, "murió en combate");
        assertEquals(1L, ledger.total(yadi.samuraiai.living.world.population.PopulationLedger.ChangeKind.DEATH));
    }

    @Test void aLastingDisputeBetweenNeighboursBecomesAFeudBetweenTheirFamilies() {
        LivingWorld lw = world();
        Settlement s = village(lw, "Kenka", 0);
        lw.economy().donate(s.id(), "rice", 2000, 0.8, Provenance.of("farm", "kenka", "reservas", 0));
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        lw.npcArrived(a, "Goro", "villager", OW, 2, 64, 2, 0);
        lw.npcArrived(b, "Jiro", "villager", OW, 5, 64, 5, 0);
        var fa = lw.families().familyOf(a).orElseThrow();
        var fb = lw.families().familyOf(b).orElseThrow();
        assertNotEquals(fa.id(), fb.id(), "two strangers, two families");
        rivalPairs.add(new UUID[]{a, b});
        hours(lw, 24 * (yadi.samuraiai.living.sim.ConditionScanner.FEUD_DAYS + 1), List.of());
        assertTrue(lw.quests().quests().stream().anyMatch(q -> q.originKind() == yadi.samuraiai.living.quest.conditions.ConditionKind.DISPUTE),
                "the dispute became a quest: " + lw.quests().quests().stream().map(q -> q.originKind() + " " + q.title()).toList());
        assertEquals(yadi.samuraiai.living.family.registry.FamilyRecord.Relation.RIVAL, fa.relations().get(fb.id()), "after days of it, the families are rivals");
        hours(lw, 24, List.of());
        assertTrue(lw.quests().campaigns().stream().anyMatch(c -> c.originKey().startsWith("grudge:")), "and an old grudge opens a campaign: " + lw.quests().campaigns());
    }

    @Test void aMasterOfATradeKeepsItsSecretAndAFamilyMovesWithItsHead() {
        LivingWorld lw = world();
        Settlement east = village(lw, "Higashi", 0), west = village(lw, "Nishi", 900);
        lw.economy().donate(east.id(), "rice", 2000, 0.8, Provenance.of("farm", "higashi", "reservas", 0));
        UUID smith = UUID.randomUUID();
        lw.npcArrived(smith, "Kaji Tetsu", "blacksmith", OW, 2, 64, 2, 0);
        lw.villages().citizen(smith).orElseThrow().professionHours(2500);
        hours(lw, 25, List.of());
        String key = "trade:blacksmith:" + smith;
        assertTrue(lw.families().techniqueOf(key).isPresent(), "a master smith keeps a secret of the trade");
        assertTrue(lw.families().techniqueOf(key).get().holders().contains(smith));
        var family = lw.families().familyOf(smith).orElseThrow();
        assertEquals(smith, family.head(), "the smith heads the family");
        lw.npcArrived(smith, "Kaji Tetsu", "blacksmith", OW, 902, 64, 2, 0);
        assertEquals(west.id(), lw.families().familyOf(smith).orElseThrow().village(), "the family moved with its head");
        assertEquals(west.id(), lw.villages().villageOf(smith).orElseThrow().id());
    }

    @Test void aCaravanCarriesSurplusBetweenVillages() {
        LivingWorld lw = world();
        Settlement a = village(lw, "Kita", 0), b = village(lw, "Minami", 900);
        for (int i = 0; i < 4; i++) lw.npcArrived(UUID.randomUUID(), "A" + i, i == 0 ? "merchant" : "villager", OW, 3 + i, 64, 3, 0);
        for (int i = 0; i < 4; i++) lw.npcArrived(UUID.randomUUID(), "B" + i, "villager", OW, 903 + i, 64, 3, 0);
        lw.economy().donate(a.id(), "rice", 1500, 0.8, Provenance.of("farm", "kita", "gran cosecha", 0));
        for (String food : List.of("rice", "wheat", "fish", "meat", "meal")) lw.economy().withdraw(b.id(), food, 100000, "vaciado");
        hours(lw, 3 * 24, List.of());
        assertFalse(lw.economy().caravans().isEmpty(), "a merchant of Kita took rice to Minami");
        assertTrue(lw.economy().ledger().total(MovementRecord.Kind.DELIVERED, "rice") > 0 || lw.economy().caravans().stream().anyMatch(c -> c.state() != yadi.samuraiai.living.economy.caravans.Caravan.State.LOADING));
    }

    @Test void warCutsTradeAndOpensQuests() {
        LivingWorld lw = world();
        Settlement a = village(lw, "Kita", 0), b = village(lw, "Minami", 900);
        lw.npcArrived(UUID.randomUUID(), "Guardia", "guard", OW, 903, 64, 3, 0);
        lw.world().scheduleEvent(WorldEventType.WAR, "Guerra en el sur", b.regionId(), null, 0.9, 0, 5 * 1440, Provenance.of("test", "", "facciones", 0), Set.of());
        hours(lw, 3, List.of());
        assertTrue(lw.world().route(a.id(), b.id(), yadi.samuraiai.living.world.roads.RoadNetwork.Preferences.CARAVAN).isEmpty(), "roads into the war are blocked");
        assertTrue(lw.quests().open().stream().anyMatch(q -> q.originKind() == yadi.samuraiai.living.quest.conditions.ConditionKind.WAR), "war becomes a campaign of quests");
        assertNotEquals(yadi.samuraiai.living.village.security.SecurityState.PEACE, lw.villages().village(b.id()).orElseThrow().security().state());
    }

    @Test void aRegionAbandonedForMonthsIsCaughtUpWhenAPlayerReturns() {
        LivingWorld lw = world();
        Settlement a = village(lw, "Lejos", 5000);
        for (int i = 0; i < 5; i++) lw.npcArrived(UUID.randomUUID(), "L" + i, "villager", OW, 5003 + i, 64, 3, 0);
        hours(lw, 2, List.of());
        lw.calendar().advanceMinutes(40L * 1440, "ausencia");
        hours(lw, 2, List.of(new StreamingEngine.Viewer(OW, 5000, 0)));
        assertFalse(of(CatchUpCompletedEvent.class).isEmpty(), "the region was brought up to date in bounded steps");
        assertTrue(lw.economy().ledger().total(MovementRecord.Kind.CONSUMED, "water") > 0, "while nobody watched, people lived");
        assertTrue(lw.world().region(a.regionId()).orElseThrow().lastSimulated() >= lw.calendar().now() - 60);
    }
}
