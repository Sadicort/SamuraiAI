package noppes.npcs.controllers.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class BlockData {
   public BlockPos pos;
   public BlockState state;
   public CompoundTag tile;
   private ItemStack stack;

   public BlockData(BlockPos pos, BlockState state, CompoundTag tile) {
      this.pos = pos;
      this.state = state;
      this.tile = tile;
   }

   public CompoundTag getNBT() {
      CompoundTag compound = new CompoundTag();
      compound.m_128405_("BuildX", this.pos.m_123341_());
      compound.m_128405_("BuildY", this.pos.m_123342_());
      compound.m_128405_("BuildZ", this.pos.m_123343_());
      compound.m_128359_("Block", ForgeRegistries.BLOCKS.getKey(this.state.m_60734_()).toString());
      if (this.tile != null) {
         compound.m_128365_("Tile", this.tile);
      }

      return compound;
   }

   public static BlockData getData(CompoundTag compound) {
      BlockPos pos = new BlockPos(compound.m_128451_("BuildX"), compound.m_128451_("BuildY"), compound.m_128451_("BuildZ"));
      Block b = (Block)ForgeRegistries.BLOCKS.getValue(new ResourceLocation(compound.m_128461_("Block")));
      if (b == null) {
         return null;
      }

      CompoundTag tile = null;
      if (compound.m_128441_("Tile")) {
         tile = compound.m_128469_("Tile");
      }

      return new BlockData(pos, b.m_49966_(), tile);
   }

   public ItemStack getStack() {
      if (this.stack == null) {
         this.stack = new ItemStack(this.state.m_60734_(), 1);
      }

      return this.stack;
   }
}
