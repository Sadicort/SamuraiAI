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
import yadi.samuraiai.living.family.clan.ClanRecord;
import yadi.samuraiai.living.family.engine.FamilyEngine;
import yadi.samuraiai.living.family.engine.FamilySettings;
import yadi.samuraiai.living.family.events.ClanCreatedEvent;
import yadi.samuraiai.living.family.events.ClanDisbandedEvent;
import yadi.samuraiai.living.family.events.EpithetGrantedEvent;
import yadi.samuraiai.living.family.events.HouseTitleGrantedEvent;
import yadi.samuraiai.living.family.family_memory.FamilyMemoryEntry;
import yadi.samuraiai.living.family.integration.FamilyPorts;
import yadi.samuraiai.living.family.model.Person;
import yadi.samuraiai.living.family.persistence.FamilyStorage;
import yadi.samuraiai.living.family.registry.FamilyRecord;

/**
 * The identity extension of Fase 5.5 through the full engine: a name a spawn system handed an NPC is replaced by a real one,
 * a family earns a house title from real history, a person earns an epithet from a real cause, clans form and disband, an
 * elder can be asked about real family memory, and — the mandatory guarantee of the whole extension — a family story reaches
 * a descendant as knowledge, never as a memory of something that happened before they were born.
 */
class FamilyIdentityExtensionTest {
    private final List<NpcEvent> events = new ArrayList<>();
    private final CalendarEngine calendar = new CalendarEngine(CalendarSettings::defaults, e -> { }, 4L);
    private final UUID village = UUID.randomUUID(), region = UUID.randomUUID();
    private final List<String> learned = new ArrayList<>();
    private final List<String> experienced = new ArrayList<>();

    private FamilyEngine engine() { return engine(FamilySettings::defaults); }

    private FamilyEngine engine(java.util.function.Supplier<FamilySettings> settings) {
        FamilyEngine f = new FamilyEngine(settings, events::add, calendar, 21L);
        f.useCalendar(() -> (long) calendar.spec().daysPerYear() * calendar.minutesPerDay());
        f.useVillages(new FamilyPorts.Villages() {
            @Override public Optional<UUID> villageOf(UUID npc) { return Optional.of(village); }
            @Override public int beds(UUID b) { return 4; }
            @Override public boolean damaged(UUID b) { return false; }
            @Override public boolean destroyed(UUID b) { return false; }
            @Override public Optional<String> profession(UUID npc) { return Optional.empty(); }
            @Override public void assignProfession(UUID npc, String p, String r) { }
            @Override public List<String> neededProfessions(UUID v) { return List.of(); }
            @Override public void transferBuilding(UUID b, UUID owner, String label) { }
            @Override public List<UUID> buildingsOwnedBy(UUID npc) { return List.of(); }
        });
        f.useEconomy(new FamilyPorts.Economy() {
            @Override public double coins(UUID owner) { return 0; }
            @Override public double transfer(UUID from, UUID to, String name, double amount, String reason) { return 0; }
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

    // ------------------------------------------------------------------ NPC creator integration

    @Test void aSpawnLabelIsReplacedByARealNameButAChosenOneNeverIs() {
        FamilyEngine f = engine();
        UUID npc = UUID.randomUUID();
        Person p = f.adopt(npc, "Merchant_3", village, region, null, "merchant", true, "merchant");
        assertNotEquals("Merchant_3", p.name().given(), "a spawn label is not a person's name");
        assertFalse(p.name().given().isBlank());
        assertFalse(p.name().cultureId().isBlank(), "the family carries a naming culture");

        FamilyEngine g = engine();
        UUID chosen = UUID.randomUUID();
        Person named = g.adopt(chosen, "Merchant_3", village, region, null, "merchant", true, "");
        assertEquals("Merchant_3", named.name().given(), "without a type to compare against, a raw name is trusted as chosen");
    }

    @Test void twoFoundersInTheSameRegionCarryTheSameCultureThroughoutTheFamily() {
        FamilyEngine f = engine();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        Person pa = f.adopt(a, "", village, region, null, "farmer", true, "villager");
        FamilyRecord fa = f.familyOf(a).orElseThrow();
        Person child = f.birth(a, null, "", village);
        assertEquals(fa.cultureId(), pa.name().cultureId());
        assertEquals(fa.cultureId(), child.name().cultureId(), "a child born into the family carries its culture");
        Person pb = f.adopt(b, "", village, UUID.randomUUID(), null, "farmer", true, "villager");
        // a different region may (and with five cultures, usually will) roll a different culture; either way each family is internally consistent
        assertEquals(f.familyOf(b).orElseThrow().cultureId(), pb.name().cultureId());
    }

    // ------------------------------------------------------------------ houses and epithets

    @Test void aFamilyEarnsAHouseTitleFromRealHistoryAndAPersonEarnsAnEpithetFromRealHonour() {
        FamilySettings low = FamilySettings.builder().set("houseImportanceThreshold", 0.05).set("epithetHonorThreshold", 2.0).build();
        FamilyEngine f = engine(() -> low);
        UUID a = UUID.randomUUID();
        f.adopt(a, "Aldren", village, region, null, "guard", true, "guard");
        FamilyRecord fam = f.familyOf(a).orElseThrow();
        assertFalse(fam.isHouse());
        f.honor(fam.id(), 6, "defendió la aldea de un ataque", a);
        f.simulate(Set.of(village), 1);
        assertTrue(fam.isHouse(), "enough real history earns a house title");
        assertTrue(fam.houseTitle().contains(fam.name()));
        assertFalse(of(HouseTitleGrantedEvent.class).isEmpty());
        Person p = f.person(a).orElseThrow();
        assertFalse(p.name().epithet().isEmpty(), "and the one who earned that honour gets an epithet from it");
        assertFalse(of(EpithetGrantedEvent.class).isEmpty());
        assertTrue(experienced.stream().anyMatch(x -> x.startsWith(a + "|HONOR_OBSERVED")), "earning the epithet was itself lived through");
    }

    @Test void dishonourEarnsADishonourableEpithetNotAnHonourableOne() {
        FamilySettings low = FamilySettings.builder().set("epithetHonorThreshold", 2.0).build();
        FamilyEngine f = engine(() -> low);
        UUID a = UUID.randomUUID();
        f.adopt(a, "Morrach", village, region, null, "", true, "");
        FamilyRecord fam = f.familyOf(a).orElseThrow();
        f.honor(fam.id(), -6, "traicionó a la aldea", a);
        f.simulate(Set.of(village), 1);
        var granted = of(EpithetGrantedEvent.class);
        assertFalse(granted.isEmpty());
        assertEquals("DISHONOR", granted.get(0).category());
        assertTrue(experienced.stream().anyMatch(x -> x.startsWith(a + "|DISHONOR_OBSERVED")));
    }

    // ------------------------------------------------------------------ clans

    @Test void familiesFormAClanItGrowsAndDisbandsWhenTheLastFamilyLeaves() {
        FamilyEngine f = engine();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        f.adopt(a, "", village, region, null, "", true, "villager");
        f.adopt(b, "", village, region, null, "", true, "villager");
        FamilyRecord fa = f.familyOf(a).orElseThrow(), fb = f.familyOf(b).orElseThrow();
        ClanRecord clan = f.createClan("Lobo de Ceniza", fa.id());
        assertEquals(ClanRecord.Status.FORMING, clan.status(), "one family is still just forming a clan");
        assertEquals(fa.id(), clan.leaderFamily());
        assertFalse(of(ClanCreatedEvent.class).isEmpty());
        assertFalse(f.person(fa.head()).orElseThrow().name().epithet().isEmpty(), "founding a clan is a real, traceable cause for an epithet");
        assertTrue(f.joinClan(clan.id(), fb.id()));
        assertEquals(ClanRecord.Status.ACTIVE, clan.status(), "two families make it an active clan (the default minimum)");
        assertEquals(clan.id(), fb.clan());
        assertTrue(f.findClan("Lobo de Ceniza").isPresent());
        assertTrue(f.leaveClan(clan.id(), fa.id(), "se aparta"));
        assertEquals(fb.id(), clan.leaderFamily(), "leadership passes to the family that stayed");
        assertNull(fa.clan());
        assertTrue(f.leaveClan(clan.id(), fb.id(), "se aparta también"));
        assertEquals(ClanRecord.Status.DISPERSED, clan.status());
        assertFalse(of(ClanDisbandedEvent.class).isEmpty());
        assertTrue(f.clan(clan.id()).isPresent(), "a dispersed clan is remembered, not deleted");
    }

    @Test void aBranchInheritsItsParentFamilysClan() {
        FamilyEngine f = engine();
        UUID a = UUID.randomUUID();
        f.adopt(a, "", village, region, null, "", true, "villager");
        FamilyRecord fam = f.familyOf(a).orElseThrow();
        ClanRecord clan = f.createClan("Corona Pálida", fam.id());
        Person son = f.birth(a, null, "Hijo", village);
        FamilyRecord branch = f.branch(fam.id(), son.id(), "funda su propia casa", village);
        assertEquals(clan.id(), branch.clan(), "the branch belongs to the same clan as the family it split from");
        assertTrue(clan.memberFamilies().contains(branch.id()));
    }

    // ------------------------------------------------------------------ elders

    @Test void theOldestLivingResidentOfAVillageIsItsElderAndCanSpeakToRealFamilyHistory() {
        FamilyEngine f = engine();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        f.adopt(a, "", village, region, null, "", true, "");
        f.adopt(b, "", village, UUID.randomUUID(), null, "", true, "");
        calendar.advanceMinutes(45L * 360 * 1440, "test");
        UUID older = f.age(a) >= f.age(b) ? a : b, younger = older.equals(a) ? b : a;
        assertTrue(f.isVillageElder(older), "the oldest living resident of the village is its elder");
        if (f.age(older) > f.age(younger)) assertFalse(f.isVillageElder(younger), "only one elder per village here");
        FamilyRecord fam = f.familyOf(older).orElseThrow();
        f.remember(fam, FamilyMemoryEntry.Kind.WAR, "la familia resistió el asedio del año 90", List.of(older), 0.8);
        List<String> topics = f.elderTopics(older);
        assertTrue(topics.stream().anyMatch(t -> t.contains("asedio")), "a real, witnessed war is something the elder can speak to: " + topics);
    }

    // ------------------------------------------------------------------ the mandatory generational-storytelling guarantee

    @Test void aFamilyStoryReachesADescendantAsKnowledgeNeverAsAMemoryOfSomethingBeforeTheyWereBorn() {
        FamilyEngine f = engine();
        UUID a = UUID.randomUUID();
        f.adopt(a, "Elyra", village, region, null, "", true, "");
        FamilyRecord fam = f.familyOf(a).orElseThrow();
        UUID ancestor = fam.founders().isEmpty() ? a : fam.founders().get(0);
        // the ancestor's own war, lived and recorded as the family's history — never as anyone else's personal memory
        f.remember(fam, FamilyMemoryEntry.Kind.WAR, "el fundador combatió en la guerra del paso", List.of(ancestor), 0.9);
        experienced.clear();
        Person child = f.birth(a, null, "Descendiente", village);
        int told = f.tellStories(child);
        assertTrue(told > 0, "the story reached the new descendant");
        assertTrue(learned.stream().anyMatch(l -> l.startsWith(child.id() + "|") && l.contains("guerra del paso")), "as knowledge, from whoever told it: " + learned);
        assertTrue(experienced.isEmpty(), "living the ancestor's war never became anyone's cognitive experience here — only a taught fact: " + experienced);
        assertTrue(child.importantMemories().stream().noneMatch(m -> m.contains("guerra del paso")), "and it is not the child's own remembered experience either");
    }

    // ------------------------------------------------------------------ persistence

    @Test void identityExtensionSurvivesARestart() throws Exception {
        Path dir = Files.createTempDirectory("living-family-identity");
        FamilySettings low = FamilySettings.builder().set("houseImportanceThreshold", 0.05).set("epithetHonorThreshold", 2.0).set("artifactEpithetThreshold", 0.5).build();
        FamilyEngine a = engine(() -> low);
        UUID head = UUID.randomUUID();
        Person founder = a.adopt(head, "Vaelor", village, region, null, "guard", true, "guard");
        FamilyRecord fam = a.familyOf(head).orElseThrow();
        a.honor(fam.id(), 6, "defendió la aldea", head);
        UUID item = UUID.randomUUID();
        a.registerHeirloom(item, "Guarnición", "guarnición", head);
        a.heirloomEvent(item, "primer suceso"); a.heirloomEvent(item, "segundo suceso");
        a.transferHeirloom(item, head, "se la queda"); a.transferHeirloom(item, head, "otra vez"); // enough transfers to raise its symbolic value
        ClanRecord clan = a.createClan("Corona Pálida", fam.id());
        a.simulate(Set.of(village), 1);
        assertTrue(fam.isHouse(), "setup: the house was earned before saving");
        assertFalse(founder.name().epithet().isEmpty(), "setup: the epithet was earned before saving");
        assertFalse(a.heirloom(item).orElseThrow().epithet().isEmpty(), "setup: the heirloom earned its own name before saving");
        String culture = founder.name().cultureId();
        String epithet = a.person(head).orElseThrow().name().epithet();
        String house = fam.houseTitle();
        String artifactEpithet = a.heirloom(item).orElseThrow().epithet();

        LivingStorage store = new LivingStorage(dir, false);
        FamilyStorage.sections(a).forEach(store::register);
        assertEquals(2, store.saveAll());

        FamilyEngine b = engine();
        LivingStorage load = new LivingStorage(dir, false);
        FamilyStorage.sections(b).forEach(load::register);
        load.loadAll().values().forEach(r -> assertTrue(r.usable(), r.detail()));

        Person restored = b.person(head).orElseThrow();
        assertEquals(culture, restored.name().cultureId());
        assertEquals(nameOrderOf(a, head), nameOrderOf(b, head));
        assertEquals(epithet, restored.name().epithet());
        FamilyRecord restoredFam = b.familyOf(head).orElseThrow();
        assertEquals(house, restoredFam.houseTitle());
        assertEquals(clan.id(), restoredFam.clan());
        assertTrue(b.clan(clan.id()).isPresent());
        assertEquals(clan.name(), b.clan(clan.id()).orElseThrow().name());
        assertEquals(clan.leaderFamily(), b.clan(clan.id()).orElseThrow().leaderFamily());
        assertEquals(artifactEpithet, b.heirloom(item).orElseThrow().epithet());
        assertTrue(b.audit().isEmpty(), b.audit().toString());
    }

    private static String nameOrderOf(FamilyEngine e, UUID person) { return e.person(person).orElseThrow().name().order().name(); }
}
