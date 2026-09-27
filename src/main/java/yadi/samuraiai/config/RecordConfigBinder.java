package yadi.samuraiai.config;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Exposes every numeric or boolean component of a settings record as a Forge config value, so the config file is always
 * exactly the record: adding a tunable to the record makes it configurable with no second list to keep in sync. Ranges are
 * enforced by the record's own constructor (it clamps), not duplicated here. Lists of strings are supported too.
 */
public final class RecordConfigBinder<T extends Record> {
    private final Map<String, ForgeConfigSpec.ConfigValue<?>> values = new LinkedHashMap<>();
    private final Class<T> type;

    public RecordConfigBinder(ForgeConfigSpec.Builder builder, Class<T> type, T defaults) {
        this.type = type;
        try {
            for (RecordComponent component : type.getRecordComponents()) {
                Object value = component.getAccessor().invoke(defaults);
                String name = component.getName();
                if (value instanceof Integer || value instanceof Double || value instanceof Boolean) values.put(name, builder.define(name, value));
                else if (value instanceof List<?> list && list.stream().allMatch(e -> e instanceof String))
                    values.put(name, builder.defineListAllowEmpty(List.of(name), () -> List.<String>of(), o -> o instanceof String));
            }
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    /** Copies the current config values into the builder; a value of the wrong type is skipped (the default stays). */
    public <B extends RecordSettingsBuilder<T, B>> void pull(B settingsBuilder) {
        for (var entry : values.entrySet()) {
            Object value = entry.getValue().get();
            RecordComponent component = null;
            for (RecordComponent c : type.getRecordComponents()) if (c.getName().equals(entry.getKey())) component = c;
            if (component == null) continue;
            Class<?> expected = component.getType();
            if (expected == int.class && value instanceof Number n) value = n.intValue();
            else if (expected == double.class && value instanceof Number n) value = n.doubleValue();
            else if (expected == boolean.class && !(value instanceof Boolean)) continue;
            else if (value instanceof List<?> list) value = List.copyOf(list);
            settingsBuilder.set(entry.getKey(), value);
        }
    }
}
