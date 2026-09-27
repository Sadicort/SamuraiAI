package yadi.samuraiai.ai.cognition;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.cognition.model.EntityRef;
import yadi.samuraiai.ai.cognition.model.ExperienceKind;
import yadi.samuraiai.ai.cognition.storage.CognitionStorage;
import yadi.samuraiai.ai.cognition.testkit.CognitionHarness;
import yadi.samuraiai.ai.emotion.model.Activity;

/** Cost of the cognitive layer with hundreds of NPCs, measured (generous bounds: they catch an order-of-magnitude regression, not noise). */
class CognitionPerformanceTest {
    @Test void hundredsOfNpcsLiveTheirMindsAtAnAffordableCostAndTheGraphStaysQueryable(@org.junit.jupiter.api.io.TempDir Path dir) {
        CognitionHarness h = new CognitionHarness();
        h.engine.useStorage(new CognitionStorage(dir, true));
        List<EntityRef> npcs = new ArrayList<>();
        for (int i = 0; i < 300; i++) { EntityRef n = h.npc("N" + i); npcs.add(n); h.engine.ensureLoaded(n.id(), h.now); }
        EntityRef player = h.player("P");
        ExperienceKind[] kinds = {ExperienceKind.CONVERSATION, ExperienceKind.HELPED_ME, ExperienceKind.ATTACKED_ME, ExperienceKind.PATROLLED, ExperienceKind.WITNESSED_ATTACK, ExperienceKind.DISCOVERED_PLACE};
        long started = System.nanoTime();
        int experiences = 0;
        for (int round = 0; round < 6; round++) {
            for (int i = 0; i < npcs.size(); i++) {
                EntityRef npc = npcs.get(i);
                EntityRef other = (i + round) % 3 == 0 ? player : npcs.get((i + 1 + round) % npcs.size());
                h.live(h.input(npc, kinds[(i + round) % kinds.length]).actor(other).target(npcs.get((i + 5) % npcs.size())).place(h.at("z" + (i % 12), i % 300, (i * 7) % 300)));
                experiences++;
            }
            h.advance(1500);
        }
        double perExperienceMicros = (System.nanoTime() - started) / 1000.0 / experiences;
        assertTrue(perExperienceMicros < 3000, "per experience (all engines): " + perExperienceMicros + " µs");

        started = System.nanoTime();
        int minds = 0;
        for (int step = 0; step < 40; step++) {
            h.advance(100);
            for (EntityRef n : npcs) { h.engine.tick(n.id(), h.now, Activity.NONE); minds++; }
        }
        double perMindMicros = (System.nanoTime() - started) / 1000.0 / minds;
        assertTrue(perMindMicros < 400, "per NPC mind tick: " + perMindMicros + " µs");
        System.out.printf("cognition perf: %.0f µs per experience, %.1f µs per mind tick (300 NPCs, %d experiences)%n", perExperienceMicros, perMindMicros, experiences);

        long queryStart = System.nanoTime();
        int known = 0;
        for (int i = 0; i < 300; i++) known += h.engine.relationships().graph().whoKnows(npcs.get(i).id()).size();
        assertTrue((System.nanoTime() - queryStart) / 1000.0 / 300 < 300);
        assertTrue(h.engine.relationships().graph().whoKnows(player.id()).size() > 50);
        assertTrue(known > 0);

        started = System.nanoTime();
        int files = h.engine.saveAll(h.now);
        double saveMillis = (System.nanoTime() - started) / 1e6;
        assertTrue(files >= 900, "files " + files);
        System.out.printf("cognition persistence: %d files in %.0f ms; %d KB on disk%n", files, saveMillis, h.engine.storage().totalBytes() / 1024);
        assertTrue(saveMillis < 20000);
    }
}
