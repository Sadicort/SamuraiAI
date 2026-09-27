package noppes.npcs.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import noppes.npcs.CustomNpcs;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.client.gui.player.tabs.InventoryTabFactions;
import noppes.npcs.client.gui.player.tabs.InventoryTabQuests;
import noppes.npcs.client.gui.player.tabs.InventoryTabVanilla;
import noppes.npcs.client.renderer.MarkRenderer;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketPlayerSoundPlays;
import noppes.npcs.schematics.SchematicWrapper;
import noppes.npcs.shared.common.util.LogWriter;

public class ClientEventHandler {
   private VertexBuffer cache = null;

   public static void onRenderTick(PoseStack matrixStack, BlockPos rpos, BlockEntity te) {
      MultiBufferSource buffer = Minecraft.m_91087_().m_91269_().m_110104_();
      LocalPlayer player = Minecraft.m_91087_().f_91074_;
      if (rpos != null && rpos != BlockPos.f_121853_ && !(rpos.m_123331_(player.m_20183_()) > 1000000.0)) {
         TileBuilder tile = (TileBuilder)te;
         SchematicWrapper schem = tile.getSchematic();
         if (schem != null) {
            matrixStack.m_85836_();
            matrixStack.m_85837_(1.0, tile.yOffest, 1.0);
            if (!TileBuilder.Compiled) {
               BlockRenderDispatcher dispatcher = Minecraft.m_91087_().m_91289_();

               try {
                  for (int i = 0; i < schem.size && i < 25000; i++) {
                     BlockState state = schem.schema.getBlockState(i);
                     if (state.m_60799_() != RenderShape.INVISIBLE && state.m_60799_() == RenderShape.MODEL) {
                        int posX = i % schem.schema.getWidth();
                        int posZ = (i - posX) / schem.schema.getWidth() % schem.schema.getLength();
                        int posY = ((i - posX) / schem.schema.getWidth() - posZ) / schem.schema.getLength();
                        BlockPos pos = schem.rotatePos(posX, posY, posZ, tile.rotation);
                        matrixStack.m_85836_();
                        matrixStack.m_85837_(pos.m_123341_(), pos.m_123342_(), pos.m_123343_());
                        state = schem.rotationState(state, tile.rotation);

                        try {
                           BakedModel ibakedmodel = dispatcher.m_110910_(state);
                           BufferBuilder builder = (BufferBuilder)buffer.m_6299_(ItemBlockRenderTypes.m_109284_(state, false));
                           dispatcher.m_110937_()
                              .renderModel(
                                 matrixStack.m_85850_(),
                                 builder,
                                 state,
                                 ibakedmodel,
                                 1.0F,
                                 1.0F,
                                 1.0F,
                                 15728880,
                                 OverlayTexture.f_118083_,
                                 ModelData.EMPTY,
                                 ItemBlockRenderTypes.m_109284_(state, false)
                              );
                        } catch (Exception e) {
                           e.printStackTrace();
                        } finally {
                           matrixStack.m_85849_();
                        }
                     }
                  }
               } catch (Exception e) {
                  LogWriter.error("Error preview builder block", e);
               } finally {
                  ;
               }
            }

            if (tile.rotation % 2 == 0) {
               drawSelectionBox(matrixStack, buffer, new BlockPos(schem.schema.getWidth(), schem.schema.getHeight(), schem.schema.getLength()));
            } else {
               drawSelectionBox(matrixStack, buffer, new BlockPos(schem.schema.getLength(), schem.schema.getHeight(), schem.schema.getWidth()));
            }

            matrixStack.m_85849_();
         }
      }
   }

   @SubscribeEvent
   public void post(Post event) {
      MarkData data = MarkData.get(event.getEntity());
      Player player = Minecraft.m_91087_().f_91074_;

      for (MarkData.Mark m : data.marks) {
         if (m.getType() != 0 && m.availability.isAvailable(player)) {
            MarkRenderer.render(event, m);
            break;
         }
      }
   }

   @SubscribeEvent
   public void playSound(PlaySoundEvent event) {
      Minecraft mc = Minecraft.m_91087_();
      if (mc != null && mc.f_91073_ != null && mc.m_91403_() != null && event != null && event.getSound() != null) {
         SoundInstance sound = event.getSound();
         Packets.sendServer(new SPacketPlayerSoundPlays(sound.m_7904_().toString(), sound.m_8070_().m_12676_(), sound.m_7775_()));
      }
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent(priority = EventPriority.LOWEST)
   public void guiPostInit(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
      if (event.getScreen() instanceof InventoryScreen screen && CustomNpcs.InventoryGuiEnabled) {
         event.addListener(new InventoryTabVanilla().init(screen));
         event.addListener(new InventoryTabFactions().init(screen));
         event.addListener(new InventoryTabQuests().init(screen));
      }
   }

   public static void drawSelectionBox(PoseStack matrixStack, MultiBufferSource buffer, BlockPos pos) {
      matrixStack.m_85836_();
      AABB bb = new AABB(BlockPos.f_121853_, pos);
      matrixStack.m_85837_(0.001F, 0.001F, 0.001F);
      LevelRenderer.m_109646_(matrixStack, buffer.m_6299_(RenderType.m_110504_()), bb, 1.0F, 0.0F, 0.0F, 1.0F);
      matrixStack.m_85849_();
   }
}
