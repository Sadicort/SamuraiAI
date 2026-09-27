package noppes.npcs.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomContainer;
import noppes.npcs.controllers.RecipeController;
import noppes.npcs.controllers.data.RecipeCarpentry;

public class ContainerCarpentryBench extends AbstractContainerMenu {
   public CraftingContainer craftMatrix = new CraftingContainer(this, 4, 4);
   public Container craftResult = new ResultContainer();
   private Player player;
   private BlockPos pos;

   public ContainerCarpentryBench(int id, Inventory par1PlayerInventory, BlockPos pos) {
      super(CustomContainer.container_carpentrybench, id);
      this.pos = pos;
      this.player = par1PlayerInventory.f_35978_;
      this.m_38897_(new SlotNpcCrafting(par1PlayerInventory.f_35978_, this.craftMatrix, this.craftResult, 0, 133, 41));

      for (int var6 = 0; var6 < 4; var6++) {
         for (int var7 = 0; var7 < 4; var7++) {
            this.m_38897_(new Slot(this.craftMatrix, var7 + var6 * 4, 17 + var7 * 18, 14 + var6 * 18));
         }
      }

      for (int var61 = 0; var61 < 3; var61++) {
         for (int var7 = 0; var7 < 9; var7++) {
            this.m_38897_(new Slot(par1PlayerInventory, var7 + var61 * 9 + 9, 8 + var7 * 18, 98 + var61 * 18));
         }
      }

      for (int var71 = 0; var71 < 9; var71++) {
         this.m_38897_(new Slot(par1PlayerInventory, var71, 8 + var71 * 18, 156));
      }

      this.m_6199_(this.craftMatrix);
   }

   public void m_6199_(Container par1Container) {
      if (!this.player.f_19853_.f_46443_) {
         RecipeCarpentry recipe = RecipeController.instance.findMatchingRecipe(this.craftMatrix);
         ItemStack item = ItemStack.f_41583_;
         if (recipe != null && recipe.availability.isAvailable(this.player)) {
            item = recipe.m_5874_(this.craftMatrix);
         }

         this.craftResult.m_6836_(0, item);
         ServerPlayer plmp = (ServerPlayer)this.player;
         plmp.f_8906_.m_9829_(new ClientboundContainerSetSlotPacket(this.f_38840_, this.m_182425_(), 0, item));
      }
   }

   public void m_6877_(Player par1Player) {
      super.m_6877_(par1Player);
      if (!par1Player.f_19853_.f_46443_) {
         for (int var2 = 0; var2 < 16; var2++) {
            ItemStack var3 = this.craftMatrix.m_8016_(var2);
            if (var3 != null) {
               par1Player.m_36176_(var3, false);
            }
         }
      }
   }

   public boolean m_6875_(Player par1Player) {
      return par1Player.f_19853_.m_8055_(this.pos).m_60734_() == CustomBlocks.carpenty
         && par1Player.m_20275_(this.pos.m_123341_() + 0.5, this.pos.m_123342_() + 0.5, this.pos.m_123343_() + 0.5) <= 64.0;
   }

   public ItemStack m_7648_(Player par1Player, int par1) {
      ItemStack var2x = ItemStack.f_41583_;
      Slot var3 = (Slot)this.f_38839_.get(par1);
      if (var3 != null && var3.m_6657_()) {
         ItemStack var4 = var3.m_7993_();
         var2x = var4.m_41777_();
         if (par1 == 0) {
            if (!this.m_38903_(var4, 17, 53, true)) {
               return ItemStack.f_41583_;
            }

            var3.m_40234_(var4, var2x);
         } else if (par1 >= 17 && par1 < 44) {
            if (!this.m_38903_(var4, 44, 53, false)) {
               return ItemStack.f_41583_;
            }
         } else if (par1 >= 44 && par1 < 53) {
            if (!this.m_38903_(var4, 17, 44, false)) {
               return ItemStack.f_41583_;
            }
         } else if (!this.m_38903_(var4, 17, 53, false)) {
            return ItemStack.f_41583_;
         }

         if (var4.m_41613_() == 0) {
            var3.m_5852_(ItemStack.f_41583_);
         } else {
            var3.m_6654_();
         }

         if (var4.m_41613_() == var2x.m_41613_()) {
            return ItemStack.f_41583_;
         }

         var3.m_142406_(par1Player, var4);
      }

      return var2x;
   }

   public boolean m_5882_(ItemStack stack, Slot slotIn) {
      return slotIn.f_40218_ != this.craftResult && super.m_5882_(stack, slotIn);
   }
}
