package noppes.npcs.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class LayerNpcCloak extends LayerInterface {
   public LayerNpcCloak(LivingEntityRenderer render) {
      super(render);
   }

   @Override
   public void render(
      PoseStack mStack,
      MultiBufferSource typeBuffer,
      int lightmapUV,
      float limbSwing,
      float limbSwingAmount,
      float partialTicks,
      float age,
      float netHeadYaw,
      float headPitch
   ) {
      if (this.npc.textureCloakLocation == null) {
         if (this.npc.display.getCapeTexture() == null || this.npc.display.getCapeTexture().isEmpty() || !(this.base instanceof PlayerModel)) {
            return;
         }

         this.npc.textureCloakLocation = new ResourceLocation(this.npc.display.getCapeTexture());
      }

      mStack.m_85836_();
      mStack.m_85837_(0.0, 0.0, 0.125);
      double d0 = Mth.m_14139_(partialTicks, this.npc.prevChasingPosX, this.npc.chasingPosX)
         - Mth.m_14139_(partialTicks, this.npc.f_19854_, this.npc.m_20185_());
      double d1 = Mth.m_14139_(partialTicks, this.npc.prevChasingPosY, this.npc.chasingPosY)
         - Mth.m_14139_(partialTicks, this.npc.f_19855_, this.npc.m_20186_());
      double d2 = Mth.m_14139_(partialTicks, this.npc.prevChasingPosZ, this.npc.chasingPosZ)
         - Mth.m_14139_(partialTicks, this.npc.f_19856_, this.npc.m_20189_());
      float f = this.npc.f_20884_ + (this.npc.f_20883_ - this.npc.f_20884_);
      double d3 = Mth.m_14031_(f * (float) (Math.PI / 180.0));
      double d4 = -Mth.m_14089_(f * (float) (Math.PI / 180.0));
      float f1 = (float)d1 * 10.0F;
      f1 = Mth.m_14036_(f1, -6.0F, 32.0F);
      float f2 = (float)(d0 * d3 + d2 * d4) * 100.0F;
      f2 = Mth.m_14036_(f2, 0.0F, 150.0F);
      float f3 = (float)(d0 * d4 - d2 * d3) * 100.0F;
      f3 = Mth.m_14036_(f3, -20.0F, 20.0F);
      if (f2 < 0.0F) {
         f2 = 0.0F;
      }

      f1 += Mth.m_14031_(Mth.m_14179_(partialTicks, this.npc.f_19867_, this.npc.f_19787_) * 6.0F) * 32.0F * partialTicks;
      if (this.npc.m_6047_()) {
         f1 += 25.0F;
      }

      mStack.m_85845_(Vector3f.f_122223_.m_122240_(6.0F + f2 / 2.0F + f1));
      mStack.m_85845_(Vector3f.f_122227_.m_122240_(f3 / 2.0F));
      mStack.m_85845_(Vector3f.f_122225_.m_122240_(180.0F - f3 / 2.0F));
      VertexConsumer ivertexbuilder = typeBuffer.m_6299_(RenderType.m_110473_(this.npc.textureCloakLocation));
      ((PlayerModel)this.base).m_103411_(mStack, ivertexbuilder, lightmapUV, OverlayTexture.f_118083_);
      mStack.m_85849_();
   }

   @Override
   public void rotate(PoseStack matrixStack, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
   }
}
