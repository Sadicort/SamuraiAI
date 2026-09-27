package noppes.npcs.containers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.CustomContainer;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.controllers.data.RecipeCarpentry;

public class ContainerManageRecipes extends AbstractContainerMenu {
   private SimpleContainer craftingMatrix;
   public RecipeCarpentry recipe;
   public int size;
   public int width;
   private boolean init = false;

   public ContainerManageRecipes(int containerId, Inventory playerInventory, int size) {
      super(CustomContainer.container_managerecipes, containerId);
      this.size = size * size;
      this.width = size;
      this.craftingMatrix = new SimpleContainer(this.size + 1);
      this.recipe = new RecipeCarpentry(new ResourceLocation(""), "");
      this.m_38897_(new Slot(this.craftingMatrix, 0, 87, 61));

      for (int i = 0; i < size; i++) {
         for (int j = 0; j < size; j++) {
            this.m_38897_(new Slot(this.craftingMatrix, i * this.width + j + 1, j * 18 + 8, i * 18 + 35));
         }
      }

      for (int i1 = 0; i1 < 3; i1++) {
         for (int l1 = 0; l1 < 9; l1++) {
            this.m_38897_(new Slot(playerInventory, l1 + i1 * 9 + 9, 8 + l1 * 18, 113 + i1 * 18));
         }
      }

      for (int j1 = 0; j1 < 9; j1++) {
         this.m_38897_(new Slot(playerInventory, j1, 8 + j1 * 18, 171));
      }
   }

   public ItemStack m_7648_(Player par1Player, int i) {
      return ItemStack.f_41583_;
   }

   public boolean m_6875_(Player entityplayer) {
      return true;
   }

   public void setRecipe(RecipeCarpentry recipe) {
      this.craftingMatrix.m_6836_(0, recipe.m_8043_());

      for (int i = 0; i < this.width; i++) {
         for (int j = 0; j < this.width; j++) {
            if (j >= recipe.getRecipeWidth()) {
               this.craftingMatrix.m_6836_(i * this.width + j + 1, ItemStack.f_41583_);
            } else {
               this.craftingMatrix.m_6836_(i * this.width + j + 1, recipe.getCraftingItem(i * recipe.getRecipeWidth() + j));
            }
         }
      }

      this.recipe = recipe;
   }

   public void saveRecipe() {
      int nextChar = 0;
      char[] chars = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P'};
      Map<ItemStack, Character> nameMapping = new HashMap<>();
      int firstRow = this.width;
      int lastRow = 0;
      int firstColumn = this.width;
      int lastColumn = 0;
      boolean seenRow = false;

      for (int i = 0; i < this.width; i++) {
         boolean seenColumn = false;

         for (int j = 0; j < this.width; j++) {
            ItemStack item = this.craftingMatrix.m_8020_(i * this.width + j + 1);
            if (!NoppesUtilServer.IsItemStackNull(item)) {
               if (!seenColumn && j < firstColumn) {
                  firstColumn = j;
               }

               if (j > lastColumn) {
                  lastColumn = j;
               }

               seenColumn = true;
               Character letter = null;

               for (ItemStack mapped : nameMapping.keySet()) {
                  if (NoppesUtilPlayer.compareItems(mapped, item, this.recipe.ignoreDamage, this.recipe.ignoreNBT)) {
                     letter = nameMapping.get(mapped);
                  }
               }

               if (letter == null) {
                  letter = chars[nextChar];
                  nextChar++;
                  nameMapping.put(item, letter);
               }
            }
         }

         if (seenColumn) {
            if (!seenRow) {
               firstRow = i;
               lastRow = i;
               seenRow = true;
            } else {
               lastRow = i;
            }
         }
      }

      ArrayList<Object> recipe = new ArrayList<>();

      for (int i = 0; i < this.width; i++) {
         if (i >= firstRow && i <= lastRow) {
            String row = "";

            for (int j = 0; j < this.width; j++) {
               if (j >= firstColumn && j <= lastColumn) {
                  ItemStack item = this.craftingMatrix.m_8020_(i * this.width + j + 1);
                  if (NoppesUtilServer.IsItemStackNull(item)) {
                     row = row + " ";
                  } else {
                     for (ItemStack mapped : nameMapping.keySet()) {
                        if (NoppesUtilPlayer.compareItems(mapped, item, false, false)) {
                           row = row + nameMapping.get(mapped);
                        }
                     }
                  }
               }
            }

            recipe.add(row);
         }
      }

      if (nameMapping.isEmpty()) {
         RecipeCarpentry r = new RecipeCarpentry(new ResourceLocation("customnpcs", this.recipe.name), this.recipe.name);
         r.copy(this.recipe);
         this.recipe = r;
      } else {
         for (ItemStack mapped : nameMapping.keySet()) {
            Character letter = nameMapping.get(mapped);
            recipe.add(letter);
            recipe.add(mapped);
         }

         this.recipe = RecipeCarpentry.createRecipe(
            new ResourceLocation("customnpcs", this.recipe.name), this.recipe, this.craftingMatrix.m_8020_(0), recipe.toArray()
         );
      }
   }
}
