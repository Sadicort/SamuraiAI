package noppes.npcs;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import noppes.npcs.constants.EnumScriptType;
import noppes.npcs.controllers.ScriptContainer;
import noppes.npcs.controllers.data.Line;
import noppes.npcs.controllers.data.Lines;
import noppes.npcs.entity.EntityNPCInterface;

public class VersionCompatibility {
   public static int ModRev = 18;

   public static void CheckNpcCompatibility(EntityNPCInterface npc, CompoundTag compound) {
      if (npc.npcVersion != ModRev) {
         if (npc.npcVersion < 12) {
            CompatabilityFix(compound, npc.advanced.save(new CompoundTag()));
            CompatabilityFix(compound, npc.ais.save(new CompoundTag()));
            CompatabilityFix(compound, npc.stats.save(new CompoundTag()));
            CompatabilityFix(compound, npc.display.save(new CompoundTag()));
            CompatabilityFix(compound, npc.inventory.save(new CompoundTag()));
         }

         if (npc.npcVersion < 5) {
            String texture = compound.m_128461_("Texture");
            texture = texture.replace("/mob/customnpcs/", "customnpcs:textures/entity/");
            texture = texture.replace("/mob/", "customnpcs:textures/entity/");
            compound.m_128359_("Texture", texture);
         }

         if (npc.npcVersion < 6 && compound.m_128423_("NpcInteractLines") instanceof ListTag) {
            List<String> interactLines = NBTTags.getStringList(compound.m_128437_("NpcInteractLines", 10));
            Lines lines = new Lines();

            for (int i = 0; i < interactLines.size(); i++) {
               Line line = new Line();
               line.setText((String)interactLines.toArray()[i]);
               lines.lines.put(i, line);
            }

            compound.m_128365_("NpcInteractLines", lines.save());
            List<String> worldLines = NBTTags.getStringList(compound.m_128437_("NpcLines", 10));
            lines = new Lines();

            for (int i = 0; i < worldLines.size(); i++) {
               Line line = new Line();
               line.setText((String)worldLines.toArray()[i]);
               lines.lines.put(i, line);
            }

            compound.m_128365_("NpcLines", lines.save());
            List<String> attackLines = NBTTags.getStringList(compound.m_128437_("NpcAttackLines", 10));
            lines = new Lines();

            for (int i = 0; i < attackLines.size(); i++) {
               Line line = new Line();
               line.setText((String)attackLines.toArray()[i]);
               lines.lines.put(i, line);
            }

            compound.m_128365_("NpcAttackLines", lines.save());
            List<String> killedLines = NBTTags.getStringList(compound.m_128437_("NpcKilledLines", 10));
            lines = new Lines();

            for (int i = 0; i < killedLines.size(); i++) {
               Line line = new Line();
               line.setText((String)killedLines.toArray()[i]);
               lines.lines.put(i, line);
            }

            compound.m_128365_("NpcKilledLines", lines.save());
         }

         if (npc.npcVersion == 12) {
            ListTag list = compound.m_128437_("StartPos", 3);
            if (list.size() == 3) {
               int z = ((IntTag)list.remove(2)).m_7047_();
               int y = ((IntTag)list.remove(1)).m_7047_();
               int x = ((IntTag)list.remove(0)).m_7047_();
               compound.m_128385_("StartPosNew", new int[]{x, y, z});
            }
         }

         if (npc.npcVersion == 13) {
            boolean bo = compound.m_128471_("HealthRegen");
            compound.m_128405_("HealthRegen", bo ? 1 : 0);
            CompoundTag comp = compound.m_128469_("TransformStats");
            bo = comp.m_128471_("HealthRegen");
            comp.m_128405_("HealthRegen", bo ? 1 : 0);
            compound.m_128365_("TransformStats", comp);
         }

         if (npc.npcVersion == 15) {
            ListTag list = compound.m_128437_("ScriptsContainers", 10);
            if (list.size() > 0) {
               ScriptContainer script = new ScriptContainer(npc.script);

               for (int i = 0; i < list.size(); i++) {
                  CompoundTag scriptOld = list.m_128728_(i);
                  EnumScriptType type = EnumScriptType.values()[scriptOld.m_128451_("Type")];
                  script.script = script.script + "\nfunction " + type.function + "(event) {\n" + scriptOld.m_128461_("Script") + "\n}";

                  for (String s : NBTTags.getStringList(compound.m_128437_("ScriptList", 10))) {
                     if (!script.scripts.contains(s)) {
                        script.scripts.add(s);
                     }
                  }
               }
            }

            if (compound.m_128471_("CanDespawn")) {
               compound.m_128405_("SpawnCycle", 4);
            }

            if (compound.m_128451_("RangeAndMelee") <= 0) {
               compound.m_128405_("DistanceToMelee", 0);
            }
         }

         if (npc.npcVersion == 16) {
            compound.m_128359_("HitSound", "random.bowhit");
            compound.m_128359_("GroundSound", "random.break");
         }

         if (npc.npcVersion == 17) {
            if (compound.m_128461_("NpcHurtSound").equals("minecraft:game.player.hurt")) {
               compound.m_128359_("NpcHurtSound", "minecraft:entity.player.hurt");
            }

            if (compound.m_128461_("NpcDeathSound").equals("minecraft:game.player.hurt")) {
               compound.m_128359_("NpcDeathSound", "minecraft:entity.player.hurt");
            }

            if (compound.m_128461_("FiringSound").equals("random.bow")) {
               compound.m_128359_("FiringSound", "minecraft:entity.arrow.shoot");
            }

            if (compound.m_128461_("HitSound").equals("random.bowhit")) {
               compound.m_128359_("HitSound", "minecraft:entity.arrow.hit");
            }

            if (compound.m_128461_("GroundSound").equals("random.break")) {
               compound.m_128359_("GroundSound", "minecraft:block.stone.break");
            }
         }

         npc.npcVersion = ModRev;
      }
   }

   public static void CheckAvailabilityCompatibility(ICompatibilty compatibilty, CompoundTag compound) {
      if (compatibilty.getVersion() != ModRev) {
         CompatabilityFix(compound, compatibilty.save(new CompoundTag()));
         compatibilty.setVersion(ModRev);
      }
   }

   private static void CompatabilityFix(CompoundTag compound, CompoundTag check) {
      for (String name : check.m_128431_()) {
         Tag nbt = check.m_128423_(name);
         if (!compound.m_128441_(name)) {
            compound.m_128365_(name, nbt);
         } else if (nbt instanceof CompoundTag && compound.m_128423_(name) instanceof CompoundTag) {
            CompatabilityFix(compound.m_128469_(name), (CompoundTag)nbt);
         }
      }
   }
}
