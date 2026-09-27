package yadi.samuraiai.registry;

import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.npc.NPCDefinition;
import yadi.samuraiai.npc.NPCTypeId;
import yadi.samuraiai.npc.definition.GuardDefinition;
import yadi.samuraiai.npc.definition.MerchantDefinition;
import yadi.samuraiai.npc.definition.SamuraiDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers and looks up NPC *type* definitions. Adding a new type means
 * calling {@link #register} with a new definition — never modifying this
 * class's consumers or the Brain.
 */
public class NPCTypeRegistry {

    private static final NPCTypeRegistry INSTANCE = new NPCTypeRegistry();

    private final Map<NPCTypeId, NPCDefinition> definitions = new ConcurrentHashMap<>();

    private NPCTypeRegistry() {
        register(SamuraiDefinition.create());
        register(GuardDefinition.create());
        register(MerchantDefinition.create());
    }

    public static NPCTypeRegistry getInstance() {
        return INSTANCE;
    }

    public void register(NPCDefinition definition) {

        NPCDefinition previous = definitions.put(definition.getType(), definition);

        if (previous != null) {
            SamuraiLogger.CORE.warn("La definicion de tipo {} ha sido reemplazada.",
                    definition.getType().value());
        }
    }

    public Optional<NPCDefinition> get(NPCTypeId type) {
        return type == null ? Optional.empty() : Optional.ofNullable(definitions.get(type));
    }

    /** Read-only; registration goes through {@link #register}. */
    public Map<NPCTypeId, NPCDefinition> getAll() {
        return Collections.unmodifiableMap(definitions);
    }

    /** Sorted type names, for command suggestions and error messages. */
    public List<String> typeNames() {
        return definitions.keySet().stream().map(NPCTypeId::value).sorted().toList();
    }
}
