package yadi.samuraiai.living;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * The phase 5 prohibitions as executable checks: the six engines never import each other (they talk through ports the hub
 * implements), the pure cores never touch Minecraft or the NPC runtime, and only the hub and the adapter see several engines.
 */
class LivingArchitectureTest {
    private static final Path LIVING = Path.of("src/main/java/yadi/samuraiai/living");
    private static final List<String> ENGINES = List.of("calendar", "world", "village", "economy", "quest", "family");

    private static List<Path> sources(String area) throws IOException {
        Path root = LIVING.resolve(area);
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> s = Files.walk(root)) { return s.filter(p -> p.toString().endsWith(".java")).toList(); }
    }

    private static List<String> violations(String area, List<String> forbidden) throws IOException {
        List<String> out = new ArrayList<>();
        for (Path file : sources(area))
            for (String line : Files.readAllLines(file)) {
                String l = line.trim();
                if (!l.startsWith("import ")) {
                    for (String prefix : forbidden) if (!l.startsWith("*") && !l.startsWith("//") && l.contains(prefix + ".")) out.add(file + " -> " + l);
                    continue;
                }
                for (String prefix : forbidden) if (l.startsWith("import " + prefix) || l.startsWith("import static " + prefix)) out.add(file + " -> " + l);
            }
        return out;
    }

    @Test void theEnginesNeverImportEachOther() throws IOException {
        List<String> all = new ArrayList<>();
        for (String engine : ENGINES) {
            List<String> forbidden = new ArrayList<>();
            for (String other : ENGINES) if (!other.equals(engine)) forbidden.add("yadi.samuraiai.living." + other);
            forbidden.add("yadi.samuraiai.living.sim");
            forbidden.add("yadi.samuraiai.living.server");
            all.addAll(violations(engine, forbidden));
        }
        assertTrue(all.isEmpty(), "Cross-engine dependencies:\n" + String.join("\n", all));
    }

    @Test void onlyTheAdapterTouchesMinecraftAndTheNpcRuntime() throws IOException {
        List<String> forbidden = List.of("net.minecraft", "net.minecraftforge", "noppes", "yadi.samuraiai.npc", "yadi.samuraiai.brain", "yadi.samuraiai.behavior",
                "yadi.samuraiai.task", "yadi.samuraiai.goal", "yadi.samuraiai.spawn", "yadi.samuraiai.world");
        List<String> all = new ArrayList<>();
        for (String area : List.of("core", "calendar", "world", "village", "economy", "quest", "family", "sim")) all.addAll(violations(area, forbidden));
        assertTrue(all.isEmpty(), "Minecraft or runtime reached from a pure core:\n" + String.join("\n", all));
    }

    @Test void theCoreDependsOnNoEngine() throws IOException {
        List<String> forbidden = new ArrayList<>();
        for (String e : ENGINES) forbidden.add("yadi.samuraiai.living." + e);
        forbidden.add("yadi.samuraiai.living.sim");
        forbidden.add("yadi.samuraiai.living.server");
        List<String> all = violations("core", forbidden);
        assertTrue(all.isEmpty(), "Core depends on an engine:\n" + String.join("\n", all));
    }

    /** Every experience the living world asks the cognitive layer for is a real ExperienceKind (an unknown one would be dropped). */
    @Test void everyExperienceNameExistsInTheCognitiveCatalogue() throws IOException {
        java.util.Set<String> known = new java.util.HashSet<>();
        for (var k : yadi.samuraiai.ai.cognition.model.ExperienceKind.values()) known.add(k.name());
        java.util.regex.Pattern[] patterns = {
                java.util.regex.Pattern.compile("experience\\([^;]*?\"([A-Z][A-Z_]+)\""),
                java.util.regex.Pattern.compile("witnesses\\([^;]*?\"([A-Z][A-Z_]+)\""),
                java.util.regex.Pattern.compile("Kind\\.MEMORY,\\s*\"([A-Z][A-Z_]+)\"")};
        List<String> unknown = new ArrayList<>();
        int checked = 0;
        for (Path file : sources(""))
            for (String line : Files.readAllLines(file))
                for (var pattern : patterns) {
                    var m = pattern.matcher(line);
                    while (m.find()) { checked++; if (!known.contains(m.group(1))) unknown.add(file.getFileName() + ": " + m.group(1)); }
                }
        assertTrue(checked > 0, "the scan found the experience names");
        assertTrue(unknown.isEmpty(), "unknown experience kinds: " + unknown);
    }
}
