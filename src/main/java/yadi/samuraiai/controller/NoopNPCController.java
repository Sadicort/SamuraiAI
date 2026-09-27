package yadi.samuraiai.controller;

import yadi.samuraiai.logging.SamuraiLogger;
import yadi.samuraiai.npc.NPCInstance;
import yadi.samuraiai.world.SpawnLocation;

/**
 * Controller with no physical body: the NPC exists, thinks and can speak, but
 * places no visible entity in the world. Used in unit tests and as the base
 * for backends that only implement part of the contract.
 *
 * <p>The "no physical body" warning is logged once per process rather than
 * once per spawn. The previous version warned on every single spawn, which
 * buried real errors on a server that placed a few dozen NPCs.
 */
public class NoopNPCController implements NPCController {

    private static volatile boolean warned;

    @Override
    public boolean spawnPhysical(NPCInstance instance, SpawnLocation location) {

        if (!warned) {
            warned = true;
            SamuraiLogger.NPC.warn("No hay un controlador fisico configurado: los NPCs pensaran y hablaran, "
                    + "pero no tendran cuerpo visible (seam para CustomNPCsController).");
        }

        return true;
    }

    @Override
    public void removePhysical(NPCInstance instance) {
    }

    @Override
    public boolean isPhysicalPresent(NPCInstance instance) {
        return false;
    }

    @Override
    public void speak(NPCInstance instance, String targetPlayerName, String text) {
        SamuraiLogger.DIALOGUE.info("{} -> {}: {}",
                instance.getIdentity().name(),
                targetPlayerName == null ? "(nadie)" : targetPlayerName,
                text);
    }
}
