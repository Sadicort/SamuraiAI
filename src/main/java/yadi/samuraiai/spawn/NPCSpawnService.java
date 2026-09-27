package yadi.samuraiai.spawn;

import java.util.*;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.controller.*;
import yadi.samuraiai.event.*;
import yadi.samuraiai.event.npc.*;
import yadi.samuraiai.memory.MemoryManager;
import yadi.samuraiai.npc.*;
import yadi.samuraiai.npc.lifecycle.*;
import yadi.samuraiai.npc.relationship.RelationshipService;
import yadi.samuraiai.registry.NPCTypeRegistry;
import yadi.samuraiai.runtime.*;
import yadi.samuraiai.logging.SamuraiLogger;

/** Owns activation/deactivation transactions and rollback, always on the server thread. */
public final class NPCSpawnService {
    private static final NPCSpawnService INSTANCE = new NPCSpawnService();
    public static NPCSpawnService getInstance() { return INSTANCE; }
    private final NPCFactory factory = new NPCFactory();
    private NPCController controller = new NoopNPCController();
    public void setController(NPCController value) {
        ServerScheduler.getInstance().requireServerThread();
        if (!NPCManager.getInstance().isEmpty()) throw new IllegalStateException("Cannot replace controller with active NPCs");
        controller.close(); controller = Objects.requireNonNull(value);
    }
    public NPCController getController() { return controller; }
    public NPCSpawnResult spawn(NPCSpawnRequest request) { return spawn(request, null, null); }
    public NPCSpawnResult restore(yadi.samuraiai.npc.persistence.NPCSnapshot snapshot) {
        ServerScheduler.getInstance().requireServerThread();
        if (snapshot == null) return NPCSpawnResult.failure("Snapshot nulo.");
        return spawn(new NPCSpawnRequest(yadi.samuraiai.npc.NPCTypeId.of(snapshot.type()), snapshot.name(), snapshot.location()), snapshot.id(),
                new yadi.samuraiai.personality.NpcPersonality(snapshot.type(), snapshot.personality()));
    }
    private NPCSpawnResult spawn(NPCSpawnRequest request, UUID restoredId, yadi.samuraiai.personality.NpcPersonality restoredPersonality) {
        ServerScheduler.getInstance().requireServerThread();
        if (request == null || request.type() == null || request.location() == null)
            return NPCSpawnResult.failure("Solicitud de aparición inválida.");
        NPCManager manager = NPCManager.getInstance();
        if (manager.count() >= SamuraiSettings.maxActiveNpcs()) return NPCSpawnResult.failure("Límite de NPCs activos alcanzado.");
        NPCDefinition definition = NPCTypeRegistry.getInstance().get(request.type()).orElse(null);
        if (definition == null) return NPCSpawnResult.failure("Tipo desconocido: " + request.type());
        String name = manager.uniqueName(request.requestedName() == null || request.requestedName().isBlank()
                ? definition.getDisplayNamePrefix() : request.requestedName());
        NPCIdentity identity = restoredId == null ? NPCIdentity.generate(name, request.type()) : new NPCIdentity(restoredId, name, request.type());
        NPCLifecycleManager lifecycle = NPCLifecycleManager.getInstance();
        lifecycle.transition(identity.id(), NPCLifecycleState.CREATING);
        NPCRuntime runtime = null;
        try {
            runtime = restoredId == null
                    ? factory.build(definition, identity, controller, request.location())
                    : factory.build(definition, identity, controller, request.location(), restoredPersonality);
            lifecycle.transition(identity.id(), NPCLifecycleState.INITIALIZING);
            if (!controller.spawnPhysical(runtime.getInstance(), request.location()))
                throw new IllegalStateException("El controlador no pudo crear el avatar.");
            manager.register(runtime);
            lifecycle.transition(identity.id(), NPCLifecycleState.ACTIVE);
            runtime.setActive(true);
            yadi.samuraiai.npc.persistence.PersistenceBootstrap.getInstance().activated(runtime);
            NPCEventBus.getInstance().post(new NPCSpawnedEvent(identity));
            NPCEventBus.getInstance().post(new NPCActivatedEvent(identity.id()));
            SamuraiLogger.SPAWN.info("spawn {}", SamuraiLogger.context(runtime));
            return NPCSpawnResult.ok(runtime.getInstance(), "NPC " + name + " creado.");
        } catch (RuntimeException | LinkageError error) {
            if (runtime != null) {
                runtime.setActive(false); runtime.getBrain().cancelAll(); manager.unregister(identity.id());
                try { controller.removePhysical(runtime.getInstance()); } catch (RuntimeException ignored) { SamuraiLogger.SPAWN.warn("Spawn rollback failed", ignored); }
            }
            if (lifecycle.getState(identity.id()) == NPCLifecycleState.ACTIVE)
                lifecycle.transition(identity.id(), NPCLifecycleState.UNLOADING);
            lifecycle.transition(identity.id(), NPCLifecycleState.REMOVED); lifecycle.forget(identity.id());
            MemoryManager.getInstance().forget(identity.id());
            RelationshipService.getInstance().forget(identity.id());
            SamuraiLogger.SPAWN.error("Spawn failed npc={}", identity.id(), error);
            return NPCSpawnResult.failure("No se pudo crear el NPC: " + error.getClass().getSimpleName());
        }
    }
    public boolean remove(UUID id, String reason, boolean forget) {
        ServerScheduler.getInstance().requireServerThread();
        NPCRuntime runtime = NPCManager.getInstance().find(id).orElse(null);
        if (runtime == null || !runtime.isActive()) return false;
        NPCLifecycleManager lifecycle = NPCLifecycleManager.getInstance();
        lifecycle.transition(id, NPCLifecycleState.UNLOADING);
        runtime.setActive(false);
        DialogueService.getInstance().cancelNpc(id);
        yadi.samuraiai.foundation.async.AsyncEngine.global().cancelOwner("npc:" + id);
        runtime.getBrain().cancelAll();
        yadi.samuraiai.ai.navigation.world.NavigationService.getInstance().forget(id);
        yadi.samuraiai.ai.perception.world.PerceptionService.getInstance().forget(id);
        yadi.samuraiai.ai.scheduler.world.SchedulerService.getInstance().forget(id);
        DialogueRouter.forgetNpc(id);
        NPCTickService.forget(id);
        try { yadi.samuraiai.npc.persistence.PersistenceBootstrap.getInstance().deactivated(runtime); }
        catch (RuntimeException error) { SamuraiLogger.SPAWN.warn("Persistence hook failed npc={}", id, error); }
        try { runtime.getController().removePhysical(runtime.getInstance()); }
        catch (RuntimeException error) { SamuraiLogger.SPAWN.error("Avatar removal failed npc={}", id, error); }
        NPCManager.getInstance().unregister(id);
        MemoryManager.getInstance().forget(id);
        RelationshipService.getInstance().forget(id);
        if (!"server stopped".equals(reason)) {
            yadi.samuraiai.world.ServerWorlds.server().ifPresent(server ->
                    yadi.samuraiai.npc.persistence.PersistenceBootstrap.getInstance().forget(id, server));
        }
        NPCEventBus.getInstance().post(new NPCDeactivatedEvent(id, reason));
        if (!forget) lifecycle.transition(id, NPCLifecycleState.INACTIVE);
        lifecycle.transition(id, NPCLifecycleState.REMOVED);
        lifecycle.forget(id);
        NPCEventBus.getInstance().post(new NPCRemovedEvent(id, reason));
        SamuraiLogger.SPAWN.info("remove reason={} {}", reason, SamuraiLogger.context(runtime));
        return true;
    }
    public int removeAll(String reason, boolean forget) {
        ServerScheduler.getInstance().requireServerThread();
        int count = 0;
        for (NPCRuntime runtime : List.copyOf(NPCManager.getInstance().getActive()))
            if (remove(runtime.getId(), reason, forget)) count++;
        return count;
    }
    public void close() { ServerScheduler.getInstance().requireServerThread(); controller.close(); controller = new NoopNPCController(); }
}
