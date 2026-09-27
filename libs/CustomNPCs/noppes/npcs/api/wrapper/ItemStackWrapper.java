package noppes.npcs.api.wrapper;

import com.google.common.collect.Multimap;
import com.google.gson.JsonParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.ItemStackEmptyWrapper;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.INbt;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IMob;
import noppes.npcs.api.entity.data.IData;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.items.ItemScripted;

public class ItemStackWrapper implements IItemStack, ICapabilitySerializable<CompoundTag> {
   private Map<String, Object> tempData = new HashMap<>();
   public static Capability<ItemStackWrapper> ITEMSCRIPTEDDATA_CAPABILITY = CapabilityManager.get(new CapabilityToken<ItemStackWrapper>() {});
   private LazyOptional<ItemStackWrapper> instance = LazyOptional.of(() -> this);
   private static final EquipmentSlot[] VALID_EQUIPMENT_SLOTS = new EquipmentSlot[]{
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
   };
   public ItemStack item;
   private CompoundTag storedData = new CompoundTag();
   public static ItemStackWrapper AIR = new ItemStackEmptyWrapper();
   private final IData tempdata = new IData() {
      @Override
      public void put(String key, Object value) {
         ItemStackWrapper.this.tempData.put(key, value);
      }

      @Override
      public Object get(String key) {
         return ItemStackWrapper.this.tempData.get(key);
      }

      @Override
      public void remove(String key) {
         ItemStackWrapper.this.tempData.remove(key);
      }

      @Override
      public boolean has(String key) {
         return ItemStackWrapper.this.tempData.containsKey(key);
      }

      @Override
      public void clear() {
         ItemStackWrapper.this.tempData.clear();
      }

      @Override
      public String[] getKeys() {
         return ItemStackWrapper.this.tempData.keySet().toArray(new String[ItemStackWrapper.this.tempData.size()]);
      }
   };
   private final IData storeddata = new IData() {
      @Override
      public void put(String key, Object value) {
         if (value instanceof Number) {
            ItemStackWrapper.this.storedData.m_128347_(key, ((Number)value).doubleValue());
         } else if (value instanceof String) {
            ItemStackWrapper.this.storedData.m_128359_(key, (String)value);
         }
      }

      @Override
      public Object get(String key) {
         if (!ItemStackWrapper.this.storedData.m_128441_(key)) {
            return null;
         }

         Tag base = ItemStackWrapper.this.storedData.m_128423_(key);
         return base instanceof NumericTag ? ((NumericTag)base).m_7061_() : base.m_7916_();
      }

      @Override
      public void remove(String key) {
         ItemStackWrapper.this.storedData.m_128473_(key);
      }

      @Override
      public boolean has(String key) {
         return ItemStackWrapper.this.storedData.m_128441_(key);
      }

      @Override
      public void clear() {
         ItemStackWrapper.this.storedData = new CompoundTag();
      }

      @Override
      public String[] getKeys() {
         return ItemStackWrapper.this.storedData.m_128431_().toArray(new String[ItemStackWrapper.this.storedData.m_128431_().size()]);
      }
   };
   private static final ResourceLocation key = new ResourceLocation("customnpcs", "itemscripteddata");

   protected ItemStackWrapper(ItemStack item) {
      this.item = item;
   }

   @Override
   public IData getTempdata() {
      return this.tempdata;
   }

   @Override
   public IData getStoreddata() {
      return this.storeddata;
   }

   @Override
   public int getStackSize() {
      return this.item.m_41613_();
   }

   @Override
   public void setStackSize(int size) {
      if (size > this.getMaxStackSize()) {
         throw new CustomNPCsException("Can't set the stacksize bigger than MaxStacksize");
      }

      this.item.m_41764_(size);
   }

   @Override
   public void setAttribute(String name, double value) {
      this.setAttribute(name, value, -1);
   }

   @Override
   public void setAttribute(String name, double value, int slot) {
      if (slot >= -1 && slot <= 5) {
         CompoundTag compound = this.item.m_41783_();
         if (compound == null) {
            this.item.m_41751_(compound = new CompoundTag());
         }

         ListTag nbttaglist = compound.m_128437_("AttributeModifiers", 10);
         ListTag newList = new ListTag();

         for (int i = 0; i < nbttaglist.size(); i++) {
            CompoundTag c = nbttaglist.m_128728_(i);
            if (!c.m_128461_("AttributeName").equals(name)) {
               newList.add(c);
            }
         }

         if (value != 0.0) {
            CompoundTag nbttagcompound = new AttributeModifier(name, value, Operation.ADDITION).m_22219_();
            nbttagcompound.m_128359_("AttributeName", name);
            if (slot >= 0) {
               nbttagcompound.m_128359_("Slot", EquipmentSlot.values()[slot].m_20751_());
            }

            newList.add(nbttagcompound);
         }

         compound.m_128365_("AttributeModifiers", newList);
      } else {
         throw new CustomNPCsException("Slot has to be between -1 and 5, given was: " + slot);
      }
   }

   @Override
   public double getAttribute(String name) {
      CompoundTag compound = this.item.m_41783_();
      if (compound == null) {
         return 0.0;
      }

      Multimap<Attribute, AttributeModifier> map = this.item.m_41638_(EquipmentSlot.MAINHAND);

      for (Entry<Attribute, AttributeModifier> entry : map.entries()) {
         if (entry.getKey().m_22087_().equals(name)) {
            AttributeModifier mod = entry.getValue();
            return mod.m_22218_();
         }
      }

      return 0.0;
   }

   @Override
   public boolean hasAttribute(String name) {
      CompoundTag compound = this.item.m_41783_();
      if (compound == null) {
         return false;
      }

      ListTag nbttaglist = compound.m_128437_("AttributeModifiers", 10);

      for (int i = 0; i < nbttaglist.size(); i++) {
         CompoundTag c = nbttaglist.m_128728_(i);
         if (c.m_128461_("AttributeName").equals(name)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void addEnchantment(String id, int strenght) {
      Enchantment ench = (Enchantment)ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(id));
      if (ench == null) {
         throw new CustomNPCsException("Unknown enchant id:" + id);
      }

      this.item.m_41663_(ench, strenght);
   }

   @Override
   public boolean isEnchanted() {
      return this.item.m_41793_();
   }

   @Override
   public boolean hasEnchant(String id) {
      Enchantment ench = (Enchantment)ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(id));
      if (ench == null) {
         throw new CustomNPCsException("Unknown enchant id:" + id);
      }

      if (!this.isEnchanted()) {
         return false;
      }

      ListTag list = this.item.m_41785_();

      for (int i = 0; i < list.size(); i++) {
         CompoundTag compound = list.m_128728_(i);
         if (compound.m_128461_("id").equalsIgnoreCase(id)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public boolean removeEnchant(String id) {
      Enchantment ench = (Enchantment)ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(id));
      if (ench == null) {
         throw new CustomNPCsException("Unknown enchant id:" + id);
      }

      if (!this.isEnchanted()) {
         return false;
      }

      ListTag list = this.item.m_41785_();
      ListTag newList = new ListTag();

      for (int i = 0; i < list.size(); i++) {
         CompoundTag compound = list.m_128728_(i);
         if (!compound.m_128461_("id").equalsIgnoreCase(id)) {
            newList.add(compound);
         }
      }

      if (list.size() == newList.size()) {
         return false;
      }

      this.item.m_41783_().m_128365_("ench", newList);
      return true;
   }

   @Override
   public boolean isBlock() {
      Block block = Block.m_49814_(this.item.m_41720_());
      return block != null && block != Blocks.f_50016_;
   }

   @Override
   public boolean hasCustomName() {
      return this.item.m_41788_();
   }

   @Override
   public void setCustomName(String name) {
      this.item.m_41714_(Component.m_237115_(name));
   }

   @Override
   public String getDisplayName() {
      return this.item.m_41786_().getString();
   }

   @Override
   public String getItemName() {
      return this.item.m_41720_().m_7626_(this.item).getString();
   }

   @Override
   public String getName() {
      return ForgeRegistries.ITEMS.getKey(this.item.m_41720_()).toString();
   }

   @Override
   public INbt getNbt() {
      CompoundTag compound = this.item.m_41783_();
      if (compound == null) {
         this.item.m_41751_(compound = new CompoundTag());
      }

      return NpcAPI.Instance().getINbt(compound);
   }

   @Override
   public boolean hasNbt() {
      CompoundTag compound = this.item.m_41783_();
      return compound != null && !compound.m_128456_();
   }

   @Override
   public ItemStack getMCItemStack() {
      return this.item;
   }

   public static ItemStack MCItem(IItemStack item) {
      return item == null ? ItemStack.f_41583_ : item.getMCItemStack();
   }

   @Override
   public void damageItem(int damage, IMob living) {
      if (living != null) {
         this.item.m_41622_(damage, living == null ? null : living.getMCEntity(), e -> e.m_21166_(EquipmentSlot.MAINHAND));
      } else if (this.item.m_41763_()) {
         if (this.item.m_41773_() <= damage) {
            this.item.m_41774_(1);
            this.item.m_41721_(0);
         } else {
            this.item.m_41721_(this.item.m_41773_() - damage);
         }
      }
   }

   @Override
   public boolean isBook() {
      return false;
   }

   @Override
   public int getFoodLevel() {
      return this.item.m_41720_().m_41473_() != null ? this.item.m_41720_().m_41473_().m_38744_() : 0;
   }

   @Override
   public IItemStack copy() {
      return createNew(this.item.m_41777_());
   }

   @Override
   public int getMaxStackSize() {
      return this.item.m_41741_();
   }

   @Override
   public boolean isDamageable() {
      return this.item.m_41763_();
   }

   @Override
   public int getDamage() {
      return this.item.m_41773_();
   }

   @Override
   public void setDamage(int value) {
      this.item.m_41721_(value);
   }

   @Deprecated
   public int getItemDamage() {
      return this.item.m_41773_();
   }

   @Deprecated
   public void setItemDamage(int value) {
      this.item.m_41721_(value);
   }

   @Override
   public int getMaxDamage() {
      return this.item.m_41776_();
   }

   @Override
   public INbt getItemNbt() {
      CompoundTag compound = new CompoundTag();
      this.item.m_41739_(compound);
      return NpcAPI.Instance().getINbt(compound);
   }

   @Override
   public double getAttackDamage() {
      Multimap<Attribute, AttributeModifier> map = this.item.m_41638_(EquipmentSlot.MAINHAND);
      double damage = 0.0;

      for (Entry<Attribute, AttributeModifier> entry : map.entries()) {
         if (entry.getKey() == Attributes.f_22281_) {
            AttributeModifier mod = entry.getValue();
            damage = mod.m_22218_();
         }
      }

      return damage + EnchantmentHelper.m_44833_(this.item, MobType.f_21640_);
   }

   @Override
   public boolean isEmpty() {
      return this.item.m_41619_();
   }

   @Override
   public int getType() {
      if (this.item.m_41720_() instanceof IPlantable) {
         return 5;
      } else {
         return this.item.m_41720_() instanceof SwordItem ? 4 : 0;
      }
   }

   @Override
   public boolean isWearable() {
      for (EquipmentSlot slot : VALID_EQUIPMENT_SLOTS) {
         if (this.item.m_41720_().canEquip(this.item, slot, EntityNPCInterface.CommandPlayer)) {
            return true;
         }
      }

      return false;
   }

   public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
      return capability == ITEMSCRIPTEDDATA_CAPABILITY ? this.instance.cast() : LazyOptional.empty();
   }

   public static void register(AttachCapabilitiesEvent<ItemStack> event) {
      ItemStackWrapper wrapper = createNew((ItemStack)event.getObject());
      event.addCapability(key, wrapper);
   }

   private static ItemStackWrapper createNew(ItemStack item) {
      if (item == null || item.m_41619_()) {
         return AIR;
      }

      if (item.m_41720_() instanceof ItemScripted) {
         return new ItemScriptedWrapper(item);
      }

      if (item.m_41720_() == Items.f_42615_
         || item.m_41720_() == Items.f_42614_
         || item.m_41720_() instanceof WritableBookItem
         || item.m_41720_() instanceof WrittenBookItem) {
         return new ItemBookWrapper(item);
      }

      if (item.m_41720_() instanceof ArmorItem) {
         return new ItemArmorWrapper(item);
      }

      Block block = Block.m_49814_(item.m_41720_());
      return block != Blocks.f_50016_ ? new ItemBlockWrapper(item) : new ItemStackWrapper(item);
   }

   @Override
   public String[] getLore() {
      CompoundTag compound = this.item.m_41737_("display");
      if (compound != null && compound.m_128435_("Lore") == 9) {
         ListTag nbttaglist = compound.m_128437_("Lore", 8);
         if (nbttaglist.isEmpty()) {
            return new String[0];
         }

         List<String> lore = new ArrayList<>();

         for (int i = 0; i < nbttaglist.size(); i++) {
            lore.add(nbttaglist.m_128778_(i));
         }

         return lore.toArray(new String[lore.size()]);
      } else {
         return new String[0];
      }
   }

   @Override
   public void setLore(String[] lore) {
      CompoundTag compound = this.item.m_41698_("display");
      if (lore != null && lore.length != 0) {
         ListTag nbtlist = new ListTag();

         for (String s : lore) {
            try {
               Serializer.m_130701_(s);
            } catch (JsonParseException jsonparseexception) {
               s = Serializer.m_130703_(Component.m_237115_(s));
            }

            nbtlist.add(StringTag.m_129297_(s));
         }

         compound.m_128365_("Lore", nbtlist);
      } else {
         compound.m_128473_("Lore");
      }
   }

   public CompoundTag serializeNBT() {
      return this.getMCNbt();
   }

   public void deserializeNBT(CompoundTag nbt) {
      this.setMCNbt(nbt);
   }

   public CompoundTag getMCNbt() {
      CompoundTag compound = new CompoundTag();
      if (!this.storedData.m_128456_()) {
         compound.m_128365_("StoredData", this.storedData);
      }

      return compound;
   }

   public void setMCNbt(CompoundTag compound) {
      if (compound == null) {
         this.storedData = new CompoundTag();
      } else {
         this.storedData = compound.m_128469_("StoredData");
      }
   }

   @Override
   public void removeNbt() {
      this.item.m_41751_(null);
   }

   @Override
   public boolean compare(IItemStack item, boolean ignoreNBT) {
      if (item == null) {
         item = AIR;
      }

      return NoppesUtilPlayer.compareItems(this.getMCItemStack(), item.getMCItemStack(), false, ignoreNBT);
   }
}
