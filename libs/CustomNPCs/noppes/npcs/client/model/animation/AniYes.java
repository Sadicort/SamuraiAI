package noppes.npcs.client.model.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;

public class AniYes implements AnimationBase {
   @Override
   public void animatePre(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
   }

   @Override
   public void animatePost(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
      float ticks = (entity.f_19797_ - animationStart) / 8.0F;
      float ticks2 = (entity.f_19797_ + 1 - animationStart) / 8.0F;
      ticks += (ticks2 - ticks) * Minecraft.m_91087_().m_91297_();
      ticks %= 2.0F;
      float ani = ticks - 0.5F;
      if (ticks > 1.0F) {
         ani = 1.5F - ticks;
      }

      model.f_102808_.f_104203_ = ani;
   }
}
