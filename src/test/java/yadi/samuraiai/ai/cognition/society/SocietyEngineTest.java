package yadi.samuraiai.ai.cognition.society;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.cognition.model.EmotionKind;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.storage.CognitionStorage;
import yadi.samuraiai.ai.cognition.testkit.CognitionHarness;
import yadi.samuraiai.ai.knowledge.culture.Tradition;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeSettings;
import yadi.samuraiai.ai.knowledge.events.CultureUpdatedEvent;
import yadi.samuraiai.ai.knowledge.events.HistoryRecordedEvent;
import yadi.samuraiai.ai.knowledge.events.RumorConfirmedEvent;
import yadi.samuraiai.ai.knowledge.events.RumorCreatedEvent;
import yadi.samuraiai.ai.knowledge.events.RumorSpreadEvent;
import yadi.samuraiai.ai.knowledge.history.HistoricalEvent;
import yadi.samuraiai.ai.knowledge.history.HistoryType;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.model.KnowledgeCategory;
import yadi.samuraiai.ai.knowledge.model.KnowledgeEvidence;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.LearnMethod;
import yadi.samuraiai.ai.knowledge.model.Predicate;
import yadi.samuraiai.ai.knowledge.model.ValidationState;
import yadi.samuraiai.ai.knowledge.propagation.PropagationEngine;
import yadi.samuraiai.ai.knowledge.propagation.PropagationTask;
import yadi.samuraiai.ai.knowledge.rumors.RumorClaim;
import yadi.samuraiai.ai.knowledge.rumors.RumorRecord;
import yadi.samuraiai.ai.knowledge.rumors.RumorState;
import yadi.samuraiai.ai.knowledge.society.Community;
import yadi.samuraiai.ai.knowledge.society.CommunityKind;
import yadi.samuraiai.ai.knowledge.society.SocietyEngine;
import yadi.samuraiai.ai.relationship.model.ReputationLabel;

class SocietyEngineTest {
    private final CognitionHarness h = new CognitionHarness();
    private final EntityRef kenji = h.npc("Kenji"), hanako = h.npc("Hanako"), taro = h.npc("Taro"), mika = h.npc("Mika"), yeremi = h.player("Yeremi");
    private final PlaceRef square = h.at("plaza", 0, 0);

    private SocietyEngine society() { return h.engine.society(); }
    private Community village() { return society().community("aldea").orElseGet(() -> society().create("aldea", "Aldea", CommunityKind.VILLAGE, "village", square, 60)); }

    private void join(EntityRef... npcs) { for (EntityRef n : npcs) society().join(n.id(), village().id(), AccessLevel.MEMBERS); }

    @Test void culturesAreDataAndShapeHowHonorIsRead() {
        Community temple = society().create("templo", "Templo", CommunityKind.TEMPLE, "temple", square, 30);
        society().join(kenji.id(), temple.id(), AccessLevel.MEMBERS);
        join(hanako);
        assertEquals(1.3, society().honorScale(kenji.id()), 1e-9);
        assertEquals(1.0, society().honorScale(hanako.id()), 1e-9);
        assertEquals(1.5, society().oathWeight(kenji.id()), 1e-9);
        // The same betrayal costs the temple member more honor observed than it costs the villager.
        h.live(kenji, ExperienceKind.BETRAYED, yeremi, null);
        h.live(hanako, ExperienceKind.BETRAYED, yeremi, null);
        double templeHonor = h.engine.relationships().find(kenji.id(), yeremi.id()).orElseThrow().honor();
        double villageHonor = h.engine.relationships().find(hanako.id(), yeremi.id()).orElseThrow().honor();
        assertTrue(templeHonor < villageHonor, templeHonor + " vs " + villageHonor);
    }

    @Test void customCulturesAreReadFromConfigurationLines() {
        h.knowledgeSettings = KnowledgeSettings.builder().set("cultures", List.of("ronin|Ronin|Duelo al alba~RITUAL~DAWN~TRAINING~0.9|HONOR_SCALE~2.0")).build();
        Community c = society().create("ronin-camp", "Campamento", CommunityKind.CLAN, "ronin", square, 20);
        society().join(kenji.id(), c.id(), AccessLevel.MEMBERS);
        assertEquals(2.0, society().honorScale(kenji.id()), 1e-9);
        assertTrue(society().dueTradition(kenji.id(), "DAWN", "TRAINING").isPresent());
    }

    @Test void aTraditionDueNowIsRecognisedAndStrengthensWhenObserved() {
        Community temple = society().create("templo", "Templo", CommunityKind.TEMPLE, "temple", square, 30);
        society().join(kenji.id(), temple.id(), AccessLevel.MEMBERS);
        assertTrue(society().dueTradition(kenji.id(), "AFTERNOON", "TEMPLE").isEmpty());
        Tradition due = society().dueTradition(kenji.id(), "DAWN", "TEMPLE").orElseThrow();
        assertEquals(Tradition.Kind.RITUAL, due.kind());
        assertTrue(society().observeTradition(kenji.id(), due, h.now));
        double strength = temple.traditions().get(due.id()).strength();
        assertTrue(strength > due.strength());
        assertEquals(1, temple.traditions().get(due.id()).observed());
        assertFalse(h.events(CultureUpdatedEvent.class).isEmpty());
        h.advance(24000L * 60);
        society().tick(h.now);
        assertTrue(temple.traditions().get(due.id()).strength() < strength, "neglected traditions erode");
    }

    private KnowledgeEvidence fact(EntityRef npc, EntityRef subject) {
        return new KnowledgeEvidence(npc.id(), KnowledgeType.FACT, KnowledgeCategory.GENERAL, subject, Predicate.HAS_ROLE, hanako, Map.of(), LearnMethod.OBSERVATION, null, -1, PlaceRef.unknown(), Set.of(), null, null, h.now, 1, 0.6, AccessLevel.PUBLIC, null);
    }

    @Test void whatEnoughMembersBelieveBecomesTheCommunitysCollectiveKnowledge() {
        join(kenji, hanako, taro, mika);
        var r1 = h.engine.knowledge().learn(fact(kenji, yeremi)).record();
        assertFalse(society().noteKnown("aldea", r1, kenji.id(), h.now), "one member is not the community");
        assertEquals(0, village().collective().size());
        var r2 = h.engine.knowledge().learn(fact(hanako, yeremi)).record();
        assertTrue(society().noteKnown("aldea", r2, hanako.id(), h.now), "half the members agree");
        assertEquals(1, village().collective().size());
        assertEquals(village().uuid(), village().collective().all().get(0).owner());
        assertEquals(1, h.engine.knowledge().metrics().adopted.get());
    }

    @Test void publicEventsBecomeHistoryAndTheWorldRemembersHeroesAndTraitors() {
        join(kenji, hanako);
        h.live(h.input(kenji, ExperienceKind.PROTECTED_OTHERS).actor(kenji).target(hanako).place(square));
        h.live(h.input(hanako, ExperienceKind.BETRAYED).actor(yeremi).place(square).publicEvent(true));
        var history = society().history("aldea");
        assertTrue(history.stream().anyMatch(e -> e.type() == HistoryType.HERO_ACT), history.toString());
        assertTrue(history.stream().anyMatch(e -> e.type() == HistoryType.BETRAYAL));
        assertTrue(society().worldMemory().heroes(5).stream().anyMatch(l -> l.id().equals(kenji.id())));
        assertTrue(society().worldMemory().traitors(5).stream().anyMatch(l -> l.id().equals(yeremi.id())));
        assertFalse(society().worldMemory().traitors(5).stream().anyMatch(l -> l.id().equals(kenji.id())));
        assertEquals(2, society().worldTimeline().size());
        assertFalse(h.events(HistoryRecordedEvent.class).isEmpty());
        assertTrue(village().standing().containsKey(yeremi.id()), "the community's view of the traitor is recorded");
    }

    @Test void historyIsBoundedAndKeepsTheGreatEvents() {
        h.knowledgeSettings = KnowledgeSettings.builder().set("historyMax", 3).build();
        village();
        double[] significance = {0.6, 0.9, 0.7, 0.55, 0.8};
        for (int i = 0; i < significance.length; i++)
            society().record("aldea", new HistoricalEvent(UUID.randomUUID(), HistoryType.BATTLE, h.now + i * 100L, square, List.of(), Set.of(), significance[i], "BATTLE", null, "aldea", null));
        var kept = society().history("aldea").stream().map(HistoricalEvent::significance).sorted().toList();
        assertEquals(List.of(0.7, 0.8, 0.9), kept);
        assertTrue(society().history("aldea").get(0).at() < society().history("aldea").get(2).at(), "time ordered");
        society().record("aldea", new HistoricalEvent(UUID.randomUUID(), HistoryType.VISIT, h.now, square, List.of(), Set.of(), 0.1, "VISIT", null, "aldea", null));
        assertEquals(3, society().history("aldea").size(), "trivial events are not history");
    }

    @Test void membersHoldRanksAndFactionKnowledgeRespectsAccessControl() {
        Community guards = society().create("guardia", "Guardia", CommunityKind.FACTION, "guard", square, 40);
        society().join(kenji.id(), guards.id(), AccessLevel.LEADERS);
        society().join(hanako.id(), guards.id(), AccessLevel.MEMBERS);
        var secret = new KnowledgeEvidence(kenji.id(), KnowledgeType.FACT, KnowledgeCategory.GENERAL, yeremi, Predicate.HAS_ROLE, taro, Map.of(), LearnMethod.OBSERVATION, null, -1, PlaceRef.unknown(), Set.of(), null, null, h.now, 1, 0.9, AccessLevel.LEADERS, null);
        var open = new KnowledgeEvidence(kenji.id(), KnowledgeType.FACT, KnowledgeCategory.GENERAL, yeremi, Predicate.KNOWS, taro, Map.of(), LearnMethod.OBSERVATION, null, -1, PlaceRef.unknown(), Set.of(), null, null, h.now, 1, 0.9, AccessLevel.PUBLIC, null);
        var s = h.engine.knowledge().learn(secret).record();
        var o = h.engine.knowledge().learn(open).record();
        assertEquals(AccessLevel.MEMBERS, society().sharedRank(kenji.id(), hanako.id()).ordinal() >= 1 ? AccessLevel.MEMBERS : AccessLevel.PUBLIC);
        var toShare = society().propagation().select(h.engine.knowledge().runtime(kenji.id()), h.engine.knowledge().runtime(hanako.id()), society().sharedRank(kenji.id(), hanako.id()), h.knowledgeSettings);
        assertTrue(toShare.stream().anyMatch(r -> r.id().equals(o.id())));
        assertFalse(toShare.stream().anyMatch(r -> r.id().equals(s.id())), "not everyone in a faction knows everything");
        assertEquals(AccessLevel.LEADERS, society().rankOf(kenji.id(), "guardia"));
        assertEquals(guards.id(), society().communitiesOf(kenji.id()).iterator().next());
    }

    @Test void theSocietyIsPersistentWithCommunitiesHistoryTraditionsAndRumors(@org.junit.jupiter.api.io.TempDir Path dir) {
        join(kenji, hanako);
        Community temple = society().create("templo", "Templo", CommunityKind.TEMPLE, "temple", square, 30);
        society().join(kenji.id(), temple.id(), AccessLevel.LEADERS);
        society().observeTradition(kenji.id(), society().dueTradition(kenji.id(), "DAWN", "TEMPLE").orElseThrow(), h.now);
        h.live(h.input(hanako, ExperienceKind.WITNESSED_ATTACK).actor(yeremi).target(kenji).place(square));
        assertFalse(society().rumors().isEmpty());
        var storage = new CognitionStorage(dir, true);
        h.engine.useStorage(storage);
        assertTrue(storage.saveSociety(h.engine));
        var second = new CognitionHarness();
        var loaded = new CognitionStorage(dir, true).loadSociety(second.engine);
        assertTrue(loaded.usable(), loaded.detail());
        SocietyEngine restored = second.engine.society();
        assertEquals(society().communities().size(), restored.communities().size());
        Community back = restored.community("templo").orElseThrow();
        assertEquals(AccessLevel.LEADERS, back.rankOf(kenji.id()));
        assertEquals(temple.traditions().keySet(), back.traditions().keySet());
        assertEquals(society().rumors().size(), restored.rumors().size());
        assertEquals(society().rumors().get(0).origin().id(), restored.rumors().get(0).origin().id());
        assertEquals(society().rumors().get(0).originMemory(), restored.rumors().get(0).originMemory());
        assertEquals(society().history("aldea").size(), restored.history("aldea").size());
        assertEquals(1.3, restored.honorScale(kenji.id()), 1e-9);
    }
}
