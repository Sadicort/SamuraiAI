package yadi.samuraiai.runtime;

import java.util.UUID;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import yadi.samuraiai.Samuraiai;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.event.*;
import yadi.samuraiai.event.npc.*;
import yadi.samuraiai.emotion.EmotionService;

/** Bridges physical facts into domain events; no combat behavior is implemented here. */
@Mod.EventBusSubscriber(modid = Samuraiai.MODID)
public final class RuntimeEvents {
    public static void install() {
        NPCEventBus.getInstance().subscribe(NpcDamagedEvent.class, event ->
                NPCManager.getInstance().find(event.npcId()).filter(npc -> npc.isActive()).ifPresent(npc ->
                        EmotionService.getInstance().stimulate(npc, EmotionService.Stimulus.DAMAGE, Math.round(event.amount()))));
    }
    @SubscribeEvent public static void damaged(LivingHurtEvent event) {
        if (event.getEntity().level.isClientSide || !ServerScheduler.getInstance().isRunning()) return;
        ServerScheduler.getInstance().requireServerThread();
        UUID entity = event.getEntity().getUUID();
        for (var npc : NPCManager.getInstance().getActive()) {
            if (npc.getController().ownsEntity(npc.getInstance(), entity)) {
                NPCEventBus.getInstance().post(new NpcDamagedEvent(npc.getId(),
                        event.getSource().getEntity() == null ? "environment" : event.getSource().getEntity().getName().getString(), event.getAmount()));
            }
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (ServerScheduler.getInstance().isServerThread()) DialogueRouter.forgetPlayer(event.getEntity().getUUID());
    }
    public static void combatStarted(UUID npc, UUID target) {
        ServerScheduler.getInstance().requireServerThread();
        NPCEventBus.getInstance().post(new CombatStartedEvent(npc, target));
    }
    public static void combatEnded(UUID npc, String reason) {
        ServerScheduler.getInstance().requireServerThread();
        NPCEventBus.getInstance().post(new CombatEndedEvent(npc, reason));
    }
}
