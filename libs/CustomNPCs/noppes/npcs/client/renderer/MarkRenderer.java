package noppes.npcs.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderLivingEvent.Post;
import noppes.npcs.controllers.data.MarkData;
import noppes.npcs.shared.client.model.Model2DRenderer;

public class MarkRenderer {
   public static final ResourceLocation markExclamation = new ResourceLocation("customnpcs", "textures/marks/exclamation.png");
   public static final ResourceLocation markQuestion = new ResourceLocation("customnpcs", "textures/marks/question.png");
   public static final ResourceLocation markPointer = new ResourceLocation("customnpcs", "textures/marks/pointer.png");
   public static final ResourceLocation markCross = new ResourceLocation("customnpcs", "textures/marks/cross.png");
   public static final ResourceLocation markSkull = new ResourceLocation("customnpcs", "textures/marks/skull.png");
   public static final ResourceLocation markStar = new ResourceLocation("customnpcs", "textures/marks/star.png");
   public static int displayList = -1;
   public static Model2DRenderer renderer = new Model2DRenderer(0, 0, 32, 32, 32, 32, markExclamation);

   public static void render(Post event, MarkData.Mark mark) {
      PoseStack matrixStack = event.getPoseStack();
      matrixStack.m_85836_();
      int color = mark.color;
      float red = (color >> 16 & 0xFF) / 255.0F;
      float green = (color >> 8 & 0xFF) / 255.0F;
      float blue = (color & 0xFF) / 255.0F;
      ResourceLocation location = markExclamation;
      if (mark.type == 1) {
         location = markQuestion;
      } else if (mark.type == 3) {
         location = markPointer;
      } else if (mark.type == 5) {
         location = markCross;
      } else if (mark.type == 4) {
         location = markSkull;
      } else if (mark.type == 6) {
         location = markStar;
      }

      matrixStack.m_85837_(0.0, event.getEntity().m_20206_() + 0.6, 0.0);
      matrixStack.m_85845_(Vector3f.f_122222_.m_122240_(180.0F));
      matrixStack.m_85845_(Vector3f.f_122225_.m_122240_(event.getEntity().f_20885_));
      matrixStack.m_85837_(-0.5, 0.0, 0.0);
      renderer.render(
         location,
         matrixStack,
         event.getMultiBufferSource().m_6299_(RenderType.m_110452_(location)),
         event.getPackedLight(),
         OverlayTexture.f_118083_,
         red,
         green,
         blue,
         1.0F
      );
      matrixStack.m_85849_();
   }
}
