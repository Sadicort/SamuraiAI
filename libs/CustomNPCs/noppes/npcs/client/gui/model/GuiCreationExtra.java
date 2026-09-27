package noppes.npcs.client.gui.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import noppes.npcs.controllers.PixelmonHelper;
import noppes.npcs.entity.EntityFakeLiving;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiButtonBiDirectional;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiButtonYesNo;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.listeners.ICustomScrollListener;

public class GuiCreationExtra extends GuiCreationScreenInterface implements ICustomScrollListener {
   private final String[] ignoredTags = new String[]{"CanBreakDoors", "Bred", "PlayerCreated", "HasReproduced"};
   private final String[] booleanTags = new String[0];
   private GuiCustomScrollNop scroll;
   private Map<String, GuiCreationExtra.GuiType> data = new HashMap<>();
   private GuiCreationExtra.GuiType selected;

   public GuiCreationExtra(EntityNPCInterface npc) {
      super(npc);
      this.active = 2;
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      if (this.entity != null) {
         if (this.scroll == null) {
            this.data = this.getData(this.entity);
            this.scroll = new GuiCustomScrollNop(this, 0);
            List<String> list = new ArrayList<>(this.data.keySet());
            this.scroll.setList(list);
            if (list.isEmpty()) {
               return;
            }

            this.scroll.setSelected(list.get(0));
         }

         this.selected = this.data.get(this.scroll.getSelected());
         if (this.selected != null) {
            this.scroll.guiLeft = this.guiLeft;
            this.scroll.guiTop = this.guiTop + 46;
            this.scroll.setSize(100, this.imageHeight - 74);
            this.addScroll(this.scroll);
            this.selected.init();
         }
      }
   }

   public Map<String, GuiCreationExtra.GuiType> getData(LivingEntity entity) {
      Map<String, GuiCreationExtra.GuiType> data = new HashMap<>();
      CompoundTag compound = this.getExtras(entity);

      for (String name : compound.m_128431_()) {
         if (!this.isIgnored(name)) {
            Tag base = compound.m_128423_(name);
            if (name.equals("Age")) {
               data.put("Child", new GuiCreationExtra.GuiTypeBoolean("Child", entity.m_6162_()));
            } else if (name.equals("Color") && base.m_7060_() == 1) {
               data.put("Color", new GuiCreationExtra.GuiTypeByte("Color", compound.m_128445_("Color")));
            } else if (base.m_7060_() == 1) {
               byte b = ((ByteTag)base).m_7063_();
               if (b == 0 || b == 1) {
                  if (this.playerdata.extra.m_128441_(name)) {
                     b = this.playerdata.extra.m_128445_(name);
                  }

                  data.put(name, new GuiCreationExtra.GuiTypeBoolean(name, b == 1));
               }
            }
         }
      }

      if (PixelmonHelper.isPixelmon(entity)) {
         data.put("Model", new GuiCreationExtra.GuiTypePixelmon("Model"));
      }

      if (entity.m_20078_().equals("tgvstyle.Dog")) {
         data.put("Breed", new GuiCreationExtra.GuiTypeDoggyStyle("Breed"));
      }

      return data;
   }

   private boolean isIgnored(String tag) {
      for (String s : this.ignoredTags) {
         if (s.equals(tag)) {
            return true;
         }
      }

      return false;
   }

   private void updateTexture() {
      LivingEntity entity = this.playerdata.getEntity(this.npc);
      EntityRenderer render = this.f_96541_.m_91290_().m_114382_(entity);
      this.npc.display.setSkinTexture(render.m_5478_(entity).toString());
   }

   private CompoundTag getExtras(LivingEntity entity) {
      CompoundTag fake = new CompoundTag();
      new EntityFakeLiving(entity.f_19853_).m_7380_(fake);
      CompoundTag compound = new CompoundTag();

      try {
         entity.m_7380_(compound);
      } catch (Throwable var7) {
      }

      for (String name : fake.m_128431_()) {
         compound.m_128473_(name);
      }

      return compound;
   }

   @Override
   public void scrollClicked(double i, double j, int k, GuiCustomScrollNop scroll) {
      if (scroll.id == 0) {
         this.m_7856_();
      } else if (this.selected != null) {
         this.selected.scrollClicked(i, j, k, scroll);
      }
   }

   @Override
   public void buttonEvent(GuiButtonNop btn) {
      if (this.selected != null) {
         this.selected.buttonEvent(btn);
      }
   }

   @Override
   public void scrollDoubleClicked(String selection, GuiCustomScrollNop scroll) {
   }

   abstract class GuiType {
      public String name;

      public GuiType(String name) {
         this.name = name;
      }

      public void init() {
      }

      public void buttonEvent(GuiButtonNop button) {
      }

      public void scrollClicked(double i, double j, int k, GuiCustomScrollNop scroll) {
      }
   }

   class GuiTypeBoolean extends GuiCreationExtra.GuiType {
      private boolean bo;

      public GuiTypeBoolean(String name, boolean bo) {
         super(name);
         this.bo = bo;
      }

      @Override
      public void init() {
         GuiCreationExtra.this.addButton(
            new GuiButtonYesNo(GuiCreationExtra.this, 11, GuiCreationExtra.this.guiLeft + 120, GuiCreationExtra.this.guiTop + 50, 60, 20, this.bo)
         );
      }

      @Override
      public void buttonEvent(GuiButtonNop button) {
         if (button.id == 11) {
            this.bo = ((GuiButtonYesNo)button).getBoolean();
            if (this.name.equals("Child")) {
               GuiCreationExtra.this.playerdata.extra.m_128405_("Age", this.bo ? -24000 : 0);
               GuiCreationExtra.this.playerdata.clearEntity();
            } else {
               GuiCreationExtra.this.playerdata.extra.m_128379_(this.name, this.bo);
               GuiCreationExtra.this.playerdata.clearEntity();
               GuiCreationExtra.this.updateTexture();
            }
         }
      }
   }

   class GuiTypeByte extends GuiCreationExtra.GuiType {
      private byte b;

      public GuiTypeByte(String name, byte b) {
         super(name);
         this.b = b;
      }

      @Override
      public void init() {
         GuiCreationExtra.this.addButton(
            new GuiButtonBiDirectional(
               GuiCreationExtra.this,
               11,
               GuiCreationExtra.this.guiLeft + 120,
               GuiCreationExtra.this.guiTop + 45,
               50,
               20,
               new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"},
               this.b
            )
         );
      }

      @Override
      public void buttonEvent(GuiButtonNop button) {
         if (button.id == 11) {
            GuiCreationExtra.this.playerdata.extra.m_128344_(this.name, (byte)button.getValue());
            GuiCreationExtra.this.playerdata.clearEntity();
            GuiCreationExtra.this.updateTexture();
         }
      }
   }

   class GuiTypeDoggyStyle extends GuiCreationExtra.GuiType {
      public GuiTypeDoggyStyle(String name) {
         super(name);
      }

      @Override
      public void init() {
         Enum breed = null;

         try {
            Method method = GuiCreationExtra.this.entity.getClass().getMethod("getBreedID");
            breed = (Enum)method.invoke(GuiCreationExtra.this.entity);
         } catch (Exception var3) {
         }

         GuiCreationExtra.this.addButton(
            new GuiButtonBiDirectional(
               GuiCreationExtra.this,
               11,
               GuiCreationExtra.this.guiLeft + 120,
               GuiCreationExtra.this.guiTop + 45,
               50,
               20,
               new String[]{
                  "0",
                  "1",
                  "2",
                  "3",
                  "4",
                  "5",
                  "6",
                  "7",
                  "8",
                  "9",
                  "10",
                  "11",
                  "12",
                  "13",
                  "14",
                  "15",
                  "16",
                  "17",
                  "18",
                  "19",
                  "20",
                  "21",
                  "22",
                  "23",
                  "24",
                  "25",
                  "26"
               },
               breed.ordinal()
            )
         );
      }

      @Override
      public void buttonEvent(GuiButtonNop button) {
         if (button.id == 11) {
            int breed = button.getValue();
            LivingEntity entity = GuiCreationExtra.this.playerdata.getEntity(GuiCreationExtra.this.npc);
            GuiCreationExtra.this.playerdata.setExtra(entity, "breed", button.getValue() + "");
            GuiCreationExtra.this.updateTexture();
         }
      }
   }

   class GuiTypePixelmon extends GuiCreationExtra.GuiType {
      public GuiTypePixelmon(String name) {
         super(name);
      }

      @Override
      public void init() {
         GuiCustomScrollNop scroll = new GuiCustomScrollNop(GuiCreationExtra.this, 1);
         scroll.setSize(120, 200);
         scroll.guiLeft = GuiCreationExtra.this.guiLeft + 120;
         scroll.guiTop = GuiCreationExtra.this.guiTop + 20;
         GuiCreationExtra.this.addScroll(scroll);
         scroll.setList(PixelmonHelper.getPixelmonList());
         scroll.setSelected(PixelmonHelper.getName(GuiCreationExtra.this.entity));
      }

      @Override
      public void scrollClicked(double i, double j, int k, GuiCustomScrollNop scroll) {
         String name = scroll.getSelected();
         GuiCreationExtra.this.playerdata.setExtra(GuiCreationExtra.this.entity, "name", name);
         GuiCreationExtra.this.updateTexture();
      }
   }
}
