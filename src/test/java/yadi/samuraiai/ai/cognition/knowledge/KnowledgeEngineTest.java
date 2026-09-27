package yadi.samuraiai.ai.cognition.knowledge;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.cognition.model.EntityKind;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.model.PlaceRef;
import yadi.samuraiai.ai.cognition.testkit.CognitionHarness;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeEngine;
import yadi.samuraiai.ai.knowledge.engine.KnowledgeRuntime;
import yadi.samuraiai.ai.knowledge.engine.LearnResult;
import yadi.samuraiai.ai.knowledge.events.DiscoveryEvent;
import yadi.samuraiai.ai.knowledge.events.KnowledgeTaughtEvent;
import yadi.samuraiai.ai.knowledge.events.KnowledgeValidatedEvent;
import yadi.samuraiai.ai.knowledge.model.AccessLevel;
import yadi.samuraiai.ai.knowledge.model.KnowledgeCategory;
import yadi.samuraiai.ai.knowledge.model.KnowledgeEvidence;
import yadi.samuraiai.ai.knowledge.model.KnowledgeRecord;
import yadi.samuraiai.ai.knowledge.model.KnowledgeType;
import yadi.samuraiai.ai.knowledge.model.LearnMethod;
import yadi.samuraiai.ai.knowledge.model.Predicate;
import yadi.samuraiai.ai.knowledge.model.ValidationEvidence;
import yadi.samuraiai.ai.knowledge.model.ValidationState;
import yadi.samuraiai.ai.knowledge.places.PlaceKnowledge;
import yadi.samuraiai.ai.knowledge.teaching.TeachingResult;

class KnowledgeEngineTest {
    private final CognitionHarness h = new CognitionHarness();
    private final EntityRef kenji = h.npc("Kenji"), hanako = h.npc("Hanako"), yeremi = h.player("Yeremi");
    private final EntityRef temple = EntityRef.of(EntityRef.nameId("place", "templo"), EntityKind.PLACE, "Templo del alba");
    private final EntityRef village = EntityRef.of(EntityRef.nameId("place", "aldea"), EntityKind.PLACE, "Aldea");
    private final EntityRef bridge = EntityRef.of(EntityRef.nameId("place", "puente"), EntityKind.PLACE, "Puente rojo");

    private KnowledgeEngine k() { return h.engine.knowledge(); }

    private KnowledgeEvidence ev(EntityRef npc, EntityRef subject, Predicate p, EntityRef object, LearnMethod method, EntityRef source) {
        return new KnowledgeEvidence(npc.id(), KnowledgeType.FACT, KnowledgeCategory.GENERAL, subject, p, object, Map.of(), method, source, -1, PlaceRef.unknown(), Set.of(), null, null, h.now, 1.0, 0.5, AccessLevel.PUBLIC, null);
    }

    @Test void knowledgeIsAStructuredRecordWithSourceConfidenceAndValidationNotFreeText() {
        LearnResult r = k().learn(ev(kenji, temple, Predicate.LOCATED_AT, null, LearnMethod.OBSERVATION, null));
        KnowledgeRecord rec = r.record();
        assertTrue(r.created());
        assertEquals(KnowledgeType.FACT, rec.type());
        assertEquals(LearnMethod.OBSERVATION, rec.origin());
        assertTrue(rec.confidence() >= 0.8);
        assertEquals(ValidationState.VERIFIED, rec.state(), "seen with its own eyes");
        assertEquals(1, rec.version());
        assertEquals(kenji.id(), rec.owner());
    }

    @Test void howSomethingWasLearnedSetsHowMuchItIsBelieved() {
        var seen = k().learn(ev(kenji, temple, Predicate.LOCATED_AT, null, LearnMethod.OBSERVATION, null)).record();
        var told = k().learn(ev(kenji, village, Predicate.LOCATED_AT, null, LearnMethod.CONVERSATION, hanako)).record();
        var rumor = k().learn(ev(kenji, bridge, Predicate.LOCATED_AT, null, LearnMethod.RUMOR, hanako)).record();
        assertTrue(seen.confidence() > told.confidence() && told.confidence() > rumor.confidence());
        assertEquals(ValidationState.RUMOR, rumor.state());
        assertNotEquals(ValidationState.VERIFIED, told.state());
    }

    @Test void aRepeatedRumorIsNeverPromotedToTruthByRepetition() {
        KnowledgeRecord r = k().learn(ev(kenji, yeremi, Predicate.ATTACKED, hanako, LearnMethod.RUMOR, hanako)).record();
        for (int i = 0; i < 12; i++) k().learn(ev(kenji, yeremi, Predicate.ATTACKED, hanako, LearnMethod.RUMOR, hanako));
        assertEquals(ValidationState.RUMOR, r.state(), "same source, however often: still a rumor");
        assertEquals(1, r.supporters().size());
        EntityRef b = h.npc("B"), c = h.npc("C"), d = h.npc("D");
        for (EntityRef source : List.of(b, c, d)) k().learn(ev(kenji, yeremi, Predicate.ATTACKED, hanako, LearnMethod.RUMOR, source));
        assertTrue(r.state() == ValidationState.LIKELY, "independent sources make it likely: " + r.state());
        assertNotEquals(ValidationState.VERIFIED, r.state(), "but only evidence makes it verified");
        k().learn(ev(kenji, yeremi, Predicate.ATTACKED, hanako, LearnMethod.OBSERVATION, null));
        assertEquals(ValidationState.VERIFIED, r.state());
        assertTrue(r.revisions().size() >= 2);
        assertFalse(h.events(KnowledgeValidatedEvent.class).isEmpty());
    }

    @Test void contradictingEvidenceProvesAFalseRumorFalse() {
        KnowledgeRecord r = k().learn(ev(kenji, yeremi, Predicate.ATTACKED, hanako, LearnMethod.RUMOR, hanako)).record();
        var direct = new ValidationEvidence(ValidationEvidence.Kind.DIRECT_CONTRADICTS, kenji.id(), 1.0, h.now, "I was there and it did not happen");
        k().validate(kenji.id(), r.id(), direct);
        k().validate(kenji.id(), r.id(), new ValidationEvidence(ValidationEvidence.Kind.CONTRADICTING_SOURCE, hanako.id(), 0.9, h.now, ""));
        assertEquals(ValidationState.FALSE, r.state());
        assertFalse(r.contradictors().isEmpty());
    }

    @Test void aOneValuePredicateReplacesTheOldBeliefWhenNewDirectEvidenceArrives() {
        EntityRef old = h.npc("OldLeader"), fresh = h.npc("NewLeader");
        var first = k().learn(ev(kenji, village, Predicate.LEADS, old, LearnMethod.OBSERVATION, null)).record();
        double before = first.confidence();
        k().learn(ev(kenji, village, Predicate.LEADS, fresh, LearnMethod.OBSERVATION, null));
        assertTrue(first.confidence() < before * 0.6, "the old belief lost certainty");
        assertFalse(first.revisions().isEmpty());
    }

    @Test void facingTheWorldAPlaceIsDiscoveredFiledAndKnownWithItsCategory() {
        var out = h.live(h.input(kenji, ExperienceKind.DISCOVERED_PLACE).place(h.at("cave-north", 300, 300)).context("placeName", "Cueva del norte").context("category", "CAVE"));
        var learned = out.knowledge().stream().filter(l -> l.record().predicate() == Predicate.LOCATED_AT).findFirst().orElseThrow();
        KnowledgeRecord place = learned.record();
        assertEquals(KnowledgeType.PLACE, place.type());
        assertEquals(KnowledgeCategory.CAVE, place.category());
        assertTrue(place.place().known() && place.importance() > 0.3);
        assertTrue(place.memoryLinks().contains(out.memory().record().id()), "the fact knows the memory it came from");
        assertTrue(out.memory().record().knowledgeLinks().contains(place.id()), "and the memory knows the fact");
        assertEquals(1, h.events(DiscoveryEvent.class).size());
        assertEquals(place.id(), PlaceKnowledge.nearest(k().runtime(kenji.id()), h.at("x", 310, 305), KnowledgeCategory.CAVE).id());
    }

    @Test void teachingPassesKnowledgeOnlyWhenTheLessonIsGoodAndAccessAllows() {
        var topic = k().learn(ev(kenji, temple, Predicate.LOCATED_AT, null, LearnMethod.OBSERVATION, null)).record();
        var secret = k().learn(new KnowledgeEvidence(kenji.id(), KnowledgeType.FACT, KnowledgeCategory.GENERAL, yeremi, Predicate.HAS_ROLE, bridge, Map.of(), LearnMethod.OBSERVATION, null, -1, PlaceRef.unknown(), Set.of(), null, null, h.now, 1.0, 0.7, AccessLevel.LEADERS, null)).record();
        TeachingResult good = k().teach(kenji.id(), hanako.id(), List.of(topic.id(), secret.id()), 0.9, 0.9, 1.0, (student, level) -> level.visibleTo(AccessLevel.MEMBERS), h.now, null);
        assertEquals(1, good.learnedCount());
        KnowledgeRecord taught = k().runtime(hanako.id()).byKey(topic.key());
        assertNotNull(taught);
        assertEquals(LearnMethod.TEACHING, taught.origin());
        assertEquals(kenji.id(), taught.source().id());
        assertTrue(taught.confidence() <= topic.confidence(), "a student never knows more than the teacher");
        assertNull(k().runtime(hanako.id()).byKey(secret.key()), "the restricted fact was not taught");
        assertEquals("restricted", good.topics().get(1).note());
        TeachingResult poor = k().teach(kenji.id(), h.npc("Bored").id(), List.of(topic.id()), 0.1, 0.05, 0.1, null, h.now, null);
        assertEquals(0, poor.learnedCount());
        assertTrue(poor.quality() < 0.35);
        assertFalse(h.events(KnowledgeTaughtEvent.class).isEmpty());
    }

    @Test void learningSpeedFollowsPersonality() {
        EntityRef curious = h.npc("Curious"), dull = h.npc("Dull");
        h.setTrait(curious, yadi.samuraiai.ai.cognition.model.Trait.CURIOSITY, 95); h.setTrait(curious, yadi.samuraiai.ai.cognition.model.Trait.DISCIPLINE, 95);
        h.setTrait(dull, yadi.samuraiai.ai.cognition.model.Trait.CURIOSITY, 5); h.setTrait(dull, yadi.samuraiai.ai.cognition.model.Trait.DISCIPLINE, 5);
        var a = k().learn(ev(curious, temple, Predicate.LOCATED_AT, null, LearnMethod.CONVERSATION, hanako)).record();
        var b = k().learn(ev(dull, temple, Predicate.LOCATED_AT, null, LearnMethod.CONVERSATION, hanako)).record();
        assertTrue(a.confidence() > b.confidence());
    }

    @Test void theKnowledgeGraphAnswersQueriesAndPathsFromItsIndexes() {
        k().learn(ev(kenji, village, Predicate.CONNECTS, bridge, LearnMethod.OBSERVATION, null));
        k().learn(ev(kenji, bridge, Predicate.CONNECTS, temple, LearnMethod.OBSERVATION, null));
        k().learn(ev(kenji, kenji, Predicate.LIVES_AT, village, LearnMethod.EXPERIENCE, null));
        var graph = k().graph(kenji.id());
        assertEquals(Set.of(bridge.id()), graph.neighbors(village.id(), Predicate.CONNECTS).stream().filter(id -> !id.equals(village.id())).collect(java.util.stream.Collectors.toSet()));
        assertEquals(1, graph.query(kenji.id(), Predicate.LIVES_AT, null).size());
        var path = graph.path(village.id(), temple.id(), 4);
        assertEquals(2, path.size());
        assertTrue(graph.path(village.id(), UUID.randomUUID(), 4).isEmpty());
        assertEquals(1, graph.incoming(temple.id(), Predicate.CONNECTS).size());
        assertEquals(3, graph.edgeCount());
    }

    @Test void theEncyclopediaAnswersByNameCategoryAndPrefix() {
        k().learn(new KnowledgeEvidence(kenji.id(), KnowledgeType.PLACE, KnowledgeCategory.TEMPLE, temple, Predicate.LOCATED_AT, null, Map.of("name", "Templo del alba"), LearnMethod.OBSERVATION, null, -1, h.at("t", 1, 1), Set.of(), null, null, h.now, 1, 0.9, AccessLevel.PUBLIC, null));
        k().learn(new KnowledgeEvidence(kenji.id(), KnowledgeType.PLACE, KnowledgeCategory.MARKET, village, Predicate.LOCATED_AT, null, Map.of("name", "Mercado"), LearnMethod.CONVERSATION, hanako, -1, h.at("m", 90, 90), Set.of(), null, null, h.now, 1, 0.4, AccessLevel.PUBLIC, null));
        var enc = k().encyclopedia(kenji.id());
        assertEquals(1, enc.lookup("templo del ALBA").size());
        assertEquals(KnowledgeCategory.TEMPLE, enc.byCategory(KnowledgeCategory.TEMPLE, 5).get(0).category());
        assertEquals(2, enc.entries(KnowledgeType.PLACE, 10).size());
        assertEquals(List.of("templo del alba"), enc.namesStartingWith("templ", 5));
        assertTrue(enc.describe("Mercado").get(0).contains("LOCATED_AT"));
        assertEquals(1, PlaceKnowledge.ofCategory(k().runtime(kenji.id()), KnowledgeCategory.MARKET).size());
    }

    @Test void unusedBeliefsFadeAndRumorsFadeFirst() {
        var fact = k().learn(ev(kenji, temple, Predicate.LOCATED_AT, null, LearnMethod.OBSERVATION, null)).record();
        var rumor = k().learn(ev(kenji, bridge, Predicate.LOCATED_AT, null, LearnMethod.RUMOR, hanako)).record();
        h.advance(1_500_000);
        k().tick(kenji.id(), h.now);
        assertNull(k().runtime(kenji.id()).get(rumor.id()), "a rumor nobody repeats is forgotten first");
        assertNotNull(k().runtime(kenji.id()).get(fact.id()));
        assertTrue(fact.confidence() > 0.7);
    }

    @Test void knowledgeIsPersistentAndKeepsItsProvenance(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) {
        var rec = k().learn(ev(kenji, yeremi, Predicate.ATTACKED, hanako, LearnMethod.RUMOR, hanako)).record();
        k().learn(ev(kenji, yeremi, Predicate.ATTACKED, hanako, LearnMethod.RUMOR, h.npc("B")));
        var storage = new yadi.samuraiai.ai.cognition.storage.CognitionStorage(dir, true);
        h.engine.useStorage(storage);
        h.engine.ensureLoaded(kenji.id(), h.now);
        storage.saveNpc(h.engine, kenji.id(), h.now, false);
        var second = new CognitionHarness();
        second.engine.useStorage(new yadi.samuraiai.ai.cognition.storage.CognitionStorage(dir, true));
        second.engine.ensureLoaded(kenji.id(), second.now);
        KnowledgeRecord back = second.engine.knowledge().runtime(kenji.id()).get(rec.id());
        assertNotNull(back);
        assertEquals(rec.state(), back.state());
        assertEquals(rec.confidence(), back.confidence(), 1e-9);
        assertEquals(rec.supporters(), back.supporters());
        assertEquals(rec.revisions().size(), back.revisions().size());
        assertEquals(rec.key(), back.key());
        assertEquals(hanako.id(), back.source().id());
    }
}
