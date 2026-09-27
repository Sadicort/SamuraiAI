package yadi.samuraiai.prompt.section;

import yadi.samuraiai.context.AIContext;

import java.util.List;

/**
 * What the NPC can currently see around it, straight from the perception
 * layer. Deliberately limited to what was actually perceived, so the model
 * cannot have an NPC react to someone hiding out of sight.
 */
public class SituationSection implements PromptSection {

    /** Enough for context without turning the prompt into a roll call. */
    private static final int MAX_LISTED = 5;

    @Override
    public String build(AIContext context) {

        List<String> nearby = context.getNearby();
        String time = context.getTimeOfDay();

        boolean hasTime = time != null && !time.isBlank();

        if (nearby.isEmpty() && !hasTime) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("------------------------\n");
        sb.append("TU ALREDEDOR\n\n");

        if (hasTime) {
            sb.append("Es ").append(time).append(".\n");
        }

        if (nearby.isEmpty()) {
            sb.append("No ves a nadie mas cerca.\n");
            return sb.toString();
        }

        List<String> shown = nearby.size() <= MAX_LISTED
                ? nearby
                : nearby.subList(0, MAX_LISTED);

        sb.append("Cerca de ti puedes ver a: ").append(String.join(", ", shown));

        if (nearby.size() > MAX_LISTED) {
            sb.append(" y ").append(nearby.size() - MAX_LISTED).append(" mas");
        }

        sb.append(".\n");

        return sb.toString();
    }
}
