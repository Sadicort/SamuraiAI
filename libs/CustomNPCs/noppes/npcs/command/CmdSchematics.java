package noppes.npcs.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.controllers.SchematicController;
import noppes.npcs.schematics.SchematicWrapper;

public class CmdSchematics {
   public static final List<String> names = new ArrayList<>();
   public static final SuggestionProvider<CommandSourceStack> SCHEMAS = SuggestionProviders.m_121658_(
      new ResourceLocation("schemas"), (context, builder) -> SharedSuggestionProvider.m_82981_(names.stream(), builder)
   );
   public static final SuggestionProvider<CommandSourceStack> ROTATION = SuggestionProviders.m_121658_(
      new ResourceLocation("rotation"), (context, builder) -> SharedSuggestionProvider.m_82967_(new String[]{"0", "90", "180", "270"}, builder)
   );

   public static LiteralArgumentBuilder<CommandSourceStack> register() {
      return (LiteralArgumentBuilder<CommandSourceStack>)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.m_82127_(
                        "schema"
                     )
                     .requires(source -> source.m_6761_(4)))
                  .then(
                     Commands.m_82127_("build")
                        .then(
                           Commands.m_82129_("name", StringArgumentType.word())
                              .suggests(SCHEMAS)
                              .then(
                                 Commands.m_82129_("pos", BlockPosArgument.m_118239_())
                                    .then(Commands.m_82129_("rotation", StringArgumentType.word()).suggests(ROTATION).executes(context -> {
                                       String name = StringArgumentType.getString(context, "name");
                                       BlockPos pos = BlockPosArgument.m_118242_(context, "pos");
                                       int rotation = Integer.parseInt(StringArgumentType.getString(context, "rotation"));
                                       SchematicWrapper schem = SchematicController.Instance.load(name);
                                       schem.init(pos, ((CommandSourceStack)context.getSource()).m_81372_(), rotation);
                                       SchematicController.Instance.build(schem, (CommandSourceStack)context.getSource());
                                       return 1;
                                    }))
                              )
                        )
                  ))
               .then(Commands.m_82127_("stop").executes(context -> {
                  SchematicController.Instance.stop((CommandSourceStack)context.getSource());
                  return 1;
               })))
            .then(Commands.m_82127_("info").executes(context -> {
               SchematicController.Instance.info((CommandSourceStack)context.getSource());
               return 1;
            })))
         .then(Commands.m_82127_("list").executes(context -> {
            List<String> list = SchematicController.Instance.list();
            if (list.isEmpty()) {
               ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237115_("No schemas available"), false);
               return 1;
            }

            String s = "";

            for (String file : list) {
               s = s + file + ", ";
            }

            ((CommandSourceStack)context.getSource()).m_81354_(Component.m_237115_(s), false);
            return 1;
         }));
   }
}
