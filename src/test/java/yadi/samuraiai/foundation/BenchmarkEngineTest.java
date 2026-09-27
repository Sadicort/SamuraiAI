package yadi.samuraiai.foundation;

import org.junit.jupiter.api.Test;
import yadi.samuraiai.foundation.benchmark.BenchmarkEngine;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class BenchmarkEngineTest {
    @Test void recordsWarmupsSamplesPercentilesAndThreshold() {
        AtomicInteger calls=new AtomicInteger();
        var result=new BenchmarkEngine().run("fixture",2,8,1000,calls::incrementAndGet);
        assertEquals(10,calls.get());assertEquals(8,result.iterations());assertTrue(result.thresholdPassed());
        assertTrue(result.maximumMillis()>=result.p95Millis());assertTrue(result.p95Millis()>=result.p50Millis());
    }
    @Test void invalidBenchmarkCannotProduceGreenResult() {
        assertThrows(IllegalArgumentException.class,()->new BenchmarkEngine().run("",0,0,0,()->{}));
    }
}
