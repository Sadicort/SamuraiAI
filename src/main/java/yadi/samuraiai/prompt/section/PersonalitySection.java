package yadi.samuraiai.prompt.section;

import yadi.samuraiai.context.AIContext;
import yadi.samuraiai.personality.NpcPersonality;

public class PersonalitySection implements PromptSection {

    @Override
    public String build(AIContext context) {

        NpcPersonality personality = context.getPersonality();

        if (personality == null) {
            return "";
        }

        return """
                ------------------------
                TU PERSONALIDAD

                %s
                """.formatted(personality.getDescription());
    }
}
