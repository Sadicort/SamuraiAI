package noppes.npcs.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import noppes.npcs.shared.client.model.NopModelPart;

public class ModelNpcCrystal extends EntityModel {
   private static final float SIN_45 = (float)Math.sin(Math.PI / 4);
   private NopModelPart field_41057_g;
   private NopModelPart field_41058_h = new NopModelPart(64, 32, 0, 0);
   private NopModelPart field_41059_i;
   float ticks;
   float tickCount;

   public ModelNpcCrystal() {
      this.field_41058_h.addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F);
      this.field_41057_g = new NopModelPart(64, 32, 32, 0);
      this.field_41057_g.addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F);
      this.field_41059_i = new NopModelPart(64, 32, 0, 16);
      this.field_41059_i.addBox(-6.0F, 16.0F, -6.0F, 12.0F, 4.0F, 12.0F);
   }

   public void m_6973_(Entity p_225597_1_, float p_225597_2_, float p_225597_3_, float p_225597_4_, float p_225597_5_, float p_225597_6_) {
   }

   public void m_6839_(Entity par1EntityLiving, float f6, float f5, float par9) {
      this.ticks = par9;
      this.tickCount = par1EntityLiving.f_19797_;
   }

   public void m_7695_(PoseStack mStack, VertexConsumer ivertex, int lightmapUV, int packedOverlayIn, float red, float green, float blue, float alpha) {
      mStack.m_85836_();
      mStack.m_85841_(2.0F, 2.0F, 2.0F);
      mStack.m_85837_(0.0, -0.5, 0.0);
      this.field_41059_i.render(mStack, ivertex, lightmapUV, packedOverlayIn, red, green, blue, alpha);
      float f = this.tickCount + this.ticks;
      float f1 = Mth.m_14031_(f * 0.2F) / 2.0F + 0.5F;
      f1 = f1 * f1 + f1;
      float par3 = f * 3.0F;
      float par4 = f1 * 0.2F;
      mStack.m_85845_(Vector3f.f_122225_.m_122240_(par3));
      mStack.m_85837_(0.0, 0.1F + par4, 0.0);
      mStack.m_85845_(new Quaternion(new Vector3f(SIN_45, 0.0F, SIN_45), 60.0F, true));
      this.field_41058_h.render(mStack, ivertex, lightmapUV, packedOverlayIn, red, green, blue, alpha);
      float sca = 0.875F;
      mStack.m_85841_(sca, sca, sca);
      mStack.m_85845_(new Quaternion(new Vector3f(SIN_45, 0.0F, SIN_45), 60.0F, true));
      mStack.m_85845_(Vector3f.f_122225_.m_122240_(par3));
      this.field_41058_h.render(mStack, ivertex, lightmapUV, packedOverlayIn, red, green, blue, alpha);
      mStack.m_85841_(sca, sca, sca);
      mStack.m_85845_(new Quaternion(new Vector3f(SIN_45, 0.0F, SIN_45), 60.0F, true));
      mStack.m_85845_(Vector3f.f_122225_.m_122240_(par3));
      this.field_41057_g.render(mStack, ivertex, lightmapUV, packedOverlayIn, red, green, blue, alpha);
      mStack.m_85849_();
   }
}
