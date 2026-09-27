package yadi.samuraiai.prompt.section;

import yadi.samuraiai.context.AIContext;

/**
 * Tells the model who it is and who it is talking to.
 *
 * <p>This block did not exist before: {@code AIContext} already carried
 * {@code npcName} and {@code playerName} but nothing ever put them in the
 * prompt, so every NPC answered without knowing its own name or the name of
 * the player in front of it.
 */
public class IdentitySection implements PromptSection {

    @Override
    public String build(AIContext context) {

        StringBuilder sb = new StringBuilder();

        sb.append("------------------------\n");
        sb.append("QUIEN ERES\n\n");
        sb.append("Te llamas ").append(context.getNpcName()).append(".\n");

        if (context.getNpcType() != null && !context.getNpcType().isBlank()) {
            sb.append("Eres un ").append(context.getNpcType()).append(".\n");
        }

        sb.append("Estas hablando con un jugador llamado ")
                .append(context.getPlayerName())
                .append(".\n");

        return sb.toString();
    }
}
