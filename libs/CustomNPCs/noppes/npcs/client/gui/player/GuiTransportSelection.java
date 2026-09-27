package noppes.npcs.client.gui.player;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import java.util.Vector;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketPlayerTransport;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import noppes.npcs.shared.client.gui.listeners.IScrollData;
import noppes.npcs.shared.client.gui.listeners.ITopButtonListener;

public class GuiTransportSelection extends GuiNPCInterface implements ITopButtonListener, IScrollData {
   private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/smallbg.png");
   protected int imageWidth = 176;
   protected int guiLeft;
   protected int guiTop;
   private GuiCustomScrollNop scroll;

   public GuiTransportSelection(EntityNPCInterface npc) {
      super(npc);
      this.drawDefaultBackground = false;
      this.title = "";
   }

   @Override
   public void m_7856_() {
      super.m_7856_();
      this.guiLeft = (this.f_96543_ - this.imageWidth) / 2;
      this.guiTop = (this.f_96544_ - 222) / 2;
      String name = "";
      this.addLabel(new GuiLabel(0, name, this.guiLeft + (this.imageWidth - this.f_96547_.m_92895_(name)) / 2, this.guiTop + 10));
      this.addButton(new GuiButtonNop(this, 0, this.guiLeft + 10, this.guiTop + 192, 156, 20, I18n.m_118938_("transporter.travel", new Object[0])));
      if (this.scroll == null) {
         this.scroll = new GuiCustomScrollNop(this, 0);
      }

      this.scroll.setSize(156, 165);
      this.scroll.guiLeft = this.guiLeft + 10;
      this.scroll.guiTop = this.guiTop + 20;
      this.addScroll(this.scroll);
   }

   @Override
   public void m_6305_(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
      this.m_7333_(matrixStack);
      RenderSystem.m_157427_(GameRenderer::m_172817_);
      RenderSystem.m_157429_(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.m_157456_(0, this.resource);
      this.m_93228_(matrixStack, this.guiLeft, this.guiTop, 0, 0, 176, 222);
      super.m_6305_(matrixStack, mouseX, mouseY, partialTicks);
   }

   @Override
   public void buttonEvent(GuiButtonNop guibutton) {
      GuiButtonNop button = guibutton;
      String sel = this.scroll.getSelected();
      if (button.id == 0 && sel != null) {
         Packets.sendServer(new SPacketPlayerTransport(sel));
         this.close();
      }
   }

   @Override
   public boolean m_6375_(double i, double j, int k) {
      super.m_6375_(i, j, k);
      this.scroll.m_6375_(i, j, k);
      return true;
   }

   @Override
   public void save() {
   }

   @Override
   public void setData(Vector<String> list, Map<String, Integer> data) {
      this.scroll.setList(list);
   }

   @Override
   public void setSelected(String selected) {
   }
}
