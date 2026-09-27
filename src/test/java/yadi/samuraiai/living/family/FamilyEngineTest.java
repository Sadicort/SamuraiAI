package yadi.samuraiai.living.family;

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
import yadi.samuraiai.living.core.persistence.LivingStorage;
import yadi.samuraiai.living.family.aging.LifeStage;
import yadi.samuraiai.living.family.debug.FamilyInspector;
import yadi.samuraiai.living.family.engine.FamilyEngine;
import yadi.samuraiai.living.family.engine.FamilySettings;
import yadi.samuraiai.living.family.events.FamilyExtinctEvent;
import yadi.samuraiai.living.family.events.FamilyHeadChangedEvent;
import yadi.samuraiai.living.family.events.GenerationAdvancedEvent;
import yadi.samuraiai.living.family.events.TechniqueLostEvent;
import yadi.samuraiai.living.family.heritage.Heirloom;
import yadi.samuraiai.living.family.integration.FamilyPorts;
import yadi.samuraiai.living.family.knowledge.Technique;
import yadi.samuraiai.living.family.lifecycle.LifeState;
import yadi.samuraiai.living.family.lineage.Lineage;
import yadi.samuraiai.living.family.mentorship.Mentorship;
import yadi.samuraiai.living.family.model.Person;
import yadi.samuraiai.living.family.persistence.FamilyStorage;
import yadi.samuraiai.living.family.registry.FamilyRecord;

class FamilyEngineTest {
    private final List<NpcEvent> events = new ArrayList<>();
    private final CalendarEngine calendar = new CalendarEngine(CalendarSettings::defaults, e -> { }, 4L);
    private final UUID village = UUID.randomUUID(), region = UUID.randomUUID(), houseA = UUID.randomUUID(), houseB = UUID.randomUUID();
    private final Map<UUID, Double> coins = new HashMap<>();
    private final Map<UUID, UUID> owners = new HashMap<>();
    private final Map<UUID, String> professions = new HashMap<>();
    private final List<String> learned = new ArrayList<>();
    private final List<String> experienced = new ArrayList<>();

    private FamilyEngine engine() {
        FamilyEngine f = new FamilyEngine(FamilySettings::defaults, events::add, calendar, 21L);
        f.useCalendar(() -> (long) calendar.spec().daysPerYear() * calendar.minutesPerDay());
        f.useVillages(new FamilyPorts.Villages() {
            @Override public Optional<UUID> villageOf(UUID npc) { return Optional.of(village); }
            @Override public int beds(UUID b) { return 4; }
            @Override public boolean damaged(UUID b) { return false; }
            @Override public boolean destroyed(UUID b) { return false; }
            @Override public Optional<String> profession(UUID npc) { return Optional.ofNullable(professions.get(npc)); }
            @Override public void assignProfession(UUID npc, String p, String r) { professions.put(npc, p); }
            @Override public List<String> neededProfessions(UUID v) { return List.of("farmer"); }
            @Override public void transferBuilding(UUID b, UUID owner, String label) { owners.put(b, owner); }
            @Override public List<UUID> buildingsOwnedBy(UUID npc) { return owners.entrySet().stream().filter(x -> x.getValue().equals(npc)).map(Map.Entry::getKey).toList(); }
        });
        f.useEconomy(new FamilyPorts.Economy() {
            @Override public double coins(UUID owner) { return coins.getOrDefault(owner, 0.0); }
            @Override public double transfer(UUID from, UUID to, String name, double amount, String reason) {
                double have = coins.getOrDefault(from, 0.0), t = Math.min(have, amount);
                coins.put(from, have - t); coins.merge(to, t, Double::sum); return t;
            }
        });
        f.useSocial(new FamilyPorts.Social() {
            @Override public double standing(UUID npc, UUID v) { return 0; }
            @Override public double trust(UUID a, UUID b) { return 60; }
            @Override public double respect(UUID a, UUID b) { return 70; }
            @Override public double teachingQuality(UUID m, UUID d) { return 0.8; }
            @Override public void learn(UUID npc, String key, String text, UUID teacher) { learned.add(npc + "|" + key + "|" + text); }
            @Override public double affinity(UUID npc, String profession) { return 0.5; }
            @Override public void experience(UUID npc, UUID other, String otherName, String kind, String note) { experienced.add(npc + "|" + kind); }
        });
        return f;
    }

    private <T> List<T> of(Class<T> type) { return events.stream().filter(type::isInstance).map(type::cast).toList(); }

    @Test void anNpcWithoutAFamilyGetsOneWithAncestors() {
        FamilyEngine f = engine();
        UUID hiro = UUID.randomUUID();
        Person p = f.adopt(hiro, "Takeda Hiro", village, region, houseA, "blacksmith", true);
        FamilyRecord fam = f.familyOf(hiro).orElseThrow();
        assertEquals("Takeda", fam.name());
        assertEquals(2, p.generation());
        assertEquals(2, f.parents(hiro).size());
        assertEquals(4, f.grandparents(hiro).size());
        assertEquals(4, fam.founders().size(), "the founders are the eldest generation (both grandparent couples)");
        assertTrue(f.ancestors(hiro, 0).keySet().stream().allMatch(a -> f.person(a).orElseThrow().state() == LifeState.HISTORICAL), "ancestors are records, not entities");
        assertEquals(hiro, fam.head());
        assertTrue(p.birthEstimated());
        assertTrue(f.age(hiro) >= 18 && f.age(hiro) <= 55);
        assertTrue(f.audit().isEmpty(), f.audit().toString());
    }

    @Test void householdsGroupPeopleIntoKinshipByAge() {
        FamilyEngine f = engine();
        UUID a = UUID.randomUUID();
        f.adopt(a, "Aiko", village, region, houseA, "farmer", true);
        Person pa = f.person(a).orElseThrow();
        UUID b = UUID.randomUUID();
        Person pb = f.adopt(b, "Kenji", village, region, houseA, "farmer", true);
        assertEquals(pa.household(), pb.household(), "both live under one roof");
        if (pa.family().equals(pb.family())) {
            assertTrue(f.isRelated(a, b) || f.partners(a).contains(b), "same family means a recorded kinship: " + f.kinship(a, b));
        } else {
            assertTrue(f.household(pa.household()).orElseThrow().families().containsAll(List.of(pa.family(), pb.family())), "a household may shelter two families");
            assertEquals(a, f.household(pa.household()).orElseThrow().head(), "the first head of the house stays head");
        }
        for (int i = 0; i < 6; i++) f.adopt(UUID.randomUUID(), "Vecino" + i, village, region, houseA, "farmer", true);
        assertTrue(f.members(pa.family()).stream().filter(m -> f.person(m).orElseThrow().state() == LifeState.ALIVE).count() >= 2, "age-compatible housemates join the family");
    }

    @Test void kinshipIsDerivedAndNamedWithoutGuessing() {
        FamilyEngine f = engine();
        UUID g = UUID.randomUUID();
        f.adopt(g, "Sato Taro", village, region, null, "farmer", true);
        calendar.advanceMinutes(0, "");
        Person child = f.birth(g, null, "Ren", village);
        Person grandchild = f.birth(child.id(), null, "Mio", village);
        assertEquals(Set.of(child.id()), f.children(g));
        assertTrue(f.grandparents(grandchild.id()).contains(g));
        assertTrue(f.kinship(grandchild.id(), g).startsWith("abuelo") || f.kinship(grandchild.id(), g).startsWith("abuel"));
        assertEquals(2, f.kinshipDegree(g, grandchild.id()));
        assertEquals(child.id(), f.commonAncestor(child.id(), grandchild.id()).ancestor(), "a parent is the nearest common ancestor of itself and its child");
        assertTrue(f.descendants(g, 0).containsKey(grandchild.id()));
        Person sibling = f.birth(g, null, "Sora", village);
        assertTrue(f.kinship(child.id(), sibling.id()).contains("herman"), f.kinship(child.id(), sibling.id()));
        assertTrue(f.kinship(grandchild.id(), sibling.id()).matches("t[íi][oa].*"), f.kinship(grandchild.id(), sibling.id()));
    }

    @Test void impossibleGenealogiesAreRejected() {
        FamilyEngine f = engine();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        f.adopt(a, "Ito A", village, region, null, "", true);
        f.adopt(b, "Mori B", village, region, null, "", true);
        assertTrue(f.addParent(a, a).isPresent(), "self-parent");
        Person c = f.birth(a, null, "C", village);
        assertTrue(f.addParent(c.id(), a).isPresent(), "a child cannot be its parent's parent (loop / cycle / time)");
        assertTrue(f.addParent(a, c.id()).isPresent(), "duplicate edge");
        Person d = f.birth(c.id(), null, "D", village);
        assertTrue(f.addParent(d.id(), a).isPresent(), "ancestry cycle");
        assertTrue(f.addPartners(a, c.id()).isPresent(), "parent and child cannot be partners");
        assertTrue(f.audit().isEmpty());
        assertTrue(f.metrics().rejectedLinks.get() >= 5);
    }

    @Test void birthsAdvanceGenerationsAndAgeComesFromTheCalendar() {
        FamilyEngine f = engine();
        UUID a = UUID.randomUUID();
        f.adopt(a, "Kato Hana", village, region, houseA, "farmer", true);
        Person baby = f.birth(a, null, null, village);
        assertEquals(3, baby.generation());
        assertEquals(1, of(GenerationAdvancedEvent.class).size());
        assertEquals(LifeStage.INFANT_FUTURE, f.stage(baby.id()));
        calendar.advanceMinutes(20L * 360 * 1440, "test");
        assertEquals(20, f.age(baby.id()), 0.01);
        assertEquals(LifeStage.YOUNG_ADULT, f.stage(baby.id()));
        assertFalse(baby.embodied(), "a born child has no entity until the adapter gives it one");
    }

    @Test void deathTriggersSuccessionInheritanceAndLegacy() {
        FamilyEngine f = engine();
        UUID head = UUID.randomUUID();
        f.adopt(head, "Yamada Isamu", village, region, houseA, "blacksmith", true);
        Person heir = f.birth(head, null, "Jun", village);
        calendar.advanceMinutes(25L * 360 * 1440, "test");
        UUID smithy = UUID.randomUUID();
        owners.put(smithy, head);
        coins.put(head, 100.0);
        UUID katana = UUID.randomUUID();
        f.registerHeirloom(katana, "Katana del fundador", "arma", head);
        f.heirloomEvent(katana, "defendió la aldea");
        f.lifeState(head, LifeState.DECEASED_FUTURE, "cayó defendiendo la aldea");
        FamilyRecord fam = f.familyOf(heir.id()).orElseThrow();
        assertEquals(heir.id(), fam.head());
        assertFalse(of(FamilyHeadChangedEvent.class).isEmpty());
        assertEquals(heir.id(), owners.get(smithy), "the workshop passes to the heir");
        assertEquals(100.0, coins.get(heir.id()), 1e-9);
        Heirloom k = f.heirloom(katana).orElseThrow();
        assertEquals(heir.id(), k.currentOwner());
        assertTrue(k.symbolicValue() > 2);
        assertFalse(f.storedLegacy(head).orElseThrow().causes().isEmpty(), "legacy has causes, not just a number");
        assertFalse(f.inheritances().isEmpty());
    }

    @Test void mastersTeachDisciplesAndSchoolsOutliveFounders() {
        FamilyEngine f = engine();
        UUID master = UUID.randomUUID(), disciple = UUID.randomUUID();
        f.adopt(master, "Miyamoto Musashi", village, region, houseA, "samurai", true);
        f.adopt(disciple, "Sasaki Kojiro", village, region, houseB, "", true);
        professions.put(master, "samurai");
        Lineage school = f.createLineage("Niten", Lineage.Type.SAMURAI, master, "samurai", null);
        f.technique("niten:two-swords", "Estilo de las dos espadas", master, school.id(), 0.9);
        Mentorship m = f.mentor(master, disciple, Mentorship.Type.SAMURAI, school.id(), false);
        for (int day = 0; day < 200 && m.state() == Mentorship.State.ACTIVE; day++) f.train(m, 1);
        assertEquals(Mentorship.State.COMPLETED, m.state());
        assertTrue(f.techniqueOf("niten:two-swords").orElseThrow().holders().contains(disciple));
        assertTrue(learned.stream().anyMatch(l -> l.startsWith(disciple + "|technique:niten:two-swords")), "the Knowledge Engine learned it for the disciple");
        f.lifeState(master, LifeState.DECEASED_FUTURE, "vejez");
        assertEquals(disciple, school.leader(), "the senior disciple leads the school");
        assertEquals(2, school.generations());
        assertTrue(FamilyInspector.lineageTree(f, school).stream().anyMatch(l -> l.contains("Kojiro")));
    }

    @Test void untaughtKnowledgeIsLostButItsEvidenceRemains() {
        FamilyEngine f = engine();
        UUID smith = UUID.randomUUID();
        f.adopt(smith, "Goto Masaru", village, region, houseA, "blacksmith", true);
        f.technique("goto:steel", "Acero plegado de los Goto", smith, null, 0.8);
        f.lifeState(smith, LifeState.MISSING, "no volvió del bosque");
        Technique t = f.techniqueOf("goto:steel").orElseThrow();
        assertTrue(t.lost());
        assertFalse(t.evidence().isEmpty());
        assertEquals(1, of(TechniqueLostEvent.class).size());
    }

    @Test void professionsAreSuggestedNotCopied() {
        FamilyEngine f = engine();
        UUID smith = UUID.randomUUID();
        f.adopt(smith, "Aoki Kenta", village, region, houseA, "blacksmith", true);
        Person child = f.birth(smith, null, "Riku", village);
        var suggestions = f.professionSuggestions(child.id());
        assertEquals("blacksmith", suggestions.get(0).profession(), "growing up at the forge makes it likely...");
        assertTrue(suggestions.stream().anyMatch(s -> s.profession().equals("farmer")), "...but the village's needs are weighed too");
        assertEquals("", child.profession(), "...and nothing is assigned automatically");
    }

    @Test void reputationHonourTraditionsAndStories() {
        FamilyEngine f = engine();
        UUID founder = UUID.randomUUID();
        f.adopt(founder, "Hayashi Tadashi", village, region, houseA, "blacksmith", true);
        FamilyRecord fam = f.familyOf(founder).orElseThrow();
        f.honor(fam.id(), 5, "defendió la aldea", founder);
        f.reputation(fam.id(), 0.3, "salvó el templo", founder);
        Person child = f.birth(founder, null, "Hideo", village);
        assertEquals(0.3, f.expectation(child.id()), 1e-9, "descendants inherit an expectation, not a personal reputation");
        f.professionChanged(child.id(), "blacksmith");
        f.simulate(List.of(village), 1);
        assertTrue(fam.traditions().contains("oficio:blacksmith"), fam.traditions().toString());
        calendar.advanceMinutes(17L * 360 * 1440, "test");
        f.simulate(List.of(village), 1);
        assertTrue(learned.stream().anyMatch(l -> l.startsWith(child.id() + "|family-story:") && l.contains("defendió la aldea")),
                "the family story reaches the child as knowledge told by a relative: " + learned);
    }

    @Test void extinctFamiliesAndBranchesKeepTheirHistory() {
        FamilyEngine f = engine();
        UUID a = UUID.randomUUID();
        f.adopt(a, "Endo Sayuri", village, region, null, "merchant", true);
        Person son = f.birth(a, null, "Takumi", village);
        calendar.advanceMinutes(25L * 360 * 1440, "test");
        FamilyRecord branch = f.branch(f.familyOf(a).orElseThrow().id(), son.id(), "casa mercante propia", village);
        assertEquals(son.id(), branch.head());
        assertNotEquals(branch.id(), f.familyOf(a).orElseThrow().id());
        FamilyRecord main = f.familyOf(a).orElseThrow();
        f.lifeState(a, LifeState.DECEASED_FUTURE, "enfermedad");
        assertEquals(FamilyRecord.Status.EXTINCT, main.status());
        assertFalse(of(FamilyExtinctEvent.class).isEmpty());
        assertFalse(f.history(main.id()).isEmpty(), "an extinct family keeps its history");
        assertTrue(f.person(a).isPresent());
    }

    @Test void familiesSurviveARestart() throws Exception {
        Path dir = Files.createTempDirectory("living-family");
        FamilyEngine a = engine();
        UUID m = UUID.randomUUID(), d = UUID.randomUUID();
        a.adopt(m, "Ota Makoto", village, region, houseA, "samurai", true);
        a.adopt(d, "Fujii Hikari", village, region, houseB, "", true);
        Lineage l = a.createLineage("Escuela Ota", Lineage.Type.SAMURAI, m, "samurai", null);
        a.technique("ota:cut", "Corte del alba", m, l.id(), 0.7);
        Mentorship ms = a.mentor(m, d, Mentorship.Type.SAMURAI, l.id(), false);
        a.train(ms, 10);
        UUID item = UUID.randomUUID();
        a.registerHeirloom(item, "Tsuba de hierro", "guarnición", m);
        Person baby = a.birth(m, null, "Nao", village);
        LivingStorage store = new LivingStorage(dir, false);
        FamilyStorage.sections(a).forEach(store::register);
        assertEquals(2, store.saveAll());
        FamilyEngine b = engine();
        LivingStorage load = new LivingStorage(dir, false);
        FamilyStorage.sections(b).forEach(load::register);
        load.loadAll().values().forEach(r -> assertTrue(r.usable(), r.detail()));
        assertEquals(a.people().size(), b.people().size());
        assertEquals(a.ancestors(baby.id(), 0).keySet(), b.ancestors(baby.id(), 0).keySet());
        assertEquals(a.familyOf(m).orElseThrow().name(), b.familyOf(m).orElseThrow().name());
        assertEquals(ms.progress(), b.masterOf(d).orElseThrow().progress(), 1e-9);
        assertEquals(1, b.lineages().size());
        assertTrue(b.heirloom(item).isPresent());
        assertTrue(b.audit().isEmpty(), b.audit().toString());
        assertFalse(FamilyInspector.tree(b, b.familyOf(m).orElseThrow()).isEmpty());
    }
}
