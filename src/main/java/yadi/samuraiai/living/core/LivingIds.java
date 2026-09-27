package yadi.samuraiai.living.core;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/** Stable identifiers: the same name always yields the same UUID, so records created twice (a reload, a replay) collide instead of duplicating. */
public final class LivingIds {
    private LivingIds() { }

    public static UUID named(String domain, String key) {
        return UUID.nameUUIDFromBytes(("living:" + domain + ":" + key.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
    }

    /** A lower-case key made only of letters, digits, '-' and '_' (safe for file names and command arguments). */
    public static String key(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.trim().toLowerCase(Locale.ROOT).toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') sb.append(c);
            else if (c == ' ' && sb.length() > 0 && sb.charAt(sb.length() - 1) != '_') sb.append('_');
        }
        return sb.toString();
    }

    public static String shortId(UUID id) { return id == null ? "-" : id.toString().substring(0, 8); }
}
