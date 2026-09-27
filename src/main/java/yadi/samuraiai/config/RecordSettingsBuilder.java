package yadi.samuraiai.config;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds an immutable settings record by overriding components by name, starting from a base instance. Records
 * validate and clamp in their canonical constructor, so every path (tests, Forge config, commands) goes through
 * the same rules. Self-typed so subclasses keep their own chainable {@code set}.
 */
public abstract class RecordSettingsBuilder<T extends Record, B extends RecordSettingsBuilder<T, B>> {
    private final Class<T> type;
    private final Map<String, Object> values = new LinkedHashMap<>();

    protected RecordSettingsBuilder(Class<T> type, T base) {
        this.type = type;
        try {
            for (RecordComponent component : type.getRecordComponents())
                values.put(component.getName(), component.getAccessor().invoke(base));
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    protected abstract B self();

    @SuppressWarnings("unchecked")
    public B set(String name, Object value) {
        if (!values.containsKey(name)) throw new IllegalArgumentException("Unknown setting: " + name);
        values.put(name, value);
        return self();
    }

    public boolean has(String name) { return values.containsKey(name); }

    public T build() {
        try {
            RecordComponent[] components = type.getRecordComponents();
            Class<?>[] types = new Class<?>[components.length];
            Object[] arguments = new Object[components.length];
            for (int i = 0; i < components.length; i++) { types[i] = components[i].getType(); arguments[i] = values.get(components[i].getName()); }
            return type.getDeclaredConstructor(types).newInstance(arguments);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Cannot build " + type.getSimpleName(), error); }
    }
}
