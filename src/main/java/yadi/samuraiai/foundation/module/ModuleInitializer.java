package yadi.samuraiai.foundation.module;

@FunctionalInterface
public interface ModuleInitializer {
    AutoCloseable initialize(ModuleContext context) throws Exception;
}
