package yadi.samuraiai.living.sim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.event.NpcEvent;
import yadi.samuraiai.living.calendar.events.DayChangedEvent;
import yadi.samuraiai.living.calendar.events.HarvestOutlookEvent;
import yadi.samuraiai.living.calendar.events.HolidayEvent;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.economy.events.CaravanAmbushedEvent;
import yadi.samuraiai.living.economy.events.CaravanArrivedEvent;
import yadi.samuraiai.living.economy.events.CaravanLostEvent;
import yadi.samuraiai.living.economy.events.ScarcityEndedEvent;
import yadi.samuraiai.living.economy.events.ScarcityStartedEvent;
import yadi.samuraiai.living.economy.events.TradeRouteDisruptedEvent;
import yadi.samuraiai.living.economy.events.TradeRouteRestoredEvent;
import yadi.samuraiai.living.economy.production.Recipe;
import yadi.samuraiai.living.family.lifecycle.LifeState;
import yadi.samuraiai.living.family.events.LifeStateChangedEvent;
import yadi.samuraiai.living.quest.conditions.ConditionKind;
import yadi.samuraiai.living.quest.conditions.WorldCondition;
import yadi.samuraiai.living.quest.engine.QuestEngine;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.buildings.BuildingKind;
import yadi.samuraiai.living.village.citizens.Citizen;
import yadi.samuraiai.living.village.events.CitizenJoinedEvent;
import yadi.samuraiai.living.village.events.CitizenLeftEvent;
import yadi.samuraiai.living.village.events.HousingShortageEvent;
import yadi.samuraiai.living.village.events.ProfessionAssignedEvent;
import yadi.samuraiai.living.village.events.VillagePopulationChangedEvent;
import yadi.samuraiai.living.village.life.VillageEventKind;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.world.events.WorldEventRecord;
import yadi.samuraiai.living.world.events.WorldEventType;
import yadi.samuraiai.living.world.events_api.RegionCreatedEvent;
import yadi.samuraiai.living.world.events_api.RoadBlockedEvent;
import yadi.samuraiai.living.world.events_api.RoadReopenedEvent;
import yadi.samuraiai.living.world.events_api.SettlementFoundedEvent;
import yadi.samuraiai.living.world.events_api.WorldEventPhaseEvent;
import yadi.samuraiai.living.world.regions.Region;
import yadi.samuraiai.living.world.settlements.Settlement;
import yadi.samuraiai.living.world.settlements.SettlementType;
import yadi.samuraiai.living.world.simulation.RegionSimulator;
import yadi.samuraiai.living.world.streaming.SimulationLevel;

/**
 * The cross-system flows of the specification (Part VIII), carried on events instead of polling:
 * <ul>
 *   <li>World → Village / Economy / Calendar: a new settlement gets its village and its economy; a new region its weather.</li>
 *   <li>World events → Village, Economy, Quests: attacks set security and open defence quests, fires burn stores and damage a
 *       house, markets and celebrations change village life, bandits and war open quests; when they end, quests close.</li>
 *   <li>Roads → Economy: a blocked road re-plans trade routes (and opens a quest).</li>
 *   <li>Economy → Quests / Villages: scarcity, a workshop without material, a lost caravan make quests; a caravan's arrival
 *       brings a merchant visitor and completes escorts.</li>
 *   <li>Calendar → Economy / World / Families: harvests reach the markets (a great one is celebrated); Obon honours ancestors;
 *       each new day runs the daily work.</li>
 *   <li>Village → World / Family / Economy: population reaches the world's ledger; a new citizen gets a family and, if a
 *       merchant, a stall; a citizen who leaves changes their family's records.</li>
 *   <li>Family → Village / Quests: someone missing opens a search.</li>
 * </ul>
 * It also registers the region simulators (villages, then economy) with the world, so each region's step advances them in
 * that order.
 */
final class LivingReactions {
    private final LivingWorld w;

    LivingReactions(LivingWorld w) {
        this.w = w;
        w.world.registerSimulator(new RegionSimulator() {
            @Override public String name() { return "villages"; }
            @Override public void simulate(Region region, long from, long to, SimulationLevel level, boolean catchUp) {
                for (Settlement s : w.world.settlementsIn(region.id())) if (w.villages.village(s.id()).isPresent()) w.villages.simulate(s.id(), from, to, level == SimulationLevel.FULL);
            }
        });
        w.world.registerSimulator(new RegionSimulator() {
            @Override public String name() { return "economy"; }
            @Override public void simulate(Region region, long from, long to, SimulationLevel level, boolean catchUp) {
                for (Settlement s : w.world.settlementsIn(region.id())) w.economy.simulate(s.id(), from, to);
            }
        });
    }

    private void report(ConditionKind kind, UUID settlement, UUID region, String subject, double severity, String cause, Map<String, String> vars) {
        w.metrics().conditions.incrementAndGet();
        w.quests.report(new WorldCondition(kind, QuestEngine.keyOf(kind, settlement, subject), settlement, region, subject, severity,
                Provenance.of(kind.name().toLowerCase(java.util.Locale.ROOT), subject, cause, w.calendar.now()), vars));
    }

    private UUID regionOf(UUID settlement) { return w.world.settlement(settlement).map(Settlement::regionId).orElse(null); }

    void handle(NpcEvent event) {
        w.metrics().reactions.incrementAndGet();
        if (event instanceof RegionCreatedEvent e) {
            w.world.region(e.regionId()).ifPresent(r -> w.calendar.trackWeather(r.scope(), r.type().climate(), r.altitude()));
        } else if (event instanceof SettlementFoundedEvent e) {
            founded(e);
        } else if (event instanceof DayChangedEvent e) {
            w.newDay(e.dayIndex(), e.skippedDays());
        } else if (event instanceof HolidayEvent e) { if (e.tags().contains("ancestors")) w.families.ancestorsHonoured();
        } else if (event instanceof HarvestOutlookEvent e) {
            harvest(e);
        } else if (event instanceof WorldEventPhaseEvent e) {
            worldEvent(e);
        } else if (event instanceof RoadBlockedEvent e) {
            w.economy.roadChanged(e.edgeId(), true);
        } else if (event instanceof RoadReopenedEvent e) {
            w.economy.roadChanged(e.edgeId(), false);
        } else if (event instanceof ScarcityStartedEvent e) {
            scarcity(e);
        } else if (event instanceof ScarcityEndedEvent e) {
                w.quests.conditionResolved(QuestEngine.keyOf(ConditionKind.SCARCITY, e.settlementId(), e.resource()));
                w.quests.conditionResolved(QuestEngine.keyOf(ConditionKind.WORKSHOP_STARVED, e.settlementId(), e.resource()));
        } else if (event instanceof CaravanLostEvent e) {
            report(ConditionKind.LOST_CARAVAN, e.origin(), regionOf(e.origin()), e.destination().toString(), 0.6, "caravana perdida",
                    Map.of("destination", e.destination().toString(), "caravanId", e.caravanId().toString()));
        } else if (event instanceof CaravanAmbushedEvent e) {
            w.economy.caravan(e.caravanId()).ifPresent(c -> w.quests.caravanAmbushed(c.origin()));
        } else if (event instanceof CaravanArrivedEvent e) {
            caravanArrived(e);
        } else if (event instanceof TradeRouteDisruptedEvent e) {
            routeDisrupted(e);
        } else if (event instanceof TradeRouteRestoredEvent e) {
            w.quests.conditionResolved(QuestEngine.keyOf(ConditionKind.ROUTE_BLOCKED, e.origin(), e.destination().toString()));
        } else if (event instanceof VillagePopulationChangedEvent e) {
            w.world.reportPopulation(e.villageId(), e.population(), e.professions());
        } else if (event instanceof CitizenJoinedEvent e) {
            citizenJoined(e);
        } else if (event instanceof CitizenLeftEvent e) {
            citizenLeft(e);
        } else if (event instanceof ProfessionAssignedEvent e) {
                w.families.professionChanged(e.npcId(), e.profession());
                if ("merchant".equals(e.profession()) && e.villageId() != null && w.economy.settlement(e.villageId()).isPresent())
                    w.villages.citizen(e.npcId()).ifPresent(c -> w.economy.registerMerchant(c.id(), c.name(), e.villageId()));
        } else if (event instanceof HousingShortageEvent e) {
            report(ConditionKind.HOUSING_SHORTAGE, e.villageId(), regionOf(e.villageId()), "houses", Math.min(1, 0.3 + e.homeless() * 0.1), "gente sin cama",
                    Map.of("quantity", String.valueOf(10 + e.homeless() * 4)));
        } else if (event instanceof LifeStateChangedEvent e) {
            lifeState(e);
        } else if (event instanceof yadi.samuraiai.living.family.events.BirthRegisteredEvent e) {
            UUID village = w.families.family(e.familyId()).map(f -> f.village()).orElse(null);
            w.world.population().record(new yadi.samuraiai.living.world.population.PopulationLedger.Change(e.minute(), yadi.samuraiai.living.world.population.PopulationLedger.ChangeKind.BIRTH,
                    village, e.person(), e.name(), "nacimiento"));
        } else if (event instanceof yadi.samuraiai.living.family.events.HeirloomLostEvent e) {
            heirloomLost(e);
        } else if (event instanceof yadi.samuraiai.living.family.events.FamilyReputationChangedEvent e) {
            familyDishonored(e);
        }
    }

    /** A family's own giver for a quest about it: a living member present as a citizen somewhere, and the village they're in. */
    private Optional<Map.Entry<UUID, UUID>> familyGiver(UUID familyId) {
        var relative = w.families.living(familyId).stream().filter(p -> w.villages.citizen(p.id()).map(Citizen::present).orElse(false)).findFirst();
        if (relative.isEmpty()) return Optional.empty();
        UUID village = w.villages.citizen(relative.get().id()).map(Citizen::village).orElse(null);
        return village == null ? Optional.empty() : Optional.of(Map.entry(relative.get().id(), village));
    }

    /** A recognisable heirloom that vanishes becomes a recovery quest for its family, per the specification's family/clan hooks. */
    private void heirloomLost(yadi.samuraiai.living.family.events.HeirloomLostEvent e) {
        if (e.familyId() == null) return;
        w.families.family(e.familyId()).ifPresent(f -> familyGiver(f.id()).ifPresent(giver -> {
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("giver", giver.getKey().toString()); vars.put("giverName", w.families.person(giver.getKey()).map(p -> p.name().full()).orElse("?"));
            vars.put("family", f.id().toString()); vars.put("familyName", f.name());
            vars.put("heirloomId", e.itemId().toString()); vars.put("heirloomName", e.name());
            report(ConditionKind.HEIRLOOM_LOST, giver.getValue(), regionOf(giver.getValue()), e.itemId().toString(), 0.65, "reliquia perdida", vars);
        }));
    }

    /** A family's honour crossing hard into the negative becomes a quest to restore it, never a random social flourish. */
    private static final double DISHONOR_THRESHOLD = -20;
    private void familyDishonored(yadi.samuraiai.living.family.events.FamilyReputationChangedEvent e) {
        if (!e.cause().startsWith("honor: ") || e.after() > DISHONOR_THRESHOLD || e.before() <= DISHONOR_THRESHOLD) return;
        w.families.family(e.familyId()).ifPresent(f -> familyGiver(f.id()).ifPresent(giver -> {
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("giver", giver.getKey().toString()); vars.put("giverName", w.families.person(giver.getKey()).map(p -> p.name().full()).orElse("?"));
            vars.put("family", f.id().toString()); vars.put("familyName", f.name());
            report(ConditionKind.FAMILY_DISHONOR, giver.getValue(), regionOf(giver.getValue()), f.id().toString(), 0.6, e.cause(), vars);
        }));
    }

    private void founded(SettlementFoundedEvent e) {
        Settlement s = w.world.settlement(e.settlementId()).orElse(null);
        if (s == null) return;
        LivingWorld.FoundingPlan plan = w.pendingPlan == null ? new LivingWorld.FoundingPlan(false, false) : w.pendingPlan;
        String culture = s.type().defaultCulture();
        if (s.type() != SettlementType.CAMP || plan.plan()) {
            Village v = w.villages.createVillage(s.id(), s.regionId(), s.name(), culture, s.dimension(), s.x(), s.y(), s.z(), s.radius(), plan.plan(), plan.built());
            w.economy.registerSettlement(s.id(), s.regionId(), s.name(), s.type().hasMarket() || v.market() != null, v.mainTemple() != null || s.type() == SettlementType.TEMPLE);
        } else w.economy.registerSettlement(s.id(), s.regionId(), s.name(), false, false);
    }

    private void harvest(HarvestOutlookEvent e) {
        List<UUID> settlements = new ArrayList<>();
        UUID region = null;
        if (e.cell().startsWith("region:")) {
            try { region = UUID.fromString(e.cell().substring(7)); } catch (IllegalArgumentException ignored) { }
            if (region != null) for (Settlement s : w.world.settlementsIn(region)) settlements.add(s.id());
        } else for (Settlement s : w.world.settlements()) if (w.calendar.weather().cell("region:" + s.regionId()).isEmpty()) settlements.add(s.id());
        w.economy.harvest(settlements, e.crop(), e.verdict(), e.yieldFactor());
        if ("GREAT".equals(e.verdict()) && w.living().celebrateGreatHarvests())
            for (UUID s : settlements) if (w.villages.village(s).isPresent())
                w.world.scheduleEvent(WorldEventType.CELEBRATION, "Fiesta por la gran cosecha de " + e.crop(), regionOf(s), s, 0.6, Provenance.of("harvest", e.cell(), "gran cosecha", e.minute()));
    }

    private void worldEvent(WorldEventPhaseEvent e) {
        WorldEventType type = WorldEventType.parse(e.type()).orElse(null);
        if (type == null) return;
        WorldEventRecord record = w.world.events().get(e.eventId()).orElse(null);
        UUID settlement = e.settlementId();
        long now = w.calendar.now();
        String source = "world-event:" + e.eventId();
        switch (e.phase()) {
            case "START" -> {
                switch (type) {
                    case ATTACK -> {
                        if (settlement != null) {
                            w.villages.attackStarted(settlement, e.eventId(), e.title());
                            report(ConditionKind.ATTACK, settlement, e.regionId(), e.eventId().toString(), Math.max(0.5, e.severity()), e.title(), Map.of("eventId", e.eventId().toString()));
                        }
                    }
                    case FIRE -> {
                        if (settlement != null) {
                            w.economy.fire(settlement, e.severity(), e.title());
                            w.villages.village(settlement).ifPresent(v -> {
                                w.villages.startEvent(settlement, VillageEventKind.EMERGENCY, e.title(), now, now + 6 * 60, source, null, null);
                                List<Building> houses = v.buildingsOf(BuildingKind.HOUSE).stream().filter(Building::usable).toList();
                                if (!houses.isEmpty()) w.villages.setBuildingState(houses.get((int) Math.floorMod(e.eventId().getLeastSignificantBits(), (long) houses.size())).id(),
                                        e.severity() > 0.75 ? Building.State.DESTROYED : Building.State.DAMAGED, "incendio");
                                w.villages.remember(settlement, Village.Counter.FIRES, "FIRE", "FIRE", e.title(), "", 0.6, null, Provenance.of("world-event", e.eventId().toString(), "incendio", now));
                            });
                            report(ConditionKind.FIRE, settlement, e.regionId(), e.eventId().toString(), e.severity(), e.title(), Map.of("eventId", e.eventId().toString(), "quantity", String.valueOf(Math.round(20 + 40 * e.severity()))));
                        }
                    }
                    case BANDITS -> {
                        UUID target = settlement != null ? settlement : firstSettlement(e.regionId());
                        if (target != null) report(ConditionKind.BANDITS, target, e.regionId(), e.eventId().toString(), e.severity(), e.title(), vars(e));
                        for (Settlement s : w.world.settlementsIn(e.regionId())) w.villages.reportThreat(s.id(), 20 * e.severity(), "bandidos en la región");
                    }
                    case WAR -> {
                        UUID target = settlement != null ? settlement : firstSettlement(e.regionId());
                        if (target != null) report(ConditionKind.WAR, target, e.regionId(), e.eventId().toString(), e.severity(), e.title(), vars(e));
                        for (Settlement s : w.world.settlementsIn(e.regionId())) w.villages.reportThreat(s.id(), 45 * e.severity(), "guerra");
                    }
                    case MARKET -> { if (settlement != null && w.villages.village(settlement).isPresent()) {
                        w.villages.startEvent(settlement, VillageEventKind.SPECIAL_MARKET, e.title(), now, record == null ? now + 1440 : record.endAt(), source, null, null);
                        w.economy.specialMarket(settlement, record == null ? now + 1440 : record.endAt()); } }
                    case CELEBRATION, FESTIVAL -> { if (settlement != null && w.villages.village(settlement).isPresent())
                        w.villages.startEvent(settlement, VillageEventKind.CELEBRATION, e.title(), now, record == null ? now + 1440 : record.endAt(), source, null, null); }
                    case DUEL -> { if (settlement != null && w.villages.village(settlement).isPresent())
                        w.villages.startEvent(settlement, VillageEventKind.TRAINING, e.title(), now, now + 120, source, null, null); }
                    case STORM, FLOOD, EMERGENCY -> {
                        List<UUID> targets = settlement != null ? List.of(settlement) : w.world.settlementsIn(e.regionId()).stream().map(Settlement::id).toList();
                        for (UUID s : targets) if (w.villages.village(s).isPresent()) w.villages.startEvent(s, VillageEventKind.EMERGENCY, e.title(), now, record == null ? now + 720 : record.endAt(), source, null, null);
                        if (type == WorldEventType.FLOOD) for (UUID s : targets) w.economy.loot(s, 0.1 * e.severity(), "crecida");
                        if (type == WorldEventType.STORM) w.calendar.forceWeather("region:" + e.regionId(), yadi.samuraiai.living.core.WeatherKind.STORM, e.severity(), record == null ? 480 : record.endAt() - now);
                    }
                    default -> { }
                }
            }
            case "CONSEQUENCES" -> {
                boolean success = record == null || record.resolution() != WorldEventRecord.Resolution.FAILED;
                switch (type) {
                    case ATTACK -> {
                        if (settlement != null) {
                            boolean defended = record != null && record.resolution() == WorldEventRecord.Resolution.RESOLVED;
                            w.villages.attackEnded(settlement, e.eventId(), defended, record == null ? "" : record.resolvedBy());
                            if (!defended) { w.economy.loot(settlement, 0.25 * e.severity(), "saqueo tras el ataque"); success = record != null && record.resolution() != WorldEventRecord.Resolution.FAILED && e.severity() < 0.8; }
                        }
                    }
                    default -> { }
                }
                w.quests.worldEventEnded(e.eventId(), success);
                if (settlement != null) w.villages.endEvents(settlement, source);
                for (ConditionKind k : List.of(ConditionKind.BANDITS, ConditionKind.WAR, ConditionKind.FIRE, ConditionKind.ATTACK)) {
                    UUID target = settlement != null ? settlement : firstSettlement(e.regionId());
                    if (target != null) w.quests.conditionResolved(QuestEngine.keyOf(k, target, e.eventId().toString()));
                }
            }
            default -> { }
        }
    }

    private Map<String, String> vars(WorldEventPhaseEvent e) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("eventId", e.eventId().toString());
        w.world.region(e.regionId()).ifPresent(r -> { v.put("x", String.valueOf(r.centerX())); v.put("z", String.valueOf(r.centerZ())); v.put("dimension", r.dimension()); });
        UUID other = null;
        for (Settlement s : w.world.settlements()) if (!s.regionId().equals(e.regionId())) { other = s.id(); break; }
        if (other != null) v.put("destination", other.toString());
        return v;
    }

    private UUID firstSettlement(UUID region) {
        if (region == null) return null;
        List<Settlement> in = w.world.settlementsIn(region);
        if (!in.isEmpty()) return in.get(0).id();
        Region r = w.world.region(region).orElse(null);
        return r == null ? null : w.world.nearestSettlement(r.dimension(), r.centerX(), r.centerZ(), 4000).map(Settlement::id).orElse(null);
    }

    private void scarcity(ScarcityStartedEvent e) {
        UUID s = e.settlementId();
        double severity = Math.max(0.2, Math.min(1, 1 - e.coverDays() / Math.max(0.1, w.economy.settings().scarceDays())));
        String res = e.resource();
        String workshop = null, product = null;
        for (var c : w.villages.citizensOf(s)) {
            if (!c.present()) continue;
            Recipe r = w.economy.recipes().of(c.profession()).orElse(null);
            if (r != null && r.source() == Recipe.Source.CRAFT && r.inputs().containsKey(res)) { workshop = c.profession(); product = r.outputs().keySet().stream().findFirst().orElse("tools"); break; }
        }
        String name = res.equals("food") ? "comida" : w.economy.resources().get(res).map(d -> d.name().toLowerCase(java.util.Locale.ROOT)).orElse(res);
        double daily = w.economy.settlement(s).map(x -> x.dailyDemand().getOrDefault(res, 5.0)).orElse(5.0);
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("resource", res);
        vars.put("resourceName", name);
        vars.put("quantity", String.valueOf(Math.max(5, Math.round(daily * 3))));
        if (workshop != null) { vars.put("product", product); vars.put("productQuantity", "1"); report(ConditionKind.WORKSHOP_STARVED, s, regionOf(s), res, severity, "falta " + name + " en el taller", vars); }
        else report(ConditionKind.SCARCITY, s, regionOf(s), res, severity, "escasez de " + name, vars);
    }

    private void caravanArrived(CaravanArrivedEvent e) {
        w.economy.caravan(e.caravanId()).ifPresent(c -> {
            w.economy.merchant(c.merchant()).ifPresent(m -> { if (w.villages.village(e.destination()).isPresent())
                w.villages.addVisitor(e.destination(), yadi.samuraiai.living.village.visitors.VisitorRecord.Kind.MERCHANT, m.name(), m.npc(), c.origin(), 12, "comercio"); });
            Set<UUID> near = w.world.settlement(e.destination()).map(s -> w.outside().playersNear(s.dimension(), s.x(), s.z(), 64)).orElse(Set.of());
            w.quests.caravanArrived(c.origin(), e.destination(), near);
            w.quests.conditionResolved(QuestEngine.keyOf(ConditionKind.LOST_CARAVAN, c.origin(), e.destination().toString()));
        });
    }

    private void routeDisrupted(TradeRouteDisruptedEvent e) {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("destination", e.destination().toString());
        w.economy.routes().stream().filter(r -> r.id().equals(e.routeId())).findFirst().flatMap(r -> r.legs().stream().findFirst())
                .flatMap(l -> w.world.roads().edge(l.edge())).ifPresent(edge -> w.world.roads().node(edge.a()).ifPresent(a -> w.world.roads().node(edge.b()).ifPresent(b -> {
                    vars.put("x", String.valueOf((a.x() + b.x()) / 2)); vars.put("z", String.valueOf((a.z() + b.z()) / 2)); vars.put("dimension", a.dimension());
                })));
        for (WorldEventRecord ev : w.world.events().open()) if (ev.type().blocksRoads() || ev.type() == WorldEventType.BANDITS) { vars.put("eventId", ev.id().toString()); break; }
        report(ConditionKind.ROUTE_BLOCKED, e.origin(), regionOf(e.origin()), e.destination().toString(), 0.6, e.reason(), vars);
    }

    private void citizenJoined(CitizenJoinedEvent e) {
        Citizen c = w.villages.citizen(e.npcId()).orElse(null);
        if (c == null || e.villageId() == null) return;
        boolean known = w.families.person(c.id()).isPresent();
        w.families.adopt(c.id(), c.name(), e.villageId(), regionOf(e.villageId()), c.home(), c.profession(), e.embodied(), c.npcType());
        // the head of a family moving to another village takes the family with them (its records, not its members' citizenship)
        if (known) w.families.familyOf(c.id()).filter(f -> c.id().equals(f.head()) && f.village() != null && !f.village().equals(e.villageId()))
                .ifPresent(f -> w.families.migrated(f.id(), e.villageId()));
        if ("merchant".equals(c.profession()) && w.economy.settlement(e.villageId()).isPresent()) w.economy.registerMerchant(c.id(), c.name(), e.villageId());
        w.world.population().record(new yadi.samuraiai.living.world.population.PopulationLedger.Change(e.minute(), yadi.samuraiai.living.world.population.PopulationLedger.ChangeKind.ARRIVAL,
                e.villageId(), c.id(), c.name(), c.profession()));
    }

    private void citizenLeft(CitizenLeftEvent e) {
        LifeState state = switch (e.status()) { case "DECEASED" -> LifeState.DECEASED_FUTURE; case "MIGRATED" -> LifeState.MIGRATED; default -> LifeState.MISSING; };
        if (state != LifeState.MIGRATED) w.families.lifeState(e.npcId(), state, e.reason());
        var kind = switch (e.status()) { case "DECEASED" -> yadi.samuraiai.living.world.population.PopulationLedger.ChangeKind.DEATH; case "MIGRATED" -> yadi.samuraiai.living.world.population.PopulationLedger.ChangeKind.MIGRATION;
            default -> yadi.samuraiai.living.world.population.PopulationLedger.ChangeKind.MISSING; };
        w.world.population().record(new yadi.samuraiai.living.world.population.PopulationLedger.Change(e.minute(), kind, e.villageId(), e.npcId(), e.name(), e.reason()));
    }

    private void lifeState(LifeStateChangedEvent e) {
        if (!"MISSING".equals(e.to())) return;
        w.families.familyOf(e.person()).ifPresent(f -> familyGiver(f.id()).ifPresent(giver -> {
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("giver", giver.getKey().toString()); vars.put("giverName", w.families.person(giver.getKey()).map(p -> p.name().full()).orElse("?"));
            vars.put("person", e.person().toString()); vars.put("personName", w.families.person(e.person()).map(p -> p.name().full()).orElse("?"));
            vars.put("family", f.id().toString()); vars.put("familyName", f.name());
            report(ConditionKind.MISSING_PERSON, giver.getValue(), regionOf(giver.getValue()), e.person().toString(), 0.7, e.cause(), vars);
        }));
    }
}
