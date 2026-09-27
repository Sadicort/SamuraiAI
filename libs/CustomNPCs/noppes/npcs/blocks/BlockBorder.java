package noppes.npcs.blocks;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import noppes.npcs.blocks.tiles.TileBorder;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class BlockBorder extends BlockInterface {
   public static final IntegerProperty ROTATION = IntegerProperty.m_61631_("rotation", 0, 3);

   public BlockBorder() {
      super(Properties.m_60926_(Blocks.f_50375_).m_60918_(SoundType.f_56742_));
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{ROTATION});
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
      ItemStack currentItem = player.m_150109_().m_36056_();
      if (!level.f_46443_ && currentItem.m_41720_() == CustomItems.wand) {
         SPacketGuiOpen.sendOpenGui(player, EnumGuiType.Border, null, pos);
         return InteractionResult.SUCCESS;
      } else {
         return InteractionResult.PASS;
      }
   }

   public BlockState m_5573_(BlockPlaceContext context) {
      if (context.m_43723_() != null) {
         int l = Mth.m_14107_(context.m_43723_().m_146908_() * 4.0F / 360.0F + 0.5) & 3;
         l %= 4;
         return (BlockState)this.m_49966_().m_61124_(ROTATION, l);
      } else {
         return super.m_5573_(context);
      }
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack item) {
      TileBorder tile = (TileBorder)level.m_7702_(pos);
      TileBorder adjacent = this.getTile(level, pos.m_122024_());
      if (adjacent == null) {
         adjacent = this.getTile(level, pos.m_122019_());
      }

      if (adjacent == null) {
         adjacent = this.getTile(level, pos.m_122012_());
      }

      if (adjacent == null) {
         adjacent = this.getTile(level, pos.m_122029_());
      }

      if (adjacent != null) {
         CompoundTag compound = new CompoundTag();
         adjacent.writeExtraNBT(compound);
         tile.readExtraNBT(compound);
      }

      tile.rotation = (Integer)state.m_61143_(ROTATION);
      if (!level.f_46443_ && entity instanceof Player) {
         SPacketGuiOpen.sendOpenGui((Player)entity, EnumGuiType.Border, null, pos);
      }
   }

   private TileBorder getTile(Level level, BlockPos pos) {
      BlockEntity tile = level.m_7702_(pos);
      return tile != null && tile instanceof TileBorder ? (TileBorder)tile : null;
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.MODEL;
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TileBorder(pos, state);
   }

   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return m_152132_(type, CustomBlocks.tile_border, TileBorder::tick);
   }
}
