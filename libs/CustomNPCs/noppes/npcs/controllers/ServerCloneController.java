package noppes.npcs.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.handler.ICloneHandler;
import noppes.npcs.packets.server.SPacketToolMobSpawner;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.NBTJsonUtil;

public class ServerCloneController implements ICloneHandler {
   public long lastLoaded = System.currentTimeMillis();
   public static ServerCloneController Instance;

   public ServerCloneController() {
      this.loadClones();
   }

   private void loadClones() {
      try {
         File dir = new File(this.getDir(), "..");
         File file = new File(dir, "clonednpcs.dat");
         if (file.exists()) {
            Map<Integer, Map<String, CompoundTag>> clones = this.loadOldClones(file);
            file.delete();
            file = new File(dir, "clonednpcs.dat_old");
            if (file.exists()) {
               file.delete();
            }

            for (int tab : clones.keySet()) {
               Map<String, CompoundTag> map = clones.get(tab);

               for (String name : map.keySet()) {
                  this.saveClone(tab, name, map.get(name));
               }
            }
         }
      } catch (Exception e) {
         LogWriter.except(e);
      }
   }

   public File getDir() {
      File dir = new File(CustomNpcs.getLevelSaveDirectory(), "clones");
      if (!dir.exists()) {
         dir.mkdir();
      }

      return dir;
   }

   private Map<Integer, Map<String, CompoundTag>> loadOldClones(File file) throws Exception {
      Map<Integer, Map<String, CompoundTag>> clones = new HashMap<>();
      CompoundTag nbttagcompound1 = NbtIo.m_128939_(new FileInputStream(file));
      ListTag list = nbttagcompound1.m_128437_("Data", 10);
      if (list == null) {
         return clones;
      }

      for (int i = 0; i < list.size(); i++) {
         CompoundTag compound = list.m_128728_(i);
         if (!compound.m_128441_("ClonedTab")) {
            compound.m_128405_("ClonedTab", 1);
         }

         Map<String, CompoundTag> tab = clones.get(compound.m_128451_("ClonedTab"));
         if (tab == null) {
            clones.put(compound.m_128451_("ClonedTab"), tab = new HashMap<>());
         }

         String name = compound.m_128461_("ClonedName");
         int number = 1;

         while (tab.containsKey(name)) {
            name = String.format("%s%s", compound.m_128461_("ClonedName"), ++number);
         }

         compound.m_128473_("ClonedName");
         compound.m_128473_("ClonedTab");
         compound.m_128473_("ClonedDate");
         this.cleanTags(compound);
         tab.put(name, compound);
      }

      return clones;
   }

   public CompoundTag getCloneData(CommandSourceStack player, String name, int tab) {
      File file = new File(new File(this.getDir(), tab + ""), name + ".json");
      if (!file.exists()) {
         if (player != null) {
            player.m_81352_(Component.m_237113_("Could not find clone file"));
         }

         return null;
      } else {
         try {
            return NBTJsonUtil.LoadFile(file);
         } catch (Exception e) {
            LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
            if (player != null) {
               player.m_81352_(Component.m_237113_(e.getMessage()));
            }

            return null;
         }
      }
   }

   public void saveClone(int tab, String name, CompoundTag compound) {
      try {
         File dir = new File(this.getDir(), tab + "");
         if (!dir.exists()) {
            dir.mkdir();
         }

         String filename = name + ".json";
         File file = new File(dir, filename + "_new");
         File file2 = new File(dir, filename);
         NBTJsonUtil.SaveFile(file, compound);
         if (file2.exists()) {
            file2.delete();
         }

         file.renameTo(file2);
         this.lastLoaded = System.currentTimeMillis();
      } catch (Exception e) {
         LogWriter.except(e);
      }
   }

   public List<String> getClones(int tab) {
      List<String> list = new ArrayList<>();
      File dir = new File(this.getDir(), tab + "");
      if (dir.exists() && dir.isDirectory()) {
         for (String file : dir.list()) {
            if (file.endsWith(".json")) {
               list.add(file.substring(0, file.length() - 5));
            }
         }

         return list;
      } else {
         return list;
      }
   }

   public boolean removeClone(String name, int tab) {
      File file = new File(new File(this.getDir(), tab + ""), name + ".json");
      if (!file.exists()) {
         return false;
      }

      file.delete();
      return true;
   }

   public String addClone(CompoundTag nbttagcompound, String name, int tab) {
      this.cleanTags(nbttagcompound);
      this.saveClone(tab, name, nbttagcompound);
      return name;
   }

   public void cleanTags(CompoundTag nbttagcompound) {
      if (nbttagcompound.m_128441_("ItemGiverId")) {
         nbttagcompound.m_128405_("ItemGiverId", 0);
      }

      if (nbttagcompound.m_128441_("TransporterId")) {
         nbttagcompound.m_128405_("TransporterId", -1);
      }

      nbttagcompound.m_128473_("StartPosNew");
      nbttagcompound.m_128473_("StartPos");
      nbttagcompound.m_128473_("MovingPathNew");
      nbttagcompound.m_128473_("Pos");
      nbttagcompound.m_128473_("Riding");
      nbttagcompound.m_128473_("UUID");
      nbttagcompound.m_128473_("UUIDMost");
      nbttagcompound.m_128473_("UUIDLeast");
      if (!nbttagcompound.m_128441_("ModRev")) {
         nbttagcompound.m_128405_("ModRev", 1);
      }

      if (nbttagcompound.m_128441_("TransformRole")) {
         CompoundTag adv = nbttagcompound.m_128469_("TransformRole");
         adv.m_128405_("TransporterId", -1);
         nbttagcompound.m_128365_("TransformRole", adv);
      }

      if (nbttagcompound.m_128441_("TransformJob")) {
         CompoundTag adv = nbttagcompound.m_128469_("TransformJob");
         adv.m_128405_("ItemGiverId", 0);
         nbttagcompound.m_128365_("TransformJob", adv);
      }

      if (nbttagcompound.m_128441_("TransformAI")) {
         CompoundTag adv = nbttagcompound.m_128469_("TransformAI");
         adv.m_128473_("StartPosNew");
         adv.m_128473_("StartPos");
         adv.m_128473_("MovingPathNew");
         nbttagcompound.m_128365_("TransformAI", adv);
      }

      if (nbttagcompound.m_128441_("id")) {
         String id = nbttagcompound.m_128461_("id");
         if (!CustomNpcs.FixUpdateFromPre_1_12) {
            id = id.replace("customnpcs.", "customnpcs:");
         }

         nbttagcompound.m_128359_("id", id);
      }
   }

   @Override
   public IEntity spawn(double x, double y, double z, int tab, String name, IWorld level) {
      CompoundTag compound = this.getCloneData(null, name, tab);
      if (compound == null) {
         throw new CustomNPCsException("Unknown clone tab:" + tab + " name:" + name);
      }

      Entity entity = SPacketToolMobSpawner.spawnClone(compound, x, y, z, level.getMCLevel());
      return entity == null ? null : NpcAPI.Instance().getIEntity(entity);
   }

   @Override
   public IEntity get(int tab, String name, IWorld level) {
      CompoundTag compound = this.getCloneData(null, name, tab);
      if (compound == null) {
         throw new CustomNPCsException("Unknown clone tab:" + tab + " name:" + name);
      }

      Instance.cleanTags(compound);
      Entity entity = (Entity)EntityType.m_20642_(compound, level.getMCLevel()).orElse(null);
      return entity == null ? null : NpcAPI.Instance().getIEntity(entity);
   }

   @Override
   public void set(int tab, String name, IEntity entity) {
      CompoundTag compound = new CompoundTag();
      if (!entity.getMCEntity().m_20086_(compound)) {
         throw new CustomNPCsException("Cannot save dead entities");
      }

      this.cleanTags(compound);
      this.saveClone(tab, name, compound);
   }

   @Override
   public void remove(int tab, String name) {
      this.removeClone(name, tab);
   }

   public boolean hasClone(int tab, String name) {
      return this.getCloneData(null, name, tab) != null;
   }
}
