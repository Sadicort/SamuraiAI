package yadi.samuraiai.ai.scheduler.routine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import yadi.samuraiai.ai.scheduler.interrupt.InterruptPolicy;
import yadi.samuraiai.ai.scheduler.util.Segments;

/**
 * The table of {@link RoutineProfile}s: built-in defaults overridden by configured lines such as
 * {@code SLEEP;minTicks=3000;maxTicks=9600;policy=SUSPEND;energy=0.03;fatigue=-0.03}.
 */
public final class RoutineProfiles {
    private final Map<RoutineType, RoutineProfile> table = new EnumMap<>(RoutineType.class);
    private final List<String> problems = new ArrayList<>();

    public RoutineProfiles(List<String> overrides) {
        //                              type                  min   max  policy                    energy  fatigue focus   stress  motiv  cooldown social
        put(RoutineType.WAKE,      60,   200, InterruptPolicy.CANCEL,   0.010, -0.005, 0.020,  0.000, 0.010, 2400, 0.0);
        put(RoutineType.PATROL,  1200,  4800, InterruptPolicy.PAUSE,   -0.003,  0.004, -0.002, -0.002, -0.001,   0, 0.0);
        put(RoutineType.WORK,    2400,  7200, InterruptPolicy.PAUSE,   -0.004,  0.005, -0.003,  0.001, 0.001,    0, 0.1);
        put(RoutineType.REST,     600,  2400, InterruptPolicy.RESUME,   0.020, -0.020, 0.010, -0.020, 0.003,  600, 0.0);
        put(RoutineType.EAT,      400,  1200, InterruptPolicy.RESTART,  0.050, -0.004, 0.005, -0.015, 0.008, 6000, 0.3);
        put(RoutineType.MEDITATE, 600,  2400, InterruptPolicy.CANCEL,   0.004, -0.010, 0.040, -0.050, 0.005, 3000, 0.0);
        put(RoutineType.SLEEP,   3000,  9600, InterruptPolicy.SUSPEND,  0.030, -0.030, 0.020, -0.030, 0.005, 6000, 0.0);
        put(RoutineType.SOCIAL,   600,  2400, InterruptPolicy.PAUSE,   -0.001,  0.001, 0.000, -0.020, 0.012,  600, 1.0);
        put(RoutineType.TRAINING,1200,  3600, InterruptPolicy.PAUSE,   -0.008,  0.008, 0.003, -0.010, 0.005, 1200, 0.2);
        put(RoutineType.PRAYER,   400,  1200, InterruptPolicy.CANCEL,   0.000, -0.005, 0.030, -0.040, 0.005, 3600, 0.0);
        put(RoutineType.GUARD,   2400,  9600, InterruptPolicy.PAUSE,   -0.003,  0.003, -0.002, 0.0005, -0.0005,  0, 0.1);
        put(RoutineType.MERCHANT,2400,  7200, InterruptPolicy.PAUSE,   -0.002,  0.002, -0.001, 0.001, 0.002,     0, 0.6);
        for (String line : overrides == null ? List.<String>of() : overrides) apply(line);
    }

    private void put(RoutineType type, int min, int max, InterruptPolicy policy, double energy, double fatigue, double focus, double stress,
                     double motivation, int cooldown, double social) {
        table.put(type, new RoutineProfile(type, min, max, policy, energy, fatigue, focus, stress, motivation, cooldown, social));
    }

    private void apply(String line) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { problems.add("routine override '" + line + "' has no routine name"); return; }
        Segments seg = parsed.get();
        RoutineType type;
        try { type = RoutineType.valueOf(seg.id().toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { problems.add("unknown routine " + seg.id()); return; }
        RoutineProfile base = table.get(type);
        InterruptPolicy policy = base.policy();
        if (seg.has("policy")) {
            try { policy = InterruptPolicy.valueOf(seg.text("policy", "").toUpperCase(Locale.ROOT)); } catch (IllegalArgumentException e) { problems.add(type + ": unknown policy"); }
        }
        table.put(type, new RoutineProfile(type, seg.integer("minTicks", base.minTicks()), seg.integer("maxTicks", base.maxTicks()), policy,
                seg.number("energy", base.energy()), seg.number("fatigue", base.fatigue()), seg.number("focus", base.focus()),
                seg.number("stress", base.stress()), seg.number("motivation", base.motivation()), seg.integer("cooldownTicks", base.cooldownTicks()),
                seg.number("socialLoad", base.socialLoad())));
        seg.problems().forEach(p -> problems.add(type + ": " + p));
    }

    public RoutineProfile of(RoutineType type) { return table.get(type); }
    public List<String> problems() { return List.copyOf(problems); }
}
