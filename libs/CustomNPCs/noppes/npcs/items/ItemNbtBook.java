package noppes.npcs.items;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import noppes.npcs.CustomTabs;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.client.PacketGuiData;
import noppes.npcs.packets.client.PacketGuiOpen;

public class ItemNbtBook extends Item {
   public ItemNbtBook() {
      super(new Properties().m_41487_(1).m_41491_(CustomTabs.tab));
   }

   public void blockEvent(RightClickBlock event) {
      Packets.send((ServerPlayer)event.getEntity(), new PacketGuiOpen(EnumGuiType.NbtBook, event.getPos()));
      BlockState state = event.getLevel().m_8055_(event.getPos());
      CompoundTag data = new CompoundTag();
      BlockEntity tile = event.getLevel().m_7702_(event.getPos());
      if (tile != null) {
         data = tile.m_187480_();
      }

      CompoundTag compound = new CompoundTag();
      compound.m_128365_("Data", data);
      Packets.send((ServerPlayer)event.getEntity(), new PacketGuiData(compound));
   }

   public void entityEvent(EntityInteract event) {
      Packets.send((ServerPlayer)event.getEntity(), new PacketGuiOpen(EnumGuiType.NbtBook, BlockPos.f_121853_));
      CompoundTag data = new CompoundTag();
      event.getTarget().m_20086_(data);
      CompoundTag compound = new CompoundTag();
      compound.m_128405_("EntityId", event.getTarget().m_19879_());
      compound.m_128365_("Data", data);
      Packets.send((ServerPlayer)event.getEntity(), new PacketGuiData(compound));
   }
}
