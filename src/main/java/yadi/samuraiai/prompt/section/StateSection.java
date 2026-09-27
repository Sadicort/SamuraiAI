package yadi.samuraiai.prompt.section;

import yadi.samuraiai.context.AIContext;
import yadi.samuraiai.emotion.Emotion;
import yadi.samuraiai.npc.relationship.Relationship;

import java.util.List;
import java.util.Map;

/**
 * How the NPC feels right now and what it thinks of this particular player.
 *
 * <p>Emotions and relationship scores are translated into words rather than
 * numbers: a model told "estas asustado" acts scared, while one told
 * "FEAR: 72" tends to talk about the number.
 */
public class StateSection implements PromptSection {

    /** Below this an emotion is background noise not worth prompting about. */
    private static final int NOTABLE_THRESHOLD = 30;

    /** Emotions above this are strong enough to dominate the NPC's tone. */
    private static final int INTENSE_THRESHOLD = 65;

    @Override
    public String build(AIContext context) {

        String mood = describeMood(context.getEmotions());
        String bond = describeBond(context.getRelationship(), context.getPlayerName());

        if (mood.isEmpty() && bond.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("------------------------\n");
        sb.append("COMO TE SIENTES\n\n");

        if (!mood.isEmpty()) {
            sb.append(mood).append("\n");
        }

        if (!bond.isEmpty()) {
            sb.append(bond).append("\n");
        }

        return sb.toString();
    }

    private static String describeMood(Map<Emotion, Integer> emotions) {

        List<String> notable = emotions.entrySet().stream()
                .filter(entry -> entry.getKey() != Emotion.CALM)
                .filter(entry -> entry.getValue() >= NOTABLE_THRESHOLD)
                .sorted(Map.Entry.<Emotion, Integer>comparingByValue().reversed())
                .limit(3)
                .map(entry -> entry.getValue() >= INTENSE_THRESHOLD
                        ? "muy " + entry.getKey().adjective()
                        : entry.getKey().adjective())
                .toList();

        if (notable.isEmpty()) {
            return "Estas tranquilo y en paz.";
        }

        return "Te sientes " + join(notable) + ".";
    }

    private static String describeBond(Relationship relationship, String playerName) {

        if (relationship == null || relationship.isNeutral()) {
            return "Apenas conoces a " + playerName + ".";
        }

        return "Sobre " + playerName + ": " + relationship.describe() + ".";
    }

    private static String join(List<String> parts) {

        if (parts.size() == 1) {
            return parts.get(0);
        }

        return String.join(", ", parts.subList(0, parts.size() - 1))
                + " y " + parts.get(parts.size() - 1);
    }
}
