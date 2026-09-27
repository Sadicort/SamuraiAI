package noppes.npcs.controllers.data;

import java.util.HashMap;
import java.util.Vector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class TransportCategory {
   public int id = -1;
   public String title = "";
   public HashMap<Integer, TransportLocation> locations = new HashMap<>();

   public Vector<TransportLocation> getDefaultLocations() {
      Vector<TransportLocation> list = new Vector<>();

      for (TransportLocation loc : this.locations.values()) {
         if (loc.isDefault()) {
            list.add(loc);
         }
      }

      return list;
   }

   public void readNBT(CompoundTag compound) {
      this.id = compound.m_128451_("CategoryId");
      this.title = compound.m_128461_("CategoryTitle");
      ListTag locs = compound.m_128437_("CategoryLocations", 10);
      if (locs != null && locs.size() != 0) {
         for (int ii = 0; ii < locs.size(); ii++) {
            TransportLocation location = new TransportLocation();
            location.readNBT(locs.m_128728_(ii));
            location.category = this;
            this.locations.put(location.id, location);
         }
      }
   }

   public void writeNBT(CompoundTag compound) {
      compound.m_128405_("CategoryId", this.id);
      compound.m_128359_("CategoryTitle", this.title);
      ListTag locs = new ListTag();

      for (TransportLocation location : this.locations.values()) {
         locs.add(location.writeNBT());
      }

      compound.m_128365_("CategoryLocations", locs);
   }
}
