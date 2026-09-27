package noppes.npcs.api.wrapper;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import noppes.npcs.api.item.IItemBook;

public class ItemBookWrapper extends ItemStackWrapper implements IItemBook {
   protected ItemBookWrapper(ItemStack item) {
      super(item);
   }

   @Override
   public String getTitle() {
      return this.getTag().m_128461_("title");
   }

   @Override
   public void setTitle(String title) {
      this.getTag().m_128359_("title", title);
   }

   @Override
   public String getAuthor() {
      return this.getTag().m_128461_("author");
   }

   @Override
   public void setAuthor(String author) {
      this.getTag().m_128359_("author", author);
   }

   @Override
   public String[] getText() {
      List<String> list = new ArrayList<>();
      ListTag pages = this.getTag().m_128437_("pages", 8);

      for (int i = 0; i < pages.size(); i++) {
         list.add(pages.m_128778_(i));
      }

      return list.toArray(new String[list.size()]);
   }

   @Override
   public void setText(String[] pages) {
      ListTag list = new ListTag();
      if (pages != null && pages.length > 0) {
         for (String page : pages) {
            list.add(StringTag.m_129297_(page));
         }
      }

      this.getTag().m_128365_("pages", list);
   }

   private CompoundTag getTag() {
      CompoundTag comp = this.item.m_41783_();
      if (comp == null) {
         this.item.m_41751_(comp = new CompoundTag());
      }

      return comp;
   }

   @Override
   public boolean isBook() {
      return true;
   }

   @Override
   public int getType() {
      return 1;
   }
}
