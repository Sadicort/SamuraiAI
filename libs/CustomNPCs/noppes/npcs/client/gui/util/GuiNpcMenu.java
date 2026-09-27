package noppes.npcs.client.gui.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import noppes.npcs.CustomNpcs;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketMenuClose;
import noppes.npcs.packets.server.SPacketNpcDelete;
import noppes.npcs.shared.client.gui.components.GuiMenuTopButton;
import noppes.npcs.shared.client.gui.components.GuiTextFieldNop;
import noppes.npcs.shared.client.gui.listeners.IGuiInterface;

public class GuiNpcMenu {
   private IGuiInterface parent;
   private GuiMenuTopButton[] topButtons = new GuiMenuTopButton[0];
   private int activeMenu;
   private EntityNPCInterface npc;

   public GuiNpcMenu(IGuiInterface parent, int activeMenu, EntityNPCInterface npc) {
      this.parent = parent;
      this.activeMenu = activeMenu;
      this.npc = npc;
   }

   public void initGui(int guiLeft, int guiTop, int width) {
      final Minecraft mc = Minecraft.m_91087_();
      mc.f_91068_.m_90926_(true);
      GuiMenuTopButton display = new GuiMenuTopButton(this.parent, 1, guiLeft + 4, guiTop - 17, "menu.display") {
         @Override
         public void m_5716_(double x, double y) {
            GuiNpcMenu.this.save();
            GuiNpcMenu.this.activeMenu = 1;
            CustomNpcs.proxy.openGui(GuiNpcMenu.this.npc, EnumGuiType.MainMenuDisplay);
         }
      };
      GuiMenuTopButton stats = new GuiMenuTopButton(this.parent, 2, display.f_93620_ + display.m_5711_(), guiTop - 17, "menu.stats") {
         @Override
         public void m_5716_(double x, double y) {
            GuiNpcMenu.this.save();
            GuiNpcMenu.this.activeMenu = 2;
            CustomNpcs.proxy.openGui(GuiNpcMenu.this.npc, EnumGuiType.MainMenuStats);
         }
      };
      GuiMenuTopButton ai = new GuiMenuTopButton(this.parent, 3, stats.f_93620_ + stats.m_5711_(), guiTop - 17, "menu.ai") {
         @Override
         public void m_5716_(double x, double y) {
            GuiNpcMenu.this.save();
            GuiNpcMenu.this.activeMenu = 3;
            CustomNpcs.proxy.openGui(GuiNpcMenu.this.npc, EnumGuiType.MainMenuAI);
         }
      };
      GuiMenuTopButton inv = new GuiMenuTopButton(this.parent, 4, ai.f_93620_ + ai.m_5711_(), guiTop - 17, "menu.inventory") {
         @Override
         public void m_5716_(double x, double y) {
            GuiNpcMenu.this.save();
            GuiNpcMenu.this.activeMenu = 4;
            NoppesUtil.requestOpenGUI(EnumGuiType.MainMenuInv);
         }
      };
      GuiMenuTopButton advanced = new GuiMenuTopButton(this.parent, 5, inv.f_93620_ + inv.m_5711_(), guiTop - 17, "menu.advanced") {
         @Override
         public void m_5716_(double x, double y) {
            GuiNpcMenu.this.save();
            GuiNpcMenu.this.activeMenu = 5;
            CustomNpcs.proxy.openGui(GuiNpcMenu.this.npc, EnumGuiType.MainMenuAdvanced);
         }
      };
      GuiMenuTopButton global = new GuiMenuTopButton(this.parent, 6, advanced.f_93620_ + advanced.m_5711_(), guiTop - 17, "menu.global") {
         @Override
         public void m_5716_(double x, double y) {
            GuiNpcMenu.this.save();
            GuiNpcMenu.this.activeMenu = 6;
            CustomNpcs.proxy.openGui(GuiNpcMenu.this.npc, EnumGuiType.MainMenuGlobal);
         }
      };
      GuiMenuTopButton close = new GuiMenuTopButton(this.parent, 0, guiLeft + width - 22, guiTop - 17, "X") {
         @Override
         public void m_5716_(double x, double y) {
            GuiNpcMenu.this.close();
         }
      };
      GuiMenuTopButton delete = new GuiMenuTopButton(this.parent, 66, guiLeft + width - 72, guiTop - 17, "selectServer.delete") {
         @Override
         public void m_5716_(double x, double y) {
            ConfirmScreen guiyesno = new ConfirmScreen(
               GuiNpcMenu.this::accept,
               Component.m_237115_(""),
               Component.m_237110_("gui.deleteMessage", new Object[]{GuiNpcMenu.this.npc.m_5446_().getString()})
            );
            mc.m_91152_(guiyesno);
         }
      };
      delete.f_93620_ = close.f_93620_ - delete.m_5711_();
      this.topButtons = new GuiMenuTopButton[]{display, stats, ai, inv, advanced, global, close, delete};

      for (GuiMenuTopButton button : this.topButtons) {
         button.active = button.id == this.activeMenu;
      }
   }

   private void save() {
      GuiTextFieldNop.unfocus();
      this.parent.save();
   }

   private void close() {
      ((Screen)this.parent).m_7379_();
      if (this.npc != null) {
         this.npc.reset();
         Packets.sendServer(new SPacketMenuClose());
      }
   }

   public boolean mouseClicked(double i, double j, int k) {
      if (k == 0) {
         Minecraft mc = Minecraft.m_91087_();

         for (GuiMenuTopButton button : this.topButtons) {
            if (button.m_6375_(i, j, k)) {
               return true;
            }
         }
      }

      return false;
   }

   public void drawElements(PoseStack matrixStack, Font font, int i, int j, Minecraft mc, float f) {
      for (GuiMenuTopButton button : this.topButtons) {
         button.m_6305_(matrixStack, i, j, f);
      }
   }

   public void accept(boolean flag) {
      Minecraft mc = Minecraft.m_91087_();
      if (flag) {
         Packets.sendServer(new SPacketNpcDelete());
         mc.m_91152_(null);
         mc.f_91067_.m_91601_();
      } else {
         NoppesUtil.openGUI(mc.f_91074_, this.parent);
      }
   }
}
