package yadi.samuraiai.ai.perception;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.perception.engine.EntityClass;
import yadi.samuraiai.ai.perception.engine.SensedEntity;
import yadi.samuraiai.ai.perception.events.SoundHeardEvent;
import yadi.samuraiai.ai.perception.events.SuspicionRaisedEvent;
import yadi.samuraiai.ai.perception.hearing.*;
import yadi.samuraiai.ai.perception.memory.MemoryKind;
import yadi.samuraiai.ai.perception.testkit.PerceptionHarness;

class HearingTest {
    private final PerceptionHarness h = new PerceptionHarness();

    private SoundEvent sound(SoundCategory category, double x, double z, double loudness) {
        return new SoundEvent(UUID.randomUUID(), category, null, x, 64.0D, z, loudness, h.tick + 1, 40, category.name());
    }

    @Test void aLoudExplosionIsHeardFarAwayWithItsDirection() {
        h.sound(sound(SoundCategory.EXPLOSION, 0.5D, 50.5D, 1.0D));
        h.run(3);
        var heard = h.events(SoundHeardEvent.class);
        assertEquals(1, heard.size());
        assertEquals("FRONT", heard.get(0).direction());
        assertTrue(heard.get(0).distance() > 45.0D);
    }

    @Test void aQuietSoundIsNotHeardFromFarAway() {
        h.sound(sound(SoundCategory.BLOCK_PLACE, 0.5D, 40.5D, 0.6D));
        h.run(3);
        assertFalse(h.saw(SoundHeardEvent.class), "each sound has its own radius");
    }

    @Test void directionIsRelativeToWhereTheListenerFaces() {
        h.sound(sound(SoundCategory.DOOR, -6.5D, 0.5D, 1.0D));   // west: to the right of someone facing south (+z)
        h.sound(sound(SoundCategory.IMPACT, 7.5D, 0.5D, 1.0D));   // east: left
        h.sound(sound(SoundCategory.BLOCK_BREAK, 0.5D, -7.5D, 1.0D)); // north: behind
        h.run(3);
        var directions = h.events(SoundHeardEvent.class).stream().map(SoundHeardEvent::direction).toList();
        assertTrue(directions.contains("RIGHT"), directions.toString());
        assertTrue(directions.contains("LEFT"), directions.toString());
        assertTrue(directions.contains("BACK"), directions.toString());
        h.clearEvents();
        h.face(90.0F);
        h.sound(new SoundEvent(UUID.randomUUID(), SoundCategory.DOOR, null, -6.5D, 64.0D, 0.5D, 1.0D, h.tick + 1, 40, "door"));
        h.run(3);
        assertEquals("FRONT", h.events(SoundHeardEvent.class).get(0).direction(), "facing west, a sound to the west is in front");
    }

    @Test void localisationIsLessPreciseTheFartherTheSound() {
        h.sound(new SoundEvent(UUID.randomUUID(), SoundCategory.DOOR, UUID.randomUUID(), 0.5D, 64.0D, 3.5D, 1.0D, h.tick + 1, 40, "near"));
        h.sound(new SoundEvent(UUID.randomUUID(), SoundCategory.DOOR, UUID.randomUUID(), 0.5D, 64.0D, 9.5D, 1.0D, h.tick + 1, 40, "far"));
        h.run(3);
        var events = h.events(SoundHeardEvent.class);
        double near = events.stream().filter(e -> e.distance() < 5).findFirst().orElseThrow().uncertainty();
        double far = events.stream().filter(e -> e.distance() > 8).findFirst().orElseThrow().uncertainty();
        assertTrue(far > near * 2.0D, "far " + far + " near " + near);
    }

    @Test void theSameSoundAndListenerAlwaysGiveTheSameEstimate() {
        var id = UUID.randomUUID();
        var event = new SoundEvent(id, SoundCategory.BLOCK_BREAK, null, 0.5D, 64.0D, 9.5D, 1.0D, h.tick + 1, 40, "break");
        h.sound(event);
        h.run(3);
        var first = h.events(SoundHeardEvent.class).get(0);
        var second = new PerceptionHarness();
        second.perceiver = yadi.samuraiai.ai.perception.engine.Perceiver.simple(h.npcId, 0.5D, 64.0D, 0.5D, 0.0F);
        second.state = new yadi.samuraiai.ai.perception.engine.PerceptionState(h.npcId);
        second.sound(new SoundEvent(id, SoundCategory.BLOCK_BREAK, null, 0.5D, 64.0D, 9.5D, 1.0D, second.tick + 1, 40, "break"));
        second.run(3);
        var again = second.events(SoundHeardEvent.class).get(0);
        assertEquals(first.x(), again.x(), 1.0E-9D);
        assertEquals(first.z(), again.z(), 1.0E-9D);
    }

    @Test void wallsDampenASoundWithoutSilencingIt() {
        var open = new PerceptionHarness();
        open.sound(new SoundEvent(UUID.randomUUID(), SoundCategory.BLOCK_BREAK, null, 0.5D, 64.0D, 9.5D, 1.0D, open.tick + 1, 40, "x"));
        open.run(3);
        h.world.solidWall(-3, 63, 5, 3, 68, 5);
        h.sound(new SoundEvent(UUID.randomUUID(), SoundCategory.BLOCK_BREAK, null, 0.5D, 64.0D, 9.5D, 1.0D, h.tick + 1, 40, "x"));
        h.run(3);
        double clear = open.events(SoundHeardEvent.class).get(0).intensity();
        double muffled = h.events(SoundHeardEvent.class).get(0).intensity();
        assertTrue(muffled < clear && muffled > clear * 0.5D, "muffled " + muffled + " clear " + clear);
    }

    @Test void footstepsDependOnGaitAndSneakingIsSilent() {
        var walker = h.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.PLAYER, "Walker", 0.5D, 64.0D, 5.5D).moving(0.0D, 0.1D, false));
        h.run(6);
        assertFalse(h.saw(SoundHeardEvent.class), "a walk is quiet at 5 blocks");
        h.world.replace(walker.moving(0.0D, 0.28D, true));
        h.run(6);
        assertTrue(h.saw(SoundHeardEvent.class), "a sprint carries farther");
        var quiet = new PerceptionHarness();
        quiet.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.PLAYER, "Sneaker", 0.5D, 64.0D, 5.5D).moving(0.0D, 0.28D, true).sneaking(true));
        quiet.run(6);
        assertFalse(quiet.saw(SoundHeardEvent.class), "a sneaking entity makes no footsteps");
    }

    @Test void aHeardSoundIsRememberedWithItsUncertaintyAndFadesAway() {
        h.sound(new SoundEvent(UUID.randomUUID(), SoundCategory.EXPLOSION, null, 0.5D, 64.0D, 30.5D, 1.0D, h.tick + 1, 40, "boom"));
        h.run(3);
        var memory = h.state.memory.strongest(MemoryKind.AUDITORY).orElseThrow();
        assertTrue(memory.uncertainty > 0.0D);
        assertEquals(30.5D, memory.z, memory.uncertainty + 0.5D);
        h.run(600);
        assertTrue(h.state.memory.entries(MemoryKind.AUDITORY).isEmpty(), "auditory memory fades out");
    }

    @Test void anUnexplainedSoundRaisesSuspicionButAVisibleSourceDoesNot() {
        h.sound(sound(SoundCategory.BLOCK_BREAK, 0.5D, 4.5D, 1.0D));
        h.run(3);
        assertTrue(h.state.suspicion.value() > 5.0D, "value " + h.state.suspicion.value());
        var explained = new PerceptionHarness();
        var walker = explained.world.add(SensedEntity.at(UUID.randomUUID(), EntityClass.PLAYER, "Sprinter", 0.5D, 64.0D, 6.5D).moving(0.0D, 0.3D, true));
        explained.run(8);
        assertTrue(explained.state.tracks.get(walker.id()).state.seen());
        assertTrue(explained.state.suspicion.value() < 3.0D, "seeing the source explains its footsteps: " + explained.state.suspicion.value());
    }

    @Test void theSoundLogIsSharedAndBoundedByAge() {
        var log = new SoundLog();
        for (int i = 0; i < 10; i++) log.add(SoundEvent.of(SoundCategory.DOOR, null, 0, 64, 0, 1.0D, i));
        assertEquals(10, log.since(-1).size());
        assertEquals(4, log.since(5).size());
        assertEquals(6, log.prune(20, 14));
        assertEquals(4, log.size());
    }

    @Test void aSoundFromTheNpcItselfIsIgnored() {
        h.sound(new SoundEvent(UUID.randomUUID(), SoundCategory.DOOR, h.perceiver.entityId(), 0.5D, 64.0D, 1.5D, 1.0D, h.tick + 1, 40, "own"));
        h.run(3);
        assertFalse(h.saw(SoundHeardEvent.class));
        assertFalse(h.saw(SuspicionRaisedEvent.class));
    }
}
