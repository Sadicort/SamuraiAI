package yadi.samuraiai.living.core;

/**
 * One instant of Deiliora's official calendar, already broken down. {@code minute} is the absolute minute since the world's
 * epoch (the only value any engine stores); the rest is derived by the Calendar Engine and never computed elsewhere.
 *
 * @param dayIndex   absolute day number since the epoch (day 0 is the first day of year {@code epochYear})
 * @param month      1-based month of the year
 * @param day        1-based day of the month
 * @param dayOfYear  1-based day of the year
 * @param weekday    0-based day of the week
 */
public record CalendarDate(long minute, long dayIndex, int year, int month, String monthName, int day, int dayOfYear, int weekday, String weekdayName,
                           int hour, int minuteOfHour, Season season, int dayOfSeason, DayPhase phase) {

    /** "Año 102, 15 de Uzuki (primavera), 06:30". */
    public String describe() {
        return String.format("Año %d, %d de %s (%s), %02d:%02d", year, day, monthName, seasonName(season), hour, minuteOfHour);
    }

    public String shortDate() { return String.format("%d-%02d-%02d", year, month, day); }

    public static String seasonName(Season season) {
        return switch (season) { case SPRING -> "primavera"; case SUMMER -> "verano"; case AUTUMN -> "otoño"; case WINTER -> "invierno"; };
    }

    /** Minutes since midnight. */
    public int minuteOfDay() { return hour * 60 + minuteOfHour; }
}
