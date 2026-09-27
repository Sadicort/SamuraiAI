package noppes.npcs.containers;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.wrapper.ContainerWrapper;

public class ContainerNpcInterface extends AbstractContainerMenu {
   private int posX;
   private int posZ;
   public Player player;
   public IContainer scriptContainer;

   public ContainerNpcInterface(MenuType type, int containerId, Inventory playerInventory) {
      super(type, containerId);
      this.player = playerInventory.f_35978_;
      this.posX = Mth.m_14107_(this.player.m_20185_());
      this.posZ = Mth.m_14107_(this.player.m_20189_());
      this.player.m_20256_(Vec3.f_82478_);
   }

   public ItemStack m_7648_(Player p_38941_, int p_38942_) {
      return ItemStack.f_41583_;
   }

   public boolean m_6875_(Player player) {
      return !player.m_213877_() && this.posX == Mth.m_14107_(player.m_20185_()) && this.posZ == Mth.m_14107_(player.m_20189_());
   }

   public static IContainer getOrCreateIContainer(ContainerNpcInterface container) {
      return container.scriptContainer != null ? container.scriptContainer : (container.scriptContainer = new ContainerWrapper(container));
   }
}
