package yadi.samuraiai.living.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import yadi.samuraiai.living.calendar.agriculture.CropStage;
import yadi.samuraiai.living.calendar.engine.CalendarEngine;
import yadi.samuraiai.living.calendar.festivals.FestivalDef;
import yadi.samuraiai.living.calendar.timeline.TimelineCategory;
import yadi.samuraiai.living.core.CalendarDate;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.core.Season;
import yadi.samuraiai.living.core.WeatherKind;
import yadi.samuraiai.living.economy.engine.EconomyPorts;
import yadi.samuraiai.living.economy.inventory.ResourceLot;
import yadi.samuraiai.living.economy.ledger.MovementRecord;
import yadi.samuraiai.living.economy.resources.ResourceDef;
import yadi.samuraiai.living.economy.wealth.WealthAccount;
import yadi.samuraiai.living.family.engine.FamilyEngine;
import yadi.samuraiai.living.family.integration.FamilyPorts;
import yadi.samuraiai.living.family.mentorship.Mentorship;
import yadi.samuraiai.living.family.registry.FamilyRecord;
import yadi.samuraiai.living.quest.api.QuestPorts;
import yadi.samuraiai.living.village.buildings.Building;
import yadi.samuraiai.living.village.buildings.BuildingKind;
import yadi.samuraiai.living.village.buildings.OwnerRef;
import yadi.samuraiai.living.village.citizens.Citizen;
import yadi.samuraiai.living.village.engine.VillagePorts;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.world.engine.WorldPorts;
import yadi.samuraiai.living.world.events.WorldEventType;
import yadi.samuraiai.living.world.regions.Region;
import yadi.samuraiai.living.world.roads.RoadEdge;
import yadi.samuraiai.living.world.roads.RoadNetwork;
import yadi.samuraiai.living.world.settlements.Settlement;

/**
 * The ports of every engine, implemented over the others. This is where "the Village asks the Economy how many days of food
 * it has" or "the Economy asks the World for iron from a region's deposit" actually happens; each engine only knows its own
 * port interfaces. Nothing here stores state: every answer is read from the engine that owns it.
 */
final class LivingBridges {
    private final LivingWorld w;

    LivingBridges(LivingWorld w) { this.w = w; }

    private CalendarEngine cal() { return w.calendar; }

    /** Weather cell of a region scope: the region's own when tracked, otherwise the world cell. */
    String cell(String scope) { return cal().weather().cell(scope).isPresent() ? scope : CalendarEngine.WORLD_CELL; }

    static String seasonName(Season s) { return CalendarDate.seasonName(s); }

    void wire() {
        wireWorld();
        wireVillages();
        wireEconomy();
        wireQuests();
        wireFamilies();
    }

    private void chronicle(long minute, String category, String title, String detail, Set<String> scopes, double significance, Provenance source) {
        cal().record(minute, TimelineCategory.parse(category).orElse(TimelineCategory.OTHER), title, detail, scopes, significance, source);
    }

    // ------------------------------------------------------------------ world

    private void wireWorld() {
        w.world.useEnvironment(new WorldPorts.Environment() {
            @Override public Season season() { return cal().season(); }
            @Override public double seasonAnimals() { return cal().seasonProfile().animals(); }
            @Override public WeatherKind weather(String regionScope) { return cal().weatherAt(cell(regionScope)); }
            @Override public double foodSurplus(UUID region) {
                double sum = 0; int n = 0;
                for (Settlement s : w.world.settlementsIn(region)) {
                    var se = w.economy.settlement(s.id());
                    if (se.isEmpty()) continue;
                    double cover = se.get().foodCoverDays();
                    sum += Double.isFinite(cover) ? Math.min(3, cover / Math.max(1, w.economy.settings().coverTargetDays())) : 1; n++;
                }
                return n == 0 ? 1.0D : Math.max(0.2D, sum / n);
            }
        });
        w.world.useChronicle(new WorldPorts.Chronicle() {
            @Override public void record(long minute, String category, String title, String detail, Set<String> scopes, double significance, Provenance source) { chronicle(minute, category, title, detail, scopes, significance, source); }
            @Override public void anniversary(String kind, String subject, String title, long minute) { cal().anniversary(kind, subject, title, minute); }
        });
    }

    // ------------------------------------------------------------------ villages

    private void wireVillages() {
        w.villages.useProfessions(new VillagePorts.Professions() {
            @Override public Optional<VillagePorts.ProfessionInfo> get(String id) {
                return w.world.professions().get(id).map(p -> new VillagePorts.ProfessionInfo(p.id(), p.name(), p.workRoutine(), p.bias(), p.locations(), p.future()));
            }
            @Override public Optional<String> forNpcType(String npcType) { return w.world.professions().forNpcType(npcType).map(p -> p.id()); }
            @Override public List<String> ids() { return w.world.professions().all().stream().map(p -> p.id()).toList(); }
        });
        w.villages.useCalendar(new VillagePorts.Calendar() {
            @Override public CalendarDate today() { return cal().today(); }
            @Override public int sunriseShift() { return cal().sun().sunriseShift(cal().today().dayOfYear()); }
            @Override public double seasonSocial() { return cal().seasonProfile().social(); }
            @Override public WeatherKind weather(String regionScope) { return cal().weatherAt(cell(regionScope)); }
            @Override public double temperature(String regionScope) { return cal().temperature(cell(regionScope), Double.NaN); }
            @Override public List<VillagePorts.FestivalInfo> festivals(String culture) {
                List<VillagePorts.FestivalInfo> out = new ArrayList<>();
                for (FestivalDef f : cal().activeFestivals()) if (f.celebratedBy(culture)) out.add(new VillagePorts.FestivalInfo(f.id(), f.name(), f.routineBias(), f.social()));
                return out;
            }
            @Override public boolean holyDay() { return !cal().activeFestivals().isEmpty() || !cal().activeHolidays().isEmpty(); }
        });
        w.villages.useCommunity(new VillagePorts.Community() {
            @Override public String ensure(String key, String name, String culture, String dimension, double x, double y, double z, double radius) { return w.outside().ensureCommunity(key, name, culture, dimension, x, y, z, radius); }
            @Override public void join(UUID npc, String key, boolean leader) { w.outside().joinCommunity(npc, key, leader); }
            @Override public void leave(UUID npc, String key) { w.outside().leaveCommunity(npc, key); }
            @Override public void remember(String key, String historyType, String title, double significance, long minute, UUID actor) { w.outside().rememberInCommunity(key, historyType, title, significance, minute, actor); }
        });
        w.villages.useChronicle(new VillagePorts.Chronicle() {
            @Override public void record(long minute, String category, String title, String detail, Set<String> scopes, double significance, Provenance source) { chronicle(minute, category, title, detail, scopes, significance, source); }
        });
        w.villages.useEconomy(new VillagePorts.Economy() {
            @Override public double foodCoverDays(UUID village) { return w.economy.settlement(village).map(s -> Double.isFinite(s.foodCoverDays()) ? s.foodCoverDays() : 99.0D).orElse(10.0D); }
            @Override public double prosperity(UUID village) { return w.economy.settlement(village).map(s -> s.prosperity()).orElse(0.5D); }
            @Override public Optional<String> mostNeededProfession(UUID village) { return w.economy.mostNeededProfession(village); }
        });
        w.villages.useFamily(npc -> w.families.person(npc).isPresent() ? w.families.stage(npc).name() : "ADULT");
    }

    // ------------------------------------------------------------------ economy

    int countBuildings(UUID settlement, String kind) {
        Optional<Village> v = w.villages.village(settlement);
        Optional<BuildingKind> k = BuildingKind.parse(kind);
        if (v.isEmpty() || k.isEmpty()) return 0;
        int n = 0;
        for (Building b : v.get().buildingsOf(k.get())) if (b.usable()) n++;
        return n;
    }

    private void wireEconomy() {
        w.economy.useWorld(new EconomyPorts.World() {
            @Override public String regionScope(UUID s) { return w.world.settlement(s).map(x -> cell("region:" + x.regionId())).orElse(CalendarEngine.WORLD_CELL); }
            @Override public double extract(UUID region, String resource, double amount) { return region == null ? 0 : w.world.extract(region, resource, amount); }
            @Override public double harvestAnimals(UUID region, String resource, double amount, double share) { return region == null ? 0 : w.world.harvestAnimals(region, resource, amount, share); }
            @Override public Optional<EconomyPorts.RouteInfo> route(UUID from, UUID to) {
                return w.world.route(from, to, RoadNetwork.Preferences.CARAVAN).map(plan -> {
                    List<EconomyPorts.Leg> legs = new ArrayList<>();
                    for (UUID e : plan.edges()) w.world.roads().edge(e).ifPresent(edge -> legs.add(new EconomyPorts.Leg(e, edge.length(), edge.danger())));
                    return new EconomyPorts.RouteInfo(legs, plan.bridges(), w.world.roads().version());
                });
            }
            @Override public long roadVersion() { return w.world.roads().version(); }
            @Override public boolean blocked(UUID edge) { return w.world.roads().edge(edge).map(RoadEdge::blocked).orElse(true); }
            @Override public double edgeDanger(UUID edge) { return w.world.roads().edge(edge).map(RoadEdge::danger).orElse(1.0D); }
            @Override public double regionDanger(UUID region) { return region == null ? 0 : w.world.region(region).map(Region::danger).orElse(0.0D); }
            @Override public double distance(UUID a, UUID b) {
                var sa = w.world.settlement(a); var sb = w.world.settlement(b);
                return sa.isPresent() && sb.isPresent() && sa.get().dimension().equals(sb.get().dimension()) ? sa.get().distance(sb.get().x(), sb.get().z()) : Double.MAX_VALUE;
            }
            @Override public List<UUID> settlements() { return w.world.settlements().stream().map(Settlement::id).toList(); }
            @Override public void travelled(UUID edge, long minute) { w.world.roads().edge(edge).ifPresent(e -> e.travelled(minute)); }
        });
        w.economy.useVillages(new EconomyPorts.Villages() {
            @Override public List<EconomyPorts.Worker> workers(UUID settlement) {
                List<EconomyPorts.Worker> out = new ArrayList<>();
                for (Citizen c : w.villages.citizensOf(settlement)) if (c.present() && !c.profession().isEmpty()) out.add(new EconomyPorts.Worker(c.id(), c.name(), c.profession(), c.professionHours()));
                return out;
            }
            @Override public int population(UUID settlement) { return (int) w.villages.citizensOf(settlement).stream().filter(Citizen::present).count(); }
            @Override public int children(UUID settlement) { return w.villages.census(settlement).children(); }
            @Override public int visitors(UUID settlement) { return w.villages.visitorsPresent(settlement); }
            @Override public boolean marketOpen(UUID settlement) { return w.villages.village(settlement).map(Village::marketOpen).orElse(false); }
            @Override public int footfall(UUID settlement) { return w.villages.village(settlement).map(Village::marketFootfall).orElse(0); }
            @Override public int buildings(UUID settlement, String kind) { return countBuildings(settlement, kind); }
            @Override public String culture(UUID settlement) { return w.villages.village(settlement).map(Village::culture).orElse("village"); }
        });
        w.economy.useCalendar(new EconomyPorts.Calendar() {
            @Override public double foodFactor() { return cal().seasonProfile().food(); }
            @Override public double fuelFactor() { return cal().seasonProfile().fuel(); }
            @Override public double tradeFactor() { return cal().seasonProfile().trade(); }
            @Override public double travelFactor() { return cal().seasonProfile().travel(); }
            @Override public double agricultureFactor(String resource, String scope) { return cal().agriculture().productionFactor(resource, cell(scope), cal().today().month()); }
            @Override public boolean harvestSeason(String resource) { return cal().agriculture().stage(resource, cal().today().month()) == CropStage.HARVEST; }
            @Override public boolean offSeason(String resource) { CropStage s = cal().agriculture().stage(resource, cal().today().month()); return s != CropStage.NONE && s != CropStage.HARVEST; }
            @Override public WeatherKind weather(String scope) { return cal().weatherAt(cell(scope)); }
            @Override public Map<String, Double> festivalDemand(String culture) {
                Map<String, Double> out = new java.util.LinkedHashMap<>();
                for (FestivalDef f : cal().activeFestivals()) if (f.celebratedBy(culture)) f.demand().forEach((k, v) -> out.merge(k, v, (a, b) -> a * b));
                return out;
            }
        });
        w.economy.useRelations((a, b) -> w.outside().trust(a, b) / 100.0D);
        w.economy.useChronicle(this::chronicle);
    }

    // ------------------------------------------------------------------ quests

    private void wireQuests() {
        w.quests.useWorld(new QuestPorts.World() {
            @Override public String settlementName(UUID s) { return w.world.settlement(s).map(Settlement::name).orElse("?"); }
            @Override public Optional<QuestPorts.Place> settlementPlace(UUID s) { return w.world.settlement(s).map(x -> new QuestPorts.Place(x.dimension(), x.x(), x.y(), x.z())); }
            @Override public String regionName(UUID r) { return w.world.region(r).map(Region::name).orElse("?"); }
            @Override public Optional<QuestPorts.Place> regionCenter(UUID r) { return w.world.region(r).map(x -> new QuestPorts.Place(x.dimension(), x.centerX(), x.altitude(), x.centerZ())); }
            @Override public Optional<UUID> neighbour(UUID s) {
                Optional<Settlement> from = w.world.settlement(s);
                if (from.isEmpty()) return Optional.empty();
                Settlement best = null;
                for (Settlement o : w.world.settlements()) if (!o.id().equals(s) && o.active() && o.dimension().equals(from.get().dimension()) && (best == null || o.distance(from.get().x(), from.get().z()) < best.distance(from.get().x(), from.get().z()))) best = o;
                return Optional.ofNullable(best).map(Settlement::id);
            }
            @Override public boolean resolveEvent(UUID event, boolean success, String by, String outcome) { return w.world.resolveEvent(event, success, by, outcome); }
            @Override public Optional<UUID> spawnEvent(String type, UUID region, UUID settlement, double severity, String cause) {
                return WorldEventType.parse(type).flatMap(t -> w.world.scheduleEvent(t, t.name() + " (" + cause + ")", region, settlement, severity, Provenance.of("quest", "", cause, cal().now()))).map(e -> e.id());
            }
            @Override public boolean buildRoad(UUID from, UUID to) { return w.world.buildRoad(from, to, RoadEdge.Kind.TRAIL).isPresent(); }
            @Override public String seasonName() { return cal().season().name(); }
            @Override public String weatherName(UUID region) { return cal().weatherAt(cell("region:" + region)).label(); }
            @Override public String moonName() { return cal().moonPhase().label(); }
        });
        w.quests.useVillages(new QuestPorts.Villages() {
            @Override public Optional<QuestPorts.Giver> giver(UUID settlement, List<String> professions) {
                List<Citizen> citizens = w.villages.citizensOf(settlement).stream().filter(Citizen::present).toList();
                for (String p : professions) for (Citizen c : citizens) if (p.equals(c.profession()) && c.embodied()) return Optional.of(new QuestPorts.Giver(c.id(), c.name(), c.profession()));
                for (String p : professions) for (Citizen c : citizens) if (p.equals(c.profession())) return Optional.of(new QuestPorts.Giver(c.id(), c.name(), c.profession()));
                for (Citizen c : citizens) if (c.embodied()) return Optional.of(new QuestPorts.Giver(c.id(), c.name(), c.profession()));
                return citizens.stream().findFirst().map(c -> new QuestPorts.Giver(c.id(), c.name(), c.profession()));
            }
            @Override public Optional<QuestPorts.Place> building(UUID settlement, String kind) {
                return w.villages.village(settlement).flatMap(v -> BuildingKind.parse(kind).flatMap(k -> v.buildingsOf(k).stream().findFirst()))
                        .map(b -> new QuestPorts.Place(b.dimension(), b.x(), b.y(), b.z()));
            }
            @Override public void renown(UUID settlement, double delta, String cause) { w.villages.village(settlement).ifPresent(v -> v.renown(delta, cause)); }
            @Override public void unrest(UUID settlement, double delta) { w.villages.village(settlement).ifPresent(v -> { v.unrest(v.unrest() + delta); v.markDirty(); }); }
            @Override public boolean buildHouse(UUID settlement) {
                Optional<Village> v = w.villages.village(settlement);
                if (v.isEmpty()) return false;
                int n = v.get().buildingsOf(BuildingKind.HOUSE).size();
                double angle = n * 0.9D, r = v.get().radius() * 0.45D;
                w.villages.registerBuilding(settlement, BuildingKind.HOUSE, "Casa " + (n + 1), v.get().dimension(), v.get().x() + Math.cos(angle) * r, v.get().y(), v.get().z() + Math.sin(angle) * r, 4,
                        Building.State.BUILT, "");
                return true;
            }
            @Override public String culture(UUID settlement) { return w.villages.village(settlement).map(Village::culture).orElse("village"); }
        });
        w.quests.useEconomy(new QuestPorts.Economy() {
            @Override public double reward(UUID settlement, UUID player, String name, double coins, String reason) { return w.economy.reward(settlement, player, name, coins, reason); }
            @Override public double giveItems(UUID settlement, UUID player, String resource, double quantity, String reason) {
                double taken = 0;
                for (ResourceLot l : w.economy.withdraw(settlement, resource, quantity, reason)) taken += l.quantity();
                return taken <= 0 ? 0 : w.outside().giveItems(player, resource, taken);
            }
            @Override public double price(UUID settlement, String resource) { return w.economy.price(settlement, resource); }
            @Override public boolean isFood(String resource) { return w.economy.resources().get(resource).map(d -> d.category() == ResourceDef.Category.FOOD).orElse(false); }
            @Override public void loot(UUID settlement, double fraction, String cause) { w.economy.loot(settlement, fraction, cause); }
        });
        w.quests.useSocial(new QuestPorts.Social() {
            @Override public double trust(UUID npc, UUID player) { return w.outside().trust(npc, player); }
            @Override public String mood(UUID npc) { return w.outside().mood(npc); }
            @Override public double standing(UUID player, UUID settlement, String context) { return w.villages.village(settlement).map(v -> w.outside().standing(player, v.communityKey(), context)).orElse(0.0D); }
            @Override public void adjustStanding(UUID player, String name, UUID settlement, String context, double amount) {
                w.villages.village(settlement).ifPresent(v -> w.outside().adjustStanding(player, name, v.communityKey(), context, amount));
            }
            @Override public void experience(UUID npc, UUID player, String name, String kind, String note) { w.outside().experience(npc, player, name, kind, note); }
            @Override public void witnesses(UUID settlement, UUID player, String name, String kind, String note) {
                int n = 0;
                for (Citizen c : w.villages.citizensOf(settlement)) { if (n >= 6) break; if (c.present() && c.embodied()) { w.outside().experience(c.id(), player, name, kind, note); n++; } }
            }
        });
        w.quests.useFamilies(new QuestPorts.Families() {
            @Override public void honor(String subject, double delta, String cause) { familyOf(subject).ifPresent(f -> w.families.honor(f.id(), delta, cause, personOf(subject))); }
            @Override public void memory(String subject, String text) {
                familyOf(subject).ifPresent(f -> w.families.remember(f, yadi.samuraiai.living.family.family_memory.FamilyMemoryEntry.Kind.OTHER, text,
                        personOf(subject) == null ? List.of() : List.of(personOf(subject)), 0.55));
            }
            @Override public void mentorship(UUID master, UUID disciple, double progress) {
                Mentorship m = w.families.mentor(master, disciple, Mentorship.Type.CUSTOM, null, false);
                m.advance(progress, cal().now());
            }
        });
        w.quests.useChronicle(this::chronicle);
        w.quests.useNotifier((player, text) -> w.outside().tell(player, text));
    }

    private Optional<FamilyRecord> familyOf(String subject) {
        if (subject.startsWith("family:")) { try { return w.families.family(UUID.fromString(subject.substring(7))); } catch (IllegalArgumentException e) { return Optional.empty(); } }
        UUID p = personOf(subject);
        return p == null ? Optional.empty() : w.families.familyOf(p);
    }

    private static UUID personOf(String subject) {
        if (!subject.startsWith("npc:")) return null;
        try { return UUID.fromString(subject.substring(4)); } catch (IllegalArgumentException e) { return null; }
    }

    // ------------------------------------------------------------------ families

    private void wireFamilies() {
        FamilyEngine f = w.families;
        f.useCalendar(() -> (long) cal().spec().daysPerYear() * cal().minutesPerDay());
        f.useVillages(new FamilyPorts.Villages() {
            @Override public Optional<UUID> villageOf(UUID npc) { return w.villages.citizen(npc).filter(Citizen::present).map(Citizen::village); }
            @Override public int beds(UUID building) { return w.villages.building(building).map(Building::capacity).orElse(0); }
            @Override public boolean damaged(UUID building) { return w.villages.building(building).map(b -> b.state() == Building.State.DAMAGED).orElse(false); }
            @Override public boolean destroyed(UUID building) { return w.villages.building(building).map(b -> b.state() == Building.State.DESTROYED).orElse(false); }
            @Override public Optional<String> profession(UUID npc) { return w.villages.citizen(npc).map(Citizen::profession); }
            @Override public void assignProfession(UUID npc, String profession, String reason) { w.villages.assignProfession(npc, profession, reason); }
            @Override public List<String> neededProfessions(UUID village) { return w.economy.mostNeededProfession(village).map(List::of).orElse(List.of()); }
            @Override public void transferBuilding(UUID building, UUID owner, String label) { w.villages.building(building).ifPresent(b -> b.owner(OwnerRef.citizen(owner, label))); }
            @Override public List<UUID> buildingsOwnedBy(UUID npc) {
                List<UUID> out = new ArrayList<>();
                for (Village v : w.villages.villages()) for (Building b : v.buildings().values()) if (npc.equals(b.owner().id()) && b.owner().kind() == OwnerRef.Kind.CITIZEN) out.add(b.id());
                return out;
            }
        });
        f.useEconomy(new FamilyPorts.Economy() {
            @Override public double coins(UUID owner) { return w.economy.wealth().coins(owner); }
            @Override public double transfer(UUID from, UUID to, String toName, double amount, String reason) {
                Optional<WealthAccount> source = w.economy.wealth().of(from);
                if (source.isEmpty()) return 0;
                WealthAccount target = w.economy.wealth().account(WealthAccount.OwnerKind.NPC, to, toName, cal().now());
                return w.economy.wealth().transferUpTo(source.get(), target, amount, MovementRecord.Kind.INHERITED, reason, cal().now(), null);
            }
        });
        f.useSocial(new FamilyPorts.Social() {
            @Override public double standing(UUID npc, UUID village) { return w.villages.village(village).map(v -> w.outside().standing(npc, v.communityKey(), "village")).orElse(0.0D); }
            @Override public double trust(UUID a, UUID b) { return w.outside().trust(a, b); }
            @Override public double respect(UUID a, UUID b) { return w.outside().respect(a, b); }
            @Override public double teachingQuality(UUID master, UUID disciple) { return w.outside().teachingQuality(master, disciple); }
            @Override public void learn(UUID npc, String key, String text, UUID teacher) { w.outside().learn(npc, key, text, teacher); }
            @Override public double affinity(UUID npc, String profession) { return w.outside().affinity(npc, profession); }
            @Override public void experience(UUID npc, UUID other, String otherName, String kind, String note) { w.outside().experience(npc, other, otherName, kind, note); }
        });
        f.useChronicle(new FamilyPorts.Chronicle() {
            @Override public void record(long minute, String category, String title, String detail, Set<String> scopes, double significance, Provenance source) { chronicle(minute, category, title, detail, scopes, significance, source); }
            @Override public void anniversary(String kind, String subject, String title, long minute) { cal().anniversary(kind, subject, title, minute); }
            @Override public int mentions(String scope) { return cal().timeline().of(scope, 0).size(); }
        });
    }
}
