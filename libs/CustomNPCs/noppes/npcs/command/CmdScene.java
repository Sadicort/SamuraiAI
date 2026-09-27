package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Map.Entry;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import noppes.npcs.entity.data.DataScenes;

public class CmdScene {
   public static LiteralArgumentBuilder<CommandSourceStack> register() {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                        "scene"
                     )
                     .requires(source -> source.m_6761_(2)))
                  .then(
                     ((LiteralArgumentBuilder)Commands.m_82127_("time")
                           .executes(
                              context -> {
                                 ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Active scenes:"), false);

                                 for (Entry<String, DataScenes.SceneState> entry : DataScenes.StartedScenes.entrySet()) {
                                    ((CommandSourceStack)context.getSource())
                                       .m_81354_(Component.m_237110_("Scene %s time is %s", new Object[]{entry.getKey(), entry.getValue().ticks}), false);
                                 }

                                 return 1;
                              }
                           ))
                        .then(((RequiredArgumentBuilder)Commands.m_82129_("time", IntegerArgumentType.integer(0)).executes(context -> {
                           int ticks = IntegerArgumentType.getInteger(context, "time");

                           for (DataScenes.SceneState state : DataScenes.StartedScenes.values()) {
                              state.ticks = ticks;
                           }

                           return 1;
                        })).then(Commands.m_82129_("name", StringArgumentType.string()).executes(context -> {
                           String name = StringArgumentType.getString(context, "name");
                           DataScenes.SceneState state = DataScenes.StartedScenes.get(name.toLowerCase());
                           if (state == null) {
                              throw new CommandRuntimeException(Component.m_237110_("Unknown scene name %s", new Object[]{name}));
                           }

                           state.ticks = IntegerArgumentType.getInteger(context, "time");
                           ((CommandSourceStack)context.getSource())
                              .m_81354_(Component.m_237110_("Scene %s set to %s", new Object[]{name, state.ticks}), false);
                           return 1;
                        })))
                  ))
               .then(((LiteralArgumentBuilder)Commands.m_82127_("reset").executes(context -> {
                  DataScenes.Reset((CommandSourceStack)context.getSource(), null);
                  return 1;
               })).then(Commands.m_82129_("name", StringArgumentType.string()).executes(context -> {
                  DataScenes.Reset((CommandSourceStack)context.getSource(), StringArgumentType.getString(context, "name"));
                  return 1;
               }))))
            .then(Commands.m_82127_("start").then(Commands.m_82129_("name", StringArgumentType.string()).executes(context -> {
               DataScenes.Start(((CommandSourceStack)context.getSource()).m_81377_(), StringArgumentType.getString(context, "name"));
               return 1;
            }))))
         .then(((LiteralArgumentBuilder)Commands.m_82127_("pause").executes(context -> {
            DataScenes.Pause((CommandSourceStack)context.getSource(), null);
            return 1;
         })).then(Commands.m_82129_("name", StringArgumentType.string()).executes(context -> {
            DataScenes.Pause((CommandSourceStack)context.getSource(), StringArgumentType.getString(context, "name"));
            return 1;
         })));
   }
}
