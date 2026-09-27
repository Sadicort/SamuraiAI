package yadi.samuraiai.foundation;

import org.junit.jupiter.api.Test;
import yadi.samuraiai.foundation.audit.AuditContext;
import yadi.samuraiai.foundation.module.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ModuleLoaderTest {
    private static ModuleLoader loader(AuditContext.Side side) {
        return new ModuleLoader(new ModuleContext(side, Map.of(), Map.of()));
    }
    private static ModuleDescriptor module(String id, ModuleSide side, int priority, ModuleDependency... dependencies) {
        return new ModuleDescriptor(id, id, "1", "test", List.of(dependencies), side, priority);
    }
    @Test void dependenciesLoadBeforeDependentsAndCloseInReverse() {
        List<String> lifecycle = new ArrayList<>();
        try (var loader = loader(AuditContext.Side.DEDICATED_SERVER)) {
            loader.register(module("brain", ModuleSide.COMMON, 100, ModuleDependency.required("common")),
                    ignored -> { lifecycle.add("brain+"); return () -> lifecycle.add("brain-"); });
            loader.register(module("common", ModuleSide.COMMON, 1),
                    ignored -> { lifecycle.add("common+"); return () -> lifecycle.add("common-"); });
            loader.loadAll();
            assertEquals(List.of("common", "brain"), loader.loadOrder());
            assertEquals(List.of("common+", "brain+"), lifecycle);
        }
        assertEquals(List.of("common+", "brain+", "brain-", "common-"), lifecycle);
    }
    @Test void cycleFailsAndLeavesNoReadyModule() {
        try (var loader = loader(AuditContext.Side.DEDICATED_SERVER)) {
            loader.register(module("one", ModuleSide.COMMON, 1, ModuleDependency.required("two")), ignored -> () -> {});
            loader.register(module("two", ModuleSide.COMMON, 1, ModuleDependency.required("one")), ignored -> () -> {});
            assertThrows(IllegalStateException.class, loader::loadAll);
            assertTrue(loader.loadOrder().isEmpty());
            assertTrue(loader.snapshot().stream().anyMatch(module -> module.detail().contains("cycle")));
        }
    }
    @Test void failedInitializerRollsBackAlreadyInitializedDependency() {
        List<String> closed = new ArrayList<>();
        try (var loader = loader(AuditContext.Side.DEDICATED_SERVER)) {
            loader.register(module("common", ModuleSide.COMMON, 10), ignored -> () -> closed.add("common"));
            loader.register(module("brain", ModuleSide.COMMON, 1, ModuleDependency.required("common")),
                    ignored -> { throw new IllegalStateException("fixture"); });
            assertThrows(IllegalStateException.class, loader::loadAll);
            assertEquals(List.of("common"), closed);
            assertFalse(loader.ready("common"));
        }
    }
    @Test void clientModuleIsNeverInitializedOnDedicatedServer() {
        boolean[] initialized = {false};
        try (var loader = loader(AuditContext.Side.DEDICATED_SERVER)) {
            loader.register(module("voice", ModuleSide.CLIENT, 1), ignored -> { initialized[0] = true; return () -> {}; });
            loader.loadAll();
            assertFalse(initialized[0]);
            assertEquals(ModuleState.DISABLED, loader.find("voice").orElseThrow().state());
        }
    }
    @Test void requiredMissingDependencyFailsButOptionalMissingDoesNot() {
        try (var missing = loader(AuditContext.Side.CLIENT)) {
            missing.register(module("voice", ModuleSide.CLIENT, 1, ModuleDependency.required("client")), ignored -> () -> {});
            assertThrows(IllegalStateException.class, missing::loadAll);
            assertEquals(ModuleState.NOT_FOUND, missing.find("voice").orElseThrow().state());
        }
        try (var optional = loader(AuditContext.Side.CLIENT)) {
            optional.register(module("debug", ModuleSide.COMMON, 1, ModuleDependency.optional("overlay")), ignored -> () -> {});
            optional.loadAll(); assertTrue(optional.ready("debug"));
        }
    }
}
