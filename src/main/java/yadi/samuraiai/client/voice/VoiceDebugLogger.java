package yadi.samuraiai.client.voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoiceDebugLogger {
    public static final Logger LOG = LoggerFactory.getLogger("SamuraiAI/Voice");
    public static void debug(String message, Object... args) { if (VoiceConfig.get().debug()) LOG.info(message, args); }
    public static void info(String message, Object... args) { LOG.info(message, args); }
    public static void warn(String message, Object... args) { LOG.warn(message, args); }
    private VoiceDebugLogger() {}
}
