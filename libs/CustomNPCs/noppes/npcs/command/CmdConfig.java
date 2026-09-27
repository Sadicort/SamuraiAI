package noppes.npcs.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.ChunkController;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketConfigFont;

public class CmdConfig {
   public static LiteralArgumentBuilder<CommandSourceStack> register() {
      LiteralArgumentBuilder<CommandSourceStack> command = Commands.m_82127_("config");
      command.then(
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("leavesdecay").requires(source -> source.m_6761_(4))).executes(context -> {
            ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("LeavesDecay: " + CustomNpcs.LeavesDecayEnabled), false);
            return 1;
         })).then(Commands.m_82129_("boolean", BoolArgumentType.bool()).executes(context -> {
            CustomNpcs.LeavesDecayEnabled = BoolArgumentType.getBool(context, "boolean");
            CustomNpcs.Config.updateConfig();
            ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("LeavesDecay: " + CustomNpcs.LeavesDecayEnabled), false);
            return 1;
         }))
      );
      command.then(
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("vineinflateth").requires(source -> source.m_6761_(4))).executes(context -> {
            ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("VineGrowth: " + CustomNpcs.VineGrowthEnabled), false);
            return 1;
         })).then(Commands.m_82129_("boolean", BoolArgumentType.bool()).executes(context -> {
            CustomNpcs.VineGrowthEnabled = BoolArgumentType.getBool(context, "boolean");
            CustomNpcs.Config.updateConfig();
            Set<ResourceLocation> names = ForgeRegistries.BLOCKS.getKeys();
            ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("VineGrowth: " + CustomNpcs.VineGrowthEnabled), false);
            return 1;
         }))
      );
      command.then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("icemelts").requires(source -> source.m_6761_(4))).executes(context -> {
         ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("IceMelts: " + CustomNpcs.IceMeltsEnabled), false);
         return 1;
      })).then(Commands.m_82129_("boolean", BoolArgumentType.bool()).executes(context -> {
         CustomNpcs.IceMeltsEnabled = BoolArgumentType.getBool(context, "boolean");
         CustomNpcs.Config.updateConfig();
         ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("IceMelts: " + CustomNpcs.IceMeltsEnabled), false);
         return 1;
      })));
      command.then(
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("freezenpcs").requires(source -> source.m_6761_(4))).executes(context -> {
            ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Frozen NPCs: " + CustomNpcs.FreezeNPCs), false);
            return 1;
         })).then(Commands.m_82129_("boolean", BoolArgumentType.bool()).executes(context -> {
            CustomNpcs.FreezeNPCs = BoolArgumentType.getBool(context, "boolean");
            ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Frozen NPCs: " + CustomNpcs.FreezeNPCs), false);
            return 1;
         }))
      );
      command.then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("debug").requires(source -> source.m_6761_(4))).executes(context -> {
         ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Verbose debug is " + CustomNpcs.VerboseDebug), false);
         return 1;
      })).then(Commands.m_82129_("boolean", BoolArgumentType.bool()).executes(context -> {
         CustomNpcs.VerboseDebug = BoolArgumentType.getBool(context, "boolean");
         ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Verbose debug is now" + CustomNpcs.VerboseDebug), false);
         return 1;
      })));
      command.then(
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("scripting").requires(source -> source.m_6761_(4))).executes(context -> {
            ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Scripting is " + CustomNpcs.EnableScripting), false);
            return 1;
         })).then(Commands.m_82129_("boolean", BoolArgumentType.bool()).executes(context -> {
            CustomNpcs.EnableScripting = BoolArgumentType.getBool(context, "boolean");
            CustomNpcs.Config.updateConfig();
            ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Scripting is now" + CustomNpcs.EnableScripting), false);
            return 1;
         }))
      );
      command.then(
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("chunkloaders").requires(source -> source.m_6761_(4)))
               .executes(
                  context -> {
                     ((CommandSourceStack)context.getSource())
                        .m_81354_(Component.m_237113_("ChunkLoaders: " + ChunkController.instance.size() + "/" + CustomNpcs.ChuckLoaders), false);
                     return 1;
                  }
               ))
            .then(Commands.m_82129_("number", IntegerArgumentType.integer(0)).executes(context -> {
               CustomNpcs.ChuckLoaders = IntegerArgumentType.getInteger(context, "number");
               CustomNpcs.Config.updateConfig();
               ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Max ChunkLoaders: " + CustomNpcs.ChuckLoaders), false);
               return 1;
            }))
      );
      command.then(
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("font").requires(source -> source.m_6761_(2))).executes(context -> {
               Packets.send(((CommandSourceStack)context.getSource()).m_81375_(), new PacketConfigFont("", 0));
               return 1;
            }))
            .then(
               ((RequiredArgumentBuilder)Commands.m_82129_("font", StringArgumentType.string()).executes(context -> {
                     Packets.send(((CommandSourceStack)context.getSource()).m_81375_(), new PacketConfigFont(StringArgumentType.getString(context, "font"), 18));
                     return 1;
                  }))
                  .then(
                     Commands.m_82129_("size", IntegerArgumentType.integer(0))
                        .executes(
                           context -> {
                              Packets.send(
                                 ((CommandSourceStack)context.getSource()).m_81375_(),
                                 new PacketConfigFont(StringArgumentType.getString(context, "font"), IntegerArgumentType.getInteger(context, "size"))
                              );
                              return 1;
                           }
                        )
                  )
            )
      );
      return command;
   }
}
