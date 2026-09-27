package noppes.npcs.controllers.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import noppes.npcs.NBTTags;

public class SpawnData implements WeightedEntry {
   public List<ResourceLocation> biomes = new ArrayList<>();
   public int id = -1;
   public String name = "";
   public Map<Integer, CloneSpawnData> data = new HashMap<>();
   public boolean liquid = false;
   public int type = 0;
   private Weight weight = Weight.m_146282_(10);

   public void readNBT(CompoundTag compound) {
      this.id = compound.m_128451_("SpawnId");
      this.name = compound.m_128461_("SpawnName");
      this.setWeight(compound.m_128451_("SpawnWeight"));
      this.biomes = NBTTags.getResourceLocationList(compound.m_128437_("SpawnBiomes", 10));
      this.data = CloneSpawnData.load(compound.m_128437_("SpawnData", 10));
      this.type = compound.m_128451_("SpawnType");
   }

   public CompoundTag writeNBT(CompoundTag compound) {
      compound.m_128405_("SpawnId", this.id);
      compound.m_128359_("SpawnName", this.name);
      compound.m_128405_("SpawnWeight", this.weight.m_146281_());
      compound.m_128365_("SpawnBiomes", NBTTags.nbtResourceLocationList(this.biomes));
      compound.m_128365_("SpawnData", CloneSpawnData.save(this.data));
      compound.m_128405_("SpawnType", this.type);
      return compound;
   }

   public void setWeight(int weight) {
      if (weight == 0) {
         weight = 1;
      }

      this.weight = Weight.m_146282_(weight);
   }

   public void setClone(int slot, int tab, String name) {
      this.data.put(slot, new CloneSpawnData(tab, name));
   }

   public CompoundTag getCompound(int slot) {
      CloneSpawnData sd = this.data.get(slot);
      return sd == null ? null : sd.getCompound();
   }

   public Weight m_142631_() {
      return this.weight;
   }
}
