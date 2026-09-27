package yadi.samuraiai.living.calendar.agriculture;

/**
 * How one crop's growing season is going in one weather cell: good days (the weather it wanted), dry days, frost days and
 * storm days since planting. The harvest's yield factor comes from these counts, so a bad harvest is the weather's doing,
 * not a dice roll at harvest time.
 */
public final class GrowingSeason {
    private final String cell, crop;
    private int year;
    private int goodDays, dryDays, frostDays, stormDays, days;
    private boolean announced;

    public GrowingSeason(String cell, String crop, int year) { this.cell = cell; this.crop = crop; this.year = year; }

    public String cell() { return cell; }
    public String crop() { return crop; }
    public int year() { return year; }
    public int goodDays() { return goodDays; }
    public int dryDays() { return dryDays; }
    public int frostDays() { return frostDays; }
    public int stormDays() { return stormDays; }
    public int days() { return days; }
    public boolean announced() { return announced; }
    public void announced(boolean v) { announced = v; }

    void restart(int newYear) { year = newYear; goodDays = dryDays = frostDays = stormDays = days = 0; announced = false; }

    void record(boolean good, boolean dry, boolean frost, boolean storm) {
        days++;
        if (good) goodDays++;
        if (dry) dryDays++;
        if (frost) frostDays++;
        if (storm) stormDays++;
    }

    public void restore(int y, int good, int dry, int frost, int storm, int total, boolean wasAnnounced) {
        year = y; goodDays = good; dryDays = dry; frostDays = frost; stormDays = storm; days = total; announced = wasAnnounced;
    }

    /** 0.2 (ruined) .. 1.6 (exceptional); 1.0 for a season with nothing recorded. */
    public double yieldFactor(CropCalendar c) {
        if (days == 0) return 1.0D;
        double good = goodDays / (double) days, dry = dryDays / (double) days, frost = frostDays / (double) days, storm = stormDays / (double) days;
        double factor = 0.85D + 0.6D * good - 0.8D * dry - c.frostDamage() * frost - c.stormDamage() * storm;
        return Math.max(0.2D, Math.min(1.6D, factor));
    }
}
