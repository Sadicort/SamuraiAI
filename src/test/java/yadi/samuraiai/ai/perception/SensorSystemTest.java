package yadi.samuraiai.ai.perception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.perception.awareness.AwarenessLevel;
import yadi.samuraiai.ai.perception.engine.BlockInterest;
import yadi.samuraiai.ai.perception.engine.EntityClass;
import yadi.samuraiai.ai.perception.engine.PerceptionSettings;
import yadi.samuraiai.ai.perception.engine.PerceptionState;
import yadi.samuraiai.ai.perception.engine.Reports;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.environment.*;
import yadi.samuraiai.ai.perception.events.SuspicionRaisedEvent;
import yadi.samuraiai.ai.perception.filters.FilterContext;
import yadi.samuraiai.ai.perception.filters.StimulusPipeline;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.memory.PerceptionMemory;
import yadi.samuraiai.ai.perception.sensors.*;
import yadi.samuraiai.ai.perception.smell.SmellSensor;
import yadi.samuraiai.ai.perception.stimuli.Stimulus;
import yadi.samuraiai.ai.perception.stimuli.StimulusCategory;
import yadi.samuraiai.ai.perception.stimuli.StimulusType;
import yadi.samuraiai.ai.perception.testkit.PerceptionHarness;
import yadi.samuraiai.ai.perception.touch.TouchSensor;

class SensorSystemTest {

    @Test void sensorsScanOnTheirOwnIntervalsAndRareOnesAlmostNever() {
        var h = new PerceptionHarness();
        h.player(0.5D, 10.5D);
        h.run(60);
        var scans = h.metrics.snapshot().sensorScans();
        assertTrue(scans.get(SensorType.VISION) >= 18 && scans.get(SensorType.VISION) <= 22, "vision every 3 ticks: " + scans.get(SensorType.VISION));
        assertTrue(scans.get(SensorType.WEATHER) <= 1, "weather every 100 ticks: " + scans.get(SensorType.WEATHER));
        assertTrue(scans.get(SensorType.BLOCK) <= 4, "blocks every 20 ticks: " + scans.get(SensorType.BLOCK));
        assertTrue(scans.get(SensorType.DAMAGE) >= 58, "event-driven sensors check every tick, cheaply");
    }

    @Test void farNpcsScanLessOftenAndAlertOnesMoreOften() {
        var near = new PerceptionHarness();
        near.player(0.5D, 10.5D);
        near.run(120);
        var far = new PerceptionHarness();
        far.tierMultiplier = 4;
        far.player(0.5D, 10.5D);
        far.run(120);
        long nearScans = near.metrics.snapshot().sensorScans().get(SensorType.VISION), farScans = far.metrics.snapshot().sensorScans().get(SensorType.VISION);
        assertTrue(farScans * 3 < nearScans * 2, "far " + farScans + " near " + nearScans);
        var scheduler = new SensorScheduler();
        var s = PerceptionSettings.defaults();
        assertTrue(scheduler.interval(SensorType.VISION, s, 1, AwarenessLevel.ALERT) < scheduler.interval(SensorType.VISION, s, 1, AwarenessLevel.UNAWARE));
    }

    @Test void aFailingSensorIsParkedThenRetriedAndTheOthersKeepWorking() {
        int[] calls = {0};
        Sensor flaky = new Sensor() {
            @Override public SensorType type() { return SensorType.LIGHT; }
            @Override public void scan(SensorContext ctx) { if (calls[0]++ < 2) throw new IllegalStateException("boom"); }
        };
        var h = new PerceptionHarness(List.of(new EntitySensor(), new VisionSensor(), flaky));
        var player = h.player(0.5D, 10.5D);
        h.run(15);
        var record = h.state.record(SensorType.LIGHT);
        assertEquals(SensorState.FAILED, record.state, "parked after failing");
        assertEquals(1, record.failures);
        assertTrue(h.state.tracks.get(player.id()).state.seen(), "vision is unaffected by another sensor failing");
        h.run(200);
        assertTrue(calls[0] >= 2 && record.failures == 2 || record.scans > 0, "retried after the cooldown");
        assertEquals(2, h.metrics.snapshot().sensorFailures());
        h.run(400);
        assertTrue(record.scans > 0, "eventually scans normally");
        assertNotEquals(SensorState.FAILED, record.state);
    }

    @Test void theStimulusPipelineMergesDuplicatesAndCoolsDownRepeats() {
        var h = new PerceptionHarness();
        var pipeline = new StimulusPipeline();
        var source = UUID.randomUUID();
        var a = Stimulus.at(StimulusType.AUDIO, StimulusCategory.FOOTSTEP, source, "step", 0, 64, 6, 0.6D, 100, 10);
        var b = Stimulus.at(StimulusType.AUDIO, StimulusCategory.FOOTSTEP, source, "step", 0, 64, 6, 0.9D, 100, 10);
        var ctx = new FilterContext(h.perceiver, 100, h.settings, h.state.cooldowns, new PerceptionMemory());
        var first = pipeline.process(List.of(a, b), ctx);
        assertEquals(1, first.accepted().size());
        assertEquals(1, first.duplicatesMerged());
        assertEquals(0.9D, first.accepted().get(0).intensity(), 1.0E-9D, "the stronger duplicate wins");
        var again = pipeline.process(List.of(a), new FilterContext(h.perceiver, 104, h.settings, h.state.cooldowns, new PerceptionMemory()));
        assertTrue(again.accepted().isEmpty(), "within the cooldown");
        assertEquals(1, again.rejectedByFilter().get("cooldown"));
        var later = pipeline.process(List.of(a), new FilterContext(h.perceiver, 130, h.settings, h.state.cooldowns, new PerceptionMemory()));
        assertEquals(1, later.accepted().size());
    }

    @Test void relationChangesPriorityFriendsFadeEnemiesStandOut() {
        var h = new PerceptionHarness();
        var friend = UUID.randomUUID();
        var enemy = UUID.randomUUID();
        var perceiver = h.perceiver.withRelation(id -> id.equals(friend) ? 90.0D : id.equals(enemy) ? -80.0D : Double.NaN);
        var pipeline = new StimulusPipeline();
        var ctx = new FilterContext(perceiver, 100, h.settings, h.state.cooldowns, new PerceptionMemory());
        var f = Stimulus.at(StimulusType.VISUAL, StimulusCategory.PLAYER, friend, "friend", 0, 64, 5, 0.9D, 100, 10);
        var e = Stimulus.at(StimulusType.VISUAL, StimulusCategory.PLAYER, enemy, "enemy", 0, 64, 5, 0.9D, 100, 10);
        var stranger = Stimulus.at(StimulusType.VISUAL, StimulusCategory.PLAYER, UUID.randomUUID(), "stranger", 0, 64, 5, 0.9D, 100, 10);
        var result = pipeline.process(List.of(f, e, stranger), ctx).accepted();
        double pf = result.stream().filter(x -> x.label().equals("friend")).findFirst().orElseThrow().priority();
        double pe = result.stream().filter(x -> x.label().equals("enemy")).findFirst().orElseThrow().priority();
        double ps = result.stream().filter(x -> x.label().equals("stranger")).findFirst().orElseThrow().priority();
        assertTrue(pf < ps && ps < pe, "friend " + pf + " stranger " + ps + " enemy " + pe);
    }

    @Test void theBlockSensorNoticesADoorOpeningAndAFireStartingAndRemembersThem() {
        var h = new PerceptionHarness(b -> b.set("blockScanCellsPerScan", 5000));
        h.world.interest(1, 65, 3, BlockInterest.DOOR_CLOSED);
        h.run(25);
        assertTrue(h.state.blockMemory.containsValue(BlockInterest.DOOR_CLOSED), "first sight of a door is only scenery");
        assertEquals(0.0D, h.state.suspicion.value(), 1.0E-9D);
        h.world.interest(1, 65, 3, BlockInterest.DOOR_OPEN);
        h.state.markBlockDirty(1, 65, 3);
        h.run(22);
        assertTrue(h.state.suspicion.value() >= 10.0D, "an opened door is suspicious: " + h.state.suspicion.value());
        assertTrue(h.state.memory.entries(MemoryKind.ENVIRONMENTAL).stream().anyMatch(e -> e.label.contains("door opened")));
        h.world.interest(-2, 65, -2, BlockInterest.FIRE);
        h.run(45);
        assertTrue(h.state.memory.entries(MemoryKind.DANGER).stream().anyMatch(e -> e.label.equals("fire")), "a fire is a danger");
        assertTrue(h.snapshot.threatened() || h.snapshot.threats().stream().anyMatch(t -> t.category() == StimulusCategory.FIRE));
    }

    @Test void blockScanningIsIncrementalAndStillCoversTheWholeNeighbourhood() {
        var h = new PerceptionHarness(b -> b.set("blockScanCellsPerScan", 400).set("blockInterval", 1));
        h.world.interest(6, 66, 7, BlockInterest.LAVA);
        h.run(3);
        assertFalse(h.state.blockMemory.containsValue(BlockInterest.LAVA), "a far corner is not reached in the first slices");
        h.run(30);
        assertTrue(h.state.blockMemory.containsValue(BlockInterest.LAVA), "but the cursor gets there");
    }

    @Test void lightWeatherAndEnvironmentChangesAreReported() {
        var h = new PerceptionHarness(b -> b.set("lightInterval", 2).set("weatherInterval", 2).set("environmentInterval", 2));
        h.run(6);
        assertTrue(h.state.environmentKnown);
        h.world.defaultLight = 0;
        boolean sawLight = false;
        for (int i = 0; i < 6; i++) sawLight |= h.step().stimuliAccepted() > 0;
        assertTrue(sawLight, "darkness is a stimulus");
        assertTrue(h.snapshot.environment().dark());
        h.world.environment = new EnvironmentSnapshot("test:world", "plains", 64, 0.8D, WeatherState.STORM, 6000L, 15, false, false, true);
        boolean sawWeather = false;
        for (int i = 0; i < 6; i++) sawWeather |= h.step().stimuliAccepted() > 0;
        assertTrue(sawWeather);
        assertEquals(WeatherState.STORM, h.snapshot.environment().weather());
        h.world.environment = new EnvironmentSnapshot("test:world", "desert", 64, 2.0D, WeatherState.STORM, 6000L, 15, false, false, true);
        long rejectedBefore = h.metrics.snapshot().stimuliRejected();
        h.run(6);
        assertEquals("desert", h.snapshot.environment().biome(), "the environment snapshot follows the world");
        assertTrue(h.metrics.snapshot().stimuliRejected() > rejectedBefore, "a biome change is too minor to matter: the priority filter drops it as noise");
    }

    @Test void touchNoticesSomethingBehindTheNpcThatVisionCannotSee() {
        var h = new PerceptionHarness();
        var behind = h.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.HOSTILE, "Creeper", 0.5D, 64.0D, -0.7D));
        h.run(4);
        assertNotNull(h.snapshot.focus());
        assertEquals(behind.id(), h.snapshot.focus().subject());
        assertTrue(h.snapshot.focus().category() == StimulusCategory.CONTACT || h.snapshot.focus().category() == StimulusCategory.HOSTILE);
    }

    @Test void smellCarriesSmokeThroughWallsAndScentFromBehind() {
        var h = new PerceptionHarness(b -> b.set("blockScanCellsPerScan", 5000).set("smellInterval", 2));
        h.world.solidWall(-3, 60, 3, 8, 70, 3);
        h.world.interest(2, 65, 6, BlockInterest.FIRE);
        h.run(30);
        assertTrue(h.state.cooldowns.keySet().stream().anyMatch(k -> k.contains("SMELL:SCENT")), "smoke smelled even though a wall hides the fire");
        var sniffer = new PerceptionHarness(b -> b.set("smellInterval", 1));
        sniffer.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.PLAYER, "Stalker", 0.5D, 64.0D, -2.5D));
        sniffer.run(6);
        assertTrue(sniffer.state.cooldowns.keySet().stream().anyMatch(k -> k.startsWith("SMELL:SCENT")), "the scent of someone right behind");
    }

    @Test void queuedConversationAndVoiceBecomeStimuliAndSocialMemory() {
        var h = new PerceptionHarness();
        var speaker = UUID.randomUUID();
        h.state.pendingConversation.add(new Reports.ConversationReport(speaker, "Alice", 1.0D, 64.0D, 2.0D, 40, h.tick + 1));
        h.state.pendingVoice.add(new Reports.VoiceReport(UUID.randomUUID(), "Bob", -1.0D, 64.0D, 3.0D, 0.8D, h.tick + 1));
        h.run(3);
        assertTrue(h.state.memory.entries(MemoryKind.SOCIAL).stream().anyMatch(e -> speaker.equals(e.subject)));
        assertEquals(2, h.state.memory.size(MemoryKind.SOCIAL));
        assertTrue(h.state.stimuliAccepted >= 2);
    }

    @Test void theSnapshotIsImmutableAndDescribesTheNpcsBeliefNotTheWorldsTruth() {
        var h = new PerceptionHarness();
        var hidden = h.player(0.5D, 8.5D);
        h.world.solidWall(-3, 64, 4, 3, 67, 4);
        h.run(12);
        assertTrue(h.snapshot.seenTargets().isEmpty(), "the NPC does not know about the player behind the wall");
        assertThrows(UnsupportedOperationException.class, () -> h.snapshot.targets().add(null));
        assertThrows(UnsupportedOperationException.class, () -> h.snapshot.threats().add(null));
        assertNotNull(hidden);
    }

    @Test void awarenessMapContainsOnlyWhatWasPerceived() {
        var h = new PerceptionHarness();
        var seen = h.player(0.5D, 10.5D);
        h.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.PLAYER, "Unseen", 0.5D, 64.0D, 40.5D));
        h.run(10);
        assertEquals(1, h.snapshot.map().players().size());
        assertEquals(seen.id(), h.snapshot.map().players().get(0).subject());
        assertEquals(10.5D, h.snapshot.map().players().get(0).z(), 0.01D);
    }

    @Test void perceptionStateResetClearsEverything() {
        var h = new PerceptionHarness();
        h.player(0.5D, 10.5D);
        h.run(10);
        assertFalse(h.state.tracks.isEmpty());
        h.state.reset();
        assertTrue(h.state.tracks.isEmpty() && h.state.memory.size() == 0 && h.state.awareness.level() == AwarenessLevel.UNAWARE);
    }

    @Test void blockPositionPackingRoundTripsIncludingNegativeCoordinates() {
        for (int[] p : new int[][]{{0, 0, 0}, {-1, -60, -1}, {12345, 200, -98765}, {-30000000, -64, 29999999}}) {
            long packed = PerceptionState.pack(p[0], p[1], p[2]);
            assertEquals(p[0], PerceptionState.unpackX(packed));
            assertEquals(p[1], PerceptionState.unpackY(packed));
            assertEquals(p[2], PerceptionState.unpackZ(packed));
        }
    }

    @Test void manyNpcsAreObservedWithinTheSharedRayBudget() {
        var h = new PerceptionHarness();
        for (int i = 0; i < 40; i++) h.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.ANIMAL, "Cow" + i, -8 + (i % 8) * 2.0D, 64.0D, 6.0D + (i / 8) * 3.0D));
        h.rayBudget = 30;
        h.run(30);
        var m = h.metrics.snapshot();
        assertTrue(m.raysRefused() > 0, "the small budget was the limit");
        assertTrue(m.raycasts() <= 30 * 30, "never more than the budget per pass");
        List<Integer> seenCounts = new ArrayList<>();
        h.rayBudget = 1000;
        h.run(12);
        seenCounts.add((int) h.state.tracks.values().stream().filter(t -> t.state.seen()).count());
        assertTrue(seenCounts.get(0) > 10, "once budget is available every animal in view is tracked: " + seenCounts);
        assertNotNull(new SuspicionRaisedEvent(h.npcId, 1, "x"));
    }
}
