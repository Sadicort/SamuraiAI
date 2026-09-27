package noppes.npcs.items;

import java.util.function.Consumer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import noppes.npcs.client.CustomTileEntityItemStackRenderer;

public class ItemNpcBlock extends BlockItem {
   public final Block block;

   public ItemNpcBlock(Block block, Properties builder) {
      super(block, builder);
      this.block = block;
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(CustomTileEntityItemStackRenderer.itemRenderProperties);
   }
}
