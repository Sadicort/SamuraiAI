package noppes.npcs.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.IPos;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.controllers.ScriptController;

public class CmdScript {
   public static LiteralArgumentBuilder<CommandSourceStack> register() {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_("script")
               .requires(source -> source.m_6761_(CustomNpcs.NoppesCommandOpOnly ? 4 : 2)))
            .then(Commands.m_82127_("reload").executes(context -> {
               ScriptController.Instance.loadCategories();
               if (ScriptController.Instance.loadPlayerScripts()) {
                  ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Reload player scripts succesfully"), false);
               } else {
                  ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Failed reloading player scripts"), false);
               }

               if (ScriptController.Instance.loadForgeScripts()) {
                  ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Reload forge scripts succesfully"), false);
               } else {
                  ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Failed reloading forge scripts"), false);
               }

               if (ScriptController.Instance.loadStoredData()) {
                  ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Reload stored data succesfully"), false);
               } else {
                  ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237113_("Failed reloading stored data"), false);
               }

               return 1;
            })))
         .then(Commands.m_82127_("trigger").then(((RequiredArgumentBuilder)Commands.m_82129_("id", IntegerArgumentType.integer(0)).executes(context -> {
            IWorld level = NpcAPI.Instance().getIWorld(((CommandSourceStack)context.getSource()).m_81372_());
            Vec3 bpos = ((CommandSourceStack)context.getSource()).m_81371_();
            IPos pos = NpcAPI.Instance().getIPos(bpos.f_82479_, bpos.f_82480_, bpos.f_82481_);
            int id = IntegerArgumentType.getInteger(context, "id");
            IEntity e = NpcAPI.Instance().getIEntity(((CommandSourceStack)context.getSource()).m_81373_());
            EventHooks.onScriptTriggerEvent(id, level, pos, e, new String[0]);
            return 1;
         })).then(Commands.m_82129_("args", StringArgumentType.greedyString()).executes(context -> {
            IWorld level = NpcAPI.Instance().getIWorld(((CommandSourceStack)context.getSource()).m_81372_());
            Vec3 bpos = ((CommandSourceStack)context.getSource()).m_81371_();
            IPos pos = NpcAPI.Instance().getIPos(bpos.f_82479_, bpos.f_82480_, bpos.f_82481_);
            IEntity e = NpcAPI.Instance().getIEntity(((CommandSourceStack)context.getSource()).m_81373_());
            int id = IntegerArgumentType.getInteger(context, "id");
            EventHooks.onScriptTriggerEvent(id, level, pos, e, StringArgumentType.getString(context, "args").split(" "));
            return 1;
         }))));
   }
}
