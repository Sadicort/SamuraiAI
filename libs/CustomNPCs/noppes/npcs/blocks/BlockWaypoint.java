package noppes.npcs.blocks;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.CustomNpcsPermissions;
import noppes.npcs.blocks.tiles.TileWaypoint;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class BlockWaypoint extends BlockInterface {
   public BlockWaypoint() {
      super(Properties.m_60926_(Blocks.f_50375_).m_60918_(SoundType.f_56743_));
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
      if (level.f_46443_) {
         return InteractionResult.PASS;
      } else {
         ItemStack currentItem = player.m_150109_().m_36056_();
         if (currentItem != null
            && currentItem.m_41720_() == CustomItems.wand
            && CustomNpcsPermissions.hasPermission((ServerPlayer)player, CustomNpcsPermissions.EDIT_BLOCKS)) {
            SPacketGuiOpen.sendOpenGui(player, EnumGuiType.Waypoint, null, pos);
            return InteractionResult.SUCCESS;
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack item) {
      if (!level.f_46443_ && entity instanceof Player) {
         SPacketGuiOpen.sendOpenGui((Player)entity, EnumGuiType.Waypoint, null, pos);
      }
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TileWaypoint(pos, state);
   }

   public RenderShape m_7514_(BlockState state) {
      return RenderShape.MODEL;
   }

   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return m_152132_(type, CustomBlocks.tile_waypoint, TileWaypoint::tick);
   }
}
