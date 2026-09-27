package yadi.samuraiai.prompt;

import yadi.samuraiai.context.AIContext;
import yadi.samuraiai.prompt.section.IdentitySection;
import yadi.samuraiai.prompt.section.PersonalitySection;
import yadi.samuraiai.prompt.section.PromptSection;
import yadi.samuraiai.prompt.section.SituationSection;
import yadi.samuraiai.prompt.section.StateSection;
import yadi.samuraiai.prompt.section.SystemSection;
import yadi.samuraiai.prompt.section.WorldSection;

import java.util.List;

/**
 * Assembles the system prompt from ordered {@link PromptSection}s.
 *
 * <p>Note what is <em>not</em> here any more: conversation history. It used
 * to be flattened into this text as "user: ... / assistant: ...", which threw
 * away the role structure the chat API exists to express and invited the
 * model to imitate the transcript format instead of speaking. History now
 * travels as real chat turns, assembled by
 * {@link yadi.samuraiai.ai.OllamaAIService}.
 *
 * <p>Sections are stateless and shared; adding one is a single entry in the
 * list below.
 */
public class PromptBuilder {

    private final List<PromptSection> sections = List.of(
            new SystemSection(),
            new IdentitySection(),
            new PersonalitySection(),
            new StateSection(),
            new SituationSection(),
            new WorldSection());

    public String build(AIContext context) {

        StringBuilder sb = new StringBuilder();

        for (PromptSection section : sections) {

            String text = section.build(context);

            if (text == null || text.isBlank()) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append('\n');
            }

            sb.append(text.stripTrailing()).append('\n');
        }

        return sb.toString();
    }
}
