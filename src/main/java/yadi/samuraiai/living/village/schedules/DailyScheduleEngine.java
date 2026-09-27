package yadi.samuraiai.living.village.schedules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import yadi.samuraiai.ai.scheduler.util.Segments;

/**
 * The Daily Schedule Engine: builds each citizen's {@link DayPlan} from the culture's template and the four modifiers the
 * specification names, so no two villagers keep an identical day:
 * <ul>
 *   <li>profession — lines such as {@code fisherman;shift=-60} or {@code monk;swap=WORK>PRAYER,MERCHANT>MEDITATE}; guards on the
 *       night watch have their whole day rotated by {@code nightShift};</li>
 *   <li>season — morning blocks follow the sunrise (later in winter, earlier in summer);</li>
 *   <li>events — festivals move the evening social block earlier and bedtime later;</li>
 *   <li>personality — each citizen's own offset (an early riser, a late sleeper).</li>
 * </ul>
 * Plans are cached per citizen and day.
 */
public final class DailyScheduleEngine {
    /** What shapes one citizen's day. */
    public record Context(String culture, String profession, boolean nightShift, int sunriseShift, boolean festive, int personalOffset) { }

    public static final List<String> DEFAULT_TEMPLATES = List.of(
            "village;blocks=04:30 WAKE,05:00 PRAYER,06:00 WORK,12:00 EAT,13:00 WORK,17:00 MERCHANT,18:30 SOCIAL,20:00 EAT,21:30 SLEEP",
            "temple;blocks=04:00 WAKE,04:30 PRAYER,06:00 MEDITATE,07:00 WORK,11:30 EAT,12:30 WORK,16:00 PRAYER,17:30 MEDITATE,19:00 EAT,20:30 SLEEP",
            "market;blocks=06:00 WAKE,06:30 EAT,07:00 MERCHANT,12:30 EAT,13:30 MERCHANT,18:00 SOCIAL,20:00 EAT,22:00 SLEEP",
            "guard;blocks=05:00 WAKE,05:30 TRAINING,07:00 GUARD,12:00 EAT,13:00 PATROL,18:00 EAT,19:00 SOCIAL,21:00 SLEEP");
    public static final List<String> DEFAULT_PROFESSIONS = List.of(
            "farmer;shift=-30",
            "fisherman;shift=-60",
            "cook;shift=-30;swap=MERCHANT>WORK",
            "merchant;swap=WORK>MERCHANT",
            "monk;swap=WORK>PRAYER,MERCHANT>MEDITATE;shift=-30",
            "guard;swap=WORK>GUARD,MERCHANT>PATROL,PRAYER>TRAINING",
            "samurai;swap=WORK>TRAINING,MERCHANT>PATROL,PRAYER>MEDITATE",
            "woodcutter;shift=-30", "miner;shift=-15", "hunter;shift=-60;swap=MERCHANT>WORK",
            "blacksmith;shift=15", "carpenter;shift=0", "herbalist;shift=-30", "weaver;shift=15", "healer;swap=MERCHANT>PRAYER");

    private record ProfessionRule(int shift, Map<String, String> swap) { }

    private final Map<String, ScheduleTemplate> templates = new LinkedHashMap<>();
    private final Map<String, ProfessionRule> rules = new LinkedHashMap<>();
    private final Map<UUID, DayPlan> cache = new HashMap<>();
    private final List<String> problems = new ArrayList<>();
    private int nightShiftMinutes = 720;
    private long plansBuilt;

    public DailyScheduleEngine(List<String> templateLines, List<String> professionLines, int nightShiftMinutes) {
        this.nightShiftMinutes = nightShiftMinutes;
        for (String l : DEFAULT_TEMPLATES) template(l, null);
        if (templateLines != null) for (String l : templateLines) template(l, problems);
        for (String l : DEFAULT_PROFESSIONS) rule(l, null);
        if (professionLines != null) for (String l : professionLines) rule(l, problems);
    }

    private void template(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("schedule line '" + line + "' has no culture"); return; }
        Segments s = parsed.get();
        List<String> sink = report == null ? new ArrayList<>() : report;
        templates.put(s.id().toLowerCase(Locale.ROOT), ScheduleTemplate.parse(s.id().toLowerCase(Locale.ROOT), s.list("blocks"), sink));
    }

    private void rule(String line, List<String> report) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { if (report != null) report.add("profession schedule line '" + line + "' has no profession"); return; }
        Segments s = parsed.get();
        Map<String, String> swap = new LinkedHashMap<>();
        for (String item : s.list("swap")) {
            String[] p = item.split(">");
            if (p.length == 2) swap.put(p[0].trim().toUpperCase(Locale.ROOT), p[1].trim().toUpperCase(Locale.ROOT));
            else if (report != null) report.add(s.id() + ": swap '" + item + "' is not FROM>TO");
        }
        rules.put(s.id().toLowerCase(Locale.ROOT), new ProfessionRule(s.integer("shift", 0), swap));
    }

    public ScheduleTemplate template(String culture) {
        ScheduleTemplate t = templates.get(culture == null ? "" : culture.toLowerCase(Locale.ROOT));
        return t != null ? t : templates.get("village");
    }

    public List<String> problems() { return List.copyOf(problems); }
    public long plansBuilt() { return plansBuilt; }
    public void forget(UUID citizen) { cache.remove(citizen); }
    public void invalidateAll() { cache.clear(); }

    /** The citizen's plan for a day (cached; a changed context builds a new one). */
    public DayPlan plan(UUID citizen, long dayIndex, Context ctx) {
        DayPlan cached = cache.get(citizen);
        if (cached != null && cached.dayIndex() == dayIndex && cached.adjustments().contains(signature(ctx))) return cached;
        DayPlan plan = build(dayIndex, ctx);
        cache.put(citizen, plan);
        plansBuilt++;
        return plan;
    }

    private static String signature(Context c) { return "#" + c.culture() + "/" + c.profession() + "/" + c.nightShift() + "/" + c.sunriseShift() + "/" + c.festive() + "/" + c.personalOffset(); }

    DayPlan build(long dayIndex, Context ctx) {
        ScheduleTemplate t = template(ctx.culture());
        ProfessionRule rule = rules.getOrDefault(ctx.profession() == null ? "" : ctx.profession().toLowerCase(Locale.ROOT), new ProfessionRule(0, Map.of()));
        List<String> why = new ArrayList<>();
        List<ScheduleTemplate.Block> out = new ArrayList<>();
        int seasonShift = Math.max(-60, Math.min(90, ctx.sunriseShift() / 2));
        for (ScheduleTemplate.Block b : t.blocks()) {
            String routine = rule.swap().getOrDefault(b.routine(), b.routine());
            int minute = b.minute() + rule.shift() + ctx.personalOffset();
            if (b.minute() < 12 * 60) minute += seasonShift;                          // the morning follows the sun
            if (ctx.festive() && routine.equals("SOCIAL")) minute -= 60;              // the festival starts earlier...
            if (ctx.festive() && routine.equals("SLEEP")) minute += 60;               // ...and ends later
            if (ctx.nightShift()) minute += nightShiftMinutes;
            out.add(new ScheduleTemplate.Block(Math.floorMod(minute, 1440), routine));
        }
        out.sort((a, b) -> Integer.compare(a.minute(), b.minute()));
        if (!ctx.profession().isEmpty() && (rule.shift() != 0 || !rule.swap().isEmpty())) why.add("oficio " + ctx.profession());
        if (seasonShift != 0) why.add(String.format("sol %+d min", seasonShift));
        if (ctx.festive()) why.add("festividad");
        if (ctx.nightShift()) why.add("turno de noche");
        if (ctx.personalOffset() != 0) why.add(String.format("ritmo propio %+d min", ctx.personalOffset()));
        why.add(signature(ctx));
        return new DayPlan(dayIndex, out, why);
    }
}
