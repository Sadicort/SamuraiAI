package noppes.npcs.controllers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.level.LevelEvent.Save;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.api.wrapper.WorldWrapper;
import noppes.npcs.controllers.data.ForgeScriptData;
import noppes.npcs.controllers.data.PlayerScriptData;
import noppes.npcs.shared.common.util.LogWriter;
import noppes.npcs.util.NBTJsonUtil;

public class ScriptController {
   public static ScriptController Instance;
   public static boolean HasStart = false;
   private ScriptEngineManager manager;
   public Map<String, String> languages = new HashMap<>();
   public Map<String, ScriptEngineFactory> factories = new HashMap<>();
   public Map<String, String> scripts = new HashMap<>();
   public PlayerScriptData playerScripts = new PlayerScriptData(null);
   public ForgeScriptData forgeScripts = new ForgeScriptData();
   public long lastLoaded = 0L;
   public long lastPlayerUpdate = 0L;
   public File dir;
   public CompoundTag compound = new CompoundTag();
   private boolean loaded = false;
   public boolean shouldSave = false;

   public ScriptController() {
      this.loaded = false;
      Instance = this;
      if (!CustomNpcs.NashorArguments.isEmpty()) {
         System.setProperty("nashorn.args", CustomNpcs.NashorArguments);
      }

      LogWriter.info("Script Engines Available:");
      this.manager = new ScriptEngineManager();

      try {
         if (this.manager.getEngineByName("ecmascript") == null) {
            Class c = Class.forName("org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory");
            ScriptEngineFactory factory = (ScriptEngineFactory)c.newInstance();
            factory.getScriptEngine();
            LogWriter.info(factory.getLanguageName() + ": .js");
            this.manager.registerEngineName("ecmascript", factory);
            this.manager.registerEngineExtension("js", factory);
            this.manager.registerEngineMimeType("application/ecmascript", factory);
            this.languages.put(factory.getLanguageName(), ".js");
            this.factories.put(factory.getLanguageName().toLowerCase(), factory);
         }
      } catch (Throwable var7) {
      }

      try {
         Class c = Class.forName("org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngineFactory");
         ScriptEngineFactory factory = (ScriptEngineFactory)c.newInstance();
         factory.getScriptEngine();
         LogWriter.info(factory.getLanguageName() + ": .ktl");
         this.manager.registerEngineName("kotlin", factory);
         this.manager.registerEngineExtension("ktl", factory);
         this.manager.registerEngineMimeType("application/kotlin", factory);
         this.languages.put(factory.getLanguageName(), ".ktl");
         this.factories.put(factory.getLanguageName().toLowerCase(), factory);
      } catch (Throwable var6) {
      }

      try {
         Class c = Class.forName("noppes.scriptengines.ScriptEngines");

         for (ScriptEngineFactory fac : (List)c.getDeclaredField("factories").get(null)) {
            if (fac.getExtensions().size() != 0
               && !this.languages.containsKey(fac.getLanguageName())
               && (fac.getScriptEngine() instanceof Invocable || fac.getLanguageName().equals("lua"))) {
               String ext = "." + fac.getExtensions().get(0).toLowerCase();
               LogWriter.info(fac.getLanguageName() + ": " + ext);
               this.languages.put(fac.getLanguageName(), ext);
               this.factories.put(fac.getLanguageName().toLowerCase(), fac);
            }
         }
      } catch (Throwable var9) {
      }

      for (ScriptEngineFactory fac : this.manager.getEngineFactories()) {
         try {
            if (fac.getExtensions().size() != 0
               && !this.languages.containsKey(fac.getLanguageName())
               && (fac.getScriptEngine() instanceof Invocable || fac.getLanguageName().equals("lua"))) {
               String ext = "." + fac.getExtensions().get(0).toLowerCase();
               LogWriter.info(fac.getLanguageName() + ": " + ext);
               this.languages.put(fac.getLanguageName(), ext);
               this.factories.put(fac.getLanguageName().toLowerCase(), fac);
            }
         } catch (Throwable e) {
            LogWriter.except(e);
         }
      }
   }

   public void loadCategories() {
      this.dir = new File(CustomNpcs.getLevelSaveDirectory(), "scripts");
      if (!this.dir.exists()) {
         this.dir.mkdirs();
      }

      if (!this.worldDataFile().exists()) {
         this.shouldSave = true;
      }

      WorldWrapper.tempData.clear();
      this.scripts.clear();

      for (String language : this.languages.keySet()) {
         String ext = this.languages.get(language);
         File scriptDir = new File(this.dir, language.toLowerCase());
         if (!scriptDir.exists()) {
            scriptDir.mkdir();
         } else {
            this.loadDir(scriptDir, "", ext);
         }
      }

      this.lastLoaded = System.currentTimeMillis();
   }

   private void loadDir(File dir, String name, String ext) {
      for (File file : dir.listFiles()) {
         String filename = name + file.getName().toLowerCase();
         if (file.isDirectory()) {
            this.loadDir(file, filename + "/", ext);
         } else if (filename.endsWith(ext)) {
            try {
               this.scripts.put(filename, this.readFile(file));
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
   }

   public boolean loadStoredData() {
      this.compound = new CompoundTag();
      File file = this.worldDataFile();

      try {
         if (!file.exists()) {
            return false;
         }

         this.compound = NBTJsonUtil.LoadFile(file);
         this.shouldSave = false;
         return true;
      } catch (Exception e) {
         LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
         return false;
      }
   }

   private File worldDataFile() {
      return new File(this.dir, "world_data.json");
   }

   private File playerScriptsFile() {
      return new File(this.dir, "player_scripts.json");
   }

   private File forgeScriptsFile() {
      return new File(this.dir, "forge_scripts.json");
   }

   public boolean loadPlayerScripts() {
      this.playerScripts.clear();
      File file = this.playerScriptsFile();

      try {
         if (!file.exists()) {
            return false;
         }

         this.playerScripts.load(NBTJsonUtil.LoadFile(file));
         return true;
      } catch (Exception e) {
         LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
         return false;
      }
   }

   public void setPlayerScripts(CompoundTag compound) {
      this.playerScripts.load(compound);
      File file = this.playerScriptsFile();

      try {
         NBTJsonUtil.SaveFile(file, compound);
         this.lastPlayerUpdate = System.currentTimeMillis();
      } catch (IOException e) {
         e.printStackTrace();
      } catch (NBTJsonUtil.JsonException e) {
         e.printStackTrace();
      }
   }

   public boolean loadForgeScripts() {
      this.forgeScripts.clear();
      File file = this.forgeScriptsFile();

      try {
         if (!file.exists()) {
            return false;
         }

         this.forgeScripts.load(NBTJsonUtil.LoadFile(file));
         return true;
      } catch (Exception e) {
         LogWriter.error("Error loading: " + file.getAbsolutePath(), e);
         return false;
      }
   }

   public void setForgeScripts(CompoundTag compound) {
      this.forgeScripts.load(compound);
      File file = this.forgeScriptsFile();

      try {
         NBTJsonUtil.SaveFile(file, compound);
         this.forgeScripts.lastInited = -1L;
      } catch (IOException e) {
         e.printStackTrace();
      } catch (NBTJsonUtil.JsonException e) {
         e.printStackTrace();
      }
   }

   private String readFile(File file) throws IOException {
      BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8"));

      try {
         StringBuilder sb = new StringBuilder();

         for (String line = br.readLine(); line != null; line = br.readLine()) {
            sb.append(line);
            sb.append("\n");
         }

         return sb.toString();
      } finally {
         br.close();
      }
   }

   public ScriptEngine getEngineByName(String language) {
      ScriptEngineFactory fac = this.factories.get(language.toLowerCase());
      return fac == null ? null : fac.getScriptEngine();
   }

   public ListTag nbtLanguages() {
      ListTag list = new ListTag();

      for (String language : this.languages.keySet()) {
         CompoundTag compound = new CompoundTag();
         ListTag scripts = new ListTag();

         for (String script : this.getScripts(language)) {
            scripts.add(StringTag.m_129297_(script));
         }

         compound.m_128365_("Scripts", scripts);
         compound.m_128359_("Language", language);
         list.add(compound);
      }

      return list;
   }

   private List<String> getScripts(String language) {
      List<String> list = new ArrayList<>();
      String ext = this.languages.get(language);
      if (ext == null) {
         return list;
      }

      for (String script : this.scripts.keySet()) {
         if (script.endsWith(ext)) {
            list.add(script);
         }
      }

      return list;
   }

   @SubscribeEvent
   public void saveLevel(Save event) {
      if (this.shouldSave && event.getLevel() instanceof ServerLevel && ((ServerLevel)event.getLevel()).m_46472_() == Level.f_46428_) {
         try {
            NBTJsonUtil.SaveFile(this.worldDataFile(), this.compound.m_6426_());
         } catch (Exception e) {
            LogWriter.except(e);
         }

         this.shouldSave = false;
      }
   }
}
