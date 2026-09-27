package yadi.samuraiai.prompt.section;

import yadi.samuraiai.context.AIContext;

import java.util.List;

/**
 * What the NPC knows about its living world at this moment: the Deiliora date and season, the weather, its village and what
 * worries it, the problems it has asked travellers to help with. The lines come from the living world already worded in
 * Spanish; this section only frames them and caps their number so the prompt stays short.
 */
public class WorldSection implements PromptSection {

    private static final int MAX_LINES = 8;

    @Override
    public String build(AIContext context) {
        List<String> lines = context.getWorld();
        if (lines.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("------------------------\n");
        sb.append("TU MUNDO\n\n");
        int shown = 0;
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            if (shown++ >= MAX_LINES) break;
            sb.append("- ").append(line.strip()).append('\n');
        }
        sb.append("Habla de ello solo si viene al caso; no inventes hechos que no estén aquí.\n");
        return sb.toString();
    }
}
