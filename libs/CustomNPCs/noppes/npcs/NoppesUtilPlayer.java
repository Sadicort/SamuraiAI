package noppes.npcs;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NoppesUtilPlayer {
   public static boolean compareItems(ItemStack item, ItemStack item2, boolean ignoreDamage, boolean ignoreNBT) {
      return !NoppesUtilServer.IsItemStackNull(item) && !NoppesUtilServer.IsItemStackNull(item2)
         ? compareItemDetails(item, item2, ignoreDamage, ignoreNBT)
         : false;
   }

   private static boolean compareItemDetails(ItemStack item, ItemStack item2, boolean ignoreDamage, boolean ignoreNBT) {
      if (item.m_41720_() != item2.m_41720_()) {
         return false;
      } else if (!ignoreDamage && item.m_41773_() != -1 && item.m_41773_() != item2.m_41773_()) {
         return false;
      } else {
         return ignoreNBT || item.m_41783_() == null || item2.m_41783_() != null && item.m_41783_().equals(item2.m_41783_())
            ? ignoreNBT || item2.m_41783_() == null || item.m_41783_() != null
            : false;
      }
   }

   public static boolean compareItems(Player player, ItemStack item, boolean ignoreDamage, boolean ignoreNBT) {
      int size = 0;

      for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
         ItemStack is = player.m_150109_().m_8020_(i);
         if (!NoppesUtilServer.IsItemStackNull(is) && compareItems(item, is, ignoreDamage, ignoreNBT)) {
            size += is.m_41613_();
         }
      }

      return size >= item.m_41613_();
   }

   public static void consumeItem(Player player, ItemStack item, boolean ignoreDamage, boolean ignoreNBT) {
      if (!NoppesUtilServer.IsItemStackNull(item)) {
         int size = item.m_41613_();

         for (int i = 0; i < player.m_150109_().m_6643_(); i++) {
            ItemStack is = player.m_150109_().m_8020_(i);
            if (!NoppesUtilServer.IsItemStackNull(is) && compareItems(item, is, ignoreDamage, ignoreNBT)) {
               if (size < is.m_41613_()) {
                  is.m_41620_(size);
                  break;
               }

               size -= is.m_41613_();
               player.m_150109_().m_6836_(i, ItemStack.f_41583_);
            }
         }
      }
   }

   public static List<ItemStack> countStacks(Container inv, boolean ignoreDamage, boolean ignoreNBT) {
      List<ItemStack> list = new ArrayList<>();

      for (int i = 0; i < inv.m_6643_(); i++) {
         ItemStack item = inv.m_8020_(i);
         if (!NoppesUtilServer.IsItemStackNull(item)) {
            boolean found = false;

            for (ItemStack is : list) {
               if (compareItems(item, is, ignoreDamage, ignoreNBT)) {
                  is.m_41764_(is.m_41613_() + item.m_41613_());
                  found = true;
                  break;
               }
            }

            if (!found) {
               list.add(item.m_41777_());
            }
         }
      }

      return list;
   }
}
