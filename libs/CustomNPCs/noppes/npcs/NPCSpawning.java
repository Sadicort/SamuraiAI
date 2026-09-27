package noppes.npcs;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.controllers.SpawnController;
import noppes.npcs.controllers.data.SpawnData;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.mixin.ChunkMapMixin;
import noppes.npcs.mixin.PersistentEntitySectionManagerMixin;
import noppes.npcs.mixin.ServerLevelMixin;

public class NPCSpawning {
   public static void findChunksForSpawning(ServerLevel level) {
      if (!SpawnController.instance.data.isEmpty() && level.m_46467_() % 400L == 0L) {
         EntitySectionStorage<Entity> sectionManager = ((PersistentEntitySectionManagerMixin)((ServerLevelMixin)level).entityManager()).sectionStorage();
         ChunkMap chunkManager = level.m_7726_().f_8325_;
         List<ChunkHolder> list = new ArrayList<>(((ChunkMapMixin)chunkManager).visibleChunkMap().values());
         Collections.shuffle(list);

         for (ChunkHolder chunkHolder : list) {
            LevelChunk levelchunk = chunkHolder.m_140085_();
            if (levelchunk == null) {
               break;
            }

            ChunkPos pos = levelchunk.m_7697_();
            Biome biome = (Biome)level.m_204166_(pos.m_45615_()).m_203334_();
            if (SpawnController.instance.hasSpawnList(ForgeRegistries.BIOMES.getKey(biome))) {
               AABB bb = new AABB(pos.m_45604_(), 0.0, pos.m_45605_(), pos.m_45608_(), level.m_151558_(), pos.m_45609_());
               List<Entity> entities = Lists.newArrayList();
               sectionManager.m_156863_(EntityType.f_20532_, bb.m_82400_(4.0), entities::add);
               if (entities.isEmpty()) {
                  sectionManager.m_156863_(CustomEntities.entityCustomNpc, bb, entities::add);
                  if (entities.size() < CustomNpcs.NpcNaturalSpawningChunkLimit) {
                     spawnChunk(level, levelchunk);
                  }
               }
            }
         }
      }
   }

   private static void spawnChunk(ServerLevel level, LevelChunk chunk) {
      BlockPos chunkposition = getChunk(level, chunk);
      int j1 = chunkposition.m_123341_();
      int k1 = chunkposition.m_123342_();
      int l1 = chunkposition.m_123343_();

      for (int i = 0; i < 3; i++) {
         int x = j1;
         int y = k1;
         int z = l1;
         byte b1 = 6;
         x += level.f_46441_.m_188503_(b1) - level.f_46441_.m_188503_(b1);
         z += level.f_46441_.m_188503_(b1) - level.f_46441_.m_188503_(b1);
         BlockPos pos = new BlockPos(x, y, z);
         ResourceLocation name = ForgeRegistries.BIOMES.getKey((Biome)level.m_204166_(pos).m_203334_());
         SpawnData data = SpawnController.instance.getRandomSpawnData(name);
         if (data != null && !data.data.isEmpty() && canCreatureTypeSpawnAtLocation(data, level, pos)) {
            spawnData(data, level, pos);
         }
      }
   }

   public static int countNPCs(ServerLevel level) {
      int count = 0;

      for (Entity entity : level.m_8583_()) {
         if (entity instanceof EntityNPCInterface) {
            count++;
         }
      }

      return count;
   }

   private static BlockPos getChunk(Level level, LevelChunk chunk) {
      ChunkPos chunkpos = chunk.m_7697_();
      int i = chunkpos.m_45604_() + level.f_46441_.m_188503_(16);
      int j = chunkpos.m_45605_() + level.f_46441_.m_188503_(16);
      int k = chunk.m_5885_(Types.WORLD_SURFACE, i, j) + 1;
      int l = level.f_46441_.m_188503_(k + 1);
      return new BlockPos(i, l, j);
   }

   public static void performLevelGenSpawning(Level level, Biome biome, int x, int z, RandomSource rand) {
      if (!(biome.m_47518_().m_48344_() >= 1.0F)
         && !(biome.m_47518_().m_48344_() < 0.0F)
         && SpawnController.instance.hasSpawnList(ForgeRegistries.BIOMES.getKey(biome))) {
         int tries = 0;

         while (rand.m_188501_() < biome.m_47518_().m_48344_()) {
            if (++tries > 20) {
               break;
            }

            SpawnData data = SpawnController.instance.getRandomSpawnData(ForgeRegistries.BIOMES.getKey(biome));
            int size = 16;
            int j1 = x + rand.m_188503_(size);
            int k1 = z + rand.m_188503_(size);
            int l1 = j1;
            int i2 = k1;

            for (int k2 = 0; k2 < 4; k2++) {
               BlockPos pos = getTopNonCollidingPos(level, CustomEntities.entityCustomNpc, 0, k1);
               if (canCreatureTypeSpawnAtLocation(data, level, pos)) {
                  if (spawnData(data, level, pos)) {
                     break;
                  }
               } else {
                  j1 += rand.m_188503_(5) - rand.m_188503_(5);

                  for (k1 += rand.m_188503_(5) - rand.m_188503_(5);
                     j1 < x || j1 >= x + size || k1 < z || k1 >= z + size;
                     k1 = i2 + rand.m_188503_(5) - rand.m_188503_(5)
                  ) {
                     j1 = l1 + rand.m_188503_(5) - rand.m_188503_(5);
                  }
               }
            }
         }
      }
   }

   private static boolean spawnData(SpawnData data, Level level, BlockPos pos) {
      try {
         CompoundTag nbt = data.getCompound(1);
         if (nbt == null) {
            return false;
         }

         Entity entity = (Entity)EntityType.m_20642_(nbt, level).orElse(null);
         if (entity == null || !(entity instanceof Mob entityliving)) {
            return false;
         }

         if (entity instanceof EntityCustomNpc npc) {
            npc.stats.spawnCycle = 4;
            npc.stats.respawnTime = 0;
            npc.ais.returnToStart = false;
            npc.ais.setStartPos(pos);
         }

         entity.m_7678_(pos.m_123341_() + 0.5, pos.m_123342_(), pos.m_123343_() + 0.5, level.f_46441_.m_188501_() * 360.0F, 0.0F);
      } catch (Exception exception) {
         exception.printStackTrace();
         return false;
      }

      Result canSpawn = ForgeEventFactory.canEntitySpawn(
         entityliving, level, pos.m_123341_() + 0.5F, pos.m_123342_(), pos.m_123343_() + 0.5F, null, MobSpawnType.NATURAL
      );
      if (canSpawn != Result.DENY && (canSpawn != Result.DEFAULT || entityliving.m_5545_(level, MobSpawnType.NATURAL))) {
         level.m_7654_().m_18707_(() -> level.m_7967_(entityliving));
         return true;
      } else {
         return false;
      }
   }

   public static boolean canCreatureTypeSpawnAtLocation(SpawnData data, Level level, BlockPos pos) {
      if (!level.m_6857_().m_61937_(pos) || !level.m_45772_(CustomEntities.entityCustomNpc.m_20585_(pos.m_123341_(), pos.m_123342_(), pos.m_123343_()))) {
         return false;
      }

      if ((data.type != 1 || level.m_7146_(pos) <= 8) && (data.type != 2 || level.m_7146_(pos) > 8)) {
         BlockState state = level.m_8055_(pos);
         Block block = state.m_60734_();
         if (data.liquid) {
            return state.m_60767_().m_76332_()
               && level.m_8055_(pos.m_7495_()).m_60767_().m_76332_()
               && !level.m_8055_(pos.m_7494_()).m_60796_(level, pos.m_7494_());
         }

         BlockPos blockpos1 = pos.m_7495_();
         BlockState state1 = level.m_8055_(blockpos1);
         Block block1 = state1.m_60734_();
         boolean flag = block1 != Blocks.f_50752_ && block1 != Blocks.f_50375_;
         BlockPos down = blockpos1.m_7495_();
         flag |= level.m_8055_(down).m_60734_().isValidSpawn(level.m_8055_(down), level, down, Type.ON_GROUND, CustomEntities.entityCustomNpc);
         return flag && !state.m_60803_() && !state.m_60767_().m_76332_() && !level.m_8055_(pos.m_7494_()).m_60803_();
      } else {
         return false;
      }
   }

   private static BlockPos getTopNonCollidingPos(LevelReader p_208498_0_, EntityType<?> p_208498_1_, int p_208498_2_, int p_208498_3_) {
      int i = p_208498_0_.m_6924_(Types.MOTION_BLOCKING_NO_LEAVES, p_208498_2_, p_208498_3_);
      MutableBlockPos blockpos$mutable = new MutableBlockPos(p_208498_2_, i, p_208498_3_);
      if (p_208498_0_.m_6042_().f_63856_()) {
         do {
            blockpos$mutable.m_122173_(Direction.DOWN);
         } while (!p_208498_0_.m_8055_(blockpos$mutable).m_60795_());

         do {
            blockpos$mutable.m_122173_(Direction.DOWN);
         } while (p_208498_0_.m_8055_(blockpos$mutable).m_60795_() && blockpos$mutable.m_123342_() > 0);
      }

      BlockPos blockpos = blockpos$mutable.m_7495_();
      return p_208498_0_.m_8055_(blockpos).m_60647_(p_208498_0_, blockpos, PathComputationType.LAND) ? blockpos : blockpos$mutable.m_7949_();
   }
}
