package noppes.npcs.roles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntityLiving;
import noppes.npcs.api.entity.data.role.IJobSpawner;
import noppes.npcs.controllers.data.CloneSpawnData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.packets.server.SPacketToolMobSpawner;
import org.apache.commons.lang3.RandomStringUtils;

public class JobSpawner extends JobInterface implements IJobSpawner {
   public Map<Integer, CloneSpawnData> data = new HashMap<>();
   private int number = 0;
   public List<LivingEntity> spawned = new ArrayList<>();
   private Map<String, Long> cooldown = new HashMap<>();
   private String id = RandomStringUtils.random(8, true, true);
   public boolean doesntDie = false;
   public int spawnType = 0;
   public int xOffset = 0;
   public int yOffset = 0;
   public int zOffset = 0;
   private LivingEntity target;
   public boolean despawnOnTargetLost = true;

   public JobSpawner(EntityNPCInterface npc) {
      super(npc);
   }

   @Override
   public CompoundTag save(CompoundTag compound) {
      compound.m_128365_("SpawnerData", CloneSpawnData.save(this.data));
      compound.m_128359_("SpawnerId", this.id);
      compound.m_128379_("SpawnerDoesntDie", this.doesntDie);
      compound.m_128405_("SpawnerType", this.spawnType);
      compound.m_128405_("SpawnerXOffset", this.xOffset);
      compound.m_128405_("SpawnerYOffset", this.yOffset);
      compound.m_128405_("SpawnerZOffset", this.zOffset);
      compound.m_128379_("DespawnOnTargetLost", this.despawnOnTargetLost);
      return compound;
   }

   public String getTitle(int slot) {
      CloneSpawnData sd = this.data.get(slot);
      return sd == null ? "gui.selectnpc" : sd.tab + ": " + sd.name;
   }

   @Override
   public void load(CompoundTag compound) {
      this.data = CloneSpawnData.load(compound.m_128437_("SpawnerData", 10));
      this.id = compound.m_128461_("SpawnerId");
      this.doesntDie = compound.m_128471_("SpawnerDoesntDie");
      this.spawnType = compound.m_128451_("SpawnerType");
      this.xOffset = compound.m_128451_("SpawnerXOffset");
      this.yOffset = compound.m_128451_("SpawnerYOffset");
      this.zOffset = compound.m_128451_("SpawnerZOffset");
      this.despawnOnTargetLost = compound.m_128471_("DespawnOnTargetLost");
   }

   public void setJobCompound(int slot, int tab, String name) {
      this.data.put(slot, new CloneSpawnData(tab, name));
   }

   public void remove(int i) {
      this.data.remove(i);
   }

   @Override
   public void aiUpdateTask() {
      if (this.spawned.isEmpty()) {
         if (this.spawnType == 0 && this.spawnEntity(this.number) == null && !this.doesntDie) {
            this.npc.m_146870_();
         }

         if (this.spawnType == 1) {
            if (this.number >= 6 && !this.doesntDie) {
               this.npc.m_146870_();
            } else {
               this.spawnEntity(0);
               this.spawnEntity(1);
               this.spawnEntity(2);
               this.spawnEntity(3);
               this.spawnEntity(4);
               this.spawnEntity(5);
               this.number = 6;
            }
         }

         if (this.spawnType == 2) {
            ArrayList<CompoundTag> list = new ArrayList<>();

            for (CloneSpawnData d : this.data.values()) {
               CompoundTag c = d.getCompound();
               if (c != null && c.m_128441_("id")) {
                  list.add(c);
               }
            }

            if (!list.isEmpty()) {
               CompoundTag compound = list.get(this.npc.m_217043_().m_188503_(list.size()));
               this.spawnEntity(compound);
            } else if (!this.doesntDie) {
               this.npc.m_146870_();
            }
         }
      } else {
         this.checkSpawns();
      }
   }

   public void checkSpawns() {
      Iterator<LivingEntity> iterator = this.spawned.iterator();

      while (iterator.hasNext()) {
         LivingEntity spawn = iterator.next();
         if (this.shouldDelete(spawn)) {
            spawn.m_146870_();
            iterator.remove();
         } else {
            this.checkTarget(spawn);
         }
      }
   }

   public void checkTarget(LivingEntity entity) {
      if (entity instanceof Mob liv) {
         if (liv.m_5448_() == null || this.npc.m_217043_().m_188503_(100) == 1) {
            liv.m_6710_(this.target);
         }
      } else if (entity.m_21188_() == null || this.npc.m_217043_().m_188503_(100) == 1) {
         entity.m_6703_(this.target);
      }
   }

   public boolean shouldDelete(LivingEntity entity) {
      return !this.npc.isInRange(entity, 60.0) || entity.m_213877_() || entity.m_21223_() <= 0.0F || this.despawnOnTargetLost && this.target == null;
   }

   private LivingEntity getTarget() {
      LivingEntity target = this.getTarget(this.npc);
      if (target != null) {
         return target;
      }

      for (LivingEntity entity : this.spawned) {
         target = this.getTarget(entity);
         if (target != null) {
            return target;
         }
      }

      return null;
   }

   private LivingEntity getTarget(LivingEntity entity) {
      if (entity instanceof Mob) {
         this.target = ((Mob)entity).m_5448_();
         if (this.target != null && !this.target.m_213877_() && this.target.m_21223_() > 0.0F) {
            return this.target;
         }
      }

      this.target = entity.m_21188_();
      return this.target != null && !this.target.m_213877_() && this.target.m_21223_() > 0.0F ? this.target : null;
   }

   private void setTarget(LivingEntity base, LivingEntity target) {
      if (base instanceof Mob) {
         ((Mob)base).m_6710_(target);
      } else {
         base.m_6703_(target);
      }
   }

   @Override
   public boolean aiShouldExecute() {
      if (!this.data.isEmpty() && !this.npc.isKilled()) {
         this.target = this.getTarget();
         if (this.npc.m_217043_().m_188503_(30) == 1 && this.spawned.isEmpty()) {
            this.spawned = this.getNearbySpawned();
         }

         if (!this.spawned.isEmpty()) {
            this.checkSpawns();
         }

         return this.target != null;
      } else {
         return false;
      }
   }

   @Override
   public boolean aiContinueExecute() {
      return this.aiShouldExecute();
   }

   @Override
   public void stop() {
      this.reset();
   }

   @Override
   public void aiStartExecuting() {
      this.number = 0;

      for (LivingEntity entity : this.spawned) {
         int i = entity.getPersistentData().m_128451_("NpcSpawnerNr");
         if (i > this.number) {
            this.number = i;
         }

         this.setTarget(entity, this.npc.m_5448_());
      }
   }

   @Override
   public void reset() {
      this.number = 0;
      if (this.spawned.isEmpty()) {
         this.spawned = this.getNearbySpawned();
      }

      this.target = null;
      this.checkSpawns();
   }

   @Override
   public void killed() {
      this.reset();
   }

   private LivingEntity spawnEntity(CompoundTag compound) {
      if (compound != null && compound.m_128441_("id")) {
         double x = this.npc.m_20185_() + this.xOffset - 0.5 + this.npc.m_217043_().m_188501_();
         double y = this.npc.m_20186_() + this.yOffset;
         double z = this.npc.m_20189_() + this.zOffset - 0.5 + this.npc.m_217043_().m_188501_();
         Entity entity = SPacketToolMobSpawner.spawnClone(compound, x, y, z, this.npc.f_19853_);
         if (entity != null && entity instanceof LivingEntity living) {
            living.getPersistentData().m_128359_("NpcSpawnerId", this.id);
            living.getPersistentData().m_128405_("NpcSpawnerNr", this.number);
            this.setTarget(living, this.npc.m_5448_());
            living.m_6034_(x, y, z);
            if (living instanceof EntityNPCInterface snpc) {
               snpc.stats.spawnCycle = 4;
               snpc.stats.respawnTime = 0;
               snpc.ais.returnToStart = false;
            }

            this.spawned.add(living);
            return living;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private CompoundTag getCompound(int i) {
      for (Entry<Integer, CloneSpawnData> entry : this.data.entrySet()) {
         if (i <= entry.getKey()) {
            CompoundTag compound = entry.getValue().getCompound();
            if (compound != null && compound.m_128441_("id")) {
               this.number = entry.getKey() + 1;
               return compound;
            }
         }
      }

      return null;
   }

   private List<LivingEntity> getNearbySpawned() {
      List<LivingEntity> spawnList = new ArrayList<>();

      for (LivingEntity entity : this.npc.f_19853_.m_45976_(LivingEntity.class, this.npc.m_20191_().m_82377_(40.0, 40.0, 40.0))) {
         if (entity.getPersistentData().m_128461_("NpcSpawnerId").equals(this.id) && !entity.m_213877_()) {
            spawnList.add(entity);
         }
      }

      return spawnList;
   }

   public boolean isOnCooldown(String name) {
      if (!this.cooldown.containsKey(name)) {
         return false;
      }

      long time = this.cooldown.get(name);
      return System.currentTimeMillis() < time + 1200000L;
   }

   @Override
   public IEntityLiving spawnEntity(int i) {
      CompoundTag compound = this.getCompound(i);
      if (compound == null) {
         return null;
      }

      LivingEntity base = this.spawnEntity(compound);
      return base == null ? null : (IEntityLiving)NpcAPI.Instance().getIEntity(base);
   }

   @Override
   public void removeAllSpawned() {
      for (LivingEntity entity : this.spawned) {
         entity.m_146870_();
      }

      this.spawned = new ArrayList<>();
   }

   @Override
   public int getType() {
      return 6;
   }
}
