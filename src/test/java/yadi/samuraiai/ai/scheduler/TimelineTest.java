package yadi.samuraiai.ai.scheduler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.ai.scheduler.engine.SchedulerSettings;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;
import yadi.samuraiai.ai.scheduler.time.Timeline;
import yadi.samuraiai.ai.scheduler.time.WorldCalendar;
import yadi.samuraiai.ai.scheduler.util.Segments;

class TimelineTest {
    private final Timeline timeline = new Timeline(SchedulerSettings.defaults());

    @Test void everyTickOfTheDayBelongsToExactlyOnePeriodAndTheOrderIsTheConfiguredOne() {
        assertEquals(DayPeriod.MORNING, timeline.periodAt(1000));
        assertEquals(DayPeriod.MORNING, timeline.periodAt(5999));
        assertEquals(DayPeriod.AFTERNOON, timeline.periodAt(6000));
        assertEquals(DayPeriod.EVENING, timeline.periodAt(11500));
        assertEquals(DayPeriod.NIGHT, timeline.periodAt(14000));
        assertEquals(DayPeriod.LATE_NIGHT, timeline.periodAt(18000));
        assertEquals(DayPeriod.DAWN, timeline.periodAt(22500));
        assertEquals(DayPeriod.DAWN, timeline.periodAt(23999));
    }

    @Test void theDayWrapsSoTheStartOfTheDayStillBelongsToDawn() {
        assertEquals(DayPeriod.DAWN, timeline.periodAt(0));
        assertEquals(DayPeriod.DAWN, timeline.periodAt(999));
        assertEquals(DayPeriod.MORNING, timeline.periodAt(24000 + 1000));
        assertEquals(DayPeriod.LATE_NIGHT, timeline.periodAt(-5000), "negative world time wraps too");
    }

    @Test void boundariesInAnyOrderInTheConfigurationStillFormAConsistentDay() {
        SchedulerSettings odd = SchedulerSettings.builder().set("morningStart", 18000).set("lateNightStart", 1000).build();
        Timeline t = new Timeline(odd);
        DayPeriod previous = null;
        int changes = 0;
        for (int tick = 0; tick < 24000; tick += 10) {
            DayPeriod p = t.periodAt(tick);
            if (previous != null && p != previous) changes++;
            previous = p;
        }
        assertTrue(changes >= 5 && changes <= 7, "six periods around the day, saw " + changes + " changes");
    }

    @Test void ticksUntilChangeAndProgressAgreeWithTheBoundaries() {
        assertEquals(1000, timeline.ticksUntilChange(5000));
        assertEquals(1, timeline.ticksUntilChange(5999));
        assertEquals(0.0D, timeline.progress(1000), 1e-9);
        assertEquals(0.5D, timeline.progress(3500), 1e-9);
        assertEquals(1500, timeline.ticksUntilChange(23500), "dawn runs into the morning that starts at tick 1000 of the next day");
    }

    @Test void theCalendarSaysWhichEventsAreActiveAndTheirBias() {
        WorldCalendar calendar = WorldCalendar.parse(List.of());
        assertTrue(calendar.problems().isEmpty(), calendar.problems().toString());
        var market = calendar.activeAt(0, DayPeriod.MORNING);
        assertEquals(1, market.size());
        assertEquals("market_day", market.get(0).name());
        assertEquals(40.0D, calendar.bias(market, RoutineType.MERCHANT), 1e-9);
        assertTrue(calendar.activeAt(1, DayPeriod.MORNING).isEmpty(), "market day repeats every five days");
        assertEquals(1, calendar.activeAt(5, DayPeriod.AFTERNOON).size());
        assertTrue(calendar.activeAt(5, DayPeriod.NIGHT).isEmpty(), "only in the listed periods");
    }

    @Test void aBadCalendarLineCostsOneEntryAndIsReportedNotThrown() {
        WorldCalendar calendar = WorldCalendar.parse(List.of("fair;every=3;periods=NOON,MORNING;bias=SOCIAL:20,DANCING:5,WORK", "=broken", "ok;every=2;bias=WORK:5"));
        assertEquals(2, calendar.events().size());
        assertFalse(calendar.problems().isEmpty());
        assertTrue(calendar.problems().stream().anyMatch(p -> p.contains("NOON")));
        assertTrue(calendar.problems().stream().anyMatch(p -> p.contains("DANCING")));
        assertEquals(20.0D, calendar.bias(calendar.activeAt(0, DayPeriod.MORNING), RoutineType.SOCIAL), 1e-9);
    }

    @Test void theSegmentParserReadsNumbersListsAndWeightsAndReportsWhatItCannot() {
        Segments s = Segments.parse("id;n=4.5;list=a, b ,c;w=X:1.5,Y:oops,Z;bare").orElseThrow();
        assertEquals("id", s.id());
        assertEquals(4.5D, s.number("n", 0), 1e-9);
        assertEquals(List.of("a", "b", "c"), s.list("list"));
        assertEquals(java.util.Map.of("X", 1.5D), s.weights("w"));
        assertEquals(3, s.problems().size(), s.problems().toString());
        assertTrue(Segments.parse("k=v;x=1").isEmpty(), "an id is required");
    }
}
