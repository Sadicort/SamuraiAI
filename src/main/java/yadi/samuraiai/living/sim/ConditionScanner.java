package yadi.samuraiai.living.sim;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.living.calendar.festivals.FestivalDef;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.family.aging.LifeStage;
import yadi.samuraiai.living.family.heritage.Heirloom;
import yadi.samuraiai.living.family.knowledge.Technique;
import yadi.samuraiai.living.family.model.Person;
import yadi.samuraiai.living.family.registry.FamilyRecord;
import yadi.samuraiai.living.quest.conditions.ConditionKind;
import yadi.samuraiai.living.quest.conditions.WorldCondition;
import yadi.samuraiai.living.quest.engine.QuestEngine;
import yadi.samuraiai.living.village.citizens.Citizen;
import yadi.samuraiai.living.village.population.VillageCensus;
import yadi.samuraiai.living.village.runtime.Village;
import yadi.samuraiai.living.world.regions.Region;

/**
 * The daily analyzers of the quest pipeline for conditions that are states rather than events: too few guards for a
 * threatened village, a festival a few days away, a full moon coming for a temple's rite, a master who knows something nobody
 * has learned, a lost heirloom, a dishonoured family, an NPC grateful to a player, an unexplored region next door. Each
 * becomes a {@link WorldCondition} with a stable key, so the Quest Engine never makes two quests of the same problem.
 */
public final class ConditionScanner {
    private final LivingWorld w;
    private final Map<String, java.util.Set<Long>> disputeDays = new java.util.HashMap<>();
    private long scans, reported;

    /** Days a dispute between two people must be seen before their families become rivals. */
    public static final int FEUD_DAYS = 3;

    ConditionScanner(LivingWorld w) { this.w = w; }

    public long scans() { return scans; }
    public long reported() { return reported; }

    private void report(ConditionKind kind, UUID village, UUID region, String subject, double severity, String cause, Map<String, String> vars) {
        reported++;
        w.metrics().conditions.incrementAndGet();
        w.quests.report(new WorldCondition(kind, QuestEngine.keyOf(kind, village, subject), village, region, subject, severity,
                Provenance.of("scan", subject, cause, w.calendar.now()), vars));
    }

    public void scan(long day) {
        scans++;
        LivingSettings s = w.living();
        for (Village v : w.villages.villages()) {
            VillageCensus census = w.villages.census(v.id());
            UUID region = v.region();
            // too few guards when the village is threatened
            if (census.residents() >= 6 && census.guards() < Math.ceil(census.residents() * s.guardRatio()) && v.security().state() != yadi.samuraiai.living.village.security.SecurityState.PEACE)
                report(ConditionKind.GUARD_SHORTAGE, v.id(), region, "guards", 0.5, "pocos guardias", Map.of());
            // a festival coming
            w.calendar.festivals().next(day).ifPresent(next -> {
                FestivalDef f = next.getKey();
                if (next.getValue() <= s.festivalWarningDays() && f.celebratedBy(v.culture())) {
                    String res = f.demand().keySet().stream().findFirst().orElse("rice");
                    Map<String, String> vars = new LinkedHashMap<>();
                    vars.put("festival", f.name()); vars.put("resource", res); vars.put("resourceName", w.economy.resources().get(res).map(d -> d.name().toLowerCase(java.util.Locale.ROOT)).orElse(res));
                    vars.put("quantity", String.valueOf(Math.max(10, census.residents() * 2)));
                    report(ConditionKind.FESTIVAL_SOON, v.id(), region, f.id() + ":" + w.calendar.today().year(), 0.4, f.name() + " se acerca", vars);
                }
            });
            // a full moon coming for the temple
            if (v.mainTemple() != null && w.calendar.moon().daysToFull(day) <= s.ritualWarningDays())
                report(ConditionKind.TEMPLE_RITUAL, v.id(), region, "moon:" + Math.floorDiv(day, (long) w.calendar.moon().cycleDays()), 0.4, "luna llena",
                        Map.of("quantity", "6"));
            // gratitude remembered by the cognitive layer
            for (var g : w.outside().grateful(v.communityKey()).entrySet()) {
                Map<String, String> vars = new LinkedHashMap<>();
                w.villages.citizen(g.getKey()).ifPresent(c -> { vars.put("giver", c.id().toString()); vars.put("giverName", c.name()); vars.put("giverProfession", c.profession()); });
                vars.put("player", g.getValue().toString());
                if (!vars.isEmpty()) report(ConditionKind.GRATITUDE, v.id(), region, g.getKey() + ">" + g.getValue(), 0.6, "gratitud", vars);
            }
            // neighbours at odds (from the cognitive relationships); a lasting one between two families becomes a feud
            for (UUID[] pair : w.outside().rivals(v.communityKey())) {
                Citizen a = w.villages.citizen(pair[0]).filter(Citizen::present).orElse(null), b = w.villages.citizen(pair[1]).filter(Citizen::present).orElse(null);
                if (a == null || b == null || !v.id().equals(a.village()) || !v.id().equals(b.village())) continue;
                Map<String, String> vars = new LinkedHashMap<>();
                vars.put("giver", a.id().toString()); vars.put("giverName", a.name()); vars.put("giverProfession", a.profession());
                vars.put("other", b.id().toString()); vars.put("otherName", b.name());
                String subject = pair[0].compareTo(pair[1]) < 0 ? pair[0] + ":" + pair[1] : pair[1] + ":" + pair[0];
                report(ConditionKind.DISPUTE, v.id(), region, "dispute:" + subject, 0.4, a.name() + " y " + b.name() + " no se hablan", vars);
                java.util.Set<Long> days = disputeDays.computeIfAbsent(subject, k -> new java.util.HashSet<>());
                days.add(day);
                var fa = w.families.familyOf(a.id()).orElse(null);
                var fb = w.families.familyOf(b.id()).orElse(null);
                if (fa != null && fb != null && !fa.id().equals(fb.id()) && days.size() >= FEUD_DAYS) {
                    var rel = fa.relations().get(fb.id());
                    if (rel != yadi.samuraiai.living.family.registry.FamilyRecord.Relation.RIVAL && rel != yadi.samuraiai.living.family.registry.FamilyRecord.Relation.HOSTILE)
                        w.families.relation(fa.id(), fb.id(), yadi.samuraiai.living.family.registry.FamilyRecord.Relation.RIVAL);
                }
                break;   // one dispute per village per day is enough to talk about
            }
            // an unexplored region next door (weekly)
            if (s.spawnExplorationQuests() && Math.floorMod(day + v.id().hashCode(), 7L) == 0) {
                Region home = w.world.region(region).orElse(null);
                if (home != null) for (UUID n : home.relations().keySet()) {
                    Region r = w.world.region(n).orElse(null);
                    if (r == null || !r.settlements().isEmpty() || w.calendar.now() - r.lastPlayerSeen() < 30L * w.calendar.minutesPerDay()) continue;
                    report(ConditionKind.EXPLORATION, v.id(), r.id(), "explore:" + r.id(), 0.3, "nadie sabe qué hay en " + r.name(), Map.of("regionName", r.name()));
                    break;
                }
            }
        }
        scanFamilies();
    }

    private void scanFamilies() {
        for (FamilyRecord f : w.families.families()) {
            if (!f.active()) continue;
            UUID village = f.village();
            if (village == null || w.villages.village(village).isEmpty()) continue;
            UUID region = w.villages.village(village).get().region();
            Person head = f.head() == null ? null : w.families.person(f.head()).orElse(null);
            // an old grudge between this family and a rival one in the same village (reported once per pair, by the lower id)
            for (var rel : f.relations().entrySet()) {
                if (rel.getValue() != FamilyRecord.Relation.RIVAL && rel.getValue() != FamilyRecord.Relation.HOSTILE) continue;
                if (f.id().compareTo(rel.getKey()) > 0 || head == null) continue;
                FamilyRecord other = w.families.family(rel.getKey()).orElse(null);
                Person otherHead = other == null || other.head() == null ? null : w.families.person(other.head()).orElse(null);
                if (other == null || !other.active() || otherHead == null || !village.equals(other.village())) continue;
                Map<String, String> vars = new LinkedHashMap<>();
                vars.put("giver", head.id().toString()); vars.put("giverName", head.name().full());
                vars.put("other", otherHead.id().toString()); vars.put("otherName", otherHead.name().full());
                vars.put("family", f.id().toString()); vars.put("familyName", f.name()); vars.put("quantity", "10");
                report(ConditionKind.GRUDGE, village, region, "grudge:" + f.id() + ":" + other.id(), rel.getValue() == FamilyRecord.Relation.HOSTILE ? 0.7 : 0.5,
                        "rencilla entre los " + f.name() + " y los " + other.name(), vars);
            }
            if (f.honor().value() < -5 && head != null)
                report(ConditionKind.FAMILY_DISHONOR, village, region, "dishonor:" + f.id(), 0.5, "la familia está deshonrada",
                        Map.of("giver", head.id().toString(), "giverName", head.name().full(), "family", f.id().toString(), "familyName", f.name(), "quantity", "10"));
            for (UUID item : f.heirlooms()) {
                Heirloom h = w.families.heirloom(item).orElse(null);
                if (h == null || !h.lost() || head == null) continue;
                Map<String, String> vars = new LinkedHashMap<>();
                vars.put("giver", head.id().toString()); vars.put("giverName", head.name().full()); vars.put("family", f.id().toString()); vars.put("familyName", f.name());
                vars.put("heirloomName", h.name());
                report(ConditionKind.HEIRLOOM_LOST, village, region, "heirloom:" + item, 0.6, h.name() + " perdida", vars);
            }
        }
        // a master of a trade keeps a secret of it: knowledge that must be taught to survive
        for (Citizen c : w.villages.citizens()) {
            if (!c.present() || c.profession().isEmpty() || !"maestro".equals(yadi.samuraiai.living.core.Skill.rank(c.professionHours()))) continue;
            if (w.families.person(c.id()).isEmpty()) continue;
            String key = "trade:" + c.profession() + ":" + c.id();
            if (w.families.techniqueOf(key).isPresent()) continue;
            String trade = w.world.professions().get(c.profession()).map(p -> p.name().toLowerCase(java.util.Locale.ROOT)).orElse(c.profession());
            w.families.technique(key, "el secreto de " + trade + " de " + c.name(), c.id(), null, 0.5);
        }
        // masters whose knowledge nobody is learning
        for (Technique t : w.families.techniques()) {
            if (t.lost() || t.holders().size() != 1) continue;
            UUID master = t.holders().iterator().next();
            if (w.families.stage(master).ordinal() < LifeStage.MATURE.ordinal() || !w.families.disciplesOf(master).stream().filter(m -> m.open()).toList().isEmpty()) continue;
            Citizen mc = w.villages.citizen(master).filter(Citizen::present).orElse(null);
            if (mc == null) continue;
            List<Citizen> young = w.villages.citizensOf(mc.village()).stream().filter(c -> c.present() && !c.id().equals(master) && w.families.person(c.id()).isPresent()
                    && w.families.stage(c.id()) == LifeStage.YOUNG_ADULT).toList();
            if (young.isEmpty()) continue;
            Citizen d = young.get(0);
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("giver", master.toString()); vars.put("giverName", mc.name()); vars.put("giverProfession", mc.profession());
            vars.put("master", mc.name()); vars.put("masterId", master.toString()); vars.put("disciple", d.name()); vars.put("discipleId", d.id().toString());
            report(ConditionKind.MENTOR_WANTED, mc.village(), w.villages.village(mc.village()).map(Village::region).orElse(null), "mentor:" + master, 0.5, t.name() + " sin heredero", vars);
        }
    }
}
