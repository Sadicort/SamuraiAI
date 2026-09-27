package yadi.samuraiai.living.village.schedules;

import java.util.List;
import java.util.Set;

/**
 * One citizen's plan for one day, already adjusted for profession, season, events and personal rhythm: blocks of routine,
 * each lasting until the next (the last runs past midnight into the first). {@link #label()} lists why it differs from the
 * culture's template.
 */
public record DayPlan(long dayIndex, List<ScheduleTemplate.Block> blocks, List<String> adjustments) {
    public static final Set<String> WORKING = Set.of("WORK", "MERCHANT", "GUARD", "PATROL", "TRAINING");

    public DayPlan {
        blocks = List.copyOf(blocks);
        adjustments = List.copyOf(adjustments);
    }

    /** The routine planned at a minute of the day. */
    public String routineAt(int minuteOfDay) {
        if (blocks.isEmpty()) return "REST";
        String found = blocks.get(blocks.size() - 1).routine();
        for (ScheduleTemplate.Block b : blocks) if (b.minute() <= minuteOfDay) found = b.routine();
        return found;
    }

    /** Minutes of each routine between two minutes of the day (from < to; used to turn a plan into hours of work). */
    public double minutesOf(Set<String> routines, int from, int to) {
        if (blocks.isEmpty() || to <= from) return 0;
        double total = 0;
        for (int i = 0; i < blocks.size(); i++) {
            int start = blocks.get(i).minute();
            int end = i + 1 < blocks.size() ? blocks.get(i + 1).minute() : 1440 + blocks.get(0).minute();
            if (!routines.contains(blocks.get(i).routine())) continue;
            total += overlap(start, end, from, to) + overlap(start - 1440, end - 1440, from, to);
        }
        return total;
    }

    private static double overlap(int a1, int a2, int b1, int b2) { return Math.max(0, Math.min(a2, b2) - Math.max(a1, b1)); }

    public double workHours() { return minutesOf(WORKING, 0, 1440) / 60.0D; }

    /** Why this plan differs from the template (the cache signature, which starts with '#', is left out). */
    public String label() {
        List<String> shown = adjustments.stream().filter(a -> !a.startsWith("#")).toList();
        return shown.isEmpty() ? "plantilla" : String.join(", ", shown);
    }
}
