package noppes.npcs.client.renderer.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.client.TextBlockClient;

public class BlockScriptedRenderer extends BlockRendererInterface<TileScripted> {
   private static RandomSource random = RandomSource.m_216327_();

   public BlockScriptedRenderer(Context dispatcher) {
      super(dispatcher);
   }

   public void render(TileScripted tile, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      matrixStack.m_85836_();
      if (this.overrideModel()) {
         matrixStack.m_85837_(0.5, 0.5, 0.5);
         matrixStack.m_85841_(2.0F, 2.0F, 2.0F);
         this.renderItem(new ItemStack(CustomBlocks.scripted), matrixStack, buffer, light, overlay);
      } else {
         matrixStack.m_85845_(Vector3f.f_122225_.m_122240_(tile.rotationY));
         matrixStack.m_85845_(Vector3f.f_122223_.m_122240_(tile.rotationX));
         matrixStack.m_85845_(Vector3f.f_122227_.m_122240_(tile.rotationZ));
         matrixStack.m_85841_(tile.scaleX, tile.scaleY, tile.scaleZ);
         Block b = tile.blockModel;
         if (b != null && b != Blocks.f_50016_ && b != CustomBlocks.scripted) {
            BlockState state = b.m_49966_();
            this.renderBlock(tile, b, state, matrixStack, buffer, light, overlay);
            if (state.m_155947_() && !tile.renderTileErrored) {
               try {
                  if (tile.renderTile == null) {
                     BlockEntity entity = ((EntityBlock)b).m_142194_(tile.m_58899_(), state);
                     entity.m_142339_(tile.m_58904_());
                     tile.renderTile = entity;
                     tile.renderState = state;
                     tile.renderTileUpdate = ((EntityBlock)b).m_142354_(tile.m_58904_(), state, entity.m_58903_());
                  }

                  BlockEntityRenderer renderer = Minecraft.m_91087_().m_167982_().m_112265_(tile.renderTile);
                  if (renderer != null) {
                     renderer.m_6922_(tile.renderTile, partialTicks, matrixStack, buffer, light, overlay);
                  } else {
                     tile.renderTileErrored = true;
                  }
               } catch (Exception e) {
                  tile.renderTileErrored = true;
               }
            }
         } else {
            matrixStack.m_85837_(0.5, 0.5, 0.5);
            matrixStack.m_85841_(2.0F, 2.0F, 2.0F);
            this.renderItem(tile.itemModel, matrixStack, buffer, light, overlay);
         }
      }

      matrixStack.m_85849_();
      if (!tile.text1.text.isEmpty()) {
         this.drawText(matrixStack, tile.text1);
      }

      if (!tile.text2.text.isEmpty()) {
         this.drawText(matrixStack, tile.text2);
      }

      if (!tile.text3.text.isEmpty()) {
         this.drawText(matrixStack, tile.text3);
      }

      if (!tile.text4.text.isEmpty()) {
         this.drawText(matrixStack, tile.text4);
      }

      if (!tile.text5.text.isEmpty()) {
         this.drawText(matrixStack, tile.text5);
      }

      if (!tile.text6.text.isEmpty()) {
         this.drawText(matrixStack, tile.text6);
      }
   }

   private void drawText(PoseStack matrixStack, TileScripted.TextPlane text1) {
      if (text1.textBlock == null || text1.textHasChanged) {
         text1.textBlock = new TextBlockClient(text1.text, 336, true, Minecraft.m_91087_().f_91074_);
         text1.textHasChanged = false;
      }

      matrixStack.m_85836_();
      matrixStack.m_85837_(0.5, 0.5, 0.5);
      matrixStack.m_85845_(Vector3f.f_122225_.m_122240_(text1.rotationY));
      matrixStack.m_85845_(Vector3f.f_122223_.m_122240_(text1.rotationX));
      matrixStack.m_85845_(Vector3f.f_122227_.m_122240_(text1.rotationZ));
      matrixStack.m_85841_(text1.scale, text1.scale, 1.0F);
      matrixStack.m_85837_(text1.offsetX, text1.offsetY, text1.offsetZ);
      float f1 = 0.6666667F;
      float f3 = 0.0133F * f1;
      matrixStack.m_85837_(0.0, 0.5, 0.01F);
      matrixStack.m_85841_(f3, -f3, f3);
      Font fontrenderer = Minecraft.m_91087_().f_91062_;
      float lineOffset = 0.0F;
      if (text1.textBlock.lines.size() < 14) {
         lineOffset = (14.0F - text1.textBlock.lines.size()) / 2.0F;
      }

      for (int i = 0; i < text1.textBlock.lines.size(); i++) {
         Component text = text1.textBlock.lines.get(i);
         fontrenderer.m_92889_(matrixStack, text, -fontrenderer.m_92852_(text) / 2, (int)((lineOffset + i) * (9.0 - 0.3)), 0);
      }

      matrixStack.m_85849_();
   }

   private void renderItem(ItemStack item, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      Minecraft.m_91087_().m_91291_().m_174269_(item, TransformType.FIXED, light, OverlayTexture.f_118083_, matrixStack, buffer, 0);
   }

   private void renderBlock(TileScripted tile, Block b, BlockState state, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      matrixStack.m_85836_();
      Minecraft.m_91087_().m_91289_().m_110912_(state, matrixStack, buffer, light, OverlayTexture.f_118083_);
      if (random.m_188503_(12) == 1) {
         state.m_60734_().m_214162_(state, tile.m_58904_(), tile.m_58899_(), random);
      }

      matrixStack.m_85849_();
   }

   private boolean overrideModel() {
      ItemStack held = Minecraft.m_91087_().f_91074_.m_21205_();
      return held == null ? false : held.m_41720_() == CustomItems.wand || held.m_41720_() == CustomItems.scripter;
   }
}
