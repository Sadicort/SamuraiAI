package noppes.npcs.client.model.animation;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class AniHug implements AnimationBase {
   @Override
   public void animatePost(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
      float f6 = Mth.m_14031_(model.f_102608_ * 3.141593F);
      float f7 = Mth.m_14031_((1.0F - (1.0F - model.f_102608_) * (1.0F - model.f_102608_)) * 3.141593F);
      model.f_102811_.f_104205_ = 0.0F;
      model.f_102812_.f_104205_ = 0.0F;
      model.f_102811_.f_104204_ = -(0.1F - f6 * 0.6F);
      model.f_102812_.f_104204_ = 0.1F;
      model.f_102811_.f_104203_ = -1.570796F;
      model.f_102812_.f_104203_ = -1.570796F;
      model.f_102811_.f_104203_ -= f6 * 1.2F - f7 * 0.4F;
      model.f_102811_.f_104205_ = model.f_102811_.f_104205_ + (Mth.m_14089_(ageInTicks * 0.09F) * 0.05F + 0.05F);
      model.f_102812_.f_104205_ = model.f_102812_.f_104205_ - (Mth.m_14089_(ageInTicks * 0.09F) * 0.05F + 0.05F);
      model.f_102811_.f_104203_ = model.f_102811_.f_104203_ + Mth.m_14031_(ageInTicks * 0.067F) * 0.05F;
      model.f_102812_.f_104203_ = model.f_102812_.f_104203_ - Mth.m_14031_(ageInTicks * 0.067F) * 0.05F;
   }

   @Override
   public void animatePre(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
   }
}
