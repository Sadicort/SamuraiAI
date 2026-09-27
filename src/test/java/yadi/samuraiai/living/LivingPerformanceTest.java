package yadi.samuraiai.living;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.living.core.Provenance;
import yadi.samuraiai.living.sim.LivingWorld;
import yadi.samuraiai.living.world.settlements.Settlement;
import yadi.samuraiai.living.world.settlements.SettlementType;
import yadi.samuraiai.living.world.streaming.StreamingEngine;

/**
 * The living world at real speed with a realistic load: ten planned villages of twenty citizens each along a road, one player
 * standing in the first village, ten Deiliora days of server ticks (one game tick per tick, 240 000 ticks). It prints what a
 * tick and a day cost so the performance document can quote measured numbers, and fails only if the average tick exceeds a
 * generous bound (a tick has 50 ms; the living world must stay far below one millisecond on average).
 */
class LivingPerformanceTest {
    private static final String OW = "minecraft:overworld";

    @Test void tenVillagesOfTwentyLiveTenDaysWithinBudget() {
        LivingWorld lw = new LivingWorld(LivingWorld.Settings.defaults(), e -> { }, 2026L);
        List<UUID> villages = new ArrayList<>();
        for (int v = 0; v < 10; v++) {
            Settlement s = lw.foundSettlement("Mura " + v, SettlementType.VILLAGE, OW, v * 700.0, 64, 0, 64, true, false, Provenance.of("test", "", "carga", 0));
            villages.add(s.id());
            lw.economy().donate(s.id(), "rice", 1500, 0.8, Provenance.of("farm", s.id().toString(), "reservas", 0));
            for (int c = 0; c < 20; c++) lw.npcArrived(UUID.randomUUID(), "Aldeano" + v + "_" + c, c % 5 == 0 ? "guard" : "villager", OW, v * 700.0 + (c % 5) * 4, 64, (c / 5) * 4.0, 0);
        }
        List<StreamingEngine.Viewer> player = List.of(new StreamingEngine.Viewer(OW, 5, 5));
        long gameTime = 1000;
        int ticks = 24000 * 10;
        long started = System.nanoTime();
        long[] worst = new long[5];
        int[] worstAt = new int[5];
        for (int i = 0; i < ticks; i++) {
            long t0 = System.nanoTime();
            lw.tick(++gameTime, gameTime, true, player);
            long dt = System.nanoTime() - t0;
            int slot = 0;
            for (int k = 1; k < worst.length; k++) if (worst[k] < worst[slot]) slot = k;
            if (i >= 24000 && dt > worst[slot]) { worst[slot] = dt; worstAt[slot] = i; }   // steady state: after the first day (class loading, JIT)
        }
        StringBuilder spikes = new StringBuilder();
        for (int k = 0; k < worst.length; k++) spikes.append(String.format(" [tick %d (día %d, minuto del día %d): %.1f ms]", worstAt[k], worstAt[k] / 24000, (worstAt[k] % 24000) * 1440 / 24000, worst[k] / 1e6));
        System.out.println("LIVING_PERF_SPIKES" + spikes);
        var mm = lw.metrics();
        System.out.printf("LIVING_PERF_DAY_STAGES worldEvents=%.2fms families=%.2fms trade=%.2fms questConditions=%.2fms maxStage=%.2fms (per day)%n",
                mm.dayStageNanos[0].get() / 1e6 / Math.max(1, mm.days.get()), mm.dayStageNanos[1].get() / 1e6 / Math.max(1, mm.days.get()),
                mm.dayStageNanos[2].get() / 1e6 / Math.max(1, mm.days.get()), mm.dayStageNanos[3].get() / 1e6 / Math.max(1, mm.days.get()), mm.maxDayStageNanos.get() / 1e6);
        double wallSeconds = (System.nanoTime() - started) / 1e9;
        var m = lw.metrics();
        double avg = m.micros(m.tickNanos), max = m.maxTickNanos.get() / 1000.0, dayMs = m.dayNanos.get() / 1e6 / Math.max(1, m.days.get());
        System.out.printf("LIVING_PERF ticks=%d wall=%.1fs avgTick=%.1fus maxTick=%.0fus calendar=%.2fus world=%.2fus villages=%.2fus economy=%.2fus quests=%.2fus days=%d avgDay=%.2fms events=%d reactions=%d citizens=%d people=%d regions=%d caravans=%d quests=%d%n",
                m.ticks.get(), wallSeconds, avg, max, m.micros(m.calendarNanos), m.micros(m.worldNanos), m.micros(m.villageNanos), m.micros(m.economyNanos), m.micros(m.questNanos),
                m.days.get(), dayMs, m.events.get(), m.reactions.get(), lw.villages().citizens().size(), lw.families().people().size(), lw.world().regionCount(),
                lw.economy().caravans().size(), lw.quests().quests().size());
        assertTrue(m.days.get() >= 9, "ten days went by: " + m.days.get());
        assertEquals(200, lw.villages().citizens().size());
        assertEquals(0, m.reactionFailures.get(), "no reaction failed: " + m.lastError);
        assertTrue(avg < 1000, "average tick " + avg + " µs");
    }
}
