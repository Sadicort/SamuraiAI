package yadi.samuraiai.integration.customnpcs;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.event.NpcEvent;
import yadi.samuraiai.controller.ChatNPCController;
import yadi.samuraiai.npc.*;
import yadi.samuraiai.runtime.*;
import yadi.samuraiai.world.*;

/** The only production package allowed to reference noppes classes. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class CustomNPCsController extends ChatNPCController {
    private final Map<UUID, ICustomNpc> avatars = new HashMap<>();
    private final Map<UUID, UUID> entityIds = new HashMap<>();
    private final Map<UUID, yadi.samuraiai.ai.navigation.world.MobMovementBody> bodies = new HashMap<>();
    private final NpcAPI api;
    private final IEventBus events;
    private final Consumer<NpcEvent.InteractEvent> interact = this::onInteract;

    public CustomNPCsController() {
        ServerScheduler.getInstance().requireServerThread();
        api = Objects.requireNonNull(NpcAPI.Instance(), "CustomNPCs API unavailable");
        events = Objects.requireNonNull(api.events(), "CustomNPCs event bus unavailable");
        events.addListener(interact);
    }
    @Override public boolean requiresPhysicalBody() { return true; }
    @Override public boolean ownsEntity(NPCInstance instance, UUID entityId) {
        ServerScheduler.getInstance().requireServerThread();
        return instance.getIdentity().id().equals(entityIds.get(entityId));
    }
    @Override public boolean spawnPhysical(NPCInstance instance, SpawnLocation location) {
        ServerScheduler.getInstance().requireServerThread();
        if (avatars.containsKey(instance.getIdentity().id())) return false;
        var level = ServerWorlds.level(location.dimensionKey());
        if (level.isEmpty()) return false;
        ICustomNpc npc = api.spawnNPC(level.get(), (int)Math.floor(location.x()), (int)Math.floor(location.y()), (int)Math.floor(location.z()));
        if (npc == null) return false;
        try {
            Entity entity = npc.getMCEntity();
            entity.setPos(location.x(), location.y(), location.z());
            npc.getDisplay().setName(instance.getIdentity().name());
            npc.getDisplay().setTitle(instance.getIdentity().type().value());
            npc.setRotation(location.yaw());
            npc.setHome((int)Math.floor(location.x()), (int)Math.floor(location.y()), (int)Math.floor(location.z()));
            takeOverMovement(npc);
            // API-spawned avatars keep the entity type's initial 1x1 box until dimensions are refreshed; without this a
            // 0.6-wide NPC physically cannot pass a door and grazes every wall corner.
            entity.refreshDimensions();
            avatars.put(instance.getIdentity().id(), npc);
            entityIds.put(entity.getUUID(), instance.getIdentity().id());
            return true;
        } catch (RuntimeException error) { npc.getMCEntity().discard(); throw error; }
    }
    @Override public void removePhysical(NPCInstance instance) {
        ServerScheduler.getInstance().requireServerThread();
        bodies.remove(instance.getIdentity().id());
        ICustomNpc npc = avatars.remove(instance.getIdentity().id());
        if (npc != null) { Entity entity = npc.getMCEntity(); entityIds.remove(entity.getUUID()); entity.discard(); }
    }
    /**
     * SamuraiAI owns routing, so CustomNPCs' own wandering and go-home behavior is switched off for the avatar:
     * two systems steering one body would fight over every step. Failures here are non-fatal; the NPC still works.
     */
    private static void takeOverMovement(ICustomNpc npc) {
        try {
            var ai = npc.getAi();
            ai.setReturnsHome(false);
            ai.setMovingType(0);
        } catch (RuntimeException error) {
            yadi.samuraiai.logging.SamuraiLogger.CUSTOM_NPCS.warn("Could not disable CustomNPCs own movement AI: {}", error.toString());
        }
    }
    @Override public java.util.Optional<yadi.samuraiai.ai.navigation.movement.MovementBody> movementBody(NPCInstance instance) {
        ServerScheduler.getInstance().requireServerThread();
        UUID id = instance.getIdentity().id();
        ICustomNpc npc = avatars.get(id);
        if (npc == null || !isPhysicalPresent(instance) || !(npc.getMCEntity() instanceof Mob mob)) { bodies.remove(id); return java.util.Optional.empty(); }
        var body = bodies.get(id);
        if (body == null || body.mob() != mob) { body = new yadi.samuraiai.ai.navigation.world.MobMovementBody(mob); bodies.put(id, body); }
        return java.util.Optional.of(body);
    }
    @Override public java.util.Set<UUID> ownedEntityIds() { return java.util.Collections.unmodifiableSet(entityIds.keySet()); }
    @Override public boolean isPhysicalPresent(NPCInstance instance) {
        ServerScheduler.getInstance().requireServerThread();
        ICustomNpc npc = avatars.get(instance.getIdentity().id());
        return npc != null && !npc.getMCEntity().isRemoved() && npc.getMCEntity().isAlive();
    }
    @Override public void synchronize(NPCInstance instance) {
        ServerScheduler.getInstance().requireServerThread();
        ICustomNpc npc = avatars.get(instance.getIdentity().id());
        if (npc == null || npc.getMCEntity().isRemoved()) return;
        Entity entity = npc.getMCEntity();
        instance.setLocation(new SpawnLocation(entity.level.dimension().location().toString(), entity.getX(), entity.getY(), entity.getZ(), entity.getYRot()));
    }
    @Override public void speak(NPCInstance instance, String playerName, String text) {
        ServerScheduler.getInstance().requireServerThread();
        if (text == null || text.isBlank() || !isPhysicalPresent(instance)) return;
        ICustomNpc npc = avatars.get(instance.getIdentity().id());
        var target = ServerWorlds.playerByName(playerName);
        if (target.isPresent() && api.getIEntity(target.get()) instanceof IPlayer player) npc.sayTo(player, text);
        else npc.say(text);
    }
    @Override public boolean moveTo(NPCInstance instance, SpawnLocation target, double speed) {
        ServerScheduler.getInstance().requireServerThread();
        ICustomNpc npc = avatars.get(instance.getIdentity().id());
        if (npc == null || target == null || !isPhysicalPresent(instance)) return false;
        Entity entity = npc.getMCEntity();
        if (!(entity instanceof Mob mob)) return false;
        if (!entity.level.dimension().location().toString().equals(target.dimensionKey())) return false;
        return mob.getNavigation().moveTo(target.x(), target.y(), target.z(), speed);
    }
    @Override public boolean lookAt(NPCInstance instance, UUID targetId) {
        ServerScheduler.getInstance().requireServerThread();
        ICustomNpc npc = avatars.get(instance.getIdentity().id());
        var target = yadi.samuraiai.world.ServerWorlds.playerById(targetId);
        if (npc == null || target.isEmpty() || !(npc.getMCEntity() instanceof Mob mob)) return false;
        mob.getLookControl().setLookAt(target.get(), 30.0F, 30.0F);
        return true;
    }
    @Override public boolean attack(NPCInstance instance, UUID targetId) {
        ServerScheduler.getInstance().requireServerThread();
        ICustomNpc npc = avatars.get(instance.getIdentity().id());
        var target = yadi.samuraiai.world.ServerWorlds.playerById(targetId);
        if (npc == null || target.isEmpty() || !(npc.getMCEntity() instanceof Mob mob)) return false;
        LivingEntity victim = target.get();
        if (victim.level != mob.level || mob.distanceToSqr(victim) > 16.0D) return false;
        mob.setTarget(victim);
        return mob.doHurtTarget(victim);
    }
    private void onInteract(NpcEvent.InteractEvent event) {
        ServerScheduler.getInstance().requireServerThread();
        if (event.npc == null || event.player == null) return;
        UUID npcId = entityIds.get(event.npc.getMCEntity().getUUID());
        if (npcId == null) return;
        if (event.player.getMCEntity() instanceof ServerPlayer player)
            NPCManager.getInstance().find(npcId).ifPresent(npc -> DialogueRouter.speakTo(npc, player, "Hola."));
    }
    @Override public void close() {
        ServerScheduler.getInstance().requireServerThread();
        events.unregister(interact);
        for (ICustomNpc npc : avatars.values()) npc.getMCEntity().discard();
        avatars.clear(); entityIds.clear(); bodies.clear();
    }
}
