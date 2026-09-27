package noppes.npcs.blocks;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.storage.loot.LootContext.Builder;

public abstract class BlockNpcDoorInterface extends DoorBlock implements EntityBlock {
   public BlockNpcDoorInterface(Properties properties) {
      super(properties);
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      super.m_6810_(state, level, pos, newState, isMoving);
      level.m_46747_(pos);
   }

   public List<ItemStack> m_7381_(BlockState p_220076_1_, Builder p_220076_2_) {
      return Collections.emptyList();
   }

   public void m_6240_(
      Level p_180657_1_, Player p_180657_2_, BlockPos p_180657_3_, BlockState p_180657_4_, @Nullable BlockEntity p_180657_5_, ItemStack p_180657_6_
   ) {
      p_180657_2_.m_36246_(Stats.f_12949_.m_12902_(this));
      p_180657_2_.m_36399_(0.005F);
      m_49881_(p_180657_4_, p_180657_1_, p_180657_3_, p_180657_5_, p_180657_2_, p_180657_6_);
   }
}
