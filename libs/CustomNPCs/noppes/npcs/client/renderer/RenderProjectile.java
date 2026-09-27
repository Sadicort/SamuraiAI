package noppes.npcs.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms.TransformType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noppes.npcs.entity.EntityProjectile;

@OnlyIn(Dist.CLIENT)
public class RenderProjectile<T extends EntityProjectile> extends EntityRenderer<T> {
   public boolean renderWithColor = true;
   private static final ResourceLocation field_110780_a = new ResourceLocation("textures/entity/projectiles/arrow.png");
   private static final ResourceLocation field_110798_h = new ResourceLocation("textures/misc/enchanted_item_glint.png");
   private boolean crash = false;
   private boolean crash2 = false;

   public RenderProjectile(Context manager) {
      super(manager);
   }

   public void render(T projectile, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
      Minecraft mc = Minecraft.m_91087_();
      matrixStack.m_85836_();
      float scale = projectile.getSize() / 10.0F;
      ItemStack item = projectile.getItemDisplay();
      matrixStack.m_85841_(scale, scale, scale);
      if (projectile.isArrow()) {
         matrixStack.m_85845_(Vector3f.f_122225_.m_122240_(Mth.m_14179_(partialTicks, projectile.f_19859_, projectile.m_146908_()) - 90.0F));
         matrixStack.m_85845_(Vector3f.f_122223_.m_122240_(Mth.m_14179_(partialTicks, projectile.f_19860_, projectile.m_146909_())));
         float f9 = projectile.arrowShake - partialTicks;
         if (f9 > 0.0F) {
            float f10 = -Mth.m_14031_(f9 * 3.0F) * f9;
            matrixStack.m_85845_(Vector3f.f_122227_.m_122240_(f10));
         }

         matrixStack.m_85845_(Vector3f.f_122223_.m_122240_(45.0F));
         matrixStack.m_85841_(0.05625F, 0.05625F, 0.05625F);
         matrixStack.m_85837_(-4.0, 0.0, 0.0);
         VertexConsumer ivertexbuilder = buffer.m_6299_(RenderType.m_110452_(this.getTextureLocation(projectile)));
         Pose matrixstack$entry = matrixStack.m_85850_();
         Matrix4f matrix4f = matrixstack$entry.m_85861_();
         Matrix3f matrix3f = matrixstack$entry.m_85864_();
         this.drawVertex(matrix4f, matrix3f, ivertexbuilder, -7, -2, -2, 0.0F, 0.15625F, -1, 0, 0, packedLight);
         this.drawVertex(matrix4f, matrix3f, ivertexbuilder, -7, -2, 2, 0.15625F, 0.15625F, -1, 0, 0, packedLight);
         this.drawVertex(matrix4f, matrix3f, ivertexbuilder, -7, 2, 2, 0.15625F, 0.3125F, -1, 0, 0, packedLight);
         this.drawVertex(matrix4f, matrix3f, ivertexbuilder, -7, 2, -2, 0.0F, 0.3125F, -1, 0, 0, packedLight);
         this.drawVertex(matrix4f, matrix3f, ivertexbuilder, -7, 2, -2, 0.0F, 0.15625F, 1, 0, 0, packedLight);
         this.drawVertex(matrix4f, matrix3f, ivertexbuilder, -7, 2, 2, 0.15625F, 0.15625F, 1, 0, 0, packedLight);
         this.drawVertex(matrix4f, matrix3f, ivertexbuilder, -7, -2, 2, 0.15625F, 0.3125F, 1, 0, 0, packedLight);
         this.drawVertex(matrix4f, matrix3f, ivertexbuilder, -7, -2, -2, 0.0F, 0.3125F, 1, 0, 0, packedLight);

         for (int j = 0; j < 4; j++) {
            matrixStack.m_85845_(Vector3f.f_122223_.m_122240_(90.0F));
            this.drawVertex(matrix4f, matrix3f, ivertexbuilder, -8, -2, 0, 0.0F, 0.0F, 0, 1, 0, packedLight);
            this.drawVertex(matrix4f, matrix3f, ivertexbuilder, 8, -2, 0, 0.5F, 0.0F, 0, 1, 0, packedLight);
            this.drawVertex(matrix4f, matrix3f, ivertexbuilder, 8, 2, 0, 0.5F, 0.15625F, 0, 1, 0, packedLight);
            this.drawVertex(matrix4f, matrix3f, ivertexbuilder, -8, 2, 0, 0.0F, 0.15625F, 0, 1, 0, packedLight);
         }
      } else if (projectile.is3D()) {
         matrixStack.m_85845_(Vector3f.f_122225_.m_122240_(Mth.m_14179_(partialTicks, projectile.f_19859_, projectile.m_146908_()) - 180.0F));
         matrixStack.m_85845_(Vector3f.f_122227_.m_122240_(Mth.m_14179_(partialTicks, projectile.f_19860_, projectile.m_146909_())));
         matrixStack.m_85837_(0.0, -0.125, 0.25);
         if (item.m_41720_() instanceof BlockItem && Block.m_49814_(item.m_41720_()).m_49966_().m_60799_() == RenderShape.ENTITYBLOCK_ANIMATED) {
            matrixStack.m_85837_(0.0, 0.1875, -0.3125);
            matrixStack.m_85845_(Vector3f.f_122223_.m_122240_(20.0F));
            matrixStack.m_85845_(Vector3f.f_122225_.m_122240_(45.0F));
            float f8 = 0.375F;
            matrixStack.m_85841_(-f8, -f8, f8);
         }

         if (!this.crash) {
            try {
               mc.m_91291_().m_174269_(item, TransformType.THIRD_PERSON_RIGHT_HAND, packedLight, OverlayTexture.f_118083_, matrixStack, buffer, 0);
            } catch (Throwable e) {
               this.crash = true;
            }
         } else if (!this.crash2) {
            try {
               mc.m_91291_().m_174269_(item, TransformType.NONE, packedLight, OverlayTexture.f_118083_, matrixStack, buffer, 0);
            } catch (Throwable ee) {
               this.crash2 = true;
            }
         } else {
            mc.m_91291_().m_174269_(new ItemStack(Blocks.f_50493_), TransformType.GROUND, packedLight, OverlayTexture.f_118083_, matrixStack, buffer, 0);
         }
      } else {
         matrixStack.m_85841_(0.5F, 0.5F, 0.5F);
         matrixStack.m_85845_(this.f_114476_.f_114358_.m_90591_());
         matrixStack.m_85845_(Vector3f.f_122225_.m_122240_(180.0F));
         mc.m_91291_().m_174269_(item, TransformType.GROUND, packedLight, OverlayTexture.f_118083_, matrixStack, buffer, 0);
      }

      if (projectile.is3D() && projectile.glows()) {
      }

      matrixStack.m_85849_();
   }

   protected ResourceLocation func_110779_a(EntityProjectile projectile) {
      return projectile.isArrow() ? field_110780_a : TextureAtlas.f_118259_;
   }

   public ResourceLocation getTextureLocation(T par1Entity) {
      return par1Entity.isArrow() ? field_110780_a : TextureAtlas.f_118259_;
   }

   public void drawVertex(
      Matrix4f matrix,
      Matrix3f normals,
      VertexConsumer vertexBuilder,
      int offsetX,
      int offsetY,
      int offsetZ,
      float textureX,
      float textureY,
      int p_229039_9_,
      int p_229039_10_,
      int p_229039_11_,
      int packedLightIn
   ) {
      vertexBuilder.m_85982_(matrix, offsetX, offsetY, offsetZ)
         .m_6122_(255, 255, 255, 255)
         .m_7421_(textureX, textureY)
         .m_86008_(OverlayTexture.f_118083_)
         .m_85969_(packedLightIn)
         .m_85977_(normals, p_229039_9_, p_229039_11_, p_229039_10_)
         .m_5752_();
   }
}
