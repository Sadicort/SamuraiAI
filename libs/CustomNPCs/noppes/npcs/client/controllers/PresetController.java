package noppes.npcs.client.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import noppes.npcs.shared.common.util.LogWriter;

public class PresetController {
   public HashMap<String, Preset> presets = new HashMap<>();
   private File dir;
   public static PresetController instance;

   public PresetController(File dir) {
      instance = this;
      this.dir = dir;
      this.load();
   }

   public Preset getPreset(String username) {
      if (this.presets.isEmpty()) {
         this.load();
      }

      return this.presets.get(username.toLowerCase());
   }

   public void load() {
      CompoundTag compound = this.loadPreset();
      HashMap<String, Preset> presets = new HashMap<>();
      if (compound != null) {
         ListTag list = compound.m_128437_("Presets", 10);

         for (int i = 0; i < list.size(); i++) {
            CompoundTag comp = list.m_128728_(i);
            Preset preset = new Preset();
            preset.load(comp);
            presets.put(preset.name.toLowerCase(), preset);
         }
      }

      Preset.FillDefault(presets);
      this.presets = presets;
   }

   private CompoundTag loadPreset() {
      String filename = "presets.dat";

      try {
         File file = new File(this.dir, filename);
         return !file.exists() ? null : NbtIo.m_128939_(new FileInputStream(file));
      } catch (Exception e) {
         LogWriter.except(e);

         try {
            File file = new File(this.dir, filename + "_old");
            return !file.exists() ? null : NbtIo.m_128939_(new FileInputStream(file));
         } catch (Exception ex) {
            LogWriter.except(ex);
            return null;
         }
      }
   }

   public void save() {
      CompoundTag compound = new CompoundTag();
      ListTag list = new ListTag();

      for (Preset preset : this.presets.values()) {
         list.add(preset.save());
      }

      compound.m_128365_("Presets", list);
      this.savePreset(compound);
   }

   private void savePreset(CompoundTag compound) {
      String filename = "presets.dat";

      try {
         File file = new File(this.dir, filename + "_new");
         File file1 = new File(this.dir, filename + "_old");
         File file2 = new File(this.dir, filename);
         NbtIo.m_128947_(compound, new FileOutputStream(file));
         if (file1.exists()) {
            file1.delete();
         }

         file2.renameTo(file1);
         if (file2.exists()) {
            file2.delete();
         }

         file.renameTo(file2);
         if (file.exists()) {
            file.delete();
         }
      } catch (Exception e) {
         LogWriter.except(e);
      }
   }

   public void addPreset(Preset preset) {
      while (this.presets.containsKey(preset.name.toLowerCase())) {
         preset.name = preset.name + "_";
      }

      this.presets.put(preset.name.toLowerCase(), preset);
      this.save();
   }

   public void removePreset(String preset) {
      if (preset != null) {
         this.presets.remove(preset.toLowerCase());
         this.save();
      }
   }
}
