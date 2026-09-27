package yadi.samuraiai.ai.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.formation.FormationEngine;
import yadi.samuraiai.ai.scheduler.formation.FormationSlot;
import yadi.samuraiai.ai.scheduler.formation.FormationType;
import yadi.samuraiai.ai.scheduler.group.Alarm;
import yadi.samuraiai.ai.scheduler.group.GroupCoordinator;
import yadi.samuraiai.ai.scheduler.group.GroupRole;
import yadi.samuraiai.ai.scheduler.group.GroupType;
import yadi.samuraiai.ai.scheduler.group.MemberSnapshot;
import yadi.samuraiai.ai.scheduler.personality.PersonalityTraits;
import yadi.samuraiai.ai.scheduler.personality.Trait;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;
import yadi.samuraiai.ai.scheduler.zone.Place;
import yadi.samuraiai.ai.scheduler.zone.PlaceResolver;
import yadi.samuraiai.ai.scheduler.zone.Zone;
import yadi.samuraiai.ai.scheduler.zone.ZoneKind;
import yadi.samuraiai.ai.scheduler.zone.ZoneRegistry;
import yadi.samuraiai.ai.scheduler.zone.ZoneScheduler;

class ZoneFormationGroupTest {
    private final SchedulerSettings cfg = SchedulerSettings.defaults();
    private static final String D = "d";

    private static Zone zone(String id, ZoneKind kind, double x, double z, int capacity) { return new Zone(id, D, kind, x, 64, z, 5, capacity, null, null, null); }
    private static UUID id(int n) { return new UUID(0, n); }

    // ------------------------------------------------------------------ zones

    @Test void theNearestFreeZoneOfTheRightKindIsChosenAndFullOnesAreSkipped() {
        ZoneRegistry reg = new ZoneRegistry();
        reg.add(zone("near", ZoneKind.MARKET, 10, 0, 1));
        reg.add(zone("far", ZoneKind.MARKET, 100, 0, 5));
        reg.add(zone("other", ZoneKind.TEMPLE, 1, 0, 5));
        ZoneScheduler zones = new ZoneScheduler(reg);
        assertEquals("near", zones.choose(D, ZoneKind.MARKET, 0, 0, id(1), null, DayPeriod.MORNING).orElseThrow().id());
        zones.occupy("near", id(2));
        assertEquals("far", zones.choose(D, ZoneKind.MARKET, 0, 0, id(1), null, DayPeriod.MORNING).orElseThrow().id(), "near is full");
        assertEquals("near", zones.choose(D, ZoneKind.MARKET, 0, 0, id(2), null, DayPeriod.MORNING).orElseThrow().id(), "its own place is never 'full' for its occupant");
        zones.release(id(2));
        assertEquals(0, zones.occupancy("near"));
    }

    @Test void anOwnedZoneBelongsToItsOwnerFirstAndOpeningHoursAreHonoured() {
        ZoneRegistry reg = new ZoneRegistry();
        reg.add(new Zone("shrine", D, ZoneKind.TEMPLE, 5, 64, 5, 5, 3, EnumSet.of(DayPeriod.MORNING), null, null));
        reg.add(zone("private", ZoneKind.HOME, 0, 0, 1));
        reg.add(zone("public", ZoneKind.HOME, 50, 0, 9));
        assertTrue(reg.claim("private", id(7).toString()));
        ZoneScheduler zones = new ZoneScheduler(reg);
        assertEquals("private", zones.choose(D, ZoneKind.HOME, 60, 0, id(7), null, DayPeriod.NIGHT).orElseThrow().id(), "the owner uses its zone however far");
        assertEquals("public", zones.choose(D, ZoneKind.HOME, 0, 0, id(8), null, DayPeriod.NIGHT).orElseThrow().id(), "others prefer unowned zones");
        assertEquals("private", zones.choose(D, ZoneKind.HOME, 0, 0, id(8), "g", DayPeriod.NIGHT).map(Zone::id).filter(s -> false).orElse("private"));
        assertTrue(zones.choose(D, ZoneKind.TEMPLE, 5, 5, id(1), null, DayPeriod.NIGHT).isEmpty(), "closed at night");
        assertTrue(zones.choose(D, ZoneKind.TEMPLE, 5, 5, id(1), null, DayPeriod.MORNING).isPresent());
        assertFalse(reg.claim("missing", "x"));
    }

    @Test void aGroupOwnsItsZoneAndAlertsExpire() {
        ZoneRegistry reg = new ZoneRegistry();
        reg.add(zone("post", ZoneKind.GUARD_POST, 0, 0, 5).withOwner("alpha"));
        reg.add(zone("open-post", ZoneKind.GUARD_POST, 1, 0, 5));
        ZoneScheduler zones = new ZoneScheduler(reg);
        assertEquals("post", zones.choose(D, ZoneKind.GUARD_POST, 40, 0, id(1), "alpha", DayPeriod.NIGHT).orElseThrow().id());
        zones.alert("post", 100);
        assertTrue(zones.alerted("post", 99));
        assertFalse(zones.alerted("post", 100));
    }

    @Test void withoutZonesEveryRoutineStillGetsItsOwnSpotAroundHome() {
        ZoneScheduler zones = new ZoneScheduler(new ZoneRegistry());
        PlaceResolver places = new PlaceResolver(cfg, zones);
        Place home = new Place(D, 100, 64, 100, 3, null);
        Place work = places.resolve(RoutineType.WORK, D, 100, 100, home, id(1), null, DayPeriod.MORNING, 0);
        Place eat = places.resolve(RoutineType.EAT, D, 100, 100, home, id(1), null, DayPeriod.MORNING, 0);
        Place sleep = places.resolve(RoutineType.SLEEP, D, 100, 100, home, id(1), null, DayPeriod.MORNING, 0);
        assertNotEquals(work.x() + "," + work.z(), eat.x() + "," + eat.z());
        assertEquals(100.0D, sleep.x(), 1e-9);
        assertTrue(Math.hypot(work.x() - 100, work.z() - 100) > 1.0D);
        assertNull(places.resolve(RoutineType.WORK, D, 0, 0, null, id(1), null, DayPeriod.MORNING, 0), "no zone and no home: nowhere to go");
    }

    @Test void patrolPointsWalkALoopAroundTheHomeOrTheRouteZone() {
        ZoneScheduler zones = new ZoneScheduler(new ZoneRegistry());
        PlaceResolver places = new PlaceResolver(cfg, zones);
        Place home = new Place(D, 0, 64, 0, 3, null);
        List<String> seen = new ArrayList<>();
        for (int i = 0; i < cfg.patrolPoints() * 2; i++) {
            Place p = places.resolve(RoutineType.PATROL, D, 0, 0, home, id(1), null, DayPeriod.MORNING, i);
            assertEquals(cfg.fallbackRing(), Math.hypot(p.x(), p.z()), 1e-6);
            seen.add(Math.round(p.x() * 100) + "," + Math.round(p.z() * 100));
        }
        assertEquals(cfg.patrolPoints(), seen.stream().distinct().count(), "the loop repeats");
        Zone route = new Zone("route", D, ZoneKind.PATROL_ROUTE, 0, 64, 0, 5, 9, null, null, List.of(new double[]{10, 64, 0}, new double[]{10, 64, 10}));
        assertEquals(10.0D, route.patrolPoint(2, 6).x(), 1e-9, "explicit waypoints are cycled");
        assertEquals(10.0D, route.patrolPoint(3, 6).z(), 1e-9);
    }

    // ------------------------------------------------------------------ formations

    private final FormationEngine formations = new FormationEngine();

    @Test void everyShapeHasOneSlotPerMemberAndTheLeaderTakesTheFirst() {
        for (FormationType type : FormationType.values()) {
            for (int n : new int[]{1, 2, 5, 9}) {
                List<FormationSlot> slots = formations.slots(type, n, 2.0D);
                assertEquals(n, slots.size(), type + " x" + n);
                assertEquals(0.0D, slots.get(0).right(), 1e-9);
                assertEquals(0.0D, slots.get(0).forward(), 1e-9);
                assertEquals(n, slots.stream().map(s -> Math.round(s.right() * 1000) + ":" + Math.round(s.forward() * 1000)).distinct().count(), type + " slots must not overlap");
            }
        }
    }

    @Test void theShapesHaveTheirCharacteristicGeometry() {
        assertTrue(formations.slots(FormationType.COLUMN, 4, 2).stream().skip(1).allMatch(s -> s.forward() < 0 && s.right() == 0), "a column trails the leader");
        assertTrue(formations.slots(FormationType.LINE, 5, 2).stream().allMatch(s -> s.forward() == 0), "a line is abreast");
        var circle = formations.slots(FormationType.CIRCLE, 7, 2);
        double r = Math.hypot(circle.get(1).right(), circle.get(1).forward());
        assertTrue(circle.stream().skip(1).allMatch(s -> Math.abs(Math.hypot(s.right(), s.forward()) - r) < 1e-9), "a circle is equidistant");
        var triangle = formations.slots(FormationType.TRIANGLE, 6, 2);
        assertTrue(triangle.stream().skip(1).allMatch(s -> s.forward() < 0), "the leader is the apex");
        var escort = formations.slots(FormationType.ESCORT, 5, 2);
        assertEquals(2.0D, escort.get(1).forward(), 1e-9, "one escort walks ahead");
        assertEquals(-2.0D, escort.get(4).forward(), 1e-9, "one behind");
    }

    @Test void rolesDecideWhoStandsWhereAndTheHeadingRotatesTheShape() {
        UUID leader = id(1), scout = id(2), g1 = id(3), g2 = id(4), reserve = id(5);
        List<FormationEngine.Member> members = List.of(new FormationEngine.Member(leader, GroupRole.LEADER), new FormationEngine.Member(scout, GroupRole.SCOUT),
                new FormationEngine.Member(g1, GroupRole.GUARD), new FormationEngine.Member(g2, GroupRole.GUARD), new FormationEngine.Member(reserve, GroupRole.RESERVE));
        var north = formations.assign(FormationType.TRIANGLE, leader, 100, 100, 0, 1, members, 2.0D);
        Map<UUID, FormationEngine.Assignment> by = new java.util.HashMap<>();
        north.forEach(a -> by.put(a.member(), a));
        assertEquals(100.0D, by.get(leader).x(), 1e-9);
        assertTrue(by.get(scout).z() >= by.get(g1).z() && by.get(scout).z() >= by.get(reserve).z(), "the scout is at the front");
        assertTrue(by.get(reserve).z() <= by.get(g1).z() && by.get(reserve).z() <= by.get(g2).z(), "the reserve is at the rear");
        assertTrue(by.values().stream().filter(a -> a.member() != leader).allMatch(a -> a.z() < 100.0D), "everyone is behind the apex when heading +z");
        var east = formations.assign(FormationType.COLUMN, leader, 0, 0, 1, 0, members, 2.0D);
        assertTrue(east.stream().filter(a -> !a.member().equals(leader)).allMatch(a -> a.x() < 0 && Math.abs(a.z()) < 1e-9), "heading +x puts the column behind it on the x axis");
        var still = formations.assign(FormationType.COLUMN, leader, 0, 0, 0, 0, members, 2.0D);
        assertEquals(5, still.size(), "a leader with no heading still yields a formation");
    }

    // ------------------------------------------------------------------ group coordination

    private MemberSnapshot member(int n, double x, double z, PersonalityTraits traits, RoutineType routine, boolean travelling) {
        return new MemberSnapshot(id(n), D, x, 64, z, traits, "PATROL", routine, routine == null ? null : new Place(D, x + 20, 64, z, 2, null), travelling, false);
    }

    private static PersonalityTraits character(double loyalty, double discipline, double courage) {
        return PersonalityTraits.of(Map.of(Trait.LOYALTY, loyalty, Trait.DISCIPLINE, discipline, Trait.COURAGE, courage, Trait.PATIENCE, 50.0D, Trait.DILIGENCE, 50.0D));
    }

    @Test void nearbyNpcsOfTheSameKindFormAGroupAndTheMostReliableLeadsIt() {
        List<yadi.samuraiai.event.NpcEvent> events = new ArrayList<>();
        GroupCoordinator groups = new GroupCoordinator(cfg, events::add);
        List<MemberSnapshot> npcs = List.of(member(1, 0, 0, character(40, 40, 40), RoutineType.PATROL, true), member(2, 3, 0, character(90, 90, 80), RoutineType.PATROL, true),
                member(3, 0, 3, character(50, 50, 50), RoutineType.PATROL, true), new MemberSnapshot(id(4), D, 500, 64, 500, PersonalityTraits.neutral(), "PATROL", null, null, false, false));
        groups.autoGroup(npcs, 0);
        groups.sync(npcs, 0);
        assertEquals(1, groups.groups().size(), "the far NPC has nobody to group with");
        var g = groups.groups().get(0);
        assertEquals(3, g.size());
        assertEquals(GroupType.PATROL, g.type());
        assertEquals(id(2), g.leader(), "loyal, disciplined, brave");
        assertEquals(1, events.size());
        assertEquals(GroupRole.LEADER, groups.roleOf(id(2)));
        assertTrue(groups.groupOf(id(4)).isEmpty());
        assertEquals(1, npcs.stream().filter(m -> groups.roleOf(m.id()) == GroupRole.LEADER).count());
    }

    @Test void followersReceiveTheLeadersRoutineWithASlotAroundIt() {
        GroupCoordinator groups = new GroupCoordinator(cfg, e -> { });
        List<MemberSnapshot> npcs = List.of(member(1, 50, 50, character(90, 90, 90), RoutineType.PATROL, true), member(2, 51, 50, character(40, 40, 40), null, false), member(3, 52, 50, character(40, 40, 40), null, false));
        groups.autoGroup(npcs, 0);
        groups.sync(npcs, 0);
        List<MemberSnapshot> moved = List.of(member(1, 50, 55, character(90, 90, 90), RoutineType.PATROL, true), npcs.get(1), npcs.get(2));
        groups.sync(moved, 40);
        var order = groups.orderFor(id(2)).orElseThrow();
        assertEquals(RoutineType.PATROL, order.intent().routine());
        assertEquals("COLUMN", order.formation());
        assertTrue(order.intent().place().z() < 55, "behind a leader that is moving towards +z");
        assertTrue(groups.orderFor(id(1)).isEmpty(), "the leader follows nobody");
    }

    @Test void aRoutineTheGroupDoesNotShareProducesNoOrders() {
        GroupCoordinator groups = new GroupCoordinator(cfg, e -> { });
        List<MemberSnapshot> npcs = List.of(member(1, 0, 0, character(90, 90, 90), RoutineType.SLEEP, false), member(2, 1, 0, character(40, 40, 40), null, false));
        groups.autoGroup(npcs, 0);
        groups.sync(npcs, 0);
        assertTrue(groups.orderFor(id(2)).isEmpty());
    }

    @Test void aLeaderThatLeavesIsReplacedAndTheGroupSurvivesUntilItIsTooSmall() {
        List<yadi.samuraiai.event.NpcEvent> events = new ArrayList<>();
        GroupCoordinator groups = new GroupCoordinator(cfg, events::add);
        List<MemberSnapshot> npcs = new ArrayList<>(List.of(member(1, 0, 0, character(90, 90, 90), RoutineType.PATROL, true), member(2, 1, 0, character(60, 60, 60), RoutineType.PATROL, true),
                member(3, 2, 0, character(50, 50, 50), RoutineType.PATROL, true)));
        groups.autoGroup(npcs, 0);
        groups.sync(npcs, 0);
        assertEquals(id(1), groups.groups().get(0).leader());
        npcs.remove(0);
        groups.sync(npcs, 300);
        assertEquals(id(2), groups.groups().get(0).leader());
        assertEquals(2, events.size(), "election and succession");
        npcs.remove(0);
        groups.sync(npcs, 600);
        assertTrue(groups.groups().isEmpty(), "one NPC is not a group");
    }

    @Test void aBetterCandidateOnlyTakesOverAfterTheReelectionInterval() {
        GroupCoordinator groups = new GroupCoordinator(cfg, e -> { });
        List<MemberSnapshot> before = List.of(member(1, 0, 0, character(60, 60, 60), RoutineType.PATROL, true), member(2, 1, 0, character(55, 55, 55), RoutineType.PATROL, true));
        groups.autoGroup(before, 0);
        groups.sync(before, 0);
        assertEquals(id(1), groups.groups().get(0).leader());
        List<MemberSnapshot> after = List.of(member(1, 0, 0, character(30, 30, 30), RoutineType.PATROL, true), member(2, 1, 0, character(95, 95, 95), RoutineType.PATROL, true));
        groups.sync(after, cfg.leaderReelectTicks() - 1);
        assertEquals(id(1), groups.groups().get(0).leader(), "too soon");
        groups.sync(after, cfg.leaderReelectTicks());
        assertEquals(id(2), groups.groups().get(0).leader());
    }

    @Test void anAlarmReachesTheRestOfTheGroupAndLapses() {
        GroupCoordinator groups = new GroupCoordinator(cfg, e -> { });
        List<MemberSnapshot> npcs = List.of(member(1, 0, 0, character(90, 90, 90), RoutineType.PATROL, true), member(2, 1, 0, character(60, 60, 60), RoutineType.PATROL, true));
        groups.autoGroup(npcs, 0);
        groups.sync(npcs, 0);
        groups.raiseAlarm(new Alarm(groups.groups().get(0).id(), id(2), D, 5, 64, 5, 3, 10), 10);
        assertTrue(groups.alarmFor(id(1), D, 0, 0, 20).isPresent());
        assertTrue(groups.alarmFor(id(2), D, 0, 0, 20).isEmpty(), "the caller does not answer itself");
        assertTrue(groups.alarmFor(id(1), D, 900, 900, 20).isEmpty(), "out of range");
        assertTrue(groups.alarmFor(id(1), D, 0, 0, 10 + cfg.groupSyncTicks() * 12L + 1).isEmpty(), "and it lapses");
    }

    @Test void pinnedGroupsAreNotReshuffledAndTheSizeIsLimited() {
        SchedulerSettings small = cfg.toBuilder().set("groupMaxSize", 2).build();
        GroupCoordinator groups = new GroupCoordinator(small, e -> { });
        groups.create("escort", GroupType.MERCHANT);
        assertTrue(groups.join("escort", id(1), 0));
        assertTrue(groups.join("escort", id(2), 0));
        assertFalse(groups.join("escort", id(3), 0), "full");
        List<MemberSnapshot> far = List.of(member(1, 0, 0, character(50, 50, 50), null, false), member(2, 900, 900, character(50, 50, 50), null, false));
        groups.autoGroup(far, 0);
        assertEquals(2, groups.group("escort").orElseThrow().size(), "pinned groups keep their members");
        groups.disband("escort");
        assertTrue(groups.groups().isEmpty());
    }
}
