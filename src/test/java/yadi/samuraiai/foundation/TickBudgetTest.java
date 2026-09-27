package yadi.samuraiai.foundation;

import org.junit.jupiter.api.Test;
import yadi.samuraiai.foundation.scheduler.TickBudget;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class TickBudgetTest {
    @Test void taskLimitDefersExcessWork() {
        var budget=new TickBudget(3,Duration.ofSeconds(1));
        assertTrue(budget.tryAcquire());assertTrue(budget.tryAcquire());assertTrue(budget.tryAcquire());
        assertFalse(budget.tryAcquire());assertEquals(3,budget.snapshot().executedTasks());assertTrue(budget.snapshot().exhausted());
    }
    @Test void timeLimitStopsWorkAndInvalidLimitsFailClosed() throws Exception {
        var budget=new TickBudget(100,Duration.ofMillis(1));Thread.sleep(5);
        assertFalse(budget.tryAcquire());assertTrue(budget.snapshot().exhausted());
        assertThrows(IllegalArgumentException.class,()->new TickBudget(0,Duration.ofMillis(1)));
    }
}
