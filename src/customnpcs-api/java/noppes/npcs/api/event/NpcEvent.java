package noppes.npcs.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IPlayer;

/**
 * Trimmed to the one nested event {@code CustomNPCsController} listens for
 * (right-click on the avatar). The real CustomNPCs mod fires several other
 * NpcEvent subclasses (DamagedEvent, DiedEvent, ...); add them back here,
 * faithfully, if a future integration needs to react to them.
 */
public class NpcEvent extends CustomNPCsEvent {
   public final ICustomNpc npc;

   public NpcEvent(ICustomNpc npc) {
      this.npc = npc;
   }

   @Cancelable
   public static class InteractEvent extends NpcEvent {
      public final IPlayer player;

      public InteractEvent(ICustomNpc npc, Player player) {
         super(npc);
         this.player = (IPlayer)NpcAPI.Instance().getIEntity(player);
      }
   }
}
