package noppes.npcs.client.model.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class AniWaving implements AnimationBase {
   @Override
   public void animatePre(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
   }

   @Override
   public void animatePost(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
      float f = Mth.m_14031_(entity.f_19797_ * 0.27F);
      float f2 = Mth.m_14031_((entity.f_19797_ + 1) * 0.27F);
      f += (f2 - f) * Minecraft.m_91087_().m_91297_();
      model.f_102811_.f_104203_ = -0.1F;
      model.f_102811_.f_104204_ = 0.0F;
      model.f_102811_.f_104205_ = (float)(2.141592653589793 - f * 0.5F);
   }
}
