package noppes.npcs.client.model.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;

public class AniDancing implements AnimationBase {
   @Override
   public void animatePost(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
      float dancing = entity.f_19797_ / 4.0F;
      float dancing2 = (entity.f_19797_ + 1) / 4.0F;
      dancing += (dancing2 - dancing) * Minecraft.m_91087_().m_91297_();
      float x = (float)Math.sin(dancing);
      float y = (float)Math.abs(Math.cos(dancing));
      model.f_102809_.f_104200_ = model.f_102808_.f_104200_ = x * 0.75F;
      model.f_102809_.f_104201_ = model.f_102808_.f_104201_ = y * 1.25F - 0.02F + (entity.m_6047_() ? 4 : 0);
      model.f_102809_.f_104202_ = model.f_102808_.f_104202_ = -y * 0.75F;
      model.f_102812_.f_104200_ += x * 0.25F;
      model.f_102812_.f_104201_ += y * 1.25F;
      model.f_102811_.f_104200_ += x * 0.25F;
      model.f_102811_.f_104201_ += y * 1.25F;
      model.f_102810_.f_104200_ = x * 0.25F;
   }

   @Override
   public void animatePre(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
   }
}
