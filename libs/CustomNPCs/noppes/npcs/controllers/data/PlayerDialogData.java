package noppes.npcs.controllers.data;

import java.util.HashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class PlayerDialogData {
   public HashSet<Integer> dialogsRead = new HashSet<>();

   public void loadNBTData(CompoundTag compound) {
      HashSet<Integer> dialogsRead = new HashSet<>();
      if (compound != null) {
         ListTag list = compound.m_128437_("DialogData", 10);
         if (list != null) {
            for (int i = 0; i < list.size(); i++) {
               CompoundTag nbttagcompound = list.m_128728_(i);
               dialogsRead.add(nbttagcompound.m_128451_("Dialog"));
            }

            this.dialogsRead = dialogsRead;
         }
      }
   }

   public void saveNBTData(CompoundTag compound) {
      ListTag list = new ListTag();

      for (int dia : this.dialogsRead) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128405_("Dialog", dia);
         list.add(nbttagcompound);
      }

      compound.m_128365_("DialogData", list);
   }
}
