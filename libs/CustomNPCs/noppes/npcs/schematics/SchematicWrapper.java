package noppes.npcs.schematics;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class SchematicWrapper {
   public static final int buildSize = 10000;
   private BlockPos offset = BlockPos.f_121853_;
   private BlockPos start = BlockPos.f_121853_;
   public ISchematic schema;
   public int buildPos;
   public int size;
   public int rotation = 0;
   private Level level;
   public boolean isBuilding = false;
   public boolean firstLayer = true;
   private Map<ChunkPos, CompoundTag>[] tileEntities;

   public SchematicWrapper(ISchematic schematic) {
      this.schema = schematic;
      this.size = schematic.getWidth() * schematic.getHeight() * schematic.getLength();
      this.tileEntities = new Map[schematic.getHeight()];

      for (int i = 0; i < schematic.getBlockEntityDimensions(); i++) {
         CompoundTag teTag = schematic.getBlockEntity(i);
         int x = teTag.m_128451_("x");
         int y = teTag.m_128451_("y");
         int z = teTag.m_128451_("z");
         Map<ChunkPos, CompoundTag> map = this.tileEntities[y];
         if (map == null) {
            this.tileEntities[y] = map = new HashMap<>();
         }

         map.put(new ChunkPos(x, z), teTag);
      }
   }

   public void load(Schematic s) {
   }

   public void init(BlockPos pos, Level level, int rotation) {
      this.start = pos;
      this.level = level;
      this.rotation = rotation;
   }

   public void offset(int x, int y, int z) {
      this.offset = new BlockPos(x, y, z);
   }

   public void build() {
      if (this.level != null && this.isBuilding) {
         long endPos = this.buildPos + 10000;
         if (endPos > this.size) {
            endPos = this.size;
         }

         for (; this.buildPos < endPos; this.buildPos++) {
            int x = this.buildPos % this.schema.getWidth();
            int z = (this.buildPos - x) / this.schema.getWidth() % this.schema.getLength();
            int y = ((this.buildPos - x) / this.schema.getWidth() - z) / this.schema.getLength();
            if (this.firstLayer) {
               this.place(x, y, z, 1);
            } else {
               this.place(x, y, z, 2);
            }
         }

         if (this.buildPos >= this.size) {
            if (this.firstLayer) {
               this.firstLayer = false;
               this.buildPos = 0;
            } else {
               this.isBuilding = false;
            }
         }
      }
   }

   public void place(int x, int y, int z, int flag) {
      BlockState state = this.schema.getBlockState(x, y, z);
      if (state != null
         && (flag != 1 || state.m_60838_(EmptyBlockGetter.INSTANCE, BlockPos.f_121853_) || state.m_60734_() == Blocks.f_50016_)
         && (flag != 2 || !state.m_60838_(EmptyBlockGetter.INSTANCE, BlockPos.f_121853_) && state.m_60734_() != Blocks.f_50016_)) {
         int rotation = this.rotation / 90;
         BlockPos pos = this.start.m_121955_(this.rotatePos(x, y, z, rotation));
         state = this.rotationState(state, rotation);
         this.level.m_7731_(pos, state, 2);
         if (state.m_60734_() instanceof EntityBlock) {
            BlockEntity tile = this.level.m_7702_(pos);
            if (tile != null) {
               CompoundTag comp = this.getBlockEntity(x, y, z, pos);
               if (comp != null) {
                  tile.m_142466_(comp);
               }
            }
         }
      }
   }

   public BlockState rotationState(BlockState state, int rotation) {
      if (rotation == 0) {
         return state;
      }

      for (Property prop : state.m_61147_()) {
         if (prop instanceof DirectionProperty) {
            Direction direction = (Direction)state.m_61143_(prop);
            if (direction != Direction.UP && direction != Direction.DOWN) {
               for (int i = 0; i < rotation; i++) {
                  direction = direction.m_122427_();
               }

               return (BlockState)state.m_61124_(prop, direction);
            }
         }
      }

      return state;
   }

   public CompoundTag getBlockEntity(int x, int y, int z, BlockPos pos) {
      if (y < this.tileEntities.length && this.tileEntities[y] != null) {
         CompoundTag compound = this.tileEntities[y].get(new ChunkPos(x, z));
         if (compound == null) {
            return null;
         }

         compound = compound.m_6426_();
         compound.m_128405_("x", pos.m_123341_());
         compound.m_128405_("y", pos.m_123342_());
         compound.m_128405_("z", pos.m_123343_());
         return compound;
      } else {
         return null;
      }
   }

   public CompoundTag getNBTSmall() {
      CompoundTag compound = new CompoundTag();
      compound.m_128376_("Width", this.schema.getWidth());
      compound.m_128376_("Height", this.schema.getHeight());
      compound.m_128376_("Length", this.schema.getLength());
      compound.m_128359_("SchematicName", this.schema.getName());
      ListTag list = new ListTag();

      for (int i = 0; i < this.size && i < 25000; i++) {
         BlockState state = this.schema.getBlockState(i);
         if (state.m_60734_() != Blocks.f_50016_ && state.m_60734_() != Blocks.f_50454_) {
            list.add(NbtUtils.m_129202_(this.schema.getBlockState(i)));
         } else {
            list.add(new CompoundTag());
         }
      }

      compound.m_128365_("Data", list);
      return compound;
   }

   public BlockPos rotatePos(int x, int y, int z, int rotation) {
      if (rotation == 1) {
         return new BlockPos(this.schema.getLength() - z - 1, y, x);
      } else if (rotation == 2) {
         return new BlockPos(this.schema.getWidth() - x - 1, y, this.schema.getLength() - z - 1);
      } else {
         return rotation == 3 ? new BlockPos(z, y, this.schema.getWidth() - x - 1) : new BlockPos(x, y, z);
      }
   }

   public int getPercentage() {
      double l = this.buildPos + (this.firstLayer ? 0 : this.size);
      return (int)(l / this.size * 50.0);
   }
}
