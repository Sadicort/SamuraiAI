package yadi.samuraiai.living.village.schedules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A culture's base day: blocks of {@code HH:MM ROUTINE}, e.g. {@code village;blocks=04:30 WAKE,05:00 PRAYER,06:00 WORK,...}.
 * It is only the starting point: professions, the season, events and each citizen's own rhythm change it.
 */
public record ScheduleTemplate(String culture, List<Block> blocks) {
    /** A block begins at {@code minute} of the day and lasts until the next block. {@code routine} is a scheduler routine name. */
    public record Block(int minute, String routine) { }

    public ScheduleTemplate {
        List<Block> sorted = new ArrayList<>(blocks);
        sorted.sort((a, b) -> Integer.compare(a.minute(), b.minute()));
        blocks = List.copyOf(sorted);
    }

    public static ScheduleTemplate parse(String culture, List<String> items, List<String> problems) {
        List<Block> blocks = new ArrayList<>();
        for (String item : items) {
            String[] p = item.trim().split("\\s+");
            if (p.length < 2) { problems.add(culture + ": block '" + item + "' is not 'HH:MM ROUTINE'"); continue; }
            String[] t = p[0].split(":");
            try {
                int minute = Integer.parseInt(t[0]) * 60 + (t.length > 1 ? Integer.parseInt(t[1]) : 0);
                blocks.add(new Block(Math.floorMod(minute, 1440), p[1].toUpperCase(Locale.ROOT)));
            } catch (NumberFormatException e) { problems.add(culture + ": bad time in '" + item + "'"); }
        }
        return new ScheduleTemplate(culture, blocks);
    }
}
