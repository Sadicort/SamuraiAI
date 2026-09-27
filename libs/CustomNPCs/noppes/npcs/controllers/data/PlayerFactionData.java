package noppes.npcs.controllers.data;

import java.util.HashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.EventHooks;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.PlayerEvent;
import noppes.npcs.api.wrapper.PlayerWrapper;
import noppes.npcs.controllers.FactionController;

public class PlayerFactionData {
   public HashMap<Integer, Integer> factionData = new HashMap<>();

   public void loadNBTData(CompoundTag compound) {
      HashMap<Integer, Integer> factionData = new HashMap<>();
      if (compound != null) {
         ListTag list = compound.m_128437_("FactionData", 10);
         if (list != null) {
            for (int i = 0; i < list.size(); i++) {
               CompoundTag nbttagcompound = list.m_128728_(i);
               factionData.put(nbttagcompound.m_128451_("Faction"), nbttagcompound.m_128451_("Points"));
            }

            this.factionData = factionData;
         }
      }
   }

   public void saveNBTData(CompoundTag compound) {
      ListTag list = new ListTag();

      for (int faction : this.factionData.keySet()) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128405_("Faction", faction);
         nbttagcompound.m_128405_("Points", this.factionData.get(faction));
         list.add(nbttagcompound);
      }

      compound.m_128365_("FactionData", list);
   }

   public int getFactionPoints(Player player, int factionId) {
      Faction faction = FactionController.instance.getFaction(factionId);
      if (faction == null) {
         return 0;
      }

      if (!this.factionData.containsKey(factionId)) {
         if (player.f_19853_.f_46443_) {
            this.factionData.put(factionId, faction.defaultPoints);
            return faction.defaultPoints;
         }

         PlayerScriptData handler = PlayerData.get(player).scriptData;
         PlayerWrapper wrapper = (PlayerWrapper)NpcAPI.Instance().getIEntity(player);
         PlayerEvent.FactionUpdateEvent event = new PlayerEvent.FactionUpdateEvent(wrapper, faction, faction.defaultPoints, true);
         EventHooks.OnPlayerFactionChange(handler, event);
         this.factionData.put(factionId, event.points);
         PlayerData data = PlayerData.get(player);
         data.updateClient = true;
      }

      return this.factionData.get(factionId);
   }

   public void increasePoints(Player player, int factionId, int points) {
      Faction faction = FactionController.instance.getFaction(factionId);
      if (faction != null && player != null && !player.f_19853_.f_46443_) {
         PlayerScriptData handler = PlayerData.get(player).scriptData;
         PlayerWrapper wrapper = (PlayerWrapper)NpcAPI.Instance().getIEntity(player);
         if (!this.factionData.containsKey(factionId)) {
            PlayerEvent.FactionUpdateEvent event = new PlayerEvent.FactionUpdateEvent(wrapper, faction, faction.defaultPoints, true);
            EventHooks.OnPlayerFactionChange(handler, event);
            this.factionData.put(factionId, event.points);
         }

         PlayerEvent.FactionUpdateEvent event = new PlayerEvent.FactionUpdateEvent(wrapper, faction, points, false);
         EventHooks.OnPlayerFactionChange(handler, event);
         this.factionData.put(factionId, this.factionData.get(factionId) + points);
      }
   }

   public CompoundTag getPlayerGuiData() {
      CompoundTag compound = new CompoundTag();
      this.saveNBTData(compound);
      ListTag list = new ListTag();

      for (int id : this.factionData.keySet()) {
         Faction faction = FactionController.instance.getFaction(id);
         if (faction != null && !faction.hideFaction) {
            CompoundTag com = new CompoundTag();
            faction.writeNBT(com);
            list.add(com);
         }
      }

      compound.m_128365_("FactionList", list);
      return compound;
   }
}
