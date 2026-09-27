package noppes.npcs.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.blocks.tiles.TileBuilder;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class BlockBuilder extends BlockInterface {
   public static final IntegerProperty ROTATION = IntegerProperty.m_61631_("rotation", 0, 3);

   public BlockBuilder() {
      super(Properties.m_60926_(Blocks.f_50375_).m_60918_(SoundType.f_56742_));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{ROTATION});
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.MODEL;
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
      if (level.f_46443_) {
         return InteractionResult.SUCCESS;
      }

      ItemStack currentItem = player.m_150109_().m_36056_();
      if (currentItem.m_41720_() == CustomItems.wand || currentItem.m_41720_() == CustomBlocks.builder_item) {
         SPacketGuiOpen.sendOpenGui(player, EnumGuiType.BuilderBlock, null, pos);
      }

      return InteractionResult.SUCCESS;
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      int var6 = Mth.m_14107_(context.m_43723_().m_146908_() / 90.0F + 0.5) & 3;
      if (!context.m_43725_().f_46443_) {
         SPacketGuiOpen.sendOpenGui(context.m_43723_(), EnumGuiType.BuilderBlock, null, context.m_8083_());
      }

      return (BlockState)this.m_49966_().m_61124_(ROTATION, var6);
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TileBuilder(pos, state);
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (TileBuilder.DrawPos != null && TileBuilder.DrawPos.equals(pos)) {
         TileBuilder.SetDrawPos(null);
      }

      super.m_6810_(state, level, pos, newState, isMoving);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return m_152132_(type, CustomBlocks.tile_builder, TileBuilder::tick);
   }
}
