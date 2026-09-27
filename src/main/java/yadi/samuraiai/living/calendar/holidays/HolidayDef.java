package yadi.samuraiai.living.calendar.holidays;

import java.util.List;

/** A fixed-date holiday (New Year, the days of the ancestors...). {@code tags} lets other engines react (Family reads {@code ancestors}). */
public record HolidayDef(String id, String name, int month, int day, int days, String kind, List<String> tags) {
    public HolidayDef {
        days = Math.max(1, days);
        kind = kind == null || kind.isBlank() ? "CELEBRATION" : kind;
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
