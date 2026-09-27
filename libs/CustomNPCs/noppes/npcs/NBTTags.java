package noppes.npcs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.controllers.IScriptHandler;
import noppes.npcs.controllers.ScriptContainer;

public class NBTTags {
   public static void getItemStackList(ListTag tagList, NonNullList<ItemStack> items) {
      items.clear();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);

         try {
            items.set(nbttagcompound.m_128445_("Slot") & 255, ItemStack.m_41712_(nbttagcompound));
         } catch (ClassCastException e) {
            items.set(nbttagcompound.m_128451_("Slot"), ItemStack.m_41712_(nbttagcompound));
         }
      }
   }

   public static Map<Integer, IItemStack> getIItemStackMap(ListTag tagList) {
      Map<Integer, IItemStack> list = new HashMap<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         ItemStack item = ItemStack.m_41712_(nbttagcompound);
         if (!item.m_41619_()) {
            try {
               list.put(nbttagcompound.m_128445_("Slot") & 255, NpcAPI.Instance().getIItemStack(item));
            } catch (ClassCastException e) {
               list.put(nbttagcompound.m_128451_("Slot"), NpcAPI.Instance().getIItemStack(item));
            }
         }
      }

      return list;
   }

   public static ItemStack[] getItemStackArray(ListTag tagList) {
      ItemStack[] list = new ItemStack[tagList.size()];

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list[nbttagcompound.m_128445_("Slot") & 0xFF] = ItemStack.m_41712_(nbttagcompound);
      }

      return list;
   }

   public static NonNullList<Ingredient> getIngredientList(ListTag tagList) {
      NonNullList<Ingredient> list = NonNullList.m_122779_();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list.add(nbttagcompound.m_128445_("Slot") & 255, Ingredient.m_43927_(new ItemStack[]{ItemStack.m_41712_(nbttagcompound)}));
      }

      return list;
   }

   public static ArrayList<int[]> getIntegerArraySet(ListTag tagList) {
      ArrayList<int[]> set = new ArrayList<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag compound = tagList.m_128728_(i);
         set.add(compound.m_128465_("Array"));
      }

      return set;
   }

   public static HashMap<Integer, Boolean> getBooleanList(ListTag tagList) {
      HashMap<Integer, Boolean> list = new HashMap<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list.put(nbttagcompound.m_128451_("Slot"), nbttagcompound.m_128471_("Boolean"));
      }

      return list;
   }

   public static HashMap<Integer, Integer> getIntegerIntegerMap(ListTag tagList) {
      HashMap<Integer, Integer> list = new HashMap<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list.put(nbttagcompound.m_128451_("Slot"), nbttagcompound.m_128451_("Integer"));
      }

      return list;
   }

   public static HashMap<Integer, Float> getFloatIntegerMap(ListTag tagList) {
      HashMap<Integer, Float> list = new HashMap<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list.put(nbttagcompound.m_128451_("Slot"), nbttagcompound.m_128457_("Integer"));
      }

      return list;
   }

   public static HashMap<Integer, Long> getIntegerLongMap(ListTag tagList) {
      HashMap<Integer, Long> list = new HashMap<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list.put(nbttagcompound.m_128451_("Slot"), nbttagcompound.m_128454_("Long"));
      }

      return list;
   }

   public static HashSet<Integer> getIntegerSet(ListTag tagList) {
      HashSet<Integer> list = new HashSet<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list.add(nbttagcompound.m_128451_("Integer"));
      }

      return list;
   }

   public static List<Integer> getIntegerList(ListTag tagList) {
      List<Integer> list = new ArrayList<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list.add(nbttagcompound.m_128451_("Integer"));
      }

      return list;
   }

   public static HashMap<String, String> getStringStringMap(ListTag tagList) {
      HashMap<String, String> list = new HashMap<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list.put(nbttagcompound.m_128461_("Slot"), nbttagcompound.m_128461_("Value"));
      }

      return list;
   }

   public static HashMap<Integer, String> getIntegerStringMap(ListTag tagList) {
      HashMap<Integer, String> list = new HashMap<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list.put(nbttagcompound.m_128451_("Slot"), nbttagcompound.m_128461_("Value"));
      }

      return list;
   }

   public static HashMap<String, Integer> getStringIntegerMap(ListTag tagList) {
      HashMap<String, Integer> list = new HashMap<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list.put(nbttagcompound.m_128461_("Slot"), nbttagcompound.m_128451_("Value"));
      }

      return list;
   }

   public static HashMap<String, Vector<String>> getVectorMap(ListTag tagList) {
      HashMap<String, Vector<String>> map = new HashMap<>();

      for (int i = 0; i < tagList.size(); i++) {
         Vector<String> values = new Vector<>();
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         ListTag list = nbttagcompound.m_128437_("Values", 10);

         for (int j = 0; j < list.size(); j++) {
            CompoundTag value = list.m_128728_(j);
            values.add(value.m_128461_("Value"));
         }

         map.put(nbttagcompound.m_128461_("Key"), values);
      }

      return map;
   }

   public static List<String> getStringList(ListTag tagList) {
      List<String> list = new ArrayList<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         String line = nbttagcompound.m_128461_("Line");
         list.add(line);
      }

      return list;
   }

   public static List<ResourceLocation> getResourceLocationList(ListTag tagList) {
      List<ResourceLocation> list = new ArrayList<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         ResourceLocation line = new ResourceLocation(nbttagcompound.m_128461_("Line"));
         list.add(line);
      }

      return list;
   }

   public static String[] getStringArray(ListTag tagList, int size) {
      String[] arr = new String[size];

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         String line = nbttagcompound.m_128461_("Value");
         int slot = nbttagcompound.m_128451_("Slot");
         arr[slot] = line;
      }

      return arr;
   }

   public static ListTag nbtIntegerArraySet(List<int[]> set) {
      ListTag nbttaglist = new ListTag();
      if (set == null) {
         return nbttaglist;
      }

      for (int[] arr : set) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128385_("Array", arr);
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtItemStackList(NonNullList<ItemStack> inventory) {
      ListTag nbttaglist = new ListTag();

      for (int slot = 0; slot < inventory.size(); slot++) {
         ItemStack item = (ItemStack)inventory.get(slot);
         if (!item.m_41619_()) {
            CompoundTag nbttagcompound = new CompoundTag();
            nbttagcompound.m_128344_("Slot", (byte)slot);
            item.m_41739_(nbttagcompound);
            nbttaglist.add(nbttagcompound);
         }
      }

      return nbttaglist;
   }

   public static ListTag nbtIItemStackMap(Map<Integer, IItemStack> inventory) {
      ListTag nbttaglist = new ListTag();
      if (inventory == null) {
         return nbttaglist;
      }

      for (int slot : inventory.keySet()) {
         IItemStack item = inventory.get(slot);
         if (item != null) {
            CompoundTag nbttagcompound = new CompoundTag();
            nbttagcompound.m_128344_("Slot", (byte)slot);
            item.getMCItemStack().m_41739_(nbttagcompound);
            nbttaglist.add(nbttagcompound);
         }
      }

      return nbttaglist;
   }

   public static ListTag nbtItemStackArray(ItemStack[] inventory) {
      ListTag nbttaglist = new ListTag();
      if (inventory == null) {
         return nbttaglist;
      }

      for (int slot = 0; slot < inventory.length; slot++) {
         ItemStack item = inventory[slot];
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128344_("Slot", (byte)slot);
         if (item != null) {
            item.m_41739_(nbttagcompound);
         }

         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtIngredientList(NonNullList<Ingredient> inventory) {
      ListTag nbttaglist = new ListTag();
      if (inventory == null) {
         return nbttaglist;
      }

      for (int slot = 0; slot < inventory.size(); slot++) {
         Ingredient ingredient = (Ingredient)inventory.get(slot);
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128344_("Slot", (byte)slot);
         if (ingredient != null && ingredient.m_43908_().length > 0) {
            ingredient.m_43908_()[0].m_41739_(nbttagcompound);
         }

         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtBooleanList(HashMap<Integer, Boolean> updatedSlots) {
      ListTag nbttaglist = new ListTag();
      if (updatedSlots == null) {
         return nbttaglist;
      }

      HashMap<Integer, Boolean> inventory2 = updatedSlots;

      for (Integer slot : inventory2.keySet()) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128405_("Slot", slot);
         nbttagcompound.m_128379_("Boolean", inventory2.get(slot));
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtIntegerIntegerMap(Map<Integer, Integer> lines) {
      ListTag nbttaglist = new ListTag();
      if (lines == null) {
         return nbttaglist;
      }

      for (int slot : lines.keySet()) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128405_("Slot", slot);
         nbttagcompound.m_128405_("Integer", lines.get(slot));
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtFloatMap(Map<Integer, Float> lines) {
      ListTag nbttaglist = new ListTag();
      if (lines == null) {
         return nbttaglist;
      }

      for (int slot : lines.keySet()) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128405_("Slot", slot);
         nbttagcompound.m_128350_("Integer", lines.get(slot));
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtIntegerLongMap(HashMap<Integer, Long> lines) {
      ListTag nbttaglist = new ListTag();
      if (lines == null) {
         return nbttaglist;
      }

      for (int slot : lines.keySet()) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128405_("Slot", slot);
         nbttagcompound.m_128356_("Long", lines.get(slot));
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtIntegerCollection(Collection<Integer> set) {
      ListTag nbttaglist = new ListTag();
      if (set == null) {
         return nbttaglist;
      }

      for (int slot : set) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128405_("Integer", slot);
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtVectorMap(HashMap<String, Vector<String>> map) {
      ListTag list = new ListTag();
      if (map == null) {
         return list;
      }

      for (String key : map.keySet()) {
         CompoundTag compound = new CompoundTag();
         compound.m_128359_("Key", key);
         ListTag values = new ListTag();

         for (String value : map.get(key)) {
            CompoundTag comp = new CompoundTag();
            comp.m_128359_("Value", value);
            values.add(comp);
         }

         compound.m_128365_("Values", values);
         list.add(compound);
      }

      return list;
   }

   public static ListTag nbtStringStringMap(HashMap<String, String> map) {
      ListTag nbttaglist = new ListTag();
      if (map == null) {
         return nbttaglist;
      }

      for (String slot : map.keySet()) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128359_("Slot", slot);
         nbttagcompound.m_128359_("Value", map.get(slot));
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtStringIntegerMap(Map<String, Integer> map) {
      ListTag nbttaglist = new ListTag();
      if (map == null) {
         return nbttaglist;
      }

      for (String slot : map.keySet()) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128359_("Slot", slot);
         nbttagcompound.m_128405_("Value", map.get(slot));
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static Tag nbtIntegerStringMap(Map<Integer, String> map) {
      ListTag nbttaglist = new ListTag();
      if (map == null) {
         return nbttaglist;
      }

      for (int slot : map.keySet()) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128405_("Slot", slot);
         nbttagcompound.m_128359_("Value", map.get(slot));
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtStringArray(String[] list) {
      ListTag nbttaglist = new ListTag();
      if (list == null) {
         return nbttaglist;
      }

      for (int i = 0; i < list.length; i++) {
         if (list[i] != null) {
            CompoundTag nbttagcompound = new CompoundTag();
            nbttagcompound.m_128359_("Value", list[i]);
            nbttagcompound.m_128405_("Slot", i);
            nbttaglist.add(nbttagcompound);
         }
      }

      return nbttaglist;
   }

   public static ListTag nbtStringList(List<String> list) {
      ListTag nbttaglist = new ListTag();

      for (String s : list) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128359_("Line", s);
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtResourceLocationList(List<ResourceLocation> list) {
      ListTag nbttaglist = new ListTag();

      for (ResourceLocation s : list) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128359_("Line", s.toString());
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }

   public static ListTag nbtDoubleList(double... par1ArrayOfDouble) {
      ListTag nbttaglist = new ListTag();
      double[] adouble = par1ArrayOfDouble;
      int i = par1ArrayOfDouble.length;

      for (int j = 0; j < i; j++) {
         double d1 = adouble[j];
         nbttaglist.add(DoubleTag.m_128500_(d1));
      }

      return nbttaglist;
   }

   public static CompoundTag NBTMerge(CompoundTag data, CompoundTag merge) {
      CompoundTag compound = data.m_6426_();

      for (String name : merge.m_128431_()) {
         Tag base = merge.m_128423_(name);
         if (base.m_7060_() == 10) {
            base = NBTMerge(compound.m_128469_(name), (CompoundTag)base);
         }

         compound.m_128365_(name, base);
      }

      return compound;
   }

   public static List<ScriptContainer> GetScript(ListTag list, IScriptHandler handler) {
      List<ScriptContainer> scripts = new ArrayList<>();

      for (int i = 0; i < list.size(); i++) {
         CompoundTag compoundd = list.m_128728_(i);
         ScriptContainer script = new ScriptContainer(handler);
         script.load(compoundd);
         scripts.add(script);
      }

      return scripts;
   }

   public static ListTag NBTScript(List<ScriptContainer> scripts) {
      ListTag list = new ListTag();

      for (ScriptContainer script : scripts) {
         CompoundTag compound = new CompoundTag();
         script.save(compound);
         list.add(compound);
      }

      return list;
   }

   public static TreeMap<Long, String> GetLongStringMap(ListTag tagList) {
      TreeMap<Long, String> list = new TreeMap<>();

      for (int i = 0; i < tagList.size(); i++) {
         CompoundTag nbttagcompound = tagList.m_128728_(i);
         list.put(nbttagcompound.m_128454_("Long"), nbttagcompound.m_128461_("String"));
      }

      return list;
   }

   public static ListTag NBTLongStringMap(Map<Long, String> map) {
      ListTag nbttaglist = new ListTag();
      if (map == null) {
         return nbttaglist;
      }

      for (long slot : map.keySet()) {
         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128356_("Long", slot);
         nbttagcompound.m_128359_("String", map.get(slot));
         nbttaglist.add(nbttagcompound);
      }

      return nbttaglist;
   }
}
