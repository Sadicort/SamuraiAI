package noppes.npcs.client.renderer;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTexture.Type;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import java.io.File;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.MatrixStackMixin;
import noppes.npcs.shared.client.util.ImageDownloadAlt;
import noppes.npcs.shared.client.util.ResourceDownloader;
import noppes.npcs.shared.common.util.LogWriter;

public class RenderNPCInterface<T extends EntityNPCInterface, M extends EntityModel<T>> extends LivingEntityRenderer<T, M> {
   public static int LastTextureTick;
   public static EntityNPCInterface currentNpc;

   public RenderNPCInterface(Context manager, M model, float f) {
      super(manager, model, f);
   }

   public void renderNameTag(T npc, Component text, PoseStack matrixStack, MultiBufferSource buffer, int light) {
      if (npc != null && this.m_6512_(npc) && this.f_114476_ != null) {
         double d0 = this.f_114476_.m_114471_(npc);
         if (!(d0 > 512.0)) {
            matrixStack.m_85836_();
            Vec3 renderOffset = this.m_7860_(npc, 0.0F);
            matrixStack.m_85837_(-renderOffset.m_7096_(), -renderOffset.m_7098_(), -renderOffset.m_7094_());
            if (npc.messages != null) {
               float height = npc.baseSize.f_20378_ / 5.0F * npc.display.getSize();
               float offset = npc.m_20206_() * (1.2F + (!npc.display.showName() ? 0.0F : (npc.display.getTitle().isEmpty() ? 0.15F : 0.25F)));
               matrixStack.m_85837_(0.0, offset, 0.0);
               npc.messages.renderMessages(matrixStack, buffer, 0.666667F * height, npc.isInRange(this.f_114476_.f_114358_.m_90592_(), 4.0), light);
               matrixStack.m_85837_(0.0, -offset, 0.0);
            }

            if (npc.display.showName()) {
               this.renderLivingLabel(npc, matrixStack, buffer, light);
            }

            matrixStack.m_85849_();
         }
      }
   }

   protected void renderLivingLabel(T npc, PoseStack matrixStack, MultiBufferSource buffer, int light) {
      float scale = npc.baseSize.f_20378_ / 5.0F * npc.display.getSize();
      float height = npc.m_20206_() - 0.06F * scale;
      matrixStack.m_85836_();
      Font fontrenderer = this.m_114481_();
      float f2 = 0.01666667F * scale;
      matrixStack.m_85837_(0.0, height, 0.0);
      matrixStack.m_85845_(this.f_114476_.m_114470_());
      int color = npc.getFaction().color;
      matrixStack.m_85837_(0.0, scale / 6.5F * 2.0F, 0.0);
      float f1 = Minecraft.m_91087_().f_91066_.m_92141_(0.25F);
      int j = (int)(f1 * 255.0F) << 24;
      matrixStack.m_85841_(-f2, -f2, f2);
      Matrix4f matrix4f = matrixStack.m_85850_().m_85861_();
      float y = 0.0F;
      boolean nearby = npc.isInRange(this.f_114476_.f_114358_.m_90592_(), 8.0);
      if (!npc.display.getTitle().isEmpty() && nearby) {
         Component title = Component.m_237113_("<").m_7220_(Component.m_237115_(npc.display.getTitle())).m_130946_(">");
         float f3 = 0.6F;
         matrixStack.m_85837_(0.0, 4.0, 0.0);
         matrixStack.m_85841_(f3, f3, f3);
         fontrenderer.m_92841_(title, -fontrenderer.m_92852_(title) / 2, 0.0F, color, false, matrix4f, buffer, false, j, light);
         matrixStack.m_85841_(1.0F / f3, 1.0F / f3, 1.0F / f3);
         y = -10.0F;
      }

      Component name = npc.m_7755_();
      fontrenderer.m_92841_(name, -fontrenderer.m_92852_(name) / 2, y, color, false, matrix4f, buffer, nearby, j, light);
      if (nearby) {
         fontrenderer.m_92841_(name, -fontrenderer.m_92852_(name) / 2, y, color, false, matrix4f, buffer, false, 0, light);
      }

      matrixStack.m_85849_();
   }

   protected void renderColor(EntityNPCInterface npc) {
      if (npc.f_20916_ <= 0 && npc.f_20919_ <= 0) {
         float red = (npc.display.getTint() >> 16 & 0xFF) / 255.0F;
         float green = (npc.display.getTint() >> 8 & 0xFF) / 255.0F;
         float blue = (npc.display.getTint() & 0xFF) / 255.0F;
         RenderSystem.m_157429_(red, green, blue, 1.0F);
      }
   }

   protected void setupRotations(T npc, PoseStack matrixScale, float f, float f1, float f2) {
      if (npc.m_6084_() && npc.m_5803_()) {
         matrixScale.m_85845_(Vector3f.f_122225_.m_122240_(npc.ais.orientation));
         matrixScale.m_85845_(Vector3f.f_122227_.m_122240_(this.m_6441_(npc)));
         matrixScale.m_85845_(Vector3f.f_122225_.m_122240_(270.0F));
      } else if (npc.m_6084_() && npc.currentAnimation == 7) {
         matrixScale.m_85845_(Vector3f.f_122225_.m_122240_(270.0F - f1));
         float scale = ((EntityCustomNpc)npc).display.getSize() / 5.0F;
         matrixScale.m_85837_(-scale + ((EntityCustomNpc)npc).modelData.getLegsY() * scale, 0.14F, 0.0);
         matrixScale.m_85845_(Vector3f.f_122227_.m_122240_(270.0F));
         matrixScale.m_85845_(Vector3f.f_122225_.m_122240_(270.0F));
      } else {
         super.m_7523_(npc, matrixScale, f, f1, f2);
      }
   }

   protected void scale(T npc, PoseStack matrixScale, float f) {
      this.renderColor(npc);
      int size = npc.display.getSize();
      matrixScale.m_85841_(npc.scaleX / 5.0F * size, npc.scaleY / 5.0F * size, npc.scaleZ / 5.0F * size);
   }

   public void render(T npc, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
      if (npc.isKilled()) {
         this.f_114477_ = 0.0F;
      }

      if (!npc.isKilled() || !npc.stats.hideKilledBody || npc.f_20919_ <= 20) {
         float xOffset = 0.0F;
         float yOffset = npc.currentAnimation == 0 ? npc.ais.bodyOffsetY / 10.0F - 0.5F : 0.0F;
         float zOffset = 0.0F;
         if (npc.m_6084_()) {
            if (npc.m_5803_()) {
               xOffset = (float)(-Math.cos(Math.toRadians(180 - npc.ais.orientation)));
               zOffset = (float)(-Math.sin(Math.toRadians(npc.ais.orientation)));
               yOffset += 0.14F;
            } else if (npc.currentAnimation == 1 || npc.m_20159_()) {
               yOffset -= 0.5F - ((EntityCustomNpc)npc).modelData.getLegsY() * 0.8F;
            }
         }

         xOffset = xOffset / 5.0F * npc.display.getSize();
         yOffset = yOffset / 5.0F * npc.display.getSize();
         zOffset = zOffset / 5.0F * npc.display.getSize();
         if ((npc.display.getBossbar() == 1 || npc.display.getBossbar() == 2 && npc.isAttacking())
            && !npc.isKilled()
            && npc.f_20919_ <= 20
            && npc.canNpcSee(Minecraft.m_91087_().f_91074_)) {
         }

         if (npc.ais.getStandingType() == 3 && !npc.isWalking() && !npc.isInteracting()) {
            npc.f_20884_ = npc.f_20883_ = npc.ais.orientation;
         }

         this.f_114477_ = npc.m_20205_() * 0.8F;
         int stackSize = ((MatrixStackMixin)matrixStack).getStack().size();

         try {
            currentNpc = npc;
            super.m_7392_(npc, entityYaw, partialTicks, matrixStack, buffer, packedLight);
         } catch (Throwable e) {
            while (((MatrixStackMixin)matrixStack).getStack().size() > stackSize) {
               matrixStack.m_85849_();
            }

            LogWriter.except(e);
         } finally {
            currentNpc = null;
         }
      }
   }

   protected float getBob(T npc, float limbSwingAmount) {
      return !npc.isKilled() && npc.display.getHasLivingAnimation() ? super.m_6930_(npc, limbSwingAmount) : 0.0F;
   }

   public ResourceLocation getTextureLocation(T npc) {
      if (npc.textureLocation == null) {
         if (npc.display.skinType == 0) {
            npc.textureLocation = new ResourceLocation(npc.display.getSkinTexture());
         } else {
            if (LastTextureTick < 5) {
               return DefaultPlayerSkin.m_118626_();
            }

            if (npc.display.skinType == 1 && npc.display.playerProfile != null) {
               Minecraft minecraft = Minecraft.m_91087_();
               Map map = minecraft.m_91109_().m_118815_(npc.display.playerProfile);
               if (map.containsKey(Type.SKIN)) {
                  npc.textureLocation = minecraft.m_91109_().m_118825_((MinecraftProfileTexture)map.get(Type.SKIN), Type.SKIN);
               }
            } else if (npc.display.skinType == 2 && !npc.display.getSkinUrl().isEmpty()) {
               try {
                  boolean fixSkin = npc instanceof EntityCustomNpc && ((EntityCustomNpc)npc).modelData.getEntity(npc) != null;
                  File file = ResourceDownloader.getUrlFile(npc.display.getSkinUrl(), fixSkin);
                  npc.textureLocation = ResourceDownloader.getUrlResourceLocation(npc.display.getSkinUrl(), fixSkin);
                  this.loadSkin(file, npc.textureLocation, npc.display.getSkinUrl(), fixSkin);
               } catch (Exception ex) {
                  ex.printStackTrace();
               }
            }
         }
      }

      return npc.textureLocation == null ? DefaultPlayerSkin.m_118626_() : npc.textureLocation;
   }

   private void loadSkin(File file, ResourceLocation resource, String par1Str, boolean fix64) {
      TextureManager texturemanager = Minecraft.m_91087_().m_91097_();
      AbstractTexture object = texturemanager.m_174786_(resource, null);
      if (object == null) {
         ResourceDownloader.load(new ImageDownloadAlt(file, par1Str, resource, DefaultPlayerSkin.m_118626_(), fix64, () -> {}));
      }
   }
}
