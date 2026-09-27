package noppes.npcs.blocks;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootContext.Builder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.ForgeHooks;
import noppes.npcs.CustomBlocks;
import noppes.npcs.CustomItems;
import noppes.npcs.EventHooks;
import noppes.npcs.blocks.tiles.TileScripted;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.packets.server.SPacketGuiOpen;

public class BlockScripted extends BlockInterface {
   public static final VoxelShape AABB = Shapes.m_83064_(new AABB(0.001F, 0.001F, 0.001F, 0.998F, 0.998F, 0.998F));

   public BlockScripted() {
      super(Properties.m_60926_(Blocks.f_50069_).m_60918_(SoundType.f_56742_).m_60913_(5.0F, 10.0F));
   }

   public BlockEntity m_142194_(BlockPos pos, BlockState state) {
      return new TileScripted(pos, state);
   }

   public VoxelShape m_5940_(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return AABB;
   }

   public VoxelShape m_5939_(BlockState blockState, BlockGetter level, BlockPos pos, CollisionContext context) {
      TileScripted tile = (TileScripted)level.m_7702_(pos);
      return tile != null && tile.isPassible ? Shapes.m_83040_() : AABB;
   }

   public InteractionResult m_6227_(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult ray) {
      if (level.f_46443_) {
         return InteractionResult.SUCCESS;
      } else {
         ItemStack currentItem = player.m_150109_().m_36056_();
         if (currentItem == null || currentItem.m_41720_() != CustomItems.wand && currentItem.m_41720_() != CustomItems.scripter) {
            Vec3 vec = ray.m_82450_();
            float x = (float)(vec.f_82479_ - pos.m_123341_());
            float y = (float)(vec.f_82480_ - pos.m_123342_());
            float z = (float)(vec.f_82481_ - pos.m_123343_());
            TileScripted tile = (TileScripted)level.m_7702_(pos);
            return EventHooks.onScriptBlockInteract(tile, player, ray.m_82434_().m_122411_(), x, y, z) ? InteractionResult.FAIL : InteractionResult.SUCCESS;
         } else {
            PlayerData data = PlayerData.get(player);
            data.scriptBlockPos = pos;
            SPacketGuiOpen.sendOpenGui(player, EnumGuiType.ScriptBlock, null, pos);
            return InteractionResult.SUCCESS;
         }
      }
   }

   public void m_6402_(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack item) {
      if (!level.f_46443_ && entity instanceof Player player) {
         PlayerData data = PlayerData.get(player);
         data.scriptBlockPos = pos;
         SPacketGuiOpen.sendOpenGui(player, EnumGuiType.ScriptBlock, null, pos);
      }
   }

   public void m_7892_(BlockState state, Level level, BlockPos pos, Entity entityIn) {
      if (!level.f_46443_) {
         TileScripted tile = (TileScripted)level.m_7702_(pos);
         EventHooks.onScriptBlockCollide(tile, entityIn);
      }
   }

   public void m_141997_(BlockState state, Level level, BlockPos pos, Precipitation type) {
      if (!level.f_46443_ && type == Precipitation.RAIN) {
         TileScripted tile = (TileScripted)level.m_7702_(pos);
         EventHooks.onScriptBlockRainFill(tile);
      }
   }

   public void m_142072_(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
      if (!level.f_46443_) {
         TileScripted tile = (TileScripted)level.m_7702_(pos);
         fallDistance = EventHooks.onScriptBlockFallenUpon(tile, entity, fallDistance);
         super.m_142072_(level, state, pos, entity, fallDistance);
      }
   }

   public void m_6256_(BlockState state, Level level, BlockPos pos, Player player) {
      if (!level.f_46443_) {
         TileScripted tile = (TileScripted)level.m_7702_(pos);
         EventHooks.onScriptBlockClicked(tile, player);
      }
   }

   public void m_6810_(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
      if (!level.f_46443_) {
         TileScripted tile = (TileScripted)level.m_7702_(pos);
         EventHooks.onScriptBlockBreak(tile);
      }

      super.m_6810_(state, level, pos, newState, isMoving);
   }

   public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
      if (!level.f_46443_) {
         TileScripted tile = (TileScripted)level.m_7702_(pos);
         if (EventHooks.onScriptBlockHarvest(tile, player)) {
            return false;
         }
      }

      return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
   }

   public List<ItemStack> m_7381_(BlockState p_220076_1_, Builder p_220076_2_) {
      return Collections.emptyList();
   }

   public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
      if (!level.f_46443_) {
         TileScripted tile = (TileScripted)level.m_7702_(pos);
         if (EventHooks.onScriptBlockExploded(tile)) {
            return;
         }
      }

      super.onBlockExploded(state, level, pos, explosion);
   }

   public void m_6861_(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos pos2, boolean isMoving) {
      if (!level.f_46443_) {
         TileScripted tile = (TileScripted)level.m_7702_(pos);
         EventHooks.onScriptBlockNeighborChanged(tile, pos2);
         int power = 0;

         for (Direction enumfacing : Direction.values()) {
            int p = level.m_46681_(pos.m_121945_(enumfacing), enumfacing);
            if (p > power) {
               power = p;
            }
         }

         if (tile.prevPower != power && tile.powering <= 0) {
            tile.newPower = power;
         }
      }
   }

   public boolean m_7899_(BlockState state) {
      return true;
   }

   public int m_6378_(BlockState state, BlockGetter worldIn, BlockPos pos, Direction side) {
      return this.m_6376_(state, worldIn, pos, side);
   }

   public int m_6376_(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
      TileScripted tile = (TileScripted)level.m_7702_(pos);
      return tile != null ? tile.activePowering : 0;
   }

   public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
      TileScripted tile = (TileScripted)level.m_7702_(pos);
      return tile != null ? tile.isLadder : false;
   }

   public boolean isValidSpawn(BlockState state, BlockGetter level, BlockPos pos, Type type, @Nullable EntityType<?> entityType) {
      return true;
   }

   public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
      TileScripted tile = (TileScripted)level.m_7702_(pos);
      return tile == null ? 0 : tile.lightValue;
   }

   public boolean m_7357_(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
      TileScripted tile = (TileScripted)level.m_7702_(pos);
      return tile != null ? tile.isPassible : false;
   }

   public boolean canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
      return super.canEntityDestroy(state, level, pos, entity);
   }

   public float m_5880_(BlockState state, Player player, BlockGetter level, BlockPos pos) {
      TileScripted tile = (TileScripted)level.m_7702_(pos);
      float f = -1.0F;
      if (tile != null) {
         f = tile.blockHardness;
      }

      if (f == -1.0F) {
         return 0.0F;
      }

      int i = ForgeHooks.isCorrectToolForDrops(state, player) ? 30 : 100;
      return player.getDigSpeed(state, pos) / f / i;
   }

   public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
      TileScripted tile = (TileScripted)level.m_7702_(pos);
      return tile != null ? tile.blockResistance : 0.0F;
   }

   public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
      return m_152132_(type, CustomBlocks.tile_scripted, TileScripted::tick);
   }
}
