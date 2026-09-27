package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import noppes.npcs.controllers.ServerCloneController;
import noppes.npcs.entity.EntityNPCInterface;

public class CmdClone {
   public static LiteralArgumentBuilder<CommandSourceStack> register() {
      LiteralArgumentBuilder<CommandSourceStack> command = Commands.m_82127_("clone");
      command.then(
         ((LiteralArgumentBuilder)Commands.m_82127_("list").requires(source -> source.m_6761_(2)))
            .then(Commands.m_82129_("tab", IntegerArgumentType.integer(0)).executes(context -> {
               int tab = IntegerArgumentType.getInteger(context, "tab");
               ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("--- Stored NPCs --- (server side)"), false);

               for (String name : ServerCloneController.Instance.getClones(tab)) {
                  ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_(name), false);
               }

               ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("------------------------------------"), false);
               return 1;
            }))
      );
      command.then(
         ((LiteralArgumentBuilder)Commands.m_82127_("add").requires(source -> source.m_6761_(4)))
            .then(
               Commands.m_82129_("npc", StringArgumentType.string())
                  .then(((RequiredArgumentBuilder)Commands.m_82129_("tab", IntegerArgumentType.integer(0)).executes(context -> {
                     addClone(context, "");
                     return 1;
                  })).then(Commands.m_82129_("name", StringArgumentType.string()).executes(context -> {
                     addClone(context, StringArgumentType.getString(context, "name"));
                     return 1;
                  })))
            )
      );
      command.then(
         ((LiteralArgumentBuilder)Commands.m_82127_("remove").requires(source -> source.m_6761_(4)))
            .then(Commands.m_82129_("npc", StringArgumentType.string()).then(Commands.m_82129_("tab", IntegerArgumentType.integer(0)).executes(context -> {
               String nametodel = StringArgumentType.getString(context, "npc");
               int tab = IntegerArgumentType.getInteger(context, "tab");
               boolean deleted = false;

               for (String name : ServerCloneController.Instance.getClones(tab)) {
                  if (nametodel.equalsIgnoreCase(name)) {
                     ServerCloneController.Instance.removeClone(name, tab);
                     deleted = true;
                     break;
                  }
               }

               if (!deleted) {
                  throw new CommandRuntimeException(Component.m_237110_("Npc '%s' wasn't found", new Object[]{nametodel}));
               } else {
                  return 1;
               }
            })))
      );
      command.then(
         ((LiteralArgumentBuilder)Commands.m_82127_("spawn").requires(source -> source.m_6761_(2)))
            .then(
               Commands.m_82129_("npc", StringArgumentType.string())
                  .then(((RequiredArgumentBuilder)Commands.m_82129_("tab", IntegerArgumentType.integer(0)).executes(context -> {
                     spawnClone(context, new BlockPos(((CommandSourceStack)context.getSource()).m_81371_()), "");
                     return 1;
                  })).then(((RequiredArgumentBuilder)Commands.m_82129_("pos", BlockPosArgument.m_118239_()).executes(context -> {
                     spawnClone(context, BlockPosArgument.m_118242_(context, "pos"), "");
                     return 1;
                  })).then(Commands.m_82129_("display_name", StringArgumentType.string()).executes(context -> {
                     spawnClone(context, BlockPosArgument.m_118242_(context, "pos"), StringArgumentType.getString(context, "display_name"));
                     return 1;
                  }))))
            )
      );
      command.then(
         ((LiteralArgumentBuilder)Commands.m_82127_("grid").requires(source -> source.m_6761_(2)))
            .then(
               Commands.m_82129_("npc", StringArgumentType.string())
                  .then(
                     Commands.m_82129_("tab", IntegerArgumentType.integer(0))
                        .then(
                           Commands.m_82129_("length", IntegerArgumentType.integer())
                              .then(
                                 ((RequiredArgumentBuilder)Commands.m_82129_("width", IntegerArgumentType.integer()).executes(context -> {
                                       int length = IntegerArgumentType.getInteger(context, "length");
                                       int width = IntegerArgumentType.getInteger(context, "width");

                                       for (int x = 0; x < length; x++) {
                                          for (int z = 0; z < width; z++) {
                                             spawnClone(
                                                context, new BlockPos(((CommandSourceStack)context.getSource()).m_81371_()).m_7918_(length, 0, width), ""
                                             );
                                          }
                                       }

                                       return 1;
                                    }))
                                    .then(
                                       ((RequiredArgumentBuilder)Commands.m_82129_("pos", BlockPosArgument.m_118239_()).executes(context -> {
                                             int length = IntegerArgumentType.getInteger(context, "length");
                                             int width = IntegerArgumentType.getInteger(context, "width");

                                             for (int x = 0; x < length; x++) {
                                                for (int z = 0; z < width; z++) {
                                                   spawnClone(context, BlockPosArgument.m_118242_(context, "pos").m_7918_(length, 0, width), "");
                                                }
                                             }

                                             return 1;
                                          }))
                                          .then(
                                             Commands.m_82129_("display_name", StringArgumentType.string())
                                                .executes(
                                                   context -> {
                                                      int length = IntegerArgumentType.getInteger(context, "length");
                                                      int width = IntegerArgumentType.getInteger(context, "width");

                                                      for (int x = 0; x < length; x++) {
                                                         for (int z = 0; z < width; z++) {
                                                            spawnClone(
                                                               context,
                                                               BlockPosArgument.m_118242_(context, "pos").m_7918_(length, 0, width),
                                                               StringArgumentType.getString(context, "display_name")
                                                            );
                                                         }
                                                      }

                                                      return 1;
                                                   }
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
      return command;
   }

   private static void addClone(CommandContext<CommandSourceStack> context, String newName) {
      String name = StringArgumentType.getString(context, "npc");
      if (newName.isEmpty()) {
         newName = name;
      }

      int tab = IntegerArgumentType.getInteger(context, "tab");
      List<EntityNPCInterface> list = CmdNoppes.getNpcsByName(((CommandSourceStack)context.getSource()).m_81372_(), name);
      if (!list.isEmpty()) {
         EntityNPCInterface npc = list.get(0);
         CompoundTag compound = new CompoundTag();
         if (npc.m_20086_(compound)) {
            ServerCloneController.Instance.addClone(compound, newName, tab);
         }
      }
   }

   private static void spawnClone(CommandContext<CommandSourceStack> context, BlockPos pos, String newName) {
      String name = StringArgumentType.getString(context, "npc").replaceAll("%", " ");
      int tab = IntegerArgumentType.getInteger(context, "tab");
      CompoundTag compound = ServerCloneController.Instance.getCloneData((CommandSourceStack)context.getSource(), name, tab);
      if (compound == null) {
         throw new CommandRuntimeException(Component.m_237113_("Unknown npc"));
      }

      if (pos == BlockPos.f_121853_) {
         throw new CommandRuntimeException(Component.m_237113_("Location needed"));
      }

      Level world = ((CommandSourceStack)context.getSource()).m_81372_();
      Entity entity = (Entity)EntityType.m_20642_(compound, world).get();
      entity.m_6034_(pos.m_123341_() + 0.5, pos.m_123342_() + 1, pos.m_123343_() + 0.5);
      if (entity instanceof EntityNPCInterface npc) {
         npc.ais.setStartPos(pos);
         if (!newName.isEmpty()) {
            npc.display.setName(newName.replaceAll("%", " "));
         }
      }

      world.m_7967_(entity);
   }
}
