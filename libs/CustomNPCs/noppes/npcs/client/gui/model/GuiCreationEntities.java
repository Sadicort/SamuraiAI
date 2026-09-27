package noppes.npcs.client.gui.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomEntities;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

public class GuiCreationEntities extends GuiCreationScreenInterface implements ICustomScrollListener {
   private List<EntityType<? extends Entity>> types;
   private GuiCustomScrollNop scroll;
   private boolean resetToSelected = true;

   public GuiCreationEntities(EntityNPCInterface npc) {
      super(npc);
      this.types = getAllEntities(npc.f_19853_);
      Collections.sort(this.types, Comparator.comparing(t -> t.m_20675_().toLowerCase()));
      this.active = 1;
      this.xOffset = 60;
   }

   private static List<EntityType<? extends Entity>> getAllEntities(Level level) {
      List<EntityType<? extends Entity>> data = new ArrayList<>();

      for (EntityType<? extends Entity> ent : ForgeRegistries.ENTITY_TYPES.getValues()) {
         try {
            Entity e = ent.m_20615_(level);
            if (e != null) {
               if (LivingEntity.class.isAssignableFrom(e.getClass()) && !EnderDragon.class.isAssignableFrom(e.getClass())) {
                  data.add(ent);
               }

               e.m_146870_();
            }
         } catch (Exception var5) {
         }
      }

      return data;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.addButton(new GuiButtonNop(this, 10, this.guiLeft, this.guiTop + 46, 120, 20, "Reset To NPC", button -> {
         this.playerdata.setEntity(null);
         this.npc.display.setSkinTexture("customnpcs:textures/entity/humanmale/steve.png");
         this.resetToSelected = true;
         this.m_7856_();
      }));
      if (this.scroll == null) {
         this.scroll = new GuiCustomScrollNop(this, 0);
         this.scroll.setUnsortedList(this.types.stream().<String>map(EntityType::m_20675_).collect(Collectors.toList()));
      }

      this.scroll.guiLeft = this.guiLeft;
      this.scroll.guiTop = this.guiTop + 68;
      this.scroll.setSize(120, this.imageHeight - 96);
      int index = -1;
      EntityType selectedType = CustomEntities.entityCustomNpc;
      if (this.entity != null) {
         for (int i = 0; i < this.types.size(); i++) {
            EntityType type = this.types.get(i);
            if (type == this.entity.m_6095_()) {
               index = i;
               break;
            }
         }
      }

      if (index >= 0) {
         this.scroll.setSelectedIndex(index);
      } else {
         this.scroll.setSelected("entity.customnpcs.customnpc");
      }

      if (this.resetToSelected) {
         this.scroll.scrollTo(this.scroll.getSelected());
         this.resetToSelected = false;
      }

      this.addScroll(this.scroll);
      this.addLabel(new GuiLabel(110, "gui.simpleRenderer", this.guiLeft + 124, this.guiTop + 5, 16711680));
      this.addButton(
         new GuiButtonYesNo(
            this, 110, this.guiLeft + 260, this.guiTop, this.playerdata.simpleRender, b -> this.playerdata.simpleRender = ((GuiButtonYesNo)b).getBoolean()
         )
      );
   }

   @Override
   public void scrollClicked(double i, double j, int k, GuiCustomScrollNop scroll) {
      String selected = scroll.getSelected();
      if (selected.equals("entity.customnpcs.customnpc")) {
         this.playerdata.setEntity(null);
      } else {
         this.playerdata.setEntity(ForgeRegistries.ENTITY_TYPES.getKey(this.types.get(scroll.getSelectedIndex())));
      }

      Entity entity = this.playerdata.getEntity(this.npc);
      if (entity != null) {
         EntityRenderer render = this.f_96541_.m_91290_().m_114382_(entity);
         if (render instanceof LivingEntityRenderer && !render.m_5478_(entity).equals("minecraft:missingno")) {
            this.npc.display.setSkinTexture(render.m_5478_(entity).toString());
         }
      } else {
         this.npc.display.setSkinTexture("customnpcs:textures/entity/humanmale/steve.png");
      }

      this.m_7856_();
   }

   @Override
   public void scrollDoubleClicked(String selection, GuiCustomScrollNop scroll) {
   }
}
