package yadi.samuraiai.personality;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Turns a type's shared {@link NpcPersonality} (every samurai starts out the
 * same) into one specific to a single NPC, by folding in a randomly chosen
 * quirk. Without this every NPC of the same {@code NPCDefinition} would
 * think and talk identically, which stops feeling like individual NPCs the
 * moment two of them stand next to each other.
 *
 * <p>Deterministic-cost and offline on purpose: this runs once per spawn, not
 * per dialogue turn, so it does not call Ollama or add spawn latency. The
 * quirk becomes one more line the prompt builder ({@code PersonalitySection})
 * feeds the model, alongside the base description.
 */
public final class PersonalityGenerator {

    /**
     * Kept generic rather than per-type: a samurai and a merchant can equally
     * plausibly be superstitious or a bad liar. Type-specific flavour already
     * comes from each {@code NPCDefinition}'s base description.
     */
    private static final List<String> QUIRKS = List.of(
            "Tiene la costumbre de repetir en voz baja la ultima palabra que le dicen antes de responder.",
            "Es supersticioso: interpreta cualquier cosa fuera de lo comun como un mal presagio.",
            "Le cuesta mentir, y cuando lo intenta se nota en como cambia el tono.",
            "Disfruta contar una pequena anecdota personal apenas tiene ocasion, venga o no a cuento.",
            "Es de pocas palabras salvo que le hablen de algo que realmente le importa.",
            "Desconfia por naturaleza de los desconocidos hasta que se gana su confianza.",
            "Tiene un sentido del humor seco y suelta comentarios sarcasticos sin cambiar el gesto.",
            "Es extremadamente puntual y le irrita que le hagan perder el tiempo.",
            "Se toma cualquier conversacion muy en serio, casi nunca bromea.",
            "Es curioso por naturaleza y tiende a hacer preguntas de vuelta antes de responder del todo.",
            "Guarda un carino especial por su tierra natal y la menciona con frecuencia.",
            "Es optimista casi hasta la exageracion, incluso cuando la situacion es mala.",
            "Es cauteloso y prefiere responder con evasivas antes que comprometerse a algo.",
            "Tiene debilidad por la comida y no puede evitar mencionarla cuando surge el tema.",
            "Es orgulloso y le cuesta admitir un error o pedir ayuda directamente."
    );

    private PersonalityGenerator() {
    }

    /**
     * @return a new {@link NpcPersonality} with the same name but a
     *         description individualised with one quirk, or {@code null} if
     *         {@code base} is {@code null} (a definition with no personality
     *         stays that way rather than gaining one from nowhere)
     */
    public static NpcPersonality individualize(NpcPersonality base) {

        if (base == null) {
            return null;
        }

        String quirk = QUIRKS.get(ThreadLocalRandom.current().nextInt(QUIRKS.size()));

        return new NpcPersonality(base.getName(), base.getDescription() + " " + quirk);
    }
}
