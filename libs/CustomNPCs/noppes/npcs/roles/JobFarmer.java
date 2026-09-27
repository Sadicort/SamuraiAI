package noppes.npcs.roles;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal.Flag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.StemGrownBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootContext.Builder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.role.IJobFarmer;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.controllers.MassBlockController;
import noppes.npcs.controllers.data.BlockData;
import noppes.npcs.entity.EntityNPCInterface;

public class JobFarmer extends JobInterface implements MassBlockController.IMassBlock, IJobFarmer {
   public int chestMode = 1;
   private List<BlockPos> trackedBlocks = new ArrayList<>();
   private int ticks = 0;
   private int walkTicks = 0;
   private int blockTicks = 800;
   private boolean waitingForBlocks = false;
   private BlockPos ripe = null;
   private BlockPos chest = null;
   private ItemStack holding = ItemStack.f_41583_;

   public JobFarmer(EntityNPCInterface npc) {
      super(npc);
      this.overrideMainHand = true;
   }

   @Override
   public IItemStack getMainhand() {
      String name = this.npc.getJobData();
      ItemStack item = this.stringToItem(name);
      return item.m_41619_() ? this.npc.inventory.weapons.get(0) : NpcAPI.Instance().getIItemStack(item);
   }

   @Override
   public CompoundTag save(CompoundTag compound) {
      compound.m_128405_("JobChestMode", this.chestMode);
      if (!this.holding.m_41619_()) {
         compound.m_128365_("JobHolding", this.holding.m_41739_(new CompoundTag()));
      }

      return compound;
   }

   @Override
   public void load(CompoundTag compound) {
      this.chestMode = compound.m_128451_("JobChestMode");
      this.holding = ItemStack.m_41712_(compound.m_128469_("JobHolding"));
      this.blockTicks = 1100;
   }

   public void setHolding(ItemStack item) {
      this.holding = item;
      this.npc.setJobData(this.itemToString(this.holding));
   }

   @Override
   public boolean aiShouldExecute() {
      if (!this.holding.m_41619_()) {
         if (this.chestMode == 0) {
            this.setHolding(ItemStack.f_41583_);
         } else if (this.chestMode == 1) {
            if (this.chest == null) {
               this.dropItem(this.holding);
               this.setHolding(ItemStack.f_41583_);
            } else {
               this.chest();
            }
         } else if (this.chestMode == 2) {
            this.dropItem(this.holding);
            this.setHolding(ItemStack.f_41583_);
         }

         return false;
      } else {
         if (this.ripe != null) {
            this.pluck();
            return false;
         }

         if (!this.waitingForBlocks && this.blockTicks++ > 1200) {
            this.blockTicks = 0;
            this.waitingForBlocks = true;
            MassBlockController.Queue(this);
         }

         if (this.ticks++ < 100) {
            return false;
         }

         this.ticks = 0;
         return true;
      }
   }

   private void dropItem(ItemStack item) {
      ItemEntity entityitem = new ItemEntity(this.npc.f_19853_, this.npc.m_20185_(), this.npc.m_20186_(), this.npc.m_20189_(), item);
      entityitem.m_32060_();
      this.npc.f_19853_.m_7967_(entityitem);
   }

   private void chest() {
      BlockPos pos = this.chest;
      this.npc.m_21573_().m_26519_(pos.m_123341_(), pos.m_123342_(), pos.m_123343_(), 1.0);
      this.npc.m_21563_().m_24950_(pos.m_123341_(), pos.m_123342_(), pos.m_123343_(), 10.0F, this.npc.m_8132_());
      if (this.npc.nearPosition(pos) || this.walkTicks++ > 400) {
         if (this.walkTicks < 400) {
            this.npc.m_6674_(InteractionHand.MAIN_HAND);
         }

         this.npc.m_21573_().m_26573_();
         this.ticks = 100;
         this.walkTicks = 0;
         BlockState state = this.npc.f_19853_.m_8055_(pos);
         BlockEntity tile = this.npc.f_19853_.m_7702_(pos);
         Container inventory = tile instanceof Container ? (Container)tile : null;
         if (state.m_60734_() instanceof ChestBlock) {
            inventory = ChestBlock.m_51511_((ChestBlock)state.m_60734_(), state, this.npc.f_19853_, pos, true);
         }

         if (inventory == null) {
            this.chest = null;
         } else {
            for (int i = 0; !this.holding.m_41619_() && i < inventory.m_6643_(); i++) {
               this.holding = this.mergeStack(inventory, i, this.holding);
            }

            for (int i = 0; !this.holding.m_41619_() && i < inventory.m_6643_(); i++) {
               ItemStack item = inventory.m_8020_(i);
               if (item.m_41619_()) {
                  inventory.m_6836_(i, this.holding);
                  this.holding = ItemStack.f_41583_;
               }
            }

            if (!this.holding.m_41619_()) {
               this.dropItem(this.holding);
               this.holding = ItemStack.f_41583_;
            }
         }

         this.setHolding(this.holding);
      }
   }

   private ItemStack mergeStack(Container inventory, int slot, ItemStack item) {
      ItemStack item2 = inventory.m_8020_(slot);
      if (!NoppesUtilPlayer.compareItems(item, item2, false, false)) {
         return item;
      } else {
         int size = item2.m_41741_() - item2.m_41613_();
         if (size >= item.m_41613_()) {
            item2.m_41764_(item2.m_41613_() + item.m_41613_());
            return ItemStack.f_41583_;
         } else {
            item2.m_41764_(item2.m_41741_());
            item.m_41764_(item.m_41613_() - size);
            return item.m_41619_() ? ItemStack.f_41583_ : item;
         }
      }
   }

   private void pluck() {
      BlockPos pos = this.ripe;
      this.npc.m_21573_().m_26519_(pos.m_123341_(), pos.m_123342_(), pos.m_123343_(), 1.0);
      this.npc.m_21563_().m_24950_(pos.m_123341_(), pos.m_123342_(), pos.m_123343_(), 10.0F, this.npc.m_8132_());
      if (this.npc.nearPosition(pos) || this.walkTicks++ > 400) {
         if (this.walkTicks > 400) {
            pos = NoppesUtilServer.GetClosePos(pos, this.npc.f_19853_);
            this.npc.m_6021_(pos.m_123341_() + 0.5, pos.m_123342_(), pos.m_123343_() + 0.5);
         }

         this.ripe = null;
         this.npc.m_21573_().m_26573_();
         this.ticks = 90;
         this.walkTicks = 0;
         this.npc.m_6674_(InteractionHand.MAIN_HAND);
         BlockState state = this.npc.f_19853_.m_8055_(pos);
         Block b = state.m_60734_();
         if (b instanceof CropBlock && ((CropBlock)b).m_52307_(state)) {
            CropBlock crop = (CropBlock)b;
            Item item = crop.m_7397_(this.npc.f_19853_, pos, state).m_41720_();
            Builder builder = new Builder((ServerLevel)this.npc.f_19853_)
               .m_230911_(this.npc.f_19853_.f_46441_)
               .m_78972_(LootContextParams.f_81460_, Vec3.m_82512_(pos))
               .m_78972_(LootContextParams.f_81463_, this.npc.m_21205_())
               .m_78972_(LootContextParams.f_81461_, state)
               .m_78984_(LootContextParams.f_81462_, this.npc.f_19853_.m_7702_(pos));
            LootTable loottable = this.npc.m_20194_().m_129898_().m_79217_(b.m_60589_());
            List<ItemStack> l = loottable.m_230922_(builder.m_78975_(LootContextParamSets.f_81421_));
            this.npc.f_19853_.m_7731_(pos, crop.m_52289_(0), 2);
            if (l.isEmpty()) {
               this.holding = ItemStack.f_41583_;
            } else if (l.size() == 1) {
               this.holding = l.get(0);
            } else {
               List<ItemStack> fl = l.stream().filter(t -> t.m_41720_() != item).collect(Collectors.toList());
               if (fl.isEmpty()) {
                  fl = l;
               }

               this.holding = fl.get(this.npc.m_217043_().m_188503_(fl.size()));
            }

            this.holding.m_41764_(1);
         }

         if (b instanceof StemGrownBlock) {
            b = this.npc.f_19853_.m_8055_(pos).m_60734_();
            this.npc.f_19853_.m_7471_(pos, false);
            this.holding = new ItemStack(b);
         }

         this.setHolding(this.holding);
      }
   }

   @Override
   public boolean aiContinueExecute() {
      return false;
   }

   @Override
   public void aiUpdateTask() {
      Iterator<BlockPos> ite = this.trackedBlocks.iterator();

      while (ite.hasNext() && this.ripe == null) {
         BlockPos pos = ite.next();
         BlockState state = this.npc.f_19853_.m_8055_(pos);
         Block b = state.m_60734_();
         if ((b instanceof CropBlock && ((CropBlock)b).m_52307_(state) || b instanceof StemGrownBlock) && b.m_60589_() != BuiltInLootTables.f_78712_) {
            this.ripe = pos;
         } else {
            ite.remove();
         }
      }

      this.npc.ais.returnToStart = this.ripe == null;
      if (this.ripe != null) {
         this.npc.m_21573_().m_26573_();
         this.npc.m_21563_().m_24950_(this.ripe.m_123341_(), this.ripe.m_123342_(), this.ripe.m_123343_(), 10.0F, this.npc.m_8132_());
      }
   }

   @Override
   public boolean isPlucking() {
      return this.ripe != null || !this.holding.m_41619_();
   }

   @Override
   public EntityNPCInterface getNpc() {
      return this.npc;
   }

   @Override
   public int getRange() {
      return 16;
   }

   @Override
   public void processed(List<BlockData> list) {
      List<BlockPos> trackedBlocks = new ArrayList<>();
      BlockPos chest = null;

      for (BlockData data : list) {
         BlockEntity tile = this.npc.f_19853_.m_7702_(data.pos);
         Block b = data.state.m_60734_();
         if (tile instanceof RandomizableContainerBlockEntity) {
            if (chest == null
               || this.npc.m_20275_(chest.m_123341_(), chest.m_123342_(), chest.m_123343_())
                  > this.npc.m_20275_(data.pos.m_123341_(), data.pos.m_123342_(), data.pos.m_123343_())) {
               chest = data.pos;
            }
         } else if ((b instanceof CropBlock || b instanceof StemBlock) && !trackedBlocks.contains(data.pos)) {
            trackedBlocks.add(data.pos);
         }
      }

      this.chest = chest;
      this.trackedBlocks = trackedBlocks;
      this.waitingForBlocks = false;
   }

   @Override
   public EnumSet<Flag> getFlags() {
      return EnumSet.of(Flag.MOVE);
   }

   @Override
   public int getType() {
      return 11;
   }
}
