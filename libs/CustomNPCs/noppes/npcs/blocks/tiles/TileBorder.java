package noppes.npcs.blocks.tiles;

import com.google.common.base.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import noppes.npcs.CustomBlocks;
import noppes.npcs.blocks.BlockBorder;
import noppes.npcs.controllers.data.Availability;

public class TileBorder extends TileNpcEntity implements Predicate {
   public Availability availability = new Availability();
   public AABB boundingbox;
   public int rotation = 0;
   public int height = 10;
   public String message = "availability.areaNotAvailble";

   public TileBorder(BlockPos pos, BlockState state) {
      super(CustomBlocks.tile_border, pos, state);
   }

   @Override
   public void m_142466_(CompoundTag compound) {
      super.m_142466_(compound);
      this.readExtraNBT(compound);
      if (this.m_58904_() != null) {
         this.m_58904_().m_46597_(this.m_58899_(), (BlockState)CustomBlocks.border.m_49966_().m_61124_(BlockBorder.ROTATION, this.rotation));
      }
   }

   public void readExtraNBT(CompoundTag compound) {
      this.availability.load(compound.m_128469_("BorderAvailability"));
      this.rotation = compound.m_128451_("BorderRotation");
      this.height = compound.m_128451_("BorderHeight");
      this.message = compound.m_128461_("BorderMessage");
   }

   @Override
   public void m_183515_(CompoundTag compound) {
      this.writeExtraNBT(compound);
      super.m_183515_(compound);
   }

   public void writeExtraNBT(CompoundTag compound) {
      compound.m_128365_("BorderAvailability", this.availability.save(new CompoundTag()));
      compound.m_128405_("BorderRotation", this.rotation);
      compound.m_128405_("BorderHeight", this.height);
      compound.m_128359_("BorderMessage", this.message);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, TileBorder tile) {
      if (!level.f_46443_) {
         AABB box = new AABB(pos.m_123341_(), pos.m_123342_(), pos.m_123343_(), pos.m_123341_() + 1, pos.m_123342_() + tile.height + 1, pos.m_123343_() + 1);

         for (Entity entity : level.m_6443_(Entity.class, box, tile)) {
            if (entity instanceof ThrownEnderpearl pearl) {
               if (pearl.m_37282_() instanceof Player && !tile.availability.isAvailable((Player)pearl.m_37282_())) {
                  entity.m_142467_(RemovalReason.DISCARDED);
               }
            } else {
               Player player = (Player)entity;
               if (!tile.availability.isAvailable(player)) {
                  BlockPos pos2 = new BlockPos(tile.f_58858_);
                  if (tile.rotation == 2) {
                     pos2 = pos2.m_122019_();
                  } else if (tile.rotation == 0) {
                     pos2 = pos2.m_122012_();
                  } else if (tile.rotation == 1) {
                     pos2 = pos2.m_122029_();
                  } else if (tile.rotation == 3) {
                     pos2 = pos2.m_122024_();
                  }

                  while (!level.m_46859_(pos2)) {
                     pos2 = pos2.m_7494_();
                  }

                  player.m_6021_(pos2.m_123341_() + 0.5, pos2.m_123342_(), pos2.m_123343_() + 0.5);
                  if (!tile.message.isEmpty()) {
                     player.m_5661_(Component.m_237115_(tile.message), true);
                  }
               }
            }
         }
      }
   }

   public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
      this.handleUpdateTag(pkt.m_131708_());
   }

   public void handleUpdateTag(CompoundTag compound) {
      this.rotation = compound.m_128451_("Rotation");
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.m_195640_(this);
   }

   public CompoundTag m_5995_() {
      CompoundTag compound = new CompoundTag();
      compound.m_128405_("x", this.f_58858_.m_123341_());
      compound.m_128405_("y", this.f_58858_.m_123342_());
      compound.m_128405_("z", this.f_58858_.m_123343_());
      compound.m_128405_("Rotation", this.rotation);
      return compound;
   }

   public boolean isEntityApplicable(Entity var1) {
      return var1 instanceof ServerPlayer || var1 instanceof ThrownEnderpearl;
   }

   public boolean apply(Object ob) {
      return this.isEntityApplicable((Entity)ob);
   }
}
