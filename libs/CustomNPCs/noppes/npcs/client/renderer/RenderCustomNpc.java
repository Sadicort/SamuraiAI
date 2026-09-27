package noppes.npcs.client.renderer;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.GuardianModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.layer.LayerGlow;
import noppes.npcs.client.layer.LayerHeadwear;
import noppes.npcs.client.layer.LayerNpcCloak;
import noppes.npcs.client.layer.LayerParts;
import noppes.npcs.client.layer.LayerPreRender;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.ArmorLayerMixin;
import noppes.npcs.mixin.LivingRenderer2Mixin;
import noppes.npcs.mixin.LivingRenderer3Mixin;

public class RenderCustomNpc<T extends EntityCustomNpc, M extends HumanoidModel<T>> extends RenderNPCInterface<T, M> {
   private float partialTicks;
   private LivingEntity entity;
   private EntityNPCInterface npc;
   private LivingEntityRenderer renderEntity;
   public M npcmodel;
   public Model otherModel;
   public ArmorLayerMixin armorLayer;
   public List<RenderLayer<T, M>> npclayers = Lists.newArrayList();
   private RenderLayer renderLayer = new RenderLayer(null) {
      public void m_6494_(
         PoseStack mStack,
         MultiBufferSource typeBuffer,
         int lightmapUV,
         Entity p_225628_4_,
         float limbSwing,
         float limbSwingAmount,
         float partialTicks,
         float age,
         float netHeadYaw,
         float headPitch
      ) {
         for (Object layer : ((LivingRenderer2Mixin)RenderCustomNpc.this.renderEntity).layers()) {
            ((RenderLayer)layer)
               .m_6494_(mStack, typeBuffer, lightmapUV, RenderCustomNpc.this.entity, limbSwing, limbSwingAmount, partialTicks, age, netHeadYaw, headPitch);
         }
      }
   };
   private final HumanoidModel renderModel;

   public RenderCustomNpc(Context manager, M model) {
      super(manager, model, 0.5F);
      this.npcmodel = model;
      this.m_115326_(new CustomHeadLayer(this, manager.m_174027_(), manager.m_234598_()));
      this.m_115326_(new LayerHeadwear(this));
      this.m_115326_(new LayerNpcCloak(this));
      this.m_115326_(new LayerParts(this));
      this.m_115326_(new ItemInHandLayer(this, manager.m_234598_()));
      this.m_115326_(new LayerGlow(this));
      HumanoidArmorLayer armorLayer = new HumanoidArmorLayer(
         this, new HumanoidModel(manager.m_174023_(ModelLayers.f_171164_)), new HumanoidModel(manager.m_174023_(ModelLayers.f_171165_))
      );
      this.m_115326_(armorLayer);
      this.armorLayer = (ArmorLayerMixin)armorLayer;
      this.renderModel = new HumanoidModel(manager.m_174023_(ModelLayers.f_171162_)) {
         public void m_7695_(PoseStack mStack, VertexConsumer iVertex, int lightmapUV, int packedOverlayIn, float red, float green, float blue, float alpha) {
            int color = RenderCustomNpc.this.npc.display.getTint();
            if (color < 16777215) {
               red = (color >> 16 & 0xFF) / 255.0F;
               green = (color >> 8 & 0xFF) / 255.0F;
               blue = (color & 0xFF) / 255.0F;
            }

            RenderCustomNpc.this.otherModel.m_7695_(mStack, iVertex, lightmapUV, packedOverlayIn, red, green, blue, alpha);
         }

         public void m_6973_(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            if (RenderCustomNpc.this.otherModel instanceof EntityModel em) {
               if (RenderCustomNpc.this.otherModel instanceof WolfModel) {
                  em.m_6973_(RenderCustomNpc.this.entity, limbSwing, limbSwingAmount, (float) (Math.PI * 3.0 / 4.0), netHeadYaw, headPitch);
               } else if (RenderCustomNpc.this.otherModel instanceof SquidModel
                  || RenderCustomNpc.this.otherModel instanceof ChickenModel
                  || RenderCustomNpc.this.otherModel instanceof ParrotModel) {
                  em.m_6973_(RenderCustomNpc.this.entity, limbSwing, limbSwingAmount, 0.0F, netHeadYaw, headPitch);
               } else if (RenderCustomNpc.this.otherModel instanceof GuardianModel) {
                  em.m_6973_(
                     RenderCustomNpc.this.entity,
                     limbSwing,
                     limbSwingAmount,
                     RenderCustomNpc.this.entity.f_19797_ + Minecraft.m_91087_().getPartialTick(),
                     netHeadYaw,
                     headPitch
                  );
               } else {
                  em.m_6973_(RenderCustomNpc.this.entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
               }
            }
         }

         public void m_6839_(Entity npc, float animationPos, float animationSpeed, float partialTicks) {
            if (PixelmonHelper.isPixelmon(RenderCustomNpc.this.entity)) {
               Model pixModel = (Model)PixelmonHelper.getModel(RenderCustomNpc.this.entity);
               if (pixModel != null) {
                  RenderCustomNpc.this.otherModel = pixModel;
                  PixelmonHelper.setupModel(RenderCustomNpc.this.entity, pixModel);
               }
            }

            if (RenderCustomNpc.this.otherModel instanceof HumanoidModel bm) {
               bm.f_102818_ = ((EntityCustomNpc)npc).m_20998_(partialTicks);
               bm.f_102817_ = RenderCustomNpc.this.npcmodel.f_102817_;
            }

            if (RenderCustomNpc.this.otherModel instanceof EntityModel em) {
               em.f_102609_ = RenderCustomNpc.this.entity.m_20159_()
                  && RenderCustomNpc.this.entity.m_20202_() != null
                  && RenderCustomNpc.this.entity.m_20202_().shouldRiderSit();
               em.f_102610_ = RenderCustomNpc.this.entity.m_6162_();
               em.f_102608_ = RenderCustomNpc.this.m_115342_((EntityCustomNpc)npc, partialTicks);
               em.m_6839_(RenderCustomNpc.this.entity, animationPos, animationSpeed, partialTicks);
            }
         }
      };
   }

   public Vec3 getRenderOffset(T npc, float partialTicks) {
      float xOffset = 0.0F;
      float yOffset = npc.currentAnimation == 0 ? npc.ais.bodyOffsetY / 10.0F - 0.5F : 0.0F;
      float zOffset = 0.0F;
      if (npc.m_6084_()) {
         if (npc.m_5803_()) {
            xOffset = (float)(-Math.cos(Math.toRadians(180 - npc.ais.orientation)));
            zOffset = (float)(-Math.sin(Math.toRadians(npc.ais.orientation)));
            yOffset += 0.14F;
         } else if (npc.currentAnimation == 1 || npc.m_20159_()) {
            yOffset -= 0.5F - npc.modelData.getLegsY() * 0.8F;
         } else if (npc.m_6047_()) {
            yOffset = (float)(yOffset - 0.125);
         }
      }

      return new Vec3(xOffset, yOffset * (npc.display.getSize() / 5.0F), zOffset);
   }

   public void render(T npc, float entityYaw, float partialTicks, PoseStack matrixStack, MultiBufferSource buffer, int packedLight) {
      this.npc = npc;
      this.partialTicks = partialTicks;
      Entity prevEntity = this.entity;
      this.entity = npc.modelData.getEntity(npc);
      if (prevEntity != null && this.entity == null) {
         this.f_115290_ = this.npcmodel;
         this.renderEntity = null;
         this.f_115291_.clear();
         this.f_115291_.addAll(this.npclayers);
      }

      if (this.entity != null) {
         EntityRenderer render = this.f_114476_.m_114382_(this.entity);
         if (npc.modelData.simpleRender) {
            this.renderEntity = null;
            matrixStack.m_85836_();
            render.m_7392_(this.entity, entityYaw, partialTicks, matrixStack, buffer, packedLight);
            this.renderNameTag(npc, Component.m_237119_(), matrixStack, buffer, packedLight);
            matrixStack.m_85849_();
            return;
         }

         if (render instanceof LivingEntityRenderer) {
            this.renderEntity = (LivingEntityRenderer)render;
            this.otherModel = this.renderEntity.m_7200_();
            this.f_115290_ = this.renderModel;
            this.f_115291_.clear();
            this.f_115291_.add(this.renderLayer);
            if (render instanceof RenderCustomNpc) {
               for (Object layer : ((LivingRenderer2Mixin)this.renderEntity).layers()) {
                  if (layer instanceof LayerPreRender) {
                     ((LayerPreRender)layer).preRender((EntityCustomNpc)this.entity);
                  }
               }
            }
         } else {
            this.renderEntity = null;
            this.entity = null;
            this.f_115290_ = this.npcmodel;
            this.f_115291_.clear();
            this.f_115291_.addAll(this.npclayers);
         }
      } else {
         for (RenderLayer<T, M> layer : this.f_115291_) {
            if (layer instanceof LayerPreRender) {
               ((LayerPreRender)layer).preRender(npc);
            }
         }
      }

      this.npcmodel.f_102816_ = this.getPose(npc, npc.m_21205_());
      this.npcmodel.f_102815_ = this.getPose(npc, npc.m_21206_());
      super.render(npc, entityYaw, partialTicks, matrixStack, buffer, packedLight);
   }

   protected RenderType getRenderType(T p_230496_1_, boolean p_230496_2_, boolean p_230496_3_, boolean p_230496_4_) {
      ResourceLocation resourcelocation = this.getTextureLocation(p_230496_1_);
      return p_230496_2_ && this.f_115290_ == this.renderModel
         ? this.otherModel.m_103119_(resourcelocation)
         : super.m_7225_(p_230496_1_, p_230496_2_, p_230496_3_, p_230496_4_);
   }

   public ArmPose getPose(T npc, ItemStack item) {
      if (NoppesUtilServer.IsItemStackNull(item)) {
         return ArmPose.EMPTY;
      }

      if (npc.m_21212_() > 0) {
         UseAnim enumaction = item.m_41780_();
         if (enumaction == UseAnim.BLOCK) {
            return ArmPose.BLOCK;
         }

         if (enumaction == UseAnim.BOW) {
            return ArmPose.BOW_AND_ARROW;
         }
      }

      return ArmPose.ITEM;
   }

   protected void scale(T npc, PoseStack matrixScale, float f) {
      if (this.renderEntity != null) {
         this.renderColor(npc);
         int size = npc.display.getSize();
         if (this.entity instanceof EntityNPCInterface) {
            ((EntityNPCInterface)this.entity).display.setSize(5);
         }

         EntityRenderer render = this.f_114476_.m_114382_(this.entity);
         if (!npc.modelData.simpleRender && render instanceof LivingEntityRenderer) {
            ((LivingRenderer3Mixin)render).callScale(this.entity, matrixScale, this.partialTicks);
         }

         npc.display.setSize(size);
         matrixScale.m_85841_(0.2F * npc.display.getSize(), 0.2F * npc.display.getSize(), 0.2F * npc.display.getSize());
      } else {
         super.scale(npc, matrixScale, f);
      }
   }
}
