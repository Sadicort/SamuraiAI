package yadi.samuraiai.ai.memory.temporal;

import yadi.samuraiai.ai.cognition.model.Stamp;

/** How long ago something was, in the terms a character would use ("ayer", "hace tres días"). Prepared for dialogue. */
public record RelativeTime(Bucket bucket, long days) {
    public enum Bucket { JUST_NOW, EARLIER_TODAY, YESTERDAY, FEW_DAYS, LAST_WEEK, WEEKS, LONG_AGO }

    public static RelativeTime between(long then, long now) {
        long delta = Math.max(0, now - then);
        long days = Stamp.TICKS_PER_DAY == 0 ? 0 : delta / Stamp.TICKS_PER_DAY;
        long dayThen = then / Stamp.TICKS_PER_DAY, dayNow = now / Stamp.TICKS_PER_DAY;
        long calendarDays = dayNow - dayThen;
        Bucket bucket;
        if (delta < 1200) bucket = Bucket.JUST_NOW;
        else if (calendarDays <= 0) bucket = Bucket.EARLIER_TODAY;
        else if (calendarDays == 1) bucket = Bucket.YESTERDAY;
        else if (calendarDays <= 4) bucket = Bucket.FEW_DAYS;
        else if (calendarDays <= 9) bucket = Bucket.LAST_WEEK;
        else if (calendarDays <= 40) bucket = Bucket.WEEKS;
        else bucket = Bucket.LONG_AGO;
        return new RelativeTime(bucket, calendarDays);
    }

    /** A Spanish phrase for dialogue and the debugger. */
    public String phrase() {
        return switch (bucket) {
            case JUST_NOW -> "hace un momento";
            case EARLIER_TODAY -> "hoy";
            case YESTERDAY -> "ayer";
            case FEW_DAYS -> "hace " + spell(days) + " días";
            case LAST_WEEK -> "hace más o menos una semana";
            case WEEKS -> "hace " + Math.max(2, days / 7) + " semanas";
            case LONG_AGO -> "hace mucho tiempo";
        };
    }

    private static String spell(long n) { return switch ((int) n) { case 2 -> "dos"; case 3 -> "tres"; case 4 -> "cuatro"; default -> Long.toString(n); }; }
}
