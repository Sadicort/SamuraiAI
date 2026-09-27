package noppes.npcs.client.model.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;

public class AniBow implements AnimationBase {
   @Override
   public void animatePre(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
   }

   @Override
   public void animatePost(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
      float ticks = (entity.f_19797_ - animationStart) / 10.0F;
      if (ticks > 1.0F) {
         ticks = 1.0F;
      }

      float ticks2 = (entity.f_19797_ + 1 - animationStart) / 10.0F;
      if (ticks2 > 1.0F) {
         ticks2 = 1.0F;
      }

      ticks += (ticks2 - ticks) * Minecraft.m_91087_().m_91297_();
      model.f_102810_.f_104203_ = ticks;
      model.f_102808_.f_104203_ = ticks;
      model.f_102812_.f_104203_ = ticks;
      model.f_102811_.f_104203_ = ticks;
      model.f_102810_.f_104202_ = -ticks * 10.0F;
      model.f_102810_.f_104201_ = ticks * 6.0F;
      model.f_102808_.f_104202_ = -ticks * 10.0F;
      model.f_102808_.f_104201_ = ticks * 6.0F;
      model.f_102812_.f_104202_ = -ticks * 10.0F;
      model.f_102812_.f_104201_ += ticks * 6.0F;
      model.f_102811_.f_104202_ = -ticks * 10.0F;
      model.f_102811_.f_104201_ += ticks * 6.0F;
   }
}
