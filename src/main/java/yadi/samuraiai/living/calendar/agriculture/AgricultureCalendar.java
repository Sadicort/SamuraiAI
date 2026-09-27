package yadi.samuraiai.living.calendar.agriculture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import yadi.samuraiai.ai.scheduler.util.Segments;
import yadi.samuraiai.living.core.WeatherKind;

/**
 * The agricultural calendar. Crops are data lines such as
 * {@code rice;plant=2,3;grow=4,5,6;harvest=7,8;rain=0.35;frost=1.5;storm=1.0}. Every day each tracked weather cell reports the
 * previous day's weather and its coldest temperature; each crop that is being planted or growing there records a good, dry,
 * frost or storm day. When the harvest begins the yield factor of that season is fixed and announced (great / normal / bad);
 * the Economy Engine multiplies farm output by {@link #productionFactor}. A resource with no crop line is not seasonal.
 */
public final class AgricultureCalendar {
    public enum Verdict { GREAT, NORMAL, BAD }

    public record Outlook(String cell, String crop, int year, double yieldFactor, Verdict verdict) { }

    public static final List<String> DEFAULT_LINES = List.of(
            "rice;plant=2,3;grow=4,5,6;harvest=7,8;rain=0.35;frost=1.5;storm=1.0",
            "wheat;plant=8,9;grow=10,11,12,1,2,3;harvest=4,5;rain=0.25;frost=0.4;storm=0.8",
            "herbs;plant=1;grow=2,3;harvest=4,5,6,7;rain=0.3;frost=1.0;storm=0.6");

    private final Map<String, CropCalendar> crops = new LinkedHashMap<>();
    private final Map<String, GrowingSeason> seasons = new LinkedHashMap<>();
    private final Map<String, Double> fixedYield = new LinkedHashMap<>();
    private final List<String> problems = new ArrayList<>();
    private final double greatThreshold, badThreshold;
    private boolean dirty;

    public AgricultureCalendar(List<String> lines, double greatThreshold, double badThreshold) {
        this.greatThreshold = greatThreshold; this.badThreshold = badThreshold;
        for (String line : DEFAULT_LINES) parse(line, null);
        if (lines != null) for (String line : lines) parse(line, problems);
    }

    private void parse(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("crop line '" + line + "' has no resource"); return; }
        Segments s = parsed.get();
        String id = s.id().toLowerCase(Locale.ROOT);
        if (s.text("remove", "false").equalsIgnoreCase("true")) { crops.remove(id); return; }
        crops.put(id, new CropCalendar(id, months(s.list("plant"), report), months(s.list("grow"), report), months(s.list("harvest"), report),
                s.number("rain", 0.3D), s.number("frost", 1.0D), s.number("storm", 0.8D)));
        if (report != null) s.problems().forEach(p -> report.add(id + ": " + p));
    }

    private static Set<Integer> months(List<String> values, List<String> report) {
        Set<Integer> out = new HashSet<>();
        for (String v : values) try { out.add(Integer.parseInt(v.trim())); } catch (NumberFormatException e) { if (report != null) report.add("bad month " + v); }
        return out;
    }

    public Collection<CropCalendar> crops() { return List.copyOf(crops.values()); }
    public Optional<CropCalendar> crop(String resource) { return Optional.ofNullable(crops.get(resource == null ? "" : resource.toLowerCase(Locale.ROOT))); }
    public List<String> problems() { return List.copyOf(problems); }

    public CropStage stage(String resource, int month) {
        CropCalendar c = crops.get(resource == null ? "" : resource.toLowerCase(Locale.ROOT));
        return c == null ? CropStage.NONE : c.stage(month);
    }

    private static String key(String cell, String crop) { return cell + "|" + crop; }

    /**
     * How much of a full day's output a producer of {@code resource} gets in this cell and month. Harvest months use the season's
     * yield factor; a non-crop resource returns 1.
     */
    public double productionFactor(String resource, String cell, int month) {
        CropStage stage = stage(resource, month);
        if (stage == CropStage.NONE) return 1.0D;
        if (stage != CropStage.HARVEST) return stage.output();
        Double fixed = fixedYield.get(key(cell, resource.toLowerCase(Locale.ROOT)));
        return fixed == null ? 1.0D : fixed;
    }

    /**
     * One day passed in a cell. {@code dominant} is yesterday's main weather, {@code wetMinutes} how long it rained or snowed,
     * {@code minTemperature} its coldest hour. Returns the harvest outlooks fixed today (the first day of a harvest).
     */
    public List<Outlook> onDay(String cell, int year, int month, WeatherKind dominant, long wetMinutes, double minTemperature) {
        List<Outlook> out = new ArrayList<>();
        for (CropCalendar c : crops.values()) {
            CropStage stage = c.stage(month);
            String k = key(cell, c.resource());
            GrowingSeason gs = seasons.computeIfAbsent(k, x -> new GrowingSeason(cell, c.resource(), year));
            if (stage == CropStage.PLANTING || stage == CropStage.GROWTH) {
                if (gs.announced()) gs.restart(year);  // a new growing season begins after last year's harvest
                boolean wet = wetMinutes >= 120;
                boolean storm = dominant == WeatherKind.STORM || dominant == WeatherKind.HAIL;
                boolean frost = minTemperature < 0.0D;
                boolean dry = !wet && dominant == WeatherKind.SUNNY && minTemperature > 18.0D;   // hot and rainless
                boolean good = !storm && !frost && (wet || dominant == WeatherKind.CLOUDY || (dominant == WeatherKind.SUNNY && !dry));
                gs.record(good, dry, frost, storm);
                dirty = true;
            } else if (stage == CropStage.HARVEST && !gs.announced()) {
                double factor = gs.yieldFactor(c);
                fixedYield.put(k, factor);
                gs.announced(true);
                dirty = true;
                Verdict verdict = factor >= greatThreshold ? Verdict.GREAT : factor <= badThreshold ? Verdict.BAD : Verdict.NORMAL;
                out.add(new Outlook(cell, c.resource(), year, factor, verdict));
            }
        }
        return out;
    }

    public Optional<GrowingSeason> season(String cell, String crop) { return Optional.ofNullable(seasons.get(key(cell, crop))); }
    public Collection<GrowingSeason> seasons() { return List.copyOf(seasons.values()); }
    public Map<String, Double> fixedYields() { return Map.copyOf(fixedYield); }

    public void restoreSeason(GrowingSeason gs) { seasons.put(key(gs.cell(), gs.crop()), gs); }
    public void restoreYield(String cell, String crop, double factor) { fixedYield.put(key(cell, crop), factor); }

    /** Forces the yield of the current harvest in a cell (a world event: fire in the fields, a blessing...). */
    public void overrideYield(String cell, String crop, double factor) { fixedYield.put(key(cell, crop.toLowerCase(Locale.ROOT)), Math.max(0.0D, Math.min(2.0D, factor))); dirty = true; }

    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }
    public void clear() { seasons.clear(); fixedYield.clear(); dirty = false; }
}
