package noppes.npcs.client.renderer.blocks;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import noppes.npcs.CustomBlocks;
import noppes.npcs.blocks.tiles.TileCopy;
import noppes.npcs.schematics.Schematic;

public class BlockCopyRenderer extends BlockRendererInterface<TileCopy> {
   private static final ItemStack item = new ItemStack(CustomBlocks.copy);
   public static Schematic schematic = null;
   public static BlockPos pos = null;

   public BlockCopyRenderer(Context dispatcher) {
      super(dispatcher);
   }

   public void render(TileCopy tile, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      matrixStack.m_85836_();
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_69461_();
      this.drawSelectionBox(matrixStack, buffer, new BlockPos(tile.width, tile.height, tile.length));
      matrixStack.m_85837_(0.5, 0.5, 0.5);
      matrixStack.m_85845_(Vector3f.f_122225_.m_122240_(180.0F));
      Minecraft.m_91087_().m_91291_().m_174269_(item, TransformType.NONE, light, OverlayTexture.f_118083_, matrixStack, buffer, 0);
      matrixStack.m_85849_();
   }

   public void drawSelectionBox(PoseStack matrixStack, MultiBufferSource buffer, BlockPos pos) {
      AABB bb = new AABB(BlockPos.f_121853_, pos);
      matrixStack.m_85837_(0.001F, 0.001F, 0.001F);
      LevelRenderer.m_109646_(matrixStack, buffer.m_6299_(RenderType.m_110504_()), bb, 1.0F, 0.0F, 0.0F, 1.0F);
   }
}
