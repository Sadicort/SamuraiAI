package noppes.npcs.client.renderer.blocks;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.blocks.BlockCarpentryBench;
import noppes.npcs.blocks.tiles.TileBlockAnvil;
import noppes.npcs.client.model.blocks.ModelCarpentryBench;

public class BlockCarpentryBenchRenderer implements BlockEntityRenderer<TileBlockAnvil> {
   private final ModelCarpentryBench model = new ModelCarpentryBench();
   private static final ResourceLocation TEXTURE = new ResourceLocation("customnpcs", "textures/models/carpentrybench.png");
   private static final RenderType type = RenderType.m_110452_(TEXTURE);

   public BlockCarpentryBenchRenderer(Context dispatcher) {
   }

   public void render(TileBlockAnvil te, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int light, int overlay) {
      int rotation = 0;
      if (te.m_58899_() != BlockPos.f_121853_) {
         rotation = (Integer)te.m_58900_().m_61143_(BlockCarpentryBench.ROTATION);
      }

      matrixStack.m_85836_();
      RenderSystem.m_69461_();
      matrixStack.m_85837_(0.5, 1.4F, 0.5);
      matrixStack.m_85841_(0.95F, 0.95F, 0.95F);
      matrixStack.m_85845_(Vector3f.f_122227_.m_122240_(180.0F));
      matrixStack.m_85845_(Vector3f.f_122225_.m_122240_(90 * rotation));
      this.model.m_7695_(matrixStack, buffer.m_6299_(type), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
      matrixStack.m_85849_();
   }
}
