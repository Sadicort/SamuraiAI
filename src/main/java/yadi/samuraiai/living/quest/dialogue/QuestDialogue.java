package yadi.samuraiai.living.quest.dialogue;

import java.util.Locale;
import yadi.samuraiai.living.quest.runtime.Quest;

/**
 * What a quest giver says, coloured by how they feel (their dominant emotion, from the cognitive layer) and how much they
 * trust the player (0..100). An angry giver is curt, a frightened one pleads, a happy one is warm; a giver who does not trust
 * the player keeps some distance or refuses to ask at all. The same lines feed the chat and the Ollama prompt.
 */
public final class QuestDialogue {
    private QuestDialogue() { }

    public static String offer(Quest q, String mood, double trust) {
        String m = mood == null ? "" : mood.toUpperCase(Locale.ROOT);
        String opening = switch (m) {
            case "ANGRY" -> "No tengo tiempo para rodeos.";
            case "FEARFUL", "ALERT" -> "Por favor, escúchame...";
            case "MELANCHOLIC", "EXHAUSTED" -> "No sé a quién más pedírselo.";
            case "HAPPY", "INSPIRED", "HOPEFUL" -> "¡Qué bien que vengas!";
            default -> "Tengo algo que pedirte.";
        };
        String distance = trust < 35 ? " No te conozco bien, pero no me queda otra opción." : trust > 70 ? " Sé que puedo contar contigo." : "";
        return q.giverName() + ": «" + opening + distance + " " + q.story().getOrDefault(yadi.samuraiai.living.quest.templates.QuestTemplate.StoryStage.PROLOGUE, q.title()) + "»";
    }

    public static String refusal(Quest q) { return q.giverName() + " te mira con desconfianza y cambia de tema: no te lo pedirá a ti."; }

    public static String progress(Quest q) {
        StringBuilder sb = new StringBuilder(q.title()).append(" — ").append(q.text());
        for (var o : q.relevant()) sb.append("\n  ").append(o.done() ? "✔ " : o.optional() ? "(opcional) " : "• ").append(o.description())
                .append(o.required() > 1 ? String.format(" [%.0f/%.0f]", o.progress(), o.required()) : "");
        return sb.toString();
    }

    public static String thanks(Quest q, String mood) {
        String m = mood == null ? "" : mood.toUpperCase(Locale.ROOT);
        return q.giverName() + ": «" + (m.equals("ANGRY") ? "Bien. Está hecho." : m.equals("HAPPY") || m.equals("HOPEFUL") ? "¡No sabes cuánto te lo agradezco!" : "Gracias. No lo olvidaré.") + "»";
    }

    public static String failure(Quest q) { return q.giverName() + ": «Ya es tarde... tendremos que arreglárnoslas.»"; }

    /** A short line for the Ollama prompt: what the NPC needs, so the conversation can bring it up naturally. */
    public static String promptLine(Quest q) {
        return String.format("Necesitas ayuda: %s (%s).%s", q.title(), q.text(), q.state() == Quest.State.ACTIVE ? " El jugador ya aceptó ayudarte." : " Puedes pedírselo al jugador.");
    }
}
