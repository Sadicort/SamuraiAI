package yadi.samuraiai.event;

import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yadi.samuraiai.Samuraiai;
import yadi.samuraiai.config.SamuraiSettings;
import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.npc.NPCManager;
import yadi.samuraiai.runtime.DialogueRouter;

/**
 * Lets players talk to NPCs simply by speaking in chat near one.
 *
 * <p>The class this replaces was an empty shell, which is why the dialogue
 * pipeline — queue, prompt builder, memory, Ollama client, all of it — was
 * only ever reachable from a {@code main} method and never from inside the
 * game.
 *
 * <p>The event is not cancelled: the player's message still goes to chat
 * normally, and the NPC answers alongside it.
 */
@Mod.EventBusSubscriber(modid = Samuraiai.MODID)
public final class NPCChatListener {

    private NPCChatListener() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {

        if (!SamuraiSettings.answerPublicChat()) {
            return;
        }

        // Cheap guard first: with no NPCs loaded this runs on every chat
        // message from every player and should cost essentially nothing.
        if (NPCManager.getInstance().isEmpty()) {
            return;
        }

        try {
            DialogueRouter.speakToNearest(event.getPlayer(), event.getRawText());
        } catch (RuntimeException e) {
            // A failure here must never swallow or break a player's chat
            // message, so it is logged and the event continues untouched.
            SamuraiLogger.DIALOGUE.error("Fallo al enrutar el chat hacia un NPC: {}", e.toString());
        }
    }
}
