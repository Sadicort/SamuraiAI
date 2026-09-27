package noppes.npcs.client.model.animation;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;

public class AniPoint implements AnimationBase {
   @Override
   public void animatePost(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
      model.f_102811_.f_104203_ = -1.570796F;
      model.f_102811_.f_104204_ = netHeadYaw / (180.0F / (float)Math.PI);
      model.f_102811_.f_104205_ = 0.0F;
   }

   @Override
   public void animatePre(
      float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Entity entity, HumanoidModel model, int animationStart
   ) {
   }
}
