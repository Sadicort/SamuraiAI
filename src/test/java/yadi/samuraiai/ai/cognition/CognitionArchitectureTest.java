package yadi.samuraiai.ai.cognition;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The cognitive contract as executable rules: pure engines, no cycles between them, and no cognition inside the Brain, perception or navigation cores. */
class CognitionArchitectureTest {
    private static final Path MAIN = Path.of("src/main/java/yadi/samuraiai");
    private static final String P = "yadi.samuraiai.ai.";

    private static List<Path> sources(String relative) throws IOException {
        Path root = MAIN.resolve(relative);
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> files = Files.walk(root)) { return files.filter(p -> p.toString().endsWith(".java")).toList(); }
    }

    private static void forbid(String area, List<String> prefixes, String... excludedSubpackages) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : sources(area)) {
            String normal = file.toString().replace('\\', '/');
            boolean excluded = false;
            for (String sub : excludedSubpackages) if (normal.contains("/" + sub + "/")) excluded = true;
            if (excluded) continue;
            for (String line : Files.readAllLines(file)) {
                String t = line.trim();
                if (!t.startsWith("import ")) continue;
                for (String prefix : prefixes) if (t.startsWith("import " + prefix) || t.startsWith("import static " + prefix)) violations.add(normal + " -> " + t);
            }
        }
        assertTrue(violations.isEmpty(), "Architecture violations:\n" + String.join("\n", violations));
    }

    /** The shared kernel (cognition.model, .storage, .trace) is open to every engine; the hub that orchestrates them is not. */
    private static final List<String> HUB = List.of(P + "cognition.engine", P + "cognition.catalog", P + "cognition.personality", P + "cognition.world", P + "cognition.debug", P + "cognition.events");
    private static final List<String> MINECRAFT = List.of("net.minecraft", "net.minecraftforge", "noppes");
    private static final List<String> GAME = List.of("yadi.samuraiai.brain", "yadi.samuraiai.behavior", "yadi.samuraiai.task", "yadi.samuraiai.action", "yadi.samuraiai.decision",
            "yadi.samuraiai.goal", "yadi.samuraiai.controller", "yadi.samuraiai.npc", "yadi.samuraiai.context", "yadi.samuraiai.emotion", "yadi.samuraiai.memory",
            P + "perception", P + "navigation", P + "scheduler");

    @Test void everyCognitiveCoreIsFreeOfMinecraftAndOfTheGameLayers() throws IOException {
        for (String area : List.of("ai/memory", "ai/relationship", "ai/emotion", "ai/knowledge", "ai/cognition")) forbid(area, concat(MINECRAFT, GAME), "world");
    }

    @Test void theFourEnginesNeverDependOnEachOther() throws IOException {
        forbid("ai/memory", List.of(P + "relationship", P + "emotion", P + "knowledge"));
        forbid("ai/relationship", concat(List.of(P + "memory", P + "emotion", P + "knowledge"), HUB));
        forbid("ai/emotion", concat(List.of(P + "memory", P + "relationship", P + "knowledge"), HUB));
        forbid("ai/knowledge", concat(List.of(P + "memory", P + "relationship", P + "emotion"), HUB));
    }

    @Test void onlyTheCognitiveHubMayTranslateBetweenEngines() throws IOException {
        // The hub is the one place that imports all four; the reverse direction is forbidden above.
        forbid("ai/cognition/model", List.of(P + "memory", P + "relationship", P + "emotion", P + "knowledge"));
        forbid("ai/cognition/storage", List.of(P + "scheduler", P + "perception", P + "navigation"));
    }

    @Test void theBrainPerceptionNavigationAndSchedulerCoresNeverImportTheEngines() throws IOException {
        for (String area : List.of("ai/perception", "ai/navigation", "ai/scheduler"))
            forbid(area, List.of(P + "memory", P + "relationship", P + "emotion", P + "knowledge", P + "cognition"), "world");
        forbid("perception", List.of(P + "cognition"));
        forbid("behavior", List.of(P + "memory", P + "relationship", P + "emotion", P + "knowledge"));
    }

    @Test void memoryLivesOutsideTheBrainAndTheBrainReadsOnlyAdvice() throws IOException {
        forbid("brain", List.of(P + "memory", P + "relationship", P + "emotion", P + "knowledge"));
        forbid("decision", List.of(P + "memory", P + "relationship", P + "emotion", P + "knowledge", P + "cognition.world", P + "cognition.storage"));
    }

    private static List<String> concat(List<String> a, List<String> b) { List<String> r = new ArrayList<>(a); r.addAll(b); return r; }
}
