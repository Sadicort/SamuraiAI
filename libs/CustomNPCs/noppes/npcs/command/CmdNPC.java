package noppes.npcs.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.CustomEntities;
import noppes.npcs.CustomNpcs;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleCompanion;
import noppes.npcs.roles.RoleFollower;

public class CmdNPC {
   public static final SuggestionProvider<CommandSourceStack> VISIBLE = SuggestionProviders.m_121658_(
      new ResourceLocation("visible"), (context, builder) -> SharedSuggestionProvider.m_82967_(new String[]{"true", "false", "semi"}, builder)
   );

   public static LiteralArgumentBuilder<CommandSourceStack> register() {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)Commands.m_82127_("npc")
            .requires(source -> source.m_6761_(CustomNpcs.NoppesCommandOpOnly ? 4 : 2)))
         .then(
            ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.m_82129_(
                                    "npc", StringArgumentType.string()
                                 )
                                 .then(Commands.m_82127_("home").then(Commands.m_82129_("pos", BlockPosArgument.m_118239_()).executes(context -> {
                                    String name = StringArgumentType.getString(context, "npc");
                                    List<EntityNPCInterface> npcs = CmdNoppes.getNpcsByName(((CommandSourceStack)context.getSource()).m_81372_(), name);
                                    if (!npcs.isEmpty()) {
                                       npcs.get(0).ais.setStartPos(BlockPosArgument.m_118242_(context, "pos"));
                                    }

                                    return 1;
                                 }))))
                              .then(
                                 Commands.m_82127_("visible")
                                    .then(Commands.m_82129_("visibility", StringArgumentType.word()).suggests(VISIBLE).executes(context -> {
                                       String name = StringArgumentType.getString(context, "npc");
                                       List<EntityNPCInterface> npcs = CmdNoppes.getNpcsByName(((CommandSourceStack)context.getSource()).m_81372_(), name);
                                       String val = StringArgumentType.getString(context, "visibility");
                                       int vis = 0;
                                       if (val.equalsIgnoreCase("false")) {
                                          vis = 1;
                                       } else if (val.equalsIgnoreCase("semi")) {
                                          vis = 2;
                                       }

                                       for (EntityNPCInterface npc : npcs) {
                                          npc.display.setVisible(vis);
                                       }

                                       return 1;
                                    }))
                              ))
                           .then(Commands.m_82127_("delete").executes(context -> {
                              String name = StringArgumentType.getString(context, "npc");

                              for (EntityNPCInterface npc : CmdNoppes.getNpcsByName(((CommandSourceStack)context.getSource()).m_81372_(), name)) {
                                 npc.delete();
                              }

                              return 1;
                           })))
                        .then(((LiteralArgumentBuilder)Commands.m_82127_("owner").executes(context -> {
                           String name = StringArgumentType.getString(context, "npc");

                           for (EntityNPCInterface npc : CmdNoppes.getNpcsByName(((CommandSourceStack)context.getSource()).m_81372_(), name)) {
                              LivingEntity owner = npc.getOwner();
                              if (owner == null) {
                                 ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("No owner"), false);
                              } else {
                                 ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Owner is: " + owner.m_7755_()), false);
                              }
                           }

                           return 1;
                        })).then(Commands.m_82129_("player", EntityArgument.m_91466_()).executes(context -> {
                           String name = StringArgumentType.getString(context, "npc");
                           List<EntityNPCInterface> npcs = CmdNoppes.getNpcsByName(((CommandSourceStack)context.getSource()).m_81372_(), name);
                           Player player = EntityArgument.m_91474_(context, "player");

                           for (EntityNPCInterface npc : npcs) {
                              if (npc.role instanceof RoleFollower) {
                                 ((RoleFollower)npc.role).setOwner(player);
                              }

                              if (npc.role instanceof RoleCompanion) {
                                 ((RoleCompanion)npc.role).setOwner(player);
                              }
                           }

                           return 1;
                        }))))
                     .then(
                        Commands.m_82127_("delete")
                           .then(
                              Commands.m_82129_("name", StringArgumentType.greedyString())
                                 .executes(
                                    context -> {
                                       List<EntityNPCInterface> npcs = CmdNoppes.getNpcsByName(
                                          ((CommandSourceStack)context.getSource()).m_81372_(), StringArgumentType.getString(context, "npc")
                                       );
                                       String name = StringArgumentType.getString(context, "name");

                                       for (EntityNPCInterface npc : npcs) {
                                          npc.display.setName(name);
                                          npc.updateClient = true;
                                       }

                                       return 1;
                                    }
                                 )
                           )
                     ))
                  .then(Commands.m_82127_("reset").executes(context -> {
                     String name = StringArgumentType.getString(context, "npc");

                     for (EntityNPCInterface npc : CmdNoppes.getNpcsByName(((CommandSourceStack)context.getSource()).m_81372_(), name)) {
                        npc.reset();
                     }

                     return 1;
                  })))
               .then(Commands.m_82127_("create").executes(context -> {
                  String name = StringArgumentType.getString(context, "npc");
                  Level pw = ((CommandSourceStack)context.getSource()).m_81372_();
                  EntityCustomNpc npc = new EntityCustomNpc(CustomEntities.entityCustomNpc, pw);
                  npc.display.setName(name);
                  Vec3 pos = ((CommandSourceStack)context.getSource()).m_81371_();
                  npc.m_19890_(pos.f_82479_, pos.f_82480_, pos.f_82481_, 0.0F, 0.0F);
                  npc.ais.setStartPos(new BlockPos(pos));
                  pw.m_7967_(npc);
                  npc.m_21153_(npc.m_21233_());
                  return 1;
               }))
                    .then(
                            Commands.m_82127_("ai")
                                    .executes(context -> {
                                        ((CommandSourceStack)context.getSource())
                                                .m_81354_(Component.m_237113_("AI funcionando"), false);
                                        return 1;
                                    })
                    )
         );
   }
}
