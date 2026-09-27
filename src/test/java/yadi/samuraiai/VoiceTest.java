package yadi.samuraiai;

import org.junit.jupiter.api.Test;
import yadi.samuraiai.client.voice.*;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class VoiceTest {
    @Test void languageParserSupportsConfiguredLanguages() {
        assertEquals(VoiceLanguageManager.Language.ES, VoiceLanguageManager.parse("es"));
        assertEquals(VoiceLanguageManager.Language.JA, VoiceLanguageManager.parse("ja"));
        assertEquals(VoiceLanguageManager.Language.AUTO, VoiceLanguageManager.parse("invalid"));
    }
    @Test void embeddedEngineFailsSafelyUntilRuntimeIsAvailable() throws Exception {
        var manager = new yadi.samuraiai.client.voice.core.VoiceEngineManager();
        var result = manager.engine().recognize(new byte[] {1, 2}, VoiceLanguageManager.Language.ES).get(1, TimeUnit.SECONDS);
        assertFalse(result.success()); assertTrue(result.error().contains("Whisper"));
    }
}
