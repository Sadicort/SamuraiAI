package noppes.npcs.client.model.animation;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class AniCrawling implements AnimationBase {
   @Override
   public void animatePre(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
   }

   @Override
   public void animatePost(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
      model.f_102808_.f_104205_ = -netHeadYaw / (180.0F / (float)Math.PI);
      model.f_102808_.f_104204_ = 0.0F;
      model.f_102808_.f_104203_ = -0.95993114F;
      model.f_102809_.f_104203_ = model.f_102808_.f_104203_;
      model.f_102809_.f_104204_ = model.f_102808_.f_104204_;
      model.f_102809_.f_104205_ = model.f_102808_.f_104205_;
      if (limbSwingAmount > 0.25) {
         limbSwingAmount = 0.25F;
      }

      float movement = Mth.m_14089_(limbSwing * 0.8F + (float) Math.PI) * limbSwingAmount;
      model.f_102812_.f_104203_ = (float) Math.PI - movement * 0.25F;
      model.f_102812_.f_104204_ = movement * -0.46F;
      model.f_102812_.f_104205_ = movement * -0.2F;
      model.f_102812_.f_104201_ = 2.0F - movement * 9.0F;
      model.f_102811_.f_104203_ = (float) Math.PI + movement * 0.25F;
      model.f_102811_.f_104204_ = movement * -0.4F;
      model.f_102811_.f_104205_ = movement * -0.2F;
      model.f_102811_.f_104201_ = 2.0F + movement * 9.0F;
      model.f_102810_.f_104204_ = movement * 0.1F;
      model.f_102810_.f_104203_ = 0.0F;
      model.f_102810_.f_104205_ = movement * 0.1F;
      model.f_102814_.f_104203_ = movement * 0.1F;
      model.f_102814_.f_104204_ = movement * 0.1F;
      model.f_102814_.f_104205_ = -0.122173056F - movement * 0.25F;
      model.f_102814_.f_104201_ = 10.4F + movement * 9.0F;
      model.f_102814_.f_104202_ = movement * 0.6F;
      model.f_102813_.f_104203_ = movement * -0.1F;
      model.f_102813_.f_104204_ = movement * 0.1F;
      model.f_102813_.f_104205_ = 0.122173056F - movement * 0.25F;
      model.f_102813_.f_104201_ = 10.4F - movement * 9.0F;
      model.f_102813_.f_104202_ = movement * -0.6F;
   }
}
