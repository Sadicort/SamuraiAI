package noppes.npcs.blocks;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.blocks.tiles.TileRedstoneBlock;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class BlockNpcRedstone extends BlockInterface {
   public static final BooleanProperty ACTIVE = BooleanProperty.m_61465_("active");

   public BlockNpcRedstone() {
      super(Properties.m_60926_(Blocks.f_50069_).m_60953_(state -> 12).m_60913_(50.0F, 2000.0F));
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
      if (level.f_46443_) {
         return InteractionResult.SUCCESS;
      } else {
         ItemStack currentItem = player.m_150109_().m_36056_();
         if (currentItem != null
            && currentItem.m_41720_() == CustomItems.wand
            && CustomNpcsPermissions.hasPermission((ServerPlayer)player, CustomNpcsPermissions.EDIT_BLOCKS)) {
            SPacketGuiOpen.sendOpenGui(player, EnumGuiType.RedstoneBlock, null, pos);
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.FAIL;
         }
      }
   }

   public void m_6807_(BlockState state, Level par1Level, BlockPos pos, BlockState stateNew, boolean bo) {
      par1Level.m_46672_(pos, this);
      par1Level.m_46672_(pos.m_7495_(), this);
      par1Level.m_46672_(pos.m_7494_(), this);
      par1Level.m_46672_(pos.m_122024_(), this);
      par1Level.m_46672_(pos.m_122029_(), this);
      par1Level.m_46672_(pos.m_122019_(), this);
      par1Level.m_46672_(pos.m_122012_(), this);
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack item) {
      if (!level.f_46443_ && entity instanceof Player) {
         SPacketGuiOpen.sendOpenGui((Player)entity, EnumGuiType.RedstoneBlock, null, pos);
      }
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      this.m_6807_(state, level, pos, state, isMoving);
   }

   public int m_6378_(BlockState state, BlockGetter worldIn, BlockPos pos, Direction side) {
      return this.isActivated(state);
   }

   public int m_6376_(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
      return this.isActivated(state);
   }

   public boolean m_7899_(BlockState state) {
      return true;
   }

   protected void m_7926_(Builder<Block, BlockState> builder) {
      builder.m_61104_(new Property[]{ACTIVE});
   }

   public int isActivated(BlockState state) {
      return state.m_61143_(ACTIVE) ? 15 : 0;
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TileRedstoneBlock(pos, state);
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.MODEL;
   }

   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return m_152132_(type, CustomBlocks.tile_redstoneblock, TileRedstoneBlock::tick);
   }
}
