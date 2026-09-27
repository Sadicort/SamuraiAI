package yadi.samuraiai.living.quest;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.calendar.engine.CalendarSettings;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.persistence.LivingStorage;
import yadi.samuraiai.living.quest.api.QuestPorts;
import yadi.samuraiai.living.quest.campaigns.Campaign;
import yadi.samuraiai.living.quest.conditions.ConditionKind;
import yadi.samuraiai.living.quest.conditions.WorldCondition;
import yadi.samuraiai.living.quest.engine.QuestEngine;
import yadi.samuraiai.living.quest.engine.QuestSettings;
import yadi.samuraiai.living.quest.events.QuestCompletedEvent;
import yadi.samuraiai.living.quest.events.QuestConsequenceAppliedEvent;
import yadi.samuraiai.living.quest.objectives.ObjectiveType;
import yadi.samuraiai.living.quest.objectives.QuestObjective;
import yadi.samuraiai.living.quest.persistence.QuestStorage;
import yadi.samuraiai.living.quest.runtime.Quest;
import yadi.samuraiai.living.quest.templates.QuestTemplate;

class QuestEngineTest {
    private static final String OW = "minecraft:overworld";
    private final List<NpcEvent> events = new ArrayList<>();
    private final CalendarEngine calendar = new CalendarEngine(CalendarSettings::defaults, e -> { }, 9L);
    private final UUID village = UUID.randomUUID(), other = UUID.randomUUID(), region = UUID.randomUUID(), player = UUID.randomUUID();
    private final UUID farmer = UUID.randomUUID(), merchant = UUID.randomUUID(), guard = UUID.randomUUID();
    private final Map<String, Double> paid = new HashMap<>();
    private final List<String> calls = new ArrayList<>();
    private double trust = 50;

    private QuestEngine engine() {
        QuestEngine e = new QuestEngine(QuestSettings::defaults, events::add, calendar, 1L);
        e.useWorld(new QuestPorts.World() {
            @Override public String settlementName(UUID s) { return s.equals(village) ? "Sakura" : "Kawa"; }
            @Override public Optional<QuestPorts.Place> settlementPlace(UUID s) { return Optional.of(s.equals(village) ? new QuestPorts.Place(OW, 0, 64, 0) : new QuestPorts.Place(OW, 800, 64, 0)); }
            @Override public String regionName(UUID r) { return "Kuroyama"; }
            @Override public Optional<QuestPorts.Place> regionCenter(UUID r) { return Optional.of(new QuestPorts.Place(OW, 256, 70, 256)); }
            @Override public Optional<UUID> neighbour(UUID s) { return Optional.of(other); }
            @Override public boolean resolveEvent(UUID ev, boolean ok, String by, String out) { calls.add("resolve:" + ev); return true; }
            @Override public Optional<UUID> spawnEvent(String t, UUID r, UUID s, double sev, String c) { calls.add("spawn:" + t); return Optional.of(UUID.randomUUID()); }
            @Override public boolean buildRoad(UUID a, UUID b) { calls.add("road"); return true; }
            @Override public String seasonName() { return "SPRING"; }
            @Override public String weatherName(UUID r) { return "SUNNY"; }
            @Override public String moonName() { return "FULL"; }
        });
        e.useVillages(new QuestPorts.Villages() {
            @Override public Optional<QuestPorts.Giver> giver(UUID s, List<String> professions) {
                for (String p : professions) {
                    if (p.equals("farmer")) return Optional.of(new QuestPorts.Giver(farmer, "Hana", "farmer"));
                    if (p.equals("merchant")) return Optional.of(new QuestPorts.Giver(merchant, "Mei", "merchant"));
                    if (p.equals("guard") || p.equals("samurai")) return Optional.of(new QuestPorts.Giver(guard, "Shin", "guard"));
                }
                return Optional.of(new QuestPorts.Giver(farmer, "Hana", "farmer"));
            }
            @Override public Optional<QuestPorts.Place> building(UUID s, String kind) { return Optional.of(new QuestPorts.Place(OW, 0, 64, -30)); }
            @Override public void renown(UUID s, double d, String c) { calls.add("renown:" + d); }
            @Override public void unrest(UUID s, double d) { calls.add("unrest:" + d); }
            @Override public boolean buildHouse(UUID s) { calls.add("house"); return true; }
            @Override public String culture(UUID s) { return "village"; }
        });
        e.useEconomy(new QuestPorts.Economy() {
            @Override public double reward(UUID s, UUID p, String n, double coins, String reason) { paid.merge("coins", coins, Double::sum); return coins; }
            @Override public double giveItems(UUID s, UUID p, String r, double q, String reason) { paid.merge(r, q, Double::sum); return q; }
            @Override public double price(UUID s, String r) { return 2; }
            @Override public boolean isFood(String r) { return Set.of("rice", "wheat", "fish", "meat", "meal").contains(r); }
            @Override public void loot(UUID s, double f, String c) { calls.add("loot:" + f); }
        });
        e.useSocial(new QuestPorts.Social() {
            @Override public double trust(UUID npc, UUID p) { return trust; }
            @Override public String mood(UUID npc) { return "CALM"; }
            @Override public double standing(UUID p, UUID s, String c) { return 0; }
            @Override public void adjustStanding(UUID p, String n, UUID s, String c, double a) { calls.add("standing:" + c + ":" + a); }
            @Override public void experience(UUID npc, UUID p, String n, String kind, String note) { calls.add("experience:" + kind); }
            @Override public void witnesses(UUID s, UUID p, String n, String kind, String note) { calls.add("witness:" + kind); }
        });
        e.useFamilies(new QuestPorts.Families() {
            @Override public void honor(String subject, double d, String c) { calls.add("honor:" + subject); }
            @Override public void memory(String subject, String t) { calls.add("familymemory:" + subject); }
            @Override public void mentorship(UUID m, UUID d, double p) { calls.add("mentorship"); }
        });
        e.useChronicle((m, c, t, d, s, x, p) -> calls.add("history:" + c));
        return e;
    }

    private WorldCondition condition(ConditionKind kind, String key, double severity, Map<String, String> vars) {
        return new WorldCondition(kind, key, village, region, vars.getOrDefault("resource", ""), severity, Provenance.of("economy", village.toString(), "prueba", calendar.now()), vars);
    }
    private <T> List<T> of(Class<T> type) { return events.stream().filter(type::isInstance).map(type::cast).toList(); }
    private QuestObjective objective(Quest q, ObjectiveType type) { return q.relevant().stream().filter(o -> o.type() == type).findFirst().orElseThrow(); }

    @Test void aScarcityBecomesAFoodQuestWithACauseAGiverAndAPlace() {
        QuestEngine e = engine();
        Quest q = e.report(condition(ConditionKind.SCARCITY, "scarcity:food", 0.7, Map.of("resource", "food", "quantity", "30"))).orElseThrow();
        assertEquals("village_food_shortage", q.template());
        assertEquals("Hambre en Sakura", q.title());
        assertEquals("Hana", q.giverName());
        assertEquals("prueba", q.cause().cause());
        assertEquals(30, objective(q, ObjectiveType.DELIVER).required(), 1e-9);
        assertTrue(objective(q, ObjectiveType.DELIVER).placed());
        assertSame(q, e.report(condition(ConditionKind.SCARCITY, "scarcity:food", 0.75, Map.of("resource", "food"))).orElseThrow(), "the same problem never makes two quests");
    }

    @Test void deliveringCompletesTheQuestPaysFromTheTreasuryAndChangesTheWorld() {
        QuestEngine e = engine();
        Quest q = e.report(condition(ConditionKind.SCARCITY, "scarcity:food", 0.7, Map.of("resource", "food", "quantity", "20"))).orElseThrow();
        assertTrue(e.accept(q.id(), player, "Ren").ok());
        assertTrue(e.choose(q.id(), player, yadi.samuraiai.living.quest.branching.Path.PEACEFUL).ok());
        assertEquals(12, e.delivered(player, village, "rice", 12), 1e-9);
        assertEquals(Quest.State.ACTIVE, q.state());
        assertEquals(8, e.delivered(player, village, "fish", 20), 1e-9, "only what is still needed counts");
        assertEquals(Quest.State.COMPLETED, q.state());
        assertTrue(paid.get("coins") > 0);
        assertTrue(calls.contains("standing:village:0.08"));
        assertTrue(calls.contains("experience:HELPED_ME"));
        assertTrue(calls.stream().anyMatch(c -> c.startsWith("renown:")));
        assertTrue(calls.contains("history:ECONOMY"));
        assertEquals(1, of(QuestCompletedEvent.class).size());
        assertEquals(1, e.history().of(player).size());
        assertTrue(e.report(condition(ConditionKind.SCARCITY, "scarcity:food", 0.9, Map.of())).isEmpty(), "cooldown after a quest on the same problem");
    }

    @Test void pathsChangeObjectivesAndConsequences() {
        QuestEngine e = engine();
        UUID bandits = UUID.randomUUID();
        Quest q = e.report(condition(ConditionKind.BANDITS, "bandits:road", 0.8, Map.of("eventId", bandits.toString(), "destination", other.toString(), "x", "400", "z", "0"))).orElseThrow();
        if (!q.template().equals("open_trade_route")) q = e.report(condition(ConditionKind.ROUTE_BLOCKED, "route:x", 0.8, Map.of("eventId", bandits.toString(), "destination", other.toString(), "x", "400", "z", "0"))).orElseThrow();
        e.accept(q.id(), player, "Ren");
        assertNull(q.path(), "three ways: the player must choose");
        trust = 10;
        assertFalse(e.paths(q, player).stream().anyMatch(b -> b.path() == yadi.samuraiai.living.quest.branching.Path.DIPLOMATIC), "negotiating needs the giver's trust");
        trust = 50;
        assertTrue(e.choose(q.id(), player, yadi.samuraiai.living.quest.branching.Path.VIOLENT).ok());
        assertEquals(QuestObjective.State.SKIPPED, q.objectives().stream().filter(o -> o.branch() == yadi.samuraiai.living.quest.branching.Path.STEALTH).findFirst().orElseThrow().state());
        QuestObjective look = objective(q, ObjectiveType.INVESTIGATE);
        e.arrived(player, OW, look.x(), look.y(), look.z());
        for (int i = 0; i < 4; i++) e.killed(player, "zombie", true, OW, look.x() + 5, look.y(), look.z());
        assertEquals(Quest.State.COMPLETED, q.state());
        assertTrue(calls.contains("resolve:" + bandits), "the bandits' world event is resolved");
        assertFalse(calls.contains("road"), "the stealth consequence did not apply");
    }

    @Test void aGiverWhoDoesNotTrustThePlayerDoesNotAsk() {
        QuestEngine e = engine();
        Quest q = e.report(new WorldCondition(ConditionKind.GRATITUDE, "grat:1", village, region, "", 0.8, Provenance.of("memory", "", "", 0),
                Map.of("giver", farmer.toString(), "giverName", "Hana"))).orElseThrow();
        trust = 20;
        assertFalse(e.accept(q.id(), player, "Ren").ok());
        trust = 80;
        assertTrue(e.accept(q.id(), player, "Ren").ok());
    }

    @Test void defendersWinTheDefenceAndAbsentPlayersDoNot() {
        QuestEngine e = engine();
        UUID attack = UUID.randomUUID();
        Quest q = e.report(condition(ConditionKind.ATTACK, "attack:1", 0.9, Map.of("eventId", attack.toString()))).orElseThrow();
        e.accept(q.id(), player, "Ren");
        e.choose(q.id(), player, yadi.samuraiai.living.quest.branching.Path.HONOR);
        e.arrived(player, OW, 5, 64, 5);
        e.worldEventEnded(attack, true);
        assertEquals(Quest.State.COMPLETED, q.state());
        assertTrue(calls.contains("witness:HONOR_OBSERVED"));
        assertTrue(calls.contains("honor:npc:" + guard));

        QuestEngine f = engine();
        UUID attack2 = UUID.randomUUID();
        Quest r = f.report(condition(ConditionKind.ATTACK, "attack:2", 0.9, Map.of("eventId", attack2.toString()))).orElseThrow();
        f.accept(r.id(), player, "Ren");
        f.choose(r.id(), player, yadi.samuraiai.living.quest.branching.Path.HONOR);
        f.worldEventEnded(attack2, false);
        assertEquals(Quest.State.FAILED, r.state());
        assertTrue(calls.stream().anyMatch(c -> c.startsWith("loot:")), "the fallen village is looted");
    }

    @Test void questsTwistMergeAndExpire() {
        QuestEngine e = engine();
        Quest q = e.report(condition(ConditionKind.SCARCITY, "scarcity:food", 0.4, Map.of("resource", "food", "quantity", "20"))).orElseThrow();
        e.accept(q.id(), player, "Ren");
        e.report(condition(ConditionKind.SCARCITY, "scarcity:food", 0.9, Map.of()));
        assertEquals(QuestTemplate.StoryStage.TWIST, q.stage());
        assertEquals(30, objective(q, ObjectiveType.DELIVER).required(), 1e-9);
        e.report(condition(ConditionKind.SCARCITY, "scarcity:food:neighbourhood", 0.6, Map.of("resource", "food", "quantity", "10")));
        e.report(condition(ConditionKind.SCARCITY, "scarcity:water", 0.6, Map.of("resource", "water", "quantity", "10")));
        assertEquals(1, q.merged());
        assertEquals(40, objective(q, ObjectiveType.DELIVER).required(), 1e-9);

        QuestEngine f = engine();
        Quest ignored = f.report(condition(ConditionKind.FIRE, "fire:1", 0.8, Map.of())).orElseThrow();
        calendar.advanceMinutes(4 * 1440, "test");
        for (int i = 0; i < 20; i++) f.tick();
        assertEquals(Quest.State.EXPIRED, ignored.state());
        assertTrue(calls.stream().anyMatch(c -> c.startsWith("unrest:")), "nobody helped: the village is left with the consequences");
    }

    @Test void theWorldCanSolveAProblemBeforeThePlayer() {
        QuestEngine e = engine();
        Quest q = e.report(condition(ConditionKind.SCARCITY, "scarcity:food", 0.7, Map.of())).orElseThrow();
        e.conditionResolved("scarcity:food");
        assertEquals(Quest.State.RESOLVED_BY_WORLD, q.state());
    }

    @Test void campaignsAndChainsContinueTheStory() {
        QuestEngine e = engine();
        UUID bandits = UUID.randomUUID();
        Quest first = e.report(condition(ConditionKind.ROUTE_BLOCKED, "route:1", 0.8, Map.of("eventId", bandits.toString(), "destination", other.toString(), "x", "400", "z", "0"))).orElseThrow();
        assertEquals("open_trade_route", first.template());
        Campaign camp = e.campaigns().iterator().next();
        e.accept(first.id(), player, "Ren");
        e.choose(first.id(), player, yadi.samuraiai.living.quest.branching.Path.STEALTH);
        QuestObjective look = objective(first, ObjectiveType.INVESTIGATE);
        e.arrived(player, OW, look.x(), look.y(), look.z());
        QuestObjective detour = first.relevant().stream().filter(o -> o.branch() == yadi.samuraiai.living.quest.branching.Path.STEALTH).findFirst().orElseThrow();
        e.arrived(player, OW, detour.x(), detour.y(), detour.z());
        assertEquals(Quest.State.COMPLETED, first.state());
        assertTrue(calls.contains("road"), "the stealth path opened a new trail");
        assertEquals(1, camp.stage());
        Quest escort = e.quests().stream().filter(x -> x.template().equals("escort_caravan")).findFirst().orElseThrow();
        assertEquals(player, escort.reservedFor());

        QuestEngine f = engine();
        Quest smith = f.report(condition(ConditionKind.WORKSHOP_STARVED, "starved:iron", 0.6, Map.of("resource", "iron", "quantity", "5"))).orElseThrow();
        f.accept(smith.id(), player, "Ren");
        f.talked(player, smith.giver(), smith.giverProfession());
        f.delivered(player, village, "iron", 5);
        assertEquals(Quest.State.COMPLETED, smith.state());
        assertTrue(f.quests().stream().anyMatch(x -> x.template().equals("forged_gift") && smith.id().equals(x.parent())));
    }

    @Test void questsSurviveARestart() throws Exception {
        Path dir = Files.createTempDirectory("living-quest");
        QuestEngine a = engine();
        Quest q = a.report(condition(ConditionKind.SCARCITY, "scarcity:food", 0.7, Map.of("resource", "food", "quantity", "20"))).orElseThrow();
        a.accept(q.id(), player, "Ren");
        a.choose(q.id(), player, yadi.samuraiai.living.quest.branching.Path.PEACEFUL);
        a.delivered(player, village, "rice", 5);
        LivingStorage store = new LivingStorage(dir, false);
        QuestStorage.sections(a).forEach(store::register);
        assertEquals(1, store.saveAll());
        QuestEngine b = engine();
        LivingStorage load = new LivingStorage(dir, false);
        QuestStorage.sections(b).forEach(load::register);
        load.loadAll().values().forEach(r -> assertTrue(r.usable(), r.detail()));
        Quest restored = b.quest(q.id()).orElseThrow();
        assertEquals(Quest.State.ACTIVE, restored.state());
        assertEquals(5, objective(restored, ObjectiveType.DELIVER).progress(), 1e-9);
        b.delivered(player, village, "rice", 15);
        assertEquals(Quest.State.COMPLETED, restored.state());
        assertFalse(of(QuestConsequenceAppliedEvent.class).isEmpty());
    }
}
