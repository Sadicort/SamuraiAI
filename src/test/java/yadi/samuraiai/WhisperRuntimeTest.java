package yadi.samuraiai;

import io.github.ggerganov.whispercpp.WhisperCpp;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class WhisperRuntimeTest {
    @Test
    void nativeRuntimeLoadsVerifiedModelWhenConfigured() {
        String model = System.getProperty("samuraiai.whisper.model", "");
        Assumptions.assumeTrue(!model.isBlank(), "Smoke test requiere -Dsamuraiai.whisper.model");
        assertDoesNotThrow(() -> {
            try (WhisperCpp whisper = new WhisperCpp()) { whisper.initContext(model); }
        });
    }
}
