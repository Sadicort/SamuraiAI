package yadi.samuraiai.ai.scheduler.lifestyle;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import yadi.samuraiai.ai.scheduler.personality.PersonalityTraits;
import yadi.samuraiai.ai.scheduler.personality.Trait;
import yadi.samuraiai.ai.scheduler.routine.RoutineType;
import yadi.samuraiai.ai.scheduler.time.DayPeriod;
import yadi.samuraiai.ai.scheduler.util.Segments;

/**
 * The lifestyles available in the world, parsed from lines such as
 * {@code guard;types=guard;group=PATROL;shifts=0,12000;traits=discipline:75,loyalty:80;MORNING=GUARD:60,PATROL:50;NIGHT=SLEEP:60}.
 * With no configured lines the built-in catalogue below applies; it is ordinary data in the same format, so a server owner
 * can copy any line into the configuration and change it. The lifestyle with {@code types=*} is the fallback.
 */
public final class LifestyleCatalog {
    public static final List<String> DEFAULT_LINES = List.of(
            "samurai;types=samurai,ronin;group=SQUAD;traits=discipline:80,courage:75,loyalty:70,diligence:65,patience:60,spirituality:55,caution:50,aggression:45,sociability:40,curiosity:45;"
                    + "DAWN=WAKE:60,MEDITATE:45,PRAYER:30,TRAINING:20;MORNING=TRAINING:55,PATROL:45,EAT:30,WORK:10;"
                    + "AFTERNOON=PATROL:50,GUARD:30,TRAINING:30,EAT:30,SOCIAL:20;EVENING=SOCIAL:35,EAT:40,MEDITATE:35,PATROL:30;"
                    + "NIGHT=SLEEP:45,MEDITATE:30,REST:30,PATROL:15;LATE_NIGHT=SLEEP:80,REST:20",
            "guard;types=guard;group=PATROL;shifts=0,12000;traits=discipline:75,loyalty:80,caution:70,courage:60,diligence:65,patience:60,aggression:45,sociability:40,curiosity:40,spirituality:35;"
                    + "DAWN=WAKE:50,GUARD:40,PATROL:35;MORNING=GUARD:60,PATROL:50,EAT:25;AFTERNOON=GUARD:55,PATROL:45,EAT:30,REST:10;"
                    + "EVENING=PATROL:50,GUARD:40,EAT:30,SOCIAL:20;NIGHT=SLEEP:60,REST:25,EAT:15;LATE_NIGHT=SLEEP:85",
            "merchant;types=merchant,trader;group=MERCHANT;traits=sociability:75,diligence:70,patience:65,curiosity:55,discipline:55,caution:50,loyalty:50,courage:40,spirituality:40,aggression:20;"
                    + "DAWN=WAKE:60,EAT:30;MORNING=MERCHANT:70,WORK:30,EAT:20;AFTERNOON=MERCHANT:65,SOCIAL:35,EAT:30;"
                    + "EVENING=SOCIAL:45,EAT:45,PRAYER:20,MERCHANT:20;NIGHT=SLEEP:60,REST:25;LATE_NIGHT=SLEEP:85",
            "monk;types=monk,priest;group=VILLAGE;traits=spirituality:90,patience:80,discipline:70,loyalty:60,caution:50,sociability:50,courage:50,diligence:55,curiosity:50,aggression:10;"
                    + "DAWN=WAKE:50,PRAYER:60,MEDITATE:55;MORNING=MEDITATE:50,WORK:40,PRAYER:35,EAT:25;AFTERNOON=WORK:45,MEDITATE:40,SOCIAL:30,EAT:25;"
                    + "EVENING=PRAYER:55,MEDITATE:45,EAT:35;NIGHT=SLEEP:60,MEDITATE:30;LATE_NIGHT=SLEEP:85",
            "villager;types=*;group=VILLAGE;traits=sociability:55,diligence:55,patience:50,curiosity:50,caution:55,courage:45,discipline:50,loyalty:50,spirituality:45,aggression:25;"
                    + "DAWN=WAKE:60,EAT:30;MORNING=WORK:60,EAT:20,SOCIAL:20;AFTERNOON=WORK:55,SOCIAL:30,EAT:30;"
                    + "EVENING=SOCIAL:45,EAT:45,PRAYER:20,REST:20;NIGHT=SLEEP:60,REST:25;LATE_NIGHT=SLEEP:85");

    private final Map<String, Lifestyle> byId = new LinkedHashMap<>();
    private final Map<String, Lifestyle> byType = new LinkedHashMap<>();
    private Lifestyle fallback;
    private final List<String> problems = new ArrayList<>();

    public LifestyleCatalog(List<String> lines) {
        List<String> source = lines == null || lines.isEmpty() ? DEFAULT_LINES : lines;
        for (String line : source) parse(line);
        if (fallback == null) {
            // A configuration without a "*" lifestyle still needs a fallback: use a neutral one so no NPC is ever unscheduled.
            for (String line : DEFAULT_LINES) if (line.startsWith("villager;")) parse(line);
        }
    }

    private void parse(String line) {
        var parsed = Segments.parse(line);
        if (parsed.isEmpty()) { problems.add("lifestyle line '" + line + "' has no id"); return; }
        Segments seg = parsed.get();
        Set<String> types = new HashSet<>();
        for (String type : seg.list("types")) types.add(type.toLowerCase(Locale.ROOT));
        if (types.isEmpty()) types.add(seg.id().toLowerCase(Locale.ROOT));
        Map<Trait, Double> traits = new EnumMap<>(Trait.class);
        seg.weights("traits").forEach((name, value) -> Trait.parse(name).ifPresentOrElse(t -> traits.put(t, value), () -> problems.add(seg.id() + ": unknown trait " + name)));
        Map<DayPeriod, Map<RoutineType, Double>> weights = new EnumMap<>(DayPeriod.class);
        for (DayPeriod period : DayPeriod.values()) {
            if (!seg.has(period.name())) continue;
            Map<RoutineType, Double> routines = new EnumMap<>(RoutineType.class);
            seg.weights(period.name()).forEach((name, value) -> {
                try { routines.put(RoutineType.valueOf(name), value); } catch (IllegalArgumentException e) { problems.add(seg.id() + ": unknown routine " + name); }
            });
            weights.put(period, routines);
        }
        List<Integer> shifts = new ArrayList<>();
        for (String s : seg.list("shifts")) try { shifts.add(Integer.parseInt(s)); } catch (NumberFormatException e) { problems.add(seg.id() + ": bad shift " + s); }
        Lifestyle lifestyle = new Lifestyle(seg.id().toLowerCase(Locale.ROOT), types, PersonalityTraits.of(traits), weights, shifts, seg.text("group", "VILLAGE").toUpperCase(Locale.ROOT));
        seg.problems().forEach(p -> problems.add(seg.id() + ": " + p));
        byId.put(lifestyle.id(), lifestyle);
        for (String type : types) {
            if (type.equals("*")) fallback = lifestyle;
            else byType.put(type, lifestyle);
        }
    }

    /** The lifestyle for an NPC type, or the fallback one. Never null. */
    public Lifestyle forType(String typeId) {
        Lifestyle found = typeId == null ? null : byType.get(typeId.toLowerCase(Locale.ROOT));
        return found != null ? found : fallback;
    }

    public List<Lifestyle> all() { return List.copyOf(byId.values()); }
    public List<String> problems() { return List.copyOf(problems); }
}
