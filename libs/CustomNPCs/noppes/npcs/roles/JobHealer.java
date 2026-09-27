package noppes.npcs.roles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import noppes.npcs.NBTTags;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.ValueUtil;

public class JobHealer extends JobInterface {
   private int healTicks = 0;
   public int range = 8;
   public byte type = 2;
   public int speed = 20;
   public HashMap<Integer, Integer> effects = new HashMap<>();
   private List<LivingEntity> affected = new ArrayList<>();

   public JobHealer(EntityNPCInterface npc) {
      super(npc);
   }

   @Override
   public CompoundTag save(CompoundTag nbttagcompound) {
      nbttagcompound.m_128405_("HealerRange", this.range);
      nbttagcompound.m_128344_("HealerType", this.type);
      nbttagcompound.m_128365_("BeaconEffects", NBTTags.nbtIntegerIntegerMap(this.effects));
      nbttagcompound.m_128405_("HealerSpeed", this.speed);
      return nbttagcompound;
   }

   @Override
   public void load(CompoundTag nbttagcompound) {
      this.range = nbttagcompound.m_128451_("HealerRange");
      this.type = nbttagcompound.m_128445_("HealerType");
      this.effects = NBTTags.getIntegerIntegerMap(nbttagcompound.m_128437_("BeaconEffects", 10));
      this.speed = ValueUtil.CorrectInt(nbttagcompound.m_128451_("HealerSpeed"), 10, Integer.MAX_VALUE);
   }

   @Override
   public boolean aiShouldExecute() {
      this.healTicks++;
      if (this.healTicks < this.speed) {
         return false;
      }

      this.healTicks = 0;
      this.affected = this.npc.f_19853_.m_45976_(LivingEntity.class, this.npc.m_20191_().m_82377_(this.range, this.range / 2.0, this.range));
      return !this.affected.isEmpty();
   }

   @Override
   public boolean aiContinueExecute() {
      return false;
   }

   @Override
   public void aiStartExecuting() {
      for (LivingEntity entity : this.affected) {
         boolean isEnemy = false;
         if (entity instanceof Player) {
            isEnemy = this.npc.faction.isAggressiveToPlayer((Player)entity);
         } else if (entity instanceof EntityNPCInterface) {
            isEnemy = this.npc.faction.isAggressiveToNpc((EntityNPCInterface)entity);
         } else {
            isEnemy = entity instanceof Mob;
         }

         if (entity != this.npc && (this.type != 0 || !isEnemy) && (this.type != 1 || isEnemy)) {
            for (Integer potionEffect : this.effects.keySet()) {
               MobEffect p = MobEffect.m_19453_(potionEffect);
               if (p != null) {
                  entity.m_7292_(new MobEffectInstance(p, 100, this.effects.get(potionEffect)));
               }
            }
         }
      }

      this.affected.clear();
   }

   @Override
   public int getType() {
      return 2;
   }
}
