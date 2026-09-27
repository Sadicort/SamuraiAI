package yadi.samuraiai.living.calendar.clock;

import yadi.samuraiai.living.core.Season;

/** One month of the Deiliora calendar: its name, its length in days and the season it belongs to. */
public record MonthDef(String name, int days, Season season) {
    public MonthDef {
        name = name == null || name.isBlank() ? "?" : name.trim();
        days = Math.max(1, Math.min(400, days));
        season = season == null ? Season.SPRING : season;
    }
}
