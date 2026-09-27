package noppes.npcs.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import noppes.npcs.CustomNpcs;
import noppes.npcs.EventHooks;
import noppes.npcs.api.handler.IRecipeHandler;
import noppes.npcs.api.handler.data.IRecipe;
import noppes.npcs.controllers.data.RecipeCarpentry;
import noppes.npcs.controllers.data.RecipesDefault;

public class RecipeController implements IRecipeHandler {
   public HashMap<ResourceLocation, RecipeCarpentry> globalRecipes = new HashMap<>();
   public HashMap<ResourceLocation, RecipeCarpentry> anvilRecipes = new HashMap<>();
   public static RecipeController instance;
   public static final int version = 1;
   public int nextId = 1;
   public static HashMap<Integer, RecipeCarpentry> syncRecipes = new HashMap<>();

   public RecipeController() {
      instance = this;
   }

   public void load() {
      this.loadCategories();
      this.reloadGlobalRecipes();
      EventHooks.onGlobalRecipesLoaded(this);
   }

   public void reloadGlobalRecipes() {
   }

   private void loadCategories() {
      File saveDir = CustomNpcs.getLevelSaveDirectory();

      try {
         File file = new File(saveDir, "recipes.dat");
         if (file.exists()) {
            this.loadCategories(file);
         } else {
            this.globalRecipes.clear();
            this.anvilRecipes.clear();
            this.loadDefaultRecipes(-1);
         }
      } catch (Exception e) {
         e.printStackTrace();

         try {
            File file = new File(saveDir, "recipes.dat_old");
            if (file.exists()) {
               this.loadCategories(file);
            }
         } catch (Exception ee) {
            e.printStackTrace();
         }
      }
   }

   private void loadDefaultRecipes(int i) {
      if (i != 1) {
         RecipesDefault.loadDefaultRecipes(i);
         this.saveCategories();
      }
   }

   private void loadCategories(File file) throws Exception {
   }

   private void saveCategories() {
      try {
         File saveDir = CustomNpcs.getLevelSaveDirectory();
         ListTag list = new ListTag();

         for (RecipeCarpentry recipe : this.globalRecipes.values()) {
            if (recipe.savesRecipe) {
               list.add(recipe.writeNBT());
            }
         }

         for (RecipeCarpentry recipe : this.anvilRecipes.values()) {
            if (recipe.savesRecipe) {
               list.add(recipe.writeNBT());
            }
         }

         CompoundTag nbttagcompound = new CompoundTag();
         nbttagcompound.m_128365_("Data", list);
         nbttagcompound.m_128405_("LastId", this.nextId);
         nbttagcompound.m_128405_("Version", 1);
         File file = new File(saveDir, "recipes.dat_new");
         File file1 = new File(saveDir, "recipes.dat_old");
         File file2 = new File(saveDir, "recipes.dat");
         NbtIo.m_128947_(nbttagcompound, new FileOutputStream(file));
         if (file1.exists()) {
            file1.delete();
         }

         file2.renameTo(file1);
         if (file2.exists()) {
            file2.delete();
         }

         file.renameTo(file2);
         if (file.exists()) {
            file.delete();
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   public RecipeCarpentry findMatchingRecipe(CraftingContainer inventoryCrafting) {
      for (RecipeCarpentry recipe : this.anvilRecipes.values()) {
         if (recipe.isValid() && recipe.m_5818_(inventoryCrafting, null)) {
            return recipe;
         }
      }

      return null;
   }

   public RecipeCarpentry getRecipe(int id) {
      if (this.globalRecipes.containsKey(id)) {
         return this.globalRecipes.get(id);
      } else {
         return this.anvilRecipes.containsKey(id) ? this.anvilRecipes.get(id) : null;
      }
   }

   public RecipeCarpentry saveRecipe(RecipeCarpentry recipe) {
      return null;
   }

   private int getUniqueId() {
      this.nextId++;
      return this.nextId;
   }

   private boolean containsRecipeName(String name) {
      return false;
   }

   public RecipeCarpentry delete(int id) {
      RecipeCarpentry recipe = this.getRecipe(id);
      return recipe == null ? null : recipe;
   }

   @Override
   public List<IRecipe> getGlobalList() {
      return new ArrayList<>(this.globalRecipes.values());
   }

   @Override
   public List<IRecipe> getCarpentryList() {
      return new ArrayList<>(this.anvilRecipes.values());
   }

   @Override
   public IRecipe addRecipe(String name, boolean global, ItemStack result, Object... objects) {
      return null;
   }

   @Override
   public IRecipe addRecipe(String name, boolean global, ItemStack result, int width, int height, ItemStack... objects) {
      NonNullList<Ingredient> list = NonNullList.m_122779_();

      for (ItemStack item : objects) {
         if (!item.m_41619_()) {
            list.add(Ingredient.m_43927_(new ItemStack[]{item}));
         }
      }

      return null;
   }
}
