package yadi.samuraiai.prompt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import yadi.samuraiai.context.AIContext;
import yadi.samuraiai.prompt.section.WorldSection;

/** The living world's lines enter the prompt framed, capped, and not at all when there are none. */
class WorldSectionTest {
    @Test void emptyWorldAddsNothing() {
        AIContext c = AIContext.builder().playerMessage("hola").build();
        assertEquals("", new WorldSection().build(c));
        assertFalse(new PromptBuilder().build(c).contains("TU MUNDO"));
    }

    @Test void linesAreFramedAndCapped() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 12; i++) lines.add("hecho " + i);
        lines.add(3, "  ");
        AIContext c = AIContext.builder().playerMessage("hola").world(lines).build();
        String text = new WorldSection().build(c);
        assertTrue(text.contains("TU MUNDO"));
        assertTrue(text.contains("- hecho 0") && text.contains("- hecho 7"));
        assertFalse(text.contains("hecho 8"), "at most eight facts");
        assertTrue(new PromptBuilder().build(c).contains("TU MUNDO"));
    }
}
