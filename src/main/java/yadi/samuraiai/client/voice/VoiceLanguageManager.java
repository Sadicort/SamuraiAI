package yadi.samuraiai.client.voice;

import java.util.Locale;

public final class VoiceLanguageManager {
    public enum Language { AUTO, ES, EN, JA, KO, ZH, PT, FR, DE, IT, RU }
    private VoiceLanguageManager() {}
    public static Language parse(String value) {
        if (value == null) return Language.AUTO;
        try { return Language.valueOf(value.trim().toUpperCase(Locale.ROOT).replace('-', '_')); }
        catch (IllegalArgumentException error) { return Language.AUTO; }
    }
    public static String tag(Language language) { return language == null || language == Language.AUTO ? "auto" : language.name().toLowerCase(Locale.ROOT); }
}
