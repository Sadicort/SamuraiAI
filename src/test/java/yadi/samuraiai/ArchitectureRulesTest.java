package yadi.samuraiai;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Enforces the architectural prohibitions of the phase 2 specification as executable checks over the source tree: the
 * engines stay separate and the pure cores stay free of Minecraft. A violation names the file and the offending import.
 */
class ArchitectureRulesTest {
    private static final Path MAIN = Path.of("src/main/java/yadi/samuraiai");

    private static List<Path> sources(String relative) throws IOException {
        Path root = MAIN.resolve(relative);
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> files = Files.walk(root)) { return files.filter(p -> p.toString().endsWith(".java")).toList(); }
    }

    private static List<String> imports(Path file) throws IOException {
        return Files.readAllLines(file).stream().map(String::trim).filter(l -> l.startsWith("import ")).toList();
    }

    private static void forbid(String area, List<String> forbiddenPrefixes, String... excludedSubpackages) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : sources(area)) {
            String normal = file.toString().replace('\\', '/');
            boolean excluded = false;
            for (String sub : excludedSubpackages) if (normal.contains("/" + sub + "/")) excluded = true;
            if (excluded) continue;
            for (String line : imports(file))
                for (String prefix : forbiddenPrefixes)
                    if (line.startsWith("import " + prefix) || line.startsWith("import static " + prefix)) violations.add(normal + " -> " + line);
        }
        assertTrue(violations.isEmpty(), "Architecture violations:\n" + String.join("\n", violations));
    }

    @Test void perceptionNeverDependsOnTheBrainBehaviorsTasksOrNavigation() throws IOException {
        forbid("ai/perception", List.of("yadi.samuraiai.brain", "yadi.samuraiai.behavior", "yadi.samuraiai.task", "yadi.samuraiai.action",
                "yadi.samuraiai.decision", "yadi.samuraiai.goal", "yadi.samuraiai.ai.navigation", "yadi.samuraiai.ai.scheduler",
                "yadi.samuraiai.controller", "yadi.samuraiai.emotion"), "world");
    }

    @Test void navigationNeverDependsOnPerceptionTheSchedulerOrTheBrain() throws IOException {
        forbid("ai/navigation", List.of("yadi.samuraiai.ai.perception", "yadi.samuraiai.ai.scheduler", "yadi.samuraiai.brain", "yadi.samuraiai.behavior",
                "yadi.samuraiai.decision", "yadi.samuraiai.goal"), "world");
    }

    @Test void thePureCoresNeverTouchMinecraft() throws IOException {
        forbid("ai/navigation", List.of("net.minecraft", "net.minecraftforge", "noppes"), "world");
        forbid("ai/perception", List.of("net.minecraft", "net.minecraftforge", "noppes"), "world");
    }

    @Test void onlyTheCustomNpcsIntegrationPackageMayImportTheirClasses() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String normal = file.toString().replace('\\', '/');
                if (normal.contains("/integration/customnpcs/")) continue;
                for (String line : imports(file)) if (line.startsWith("import noppes.")) violations.add(normal + " -> " + line);
            }
        }
        assertTrue(violations.isEmpty(), "noppes imports outside the adapter:\n" + String.join("\n", violations));
    }

    @Test void theSchedulerNeverMovesEntitiesDirectly() throws IOException {
        forbid("ai/scheduler", List.of("net.minecraft.world.entity", "yadi.samuraiai.ai.navigation.movement", "yadi.samuraiai.ai.navigation.world"), "world");
    }

    @Test void theSchedulerCoreKnowsNeitherTheBrainNorPerceptionNorNavigationNorMinecraft() throws IOException {
        forbid("ai/scheduler", List.of("net.minecraft", "net.minecraftforge", "noppes", "yadi.samuraiai.brain", "yadi.samuraiai.behavior", "yadi.samuraiai.task",
                "yadi.samuraiai.action", "yadi.samuraiai.decision", "yadi.samuraiai.goal", "yadi.samuraiai.controller", "yadi.samuraiai.emotion",
                "yadi.samuraiai.npc", "yadi.samuraiai.ai.perception", "yadi.samuraiai.ai.navigation", "yadi.samuraiai.context"), "world");
    }

    @Test void onlyTheSchedulerAdapterMayReachIntoPerceptionAndNavigationOnItsBehalf() throws IOException {
        // The scheduler adapter is the one place that reads perception and feeds temperament back to navigation; nothing else in the
        // scheduler package tree may, and the Brain reaches the scheduler only through the world context and the goal mapper.
        forbid("ai/scheduler/engine", List.of("yadi.samuraiai.ai.perception", "yadi.samuraiai.ai.navigation"));
        forbid("ai/scheduler/group", List.of("yadi.samuraiai.ai.perception", "yadi.samuraiai.ai.navigation"));
    }
}
