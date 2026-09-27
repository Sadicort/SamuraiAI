package noppes.npcs.controllers.data;

import java.util.HashMap;
import net.minecraft.nbt.CompoundTag;
import noppes.npcs.NBTTags;
import noppes.npcs.NpcMiscInventory;

public class Bank {
   public int id = -1;
   public String name = "";
   public HashMap<Integer, Integer> slotTypes;
   public int startSlots = 1;
   public int maxSlots = 6;
   public NpcMiscInventory currencyInventory;
   public NpcMiscInventory upgradeInventory;

   public Bank() {
      this.slotTypes = new HashMap<>();
      this.currencyInventory = new NpcMiscInventory(6);
      this.upgradeInventory = new NpcMiscInventory(6);

      for (int i = 0; i < 6; i++) {
         this.slotTypes.put(i, 0);
      }
   }

   public void addAdditionalSaveData(CompoundTag nbttagcompound) {
      nbttagcompound.m_128405_("BankID", this.id);
      nbttagcompound.m_128365_("BankCurrency", this.currencyInventory.getToNBT());
      nbttagcompound.m_128365_("BankUpgrade", this.upgradeInventory.getToNBT());
      nbttagcompound.m_128359_("Username", this.name);
      nbttagcompound.m_128405_("MaxSlots", this.maxSlots);
      nbttagcompound.m_128405_("StartSlots", this.startSlots);
      nbttagcompound.m_128365_("BankTypes", NBTTags.nbtIntegerIntegerMap(this.slotTypes));
   }

   public void readAdditionalSaveData(CompoundTag nbttagcompound) {
      this.id = nbttagcompound.m_128451_("BankID");
      this.name = nbttagcompound.m_128461_("Username");
      this.startSlots = nbttagcompound.m_128451_("StartSlots");
      this.maxSlots = nbttagcompound.m_128451_("MaxSlots");
      this.slotTypes = NBTTags.getIntegerIntegerMap(nbttagcompound.m_128437_("BankTypes", 10));
      this.currencyInventory.setFromNBT(nbttagcompound.m_128469_("BankCurrency"));
      this.upgradeInventory.setFromNBT(nbttagcompound.m_128469_("BankUpgrade"));
   }

   public boolean isUpgraded(int slot) {
      return this.slotTypes.get(slot) != null && this.slotTypes.get(slot) == 2;
   }

   public boolean canBeUpgraded(int slot) {
      return this.upgradeInventory.m_8020_(slot) != null && !this.upgradeInventory.m_8020_(slot).m_41619_()
         ? this.slotTypes.get(slot) == null || this.slotTypes.get(slot) == 0
         : false;
   }

   public int getMaxSlots() {
      for (int i = 0; i < this.maxSlots; i++) {
         if ((this.currencyInventory.m_8020_(i) == null || this.currencyInventory.m_8020_(i).m_41619_()) && i > this.startSlots - 1) {
            return i;
         }
      }

      return this.maxSlots;
   }
}
