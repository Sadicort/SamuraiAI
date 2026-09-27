package yadi.samuraiai.living.calendar.holidays;

import java.util.UUID;

/**
 * A date worth remembering every year: a village's founding, a victory or defeat, a family event, a birth, something a player
 * did. {@code subject} names what it belongs to ({@code village:<id>}, {@code family:<id>}, {@code npc:<uuid>}...).
 */
public record AnniversaryRecord(UUID id, String kind, String subject, String title, long originMinute, int month, int day, int originYear) {
    public AnniversaryRecord {
        kind = kind == null || kind.isBlank() ? "CUSTOM" : kind;
        subject = subject == null ? "" : subject;
        title = title == null ? "" : title;
    }
}
